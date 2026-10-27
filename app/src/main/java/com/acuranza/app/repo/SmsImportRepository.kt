package com.acuranza.app.repo

import androidx.room.withTransaction
import com.acuranza.app.data.AccountEntity
import com.acuranza.app.data.SumvaloDatabase
import com.acuranza.app.data.SmsImportBatchEntity
import com.acuranza.app.data.SmsImportEntity
import com.acuranza.app.data.TransactionEntity
import com.acuranza.app.model.AccountType
import com.acuranza.app.model.TxnSource
import com.acuranza.app.model.TxnType
import com.acuranza.app.sms.CategoryGuesser
import com.acuranza.app.sms.MerchantNormalizer
import com.acuranza.app.sms.ParsedBalance
import com.acuranza.app.sms.ParsedTransaction
import com.acuranza.app.sms.SmsConfidence

/**
 * A transaction the user confirmed during the first-import review, after any edits.
 * [parsed.fingerprint] stays the parser's key for the message it came from, so the same SMS is
 * never imported twice even when the user corrected the amount or category.
 */
data class ApprovedSmsImport(
    val parsed: ParsedTransaction,
    /** The SMS sender id, kept as import metadata; the message body is never stored. */
    val sender: String,
    val amount: Long,
    val categoryKey: String,
    val merchant: String?,
    val timestampMillis: Long,
    val accountId: Long,
    /** The scan's grading; below HIGH the transaction is saved `suspicious` until Review confirms it. */
    val confidence: SmsConfidence,
)

/**
 * Saves, lists, edits and reverses SMS-derived transactions. Everything is scoped to the batch
 * bookkeeping in `sms_import_batches` / `sms_imports`; a pre-existing transaction can never be
 * touched by an undo because every import row references only the transaction its own batch
 * created.
 */
class SmsImportRepository(private val db: SumvaloDatabase, private val attachments: com.acuranza.app.repo.AttachmentRepository? = null) {

    /**
     * The account a parsed transaction belongs to. Matching is by the digits the message showed
     * (last four of the account or card) against accounts that already know those digits, then by
     * bank + type. When nothing matches, a clearly-labelled account is created so the money lands
     * somewhere honest instead of silently in the user's main account.
     */
    /** The account that already matches [txn], without creating anything; null = a new one at import. */
    suspend fun matchAccount(txn: ParsedTransaction): Long? {
        val accounts = db.accounts().all().filterNot { it.archived }
        val digits = listOfNotNull(txn.accountLast4)
        accounts.firstOrNull { a -> digits.isNotEmpty() && digits.any { it in a.allDigits } }?.let { return it.id }
        val type = if (txn.isCreditCard) AccountType.CREDIT_CARD else AccountType.BANK
        return db.accounts().byBankAndType(txn.bank.displayName, type).firstOrNull { !it.archived }?.id
    }

    suspend fun resolveAccount(txn: ParsedTransaction): Long {
        val accounts = db.accounts().all().filterNot { it.archived }
        val digits = listOfNotNull(txn.accountLast4)
        val byDigits = accounts.firstOrNull { a -> digits.isNotEmpty() && digits.any { it in a.allDigits } }
        if (byDigits != null) return byDigits.id

        val type = if (txn.isCreditCard) AccountType.CREDIT_CARD else AccountType.BANK
        val byBank = db.accounts().byBankAndType(txn.bank.displayName, type).firstOrNull { !it.archived }
        if (byBank != null) {
            // Teach the account the digits this bank uses for it, if new.
            val learned = txn.accountLast4?.let { byBank.plusOtherDigits(it) } ?: byBank
            if (learned != byBank) db.accounts().update(learned)
            return byBank.id
        }

        return db.accounts().insert(
            AccountEntity(
                name = txn.bank.displayName + (txn.accountLast4?.let { " $it" } ?: ""),
                type = type,
                bank = txn.bank.displayName,
                last4 = txn.accountLast4,
                autoCreated = true,
            ),
        )
    }

    /**
     * Saves one confirmed import batch inside a single database transaction: the batch row, each
     * transaction (deduped by the unique fingerprint index) and its import row. Fingerprints that
     * already exist are skipped, so re-scanning the same inbox adds nothing twice.
     *
     * Ambiguous (below HIGH confidence) rows are saved `suspicious` — visible in Review, but they
     * never move balances or stats until the user confirms them.
     *
     * @return the batch id and how many transactions were actually created.
     */
    suspend fun importBatch(
        label: String,
        items: List<ApprovedSmsImport>,
        applyBalances: List<ParsedBalance> = emptyList(),
    ): Pair<Long, Int> = db.withTransaction {
        var created = 0
        val batchId = db.smsImports().insertBatch(
            SmsImportBatchEntity(importedAt = System.currentTimeMillis(), txnCount = 0, label = label),
        )
        for (item in items) {
            if (db.smsImports().hasFingerprint(item.parsed.fingerprint)) continue
            // Fuzzy duplicate guard: a transaction with the same account, type and amount within
            // ±1 day is almost always the same money seen twice (an SMS and a CSV import, or two
            // bank notification formats). Skipped rather than silently double-counted; the
            // fingerprint check above still allows the same message to be re-reviewed safely.
            val itemDay = com.acuranza.app.util.Time.epochDayOf(item.timestampMillis)
            val nearExisting = db.transactions().findAmountNear(
                item.accountId,
                item.amount,
                com.acuranza.app.util.Time.startOfDay(java.time.LocalDate.ofEpochDay(itemDay - 1)),
                com.acuranza.app.util.Time.startOfDay(java.time.LocalDate.ofEpochDay(itemDay + 1)) - 1,
            ).any { existing ->
                (existing.type == item.parsed.type || existing.type == TxnType.TRANSFER) && existing.fingerprint != item.parsed.fingerprint
            }
            if (nearExisting) continue
            val confidence = item.confidence
            val txnId = db.transactions().insertIgnore(
                TransactionEntity(
                    accountId = item.accountId,
                    type = item.parsed.type,
                    amount = item.amount,
                    categoryKey = item.categoryKey,
                    merchant = item.merchant?.takeIf { it.isNotBlank() },
                    note = noteFor(item),
                    timestamp = item.timestampMillis,
                    channel = item.parsed.channel,
                    source = TxnSource.SMS,
                    fingerprint = item.parsed.fingerprint,
                    referenceNo = item.parsed.referenceNo,
                    reviewed = false,
                    suspicious = confidence != SmsConfidence.HIGH,
                ),
            )
            if (txnId == -1L) continue // unique fingerprint already present
            db.smsImports().insertIgnore(
                SmsImportEntity(
                    batchId = batchId,
                    transactionId = txnId,
                    fingerprint = item.parsed.fingerprint,
                    sender = item.sender,
                    bank = item.parsed.bank.displayName,
                    accountLast4 = item.parsed.accountLast4,
                    type = item.parsed.type,
                    amount = item.amount,
                    merchant = item.merchant?.takeIf { it.isNotBlank() },
                    timestamp = item.timestampMillis,
                    channel = item.parsed.channel,
                    confidence = confidence,
                ),
            )
            created++
        }
        for (balance in applyBalances) applyBalance(balance)
        db.smsImports().batchById(batchId)?.let {
            db.smsImports().updateBatch(it.copy(txnCount = created))
        }
        batchId to created
    }

    /**
     * Reverses a batch: deletes exactly the transactions it created (each is referenced by this
     * batch's import rows and nothing else), then the import rows and the batch record.
     * Transactions that existed before the import — manual entries, CSV imports — are structurally
     * out of reach.
     *
     * @return how many transactions were removed.
     */
    suspend fun undoBatch(batchId: Long): Int = db.withTransaction {
        val rows = db.smsImports().byBatch(batchId)
        db.smsImports().deleteTransactionsOf(batchId)
        db.smsImports().deleteRowsOf(batchId)
        db.smsImports().deleteBatch(batchId)
        rows.count { it.transactionId != null }
    }

    /** Deletes one import and its transaction (and any receipt files on it); the rest of the batch is untouched. */
    suspend fun deleteImport(importId: Long) = db.withTransaction {
        val row = db.smsImports().byId(importId) ?: return@withTransaction
        // Receipt files have no SQL cascade — delete them via the repo before the row goes,
        // otherwise the encrypted files are orphaned on disk.
        row.transactionId?.let { attachments?.deleteAllFor(it) }
        db.smsImports().deleteTransactionOf(importId)
        db.smsImports().deleteRow(importId)
        row
    }

    /**
     * Removes every SMS import, its transactions, its receipt files and the batch records, then
     * deletes the SMS-created accounts that are left with zero transactions (provenance:
     * `autoCreated`, and any account the user added transactions to survives). Manual data is
     * untouched. Transactional and idempotent — a second call finds nothing and removes zero.
     */
    suspend fun deleteAll(): Int = db.withTransaction {
        var removed = 0
        val batches = db.smsImports().latestBatches(limit = 10_000)
        // Receipt files have no SQL cascade — remove them via the repo before their rows cascade away.
        for (batch in batches)
            for (row in db.smsImports().byBatch(batch.id))
                row.transactionId?.let { attachments?.deleteAllFor(it) }
        for (batch in batches) removed += undoBatch(batch.id)
        for (account in db.accounts().emptyAutoCreated()) db.accounts().delete(account)
        removed
    }

    /** The bank's own balance report: reconciles the matched account without inventing transactions. */
    suspend fun applyBalance(balance: ParsedBalance): Boolean {
        val available = balance.availableBalance ?: return false
        val accounts = db.accounts().all()
        val match = accounts.firstOrNull { a -> balance.accountLast4?.let { it in a.allDigits } == true && !a.archived }
            ?: accounts.firstOrNull { it.bank == balance.bank.displayName && it.type == AccountType.BANK && !it.archived }
            ?: return false
        db.accounts().update(match.copy(reportedBalance = available, reportedAt = balance.timestampMillis))
        return true
    }

    /** Category for a parsed txn: rules the user taught first, then the dataset's guess. */
    suspend fun suggestCategory(txn: ParsedTransaction, merchant: String?): String {
        val key = merchant?.takeIf { it.isNotBlank() } ?: txn.merchant
        val normalised = key?.let { runCatching { MerchantNormalizer.normalize(it) }.getOrNull() } ?: key
        if (normalised != null) {
            db.merchantRules().categoryFor(normalised)?.let { return it }
        }
        return CategoryGuesser.guess(merchant, merchant ?: txn.merchant ?: "", txn.type, txn.channel)
    }

    // ---- Permissionless capture: shared/pasted messages ----

    /** Outcomes of one shared or pasted message. */
    sealed interface PendingOutcome {
        data class Added(
            val id: Long,
            /** Presentation fields for the detected-transaction alert; null where the SMS lacked them. */
            val bank: String,
            val amount: Long,
            val type: com.acuranza.app.model.TxnType,
            val merchant: String?,
        ) : PendingOutcome
        data class Duplicate(val id: Long) : PendingOutcome
        data class Rejected(val reason: String) : PendingOutcome
    }

    /**
     * Parses one shared message and parks it for review. Nothing reaches the transaction table
     * before the user confirms; the message text itself is discarded the moment it is parsed.
     */
    suspend fun addPending(body: String, nowMillis: Long = System.currentTimeMillis()): PendingOutcome = db.withTransaction {
        when (val result = com.acuranza.app.sms.SharedSmsPipeline.process(body, nowMillis)) {
            is com.acuranza.app.sms.SharedSmsPipeline.Result.Rejected -> PendingOutcome.Rejected(result.reason)
            is com.acuranza.app.sms.SharedSmsPipeline.Result.Candidate -> {
                val txn = result.txn
                if (db.smsImports().hasFingerprint(txn.fingerprint)) {
                    return@withTransaction PendingOutcome.Duplicate(-1)
                }
                // The presentation summary is captured before the parsed message is discarded —
                // the alert must never need the raw text again.
                val summaryBank = txn.bank.displayName
                val summaryAmount = txn.amount
                val summaryType = txn.type
                val summaryMerchant = txn.merchant
                val batch = db.smsImports().pendingBatch()
                    ?: db.smsImports().insertBatch(SmsImportBatchEntity(importedAt = 0, txnCount = 0, label = "Pending review"))
                        .let { db.smsImports().batchById(it)!! }
                val id = db.smsImports().insertIgnore(
                    SmsImportEntity(
                        batchId = batch.id,
                        transactionId = null,
                        fingerprint = txn.fingerprint,
                        sender = "shared",
                        bank = txn.bank.displayName,
                        accountLast4 = txn.accountLast4,
                        type = txn.type,
                        amount = txn.amount,
                        merchant = txn.merchant,
                        timestamp = txn.timestampMillis,
                        channel = txn.channel,
                        confidence = result.confidence,
                        suggestedCategory = txn.suggestedCategory,
                    ),
                )
                if (id == -1L) PendingOutcome.Duplicate(-1)
                else PendingOutcome.Added(id, summaryBank, summaryAmount, summaryType, summaryMerchant)
            }
        }
    }

    /**
     * Full variant: parks an already-parsed inbox candidate for review, exactly like a shared
     * message. Nothing imports before the user confirms. Deduplicated by fingerprint.
     */
    suspend fun addPendingParsed(txn: ParsedTransaction, confidence: SmsConfidence): PendingOutcome = db.withTransaction {
        if (db.smsImports().hasFingerprint(txn.fingerprint)) return@withTransaction PendingOutcome.Duplicate(-1)
        val batch = db.smsImports().pendingBatch()
            ?: db.smsImports().insertBatch(SmsImportBatchEntity(importedAt = 0, txnCount = 0, label = "Pending review"))
                .let { db.smsImports().batchById(it)!! }
        val id = db.smsImports().insertIgnore(
            SmsImportEntity(
                batchId = batch.id,
                transactionId = null,
                fingerprint = txn.fingerprint,
                sender = "inbox",
                bank = txn.bank.displayName,
                accountLast4 = txn.accountLast4,
                type = txn.type,
                amount = txn.amount,
                merchant = txn.merchant,
                timestamp = txn.timestampMillis,
                channel = txn.channel,
                confidence = confidence,
                suggestedCategory = txn.suggestedCategory,
            ),
        )
        if (id == -1L) PendingOutcome.Duplicate(-1)
        else PendingOutcome.Added(id, txn.bank.displayName, txn.amount, txn.type, txn.merchant)
    }

    /** Candidates waiting for review, newest first. */
    suspend fun pending(): List<SmsImportEntity> = db.smsImports().pending()

    suspend fun pendingCount(): Int = db.smsImports().pendingCount()

    /** Drops one pending candidate without importing anything. */
    suspend fun removePending(id: Long) = db.withTransaction { db.smsImports().deleteRow(id) }

    /** Clears every pending candidate. */
    suspend fun clearPending() = db.withTransaction {
        for (row in db.smsImports().pending()) db.smsImports().deleteRow(row.id)
    }

    /** The user's corrections to a pending candidate before it becomes a transaction. */
    data class PendingEdit(val amount: Long? = null, val merchant: String? = null, val categoryKey: String? = null, val accountId: Long? = null)

    /**
     * The user confirmed the pending candidates: each becomes a real transaction in one new
     * batch (deduped again by fingerprint) and is linked to its import row; rows the user
     * excluded are dropped. Only the already-extracted fields were ever kept — no message text.
     *
     * @return how many transactions were created.
     */
    suspend fun confirmPending(keep: List<SmsImportEntity>, edits: Map<Long, PendingEdit> = emptyMap()): Int = db.withTransaction {
        if (keep.isEmpty()) return@withTransaction 0 // nothing to confirm: no empty batch rows
        val batchId = db.smsImports().insertBatch(
            SmsImportBatchEntity(importedAt = System.currentTimeMillis(), txnCount = 0, label = "Shared import"),
        )
        var created = 0
        for (row in keep) {
            val edit = edits[row.id]
            val category = edit?.categoryKey
                ?: row.suggestedCategory
                ?: CategoryGuesser.guess(row.merchant, row.merchant ?: "", row.type, row.channel)
            val accountId = edit?.accountId ?: resolveAccountFor(row)
            val txnId = db.transactions().insertIgnore(
                TransactionEntity(
                    accountId = accountId,
                    type = row.type,
                    amount = edit?.amount ?: row.amount,
                    categoryKey = category,
                    merchant = (edit?.merchant ?: row.merchant)?.takeIf { it.isNotBlank() },
                    note = row.merchant?.takeIf { it.isNotBlank() },
                    timestamp = row.timestamp,
                    channel = row.channel,
                    source = TxnSource.SMS,
                    fingerprint = row.fingerprint,
                    reviewed = false,
                    suspicious = row.confidence != SmsConfidence.HIGH,
                ),
            )
            if (txnId == -1L) {
                db.smsImports().deleteRow(row.id)
                continue
            }
            db.smsImports().linkTransaction(row.id, txnId, batchId)
            created++
        }
        db.smsImports().batchById(batchId)?.let { db.smsImports().updateBatch(it.copy(txnCount = created)) }
        created
    }

    /** An account for a pending row: an existing digits/bank match, else a clearly-labelled new one. */
    /** The accounts the matcher sees, in its vocabulary. */
    private suspend fun candidates(): List<com.acuranza.app.data.sms.SmsAccountMatcher.CandidateAccount> =
        db.accounts().all().filterNot { it.archived }.map {
            com.acuranza.app.data.sms.SmsAccountMatcher.CandidateAccount(
                id = it.id,
                name = it.name,
                bank = it.bank,
                type = it.type,
                allDigits = it.allDigits,
            )
        }

    /**
     * Read-only: what would happen to [row] on confirm — which account it matches and how
     * confidently, so the review screen can show it and the user can override it. A [None]
     * match means confirming would create a new, clearly-labelled account; the UI flags that.
     */
    suspend fun accountHint(row: SmsImportEntity): com.acuranza.app.data.sms.SmsAccountMatcher.Match =
        com.acuranza.app.data.sms.SmsAccountMatcher.match(candidates(), row.bank, row.accountLast4, row.channel)

    /** All live accounts as picker entries for the review screen. */
    suspend fun pickerAccounts(): List<com.acuranza.app.data.sms.SmsAccountMatcher.CandidateAccount> = candidates()

    private suspend fun resolveAccountFor(row: SmsImportEntity): Long = when (val m = accountHint(row)) {
        is com.acuranza.app.data.sms.SmsAccountMatcher.Match.Exact -> m.account.id
        is com.acuranza.app.data.sms.SmsAccountMatcher.Match.ByBank -> m.account.id
        com.acuranza.app.data.sms.SmsAccountMatcher.Match.None -> {
            val type = if (row.channel == com.acuranza.app.model.TxnChannel.CARD) AccountType.CREDIT_CARD else AccountType.BANK
            db.accounts().insert(
                AccountEntity(
                    name = row.bank + (row.accountLast4?.let { " $it" } ?: ""),
                    type = type,
                    bank = row.bank,
                    last4 = row.accountLast4,
                    autoCreated = true,
                ),
            )
        }
    }

    /**
     * Near-duplicate guard: an SMS-sourced transaction with the same account, amount and type
     * within [windowMillis] is the same real payment read twice (fingerprint keys diverge only
     * for messages with neither a reference number nor an in-body date — inbox and notification
     * capture then disagree on the delivery minute). The check is coarse on purpose: five
     * minutes of slack, and only SMS-sourced rows participate.
     */
    suspend fun hasNearDuplicateSms(accountId: Long, amount: Long, type: TxnType, timestampMillis: Long, windowMillis: Long = 5 * 60_000): Boolean {
        val from = timestampMillis - windowMillis
        val to = timestampMillis + windowMillis
        return db.transactions().between(from, to).any {
            it.source == TxnSource.SMS && it.accountId == accountId && it.amount == amount && it.type == type
        }
    }

    /** Fingerprints already imported; a scan skips those without touching the database again. */
    suspend fun importedFingerprints(): Set<String> = db.smsImports().allFingerprints().toSet()

    suspend fun latestBatches(limit: Int = 10) = db.smsImports().latestBatches(limit)

    suspend fun managementRows(limit: Int = 200) = db.smsImports().managementRows(limit)

    suspend fun totalImports() = db.smsImports().count()

    private fun noteFor(item: ApprovedSmsImport): String? {
        val bits = buildList {
            item.merchant?.takeIf { it.isNotBlank() }?.let { add(it) }
            item.parsed.referenceNo?.let { add("ref $it") }
        }
        return bits.joinToString(" · ").takeIf { it.isNotBlank() }
    }
}
