package com.acuranza.app.ui.home

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.AccountBalance
import androidx.compose.material.icons.outlined.ChevronRight
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.acuranza.app.designsystem.components.AmountText
import com.acuranza.app.designsystem.components.BarChart
import com.acuranza.app.designsystem.components.ChartSlice
import com.acuranza.app.designsystem.components.DonutChart
import com.acuranza.app.designsystem.components.EmptyState
import com.acuranza.app.designsystem.components.InsightCard
import com.acuranza.app.designsystem.components.RangeChips
import com.acuranza.app.designsystem.components.SectionHeader
import com.acuranza.app.designsystem.components.SpendHeatmap
import com.acuranza.app.designsystem.components.SumvaloCard
import com.acuranza.app.designsystem.components.TrendChart
import com.acuranza.app.designsystem.theme.AmountStyles
import com.acuranza.app.designsystem.theme.SumvaloText
import com.acuranza.app.designsystem.theme.SumvaloTheme
import com.acuranza.app.model.TxnType
import com.acuranza.app.ui.common.Fmt
import com.acuranza.app.ui.common.color
import com.acuranza.app.ui.common.toUi

/**
 * Stateless Home screen, rendered from [HomeState]; used by the app and by screenshot tests.
 * The redesigned dashboard: new header/date strip/net-worth hero and the reference's section
 * treatments, while every [HomeSection] stays user-reorderable and hideable as before.
 */
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
    onPlanning: () -> Unit = {},
    listState: LazyListState = rememberLazyListState(),
) {
    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        state = listState,
        contentPadding = PaddingValues(bottom = 8.dp),
    ) {
        item(key = "header") {
            HomeHeader(state, onNotifications, onProfile)
        }

        item(key = "hero") {
            NetWorthHero(state, hidden, onToggleHidden)
        }

        // Action banners and the first-run card sit directly under the balance card: they ask the
        // user for something, so they never compete with charts for attention.
        if (state.ignoredSms > 0) {
            item(key = "ignored-sms") {
                com.acuranza.app.ui.accounts.IgnoredSmsBanner(
                    state.ignoredSms, onIgnoredSms, onDismissIgnoredSms,
                    Modifier.padding(horizontal = 16.dp).padding(top = 16.dp),
                )
            }
        }
        if (state.pendingAccounts > 0) {
            item(key = "pending-accounts") {
                com.acuranza.app.ui.accounts.PendingAccountsBanner(
                    state.pendingAccounts, onPendingAccounts,
                    Modifier.padding(horizontal = 16.dp).padding(top = 16.dp),
                )
            }
        }
        if (state.unreviewed > 0) {
            item(key = "review") {
                ReviewBanner(
                    state.unreviewed, Modifier.padding(horizontal = 16.dp).padding(top = 16.dp),
                    onReview, state.reviewFromSms, state.reviewFromEmail,
                )
            }
        }
        if (state.uncategorised > UNCATEGORISED_BANNER_MIN) {
            item(key = "uncategorised") {
                com.acuranza.app.ui.transactions.UncategorisedBanner(
                    state.uncategorised, onUncategorised,
                    Modifier.padding(horizontal = 16.dp).padding(top = 12.dp),
                )
            }
        }
        if (state.isEmpty) {
            item(key = "empty") {
                Column(Modifier.padding(horizontal = 16.dp).padding(top = 24.dp)) {
                    SumvaloCard {
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
        }

        // The dashboard below is the user's own layout (Settings → Appearance → Edit home):
        // every section keeps its data conditions, order and visibility come from the saved layout,
        // and animateItem() makes reordering and show/hide feel fluid instead of jumpy.
        state.sections.forEach { section ->
            when (section) {
                HomeSection.QUICK -> item(key = "quick") {
                    Box(Modifier.animateItem()) {
                        QuickActionsGrid(state, onActivity, onBills, onPlanning, onAccounts, onTransfer, onReports)
                    }
                }
                HomeSection.SAFE -> state.safeToSpend?.let { safe -> item(key = "safe") {
                    Column(Modifier.animateItem().padding(horizontal = 16.dp).padding(top = 20.dp)) {
                        SafeToSpendCard(safe, hidden, onSafeToSpend)
                    }
                } }
                HomeSection.STATS -> item(key = "stats") {
                    Box(Modifier.animateItem()) {
                        MonthlySpendingCard(state, hidden, onSafeToSpend)
                    }
                }
                HomeSection.PACE -> if (state.charts.hasPace) item(key = "pace") {
                    Box(Modifier.animateItem()) {
                        PaceSectionV2(state, hidden, onActivity)
                    }
                }
                HomeSection.BILLS -> if (state.bills.isNotEmpty()) item(key = "bills") {
                    Box(Modifier.animateItem()) {
                        BillsSectionV2(state, hidden, onBills)
                    }
                }
                HomeSection.BUDGETS -> if (state.budgets.any { !it.isOverall }) item(key = "budgets") {
                    Box(Modifier.animateItem()) {
                        BudgetsSectionV2(state, hidden, onPlanning)
                    }
                }
                HomeSection.INSIGHT -> state.insights.firstOrNull()?.let { top -> item(key = "insight") {
                    Column(Modifier.animateItem().padding(horizontal = 16.dp).padding(top = 20.dp)) {
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
                        Column(Modifier.animateItem().padding(horizontal = 16.dp).padding(top = 20.dp)) {
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
                    Column(Modifier.animateItem().padding(horizontal = 16.dp).padding(top = 20.dp)) {
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
                    Column(Modifier.animateItem().padding(horizontal = 16.dp).padding(top = 20.dp)) {
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
                    Column(Modifier.animateItem().padding(horizontal = 16.dp).padding(top = 20.dp)) {
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
                            RangeChips(listOf("1D", "1W", "1M", "6M", "1Y", "ALL"), range.ordinal, onSelect = { range = TrendRange.values()[it] })
                        }
                    }
                }
                HomeSection.RECENT -> if (state.recent.isNotEmpty()) item(key = "recent") {
                    Column(Modifier.animateItem().padding(horizontal = 16.dp).padding(top = 20.dp)) {
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
                    Column(Modifier.animateItem().padding(horizontal = 16.dp).padding(top = 20.dp)) {
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
