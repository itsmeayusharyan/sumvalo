package com.acuranza.app.data

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey
import com.acuranza.app.model.AccountType
import com.acuranza.app.model.Paise
import com.acuranza.app.model.TxnChannel
import com.acuranza.app.model.TxnSource
import com.acuranza.app.model.TxnType

/**
 * A bank account, credit card, cash wallet or e-wallet.
 *
 * For bank accounts the balance is `openingBalance` plus every transaction after `openingDate`;
 * `reportedBalance` is the latest balance a bank SMS told us, used to reconcile.
 */
@Entity(tableName = "accounts")
data class AccountEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val name: String,
    val type: AccountType,
    val bank: String,
    val last4: String? = null,
    val openingBalance: Paise = 0,
    val openingDate: Long = System.currentTimeMillis(),
    val reportedBalance: Paise? = null,
    val reportedAt: Long? = null,
    val creditLimit: Paise? = null,
    val statementDay: Int? = null,
    val dueDay: Int? = null,
    val colorArgb: Long? = null,
    val archived: Boolean = false,
    /** Created automatically from an SMS or e-mail; the user should confirm its name and opening balance. */
    val autoCreated: Boolean = false,
    /** Savings interest rate the user set (% a year); null uses Sumvalo's built-in rate for the bank. */
    val interestRatePct: Double? = null,
    /** How often the bank credits savings interest: QUARTERLY (default) or MONTHLY. */
    @androidx.room.ColumnInfo(defaultValue = "QUARTERLY")
    val interestCreditFrequency: String = "QUARTERLY",
    /**
     * Other digits the bank's messages use for this account, comma-separated: a debit card's last four, or the
     * digits of an account found in SMS that the user merged into this one.
     */
    val otherDigits: String? = null,
) {
    private val otherDigitList: List<String> get() = otherDigits.orEmpty().split(',').map { it.trim() }.filter { it.isNotEmpty() }

    /** [last4] and [otherDigits]. */
    val allDigits: List<String> get() = listOfNotNull(last4) + otherDigitList

    /** This account remembering [digits] as another of its numbers (unchanged if it already knows them). */
    fun plusOtherDigits(digits: String): AccountEntity =
        if (digits.isBlank() || digits in allDigits) this else copy(otherDigits = (otherDigitList + digits).joinToString(","))
}

@Entity(tableName = "categories", indices = [Index(value = ["key"], unique = true)])
data class CategoryEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val key: String,
    val name: String,
    val type: TxnType,
    val icon: String,
    val colorArgb: Long,
    val isDefault: Boolean = true,
)

@Entity(
    tableName = "transactions",
    foreignKeys = [
        ForeignKey(entity = AccountEntity::class, parentColumns = ["id"], childColumns = ["accountId"], onDelete = ForeignKey.CASCADE),
    ],
    indices = [
        Index("accountId"), Index("timestamp"), Index("categoryKey"),
        Index(value = ["fingerprint"], unique = true),
        // v9: balances look up both sides of a transfer, and every SMS checks reference numbers.
        Index("toAccountId"), Index("referenceNo"),
    ],
)
data class TransactionEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val accountId: Long,
    /** Destination account for transfers (e.g. paying a credit card from a bank account). */
    val toAccountId: Long? = null,
    val type: TxnType,
    val amount: Paise,
    val categoryKey: String,
    val merchant: String? = null,
    val note: String? = null,
    val timestamp: Long,
    val channel: TxnChannel = TxnChannel.OTHER,
    val source: TxnSource = TxnSource.MANUAL,
    /** Dedupe key for SMS/email imports; null for manual entries. */
    val fingerprint: String? = null,
    val referenceNo: String? = null,
    /** Auto-imported transactions start unreviewed so the user can confirm or fix them. */
    val reviewed: Boolean = true,
    val excludeFromStats: Boolean = false,
    /**
     * An imported amount that looks implausible (for example a reference number read as an amount).
     * Suspicious rows don't move balances or count in stats until the user confirms them in Review.
     */
    @androidx.room.ColumnInfo(defaultValue = "0")
    val suspicious: Boolean = false,
    /** User tags such as "Goa trip" or "Reimbursable", stored as `|tag|tag|` (see [Tags]); empty when none. */
    @androidx.room.ColumnInfo(defaultValue = "")
    val tags: String = "",
)

@Entity(
    tableName = "card_bills",
    foreignKeys = [
        ForeignKey(entity = AccountEntity::class, parentColumns = ["id"], childColumns = ["accountId"], onDelete = ForeignKey.CASCADE),
    ],
    indices = [Index("accountId"), Index(value = ["fingerprint"], unique = true)],
)
data class CardBillEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val accountId: Long,
    val totalDue: Paise,
    val minimumDue: Paise? = null,
    /** Epoch day (LocalDate.toEpochDay). */
    val dueDate: Long?,
    val statementDate: Long? = null,
    val source: TxnSource,
    val fingerprint: String,
    val paid: Boolean = false,
    val paidAt: Long? = null,
    val createdAt: Long = System.currentTimeMillis(),
)

/** A monthly budget; `categoryKey == null` is the overall budget. */
@Entity(tableName = "budgets", indices = [Index(value = ["categoryKey"], unique = true)])
data class BudgetEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val categoryKey: String?,
    val monthlyLimit: Paise,
    /** Unspent budget carries into the next month (and overspending reduces it). */
    @androidx.room.ColumnInfo(defaultValue = "0")
    val rollover: Boolean = false,
)

@Entity(tableName = "goals")
data class GoalEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val name: String,
    val target: Paise,
    val saved: Paise = 0,
    /** Epoch day. */
    val deadline: Long,
    val icon: String = "flag",
    val expectedReturnPct: Double = 7.0,
    val createdAt: Long = System.currentTimeMillis(),
    /** Loan the user is willing to take at purchase; 0 means no loan. */
    val loanAmount: Paise = 0,
    val loanRatePct: Double = 9.0,
    val loanTenureMonths: Int = 36,
)

/** Learned categorisation: when the user recategorises a merchant, future imports follow. */
@Entity(tableName = "merchant_rules", indices = [Index(value = ["merchant"], unique = true)])
data class MerchantRuleEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val merchant: String,
    val categoryKey: String,
)

/** Daily net worth, recorded by the background refresh for the history chart. */
@Entity(tableName = "net_worth_history")
data class NetWorthSnapshotEntity(
    @PrimaryKey val epochDay: Long,
    val liquid: Paise,
    val cardDebt: Paise,
    val total: Paise,
)

/** One part of a transaction split across categories; the parts replace the transaction's own category in stats. */
@Entity(
    tableName = "txn_splits",
    foreignKeys = [
        ForeignKey(entity = TransactionEntity::class, parentColumns = ["id"], childColumns = ["transactionId"], onDelete = ForeignKey.CASCADE),
    ],
    indices = [Index("transactionId")],
)
data class SplitEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val transactionId: Long,
    val categoryKey: String,
    val amount: Paise,
    val note: String? = null,
)

/** Money lent to or borrowed from a person, settled over time. [direction] is LENT or BORROWED. */
@Entity(tableName = "debts")
data class DebtEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val person: String,
    val direction: String,
    val amount: Paise,
    val repaid: Paise = 0,
    /** Epoch day. */
    val date: Long,
    /** Epoch day the money is due back, when agreed. */
    val dueDate: Long? = null,
    val note: String? = null,
    val closed: Boolean = false,
    val createdAt: Long = System.currentTimeMillis(),
)

/**
 * A payment the user schedules (rent paid in cash, a SIP, a salary credit). When [autoAdd] is on, the daily refresh
 * records each due occurrence as a transaction; [nextDay] is the next occurrence not yet recorded.
 */
@Entity(
    tableName = "recurring_rules",
    foreignKeys = [
        ForeignKey(entity = AccountEntity::class, parentColumns = ["id"], childColumns = ["accountId"], onDelete = ForeignKey.CASCADE),
    ],
    indices = [Index("accountId")],
)
data class RecurringRuleEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val name: String,
    val type: TxnType,
    val amount: Paise,
    val categoryKey: String,
    val accountId: Long,
    val toAccountId: Long? = null,
    /** A `finance.Recurring.Cadence` name. */
    val cadence: String,
    /** Epoch day of the first occurrence; later ones keep its day of month or weekday. */
    val anchorDay: Long,
    /** Epoch day of the next occurrence not yet recorded. */
    val nextDay: Long,
    val active: Boolean = true,
    val autoAdd: Boolean = true,
    val note: String? = null,
    val createdAt: Long = System.currentTimeMillis(),
)

/** A home, car, personal or education loan; the schedule comes from `finance.Loans`. */
@Entity(tableName = "loans")
data class LoanEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val name: String,
    val lender: String,
    /** HOME, CAR, PERSONAL, EDUCATION or OTHER. */
    val kind: String,
    val principal: Paise,
    val ratePct: Double,
    val tenureMonths: Int,
    /** Epoch day of the first EMI. */
    val firstEmiDay: Long,
    /** The EMI the lender charges, when it differs from the computed one. */
    val emi: Paise? = null,
    /** Prepayments as a JSON array of {day, amount, reduce}. */
    val prepaymentsJson: String = "[]",
    /** A home loan on a house the user lives in (section 24(b) limit of ₹2 lakh applies). */
    val selfOccupied: Boolean = true,
    val archived: Boolean = false,
    val createdAt: Long = System.currentTimeMillis(),
)

/** A receipt or warranty photo kept (encrypted) in app storage and linked to a transaction. */
@Entity(
    tableName = "attachments",
    foreignKeys = [
        ForeignKey(entity = TransactionEntity::class, parentColumns = ["id"], childColumns = ["transactionId"], onDelete = ForeignKey.CASCADE),
    ],
    indices = [Index("transactionId")],
)
data class AttachmentEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val transactionId: Long,
    /** File name inside the app's private attachments folder. */
    val fileName: String,
    val mimeType: String,
    val sizeBytes: Long,
    val label: String? = null,
    val createdAt: Long = System.currentTimeMillis(),
)

/** A shared-expense group (e.g. "Roommates", "Trip to Manali"). [archived] hides it without deleting history. */
@Entity(tableName = "split_groups")
data class SplitGroupEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val name: String,
    val archived: Boolean = false,
    val createdAt: Long = System.currentTimeMillis(),
)

/** A participant in a split group. Local display name only — no account, registration or internet needed. */
@Entity(
    tableName = "split_group_participants",
    foreignKeys = [
        ForeignKey(entity = SplitGroupEntity::class, parentColumns = ["id"], childColumns = ["groupId"], onDelete = ForeignKey.CASCADE),
    ],
    indices = [Index("groupId")],
)
data class SplitGroupParticipantEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val groupId: Long,
    val name: String,
    /** Keeps historical splits readable when a participant is renamed. */
    val createdAt: Long = System.currentTimeMillis(),
)

/** One shared expense recorded inside a group. [linkedTransactionId] optionally references a personal Sumvalo transaction. */
@Entity(
    tableName = "split_expenses",
    foreignKeys = [
        ForeignKey(entity = SplitGroupEntity::class, parentColumns = ["id"], childColumns = ["groupId"], onDelete = ForeignKey.CASCADE),
        ForeignKey(entity = SplitGroupParticipantEntity::class, parentColumns = ["id"], childColumns = ["payerId"], onDelete = ForeignKey.CASCADE),
        ForeignKey(entity = TransactionEntity::class, parentColumns = ["id"], childColumns = ["linkedTransactionId"], onDelete = ForeignKey.SET_NULL),
    ],
    indices = [Index("groupId"), Index("payerId"), Index("linkedTransactionId")],
)
data class SplitExpenseEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val groupId: Long,
    val description: String,
    val amount: Paise,
    /** Epoch day (LocalDate.toEpochDay). */
    val date: Long,
    val payerId: Long,
    val categoryKey: String? = null,
    val note: String? = null,
    /** Links this shared expense to an existing personal transaction, if the user chose to create one. */
    val linkedTransactionId: Long? = null,
    val createdAt: Long = System.currentTimeMillis(),
)

/** How much one participant owes (or is owed) for one expense. Positive = owes this amount. */
@Entity(
    tableName = "split_shares",
    foreignKeys = [
        ForeignKey(entity = SplitExpenseEntity::class, parentColumns = ["id"], childColumns = ["expenseId"], onDelete = ForeignKey.CASCADE),
        ForeignKey(entity = SplitGroupParticipantEntity::class, parentColumns = ["id"], childColumns = ["participantId"], onDelete = ForeignKey.CASCADE),
    ],
    indices = [Index("expenseId"), Index("participantId")],
)
data class SplitShareEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val expenseId: Long,
    val participantId: Long,
    /** The participant's share of the expense, in paise. */
    val share: Paise,
)

/** A repayment between two participants in a group. [fromId] pays [toId]. */
@Entity(
    tableName = "split_settlements",
    foreignKeys = [
        ForeignKey(entity = SplitGroupEntity::class, parentColumns = ["id"], childColumns = ["groupId"], onDelete = ForeignKey.CASCADE),
        ForeignKey(entity = SplitGroupParticipantEntity::class, parentColumns = ["id"], childColumns = ["fromId"], onDelete = ForeignKey.CASCADE),
        ForeignKey(entity = SplitGroupParticipantEntity::class, parentColumns = ["id"], childColumns = ["toId"], onDelete = ForeignKey.CASCADE),
    ],
    indices = [Index("groupId"), Index("fromId"), Index("toId")],
)
data class SplitSettlementEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val groupId: Long,
    val fromId: Long,
    val toId: Long,
    val amount: Paise,
    /** Epoch day. */
    val date: Long,
    val note: String? = null,
    /** If true, this settlement was reversed (kept for history, excluded from balances). */
    val reversed: Boolean = false,
    val createdAt: Long = System.currentTimeMillis(),
)

/** A purchase record with optional receipt attachment and warranty/return deadlines. */
@Entity(tableName = "purchase_records")
data class PurchaseRecordEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val itemName: String,
    val merchant: String? = null,
    /** Epoch day of purchase. */
    val purchaseDate: Long,
    val amount: Paise? = null,
    val category: String? = null,
    val notes: String? = null,
    /** Tags stored as \|tag1\|tag2\|. */
    val tags: String = "",
    /** Epoch day warranty started, if known. */
    val warrantyStart: Long? = null,
    /** Epoch day warranty expires. */
    val warrantyUntil: Long? = null,
    /** Epoch day return/replacement deadline. */
    val returnUntil: Long? = null,
    val invoiceNo: String? = null,
    val serialNo: String? = null,
    /** Links this purchase to an existing personal transaction, if any. */
    val linkedTransactionId: Long? = null,
    /** Reminders enabled for this record. */
    val remindersEnabled: Boolean = true,
    val createdAt: Long = System.currentTimeMillis(),
)

/** A document attached to a purchase record (receipt, invoice, warranty card, etc.). */
@Entity(
    tableName = "purchase_attachments",
    foreignKeys = [
        ForeignKey(entity = PurchaseRecordEntity::class, parentColumns = ["id"], childColumns = ["purchaseId"], onDelete = ForeignKey.CASCADE),
    ],
    indices = [Index("purchaseId")],
)
data class PurchaseAttachmentEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val purchaseId: Long,
    /** File name inside the app's private purchase-attachments folder. */
    val fileName: String,
    val mimeType: String,
    val sizeBytes: Long,
    val label: String? = null,
    val createdAt: Long = System.currentTimeMillis(),
)
