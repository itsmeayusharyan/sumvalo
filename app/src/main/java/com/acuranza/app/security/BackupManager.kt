package com.acuranza.app.security

import android.content.Context
import android.net.Uri
import androidx.room.withTransaction
import com.acuranza.app.data.AccountEntity
import com.acuranza.app.data.AttachmentEntity
import com.acuranza.app.data.AttachmentStore
import com.acuranza.app.data.DebtEntity
import com.acuranza.app.data.LoanEntity
import com.acuranza.app.data.RecurringRuleEntity
import com.acuranza.app.data.SplitEntity
import com.acuranza.app.data.BudgetEntity
import com.acuranza.app.data.CardBillEntity
import com.acuranza.app.data.CategoryEntity
import com.acuranza.app.data.DeletedFingerprintEntity
import com.acuranza.app.data.PurchaseAttachmentEntity
import com.acuranza.app.data.PurchaseRecordEntity
import com.acuranza.app.data.SmsImportBatchEntity
import com.acuranza.app.data.SmsImportEntity
import com.acuranza.app.data.SplitExpenseEntity
import com.acuranza.app.data.SplitGroupEntity
import com.acuranza.app.data.SplitGroupParticipantEntity
import com.acuranza.app.data.SplitSettlementEntity
import com.acuranza.app.data.SplitShareEntity
import com.acuranza.app.sms.SmsConfidence
import com.acuranza.app.data.GoalEntity
import com.acuranza.app.data.SumvaloDatabase
import com.acuranza.app.data.MerchantRuleEntity
import com.acuranza.app.data.NetWorthSnapshotEntity
import com.acuranza.app.data.TransactionEntity
import com.acuranza.app.model.AccountType
import com.acuranza.app.model.TxnChannel
import com.acuranza.app.model.TxnSource
import com.acuranza.app.model.TxnType
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.NonCancellable
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject

data class BackupStats(val accounts: Int, val transactions: Int, val bills: Int)

/** What [BackupManager.restore] restored, plus the file's metadata the caller uses to finish the restore. */
data class RestoreResult(val stats: BackupStats, val meta: BackupMeta)

/** Header fields of a backup file. Files from format 1 have no [repairVersion] (null). */
data class BackupMeta(val format: Int, val exportedAt: Long?, val repairVersion: Int?)

/**
 * Encrypted export/import of everything in the database. Files are written and read through Storage Access
 * Framework URIs (ACTION_CREATE_DOCUMENT / ACTION_OPEN_DOCUMENT), so the user picks where the backup lives.
 */
class BackupManager(private val context: Context, private val db: SumvaloDatabase, private val files: AttachmentStore) {

    /** Restore files are small JSON; anything near this bound is not a Sumvalo backup. */
    private companion object {
        const val MAX_RESTORE_BYTES = 64L * 1024 * 1024
    }

    suspend fun export(uri: Uri, password: CharArray, repairVersion: Int? = null): BackupStats = withContext(Dispatchers.IO) {
        val (bytes, stats) = encrypted(password, repairVersion)
        context.contentResolver.openOutputStream(uri, "wt")?.use { it.write(bytes) } ?: error("Can't write the backup file")
        stats
    }

    /** The whole backup file, encrypted with [password], and what it holds. */
    suspend fun encrypted(password: CharArray, repairVersion: Int? = null): Pair<ByteArray, BackupStats> = withContext(Dispatchers.IO) {
        val data = snapshot()
        val json = BackupJson.encode(data, repairVersion = repairVersion)
        BackupCrypto.encrypt(json.toByteArray(Charsets.UTF_8), password) to statsOf(data)
    }

    /**
     * Replaces all data with the backup's contents. Throws [BackupCrypto.WrongPasswordException] on a bad password.
     *
     * The file is fully read, decrypted and parsed before anything is touched. The delete and the re-insert then
     * run in one transaction that is not cancelled with the caller, so leaving the screen, rotating the phone or an
     * SMS arriving mid-restore can't leave the database empty or half-restored.
     */
    suspend fun restore(uri: Uri, password: CharArray): RestoreResult = withContext(Dispatchers.IO) {
        val bytes = context.contentResolver.openInputStream(uri)?.use { stream ->
            // Bound the read before allocating: a mis-picked huge file must not OOM the app mid-restore.
            val size = context.contentResolver.query(uri, arrayOf(android.provider.OpenableColumns.SIZE), null, null, null)
                ?.use { c -> if (c.moveToFirst() && !c.isNull(0)) c.getLong(0) else -1L } ?: -1L
            if (size in 0 until MAX_RESTORE_BYTES) {
                stream.readBytes()
            } else {
                val capped = stream.readNBytes(MAX_RESTORE_BYTES.toInt() + 1)
                check(capped.size <= MAX_RESTORE_BYTES) { "That backup file is too large" }
                capped
            }
        } ?: error("Can't read the backup file")
        val json = BackupCrypto.decrypt(bytes, password).toString(Charsets.UTF_8)
        val data = BackupJson.decode(json)
        val meta = BackupJson.meta(json)
        withContext(NonCancellable) {
            db.withTransaction {
                val r = db.restore()
                r.clearSmsImports(); r.clearSmsBatches()
                r.clearSplitSettlements(); r.clearSplitShares(); r.clearSplitExpenses()
                r.clearSplitGroupParticipants(); r.clearSplitGroups()
                r.clearPurchaseAttachments(); r.clearPurchaseRecords()
                r.clearAttachments(); r.clearSplits(); r.clearRecurring(); r.clearDebts(); r.clearLoans()
                r.clearTransactions(); r.clearBills()
                r.clearAccounts(); r.clearCategories(); r.clearBudgets()
                r.clearGoals(); r.clearRules(); r.clearNetWorth(); r.clearTombstones()
                r.insertCategories(data.categories)
                r.insertAccounts(data.accounts)
                r.insertTransactions(data.transactions)
                r.insertBills(data.bills)
                r.insertBudgets(data.budgets)
                r.insertGoals(data.goals)
                r.insertRules(data.rules)
                r.insertNetWorth(data.netWorth)
                r.insertTombstones(data.tombstones)
                r.insertSplits(data.splits)
                r.insertDebts(data.debts)
                r.insertRecurring(data.recurring)
                r.insertLoans(data.loans)
                r.insertAttachments(data.attachments)
                r.insertSplitGroups(data.splitGroups)
                r.insertSplitGroupParticipants(data.splitParticipants)
                r.insertSplitExpenses(data.splitExpenses)
                r.insertSplitShares(data.splitShares)
                r.insertSplitSettlements(data.splitSettlements)
                r.insertPurchaseRecords(data.purchaseRecords)
                r.insertPurchaseAttachments(data.purchaseAttachments)
                r.insertSmsBatches(data.smsBatches)
                r.insertSmsImports(data.smsImports)
            }
            // Files follow the committed rows: the old ones go, the backup's are written back encrypted.
            files.deleteAll()
            data.attachmentFiles.forEach { (name, bytes) ->
                val mime = data.attachments.firstOrNull { it.fileName == name }?.mimeType ?: AttachmentStore.JPEG
                runCatching { files.saveBytes(bytes, mime, name) }
            }
        }
        RestoreResult(statsOf(data), meta)
    }

    /** Reads every table in one transaction, so an SMS imported mid-export can't produce an inconsistent file. */
    private suspend fun snapshot(): BackupData = db.withTransaction {
        val r = db.restore()
        BackupData(
            accounts = r.accounts(),
            categories = r.categories(),
            transactions = r.transactions(),
            bills = r.bills(),
            budgets = r.budgets(),
            goals = r.goals(),
            rules = r.rules(),
            netWorth = r.netWorth(),
            tombstones = r.tombstones(),
            splits = r.splits(),
            debts = r.debts(),
            recurring = r.recurring(),
            loans = r.loans(),
            attachments = r.attachments(),
            splitGroups = r.splitGroups(),
            splitParticipants = r.splitGroupParticipants(),
            splitExpenses = r.splitExpenses(),
            splitShares = r.splitShares(),
            splitSettlements = r.splitSettlements(),
            purchaseRecords = r.purchaseRecords(),
            purchaseAttachments = r.purchaseAttachments(),
            smsBatches = r.smsBatches(),
            smsImports = r.smsImports(),
        ).let { d -> d.copy(attachmentFiles = d.attachments.mapNotNull { a -> runCatching { a.fileName to files.read(a.fileName) }.getOrNull() }.toMap()) }
    }

    private fun statsOf(d: BackupData) = BackupStats(d.accounts.size, d.transactions.size, d.bills.size)
}

data class BackupData(
    val accounts: List<AccountEntity>,
    val categories: List<CategoryEntity>,
    val transactions: List<TransactionEntity>,
    val bills: List<CardBillEntity>,
    val budgets: List<BudgetEntity>,
    val goals: List<GoalEntity>,
    val rules: List<MerchantRuleEntity>,
    val netWorth: List<NetWorthSnapshotEntity>,
    val tombstones: List<DeletedFingerprintEntity> = emptyList(),
    val splits: List<SplitEntity> = emptyList(),
    val debts: List<DebtEntity> = emptyList(),
    val recurring: List<RecurringRuleEntity> = emptyList(),
    val loans: List<LoanEntity> = emptyList(),
    val attachments: List<AttachmentEntity> = emptyList(),
    /** Decrypted receipt files by [AttachmentEntity.fileName]. */
    val attachmentFiles: Map<String, ByteArray> = emptyMap(),
    // Format 4: the v13/v14 feature tables. Older backups simply omit them (they restore as empty,
    // which matches what those backups contain). Purchase-attachment FILES are not included — the
    // rows are, and on the same device their files are still in place.
    val splitGroups: List<SplitGroupEntity> = emptyList(),
    val splitParticipants: List<SplitGroupParticipantEntity> = emptyList(),
    val splitExpenses: List<SplitExpenseEntity> = emptyList(),
    val splitShares: List<SplitShareEntity> = emptyList(),
    val splitSettlements: List<SplitSettlementEntity> = emptyList(),
    val purchaseRecords: List<PurchaseRecordEntity> = emptyList(),
    val purchaseAttachments: List<PurchaseAttachmentEntity> = emptyList(),
    val smsBatches: List<SmsImportBatchEntity> = emptyList(),
    val smsImports: List<SmsImportEntity> = emptyList(),
)

/** JSON form of [BackupData]; nullable fields are omitted when null. */
object BackupJson {
    /** A double from an untrusted backup file, coerced into [min, max]; NaN/absent → [default].
     *  Restored values must meet the same bounds the UI enforces (a NaN rate once made a goal
     *  plan report "on track" with zero required saving). */
    private fun org.json.JSONObject.safeDouble(key: String, default: Double, min: Double, max: Double): Double {
        if (!has(key) || isNull(key)) return default
        val v = optDouble(key, default)
        return if (v.isNaN()) default else v.coerceIn(min, max)
    }


    /**
     * 1: original format. 2: adds goal loans, per-account savings interest settings, deleted-import fingerprints
     * and the data-repair version. 3: adds tags, split parts, budget rollover, money lent and borrowed, recurring
     * rules, loans and receipts (with their files). 4: adds the v13/v14 tables (split groups with participants,
     * expenses, shares and settlements, purchase records with attachment rows, and SMS import batches/imports) —
     * restores of older files still work; those tables restore empty. Missing fields take the migrations' defaults.
     */
    const val FORMAT_VERSION = 4

    fun meta(json: String): BackupMeta {
        val o = JSONObject(json)
        return BackupMeta(
            format = o.optInt("format", 0),
            exportedAt = if (o.has("exportedAt")) o.optLong("exportedAt") else null,
            repairVersion = if (o.has("repairVersion")) o.optInt("repairVersion") else null,
        )
    }

    fun encode(d: BackupData, exportedAt: Long = System.currentTimeMillis(), repairVersion: Int? = null): String = JSONObject()
        .put("format", FORMAT_VERSION)
        .put("exportedAt", exportedAt)
        .putOpt("repairVersion", repairVersion)
        .put("accounts", arr(d.accounts) { a ->
            JSONObject().put("id", a.id).put("name", a.name).put("type", a.type.name).put("bank", a.bank)
                .putOpt("last4", a.last4).put("openingBalance", a.openingBalance).put("openingDate", a.openingDate)
                .putOpt("reportedBalance", a.reportedBalance).putOpt("reportedAt", a.reportedAt)
                .putOpt("creditLimit", a.creditLimit).putOpt("statementDay", a.statementDay).putOpt("dueDay", a.dueDay)
                .putOpt("colorArgb", a.colorArgb).put("archived", a.archived).put("autoCreated", a.autoCreated)
                .putOpt("interestRatePct", a.interestRatePct).put("interestCreditFrequency", a.interestCreditFrequency)
                .putOpt("otherDigits", a.otherDigits)
        })
        .put("categories", arr(d.categories) { c ->
            JSONObject().put("id", c.id).put("key", c.key).put("name", c.name).put("type", c.type.name)
                .put("icon", c.icon).put("colorArgb", c.colorArgb).put("isDefault", c.isDefault)
        })
        .put("transactions", arr(d.transactions) { t ->
            JSONObject().put("id", t.id).put("accountId", t.accountId).putOpt("toAccountId", t.toAccountId)
                .put("type", t.type.name).put("amount", t.amount).put("categoryKey", t.categoryKey)
                .putOpt("merchant", t.merchant).putOpt("note", t.note).put("timestamp", t.timestamp)
                .put("channel", t.channel.name).put("source", t.source.name).putOpt("fingerprint", t.fingerprint)
                .putOpt("referenceNo", t.referenceNo).put("reviewed", t.reviewed).put("excludeFromStats", t.excludeFromStats).put("suspicious", t.suspicious)
                .put("tags", t.tags)
        })
        .put("bills", arr(d.bills) { b ->
            JSONObject().put("id", b.id).put("accountId", b.accountId).put("totalDue", b.totalDue)
                .putOpt("minimumDue", b.minimumDue).putOpt("dueDate", b.dueDate).putOpt("statementDate", b.statementDate)
                .put("source", b.source.name).put("fingerprint", b.fingerprint).put("paid", b.paid)
                .putOpt("paidAt", b.paidAt).put("createdAt", b.createdAt)
        })
        .put("budgets", arr(d.budgets) { b ->
            JSONObject().put("id", b.id).putOpt("categoryKey", b.categoryKey).put("monthlyLimit", b.monthlyLimit).put("rollover", b.rollover)
        })
        .put("goals", arr(d.goals) { g ->
            JSONObject().put("id", g.id).put("name", g.name).put("target", g.target).put("saved", g.saved)
                .put("deadline", g.deadline).put("icon", g.icon).put("expectedReturnPct", g.expectedReturnPct)
                .put("createdAt", g.createdAt).put("loanAmount", g.loanAmount).put("loanRatePct", g.loanRatePct)
                .put("loanTenureMonths", g.loanTenureMonths)
        })
        .put("rules", arr(d.rules) { r -> JSONObject().put("id", r.id).put("merchant", r.merchant).put("categoryKey", r.categoryKey) })
        .put("netWorth", arr(d.netWorth) { n ->
            JSONObject().put("epochDay", n.epochDay).put("liquid", n.liquid).put("cardDebt", n.cardDebt).put("total", n.total)
        })
        .put("deletedFingerprints", arr(d.tombstones) { t -> JSONObject().put("fingerprint", t.fingerprint).put("deletedAt", t.deletedAt) })
        .put("splits", arr(d.splits) { p ->
            JSONObject().put("id", p.id).put("transactionId", p.transactionId).put("categoryKey", p.categoryKey)
                .put("amount", p.amount).putOpt("note", p.note)
        })
        .put("debts", arr(d.debts) { x ->
            JSONObject().put("id", x.id).put("person", x.person).put("direction", x.direction).put("amount", x.amount)
                .put("repaid", x.repaid).put("date", x.date).putOpt("dueDate", x.dueDate).putOpt("note", x.note)
                .put("closed", x.closed).put("createdAt", x.createdAt)
        })
        .put("recurring", arr(d.recurring) { x ->
            JSONObject().put("id", x.id).put("name", x.name).put("type", x.type.name).put("amount", x.amount)
                .put("categoryKey", x.categoryKey).put("accountId", x.accountId).putOpt("toAccountId", x.toAccountId)
                .put("cadence", x.cadence).put("anchorDay", x.anchorDay).put("nextDay", x.nextDay).put("active", x.active)
                .put("autoAdd", x.autoAdd).putOpt("note", x.note).put("createdAt", x.createdAt)
        })
        .put("loans", arr(d.loans) { x ->
            JSONObject().put("id", x.id).put("name", x.name).put("lender", x.lender).put("kind", x.kind)
                .put("principal", x.principal).put("ratePct", x.ratePct).put("tenureMonths", x.tenureMonths)
                .put("firstEmiDay", x.firstEmiDay).putOpt("emi", x.emi).put("prepaymentsJson", x.prepaymentsJson)
                .put("selfOccupied", x.selfOccupied).put("archived", x.archived).put("createdAt", x.createdAt)
        })
        .put("attachments", arr(d.attachments) { x ->
            JSONObject().put("id", x.id).put("transactionId", x.transactionId).put("fileName", x.fileName)
                .put("mimeType", x.mimeType).put("sizeBytes", x.sizeBytes).putOpt("label", x.label)
                .put("createdAt", x.createdAt)
        })
        .put("attachmentFiles", JSONObject().apply {
            d.attachmentFiles.forEach { (name, bytes) -> put(name, java.util.Base64.getEncoder().encodeToString(bytes)) }
        })
        .put("splitGroups", arr(d.splitGroups) { g ->
            JSONObject().put("id", g.id).put("name", g.name).put("archived", g.archived).put("createdAt", g.createdAt)
        })
        .put("splitParticipants", arr(d.splitParticipants) { p ->
            JSONObject().put("id", p.id).put("groupId", p.groupId).put("name", p.name).put("createdAt", p.createdAt)
        })
        .put("splitExpenses", arr(d.splitExpenses) { e ->
            JSONObject().put("id", e.id).put("groupId", e.groupId).put("description", e.description).put("amount", e.amount)
                .put("date", e.date).put("payerId", e.payerId).putOpt("categoryKey", e.categoryKey).putOpt("note", e.note)
                .putOpt("linkedTransactionId", e.linkedTransactionId).put("createdAt", e.createdAt)
        })
        .put("splitShares", arr(d.splitShares) { s ->
            JSONObject().put("id", s.id).put("expenseId", s.expenseId).put("participantId", s.participantId).put("share", s.share)
        })
        .put("splitSettlements", arr(d.splitSettlements) { s ->
            JSONObject().put("id", s.id).put("groupId", s.groupId).put("fromId", s.fromId).put("toId", s.toId)
                .put("amount", s.amount).put("date", s.date).putOpt("note", s.note).put("reversed", s.reversed).put("createdAt", s.createdAt)
        })
        .put("purchaseRecords", arr(d.purchaseRecords) { r ->
            JSONObject().put("id", r.id).put("itemName", r.itemName).putOpt("merchant", r.merchant).put("purchaseDate", r.purchaseDate)
                .putOpt("amount", r.amount).putOpt("category", r.category).putOpt("notes", r.notes).put("tags", r.tags)
                .putOpt("warrantyStart", r.warrantyStart).putOpt("warrantyUntil", r.warrantyUntil).putOpt("returnUntil", r.returnUntil)
                .putOpt("invoiceNo", r.invoiceNo).putOpt("serialNo", r.serialNo).putOpt("linkedTransactionId", r.linkedTransactionId)
                .put("remindersEnabled", r.remindersEnabled).put("createdAt", r.createdAt)
        })
        .put("purchaseAttachments", arr(d.purchaseAttachments) { a ->
            JSONObject().put("id", a.id).put("purchaseId", a.purchaseId).put("fileName", a.fileName)
                .put("mimeType", a.mimeType).put("sizeBytes", a.sizeBytes).putOpt("label", a.label).put("createdAt", a.createdAt)
        })
        .put("smsBatches", arr(d.smsBatches) { b ->
            JSONObject().put("id", b.id).put("importedAt", b.importedAt).put("txnCount", b.txnCount)
                .put("label", b.label).put("undone", b.undone)
        })
        .put("smsImports", arr(d.smsImports) { m ->
            JSONObject().put("id", m.id).put("batchId", m.batchId).putOpt("transactionId", m.transactionId)
                .put("fingerprint", m.fingerprint).put("sender", m.sender).put("bank", m.bank)
                .putOpt("accountLast4", m.accountLast4).put("type", m.type.name).put("amount", m.amount)
                .putOpt("merchant", m.merchant).put("timestamp", m.timestamp).put("channel", m.channel.name)
                .put("confidence", m.confidence.name).putOpt("suggestedCategory", m.suggestedCategory)
        })
        .toString()

    fun decode(json: String): BackupData {
        val o = JSONObject(json)
        require(o.optInt("format", 0) in 1..FORMAT_VERSION) { "Unsupported backup format" }
        return BackupData(
            accounts = list(o, "accounts") { a ->
                AccountEntity(
                    id = a.getLong("id"), name = a.getString("name"), type = AccountType.valueOf(a.getString("type")),
                    bank = a.getString("bank"), last4 = a.str("last4"), openingBalance = a.getLong("openingBalance"),
                    openingDate = a.getLong("openingDate"), reportedBalance = a.long("reportedBalance"), reportedAt = a.long("reportedAt"),
                    creditLimit = a.long("creditLimit"), statementDay = a.int("statementDay"), dueDay = a.int("dueDay"),
                    colorArgb = a.long("colorArgb"), archived = a.optBoolean("archived"), autoCreated = a.optBoolean("autoCreated"),
                    interestRatePct = a.double("interestRatePct"),
                    interestCreditFrequency = a.optString("interestCreditFrequency", "QUARTERLY"),
                    otherDigits = a.str("otherDigits"),
                )
            },
            categories = list(o, "categories") { c ->
                CategoryEntity(
                    id = c.getLong("id"), key = c.getString("key"), name = c.getString("name"), type = TxnType.valueOf(c.getString("type")),
                    icon = c.getString("icon"), colorArgb = c.getLong("colorArgb"), isDefault = c.optBoolean("isDefault", true),
                )
            },
            transactions = list(o, "transactions") { t ->
                TransactionEntity(
                    id = t.getLong("id"), accountId = t.getLong("accountId"), toAccountId = t.long("toAccountId"),
                    type = TxnType.valueOf(t.getString("type")), amount = t.getLong("amount"), categoryKey = t.getString("categoryKey"),
                    merchant = t.str("merchant"), note = t.str("note"), timestamp = t.getLong("timestamp"),
                    channel = TxnChannel.valueOf(t.optString("channel", TxnChannel.OTHER.name)),
                    source = TxnSource.valueOf(t.optString("source", TxnSource.MANUAL.name)), fingerprint = t.str("fingerprint"),
                    referenceNo = t.str("referenceNo"), reviewed = t.optBoolean("reviewed", true),
                    excludeFromStats = t.optBoolean("excludeFromStats"),
                    suspicious = t.optBoolean("suspicious"),
                    tags = t.optString("tags", ""),
                )
            },
            bills = list(o, "bills") { b ->
                CardBillEntity(
                    id = b.getLong("id"), accountId = b.getLong("accountId"), totalDue = b.getLong("totalDue"),
                    minimumDue = b.long("minimumDue"), dueDate = b.long("dueDate"), statementDate = b.long("statementDate"),
                    source = TxnSource.valueOf(b.getString("source")), fingerprint = b.getString("fingerprint"),
                    paid = b.optBoolean("paid"), paidAt = b.long("paidAt"), createdAt = b.optLong("createdAt"),
                )
            },
            budgets = list(o, "budgets") { b ->
                BudgetEntity(b.getLong("id"), b.str("categoryKey"), b.getLong("monthlyLimit"), rollover = b.optBoolean("rollover"))
            },
            goals = list(o, "goals") { g ->
                GoalEntity(
                    id = g.getLong("id"), name = g.getString("name"),
                    target = g.getLong("target").coerceAtLeast(0), saved = g.getLong("saved").coerceAtLeast(0),
                    deadline = g.getLong("deadline"), icon = g.optString("icon", "flag"),
                    expectedReturnPct = g.safeDouble("expectedReturnPct", 7.0, 0.0, 30.0), createdAt = g.optLong("createdAt"),
                    loanAmount = g.optLong("loanAmount", 0).coerceAtLeast(0), loanRatePct = g.safeDouble("loanRatePct", 9.0, 0.0, 100.0),
                    loanTenureMonths = g.optInt("loanTenureMonths", 36).coerceIn(1, 600),
                )
            },
            rules = list(o, "rules") { r -> MerchantRuleEntity(r.getLong("id"), r.getString("merchant"), r.getString("categoryKey")) },
            netWorth = list(o, "netWorth") { n ->
                // Old Sumvalo backups carry an extra "investments" column; it is simply dropped here.
                NetWorthSnapshotEntity(n.getLong("epochDay"), n.getLong("liquid"), n.getLong("cardDebt"), n.getLong("total"))
            },
            tombstones = list(o, "deletedFingerprints") { t ->
                DeletedFingerprintEntity(t.getString("fingerprint"), t.optLong("deletedAt"))
            },
            splits = list(o, "splits") { p ->
                SplitEntity(p.getLong("id"), p.getLong("transactionId"), p.getString("categoryKey"), p.getLong("amount"), p.str("note"))
            },
            debts = list(o, "debts") { x ->
                DebtEntity(
                    id = x.getLong("id"), person = x.getString("person"), direction = x.getString("direction"), amount = x.getLong("amount"),
                    repaid = x.optLong("repaid"), date = x.getLong("date"), dueDate = x.long("dueDate"), note = x.str("note"),
                    closed = x.optBoolean("closed"), createdAt = x.optLong("createdAt"),
                )
            },
            recurring = list(o, "recurring") { x ->
                RecurringRuleEntity(
                    id = x.getLong("id"), name = x.getString("name"), type = TxnType.valueOf(x.getString("type")), amount = x.getLong("amount"),
                    categoryKey = x.getString("categoryKey"), accountId = x.getLong("accountId"), toAccountId = x.long("toAccountId"),
                    cadence = x.getString("cadence"), anchorDay = x.getLong("anchorDay"), nextDay = x.getLong("nextDay"),
                    active = x.optBoolean("active", true), autoAdd = x.optBoolean("autoAdd", true), note = x.str("note"),
                    createdAt = x.optLong("createdAt"),
                )
            },
            loans = list(o, "loans") { x ->
                LoanEntity(
                    id = x.getLong("id"), name = x.getString("name"), lender = x.optString("lender", ""), kind = x.optString("kind", "OTHER"),
                    principal = x.getLong("principal").coerceAtLeast(0), ratePct = x.safeDouble("ratePct", 9.0, 0.0, 100.0),
                    tenureMonths = x.getInt("tenureMonths").coerceIn(1, 600),
                    firstEmiDay = x.getLong("firstEmiDay"), emi = (x.long("emi") ?: 0).coerceAtLeast(0), prepaymentsJson = x.optString("prepaymentsJson", "[]"),
                    selfOccupied = x.optBoolean("selfOccupied", true), archived = x.optBoolean("archived"), createdAt = x.optLong("createdAt"),
                )
            },
            splitGroups = list(o, "splitGroups") { g ->
                SplitGroupEntity(id = g.getLong("id"), name = g.getString("name"), archived = g.optBoolean("archived"), createdAt = g.optLong("createdAt"))
            },
            splitParticipants = list(o, "splitParticipants") { p ->
                SplitGroupParticipantEntity(id = p.getLong("id"), groupId = p.getLong("groupId"), name = p.getString("name"), createdAt = p.optLong("createdAt"))
            },
            splitExpenses = list(o, "splitExpenses") { e ->
                SplitExpenseEntity(
                    id = e.getLong("id"), groupId = e.getLong("groupId"), description = e.getString("description"),
                    amount = e.getLong("amount"), date = e.getLong("date"), payerId = e.getLong("payerId"),
                    categoryKey = e.str("categoryKey"), note = e.str("note"), linkedTransactionId = e.long("linkedTransactionId"),
                    createdAt = e.optLong("createdAt"),
                )
            },
            splitShares = list(o, "splitShares") { s ->
                SplitShareEntity(id = s.getLong("id"), expenseId = s.getLong("expenseId"), participantId = s.getLong("participantId"), share = s.getLong("share"))
            },
            splitSettlements = list(o, "splitSettlements") { s ->
                SplitSettlementEntity(
                    id = s.getLong("id"), groupId = s.getLong("groupId"), fromId = s.getLong("fromId"), toId = s.getLong("toId"),
                    amount = s.getLong("amount"), date = s.getLong("date"), note = s.str("note"),
                    reversed = s.optBoolean("reversed"), createdAt = s.optLong("createdAt"),
                )
            },
            purchaseRecords = list(o, "purchaseRecords") { r ->
                PurchaseRecordEntity(
                    id = r.getLong("id"), itemName = r.getString("itemName"), merchant = r.str("merchant"),
                    purchaseDate = r.getLong("purchaseDate"), amount = r.long("amount"), category = r.str("category"),
                    notes = r.str("notes"), tags = r.optString("tags", ""),
                    warrantyStart = r.long("warrantyStart"), warrantyUntil = r.long("warrantyUntil"), returnUntil = r.long("returnUntil"),
                    invoiceNo = r.str("invoiceNo"), serialNo = r.str("serialNo"), linkedTransactionId = r.long("linkedTransactionId"),
                    remindersEnabled = r.optBoolean("remindersEnabled", true), createdAt = r.optLong("createdAt"),
                )
            },
            purchaseAttachments = list(o, "purchaseAttachments") { a ->
                PurchaseAttachmentEntity(
                    id = a.getLong("id"), purchaseId = a.getLong("purchaseId"), fileName = a.getString("fileName"),
                    mimeType = a.getString("mimeType"), sizeBytes = a.getLong("sizeBytes"), label = a.str("label"),
                    createdAt = a.optLong("createdAt"),
                )
            },
            smsBatches = list(o, "smsBatches") { b ->
                SmsImportBatchEntity(
                    id = b.getLong("id"), importedAt = b.getLong("importedAt"), txnCount = b.getInt("txnCount"),
                    label = b.getString("label"), undone = b.optBoolean("undone"),
                )
            },
            smsImports = list(o, "smsImports") { m ->
                SmsImportEntity(
                    id = m.getLong("id"), batchId = m.getLong("batchId"), transactionId = m.long("transactionId"),
                    fingerprint = m.getString("fingerprint"), sender = m.getString("sender"), bank = m.getString("bank"),
                    accountLast4 = m.str("accountLast4"), type = TxnType.valueOf(m.getString("type")), amount = m.getLong("amount"),
                    merchant = m.str("merchant"), timestamp = m.getLong("timestamp"), channel = TxnChannel.valueOf(m.getString("channel")),
                    confidence = SmsConfidence.valueOf(m.getString("confidence")), suggestedCategory = m.str("suggestedCategory"),
                )
            },
            attachments = list(o, "attachments") { x ->
                AttachmentEntity(
                    id = x.getLong("id"), transactionId = x.getLong("transactionId"), fileName = x.getString("fileName"),
                    mimeType = x.getString("mimeType"), sizeBytes = x.optLong("sizeBytes"), label = x.str("label"),
                    createdAt = x.optLong("createdAt"),
                )
            },
            attachmentFiles = o.optJSONObject("attachmentFiles")?.let { f ->
                f.keys().asSequence().associateWith { java.util.Base64.getDecoder().decode(f.getString(it)) }
            }.orEmpty(),
        )
    }

    private fun <T> arr(items: List<T>, map: (T) -> JSONObject) = JSONArray().apply { items.forEach { put(map(it)) } }

    private fun <T> list(o: JSONObject, key: String, map: (JSONObject) -> T): List<T> {
        val a = o.optJSONArray(key) ?: return emptyList()
        return (0 until a.length()).map { map(a.getJSONObject(it)) }
    }

    private fun JSONObject.str(key: String): String? = if (has(key) && !isNull(key)) getString(key) else null
    private fun JSONObject.long(key: String): Long? = if (has(key) && !isNull(key)) getLong(key) else null
    private fun JSONObject.int(key: String): Int? = if (has(key) && !isNull(key)) getInt(key) else null
    private fun JSONObject.double(key: String): Double? = if (has(key) && !isNull(key)) getDouble(key) else null
}
