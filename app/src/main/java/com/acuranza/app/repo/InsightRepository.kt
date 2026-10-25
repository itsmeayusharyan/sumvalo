package com.acuranza.app.repo

import com.acuranza.app.data.SumvaloDatabase
import com.acuranza.app.data.NetWorthSnapshotEntity
import com.acuranza.app.data.prefs.SettingsStore
import com.acuranza.app.domain.SmsMapping
import com.acuranza.app.finance.AccountInput
import com.acuranza.app.finance.BillInput
import com.acuranza.app.finance.BudgetInput
import com.acuranza.app.finance.GoalInput
import com.acuranza.app.finance.Insight
import com.acuranza.app.finance.InsightEngine
import com.acuranza.app.finance.InsightInput
import com.acuranza.app.finance.InsightTxn
import com.acuranza.app.model.AccountType
import com.acuranza.app.model.Paise
import com.acuranza.app.util.Time
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.debounce
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.mapLatest
import java.time.LocalDate

/** Offline insights from the rule engine, regenerated whenever the underlying data changes. */
class InsightRepository(
    private val db: SumvaloDatabase,
    private val accounts: AccountRepository,
    private val settings: SettingsStore,
    private val budgetRepo: BudgetRepository,
    private val planningPrefs: com.acuranza.app.data.prefs.PlanningPrefs,
) {

    /** Insights the user has not dismissed, most important first. */
    @OptIn(FlowPreview::class, ExperimentalCoroutinesApi::class)
    fun observeInsights(): Flow<List<Insight>> =
        combine(
            listOf<Flow<Any?>>(
            refreshTick,
            db.transactions().observeChanges(),
            db.accounts().observeActive(),
            db.budgets().observeAll(),
            db.goals().observeAll(),
            db.bills().observeUnpaid(),
            // What insights read from settings; scan times and other writes don't need a rebuild.
            settings.settings.map { Triple(it.payCycle, it.dismissedInsightIds, it.monthStartDay) }.distinctUntilChanged(),
            planningPrefs.goalInflation,
            ),
        ) { values -> values.size }
            .debounce(300)
            .mapLatest { generate(includeDismissed = false) }
            .flowOn(Dispatchers.Default)

    suspend fun generate(includeDismissed: Boolean = false, today: LocalDate = Time.today()): List<Insight> {
        val input = buildInput(today)
        val dismissed = if (includeDismissed) emptySet() else settings.current().dismissedInsightIds
        // Ids are unique by construction; distinctBy keeps a list keyed by id from ever crashing on a repeat.
        return InsightEngine.generate(input).filterNot { it.id in dismissed }.distinctBy { it.id }
    }

    suspend fun dismiss(id: String) = settings.dismissInsight(id)

    /** Bumped to force [observeInsights] to regenerate, e.g. on pull-to-refresh. */
    private val refreshTick = MutableStateFlow(0)

    /** Regenerates the observed insights now. */
    fun refresh() {
        refreshTick.value = refreshTick.value + 1
    }

    /** Builds the engine input from the last ~4 months of data. */
    suspend fun buildInput(today: LocalDate = Time.today()): InsightInput {
        val since = Time.startOfDay(today.withDayOfMonth(1).minusMonths(5))
        val s = settings.settings.first()
        val monthStartDay = s.monthStartDay
        val accountList = accounts.accounts()
        val cardIds = accountList.filter { it.isCard }.map { it.id }.toSet()
        // Split transactions count as their parts, as on the Home and Budgets screens.
        val txns = com.acuranza.app.domain.Splits.expand(
            db.transactions().since(since).filter { SmsMapping.countsInStats(it) },
            com.acuranza.app.domain.Splits.byTransaction(db.splits().all()),
        )
            .map { t ->
                InsightTxn(
                    date = Time.toDate(t.timestamp),
                    amount = t.amount,
                    type = t.type,
                    category = t.categoryKey,
                    merchant = t.merchant,
                    accountId = t.accountId,
                    isCreditCard = t.accountId in cardIds,
                    earned = t.type != com.acuranza.app.model.TxnType.INCOME || com.acuranza.app.domain.MoneyIn.isEarned(t),
                )
            }
        val byId = accountList.associateBy { it.id }
        return InsightInput(
            today = today,
            transactions = txns,
            // Limits include what rolled over, matching the Budgets screen.
            budgets = budgetRepo.observeStatuses().first().map { BudgetInput(it.budget.categoryKey, it.limit) },
            goals = planningPrefs.goalInflation.first().let { inflation -> db.goals().all().map { it.toInput(inflation[it.id] ?: 0.0) } },
            // Accounts the user hasn't confirmed have no trustworthy balance, so balance rules ignore them.
            accounts = accountList.filter { it.counted }.map { a ->
                AccountInput(
                    id = a.id,
                    name = a.account.name,
                    type = a.account.type,
                    // The engine reads a card's balance as the amount owed.
                    balance = if (a.isCard) a.outstanding else a.balance,
                    creditLimit = a.account.creditLimit,
                )
            },
            upcomingBills = db.bills().unpaid().mapNotNull { b ->
                val due = b.dueDate ?: return@mapNotNull null
                BillInput(
                    byId[b.accountId]?.account?.name ?: "Card", b.totalDue, LocalDate.ofEpochDay(due), b.paid,
                    accountId = b.accountId, statementDate = b.statementDate?.let(LocalDate::ofEpochDay),
                )
            },
            categoryNames = db.categories().all().associate { it.key to it.name },
            monthStartDay = monthStartDay,
            payCycle = s.payCycle,
        )
    }
}

/**
 * Net worth split into its parts: money in confirmed bank, cash and wallet accounts, minus amounts owed on
 * confirmed credit cards and loans still outstanding. [counted] lists the accounts behind [liquid] and
 * [cardDebt]; [pending] lists accounts found in SMS/e-mail that are not counted until the user confirms them.
 */
data class NetWorth(
    val liquid: Paise,
    val cardDebt: Paise,
    val counted: List<AccountWithBalance> = emptyList(),
    val pending: List<AccountWithBalance> = emptyList(),
    /** Still owed on loans (home, car, personal, education). */
    val loans: Paise = 0,
    val loanItems: List<Pair<String, Paise>> = emptyList(),
) {
    val total: Paise get() = liquid - cardDebt - loans
}

class NetWorthRepository(
    private val db: SumvaloDatabase,
    private val accounts: AccountRepository,
    private val loanRepo: LoanRepository,
) {

    fun observeNetWorth(): Flow<NetWorth> =
        combine(accounts.observeAccounts(), loanRepo.observe()) { accs, loans -> compute(accs, loans) }

    fun observeHistory(): Flow<List<NetWorthSnapshotEntity>> = db.netWorth().observeAll()

    suspend fun current(): NetWorth = compute(accounts.accounts(), loanRepo.active())


    /** Records today's net worth for the history chart (called by the daily worker). Loans count with card dues as debt. */
    suspend fun snapshot(today: LocalDate = Time.today()) {
        val nw = current()
        db.netWorth().upsert(NetWorthSnapshotEntity(today.toEpochDay(), nw.liquid, nw.cardDebt + nw.loans, nw.total))
    }

    /** Clears the recorded history (used once after the v1.4 balance-rule repair) and records today afresh. */
    suspend fun resetHistory(today: LocalDate = Time.today()) {
        db.netWorth().deleteAll()
        snapshot(today)
    }

    private fun compute(
        accs: List<AccountWithBalance>,
        loans: List<LoanStatus> = emptyList(),
    ): NetWorth {
        val counted = accs.filter { it.counted }
        val liquid = counted.filter { it.account.type != AccountType.CREDIT_CARD }.sumOf { it.balance }
        val debt = counted.filter { it.isCard }.sumOf { it.outstanding.coerceAtLeast(0) }
        return NetWorth(
            liquid = liquid,
            cardDebt = debt,
            counted = counted,
            pending = accs.filter { it.isPending },
            loans = loans.sumOf { it.outstanding },
            loanItems = loans.filter { it.outstanding > 0 }.map { it.loan.name to it.outstanding },
        )
    }
}
