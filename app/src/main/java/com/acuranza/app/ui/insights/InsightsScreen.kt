package com.acuranza.app.ui.insights

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateContentSize
import androidx.compose.animation.core.tween
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.AutoAwesome
import androidx.compose.material.icons.outlined.ExpandLess
import androidx.compose.material.icons.outlined.ExpandMore
import androidx.compose.material.icons.outlined.Restore
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.selected
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.ViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewModelScope
import androidx.navigation.NavController
import com.acuranza.app.designsystem.components.EmptyState
import com.acuranza.app.designsystem.components.IconBubble
import com.acuranza.app.designsystem.components.SumvaloCard
import com.acuranza.app.designsystem.components.MiniBarChart
import com.acuranza.app.designsystem.components.MiniLineChart
import com.acuranza.app.designsystem.components.ProgressRing
import com.acuranza.app.designsystem.components.SwipeActionsRow
import com.acuranza.app.designsystem.components.accent
import com.acuranza.app.designsystem.theme.SumvaloText
import com.acuranza.app.designsystem.theme.SumvaloTheme
import com.acuranza.app.di.AppContainer
import com.acuranza.app.finance.Insight
import com.acuranza.app.finance.InsightAction
import com.acuranza.app.finance.InsightChart
import com.acuranza.app.finance.InsightMetric
import com.acuranza.app.finance.InsightRoute
import com.acuranza.app.finance.Severity
import com.acuranza.app.ui.common.SumvaloScreen
import com.acuranza.app.ui.common.LocalHideAmounts
import com.acuranza.app.ui.common.acuranzaViewModel
import com.acuranza.app.ui.common.rememberHaptic
import com.acuranza.app.ui.common.toUi
import com.acuranza.app.ui.nav.Routes
import com.acuranza.app.ui.transactions.ActivityFilterRequest
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import com.acuranza.app.util.Time
import com.acuranza.app.util.Period
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.flatMapLatest
import com.acuranza.app.ui.nav.popBackSafely
import com.acuranza.app.ui.nav.navigateToTab

@OptIn(kotlinx.coroutines.ExperimentalCoroutinesApi::class)
class InsightsViewModel(private val c: AppContainer) : ViewModel() {
    val insights: StateFlow<List<Insight>?> = c.insights.observeInsights().map<List<Insight>, List<Insight>?> { it }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), null)

    /** The selected analysis window for the comparison card. */
    val range = kotlinx.coroutines.flow.MutableStateFlow(InsightRange.M1)
    fun setRange(r: InsightRange) { range.value = r }

    val comparison: kotlinx.coroutines.flow.StateFlow<PeriodComparison?> = range
        .flatMapLatest { r ->
            c.settings.settings.map { it.payCycle }.distinctUntilChanged().flatMapLatest { cycle ->
                kotlinx.coroutines.flow.flow {
                    val today = Time.today()
                    val current = if (r.months == 1) {
                        Period.containing(today, cycle)
                    } else {
                        Period(today.minusMonths((r.months - 1).toLong()).withDayOfMonth(1), today.plusDays(1))
                    }
                    val previous = if (r.months == 1) {
                        current.previous()
                    } else {
                        Period(current.start.minusMonths(r.months.toLong()), current.start)
                    }
                    val cur = runCatching { c.transactions.summary(current) }.getOrNull()
                    val prev = runCatching { c.transactions.summary(previous) }.getOrNull()
                    if (cur != null) emit(PeriodComparison(r, cur, prev))
                }
            }
        }
        .stateIn(viewModelScope, kotlinx.coroutines.flow.SharingStarted.WhileSubscribed(5_000), null)

    fun dismiss(id: String) = viewModelScope.launch { c.insights.dismiss(id) }
    fun restoreDismissed() = viewModelScope.launch { c.settings.clearDismissedInsights() }
    suspend fun regenerate() = c.insights.refresh()
}

private fun Severity.heading(): String = when (this) {
    Severity.ALERT -> "Needs attention"
    Severity.WARNING -> "Heads up"
    Severity.TIP -> "Tips"
    Severity.GOOD -> "Going well"
}

/** Navigates to where an insight's action points. */
fun openInsightAction(nav: NavController, action: InsightAction) {
    when (action.route) {
        InsightRoute.CATEGORY -> {
            ActivityFilterRequest.pendingCategory = action.argument
            nav.navigateToTab(Routes.TRANSACTIONS)
        }
        InsightRoute.ACTIVITY -> nav.navigateToTab(Routes.TRANSACTIONS)
        InsightRoute.BUDGETS, InsightRoute.GOALS -> {
            com.acuranza.app.ui.planning.PlanningTabRequest.pending = if (action.route == InsightRoute.GOALS) 1 else 0
            nav.navigateToTab(Routes.PLANNING)
        }
        InsightRoute.BILLS -> nav.navigate(Routes.BILLS)
        InsightRoute.ACCOUNTS -> nav.navigate(Routes.ACCOUNTS)
    }
}

/** Selectable analysis window for the income-vs-spending comparison. */
enum class InsightRange(val months: Int, val label: String) {
    M1(1, "1M"), M3(3, "3M"), M6(6, "6M"), Y1(12, "1Y"),
}

/** The selected window's totals beside the equivalent previous window's, for the delta card. */
data class PeriodComparison(
    val range: InsightRange,
    val current: com.acuranza.app.repo.MonthSummary,
    val previous: com.acuranza.app.repo.MonthSummary?,
) {
    /** Percent change vs the previous window; null when the previous window had nothing to compare. */
    fun delta(of: (com.acuranza.app.repo.MonthSummary) -> Long): Double? {
        val prev = previous ?: return null
        val base = of(prev).toDouble().let { if (it == 0.0) return null else kotlin.math.abs(it) }
        return (of(current) - of(prev)) / base * 100.0
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun InsightsScreen(nav: NavController) {
    val vm = acuranzaViewModel { InsightsViewModel(it) }
    val insights by vm.insights.collectAsStateWithLifecycle()
    val range by vm.range.collectAsState()
    val comparison by vm.comparison.collectAsState()
    val scope = rememberCoroutineScope()
    val haptic = rememberHaptic()
    var refreshing by remember { mutableStateOf(false) }

    SumvaloScreen(
        title = "Insights",
        subtitle = "Worked out on your phone, from your own data",
        actions = { IconButton(onClick = vm::restoreDismissed) { Icon(Icons.Outlined.Restore, contentDescription = "Show dismissed insights") } },
    ) { padding ->
        PullToRefreshBox(
            isRefreshing = refreshing,
            onRefresh = {
                refreshing = true
                scope.launch {
                    vm.regenerate()
                    delay(400)
                    refreshing = false
                }
            },
            modifier = Modifier.fillMaxSize().padding(padding),
        ) {
            InsightsContent(
                insights = insights,
                hidden = LocalHideAmounts.current,
                range = range,
                comparison = comparison,
                onRange = vm::setRange,
                onDismiss = { haptic(); vm.dismiss(it.id) },
                onAction = { openInsightAction(nav, it) },
            )
        }
    }
}

/** Stateless insights list: range selector, comparison, month summary, severity-grouped cards. */
@Composable
fun InsightsContent(
    insights: List<Insight>?,
    hidden: Boolean,
    range: InsightRange = InsightRange.M1,
    comparison: PeriodComparison? = null,
    onRange: (InsightRange) -> Unit = {},
    onDismiss: (Insight) -> Unit = {},
    onAction: (InsightAction) -> Unit = {},
    expandedByDefault: Set<String> = emptySet(),
    listState: androidx.compose.foundation.lazy.LazyListState = androidx.compose.foundation.lazy.rememberLazyListState(),
) {
    val list = insights
    if (list != null && list.isEmpty()) {
        EmptyState(
            "No insights right now",
            "As you add transactions, budgets and goals, Sumvalo spots overspending, subscriptions, idle cash and more.",
            Modifier.padding(top = 60.dp),
            icon = Icons.Outlined.AutoAwesome,
        )
        return
    }
    val summary = list.orEmpty().firstOrNull { it.isSummary }
    val cards = list.orEmpty().filterNot { it.isSummary }
    LazyColumn(state = listState, contentPadding = PaddingValues(16.dp, 4.dp, 16.dp, 40.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
        item(key = "range") { RangeSelector(range, onRange, Modifier.padding(vertical = 4.dp)) }
        comparison?.let { cmp -> item(key = "comparison") { ComparisonCard(cmp, hidden) } }
        summary?.let { s -> item(key = "summary") { SummaryCard(s, hidden) } }
        summary?.chart?.let { chart -> item(key = "summary-chart") { SummaryChartCard(chart, hidden) } }
        cards.groupBy { it.severity }.toSortedMap().forEach { (sev, items) ->
            item(key = "h-$sev") {
                Text(
                    sev.heading().uppercase(),
                    style = SumvaloText.sectionLabel,
                    color = sev.toUi().accent().first,
                    modifier = Modifier.padding(top = 10.dp, start = 4.dp),
                )
            }
            item(key = "g-$sev") { GroupCard(items, hidden, onDismiss, onAction, expandedByDefault) }
        }
        if (cards.isNotEmpty()) {
            item(key = "hint") {
                Text("Tap an insight for details · swipe left to dismiss", style = MaterialTheme.typography.bodySmall,
                    color = SumvaloTheme.colors.subtleText, modifier = Modifier.padding(top = 10.dp, start = 4.dp))
            }
        }
    }
}

/** Full-width segmented analysis-window selector (bordered container, filled active segment). */
@Composable
private fun RangeSelector(range: InsightRange, onRange: (InsightRange) -> Unit, modifier: Modifier = Modifier) {
    val haptic = rememberHaptic()
    val colors = SumvaloTheme.colors
    Row(
        modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(10.dp))
            .border(1.dp, colors.hairline, RoundedCornerShape(10.dp))
            .background(colors.cardSurface)
            .padding(2.dp),
    ) {
        InsightRange.entries.forEach { r ->
            val active = r == range
            Box(
                Modifier
                    .weight(1f)
                    .clip(RoundedCornerShape(8.dp))
                    .background(if (active) colors.fill else Color.Transparent)
                    .clickable { if (!active) { haptic.tick(); onRange(r) } }
                    .padding(vertical = 7.dp)
                    .semantics { role = Role.Tab; this.selected = active },
                contentAlignment = Alignment.Center,
            ) {
                Text(
                    r.label,
                    style = MaterialTheme.typography.labelMedium,
                    color = if (active) colors.ink else colors.subtleText,
                    fontWeight = if (active) FontWeight.SemiBold else FontWeight.Medium,
                    maxLines = 1,
                )
            }
        }
    }
}

/** Income vs spending for the selected window, with deltas against the previous window. */
@Composable
private fun ComparisonCard(cmp: PeriodComparison, hidden: Boolean) {
    val colors = SumvaloTheme.colors
    fun money(v: Long): String = if (hidden) "₹ ••••" else com.acuranza.app.model.Money.format(v, showPaise = false)
    SumvaloCard {
        Column(Modifier.fillMaxWidth().padding(16.dp)) {
            Text(
                "Income vs spending · ${cmp.range.label}",
                style = MaterialTheme.typography.titleSmall,
                color = colors.ink,
                fontWeight = FontWeight.SemiBold,
            )
            Spacer(Modifier.height(12.dp))
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                ComparisonMetric("Income", money(cmp.current.income), cmp.delta { it.income }, Modifier.weight(1f))
                ComparisonMetric("Spending", money(cmp.current.expense), cmp.delta { it.expense }, Modifier.weight(1f))
                ComparisonMetric("Savings", money(cmp.current.savings), cmp.delta { it.savings }, Modifier.weight(1f))
            }
            Spacer(Modifier.height(12.dp))
            HorizontalDivider(color = colors.hairline, thickness = 1.dp)
            Spacer(Modifier.height(10.dp))
            val rate = cmp.current.savingsRate
            if (rate != null && !hidden) {
                Text(
                    "Savings rate ${(rate * 100).toInt()}% of income" +
                        (cmp.previous?.savingsRate?.let { prev ->
                            if (prev > 0) " · was ${(prev * 100).toInt()}%" else ""
                        } ?: ""),
                    style = MaterialTheme.typography.bodySmall,
                    color = colors.subtleText,
                )
            }
        }
    }
}

@Composable
private fun ComparisonMetric(label: String, value: String, deltaPct: Double?, modifier: Modifier = Modifier) {
    val colors = SumvaloTheme.colors
    Column(modifier) {
        Text(label, style = MaterialTheme.typography.bodySmall, color = colors.subtleText)
        Spacer(Modifier.height(2.dp))
        Text(value, style = MaterialTheme.typography.titleMedium, color = colors.ink, fontWeight = FontWeight.SemiBold)
        if (deltaPct != null) {
            val arrow = if (deltaPct >= 0) "▲" else "▼"
            val good = if (label == "Spending") deltaPct <= 0 else deltaPct >= 0
            Spacer(Modifier.height(2.dp))
            Text(
                "$arrow ${kotlin.math.abs(deltaPct).toInt()}% vs prev",
                style = MaterialTheme.typography.labelSmall,
                color = if (deltaPct == 0.0) colors.subtleText else if (good) colors.income else colors.alert,
            )
        } else {
            Spacer(Modifier.height(2.dp))
            Text("—", style = MaterialTheme.typography.labelSmall, color = colors.subtleText)
        }
    }
}

@Composable
private fun SummaryCard(s: Insight, hidden: Boolean) {
    val colors = SumvaloTheme.colors
    val score = s.metrics.firstOrNull { it.label == "Score" }?.value?.substringBefore('/')?.toIntOrNull()
    val strip = s.metrics.filterNot { it.label == "Score" }
    SumvaloCard {
        Row(verticalAlignment = Alignment.CenterVertically) {
            if (score != null) {
                ProgressRing(
                    score / 100f,
                    size = 44.dp,
                    thickness = 4.dp,
                    brush = SolidColor(colors.ink),
                    trackColor = colors.hairline,
                ) {
                    Text("$score", style = MaterialTheme.typography.titleSmall, color = colors.ink, fontWeight = FontWeight.Bold)
                }
                Spacer(Modifier.width(14.dp))
            }
            Column(Modifier.weight(1f)) {
                Text("THIS MONTH", style = SumvaloText.sectionLabel, color = colors.subtleText)
                Text(s.title, style = MaterialTheme.typography.titleMedium, color = colors.ink, fontWeight = FontWeight.SemiBold)
            }
        }
        Spacer(Modifier.height(12.dp))
        val body = if (hidden) s.message.maskAmounts() else (s.detail ?: s.message)
        Text(boldAmounts(body), style = MaterialTheme.typography.bodyMedium, color = colors.subtleText)
        if (strip.isNotEmpty()) {
            Spacer(Modifier.height(12.dp))
            StatStrip(strip, hidden)
        }
    }
}

/** The month summary's figures as a bordered strip with hairline dividers. */
@Composable
private fun StatStrip(metrics: List<InsightMetric>, hidden: Boolean) {
    val colors = SumvaloTheme.colors
    Row(
        Modifier
            .fillMaxWidth()
            .height(IntrinsicSize.Min)
            .clip(RoundedCornerShape(10.dp))
            .border(1.dp, colors.hairline, RoundedCornerShape(10.dp)),
    ) {
        metrics.forEachIndexed { index, m ->
            if (index > 0) Box(Modifier.width(1.dp).fillMaxHeight().background(colors.hairline))
            Column(Modifier.weight(1f).padding(horizontal = 10.dp, vertical = 8.dp)) {
                Text(m.label, style = MaterialTheme.typography.labelSmall, color = colors.subtleText, maxLines = 1, overflow = TextOverflow.Ellipsis)
                Spacer(Modifier.height(2.dp))
                Text(
                    if (hidden) m.value.maskAmounts() else m.value,
                    style = MaterialTheme.typography.labelLarge,
                    color = colors.ink,
                    fontWeight = FontWeight.SemiBold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            }
        }
    }
}

/** The summary's six-month spending bars as their own card, with dashed guides and month labels. */
@Composable
private fun SummaryChartCard(chart: InsightChart, hidden: Boolean) {
    val colors = SumvaloTheme.colors
    var selected by remember(chart.values) { mutableStateOf<Int?>(null) }
    val idx = selected ?: chart.values.lastIndex
    Column(
        Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .background(colors.cardSurface)
            .border(1.dp, colors.hairline, RoundedCornerShape(16.dp))
            .padding(16.dp),
    ) {
        Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
            Text(
                chart.caption ?: "",
                style = MaterialTheme.typography.titleSmall,
                color = colors.ink,
                fontWeight = FontWeight.SemiBold,
                modifier = Modifier.weight(1f),
            )
            Text(chart.labels.getOrNull(idx).orEmpty(), style = MaterialTheme.typography.labelMedium, color = colors.subtleText)
            Spacer(Modifier.width(6.dp))
            val v = chart.values.getOrNull(idx) ?: 0L
            Text(
                if (hidden) "₹ ••••" else com.acuranza.app.model.Money.compact(v),
                style = MaterialTheme.typography.labelLarge,
                color = colors.ink,
                fontWeight = FontWeight.SemiBold,
            )
        }
        Spacer(Modifier.height(12.dp))
        Canvas(
            Modifier
                .fillMaxWidth()
                .height(120.dp)
                .pointerInput(chart.values) {
                    val width = size.width.toFloat()
                    detectTapGestures { pos ->
                        if (chart.values.isNotEmpty()) {
                            selected = (pos.x / (width / chart.values.size)).toInt().coerceIn(0, chart.values.lastIndex)
                        }
                    }
                },
        ) {
            if (chart.values.isEmpty()) return@Canvas
            // Dashed guides at the top, a third and two thirds down, like the design.
            listOf(0f, 1f / 3f, 2f / 3f).forEach { f ->
                val y = size.height * f
                drawLine(
                    colors.hairline, Offset(0f, y), Offset(size.width, y), 1.dp.toPx(),
                    pathEffect = PathEffect.dashPathEffect(floatArrayOf(5.dp.toPx(), 5.dp.toPx())),
                )
            }
            val top = ((chart.values.maxOrNull() ?: 1L).coerceAtLeast(1L)).toFloat() * 1.12f
            val slot = size.width / chart.values.size
            val barW = 28.dp.toPx()
            chart.values.forEachIndexed { i, v ->
                val bh = (v.coerceAtLeast(0L).toFloat() / top) * size.height
                val hot = i == chart.values.lastIndex || i == selected
                drawRoundRect(
                    if (hot) colors.ink else colors.hairline,
                    topLeft = Offset(i * slot + (slot - barW) / 2f, size.height - bh),
                    size = Size(barW, bh),
                    cornerRadius = CornerRadius(4.dp.toPx(), 4.dp.toPx()),
                )
            }
            drawLine(colors.hairline, Offset(0f, size.height - 0.5f), Offset(size.width, size.height - 0.5f), 1.dp.toPx())
        }
        Spacer(Modifier.height(8.dp))
        Row(Modifier.fillMaxWidth()) {
            chart.labels.forEachIndexed { i, label ->
                val hot = i == chart.values.lastIndex || i == selected
                Text(
                    label,
                    style = MaterialTheme.typography.labelSmall,
                    color = if (hot) colors.ink else colors.subtleText,
                    fontWeight = if (hot) FontWeight.SemiBold else FontWeight.Medium,
                    textAlign = androidx.compose.ui.text.style.TextAlign.Center,
                    modifier = Modifier.weight(1f),
                )
            }
        }
    }
}

/** One bordered container per severity, rows divided by hairlines. */
@Composable
private fun GroupCard(
    items: List<Insight>,
    hidden: Boolean,
    onDismiss: (Insight) -> Unit,
    onAction: (InsightAction) -> Unit,
    expandedByDefault: Set<String>,
) {
    val shape = RoundedCornerShape(16.dp)
    Column(
        Modifier
            .fillMaxWidth()
            .clip(shape)
            .background(SumvaloTheme.colors.cardSurface)
            .border(1.dp, SumvaloTheme.colors.hairline, shape),
    ) {
        items.forEachIndexed { index, i ->
            if (index > 0) HorizontalDivider(color = SumvaloTheme.colors.hairline, thickness = 1.dp)
            SwipeActionsRow(onDelete = {
                onDismiss(i)
                true
            }) {
                InsightRow(i, hidden, onAction, initiallyExpanded = i.id in expandedByDefault)
            }
        }
    }
}

@Composable
private fun InsightRow(i: Insight, hidden: Boolean, onAction: (InsightAction) -> Unit, initiallyExpanded: Boolean) {
    val expandIn = com.acuranza.app.designsystem.theme.motionTween<Float>(com.acuranza.app.designsystem.theme.Motion.STANDARD)
    val expandSize = com.acuranza.app.designsystem.theme.motionTween<androidx.compose.ui.unit.IntSize>(com.acuranza.app.designsystem.theme.Motion.STANDARD)
    val collapseFade = com.acuranza.app.designsystem.theme.motionTween<Float>(com.acuranza.app.designsystem.theme.Motion.FAST)
    val collapseSize = com.acuranza.app.designsystem.theme.motionTween<androidx.compose.ui.unit.IntSize>(com.acuranza.app.designsystem.theme.Motion.FAST)
    var expanded by rememberSaveable(i.id) { mutableStateOf(initiallyExpanded) }
    val (accent, icon) = i.severity.toUi().accent()
    val hasMore = i.detail != null || i.metrics.isNotEmpty() || i.chart != null
    Column(
        Modifier
            .fillMaxWidth()
            .clickable(enabled = hasMore) { expanded = !expanded }
            .animateContentSize(com.acuranza.app.designsystem.theme.motionTween(com.acuranza.app.designsystem.theme.Motion.STANDARD)),
    ) {
        Row(Modifier.fillMaxWidth().padding(12.dp), verticalAlignment = Alignment.Top) {
            IconBubble(color = accent, size = 32.dp, shape = RoundedCornerShape(8.dp), alpha = 0.12f) {
                Icon(icon, contentDescription = null, tint = accent, modifier = Modifier.size(18.dp))
            }
            Spacer(Modifier.width(12.dp))
            Column(Modifier.weight(1f)) {
                Text(i.title, style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.SemiBold)
                Spacer(Modifier.height(2.dp))
                Text(
                    if (hidden) i.message.maskAmounts() else i.message,
                    style = MaterialTheme.typography.bodySmall,
                    color = SumvaloTheme.colors.subtleText,
                    maxLines = if (expanded) Int.MAX_VALUE else 2,
                    overflow = TextOverflow.Ellipsis,
                )
            }
            i.action?.let { a ->
                Spacer(Modifier.width(8.dp))
                ActionPill(a.label) { onAction(a) }
            }
            if (hasMore) {
                Spacer(Modifier.width(6.dp))
                Icon(
                    if (expanded) Icons.Outlined.ExpandLess else Icons.Outlined.ExpandMore,
                    contentDescription = if (expanded) "Show less" else "Show more",
                    tint = SumvaloTheme.colors.tertiaryText,
                    modifier = Modifier.size(20.dp),
                )
            }
        }
        AnimatedVisibility(
            expanded,
            enter = fadeIn(expandIn) + expandVertically(expandSize),
            exit = fadeOut(collapseFade) + shrinkVertically(collapseSize),
        ) {
            Column(Modifier.padding(start = 44.dp, end = 12.dp, bottom = 12.dp)) {
                i.detail?.let {
                    Text(
                        boldAmounts(if (hidden) it.maskAmounts() else it),
                        style = MaterialTheme.typography.bodySmall,
                        color = SumvaloTheme.colors.subtleText,
                    )
                    Spacer(Modifier.height(12.dp))
                }
                if (i.metrics.isNotEmpty()) {
                    MetricChips(i.metrics, hidden)
                    Spacer(Modifier.height(12.dp))
                }
                i.chart?.let { InsightChartView(it, hidden) }
            }
        }
    }
}

/** Compact outlined action pill shown inline on an insight row. */
@Composable
private fun ActionPill(label: String, onClick: () -> Unit) {
    val shape = RoundedCornerShape(8.dp)
    Box(
        Modifier
            .clip(shape)
            .border(1.dp, SumvaloTheme.colors.hairline, shape)
            .clickable(onClick = onClick)
            .padding(horizontal = 10.dp, vertical = 5.dp),
    ) {
        Text(label, style = MaterialTheme.typography.labelMedium, color = SumvaloTheme.colors.ink, fontWeight = FontWeight.SemiBold)
    }
}

/** Renders rupee amounts inside free text in the emphasized voice, per the design's narrative style. */
@Composable
private fun boldAmounts(text: String): androidx.compose.ui.text.AnnotatedString = androidx.compose.ui.text.buildAnnotatedString {
    append(text)
    Regex("₹[0-9][0-9,]*(?:\\.[0-9]+)?[KLCr]*").findAll(text).forEach { m ->
        addStyle(
            androidx.compose.ui.text.SpanStyle(color = SumvaloTheme.colors.ink, fontWeight = FontWeight.SemiBold),
            m.range.first,
            m.range.last + 1,
        )
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun MetricChips(metrics: List<InsightMetric>, hidden: Boolean) {
    FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
        metrics.forEach { m ->
            Column(
                Modifier
                    .clip(RoundedCornerShape(12.dp))
                    .background(SumvaloTheme.colors.fill)
                    .padding(horizontal = 12.dp, vertical = 8.dp),
            ) {
                Text(m.label, style = MaterialTheme.typography.labelSmall, color = SumvaloTheme.colors.subtleText, maxLines = 1, overflow = TextOverflow.Ellipsis)
                Text(if (hidden) m.value.maskAmounts() else m.value, style = MaterialTheme.typography.labelLarge, fontWeight = FontWeight.SemiBold, maxLines = 1, overflow = TextOverflow.Ellipsis)
            }
        }
    }
}

@Composable
private fun InsightChartView(chart: InsightChart, hidden: Boolean) {
    Column {
        chart.caption?.let { Text(it, style = MaterialTheme.typography.labelSmall, color = SumvaloTheme.colors.tertiaryText) }
        when (chart.kind) {
            InsightChart.Kind.BARS -> MiniBarChart(chart.labels, chart.values, reference = chart.reference, hidden = hidden)
            InsightChart.Kind.LINE -> MiniLineChart(
                chart.values, reference = chart.reference, hidden = hidden,
                startLabel = chart.labels.firstOrNull(), endLabel = chart.labels.lastOrNull(),
            )
        }
    }
}

/** Replaces rupee amounts with dots for privacy mode. */
private fun String.maskAmounts(): String = replace(Regex("[−-]?₹[0-9,.]+[KLCr]*"), "₹ ••••")
