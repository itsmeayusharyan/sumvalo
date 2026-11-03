package com.acuranza.app.ui.home

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ReceiptLong
import androidx.compose.material.icons.outlined.AccountBalance
import androidx.compose.material.icons.outlined.ArrowDropDown
import androidx.compose.material.icons.outlined.Assessment
import androidx.compose.material.icons.outlined.ChevronLeft
import androidx.compose.material.icons.outlined.ChevronRight
import androidx.compose.material.icons.outlined.CompareArrows
import androidx.compose.material.icons.outlined.Notifications
import androidx.compose.material.icons.outlined.PieChart
import androidx.compose.material.icons.outlined.SwapHoriz
import androidx.compose.material.icons.outlined.Visibility
import androidx.compose.material.icons.outlined.VisibilityOff
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.selected
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.acuranza.app.R
import com.acuranza.app.designsystem.components.AmountText
import com.acuranza.app.designsystem.components.RangeChips
import com.acuranza.app.designsystem.components.SectionHeader
import com.acuranza.app.designsystem.components.Sparkline
import com.acuranza.app.designsystem.components.SpendPaceChart
import com.acuranza.app.designsystem.components.SumvaloCard
import com.acuranza.app.designsystem.components.SumvaloLogo
import com.acuranza.app.designsystem.components.formatAmount
import com.acuranza.app.designsystem.theme.SumvaloTheme
import com.acuranza.app.model.Money
import com.acuranza.app.repo.BillItem
import com.acuranza.app.ui.common.Fmt
import com.acuranza.app.ui.common.label
import com.acuranza.app.ui.common.rememberHaptic
import com.acuranza.app.ui.settings.ProfileAvatar
import com.acuranza.app.util.Time
import java.time.DayOfWeek
import java.time.temporal.TemporalAdjusters
import java.util.Locale
import kotlin.math.abs
import kotlin.math.roundToInt

/**
 * Chakra Petch — the display face of the redesigned home (bundled under res/font, SIL OFL).
 * The rupee glyph falls back to the system font per-glyph when the typeface lacks it.
 */
val Chakra = FontFamily(
    Font(R.font.chakrapetch_medium, FontWeight.Medium),
    Font(R.font.chakrapetch_semibold, FontWeight.SemiBold),
    Font(R.font.chakrapetch_bold, FontWeight.Bold),
)

/** Chakra-flavoured copy of a text style, so home headings carry the new voice. */
private fun chakra(style: TextStyle, weight: FontWeight = FontWeight.SemiBold): TextStyle =
    style.copy(fontFamily = Chakra, fontWeight = weight)

private fun chakraText(weight: FontWeight, size: Int, lineHeight: Int = size + 6, letterSpacingSp: Float = 0f): TextStyle =
    TextStyle(fontFamily = Chakra, fontWeight = weight, fontSize = size.sp, lineHeight = lineHeight.sp, letterSpacing = letterSpacingSp.sp)

private val cardShape = RoundedCornerShape(14.dp)

/** One-line horizontal hairline used between design rows. */
@Composable
private fun V2Hairline(modifier: Modifier = Modifier) {
    Box(modifier.fillMaxWidth().height(1.dp).background(MaterialTheme.colorScheme.outlineVariant))
}

/* ---------------------------------- Header ---------------------------------- */

/**
 * The redesigned header: logo tile + SUMVALO wordmark on the left, a pill with the notification
 * bell (unread badge) and the profile avatar on the right. The hide-amounts eye lives on the
 * net-worth card, as in the reference design.
 */
@Composable
internal fun HomeHeader(
    state: HomeState,
    onNotifications: () -> Unit,
    onProfile: () -> Unit,
) {
    Column(Modifier.fillMaxWidth().padding(horizontal = 16.dp).padding(top = 12.dp)) {
        Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
            Box(
                Modifier
                    .size(40.dp)
                    .clip(RoundedCornerShape(12.dp))
                    .background(SumvaloTheme.colors.cardSurface)
                    .border(1.dp, SumvaloTheme.colors.hairline, RoundedCornerShape(12.dp)),
                contentAlignment = Alignment.Center,
            ) { SumvaloLogo(size = 30.dp) }
            Spacer(Modifier.width(12.dp))
            Text(
                "SUMVALO",
                style = chakraText(FontWeight.Bold, 17, 22, 3f),
                color = SumvaloTheme.colors.ink,
            )
            Spacer(Modifier.weight(1f))
            Row(
                Modifier
                    .clip(RoundedCornerShape(50))
                    .background(SumvaloTheme.colors.cardSurface)
                    .border(1.dp, SumvaloTheme.colors.hairline, RoundedCornerShape(50))
                    .padding(horizontal = 6.dp, vertical = 5.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Box(Modifier.size(32.dp), contentAlignment = Alignment.Center) {
                    Icon(
                        Icons.Outlined.Notifications,
                        contentDescription = if (state.notificationsUnread > 0) "Notifications, ${state.notificationsUnread} unread" else "Notifications",
                        tint = SumvaloTheme.colors.subtleText,
                        modifier = Modifier
                            .size(22.dp)
                            .clip(CircleShape)
                            .clickable(role = Role.Button, onClick = onNotifications)
                            .padding(1.dp),
                    )
                    if (state.notificationsUnread > 0) {
                        Box(
                            Modifier
                                .align(Alignment.TopEnd)
                                .clip(RoundedCornerShape(50))
                                .background(SumvaloTheme.colors.ink)
                                .padding(horizontal = 4.dp, vertical = 1.dp),
                        ) {
                            Text(
                                "${state.notificationsUnread}",
                                style = chakraText(FontWeight.Bold, 9, 10),
                                color = SumvaloTheme.colors.onInk,
                            )
                        }
                    }
                }
                Spacer(Modifier.width(6.dp))
                Box(Modifier.width(1.dp).height(18.dp).background(SumvaloTheme.colors.hairline))
                Spacer(Modifier.width(6.dp))
                ProfileAvatar(
                    path = state.profileAvatar,
                    name = state.profileName,
                    size = 28.dp,
                    modifier = Modifier
                        .clip(CircleShape)
                        .clickable(role = Role.Button, onClick = onProfile),
                )
            }
        }
        Spacer(Modifier.height(18.dp))
        Text(
            Fmt.greeting() + (state.profileName?.takeIf { it.isNotBlank() }?.let { ", $it" } ?: ""),
            style = chakraText(FontWeight.Bold, 20, 26),
            color = SumvaloTheme.colors.ink,
            maxLines = 2,
            overflow = TextOverflow.Ellipsis,
        )
        Spacer(Modifier.height(12.dp))
        DateStrip()
    }
}

/** The date card: big day number, month/weekday, then the current week with today marked. */
@Composable
private fun DateStrip() {
    val today = Time.today()
    // The strip is a real date navigator: the days are tappable and the arrows step a week in
    // either direction; LocalDate arithmetic carries month and year boundaries for free.
    var selectedEpochDay by rememberSaveable { mutableStateOf(today.toEpochDay()) }
    val selected = java.time.LocalDate.ofEpochDay(selectedEpochDay)
    val monday = selected.with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY))
    Row(
        Modifier
            .fillMaxWidth()
            .height(64.dp)
            .clip(cardShape)
            .background(SumvaloTheme.colors.cardSurface)
            .border(1.dp, SumvaloTheme.colors.hairline, cardShape)
            .padding(horizontal = 14.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            "%02d".format(selected.dayOfMonth),
            style = chakraText(FontWeight.Bold, 34, 38, -0.5f),
            color = SumvaloTheme.colors.ink,
        )
        Spacer(Modifier.width(8.dp))
        Column {
            Text(
                selected.month.getDisplayName(java.time.format.TextStyle.SHORT, Locale.ENGLISH).uppercase(Locale.ENGLISH),
                style = chakraText(FontWeight.SemiBold, 10, 12, 1f),
                color = SumvaloTheme.colors.tertiaryText,
            )
            Text(
                selected.dayOfWeek.getDisplayName(java.time.format.TextStyle.SHORT, Locale.ENGLISH).uppercase(Locale.ENGLISH),
                style = chakraText(FontWeight.SemiBold, 10, 12, 1f),
                color = SumvaloTheme.colors.tertiaryText,
            )
        }
        if (selected != today) {
            Spacer(Modifier.width(8.dp))
            Text(
                "Today",
                style = chakraText(FontWeight.SemiBold, 10, 12, 0.5f),
                color = SumvaloTheme.colors.accent,
                modifier = Modifier
                    .clip(RoundedCornerShape(8.dp))
                    .clickable { selectedEpochDay = today.toEpochDay() }
                    .padding(horizontal = 6.dp, vertical = 4.dp),
            )
        }
        Spacer(Modifier.width(14.dp))
        Box(Modifier.width(1.dp).height(34.dp).background(SumvaloTheme.colors.hairline))
        Spacer(Modifier.weight(1f))
        Icon(
            Icons.Outlined.ChevronLeft,
            contentDescription = "Previous week",
            tint = SumvaloTheme.colors.tertiaryText,
            modifier = Modifier
                .size(22.dp)
                .clip(CircleShape)
                .clickable(role = Role.Button) { selectedEpochDay = selected.minusDays(7).toEpochDay() }
                .padding(2.dp),
        )
        Row(Modifier.weight(1f), horizontalArrangement = Arrangement.spacedBy(2.dp)) {
            (0 until 7).forEach { i ->
                val d = monday.plusDays(i.toLong())
                val isSelected = d == selected
                val isToday = d == today
                Column(
                    Modifier
                        .weight(1f)
                        .clip(RoundedCornerShape(8.dp))
                        .clickable(role = Role.Button) { selectedEpochDay = d.toEpochDay() }
                        .padding(vertical = 2.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                ) {
                    Text(
                        d.dayOfWeek.getDisplayName(java.time.format.TextStyle.NARROW, Locale.ENGLISH),
                        style = chakraText(if (isSelected || isToday) FontWeight.Bold else FontWeight.Medium, 10, 12),
                        color = if (isSelected || isToday) SumvaloTheme.colors.ink else SumvaloTheme.colors.tertiaryText,
                    )
                    Text(
                        "${d.dayOfMonth}",
                        style = chakraText(if (isSelected || isToday) FontWeight.Bold else FontWeight.Medium, 13, 18),
                        color = if (isSelected || isToday) SumvaloTheme.colors.ink else SumvaloTheme.colors.subtleText,
                    )
                    Box(
                        Modifier
                            .padding(top = 2.dp)
                            .size(width = 12.dp, height = 2.dp)
                            .clip(RoundedCornerShape(50))
                            .background(
                                when {
                                    isSelected -> SumvaloTheme.colors.accent
                                    isToday -> SumvaloTheme.colors.ink
                                    else -> Color.Transparent
                                },
                            ),
                    )
                }
            }
        }
        Icon(
            Icons.Outlined.ChevronRight,
            contentDescription = "Next week",
            tint = SumvaloTheme.colors.tertiaryText,
            modifier = Modifier
                .size(22.dp)
                .clip(CircleShape)
                .clickable(role = Role.Button) { selectedEpochDay = selected.plusDays(7).toEpochDay() }
                .padding(2.dp),
        )
    }
}

/**
 * The hero's four-way range selector: all options inside one bordered pill container, the active
 * range on a filled segment — the same treatment as the header's bell/profile pill controls.
 */
@Composable
private fun HeroRangeSelector(labels: List<String>, selectedIndex: Int, onSelect: (Int) -> Unit) {
    val haptic = rememberHaptic()
    val colors = SumvaloTheme.colors
    Row(
        Modifier
            .clip(RoundedCornerShape(10.dp))
            .border(1.dp, colors.hairline, RoundedCornerShape(10.dp))
            .background(colors.cardSurface)
            .padding(2.dp),
        horizontalArrangement = Arrangement.spacedBy(2.dp),
    ) {
        labels.forEachIndexed { i, label ->
            val active = i == selectedIndex
            Box(
                Modifier
                    .clip(RoundedCornerShape(8.dp))
                    .background(if (active) colors.fill else Color.Transparent)
                    .clickable { if (!active) { haptic.tick(); onSelect(i) } }
                    .padding(horizontal = 9.dp, vertical = 5.dp)
                    .semantics { role = Role.Tab; this.selected = active },
                contentAlignment = Alignment.Center,
            ) {
                Text(
                    label,
                    style = chakraText(if (active) FontWeight.SemiBold else FontWeight.Medium, 12, 16),
                    color = if (active) colors.ink else colors.subtleText,
                    maxLines = 1,
                )
            }
        }
    }
}

/* ------------------------------- Net worth hero ------------------------------ */

/**
 * The net-worth card: label with the hide-amounts eye, range chips (1M/6M/1Y/All), the big total
 * with a sparkline, the movement since last month, and the assets/liabilities split bar.
 */
@Composable
internal fun NetWorthHero(state: HomeState, hidden: Boolean, onToggleHidden: () -> Unit) {
    val nw = state.netWorth
    var range by rememberSaveable { mutableStateOf(TrendRange.M1) }
    val ranges = remember { listOf(TrendRange.M1, TrendRange.M6, TrendRange.Y1, TrendRange.ALL) }
    val labels = remember { listOf("1M", "6M", "1Y", "All") }
    val points = remember(state.charts, range) { state.charts.trendPoints(range) }

    // Movement vs the end of the previous calendar month, straight from the daily history.
    val monthStart = Time.today().withDayOfMonth(1)
    val prevPoint = state.charts.netWorth.lastOrNull { it.first.isBefore(monthStart) }
    val delta = if (nw != null && prevPoint != null) nw.total - prevPoint.second else null
    val deltaPct = if (delta != null && prevPoint != null && prevPoint.second != 0L) {
        delta * 100.0 / prevPoint.second
    } else null
    val vsMonth = monthStart.minusMonths(1).month.getDisplayName(java.time.format.TextStyle.SHORT, Locale.ENGLISH)

    val assets = nw?.liquid ?: 0
    val liabilities = (nw?.cardDebt ?: 0) + (nw?.loans ?: 0)
    val share = if (assets + liabilities > 0) (assets.toDouble() / (assets + liabilities)).toFloat() else 1f

    Column(Modifier.fillMaxWidth().padding(horizontal = 16.dp).padding(top = 16.dp)) {
        SumvaloCard(shape = cardShape, contentPadding = PaddingValues(16.dp)) {
            Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                Text(
                    "Net worth",
                    style = chakraText(FontWeight.Medium, 13, 18),
                    color = SumvaloTheme.colors.subtleText,
                )
                // The hide/show toggle sits right beside the title it controls.
                Spacer(Modifier.width(6.dp))
                Icon(
                    if (hidden) Icons.Outlined.VisibilityOff else Icons.Outlined.Visibility,
                    contentDescription = if (hidden) "Show amounts" else "Hide amounts",
                    tint = SumvaloTheme.colors.tertiaryText,
                    modifier = Modifier
                        .size(28.dp)
                        .clip(CircleShape)
                        .clickable(role = Role.Button, onClick = onToggleHidden)
                        .padding(4.dp),
                )
                Spacer(Modifier.weight(1f))
                HeroRangeSelector(labels, ranges.indexOf(range), onSelect = { range = ranges[it] })
            }
            Spacer(Modifier.height(10.dp))
            Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.Bottom) {
                AmountText(
                    paise = nw?.total ?: 0,
                    style = chakraText(FontWeight.Bold, 34, 40, -0.5f),
                    hidden = hidden,
                    animate = true,
                    modifier = Modifier.weight(1f),
                )
                if (points.size >= 2) {
                    Sparkline(
                        values = points.map { it.value.toFloat() },
                        modifier = Modifier.padding(bottom = 6.dp).size(width = 84.dp, height = 34.dp),
                        color = SumvaloTheme.colors.subtleText,
                        fill = false,
                    )
                }
            }
            if (delta != null && deltaPct != null) {
                Spacer(Modifier.height(4.dp))
                val up = delta >= 0
                val color = if (up) SumvaloTheme.colors.income else SumvaloTheme.colors.expense
                Text(
                    (if (up) "▲ " else "▼ ") + formatAmount(abs(delta), compact = true) +
                        " (" + String.format(Locale.US, "%.1f", abs(deltaPct)) + "%) vs $vsMonth",
                    style = chakraText(FontWeight.SemiBold, 13, 18),
                    color = color,
                )
            }
            Spacer(Modifier.height(14.dp))
            V2Hairline()
            Spacer(Modifier.height(12.dp))
            Row(Modifier.fillMaxWidth().height(IntrinsicSize.Min)) {
                Column(Modifier.weight(1f)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(Modifier.size(8.dp).clip(CircleShape).background(SumvaloTheme.colors.income))
                        Spacer(Modifier.width(6.dp))
                        Text("Assets", style = chakraText(FontWeight.Medium, 12, 16), color = SumvaloTheme.colors.tertiaryText)
                    }
                    Spacer(Modifier.height(3.dp))
                    AmountText(assets, style = chakraText(FontWeight.SemiBold, 16, 22), hidden = hidden)
                }
                Box(Modifier.width(1.dp).fillMaxHeight().background(SumvaloTheme.colors.hairline))
                Column(Modifier.weight(1f).padding(start = 16.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(Modifier.size(8.dp).clip(CircleShape).background(SumvaloTheme.colors.expense))
                        Spacer(Modifier.width(6.dp))
                        Text("Liabilities", style = chakraText(FontWeight.Medium, 12, 16), color = SumvaloTheme.colors.tertiaryText)
                    }
                    Spacer(Modifier.height(3.dp))
                    AmountText(liabilities, style = chakraText(FontWeight.SemiBold, 16, 22), hidden = hidden)
                }
            }
            Spacer(Modifier.height(12.dp))
            Row(
                Modifier
                    .fillMaxWidth()
                    .height(6.dp)
                    .clip(RoundedCornerShape(50))
                    .background(SumvaloTheme.colors.fill),
            ) {
                Box(Modifier.fillMaxWidth(share.coerceIn(0.02f, 0.98f)).fillMaxHeight().background(SumvaloTheme.colors.income))
                Box(Modifier.weight(1f).fillMaxHeight().background(SumvaloTheme.colors.expense))
            }
        }
    }
}

/* ------------------------------- Quick actions ------------------------------- */

private class QuickTileSpec(val title: String, val subtitle: String, val icon: ImageVector, val onClick: () -> Unit)

/** The 2×3 quick-action grid: the reference's four tiles plus Transfer and Reports. */
@Composable
internal fun QuickActionsGrid(
    state: HomeState,
    onActivity: () -> Unit,
    onBills: () -> Unit,
    onPlanning: () -> Unit,
    onAccounts: () -> Unit,
    onTransfer: () -> Unit,
    onReports: () -> Unit,
) {
    val budgetCount = state.budgets.count { !it.isOverall }
    val tiles = listOf(
        QuickTileSpec("Transactions", "${state.summary?.transactionCount ?: 0} this month", Icons.Outlined.SwapHoriz, onActivity),
        QuickTileSpec("Bills", if (state.billsDue > 0) "${state.billsDue} due soon" else "Nothing due", Icons.AutoMirrored.Outlined.ReceiptLong, onBills),
        QuickTileSpec("Budgets", if (budgetCount > 0) "$budgetCount active" else "Not set", Icons.Outlined.PieChart, onPlanning),
        QuickTileSpec("Accounts", "${state.accounts.size} linked", Icons.Outlined.AccountBalance, onAccounts),
        QuickTileSpec("Transfer", "Between accounts", Icons.Outlined.CompareArrows, onTransfer),
        QuickTileSpec("Reports", "Monthly & yearly PDFs", Icons.Outlined.Assessment, onReports),
    )
    Column(
        Modifier.fillMaxWidth().padding(horizontal = 16.dp).padding(top = 12.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        tiles.chunked(2).forEach { row ->
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                row.forEach { t -> QuickTile(t, Modifier.weight(1f)) }
                if (row.size == 1) Spacer(Modifier.weight(1f))
            }
        }
    }
}

@Composable
private fun QuickTile(t: QuickTileSpec, modifier: Modifier = Modifier) {
    Row(
        modifier
            .clip(RoundedCornerShape(12.dp))
            .background(SumvaloTheme.colors.cardSurface)
            .border(1.dp, SumvaloTheme.colors.hairline, RoundedCornerShape(12.dp))
            .clickable(role = Role.Button, onClick = t.onClick)
            .padding(horizontal = 12.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(
            Modifier
                .size(32.dp)
                .clip(RoundedCornerShape(8.dp))
                .background(MaterialTheme.colorScheme.onBackground.copy(alpha = 0.08f)),
            contentAlignment = Alignment.Center,
        ) {
            Icon(t.icon, contentDescription = null, tint = SumvaloTheme.colors.ink, modifier = Modifier.size(18.dp))
        }
        Spacer(Modifier.width(10.dp))
        Column(Modifier.weight(1f)) {
            Text(
                t.title,
                style = chakraText(FontWeight.SemiBold, 13, 18),
                color = SumvaloTheme.colors.ink,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            Text(
                t.subtitle,
                style = chakraText(FontWeight.Medium, 11, 14),
                color = SumvaloTheme.colors.tertiaryText,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        }
        Icon(Icons.Outlined.ChevronRight, contentDescription = null, tint = SumvaloTheme.colors.tertiaryText, modifier = Modifier.size(14.dp))
    }
}

/* ----------------------------- Monthly spending ------------------------------ */

/**
 * The "Monthly spending" card: a donut of budget use with Spent/Budget/Remaining rows and the
 * payday footer. Without an overall budget it falls back to income/spent/saved figures.
 */
@Composable
internal fun MonthlySpendingCard(state: HomeState, hidden: Boolean, onSetTarget: () -> Unit) {
    val s = state.summary
    val overall = state.budgets.firstOrNull { it.isOverall }
    val hasBudget = overall != null && overall.limit > 0
    val spent = s?.expense ?: 0
    val periodLabel = s?.period?.label?.let {
        it.month.getDisplayName(java.time.format.TextStyle.SHORT, Locale.ENGLISH) + " " + it.year
    } ?: ""

    Column(Modifier.fillMaxWidth().padding(horizontal = 16.dp).padding(top = 20.dp)) {
        SumvaloCard(shape = cardShape, contentPadding = PaddingValues(16.dp)) {
            Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                Text(
                    "Monthly spending",
                    style = chakraText(FontWeight.SemiBold, 14, 20),
                    color = SumvaloTheme.colors.ink,
                    modifier = Modifier.weight(1f),
                )
                Row(
                    Modifier
                        .clip(RoundedCornerShape(50))
                        .background(SumvaloTheme.colors.fill)
                        .border(1.dp, SumvaloTheme.colors.hairline, RoundedCornerShape(50))
                        .padding(horizontal = 10.dp, vertical = 4.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Text(periodLabel, style = chakraText(FontWeight.SemiBold, 12, 16), color = SumvaloTheme.colors.ink)
                    Icon(Icons.Outlined.ArrowDropDown, contentDescription = null, tint = SumvaloTheme.colors.ink, modifier = Modifier.size(14.dp))
                }
            }
            Spacer(Modifier.height(16.dp))
            if (hasBudget) {
                val limit = overall.limit
                val pct = if (limit > 0) spent.toDouble() / limit else 0.0
                Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                    SpendRing(fraction = pct, size = 96.dp, stroke = 9.dp, modifier = Modifier.size(96.dp))
                    Spacer(Modifier.width(20.dp))
                    Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                        BudgetRow("Spent", spent, SumvaloTheme.colors.ink, hidden)
                        BudgetRow("Budget", limit, SumvaloTheme.colors.ink, hidden)
                        BudgetRow("Remaining", limit - spent, if (spent > limit) SumvaloTheme.colors.expense else SumvaloTheme.colors.income, hidden)
                    }
                }
            } else {
                Row(Modifier.fillMaxWidth().height(IntrinsicSize.Min)) {
                    com.acuranza.app.designsystem.components.StatChip("Income", s?.income ?: 0, Modifier.weight(1f), type = com.acuranza.app.model.TxnType.INCOME, hidden = hidden)
                    Box(Modifier.fillMaxHeight().width(1.dp).background(SumvaloTheme.colors.divider))
                    com.acuranza.app.designsystem.components.StatChip("Spent", s?.expense ?: 0, Modifier.weight(1f).padding(start = 14.dp), type = com.acuranza.app.model.TxnType.EXPENSE, hidden = hidden)
                    Box(Modifier.fillMaxHeight().width(1.dp).background(SumvaloTheme.colors.divider))
                    com.acuranza.app.designsystem.components.StatChip("Saved", s?.savings ?: 0, Modifier.weight(1f).padding(start = 14.dp), hidden = hidden)
                }
                com.acuranza.app.ui.home.moneyInNote(s?.givenBack ?: 0, s?.received ?: 0, hidden)?.let {
                    Spacer(Modifier.height(10.dp))
                    Text(it, style = MaterialTheme.typography.bodySmall, color = SumvaloTheme.colors.subtleText)
                }
            }
            Spacer(Modifier.height(14.dp))
            V2Hairline()
            Spacer(Modifier.height(12.dp))
            Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                val safe = state.safeToSpend
                Text(
                    safe?.let { "Payday in ${it.days}d · ${Fmt.date(it.payday)}" } ?: "Set your payday in the plan",
                    style = chakraText(FontWeight.Medium, 12, 16),
                    color = SumvaloTheme.colors.tertiaryText,
                    modifier = Modifier.weight(1f),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
                Text(
                    "Set target →",
                    style = chakraText(FontWeight.SemiBold, 12, 16),
                    color = SumvaloTheme.colors.ink,
                    modifier = Modifier.clickable(role = Role.Button, onClick = onSetTarget).padding(2.dp),
                )
            }
        }
    }
}

@Composable
private fun BudgetRow(label: String, value: Long, color: Color, hidden: Boolean) {
    Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
        Text(label, style = chakraText(FontWeight.Medium, 12, 16), color = SumvaloTheme.colors.tertiaryText, modifier = Modifier.weight(1f))
        AmountText(value, style = chakraText(FontWeight.SemiBold, 15, 20), hidden = hidden, color = color)
    }
}

/** A thin progress ring with the percentage centred — used by monthly spending and budgets. */
@Composable
private fun SpendRing(
    fraction: Double,
    size: Dp,
    stroke: Dp,
    modifier: Modifier = Modifier,
    centerPercent: Boolean = true,
    ringColor: Color = Color.Unspecified,
) {
    val colors = SumvaloTheme.colors
    val progress = fraction.coerceIn(0.0, 1.0).toFloat()
    val arcColor = when {
        ringColor != Color.Unspecified -> ringColor
        fraction >= 0.85 -> colors.expense
        else -> colors.ink
    }
    Box(modifier.size(size), contentAlignment = Alignment.Center) {
        Canvas(Modifier.fillMaxSize()) {
            val strokePx = stroke.toPx()
            val inset = strokePx / 2
            val arcSize = Size(this.size.width - strokePx, this.size.height - strokePx)
            val topLeft = Offset(inset, inset)
            drawArc(
                color = colors.fill,
                startAngle = 0f,
                sweepAngle = 360f,
                useCenter = false,
                topLeft = topLeft,
                size = arcSize,
                style = Stroke(width = strokePx, cap = StrokeCap.Round),
            )
            if (progress > 0f) {
                drawArc(
                    color = arcColor,
                    startAngle = -90f,
                    sweepAngle = 360f * progress,
                    useCenter = false,
                    topLeft = topLeft,
                    size = arcSize,
                    style = Stroke(width = strokePx, cap = StrokeCap.Round),
                )
            }
        }
        if (centerPercent) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text(
                    "${(fraction * 100).roundToInt()}%",
                    style = chakraText(FontWeight.Bold, if (size >= 80.dp) 20 else 12, if (size >= 80.dp) 24 else 16),
                    color = if (fraction >= 0.85) colors.expense else colors.ink,
                )
                if (size >= 80.dp) {
                    Text("used", style = chakraText(FontWeight.Medium, 11, 14), color = colors.tertiaryText)
                }
            }
        }
    }
}

/* ------------------------------ Spending pace -------------------------------- */

/** The spending-pace section: this month against the same day last month, charted. */
@Composable
internal fun PaceSectionV2(state: HomeState, hidden: Boolean, onActivity: () -> Unit) {
    val charts = state.charts
    val current = charts.paceCurrent.lastOrNull() ?: 0
    val sameDay = charts.pacePrevious.getOrNull((charts.paceCurrent.size - 1).coerceAtLeast(0))
        ?: charts.pacePrevious.lastOrNull() ?: 0
    Column(Modifier.fillMaxWidth().padding(horizontal = 16.dp).padding(top = 20.dp)) {
        SectionHeader("Spending pace", actionLabel = "See all", onAction = onActivity)
        Spacer(Modifier.height(10.dp))
        SumvaloCard(shape = cardShape, contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 16.dp, bottom = 12.dp)) {
            Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.Bottom) {
                Column {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(Modifier.size(6.dp).clip(CircleShape).background(SumvaloTheme.colors.ink))
                        Spacer(Modifier.width(6.dp))
                        Text("This month", style = chakraText(FontWeight.Medium, 11, 14), color = SumvaloTheme.colors.tertiaryText)
                    }
                    Spacer(Modifier.height(2.dp))
                    AmountText(current, style = chakraText(FontWeight.Bold, 24, 30), hidden = hidden)
                }
                Spacer(Modifier.weight(1f))
                Column(horizontalAlignment = Alignment.End) {
                    Text("Last month (same day)", style = chakraText(FontWeight.Medium, 11, 14), color = SumvaloTheme.colors.tertiaryText)
                    Spacer(Modifier.height(2.dp))
                    AmountText(sameDay, style = chakraText(FontWeight.SemiBold, 15, 20), hidden = hidden, color = SumvaloTheme.colors.subtleText)
                }
            }
            Spacer(Modifier.height(12.dp))
            SpendPaceChart(
                current = charts.paceCurrent,
                previous = charts.pacePrevious,
                daysInMonth = charts.periodDays,
                projected = charts.projected,
                hidden = hidden,
                height = 110.dp,
            )
        }
    }
}

/* ------------------------------ Upcoming bills ------------------------------- */

/** Upcoming bills as date-badge rows straight on the background, hairline-separated. */
@Composable
internal fun BillsSectionV2(state: HomeState, hidden: Boolean, onBills: () -> Unit) {
    Column(Modifier.fillMaxWidth().padding(horizontal = 16.dp).padding(top = 20.dp)) {
        SectionHeader("Upcoming bills", actionLabel = "See all", onAction = onBills)
        Spacer(Modifier.height(8.dp))
        Column {
            state.bills.take(3).forEachIndexed { i, b ->
                if (i > 0) {
                    Spacer(Modifier.height(6.dp))
                    V2Hairline()
                    Spacer(Modifier.height(6.dp))
                }
                BillRowV2(b, hidden, onBills)
            }
        }
    }
}

@Composable
private fun BillRowV2(b: BillItem, hidden: Boolean, onClick: () -> Unit) {
    val due = b.dueDate
    val overdue = b.isOverdue
    Row(
        Modifier
            .fillMaxWidth()
            .clickable(role = Role.Button, onClick = onClick)
            .padding(vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Column(
            Modifier
                .size(44.dp)
                .clip(RoundedCornerShape(10.dp))
                .background(if (overdue) SumvaloTheme.colors.alertTint else SumvaloTheme.colors.fill)
                .padding(vertical = 5.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center,
        ) {
            Text(
                due?.let { "%02d".format(it.dayOfMonth) } ?: "–",
                style = chakraText(FontWeight.Bold, 17, 20),
                color = if (overdue) SumvaloTheme.colors.expense else SumvaloTheme.colors.ink,
            )
            Text(
                due?.month?.getDisplayName(java.time.format.TextStyle.SHORT, Locale.ENGLISH)?.uppercase(Locale.ENGLISH) ?: "",
                style = chakraText(FontWeight.Medium, 9, 11),
                color = SumvaloTheme.colors.subtleText,
            )
        }
        Spacer(Modifier.width(12.dp))
        Column(Modifier.weight(1f)) {
            Text(
                b.card?.label() ?: "Card bill",
                style = chakraText(FontWeight.SemiBold, 14, 20),
                color = SumvaloTheme.colors.ink,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            Text(
                b.card?.last4?.takeIf { it.isNotBlank() }?.let { "··$it" } ?: (b.card?.bank ?: "No due date"),
                style = chakraText(FontWeight.Medium, 12, 16),
                color = SumvaloTheme.colors.tertiaryText,
            )
        }
        AmountText(b.bill.totalDue, style = chakraText(FontWeight.SemiBold, 15, 20), hidden = hidden)
    }
}

/* ----------------------------- Budget progress ------------------------------- */

/** The budget-progress section: one ring per category budget, red as it runs hot. */
@Composable
internal fun BudgetsSectionV2(state: HomeState, hidden: Boolean, onPlanning: () -> Unit) {
    val list = state.budgets.filterNot { it.isOverall }.take(4)
    if (list.isEmpty()) return
    Column(Modifier.fillMaxWidth().padding(horizontal = 16.dp).padding(top = 20.dp)) {
        SectionHeader("Budget progress", actionLabel = "See all", onAction = onPlanning)
        Spacer(Modifier.height(10.dp))
        SumvaloCard(shape = cardShape, contentPadding = PaddingValues(horizontal = 8.dp, vertical = 16.dp)) {
            Row(Modifier.fillMaxWidth()) {
                list.forEachIndexed { i, b ->
                    if (i > 0) Spacer(Modifier.width(4.dp))
                    val pct = if (b.limit > 0) b.spent.toDouble() / b.limit else 0.0
                    val hot = pct >= 0.85
                    Column(Modifier.weight(1f), horizontalAlignment = Alignment.CenterHorizontally) {
                        SpendRing(fraction = pct, size = 48.dp, stroke = 4.dp, modifier = Modifier.size(48.dp))
                        Spacer(Modifier.height(8.dp))
                        Text(
                            b.category?.name ?: "Budget",
                            style = chakraText(FontWeight.SemiBold, 12, 16),
                            color = SumvaloTheme.colors.ink,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                            textAlign = TextAlign.Center,
                        )
                        Spacer(Modifier.height(2.dp))
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            AmountText(
                                b.spent,
                                style = chakraText(FontWeight.SemiBold, 11, 14),
                                compact = true,
                                hidden = hidden,
                                color = if (hot) SumvaloTheme.colors.expense else SumvaloTheme.colors.ink,
                            )
                            Text(" / ", style = chakraText(FontWeight.Medium, 11, 14), color = SumvaloTheme.colors.tertiaryText)
                            AmountText(b.limit, style = chakraText(FontWeight.Medium, 11, 14), compact = true, hidden = hidden, color = SumvaloTheme.colors.subtleText)
                        }
                    }
                }
            }
        }
    }
}
