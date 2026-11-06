package com.acuranza.app.ui.features

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.outlined.ArrowDownward
import androidx.compose.material.icons.outlined.ArrowUpward
import androidx.compose.material.icons.outlined.Autorenew
import androidx.compose.material.icons.outlined.CalendarMonth
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import com.acuranza.app.R
import androidx.compose.ui.unit.sp
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Add
import androidx.compose.material.icons.outlined.Delete
import androidx.compose.material.icons.outlined.MoreVert
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExtendedFloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import com.acuranza.app.data.RecurringRuleEntity
import com.acuranza.app.designsystem.components.AmountText
import com.acuranza.app.designsystem.components.EmptyState
import com.acuranza.app.designsystem.components.SumvaloCard
import com.acuranza.app.designsystem.components.PrimaryButton
import com.acuranza.app.designsystem.components.SectionHeader
import com.acuranza.app.designsystem.components.SegmentedTabs
import com.acuranza.app.designsystem.theme.SumvaloTheme
import com.acuranza.app.finance.BudgetMath
import com.acuranza.app.finance.Recurring
import com.acuranza.app.model.TxnType
import com.acuranza.app.ui.common.AmountField
import com.acuranza.app.ui.common.ChoiceField
import com.acuranza.app.ui.common.ConfirmDialog
import com.acuranza.app.ui.common.DateField
import com.acuranza.app.ui.common.Fmt
import com.acuranza.app.ui.common.FormColumn
import com.acuranza.app.ui.common.SumvaloScreen
import com.acuranza.app.ui.common.rememberHaptic
import com.acuranza.app.ui.common.SumvaloTextField
import com.acuranza.app.ui.common.LocalContainer
import com.acuranza.app.ui.common.LocalHideAmounts
import com.acuranza.app.ui.common.SwitchRow
import com.acuranza.app.ui.common.label
import com.acuranza.app.ui.nav.Routes
import com.acuranza.app.ui.nav.popBackSafely
import com.acuranza.app.util.Time
import kotlinx.coroutines.launch
import java.time.LocalDate
import java.time.temporal.ChronoUnit

fun Recurring.Cadence.title(): String = when (this) {
    Recurring.Cadence.WEEKLY -> "Weekly"
    Recurring.Cadence.MONTHLY -> "Monthly"
    Recurring.Cadence.QUARTERLY -> "Quarterly"
    Recurring.Cadence.YEARLY -> "Yearly"
}

private fun cadenceOf(name: String) = runCatching { Recurring.Cadence.valueOf(name) }.getOrDefault(Recurring.Cadence.MONTHLY)

/** Chakra Petch text styles for the redesigned recurring cards. */
private val RecurringChakra = FontFamily(
    Font(R.font.chakrapetch_medium, FontWeight.Medium),
    Font(R.font.chakrapetch_semibold, FontWeight.SemiBold),
    Font(R.font.chakrapetch_bold, FontWeight.Bold),
)

private fun chakra(weight: FontWeight, size: Int, lineHeight: Int = size + 4): TextStyle =
    TextStyle(fontFamily = RecurringChakra, fontWeight = weight, fontSize = size.sp, lineHeight = lineHeight.sp)

/** Subscriptions, EMIs and UPI AutoPay mandates found in spending, the user's own schedules, and what's due soon. */
@Composable
fun RecurringScreen(nav: NavController) {
    val haptic = rememberHaptic()
    val c = LocalContainer.current
    val hidden = LocalHideAmounts.current
    val scope = rememberCoroutineScope()
    val today = Time.today()
    // Remembered so recompositions don't restart detection over 400 days of transactions.
    val detected by remember { c.recurring.observeDetected() }.collectAsState(initial = null)
    val rules by remember { c.recurring.observeRules() }.collectAsState(initial = emptyList())
    val features by remember { c.featurePrefs.settings }.collectAsState(initial = null)
    var upcoming by remember { mutableStateOf<List<BudgetMath.Due>>(emptyList()) }
    LaunchedEffect(detected, rules) {
        upcoming = runCatching { c.recurring.upcoming(today, today.plusDays(31)) }.getOrDefault(emptyList())
    }

    SumvaloScreen(
        title = "Recurring",
        onBack = { nav.popBackSafely() },
        floating = {
            ExtendedFloatingActionButton(
                onClick = { haptic.tick(); nav.navigate(Routes.editRecurring()) },
                icon = { Icon(Icons.Outlined.Add, null) },
                text = { Text("Schedule") },
            )
        },
    ) { pad ->
        RecurringContent(
            series = detected,
            rules = rules,
            upcoming = upcoming,
            hiddenKeys = features?.hiddenRecurring.orEmpty(),
            hidden = hidden,
            today = today,
            contentPadding = pad,
            onEditRule = { nav.navigate(Routes.editRecurring(it)) },
            onNewSchedule = { nav.navigate(Routes.editRecurring()) },
            onScheduleSeries = { nav.navigate(Routes.editRecurring(fromSeries = it)) },
            onHideSeries = { scope.launch { c.recurring.setHidden(it, true) } },
            onShowSeries = { scope.launch { c.recurring.setHidden(it, false) } },
        )
    }
}

/** Stateless recurring body (hero, next 30 days, schedules, spotted series); used by the app and screenshot tests. */
@Composable
fun RecurringContent(
    series: List<Recurring.Series>?,
    rules: List<RecurringRuleEntity>,
    upcoming: List<BudgetMath.Due>,
    hiddenKeys: Set<String>,
    hidden: Boolean,
    today: LocalDate,
    contentPadding: PaddingValues = PaddingValues(0.dp),
    onEditRule: (Long) -> Unit = {},
    onNewSchedule: () -> Unit = {},
    onScheduleSeries: (String) -> Unit = {},
    onHideSeries: (String) -> Unit = {},
    onShowSeries: (String) -> Unit = {},
) {
    val colors = SumvaloTheme.colors
    var showHidden by remember { mutableStateOf(false) }
    LazyColumn(
        Modifier.fillMaxSize().padding(contentPadding),
        contentPadding = PaddingValues(start = 20.dp, end = 20.dp, bottom = 8.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        item {
            val monthly = (series.orEmpty().filter { !it.overdue }.sumOf { it.monthlyEquivalent }) +
                rules.filter { it.active && it.type == TxnType.EXPENSE }.sumOf { monthlyOf(it) }
            SumvaloCard(contentPadding = PaddingValues(16.dp)) {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text("Repeating payments cost about", style = chakra(FontWeight.Medium, 13, 18), color = colors.subtleText)
                    Row(verticalAlignment = Alignment.Bottom, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        AmountText(monthly, style = chakra(FontWeight.Bold, 34, 38), hidden = hidden)
                        Text(
                            "a month",
                            style = chakra(FontWeight.Medium, 14, 24),
                            color = colors.subtleText,
                            modifier = Modifier.padding(bottom = 4.dp),
                        )
                    }
                    val mandates = series.orEmpty().count { it.autoDebit }
                    if (mandates > 0) {
                        Box(Modifier.fillMaxWidth().height(1.dp).background(colors.cardSurfaceHigh))
                        Text(
                            "$mandates paid by auto-debit (UPI AutoPay, NACH or standing instruction). " +
                                "Cancel unused mandates in your UPI app or bank.",
                            style = chakra(FontWeight.Normal, 12, 17),
                            color = colors.subtleText,
                        )
                    }
                }
            }
        }
        item { SectionHeader("Next 30 days") }
        item {
            if (upcoming.isEmpty()) {
                Text("Nothing expected.", style = chakra(FontWeight.Medium, 12, 16), color = colors.subtleText)
            } else {
                val rows = upcoming.take(30)
                SumvaloCard(contentPadding = PaddingValues(horizontal = 16.dp, vertical = 4.dp)) {
                    rows.forEachIndexed { i, d ->
                        RecurringRow(
                            title = d.label,
                            subtitle = run {
                                val days = ChronoUnit.DAYS.between(today, d.date)
                                if (days == 0L) "Today" else if (days == 1L) "Tomorrow" else Fmt.dayLabel(d.date, today)
                            },
                            amount = d.amount,
                            hidden = hidden,
                        )
                        if (i < rows.lastIndex) Box(Modifier.fillMaxWidth().height(1.dp).background(colors.cardSurfaceHigh))
                    }
                }
            }
        }
        item { SectionHeader("Scheduled by you") }
        if (rules.isEmpty()) {
            item {
                Box(
                    Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(20.dp))
                        .border(1.dp, colors.hairline, RoundedCornerShape(20.dp))
                        .padding(horizontal = 20.dp, vertical = 20.dp),
                    contentAlignment = Alignment.Center,
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Box(
                            Modifier
                                .size(36.dp)
                                .clip(RoundedCornerShape(10.dp))
                                .background(colors.fill),
                            contentAlignment = Alignment.Center,
                        ) {
                            Icon(Icons.Outlined.CalendarMonth, contentDescription = null, tint = colors.subtleText, modifier = Modifier.size(18.dp))
                        }
                        Text(
                            "Schedule rent, SIPs, EMIs or pocket money and Sumvalo adds them on their dates.",
                            style = chakra(FontWeight.Medium, 13, 19),
                            color = colors.subtleText,
                            textAlign = TextAlign.Center,
                        )
                    }
                }
            }
        } else {
            item {
                SumvaloCard(contentPadding = PaddingValues(horizontal = 16.dp, vertical = 4.dp)) {
                    rules.forEachIndexed { i, r ->
                        RecurringRow(
                            title = r.name,
                            subtitle = "${cadenceOf(r.cadence).title()} · next ${Fmt.date(LocalDate.ofEpochDay(r.nextDay))}" +
                                if (!r.active) " · paused" else if (!r.autoAdd) " · reminder only" else "",
                            amount = r.amount,
                            hidden = hidden,
                            type = r.type,
                            onClick = { onEditRule(r.id) },
                        )
                        if (i < rules.lastIndex) Box(Modifier.fillMaxWidth().height(1.dp).background(colors.cardSurfaceHigh))
                    }
                }
            }
        }
        item { SectionHeader("Spotted in your spending") }
        when {
            series == null -> item { Text("Looking…", style = chakra(FontWeight.Medium, 12, 16), color = colors.subtleText) }
            series.isEmpty() -> item {
                EmptyState(
                    "No repeats yet",
                    "Subscriptions and EMIs show up here once Sumvalo has seen them a few times.",
                )
            }
            else -> items(series, key = { "s${it.key}" }) { s ->
                SeriesCard(s, hidden, today, onSchedule = { onScheduleSeries(s.key) }, onHide = { onHideSeries(s.key) })
            }
        }
        if (hiddenKeys.isNotEmpty()) {
            item {
                TextButton(onClick = { showHidden = !showHidden }) {
                    Text(if (showHidden) "Hide the list" else "Not recurring (${hiddenKeys.size})")
                }
            }
            if (showHidden) {
                items(hiddenKeys.sorted()) { key ->
                    Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                        Text(key.substringBefore(':').replaceFirstChar { it.uppercase() }, Modifier.weight(1f))
                        TextButton(onClick = { onShowSeries(key) }) { Text("Show again") }
                    }
                }
            }
        }
    }
}

/** One row of a grouped recurring card: title + muted subtitle on the left, amount on the right. */
@Composable
private fun RecurringRow(
    title: String,
    subtitle: String,
    amount: Long,
    hidden: Boolean,
    type: TxnType = TxnType.EXPENSE,
    onClick: (() -> Unit)? = null,
) {
    val colors = SumvaloTheme.colors
    Row(
        Modifier
            .fillMaxWidth()
            .then(if (onClick != null) Modifier.clickable(role = Role.Button, onClick = onClick) else Modifier)
            .padding(vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
            Text(title, style = chakra(FontWeight.SemiBold, 14, 20), color = colors.ink, maxLines = 1, overflow = TextOverflow.Ellipsis)
            Text(subtitle, style = chakra(FontWeight.Medium, 12, 16), color = colors.subtleText, maxLines = 1, overflow = TextOverflow.Ellipsis)
        }
        AmountText(amount, style = chakra(FontWeight.SemiBold, 15, 20), type = type, hidden = hidden)
    }
}

private fun monthlyOf(r: RecurringRuleEntity): Long = when (cadenceOf(r.cadence)) {
    // Weekly matches the engine's Double math (finance.Recurring) so screen and engine agree.
    Recurring.Cadence.WEEKLY -> (r.amount * 52.0 / 12).toLong()
    Recurring.Cadence.MONTHLY -> r.amount
    Recurring.Cadence.QUARTERLY -> r.amount / 3
    Recurring.Cadence.YEARLY -> r.amount / 12
}

@Composable
private fun SeriesCard(s: Recurring.Series, hidden: Boolean, today: LocalDate, onHide: () -> Unit, onSchedule: () -> Unit) {
    val colors = SumvaloTheme.colors
    var menu by remember { mutableStateOf(false) }
    SumvaloCard(contentPadding = PaddingValues(start = 16.dp, end = 8.dp, top = 16.dp, bottom = 16.dp)) {
        Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                    Text(s.merchant, style = chakra(FontWeight.SemiBold, 15, 20), color = colors.ink, maxLines = 1, overflow = TextOverflow.Ellipsis)
                    val next = if (s.overdue) "last paid ${Fmt.date(s.lastDate)}" else "next ${Fmt.dayLabel(s.nextDate, today)}"
                    Text(
                        "${s.cadence.title()} · ${s.occurrences} payments · $next",
                        style = chakra(FontWeight.Medium, 12, 16),
                        color = colors.subtleText,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                }
                AmountText(s.lastAmount, style = chakra(FontWeight.SemiBold, 15, 20), hidden = hidden)
                Box0 {
                    IconButton(onClick = { menu = true }) {
                        Icon(Icons.Outlined.MoreVert, "More", tint = colors.subtleText, modifier = Modifier.size(18.dp))
                    }
                    DropdownMenu(expanded = menu, onDismissRequest = { menu = false }) {
                        DropdownMenuItem(text = { Text("Schedule it") }, onClick = { menu = false; onSchedule() })
                        DropdownMenuItem(text = { Text("Not recurring") }, onClick = { menu = false; onHide() })
                    }
                }
            }
            val pills = buildList {
                if (s.autoDebit) add(RecurringPill("AutoPay", colors.ink, colors.cardSurfaceHigh, colors.hairline, Icons.Outlined.Autorenew))
                s.priceChangePct?.let { pct ->
                    if (pct > 0) {
                        add(RecurringPill("Price up ${Fmt.pct(pct, 0)}", colors.alert, colors.alert.copy(alpha = 0.12f), colors.alert.copy(alpha = 0.4f), Icons.Outlined.ArrowUpward))
                    } else {
                        add(RecurringPill("Price down ${Fmt.pct(-pct, 0)}", colors.income, colors.income.copy(alpha = 0.12f), colors.income.copy(alpha = 0.4f), Icons.Outlined.ArrowDownward))
                    }
                }
                if (s.overdue) add(RecurringPill("Stopped?", colors.alert, colors.alert.copy(alpha = 0.12f), colors.alert.copy(alpha = 0.4f)))
            }
            if (pills.isNotEmpty()) {
                Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) { pills.forEach { RecurringPillRow(it) } }
            }
        }
    }
}

/** One status pill on a spotted series: tinted text on a tinted face with a hairline border. */
private data class RecurringPill(val label: String, val tint: Color, val bg: Color, val border: Color, val icon: ImageVector? = null)

@Composable
private fun RecurringPillRow(p: RecurringPill) {
    Row(
        Modifier
            .clip(RoundedCornerShape(50))
            .background(p.bg)
            .border(1.dp, p.border, RoundedCornerShape(50))
            .padding(horizontal = 10.dp, vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(4.dp),
    ) {
        if (p.icon != null) Icon(p.icon, contentDescription = null, tint = p.tint, modifier = Modifier.size(12.dp))
        Text(p.label, style = chakra(FontWeight.SemiBold, 12, 16), color = p.tint, maxLines = 1)
    }
}

@Composable
private fun Box0(content: @Composable () -> Unit) = androidx.compose.foundation.layout.Box { content() }

/** Adds or edits a scheduled payment. [fromSeries] prefills it from a detected repeating payment. */
@Composable
fun RecurringEditScreen(nav: NavController, id: Long?, fromSeries: String?) {
    val c = LocalContainer.current
    val scope = rememberCoroutineScope()
    val today = Time.today()
    // Remembered: this screen recomposes on every keystroke, and a new flow would re-query each time.
    val accounts by remember { c.accounts.observeAccounts() }.collectAsState(initial = emptyList())
    val categories by remember { c.categories.observeAll() }.collectAsState(initial = emptyList())
    var loaded by remember { mutableStateOf(id == null && fromSeries == null) }
    var existing by remember { mutableStateOf<RecurringRuleEntity?>(null) }
    var name by remember { mutableStateOf("") }
    var type by remember { mutableStateOf(TxnType.EXPENSE) }
    var amount by remember { mutableStateOf<Long?>(null) }
    var category by remember { mutableStateOf<String?>(null) }
    var accountId by remember { mutableStateOf<Long?>(null) }
    var toAccountId by remember { mutableStateOf<Long?>(null) }
    var cadence by remember { mutableStateOf(Recurring.Cadence.MONTHLY) }
    var first by remember { mutableStateOf(today) }
    var autoAdd by remember { mutableStateOf(true) }
    var active by remember { mutableStateOf(true) }
    var note by remember { mutableStateOf("") }
    var confirmDelete by remember { mutableStateOf(false) }

    LaunchedEffect(id, fromSeries) {
        if (id != null) {
            c.recurring.rule(id)?.let { r ->
                existing = r; name = r.name; type = r.type; amount = r.amount; category = r.categoryKey
                accountId = r.accountId; toAccountId = r.toAccountId; cadence = cadenceOf(r.cadence)
                first = LocalDate.ofEpochDay(r.anchorDay); autoAdd = r.autoAdd; active = r.active; note = r.note.orEmpty()
            }
        } else if (fromSeries != null) {
            c.recurring.detected(today).firstOrNull { it.key == fromSeries }?.let { s ->
                name = s.merchant; amount = s.lastAmount; category = s.category; cadence = s.cadence; first = s.nextDate
                autoAdd = false
            }
        }
        loaded = true
    }
    LaunchedEffect(accounts) {
        if (accountId == null) accountId = accounts.firstOrNull { !it.isPending && !it.isCard }?.id ?: accounts.firstOrNull()?.id
    }

    val usable = accounts.filter { !it.isPending }
    val catOptions = categories.filter { it.type == (if (type == TxnType.TRANSFER) TxnType.EXPENSE else type) }
    val valid = name.isNotBlank() && (amount ?: 0) > 0 && accountId != null &&
        (type == TxnType.TRANSFER && toAccountId != null && toAccountId != accountId || type != TxnType.TRANSFER && category != null)

    SumvaloScreen(
        title = if (id == null) "Schedule a payment" else "Edit schedule",
        onBack = { nav.popBackSafely() },
        actions = {
            if (existing != null) IconButton(onClick = { confirmDelete = true }) { Icon(Icons.Outlined.Delete, "Delete") }
        },
    ) { pad ->
        if (!loaded) return@SumvaloScreen
        Column(Modifier.fillMaxSize().padding(pad).verticalScroll(rememberScrollState()).padding(20.dp)) {
            FormColumn {
                SegmentedTabs(listOf("Expense", "Income", "Transfer"), listOf(TxnType.EXPENSE, TxnType.INCOME, TxnType.TRANSFER).indexOf(type),
                    onSelect = { type = listOf(TxnType.EXPENSE, TxnType.INCOME, TxnType.TRANSFER)[it]; category = null })
                SumvaloTextField(name, { name = it.take(60) }, "Name", supporting = "e.g. Rent, SIP, Netflix, Maid")
                AmountField("Amount", amount, { amount = it }, allowZero = false)
                if (type != TxnType.TRANSFER) {
                    ChoiceField("Category", catOptions, catOptions.firstOrNull { it.key == category }, { it.name }, { category = it.key })
                }
                ChoiceField(if (type == TxnType.TRANSFER) "From account" else "Account", usable, usable.firstOrNull { it.id == accountId },
                    { it.account.label() }, { accountId = it.id })
                if (type == TxnType.TRANSFER) {
                    ChoiceField("To account", usable, usable.firstOrNull { it.id == toAccountId }, { it.account.label() }, { toAccountId = it.id })
                }
                ChoiceField("Repeats", Recurring.Cadence.entries, cadence, { it.title() }, { cadence = it })
                DateField(if (existing == null) "First payment" else "Schedule starts", first, { first = it })
                SwitchRow("Add automatically", "Records it on each date. Off: only shows it as upcoming.", autoAdd, { autoAdd = it })
                if (existing != null) SwitchRow("Active", null, active, { active = it })
                SumvaloTextField(note, { note = it.take(200) }, "Note (optional)")
                Text(
                    "Dates before today are not added; the schedule starts from its next date.",
                    style = MaterialTheme.typography.bodySmall,
                    color = SumvaloTheme.colors.subtleText,
                )
                Spacer(Modifier.height(4.dp))
                PrimaryButton("Save", enabled = valid, modifier = Modifier.fillMaxWidth(), onClick = {
                    scope.launch {
                        val anchor = first
                        val old = existing
                        val keepNext = old != null && old.anchorDay == anchor.toEpochDay() && old.cadence == cadence.name
                        val next = if (keepNext) old!!.nextDay else Recurring.nextOccurrence(anchor, cadence, today.minusDays(1)).toEpochDay()
                        val rule = RecurringRuleEntity(
                            id = old?.id ?: 0,
                            name = name.trim(),
                            type = type,
                            amount = amount ?: 0,
                            categoryKey = if (type == TxnType.TRANSFER) "transfer" else category ?: "other",
                            accountId = accountId!!,
                            toAccountId = if (type == TxnType.TRANSFER) toAccountId else null,
                            cadence = cadence.name,
                            anchorDay = anchor.toEpochDay(),
                            nextDay = next,
                            active = active,
                            autoAdd = autoAdd,
                            note = note.trim().ifBlank { null },
                            createdAt = old?.createdAt ?: System.currentTimeMillis(),
                        )
                        c.recurring.save(rule)
                        if (rule.active && rule.autoAdd) runCatching { c.recurring.recordDue(today) }
                        nav.popBackSafely()
                    }
                })
                Spacer(Modifier.width(1.dp).height(24.dp))
            }
        }
    }
    if (confirmDelete) {
        ConfirmDialog(
            "Delete this schedule?",
            "Payments it already added stay in your transactions.",
            "Delete",
            onConfirm = {
                confirmDelete = false
                scope.launch { existing?.let { c.recurring.delete(it) }; nav.popBackSafely() }
            },
            onDismiss = { confirmDelete = false },
            destructive = true,
        )
    }
}
