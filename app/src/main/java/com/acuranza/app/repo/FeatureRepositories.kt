package com.acuranza.app.repo

import android.net.Uri
import com.acuranza.app.data.AttachmentEntity
import com.acuranza.app.data.AttachmentStore
import com.acuranza.app.data.DebtEntity
import com.acuranza.app.data.SumvaloDatabase
import com.acuranza.app.data.LoanEntity
import com.acuranza.app.finance.Loans
import com.acuranza.app.model.Paise
import com.acuranza.app.util.Time
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
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

    /** Home-loan interest and principal paid in the financial year starting April [fyStartYear], for the tax planner. */
    suspend fun homeLoanYear(fyStartYear: Int): Loans.YearSplit =
        db.loans().active().filter { it.kind == HOME }
            .mapNotNull { runCatching { Loans.financialYear(spec(it), fyStartYear) }.getOrNull() }
            .fold(Loans.YearSplit(0, 0)) { a, b -> Loans.YearSplit(a.interest + b.interest, a.principal + b.principal) }

    fun status(loan: LoanEntity, today: LocalDate = Time.today()): LoanStatus {
        val spec = runCatching { spec(loan) }.getOrNull()
        val rows = spec?.let { runCatching { Loans.schedule(it) }.getOrNull() }.orEmpty()
        return LoanStatus(
            loan = loan,
            spec = spec?.takeIf { rows.isNotEmpty() },
            outstanding = spec?.let { runCatching { Loans.outstanding(it, today) }.getOrNull() } ?: loan.principal.coerceAtLeast(0),
            emi = rows.firstOrNull { !it.date.isBefore(today) }?.emi ?: rows.lastOrNull()?.emi ?: (loan.emi ?: 0),
            endDate = rows.lastOrNull()?.date,
            paidMonths = rows.count { !it.date.isAfter(today) },
            totalMonths = rows.size,
            totalInterest = rows.sumOf { it.interest },
        )
    }

    /**
     * Records every loan EMI due on or before [today] as an expense from the first active bank
     * or wallet account, so the user confirms it in Review instead of entering it by hand. The
     * `loan-emi:<loanId>:<day>` fingerprint keeps an occurrence from ever being recorded twice.
     * Returns how many transactions were added.
     */
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
