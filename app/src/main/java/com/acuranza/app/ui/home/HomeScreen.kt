package com.acuranza.app.ui.home

import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.material3.Badge
import androidx.compose.material3.BadgedBox
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.material.icons.outlined.AccountBalance
import androidx.compose.material.icons.outlined.ChevronRight
import androidx.compose.material.icons.outlined.CreditCard
import androidx.compose.material.icons.outlined.Notifications
import androidx.compose.material.icons.outlined.Lightbulb
import androidx.compose.material.icons.outlined.Visibility
import androidx.compose.material.icons.outlined.VisibilityOff
import androidx.compose.ui.text.style.TextAlign
import com.acuranza.app.designsystem.components.StackedBar
import com.acuranza.app.designsystem.theme.SumvaloText
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.outlined.Bolt
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.ViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewModelScope
import androidx.navigation.NavController
import com.acuranza.app.designsystem.components.AmountText
import com.acuranza.app.designsystem.components.BankBadge
import com.acuranza.app.designsystem.components.ChartLegend
import com.acuranza.app.designsystem.components.ChartSlice
import com.acuranza.app.designsystem.components.DonutChart
import com.acuranza.app.designsystem.components.EmptyState
import com.acuranza.app.designsystem.components.InsightCard
import com.acuranza.app.designsystem.components.SumvaloCard
import com.acuranza.app.designsystem.components.SectionHeader
import com.acuranza.app.designsystem.components.StatChip
import com.acuranza.app.designsystem.components.ThinProgressBar
import com.acuranza.app.designsystem.components.TransactionRow
import com.acuranza.app.designsystem.theme.staggeredEntrance
import com.acuranza.app.designsystem.theme.AmountStyles
import com.acuranza.app.designsystem.theme.SumvaloTheme
import com.acuranza.app.di.AppContainer
import com.acuranza.app.finance.Insight
import com.acuranza.app.model.TxnType
import com.acuranza.app.repo.AccountWithBalance
import com.acuranza.app.repo.BillItem
import com.acuranza.app.repo.MonthSummary
import com.acuranza.app.repo.NetWorth
import com.acuranza.app.repo.TransactionItem
import com.acuranza.app.ui.common.Fmt
import com.acuranza.app.ui.common.Pill
import com.acuranza.app.ui.common.LocalContainer
import com.acuranza.app.ui.common.LocalHideAmounts
import com.acuranza.app.ui.common.bankEnum
import com.acuranza.app.ui.common.color
import com.acuranza.app.ui.common.acuranzaViewModel
import com.acuranza.app.ui.common.label
import com.acuranza.app.ui.common.toUi
import com.acuranza.app.ui.nav.Routes
import com.acuranza.app.util.Period
import com.acuranza.app.util.Time
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.mapLatest
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.Dispatchers
import com.acuranza.app.designsystem.components.BarChart
import com.acuranza.app.designsystem.components.SpendHeatmap
import com.acuranza.app.designsystem.components.RangeChips
import com.acuranza.app.designsystem.components.TrendChart
import com.acuranza.app.ui.home.TrendRange
import com.acuranza.app.designsystem.components.SpendPaceChart
import kotlinx.coroutines.flow.shareIn
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import com.acuranza.app.ui.nav.navigateToTab
import androidx.compose.material.icons.automirrored.outlined.ReceiptLong
import androidx.compose.material.icons.outlined.SwapHoriz
import androidx.compose.material.icons.outlined.Add
import androidx.compose.material.icons.outlined.Assessment
import androidx.compose.material3.LocalContentColor
import androidx.compose.runtime.CompositionLocalProvider
import com.acuranza.app.designsystem.components.GradientCard
import com.acuranza.app.designsystem.components.HeroActionButton
import com.acuranza.app.designsystem.components.QuickAction
import com.acuranza.app.designsystem.components.Sparkline

data class HomeState(
    val loading: Boolean = true,
    val netWorth: NetWorth? = null,
    val trend: List<Float> = emptyList(),
    val summary: MonthSummary? = null,
    val unreviewed: Int = 0,
    val reviewFromSms: Int = 0,
    val reviewFromEmail: Int = 0,
    val bills: List<BillItem> = emptyList(),
    /** Unpaid card bills — used by the bills card and "left to spend". */
    val billsDue: Int = 0,
    /** Unread items for the header's notification bell. */
    val notificationsUnread: Int = 0,
    /** The user's display name for the greeting; null = just "Good morning". */
    val profileName: String? = null,
    /** The saved profile photo path for the header avatar; null = initials fallback. */
    val profileAvatar: String? = null,
    val insights: List<Insight> = emptyList(),
    val recent: List<TransactionItem> = emptyList(),
    val accounts: List<AccountWithBalance> = emptyList(),
    val charts: HomeCharts = HomeCharts(),
    /** Accounts found in SMS/e-mail waiting for the user's confirmation (not in net worth). */
    val pendingAccounts: Int = 0,
    /** Bank SMS from the last week stored on accounts the user ignored, so left out of every total. */
    val ignoredSms: Int = 0,
    /** Imported expenses this month still in "Other". */
    val uncategorised: Int = 0,
    /** What can be spent each day until payday, once bills and repeating payments are set aside. */
    val safeToSpend: com.acuranza.app.repo.SafeToSpend.Result? = null,
    /** What the salary-plan sheet starts from. */
    val salaryPlan: SalaryPlanInfo = SalaryPlanInfo(),
    /** The dashboard sections to render, in the user's saved order (Settings → Appearance → Edit home). */
    val sections: List<HomeSection> = HomeLayout.DEFAULT,
) {
    val isEmpty: Boolean get() = !loading && accounts.isEmpty() && recent.isEmpty()
}

@OptIn(ExperimentalCoroutinesApi::class)
class HomeViewModel(private val c: AppContainer) : ViewModel() {

    private val period = c.settings.settings.map { Period.containing(Time.today(), it.payCycle) }.distinctUntilChanged()

    // Shared: the money card and "left to spend" both use it, and it shouldn't be computed twice per change.
    private val summary = period.flatMapLatest { c.transactions.observeSummary(it) }
        .shareIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), replay = 1)

    private val money = combine(
        c.netWorth.observeNetWorth(),
        c.netWorth.observeHistory(),
        summary,
    ) { nw, history, s -> Triple(nw, history.takeLast(30).map { (it.total / 100.0).toFloat() }, s) }

    private val lists = combine(
        c.transactions.observeUnreviewed(),
        c.bills.observeUnpaid(),
        c.insights.observeInsights(),
        c.transactions.observeRecent(8),
        c.accounts.observeAccounts(),
    ) { unreviewed, bills, insights, recent, accounts ->
        Lists(
            unreviewed.size,
            unreviewed.count { it.txn.source == com.acuranza.app.model.TxnSource.SMS },
            unreviewed.count { it.txn.source == com.acuranza.app.model.TxnSource.EMAIL },
            bills, insights, recent, accounts,
        )
    }

    private val charts = combine(
        period,
        c.db.transactions().observeChanges(),
        c.netWorth.observeHistory(),
    ) { p, _, history -> p to history }
        .mapLatest { (p, history) ->
            val since = Time.startOfDay(p.start.minusMonths(6))
            HomeCharts.build(c.db.transactions().since(since), p, Time.today(), history)
        }
        .flowOn(Dispatchers.Default)

    private val uncategorised = period.flatMapLatest { p -> c.db.transactions().observeUncategorisedCount(p.startMillis, p.endMillis) }

    private val ignoredSms = c.settings.settings
        // Whole days, so the query isn't restarted on every settings change.
        .map { maxOf(Time.startOfDay(Time.today().minusDays(IGNORED_NOTICE_DAYS)), it.ignoredNoticeAt) }
        .distinctUntilChanged()
        .flatMapLatest { from -> c.db.transactions().observeCountOnIgnored(from) }

    private val counts = combine(uncategorised, ignoredSms) { other, ignored -> other to ignored }

    private val safeToSpend = combine(
        combine(c.accounts.observeAccounts(), c.bills.observeUnpaid()) { a, b -> a to b },
        c.settings.settings,
        summary,
        combine(c.budgets.observeStatuses(), c.featurePrefs.settings) { b, f -> b to f },
        c.recurring.observeRules(),
    ) { (accounts, bills), s, sum, (budgets, f), _ -> SafeInputs(accounts, bills, s.payCycle, sum, budgets, f.salary, f.savingsTarget, s.holidayNames) }
        .mapLatest { inp ->
            runCatching {
                val today = Time.today()
                val salaryThisPeriod = inp.summary.incomeByCategory.firstOrNull { it.categoryKey == com.acuranza.app.repo.SafeToSpend.SALARY_CATEGORY }?.total ?: 0L
                val latestSalary = c.db.transactions().since(Time.startOfDay(today.minusDays(45)))
                    .filter { it.type == TxnType.INCOME && it.categoryKey == com.acuranza.app.repo.SafeToSpend.SALARY_CATEGORY && com.acuranza.app.domain.SmsMapping.countsInStats(it) }
                    .maxByOrNull { it.timestamp }?.amount
                val detected = salaryThisPeriod.takeIf { it > 0 } ?: latestSalary
                val salary = com.acuranza.app.repo.SafeToSpend.salaryFor(inp.salary, salaryThisPeriod, latestSalary)
                // Usual monthly spending: the average of the last three budget months that had any.
                val spends = mutableListOf<Long>()
                var p = Period.containing(today, inp.cycle)
                repeat(3) {
                    p = p.previous()
                    c.transactions.summary(p).expense.takeIf { it > 0 }?.let(spends::add)
                }
                val usual = spends.takeIf { it.isNotEmpty() }?.let { it.sum() / it.size }
                val result = com.acuranza.app.repo.SafeToSpend.compute(
                    inp.accounts, inp.bills, inp.cycle, c.recurring, inp.budgets, inp.summary, usual, salary, inp.savingsTarget, today,
                )
                result to SalaryPlanInfo(inp.salary, detected, inp.savingsTarget, inp.cycle.dueDay, paydayNote(inp.cycle, inp.holidayNames, today))
            }.getOrNull() ?: (null to SalaryPlanInfo(inp.salary, null, inp.savingsTarget, inp.cycle.dueDay, paydayNote(inp.cycle, inp.holidayNames, Time.today())))
        }
        .flowOn(Dispatchers.Default)

    private data class SafeInputs(
        val accounts: List<AccountWithBalance>,
        val bills: List<BillItem>,
        val cycle: com.acuranza.app.finance.PayCycle,
        val summary: MonthSummary,
        val budgets: List<com.acuranza.app.repo.BudgetStatus>,
        val salary: Long?,
        val savingsTarget: Long?,
        val holidayNames: Map<java.time.LocalDate, String>,
    )

    private val unreadNotifications = c.notificationCenter.observeNotifications()
        .map { list -> list.count { it.unread } }

    fun dismissIgnoredNotice() = viewModelScope.launch { c.settings.dismissIgnoredNotice() }

    fun saveSalaryPlan(salary: Long?, savingsTarget: Long?, payDay: Int) = viewModelScope.launch {
        c.featurePrefs.setSalaryPlan(salary, savingsTarget)
        c.settings.setMonthStartDay(payDay)
    }

    // kotlinx' named combine overloads stop at five flows, so the unread count is layered on top
    // of the base state instead of being a sixth source.
    private val baseState: Flow<HomeState> = combine(money, lists, charts, counts, safeToSpend) { (nw, trend, s), l, ch, (other, ignored), safe ->
        HomeState(
            safeToSpend = safe.first,
            salaryPlan = safe.second,
            pendingAccounts = nw.pending.size,
            ignoredSms = ignored,
            uncategorised = other,
            charts = ch,
            loading = false,
            netWorth = nw,
            trend = trend,
            summary = s,
            unreviewed = l.unreviewed,
            reviewFromSms = l.fromSms,
            reviewFromEmail = l.fromEmail,
            bills = l.bills.take(6),
            billsDue = l.bills.size,
            insights = l.insights.filterNot { it.isSummary }.take(5),
            recent = l.recent,
            accounts = l.accounts,
        )
    }.flowOn(Dispatchers.Default)

    val state: StateFlow<HomeState> = combine(
        baseState,
        unreadNotifications,
        c.settings.settings.map { it.homeLayout }.distinctUntilChanged(),
        c.settings.settings.map { it.profileName }.distinctUntilChanged(),
        c.settings.settings.map { it.profileAvatar }.distinctUntilChanged(),
    ) { base, unread, savedLayout, profileName, profileAvatar ->
        base.copy(
            notificationsUnread = unread,
            sections = HomeLayout.visible(HomeLayout.decode(savedLayout)),
            profileName = profileName,
            profileAvatar = profileAvatar,
        )
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), HomeState())

    fun toggleHidden(current: Boolean) = viewModelScope.launch { c.settings.setHideAmounts(!current) }

    private data class Lists(
        val unreviewed: Int,
        val fromSms: Int,
        val fromEmail: Int,
        val bills: List<BillItem>,
        val insights: List<Insight>,
        val recent: List<TransactionItem>,
        val accounts: List<AccountWithBalance>,
    )
}

@Composable
fun HomeScreen(nav: NavController, onHomeReTap: () -> Unit = {}) {
    val vm = acuranzaViewModel { HomeViewModel(it) }
    val state by vm.state.collectAsStateWithLifecycle()
    val hidden = LocalHideAmounts.current
    val container = LocalContainer.current
    val scope = rememberCoroutineScope()
    val listState = rememberLazyListState()
    var loadingDemo by remember { mutableStateOf(false) }
    var planSheet by remember { mutableStateOf(false) }

    // Re-tap on the Home tab while already on Home: scroll to the top and let the
    // reactive flows re-emit (they are already live via SharingStarted.WhileSubscribed,
    // so no new database queries are triggered — the UI simply re-renders from the
    // latest data).
    LaunchedEffect(onHomeReTap) {
        listState.scrollToItem(0)
    }

    if (planSheet) {
        SalaryPlanSheet(
            info = state.salaryPlan,
            onDismiss = { planSheet = false },
            onSave = { salary, save, day -> vm.saveSalaryPlan(salary, save, day); planSheet = false },
            onClear = { vm.saveSalaryPlan(null, null, state.salaryPlan.payDay); planSheet = false },
            onUpcoming = { planSheet = false; nav.navigate(Routes.RECURRING) },
        )
    }
    HomeScreenDesloped(
        nav = nav,
        state = state,
        hidden = hidden,
        listState = listState,
        onProfile = { nav.navigate(com.acuranza.app.ui.nav.Routes.PROFILE) },
        onToggleHidden = { vm.toggleHidden(hidden) },
        onInsights = { nav.navigateToTab(Routes.INSIGHTS) },
        onReview = { nav.navigate(Routes.REVIEW) },
        onBills = { nav.navigate(Routes.BILLS) },
        onNotifications = { nav.navigate(Routes.NOTIFICATIONS) },
        onActivity = { nav.navigateToTab(Routes.TRANSACTIONS) },
        onCategory = { key ->
            com.acuranza.app.ui.transactions.ActivityFilterRequest.pendingCategory = key
            nav.navigateToTab(Routes.TRANSACTIONS)
        },
        onTransaction = { nav.navigate(Routes.txn(it)) },
        onAccounts = { nav.navigate(Routes.ACCOUNTS) },
        onAccount = { nav.navigate(Routes.account(it)) },
        onAddAccount = { nav.navigate(Routes.editAccount()) },
        onPendingAccounts = { nav.navigate(Routes.PENDING_ACCOUNTS) },
        onIgnoredSms = { nav.navigate(Routes.ACCOUNTS) },
        onDismissIgnoredSms = { vm.dismissIgnoredNotice() },
        onUncategorised = { nav.navigate(Routes.UNCATEGORISED) },
        onSafeToSpend = { planSheet = true },
        onAddExpense = { nav.navigate(Routes.addTxn(TxnType.EXPENSE)) },
        onAddIncome = { nav.navigate(Routes.addTxn(TxnType.INCOME)) },
        onTransfer = { nav.navigate(Routes.addTxn(TxnType.TRANSFER)) },
        onReports = { nav.navigate(Routes.REPORTS) },
        onLoadDemo = {
            loadingDemo = true
            scope.launch {
                container.demoData.load()
                loadingDemo = false
            }
        },
    )
}

/** Shown when more imported expenses than this are in "Other" this month. */
const val UNCATEGORISED_BANNER_MIN = 5

/** Days of bank SMS on ignored accounts the Home notice looks back over. */
const val IGNORED_NOTICE_DAYS = 7L

/**
 * The insights shortcut that used to occupy the header's top-right corner. Removed from the UI in
 * favour of the card-bills badge; kept intact — handler and all — for potential future use.
 */
@Composable
private fun InsightsBulbButton(onInsights: () -> Unit) {
    IconButton(onClick = onInsights) {
        Icon(Icons.Outlined.Lightbulb, contentDescription = "Insights", tint = SumvaloTheme.colors.subtleText)
    }
}

/**
 * The header's notification bell: opens the in-app notification center, with a badge showing the
 * number of unread items. No badge when everything is read.
 */
@Composable
private fun NotificationsBellButton(unread: Int, onOpen: () -> Unit) {
    IconButton(onClick = onOpen) {
        BadgedBox(
            badge = {
                if (unread > 0) {
                    Badge(
                        containerColor = SumvaloTheme.colors.accent,
                        contentColor = SumvaloTheme.colors.onInk,
                    ) {
                        Text("$unread")
                    }
                }
            },
        ) {
            Icon(
                Icons.Outlined.Notifications,
                contentDescription = if (unread > 0) "Notifications, $unread unread" else "Notifications",
                tint = SumvaloTheme.colors.subtleText,
            )
        }
    }
}

/** Stateless Home screen, rendered from [HomeState]; used by the app and by screenshot tests. */
@Composable
fun HomeContent(
    state: HomeState,
    hidden: Boolean,
    onProfile: () -> Unit = {},
    loadingDemo: Boolean = false,
    onToggleHidden: () -> Unit = {},
    onInsights: () -> Unit = {},
    onReview: () -> Unit = {},
    onBills: () -> Unit = {},
    onNotifications: () -> Unit = {},
    onActivity: () -> Unit = {},
    onCategory: (String) -> Unit = {},
    onTransaction: (Long) -> Unit = {},
    onAccounts: () -> Unit = {},
    onAccount: (Long) -> Unit = {},
    onAddAccount: () -> Unit = {},
    onLoadDemo: () -> Unit = {},
    onPendingAccounts: () -> Unit = {},
    onIgnoredSms: () -> Unit = {},
    onDismissIgnoredSms: () -> Unit = {},
    onUncategorised: () -> Unit = {},
    onSafeToSpend: () -> Unit = {},
    onAddExpense: () -> Unit = {},
    onAddIncome: () -> Unit = {},
    onTransfer: () -> Unit = {},
    onReports: () -> Unit = {},
    listState: androidx.compose.foundation.lazy.LazyListState = androidx.compose.foundation.lazy.rememberLazyListState(),
) {
    val pad = Modifier.padding(horizontal = 20.dp)
    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        state = listState,
        contentPadding = PaddingValues(bottom = 8.dp),
    ) {
        item(key = "header") {
            Row(
                Modifier.fillMaxWidth().padding(start = 20.dp, end = 8.dp, top = 12.dp, bottom = 4.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                com.acuranza.app.ui.settings.ProfileAvatar(
                    path = state.profileAvatar,
                    name = state.profileName,
                    size = 40.dp,
                    modifier = Modifier
                        .clip(androidx.compose.foundation.shape.CircleShape)
                        .clickable(role = androidx.compose.ui.semantics.Role.Button, onClick = onProfile),
                )
                Spacer(Modifier.width(10.dp))
                Column(Modifier.weight(1f)) {
                    Text(Fmt.todayLong(), style = MaterialTheme.typography.bodySmall, color = SumvaloTheme.colors.subtleText)
                    // Two lines keeps "Good evening, <long name>" readable at any width or font
                    // scale instead of hard-clipping; icons can never overlap it (weight(1f)).
                    Text(
                        Fmt.greeting() + (state.profileName?.takeIf { it.isNotBlank() }?.let { ", $it" } ?: ""),
                        style = SumvaloText.screenTitle,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis,
                    )
                    // The one compact financial indicator: real Safe-to-spend data, shown only
                    // while the user hasn't given it a home section of its own (no duplication),
                    // and only when a plan actually exists. Opens the existing plan sheet.
                    if (state.safeToSpend != null && state.sections.none { it == HomeSection.SAFE }) {
                        val safe = state.safeToSpend
                        Spacer(Modifier.height(4.dp))
                        Row(
                            Modifier
                                .clip(RoundedCornerShape(SumvaloTheme.spacing.cardRadius))
                                .background(SumvaloTheme.colors.accent.copy(alpha = 0.12f))
                                .clickable(role = androidx.compose.ui.semantics.Role.Button, onClick = onSafeToSpend)
                                .padding(horizontal = 10.dp, vertical = 4.dp),
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            Icon(
                                Icons.Outlined.Bolt,
                                contentDescription = null,
                                tint = if (safe.available < 0) SumvaloTheme.colors.expense else SumvaloTheme.colors.accent,
                                modifier = Modifier.size(14.dp),
                            )
                            Spacer(Modifier.width(5.dp))
                            Text(
                                if (safe.available < 0) "Overspent" else "Safe to spend",
                                style = MaterialTheme.typography.labelMedium,
                                color = if (safe.available < 0) SumvaloTheme.colors.expense else SumvaloTheme.colors.accent,
                            )
                            Spacer(Modifier.width(4.dp))
                            AmountText(
                                paise = safe.available,
                                style = MaterialTheme.typography.labelMedium,
                                compact = true,
                                hidden = hidden,
                                color = if (safe.available < 0) SumvaloTheme.colors.expense else SumvaloTheme.colors.accent,
                            )
                            Spacer(Modifier.width(4.dp))
                            Text(
                                "· ${safe.days}d",
                                style = MaterialTheme.typography.labelMedium,
                                color = SumvaloTheme.colors.subtleText,
                            )
                        }
                    }
                }
                IconButton(onClick = onToggleHidden) {
                    Icon(
                        if (hidden) Icons.Outlined.VisibilityOff else Icons.Outlined.Visibility,
                        contentDescription = if (hidden) "Show amounts" else "Hide amounts",
                        tint = SumvaloTheme.colors.subtleText,
                    )
                }
                NotificationsBellButton(unread = state.notificationsUnread, onOpen = onNotifications)
            }
        }

        item(key = "hero") {
            val nw = state.netWorth
            GradientCard(pad.padding(top = 20.dp), contentPadding = PaddingValues(20.dp)) {
                CompositionLocalProvider(LocalContentColor provides Color.White) {
                    Text("Net worth", style = MaterialTheme.typography.bodyMedium, color = Color.White.copy(alpha = 0.72f))
                    Spacer(Modifier.height(2.dp))
                    Row(verticalAlignment = Alignment.Bottom, modifier = Modifier.fillMaxWidth()) {
                        AmountText(
                            paise = nw?.total ?: 0,
                            style = AmountStyles.hero,
                            color = Color.White,
                            hidden = hidden,
                            animate = true,
                            modifier = Modifier.weight(1f),
                        )
                        if (state.trend.size >= 2) {
                            Sparkline(
                                values = state.trend,
                                modifier = Modifier.padding(bottom = 8.dp).size(width = 72.dp, height = 28.dp),
                                color = Color.White,
                                fill = false,
                            )
                        }
                    }
                    if (nw != null) {
                        Spacer(Modifier.height(14.dp))
                        Row(horizontalArrangement = Arrangement.spacedBy(20.dp)) {
                            MiniFigure("Cash & bank", nw.liquid, hidden, onDark = true)
                            MiniFigure(if (nw.loans > 0) "Dues & loans" else "Card dues", nw.cardDebt + nw.loans, hidden, onDark = true)
                        }
                    }
                    Spacer(Modifier.height(16.dp))
                    Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                        HeroActionButton("Add expense", onAddExpense, Modifier.weight(1f), filled = true, icon = Icons.Outlined.Add)
                        HeroActionButton("Add income", onAddIncome, Modifier.weight(1f), filled = false, icon = Icons.Outlined.Add)
                    }
                }
            }
        }

        // Action banners and the first-run card sit directly under the balance card: they ask the
        // user for something, so they never compete with charts for attention.
        if (state.ignoredSms > 0) {
item(key = "ignored-sms") {
                com.acuranza.app.ui.accounts.IgnoredSmsBanner(state.ignoredSms, onIgnoredSms, onDismissIgnoredSms, pad.padding(top = 16.dp))
            }
        }
        if (state.pendingAccounts > 0) {
item(key = "pending-accounts") {
                com.acuranza.app.ui.accounts.PendingAccountsBanner(state.pendingAccounts, onPendingAccounts, pad.padding(top = 16.dp))
            }
        }
        if (state.unreviewed > 0) {
item(key = "review") {
                ReviewBanner(state.unreviewed, pad.padding(top = 16.dp), onReview, state.reviewFromSms, state.reviewFromEmail)
            }
        }
        if (state.uncategorised > UNCATEGORISED_BANNER_MIN) {
item(key = "uncategorised") {
                com.acuranza.app.ui.transactions.UncategorisedBanner(state.uncategorised, onUncategorised, pad.padding(top = 12.dp))
            }
        }
        if (state.isEmpty) {
item(key = "empty") {
                SumvaloCard(pad.padding(top = SumvaloTheme.spacing.section)) {
                    EmptyState(
                        title = "Set up Sumvalo",
                        message = "Add your bank accounts, or scan your SMS inbox to pull in transactions automatically.",
                        icon = Icons.Outlined.AccountBalance,
                        actionLabel = "Add an account",
                        onAction = onAddAccount,
                    )
                    Text(
                        if (loadingDemo) "Loading sample data…" else "Explore with sample data",
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(12.dp))
                            .clickable(enabled = !loadingDemo, onClick = onLoadDemo)
                            .padding(12.dp),
                        color = SumvaloTheme.colors.accent,
                        style = MaterialTheme.typography.labelLarge,
                        textAlign = TextAlign.Center,
                    )
                }
            }
        }

        // The dashboard below is the user's own layout (Settings → Appearance → Edit home):
        // every section keeps its data conditions, order and visibility come from the saved layout,
        // and animateItem() makes reordering and show/hide feel fluid instead of jumpy.
        state.sections.forEach { section ->
            when (section) {
                HomeSection.QUICK -> item(key = "quick") {
            Row(Modifier.animateItem().padding(horizontal = 20.dp).padding(top = 12.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                QuickAction("Transfer", Icons.Outlined.SwapHoriz, onTransfer, Modifier.weight(1f))
                QuickAction("Bills", Icons.AutoMirrored.Outlined.ReceiptLong, onBills, Modifier.weight(1f))
                QuickAction("Reports", Icons.Outlined.Assessment, onReports, Modifier.weight(1f))
                QuickAction("Accounts", Icons.Outlined.AccountBalance, onAccounts, Modifier.weight(1f))
            }
        }
                HomeSection.SAFE -> state.safeToSpend?.let { safe -> item(key = "safe") {
                Column(Modifier.animateItem().padding(horizontal = 20.dp).padding(top = SumvaloTheme.spacing.section)) {
                    SafeToSpendCard(safe, hidden, onSafeToSpend)
                }
            } }
                HomeSection.STATS -> item(key = "stats") {
            val s = state.summary
            Column(Modifier.animateItem().padding(horizontal = 20.dp).padding(top = SumvaloTheme.spacing.section)) {
                SectionHeader("This month")
                SumvaloCard(contentPadding = PaddingValues(horizontal = 16.dp, vertical = 14.dp)) {
                    Row(Modifier.fillMaxWidth().height(IntrinsicSize.Min)) {
                        StatChip("Income", s?.income ?: 0, Modifier.weight(1f), type = TxnType.INCOME, hidden = hidden)
                        VerticalHairline()
                        StatChip("Spent", s?.expense ?: 0, Modifier.weight(1f).padding(start = 14.dp), type = TxnType.EXPENSE, hidden = hidden)
                        VerticalHairline()
                        StatChip("Saved", s?.savings ?: 0, Modifier.weight(1f).padding(start = 14.dp), hidden = hidden)
                    }
                    moneyInNote(s?.givenBack ?: 0, s?.received ?: 0, hidden)?.let {
                        Spacer(Modifier.height(10.dp))
                        Text(it, style = MaterialTheme.typography.bodySmall, color = SumvaloTheme.colors.subtleText)
                    }
                }
            }
        }
                HomeSection.PACE -> if (state.charts.hasPace) item(key = "pace") {
                Column(Modifier.animateItem().padding(horizontal = 20.dp).padding(top = SumvaloTheme.spacing.section)) {
                    SectionHeader("Spending pace", actionLabel = "Details", onAction = onActivity)
                    SumvaloCard {
                        SpendPaceChart(
                            current = state.charts.paceCurrent,
                            previous = state.charts.pacePrevious,
                            daysInMonth = state.charts.periodDays,
                            projected = state.charts.projected,
                            hidden = hidden,
                        )
                    }
                }
            }
                HomeSection.BILLS -> if (state.bills.isNotEmpty()) item(key = "bills") {
                Column(Modifier.animateItem().padding(horizontal = 20.dp).padding(top = SumvaloTheme.spacing.section)) {
                    SectionHeader("Upcoming bills", actionLabel = "See all", onAction = onBills)
                    SumvaloCard(contentPadding = PaddingValues(horizontal = 16.dp, vertical = 4.dp)) {
                        state.bills.take(3).forEachIndexed { i, b ->
                            if (i > 0) ListHairline()
                            BillListRow(b, hidden, onBills)
                        }
                    }
                }
            }
                HomeSection.INSIGHT -> state.insights.firstOrNull()?.let { top -> item(key = "insight") {
                Column(Modifier.animateItem().padding(horizontal = 20.dp).padding(top = SumvaloTheme.spacing.section)) {
                    SectionHeader("Insight", actionLabel = if (state.insights.size > 1) "All ${state.insights.size}" else null, onAction = onInsights)
                    InsightCard(
                        severity = top.severity.toUi(),
                        title = top.title,
                        message = top.message,
                        compact = true,
                        onClick = onInsights,
                    )
                }
            } }
                HomeSection.SPEND -> {
                    val spend = state.summary?.byCategory.orEmpty()
                    if (spend.isNotEmpty()) item(key = "spend") {
                val slices = remember(spend) {
                    spend.take(6).map { ChartSlice(it.category?.name ?: it.categoryKey, it.total, it.category.color()) }
                }
                Column(Modifier.animateItem().padding(horizontal = 20.dp).padding(top = SumvaloTheme.spacing.section)) {
                    SectionHeader("Where it went", actionLabel = "Details", onAction = onActivity)
                    SumvaloCard {
                        Box(Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
                            DonutChart(
                                slices = slices,
                                size = 188.dp,
                                thickness = 12.dp,
                                centerLabel = "Spent",
                                centerValue = state.summary?.expense,
                                hidden = hidden,
                            )
                        }
                        Spacer(Modifier.height(16.dp))
                        spend.take(6).forEachIndexed { i, c ->
                            if (i > 0) ListHairline(start = 18.dp)
                            Column(
                                Modifier
                                    .fillMaxWidth()
                                    .clip(RoundedCornerShape(12.dp))
                                    .clickable { onCategory(c.categoryKey) }
                                    .padding(vertical = 10.dp),
                            ) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Box(Modifier.size(8.dp).clip(CircleShape).background(c.category.color()))
                                    Spacer(Modifier.width(10.dp))
                                    Text(c.category?.name ?: c.categoryKey, style = MaterialTheme.typography.bodyMedium, modifier = Modifier.weight(1f), maxLines = 1, overflow = TextOverflow.Ellipsis)
                                    Text(Fmt.pct(c.share * 100, 0), style = MaterialTheme.typography.bodySmall, color = SumvaloTheme.colors.tertiaryText)
                                    Spacer(Modifier.width(12.dp))
                                    AmountText(c.total, style = AmountStyles.small, hidden = hidden, compact = true)
                                    Icon(Icons.Outlined.ChevronRight, contentDescription = null, tint = SumvaloTheme.colors.tertiaryText, modifier = Modifier.size(16.dp))
                                }
                                Spacer(Modifier.height(8.dp))
                                // Share of the month's spend, as in the reference breakdown.
                                Box(
                                    Modifier
                                        .fillMaxWidth()
                                        .height(4.dp)
                                        .clip(CircleShape)
                                        .background(SumvaloTheme.colors.fill),
                                ) {
                                    Box(
                                        Modifier
                                            .fillMaxWidth(c.share.coerceIn(0.0, 1.0).toFloat())
                                            .height(4.dp)
                                            .clip(CircleShape)
                                            .background(SumvaloTheme.colors.accent),
                                    )
                                }
                            }
                        }
                    }
                }
            }
                }
                HomeSection.SIX -> if (state.charts.hasSixMonths) item(key = "six") {
                Column(Modifier.animateItem().padding(horizontal = 20.dp).padding(top = SumvaloTheme.spacing.section)) {
                    SectionHeader("Income vs spending")
                    SumvaloCard {
                        BarChart(groups = state.charts.sixMonths, height = 170.dp, hidden = hidden)
                        Spacer(Modifier.height(10.dp))
                        Text("SAVINGS RATE", style = SumvaloText.sectionLabel, color = SumvaloTheme.colors.subtleText)
                        Spacer(Modifier.height(4.dp))
                        Row(Modifier.fillMaxWidth()) {
                            state.charts.savingsRates.forEach { r ->
                                Text(
                                    r,
                                    style = MaterialTheme.typography.labelMedium,
                                    fontWeight = FontWeight.SemiBold,
                                    color = if (r.startsWith("-")) SumvaloTheme.colors.alert else SumvaloTheme.colors.ink,
                                    textAlign = TextAlign.Center,
                                    modifier = Modifier.weight(1f),
                                )
                            }
                        }
                    }
                }
            }
                HomeSection.HEAT -> if (state.charts.hasHeat) item(key = "heat") {
                Column(Modifier.animateItem().padding(horizontal = 20.dp).padding(top = SumvaloTheme.spacing.section)) {
                    SectionHeader("Daily spending")
                    SumvaloCard {
                        SpendHeatmap(weeks = state.charts.heatWeeks, dayLabels = state.charts.heatLabels, hidden = hidden)
                    }
                }
            }
                HomeSection.NETWORTH -> if (state.charts.netWorth.size >= 2 || state.charts.todayHourly.size >= 2) item(key = "networth") {
                var range by remember { mutableStateOf(TrendRange.M1) }
                var selectedIdx by remember(range) { mutableStateOf<Int?>(null) }
                val points = remember(state.charts.netWorth, state.charts.todayHourly, range) {
                    state.charts.trendPoints(range)
                }
                Column(Modifier.animateItem().padding(horizontal = 20.dp).padding(top = SumvaloTheme.spacing.section)) {
                    SectionHeader("Net worth trend")
                    SumvaloCard {
                        AmountText(
                            paise = points.getOrNull(selectedIdx ?: points.lastIndex)?.value
                                ?: state.netWorth?.total ?: 0,
                            style = AmountStyles.medium,
                            compact = true,
                            hidden = hidden,
                        )
                        Spacer(Modifier.height(10.dp))
                        if (points.size >= 2) {
                            TrendChart(points = points, height = 150.dp, hidden = hidden, onSelectionChanged = { selectedIdx = it })
                        } else {
                            Text("Not enough history for this range yet.", style = MaterialTheme.typography.bodySmall, color = SumvaloTheme.colors.subtleText)
                        }
                        Spacer(Modifier.height(14.dp))
                        RangeChips(listOf("1D", "1W", "1M", "1Y", "ALL"), range.ordinal, onSelect = { range = TrendRange.values()[it] })
                    }
                }
            }
                HomeSection.RECENT -> if (state.recent.isNotEmpty()) item(key = "recent") {
                Column(Modifier.animateItem().padding(horizontal = 20.dp).padding(top = SumvaloTheme.spacing.section)) {
                    SectionHeader("Recent", actionLabel = "See all", onAction = onActivity)
                    SumvaloCard(contentPadding = PaddingValues(horizontal = 14.dp, vertical = 2.dp)) {
                        state.recent.forEachIndexed { i, t ->
                            if (i > 0) ListHairline(start = 56.dp)
                            TxnRow(t, hidden) { onTransaction(t.txn.id) }
                        }
                    }
                }
            }
                HomeSection.ACCOUNTS -> if (state.accounts.isNotEmpty()) item(key = "accounts") {
                Column(Modifier.animateItem().padding(horizontal = 20.dp).padding(top = SumvaloTheme.spacing.section)) {
                    SectionHeader("Accounts", actionLabel = "Manage", onAction = onAccounts)
                    SumvaloCard(contentPadding = PaddingValues(horizontal = 16.dp, vertical = 4.dp)) {
                        state.accounts.forEachIndexed { i, a ->
                            if (i > 0) ListHairline(start = 48.dp)
                            AccountListRow(a, hidden) { onAccount(a.id) }
                        }
                    }
                }
            }
            }
        }
    }
}

@Composable
private fun MiniFigure(label: String, value: Long, hidden: Boolean, onDark: Boolean = false) {
    val labelColor = if (onDark) Color.White.copy(alpha = 0.72f) else SumvaloTheme.colors.subtleText
    val valueColor = if (onDark) Color.White else SumvaloTheme.colors.ink
    Column {
        Text(label, style = MaterialTheme.typography.bodySmall, color = labelColor)
        AmountText(value, style = AmountStyles.small, compact = true, hidden = hidden, color = valueColor)
    }
}

@Composable
private fun VerticalHairline() {
    Box(Modifier.fillMaxHeight().width(1.dp).background(SumvaloTheme.colors.divider))
}

/** A 1dp divider for rows inside a card, optionally inset from the start. */
@Composable
fun ListHairline(start: androidx.compose.ui.unit.Dp = 0.dp) {
    Box(Modifier.fillMaxWidth().padding(start = start).height(1.dp).background(SumvaloTheme.colors.divider))
}

@Composable
private fun ReviewBanner(count: Int, modifier: Modifier, onClick: () -> Unit, fromSms: Int = 0, fromEmail: Int = 0) {
    Row(
        modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .background(SumvaloTheme.colors.accentTint)
            .clickable(onClick = onClick)
            .padding(horizontal = 14.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(Modifier.size(8.dp).clip(CircleShape).background(SumvaloTheme.colors.accent))
        Spacer(Modifier.width(10.dp))
        Column(Modifier.weight(1f)) {
            Text(
                "$count transaction${if (count == 1) "" else "s"} to review",
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.Medium,
                color = SumvaloTheme.colors.ink,
            )
            if (fromSms > 0) {
                Text(
                    if (fromEmail > 0) "Imported from SMS ($fromSms) and earlier email imports ($fromEmail)" else "$fromSms imported from SMS",
                    style = MaterialTheme.typography.bodySmall,
                    color = SumvaloTheme.colors.subtleText,
                )
            }
        }
        Text("Review", style = MaterialTheme.typography.labelMedium, color = SumvaloTheme.colors.accent)
        Icon(Icons.Outlined.ChevronRight, contentDescription = null, tint = SumvaloTheme.colors.accent, modifier = Modifier.size(18.dp))
    }
}

@Composable
private fun BillListRow(b: BillItem, hidden: Boolean, onClick: () -> Unit) {
    val days = b.daysLeft
    val urgent = days != null && days <= 3
    val dueColor = when {
        days != null && days < 0 -> SumvaloTheme.colors.alert
        urgent -> SumvaloTheme.colors.warning
        else -> SumvaloTheme.colors.subtleText
    }
    val bank = b.card?.bankEnum() ?: com.acuranza.app.model.Bank.OTHER
    Row(
        Modifier.fillMaxWidth().clickable(onClick = onClick).padding(vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        BankBadge(bank, size = 32.dp)
        Spacer(Modifier.width(12.dp))
        Column(Modifier.weight(1f)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(b.card?.label() ?: "Card", style = MaterialTheme.typography.bodyLarge, maxLines = 1, overflow = TextOverflow.Ellipsis)
                if (urgent || (days != null && days < 0)) {
                    Spacer(Modifier.width(6.dp))
                    Pill(
                        if (days != null && days < 0) "Overdue" else if (days == 0L) "Due today" else Fmt.relativeDays(days ?: 0),
                        dueColor,
                    )
                }
            }
            Text(days?.let { Fmt.relativeDays(it) } ?: "No due date", style = MaterialTheme.typography.bodySmall, color = dueColor)
        }
        AmountText(b.bill.totalDue, style = AmountStyles.medium, hidden = hidden)
    }
}

@Composable
private fun AccountListRow(a: AccountWithBalance, hidden: Boolean, onClick: () -> Unit) {
    Row(
        Modifier.fillMaxWidth().clickable(onClick = onClick).padding(vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        BankBadge(a.bank, size = 32.dp)
        Spacer(Modifier.width(12.dp))
        Column(Modifier.weight(1f)) {
            Text(a.account.name, style = MaterialTheme.typography.bodyLarge, maxLines = 1, overflow = TextOverflow.Ellipsis)
            Text(
                if (a.isCard) "Outstanding" else a.account.type.label(),
                style = MaterialTheme.typography.bodySmall,
                color = SumvaloTheme.colors.subtleText,
            )
        }
        AmountText(if (a.isCard) a.outstanding else a.balance, style = AmountStyles.medium, hidden = hidden)
    }
}

@Composable
fun BillMiniCard(b: BillItem, hidden: Boolean, onClick: () -> Unit) {
    SumvaloCard(Modifier.width(220.dp), onClick = onClick, contentPadding = PaddingValues(horizontal = 14.dp, vertical = 2.dp)) {
        BillListRow(b, hidden, onClick)
    }
}

/** A transaction row with the standard subtitle. */
@Composable
fun TxnRow(t: TransactionItem, hidden: Boolean, modifier: Modifier = Modifier, onClick: () -> Unit) {
    val title = t.txn.merchant ?: t.category?.name ?: "Transaction"
    val account = t.account?.label() ?: ""
    val sub = buildString {
        if (t.txn.type == TxnType.TRANSFER && t.toAccount != null) append("${t.account?.name ?: ""} → ${t.toAccount.name}")
        else append(account)
        append(" · ")
        append(Fmt.time(t.txn.timestamp))
    }
    TransactionRow(
        title = title,
        subtitle = sub,
        amount = t.txn.amount,
        type = t.txn.type,
        iconName = t.category?.icon,
        categoryColor = t.category.color(),
        modifier = modifier,
        source = t.txn.source,
        unreviewed = !t.txn.reviewed,
        hidden = hidden,
        onClick = onClick,
    )
}

/** Inputs of the salary-plan sheet: what the user entered and what Sumvalo detected. */
data class SalaryPlanInfo(
    val salaryEntered: Long? = null,
    val salaryDetected: Long? = null,
    val savingsTarget: Long? = null,
    val payDay: Int = 1,
    /** When the next salary is expected and why it moved, e.g. "Next salary Fri 23 Oct (25 Oct is a Sunday, …)". */
    val paydayNote: String? = null,
)

/** When the next salary is expected and why it isn't on its due day; null for calendar months. */
internal fun paydayNote(cycle: com.acuranza.app.finance.PayCycle, holidayNames: Map<java.time.LocalDate, String>, today: java.time.LocalDate): String? {
    if (cycle.dueDay <= 1) return null
    val next = cycle.nextPayday(today)
    val why = cycle.reason(cycle.periodContaining(next).month, holidayNames)
    val day = java.time.format.DateTimeFormatter.ofPattern("EEE d MMM", java.util.Locale.ENGLISH).format(next)
    return "Next salary $day" + (why?.let { " ($it)" } ?: "")
}

@Composable
private fun SafeToSpendCard(safe: com.acuranza.app.repo.SafeToSpend.Result, hidden: Boolean, onClick: () -> Unit) {
    val over = safe.available < 0
    val money = { p: Long -> if (hidden) "₹ ••••" else com.acuranza.app.model.Money.format(p, showPaise = false) }
    SumvaloCard(onClick = onClick) {
        Row(verticalAlignment = Alignment.Top) {
            Column(Modifier.weight(1f)) {
                Text(if (over) "Over plan" else "Left to spend", style = MaterialTheme.typography.bodyMedium, color = SumvaloTheme.colors.subtleText)
                AmountText(
                    if (over) -safe.available else safe.available,
                    style = AmountStyles.large,
                    hidden = hidden,
                    color = if (over) SumvaloTheme.colors.alert else androidx.compose.ui.graphics.Color.Unspecified,
                )
                if (!over) {
                    Text("≈ ${money(safe.perDay)} a day", style = MaterialTheme.typography.bodySmall, color = SumvaloTheme.colors.subtleText)
                }
            }
            Column(horizontalAlignment = Alignment.End) {
                Text("until payday", style = MaterialTheme.typography.bodySmall, color = SumvaloTheme.colors.subtleText)
                Text(Fmt.date(safe.payday), style = MaterialTheme.typography.bodyMedium, fontWeight = androidx.compose.ui.text.font.FontWeight.SemiBold)
                Text(if (safe.days == 1) "1 day" else "${safe.days} days", style = MaterialTheme.typography.bodySmall, color = SumvaloTheme.colors.subtleText)
            }
        }
        Spacer(Modifier.height(10.dp))
        val start = when (safe.basis) {
            com.acuranza.app.repo.SafeToSpend.Basis.SALARY -> "${money(safe.plan)} salary − ${money(safe.savings)} to save"
            com.acuranza.app.repo.SafeToSpend.Basis.BUDGET -> "${money(safe.plan)} budget"
            com.acuranza.app.repo.SafeToSpend.Basis.INCOME -> "${money(safe.plan)} income this month"
            com.acuranza.app.repo.SafeToSpend.Basis.USUAL -> "Your usual ${money(safe.plan)} a month"
        }
        val due = if (safe.upcoming > 0) " − ${money(safe.upcoming)} due before payday" else ""
        Text(
            "$start − ${money(safe.spent)} spent since payday$due",
            style = MaterialTheme.typography.bodySmall,
            color = SumvaloTheme.colors.subtleText,
        )
        if (safe.cashLimited) {
            Text(
                "Capped by your bank balance after card bills.",
                style = MaterialTheme.typography.bodySmall,
                color = if (over) SumvaloTheme.colors.alert else SumvaloTheme.colors.subtleText,
            )
        }
        if (safe.basis != com.acuranza.app.repo.SafeToSpend.Basis.SALARY) {
            Spacer(Modifier.height(6.dp))
            Text(
                "Set your salary and savings target →",
                style = MaterialTheme.typography.bodySmall,
                fontWeight = androidx.compose.ui.text.font.FontWeight.SemiBold,
                color = SumvaloTheme.colors.accent,
            )
        }
    }
}

/** Salary, pay day and monthly savings target behind "Left to spend". */
@OptIn(androidx.compose.material3.ExperimentalMaterial3Api::class)
@Composable
private fun SalaryPlanSheet(
    info: SalaryPlanInfo,
    onDismiss: () -> Unit,
    onSave: (salary: Long?, savingsTarget: Long, payDay: Int) -> Unit,
    onClear: () -> Unit,
    onUpcoming: () -> Unit,
) {
    var salary by remember { mutableStateOf(info.salaryEntered ?: info.salaryDetected) }
    var save by remember { mutableStateOf(info.savingsTarget) }
    var payDay by remember { mutableIntStateOf(info.payDay) }
    val money = { p: Long -> com.acuranza.app.model.Money.format(p, showPaise = false) }
    androidx.compose.material3.ModalBottomSheet(onDismissRequest = onDismiss) {
        Column(
            Modifier.padding(horizontal = 20.dp).navigationBarsPadding().verticalScroll(androidx.compose.foundation.rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Text("Your monthly plan", style = MaterialTheme.typography.titleMedium, fontWeight = androidx.compose.ui.text.font.FontWeight.SemiBold)
            Text(
                "Sumvalo takes what you want to save off your salary first, then shows what's left to spend until your next payday.",
                style = MaterialTheme.typography.bodySmall,
                color = SumvaloTheme.colors.subtleText,
            )
            com.acuranza.app.ui.common.AmountField("Take-home salary", salary, { salary = it }, allowZero = false)
            info.salaryDetected?.let {
                Text("Sumvalo saw ${money(it)} credited as salary.", style = MaterialTheme.typography.bodySmall, color = SumvaloTheme.colors.subtleText)
            }
            com.acuranza.app.ui.common.ChoiceField("Salary arrives on", (1..28).toList(), payDay, { "Day $it of the month" }, { payDay = it })
            if (payDay > 1) {
                Text(
                    (info.paydayNote?.takeIf { payDay == info.payDay }?.let { "$it. " } ?: "") +
                        "On a weekend or holiday, Sumvalo expects salary the working day before, and follows the day it actually " +
                        "arrives. Weekends and holidays are in More › Budget month.",
                    style = MaterialTheme.typography.bodySmall,
                    color = SumvaloTheme.colors.subtleText,
                )
            }
            com.acuranza.app.ui.common.AmountField("Save each month", save, { save = it })
            val base = salary
            if (base != null && base > 0) {
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    listOf(10, 20, 30, 40).forEach { pct ->
                        val amount = base * pct / 100 / 100 * 100 // whole rupees
                        androidx.compose.material3.FilterChip(
                            selected = save == amount,
                            onClick = { save = amount },
                            label = { Text("$pct%") },
                        )
                    }
                }
                val s2 = save
                if (s2 != null) {
                    Text(
                        if (s2 > base) "That's more than your salary."
                        else "That leaves ${money(base - s2)} a month for spending, bills and EMIs.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = if (s2 > base) SumvaloTheme.colors.alert else SumvaloTheme.colors.subtleText,
                    )
                }
            }
            com.acuranza.app.designsystem.components.PrimaryButton(
                "Save plan",
                enabled = (salary ?: 0) > 0 && save != null && (save ?: 0) <= (salary ?: 0),
                modifier = Modifier.fillMaxWidth(),
                onClick = {
                    // A salary equal to what Sumvalo detected is left automatic, so a raise is picked up by itself.
                    val entered = salary?.takeIf { it != info.salaryDetected }
                    onSave(entered, save ?: 0, payDay)
                },
            )
            Row {
                androidx.compose.material3.TextButton(onClick = onUpcoming) { Text("See payments due") }
                Spacer(Modifier.weight(1f))
                if (info.savingsTarget != null) androidx.compose.material3.TextButton(onClick = onClear) { Text("Clear plan", color = SumvaloTheme.colors.alert) }
            }
            Spacer(Modifier.height(12.dp))
        }
    }
}

/** Why the income and spending figures leave out some credits, or null when nothing was left out. */
internal fun moneyInNote(givenBack: Long, received: Long, hidden: Boolean): String? {
    val money = { p: Long -> if (hidden) "₹ ••••" else com.acuranza.app.model.Money.format(p, showPaise = false) }
    val parts = listOfNotNull(
        givenBack.takeIf { it > 0 }?.let { "${money(it)} refunds and cashback came off spending" },
        received.takeIf { it > 0 }?.let { "${money(it)} received from others isn't counted as income" },
    )
    return parts.takeIf { it.isNotEmpty() }?.joinToString(" · ")
}
