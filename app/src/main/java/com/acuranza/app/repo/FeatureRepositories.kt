package com.acuranza.app.repo

import android.net.Uri
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Transaction
import androidx.room.Upsert
import androidx.room.withTransaction
import com.acuranza.app.data.AttachmentEntity
import com.acuranza.app.data.AttachmentStore
import com.acuranza.app.data.DebtEntity
import com.acuranza.app.data.PurchaseAttachmentEntity
import com.acuranza.app.data.PurchaseRecordEntity
import com.acuranza.app.data.SplitExpenseEntity
import com.acuranza.app.data.SplitGroupEntity
import com.acuranza.app.data.SplitGroupParticipantEntity
import com.acuranza.app.data.SplitShareEntity
import com.acuranza.app.data.SplitSettlementEntity
import com.acuranza.app.data.SumvaloDatabase
import com.acuranza.app.data.LoanEntity
import com.acuranza.app.finance.Loans
import com.acuranza.app.model.Paise
import com.acuranza.app.util.Time
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject
import java.time.LocalDate

/** Money lent to friends and family, or borrowed from them. */
class DebtRepository(private val db: SumvaloDatabase) {
    fun observe(): Flow<List<DebtEntity>> = db.debts().observeAll()

    suspend fun save(debt: DebtEntity): Long = db.debts().upsert(debt)

    suspend fun delete(debt: DebtEntity) = db.debts().delete(debt)

    /** Records a repayment; the debt closes once it is fully repaid. */
    suspend fun repay(id: Long, amount: Paise) {
        val d = db.debts().byId(id) ?: return
        val repaid = (d.repaid + amount).coerceIn(0, d.amount)
        db.debts().upsert(d.copy(repaid = repaid, closed = repaid >= d.amount))
    }

    companion object {
        const val LENT = "LENT"
        const val BORROWED = "BORROWED"
    }
}

/** A loan with its schedule worked out for today. [spec] is null when the saved details can't make a schedule. */
data class LoanStatus(
    val loan: LoanEntity,
    val spec: Loans.Spec?,
    val outstanding: Paise,
    val emi: Paise,
    val endDate: LocalDate?,
    val paidMonths: Int,
    val totalMonths: Int,
    val totalInterest: Paise,
)

class LoanRepository(private val db: SumvaloDatabase) {
    fun observe(): Flow<List<LoanStatus>> = db.loans().observeActive().map { list -> list.map { status(it) } }

    fun observe(id: Long): Flow<LoanStatus?> = db.loans().observe(id).map { it?.let(::status) }

    suspend fun active(): List<LoanStatus> = db.loans().active().map { status(it) }

    suspend fun save(loan: LoanEntity): Long = db.loans().upsert(loan)

    suspend fun delete(loan: LoanEntity) = db.loans().delete(loan)

    /** Total still owed on every active loan today. */
    suspend fun totalOutstanding(): Paise = active().sumOf { it.outstanding }

    fun status(loan: LoanEntity, today: LocalDate = Time.today()): LoanStatus {
        val spec = runCatching { spec(loan) }.getOrNull()
        val rows = spec?.let { runCatching { Loans.schedule(it) }.getOrNull() }.orEmpty()
        return LoanStatus(
            loan = loan,
            spec = spec?.takeIf { rows.isNotEmpty() },
            outstanding = spec?.let { runCatching { Loans.outstanding(it, today) }.getOrNull() } ?: loan.principal.coerceAtLeast(0),
            emi = rows.firstOrNull { !it.date.isAfter(today) }?.emi ?: rows.lastOrNull()?.emi ?: (loan.emi ?: 0),
            endDate = rows.lastOrNull()?.date,
            paidMonths = rows.count { !it.date.isAfter(today) },
            totalMonths = rows.size,
            totalInterest = rows.sumOf { it.interest },
        )
    }

    /** Records every loan EMI due on or before [today] as an expense from the first active bank or wallet account. */
    suspend fun recordDueEmis(today: LocalDate = Time.today()): Int {
        val accounts = db.accounts().active()
        val payer = accounts.firstOrNull { it.type == com.acuranza.app.model.AccountType.BANK || it.type == com.acuranza.app.model.AccountType.WALLET }
            ?: return 0
        var added = 0
        for (loan in db.loans().active()) {
            val spec = runCatching { spec(loan) }.getOrNull() ?: continue
            val rows = runCatching { Loans.schedule(spec) }.getOrNull() ?: continue
            for (row in rows.filter { !it.date.isAfter(today) }) {
                val fingerprint = "loan-emi:${loan.id}:${row.date.toEpochDay()}"
                if (db.transactions().countFingerprint(fingerprint) > 0) continue
                val id = db.transactions().insertIgnore(
                    com.acuranza.app.data.TransactionEntity(
                        accountId = payer.id,
                        type = com.acuranza.app.model.TxnType.EXPENSE,
                        amount = row.emi,
                        categoryKey = "emi",
                        merchant = loan.name,
                        note = "EMI · ${loan.lender}",
                        timestamp = Time.startOfDay(row.date) + 9L * 60 * 60 * 1000,
                        channel = com.acuranza.app.model.TxnChannel.AUTO_DEBIT,
                        source = com.acuranza.app.model.TxnSource.AUTO,
                        fingerprint = fingerprint,
                        reviewed = false,
                    ),
                )
                if (id > 0) added++
            }
        }
        return added
    }

    companion object {
        const val HOME = "HOME"
        val KINDS = listOf("HOME", "CAR", "PERSONAL", "EDUCATION", "OTHER")

        fun spec(loan: LoanEntity): Loans.Spec = Loans.Spec(
            principal = loan.principal,
            annualRatePct = loan.ratePct,
            tenureMonths = loan.tenureMonths,
            firstEmiDate = LocalDate.ofEpochDay(loan.firstEmiDay),
            emi = loan.emi,
            prepayments = decodePrepayments(loan.prepaymentsJson),
        )

        fun decodePrepayments(json: String): List<Loans.Prepayment> = runCatching {
            val a = JSONArray(json)
            (0 until a.length()).map { i ->
                val o = a.getJSONObject(i)
                Loans.Prepayment(
                    LocalDate.ofEpochDay(o.getLong("day")),
                    o.getLong("amount"),
                    runCatching { Loans.Reduce.valueOf(o.optString("reduce", "TENURE")) }.getOrDefault(Loans.Reduce.TENURE),
                )
            }
        }.getOrDefault(emptyList())

        fun encodePrepayments(list: List<Loans.Prepayment>): String = JSONArray().apply {
            list.sortedBy { it.date }.forEach { put(JSONObject().put("day", it.date.toEpochDay()).put("amount", it.amount).put("reduce", it.reduce.name)) }
        }.toString()
    }
}

/** Receipt files (photos, PDFs) linked to transactions. */
class AttachmentRepository(private val db: SumvaloDatabase, private val store: AttachmentStore) {
    fun observeFor(txnId: Long): Flow<List<AttachmentEntity>> = db.attachments().observeFor(txnId)

    suspend fun add(txnId: Long, uri: Uri, label: String? = null): Long {
        val saved = store.save(uri)
        return runCatching {
            db.attachments().insert(
                AttachmentEntity(
                    transactionId = txnId, fileName = saved.fileName, mimeType = saved.mimeType, sizeBytes = saved.sizeBytes,
                    label = label,
                ),
            )
        }.getOrElse { store.delete(saved.fileName); throw it }
    }

    suspend fun update(a: AttachmentEntity) = db.attachments().update(a)

    suspend fun delete(a: AttachmentEntity) {
        db.attachments().delete(a)
        store.delete(a.fileName)
    }

    /** Deletes the files of [txnId] (used before the transaction itself is deleted). */
    suspend fun deleteAllFor(txnId: Long) {
        db.attachments().observeFor(txnId).first().forEach { delete(it) }
    }

    fun bytes(a: AttachmentEntity): ByteArray = store.read(a.fileName)
}

/** ============================================================================
 * Split Any Expense — Repositories
 * ============================================================================ */

/** Groups and participants. */
class SplitGroupRepository(private val db: SumvaloDatabase) {
    fun observeActive(): Flow<List<SplitGroupEntity>> = db.splitGroups().observeActive()
    fun observeAll(): Flow<List<SplitGroupEntity>> = db.splitGroups().observeAll()

    suspend fun byId(id: Long): SplitGroupEntity? = db.splitGroups().byId(id)
    suspend fun save(group: SplitGroupEntity): Long = db.splitGroups().upsert(group)
    suspend fun archive(id: Long) = db.splitGroups().archive(id)

    fun observeParticipants(groupId: Long): Flow<List<SplitGroupParticipantEntity>> =
        db.splitGroupParticipants().observeForGroup(groupId)
    suspend fun saveParticipant(p: SplitGroupParticipantEntity): Long =
        db.splitGroupParticipants().upsert(p)
    suspend fun deleteParticipant(id: Long) = db.splitGroupParticipants().delete(id)

    /** All groups with their participant counts and outstanding balances. */
    suspend fun groupSummaries(): List<GroupSummary> {
        val groups = db.splitGroups().observeAll().first().filter { !it.archived }
        return groups.map { g ->
            val participants = db.splitGroupParticipants().forGroup(g.id)
            val expenses = db.splitExpenses().observeForGroup(g.id).first()
            val settlements = db.splitSettlements().forGroup(g.id)
            val sharesByExpense = expenses.associate { it.id to db.splitExpenses().sharesFor(it.id) }
            GroupSummary(g, participants, expenses, settlements, sharesByExpense)
        }
    }

    data class GroupSummary(
        val group: SplitGroupEntity,
        val participants: List<SplitGroupParticipantEntity>,
        val expenses: List<SplitExpenseEntity>,
        val settlements: List<SplitSettlementEntity>,
        private val sharesByExpense: Map<Long, List<SplitShareEntity>>,
    ) {
        /** Net balance per participant: positive = owed to them, negative = they owe. */
        fun balances(): Map<Long, Paise> {
            val balances = LinkedHashMap<Long, Paise>()
            participants.forEach { balances[it.id] = 0L }
            for (e in expenses) {
                val shares = sharesByExpense[e.id] ?: emptyList()
                for (s in shares) {
                    val cur = balances[s.participantId] ?: 0L
                    balances[s.participantId] = cur - s.share
                }
                val cur = balances[e.payerId] ?: 0L
                balances[e.payerId] = cur + e.amount
            }
            for (s in settlements) {
                val fromCur = balances[s.fromId] ?: 0L
                balances[s.fromId] = fromCur - s.amount
                val toCur = balances[s.toId] ?: 0L
                balances[s.toId] = toCur + s.amount
            }
            return balances
        }
    }
}

/** Shared expenses and their splits. */
class SplitExpenseRepository(private val db: SumvaloDatabase) {
    fun observeForGroup(groupId: Long): Flow<List<SplitExpenseEntity>> =
        db.splitExpenses().observeForGroup(groupId)

    suspend fun byId(id: Long): SplitExpenseEntity? = db.splitExpenses().byId(id)
    suspend fun save(expense: SplitExpenseEntity): Long = db.splitExpenses().upsert(expense)
    suspend fun delete(id: Long) = db.splitExpenses().delete(id)

    /** Saves an expense with its shares, replacing any existing shares. */
    @JvmName("saveWithShares")
    suspend fun saveWithShares(expense: SplitExpenseEntity, shares: List<SplitShareEntity>): Long =
        db.withTransaction {
            val id = db.splitExpenses().upsert(expense.copy(id = 0))
            db.splitExpenses().replaceShares(id, shares)
            id
        }

    /** Updates an existing expense and replaces its shares. */
    suspend fun updateWithShares(expense: SplitExpenseEntity, shares: List<SplitShareEntity>) =
        db.withTransaction {
            db.splitExpenses().upsert(expense)
            db.splitExpenses().replaceShares(expense.id, shares)
        }

    suspend fun sharesFor(expenseId: Long): List<SplitShareEntity> =
        db.splitExpenses().sharesFor(expenseId)

    fun observeAllSharesForGroup(groupId: Long): Flow<List<SplitShareEntity>> =
        db.splitExpenses().observeAllSharesForGroup(groupId)
}

/** Settlements between participants. */
class SplitSettlementRepository(private val db: SumvaloDatabase) {
    fun observeForGroup(groupId: Long): Flow<List<SplitSettlementEntity>> =
        db.splitSettlements().observeForGroup(groupId)

    suspend fun forGroup(groupId: Long): List<SplitSettlementEntity> =
        db.splitSettlements().forGroup(groupId)

    suspend fun save(settlement: SplitSettlementEntity): Long = db.splitSettlements().upsert(settlement)
    suspend fun delete(id: Long) = db.splitSettlements().delete(id)
    suspend fun reverse(id: Long) = db.splitSettlements().reverse(id)
}

/** ============================================================================
 * Purchase Memory — Repository
 * ============================================================================ */

class PurchaseRepository(
    private val db: SumvaloDatabase,
    private val store: PurchaseAttachmentStore,
) {
    fun observeAll(): Flow<List<PurchaseRecordEntity>> = db.purchaseRecords().observeAll()

    fun search(query: String, category: String): Flow<List<PurchaseRecordEntity>> =
        db.purchaseRecords().search(query, category)

    suspend fun byId(id: Long): PurchaseRecordEntity? = db.purchaseRecords().byId(id)
    suspend fun save(record: PurchaseRecordEntity): Long = db.purchaseRecords().upsert(record)
    suspend fun delete(id: Long) {
        val record = db.purchaseRecords().byId(id) ?: return
        db.purchaseAttachments().deleteAllFor(record.id)
        db.purchaseRecords().delete(id)
        db.purchaseAttachments().observeFor(record.id).first().forEach { a ->
            store.delete(a.fileName)
        }
    }

    fun observeAttachments(purchaseId: Long): Flow<List<PurchaseAttachmentEntity>> =
        db.purchaseAttachments().observeFor(purchaseId)

    suspend fun addAttachment(purchaseId: Long, uri: android.net.Uri, label: String? = null): Long {
        val saved = store.save(uri)
        return db.purchaseAttachments().upsert(
            PurchaseAttachmentEntity(purchaseId = purchaseId, fileName = saved.fileName,
                mimeType = saved.mimeType, sizeBytes = saved.sizeBytes, label = label)
        )
    }

    suspend fun deleteAttachment(id: Long) {
        val a = db.purchaseAttachments().byId(id) ?: return
        db.purchaseAttachments().delete(a.id)
        store.delete(a.fileName)
    }

    /** Reads attachment bytes (encrypted at rest, decrypted on read). */
    suspend fun attachmentBytes(a: PurchaseAttachmentEntity): ByteArray = store.read(a.fileName)
}

/** Encrypted storage for purchase attachments (separate folder from receipt attachments). */
class PurchaseAttachmentStore(private val context: android.content.Context) {
    private val dir: java.io.File get() =
        java.io.File(context.filesDir, "purchase_attachments").apply { mkdirs() }
    private val key: ByteArray by lazy { com.acuranza.app.security.LocalSecrets.key(context, "purchase_attachments") }

    data class Saved(val fileName: String, val mimeType: String, val sizeBytes: Long)

    suspend fun save(uri: android.net.Uri): Saved = withContext(Dispatchers.IO) {
        val type = context.contentResolver.getType(uri) ?: "application/octet-stream"
        val raw = context.contentResolver.openInputStream(uri)?.use { it.readBytes() } ?: error("Can't read the file")
        require(raw.size <= 10_000_000) { "File too large (max 10 MB)" }
        saveBytes(raw, type)
    }

    fun saveBytes(bytes: ByteArray, mimeType: String): Saved {
        val fileName = java.util.UUID.randomUUID().toString()
        java.io.File(dir, fileName).writeBytes(com.acuranza.app.security.LocalSecrets.seal(key, bytes))
        return Saved(fileName, mimeType, bytes.size.toLong())
    }

    fun read(fileName: String): ByteArray =
        com.acuranza.app.security.LocalSecrets.open(key, java.io.File(dir, fileName).readBytes())

    fun delete(fileName: String) {
        java.io.File(dir, fileName).delete()
    }

    private fun java.io.File.safe() = require(!name.contains("/") && !name.contains("..")) { "Bad file name" }
}

/** ============================================================================
 * Split calculation engine — pure, testable, independent of UI
 * ============================================================================ */

/** Result of calculating balances for a group. */
data class ParticipantBalance(
    val participantId: Long,
    val participantName: String,
    /** Positive = owed to this participant, negative = they owe. */
    val netBalance: Paise,
    val totalPaid: Paise,
    val totalOwed: Paise,
)

/** A suggested settlement: from pays to the given amount. */
data class SettlementSuggestion(
    val fromId: Long,
    val fromName: String,
    val toId: Long,
    val toName: String,
    val amount: Paise,
)

/**
 * Pure calculation engine for shared-expense balances and settlement suggestions.
 * All amounts in paise; no floating-point arithmetic.
 */
object SplitCalculator {
    /** Calculates per-participant net balances from expenses and settlements. */
    fun balances(
        participants: List<SplitGroupParticipantEntity>,
        expenses: List<SplitExpenseEntity>,
        sharesByExpense: Map<Long, List<SplitShareEntity>>,
        settlements: List<SplitSettlementEntity>,
    ): List<ParticipantBalance> {
        val paid = LinkedHashMap<Long, Paise>()
        val owed = LinkedHashMap<Long, Paise>()
        participants.forEach { paid[it.id] = 0L; owed[it.id] = 0L }

        for (e in expenses) {
            val cur = paid[e.payerId] ?: 0L
            paid[e.payerId] = cur + e.amount
            val shares = sharesByExpense[e.id] ?: emptyList()
            for (s in shares) {
                val cur = owed[s.participantId] ?: 0L
                owed[s.participantId] = cur + s.share
            }
        }

        for (s in settlements) {
            val fromCur = paid[s.fromId] ?: 0L
            paid[s.fromId] = fromCur - s.amount
            val toCur = owed[s.toId] ?: 0L
            owed[s.toId] = toCur - s.amount
        }

        return participants.map { p ->
            val net = (paid[p.id] ?: 0L) - (owed[p.id] ?: 0L)
            ParticipantBalance(
                participantId = p.id,
                participantName = p.name,
                netBalance = net,
                totalPaid = paid[p.id] ?: 0L,
                totalOwed = owed[p.id] ?: 0L,
            )
        }
    }

    /** Splits an amount equally among n participants, rounding to paise. Largest remainder method. */
    fun equalSplit(total: Paise, n: Int): List<Paise> {
        require(n > 0) { "At least one participant" }
        val base = total / n
        val remainder = total % n
        return List(n) { i -> base + (if (i < remainder) 1 else 0) }
    }
}
