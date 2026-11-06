package com.acuranza.app.ui.transactions

import com.acuranza.app.designsystem.components.SumvaloCard
import com.acuranza.app.designsystem.theme.SumvaloText
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsFocusedAsState
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowLeft
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.automirrored.outlined.ReceiptLong
import androidx.compose.material.icons.outlined.FilterAlt
import androidx.compose.material.icons.outlined.Search
import androidx.compose.material.icons.outlined.SearchOff
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.TextButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SnackbarDuration
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.SnackbarResult
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.SolidColor
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
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.graphics.Color
import androidx.lifecycle.ViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewModelScope
import androidx.navigation.NavController
import com.acuranza.app.data.TransactionEntity
import com.acuranza.app.data.tombstones
import com.acuranza.app.R
import com.acuranza.app.designsystem.components.DateHeader
import com.acuranza.app.designsystem.components.EmptyState
import com.acuranza.app.designsystem.components.AmountText
import com.acuranza.app.designsystem.components.FilterChipsRow
import com.acuranza.app.designsystem.components.StatChip
import com.acuranza.app.designsystem.components.SwipeActionsRow
import com.acuranza.app.di.AppContainer
import com.acuranza.app.model.TxnSource
import com.acuranza.app.model.TxnType
import com.acuranza.app.repo.TransactionItem
import com.acuranza.app.repo.TxnFilter
import com.acuranza.app.ui.common.Fmt
import com.acuranza.app.ui.common.LocalHideAmounts
import com.acuranza.app.ui.common.acuranzaViewModel
import com.acuranza.app.ui.common.rememberHaptic
import com.acuranza.app.ui.home.TxnRow
import com.acuranza.app.ui.nav.Routes
import com.acuranza.app.util.Period
import com.acuranza.app.util.Time
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.debounce
import kotlinx.coroutines.flow.first
import com.acuranza.app.designsystem.theme.SumvaloTheme
import com.acuranza.app.repo.TransactionRepository
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.time.LocalDate

private const val F_EXPENSE = "Expenses"
private const val F_INCOME = "Income"
private const val F_TRANSFER = "Transfers"
internal const val F_SMS = "From SMS"
private const val F_MANUAL = "Manual"

/** Opens the fix-uncategorised screen rather than filtering the list. */
internal const val F_OTHER = "Other"

/** One-shot filter requested by another screen (e.g. Settings → "View email transactions"). */
object ActivityFilterRequest {
    @Volatile var pending: String? = null

    /** A category key to filter by, set by insights and the Home category chart before navigating. */
    @Volatile var pendingCategory: String? = null

    fun consume(): String? = pending.also { pending = null }
    fun consumeCategory(): String? = pendingCategory.also { pendingCategory = null }
}

internal const val CATEGORY_CHIP_PREFIX = "Category: "

/** Type filters driven by the segmented control; sources and clean-up live in the filter sheet. */
private val TYPE_FILTERS = setOf(F_EXPENSE, F_INCOME, F_TRANSFER)

data class DayGroup(val date: LocalDate, val items: List<TransactionItem>, val net: Long)

data class TxnListState(
    val period: Period,
    val query: String = "",
    val filters: Set<String> = emptySet(),
    val groups: List<DayGroup> = emptyList(),
    val income: Long = 0,
    val expense: Long = 0,
    val loaded: Boolean = false,
    /** Name of the category being filtered, if any; shown as a removable chip. */
    val categoryName: String? = null,
    /** Imported expenses this period still in "Other". */
    val uncategorised: Int = 0,
    /** True when a category, type, source or search narrows the list; the totals then cover only what's shown. */
    val narrowed: Boolean = false,
)

@OptIn(ExperimentalCoroutinesApi::class)
class TransactionsViewModel(private val c: AppContainer) : ViewModel() {
    /** Months before the current budgeting month (0 = this month). */
    private val offset = MutableStateFlow(0)
    private val period = combine(c.settings.settings, offset) { s, o ->
        var p = Period.containing(Time.today(), s.payCycle)
        repeat(o) { p = p.previous() }
        p
    }.distinctUntilChanged()
    private val query = MutableStateFlow("")
    private val filters = MutableStateFlow<Set<String>>(emptySet())
    private val category = MutableStateFlow<String?>(null)

    // Typing waits for a pause before re-querying; clearing the search applies at once.
    @OptIn(kotlinx.coroutines.FlowPreview::class)
    private val typed = query.debounce { if (it.isEmpty()) 0L else 250L }

    val state: StateFlow<TxnListState> = combine(period, typed, filters, category) { p, q, f, cat -> listOf(p, q, f, cat) }
        .flatMapLatest { values ->
            val p = values[0] as Period
            val q = values[1] as String
            @Suppress("UNCHECKED_CAST") val f = values[2] as Set<String>
            val cat = values[3] as String?
            c.transactions.observe(TxnFilter(period = p, query = q, categoryKey = cat)).let { flow ->
                combine(flow, c.categories.observeAll()) { list, cats ->
                    val filtered = list.filter { matches(it, f) }
                    // Totals come from exactly the rows shown, with the same rules as Home and Insights
                    // (transfers, excluded and held-for-review rows don't count).
                    val summary = TransactionRepository.summarise(p, filtered.map { it.txn }, cats.associateBy { it.key })
                    val groups = filtered.groupBy { it.date }.toSortedMap(compareByDescending { it })
                        .map { (d, items) -> DayGroup(d, items, items.sumOf { it.signedAmount }) }
                    val catName = cat?.let { key -> list.firstOrNull()?.category?.name ?: c.categories.byKey(key)?.name ?: key }
                    val other = list.count {
                        it.txn.type == TxnType.EXPENSE && it.txn.categoryKey == "other" && it.txn.source != TxnSource.MANUAL
                    }
                    TxnListState(
                        p, q, f, groups, summary.income, summary.expense, loaded = true, categoryName = catName,
                        uncategorised = other, narrowed = cat != null || f.isNotEmpty() || q.isNotBlank(),
                    )
                }
            }
        }
        // Filtering, grouping and totals run off the main thread.
        .flowOn(kotlinx.coroutines.Dispatchers.Default)
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), TxnListState(Period.containing(Time.today())))

    /** The period currently shown (for callers that need it synchronously). */
    val currentPeriod: Period get() = state.value.period

    private fun matches(t: TransactionItem, f: Set<String>): Boolean {
        val types = f.mapNotNull {
            when (it) {
                F_EXPENSE -> TxnType.EXPENSE
                F_INCOME -> TxnType.INCOME
                F_TRANSFER -> TxnType.TRANSFER
                else -> null
            }
        }
        val sources = f.mapNotNull {
            when (it) {
                F_SMS -> TxnSource.SMS
                F_MANUAL -> TxnSource.MANUAL
                else -> null
            }
        }
        return (types.isEmpty() || t.txn.type in types) && (sources.isEmpty() || t.txn.source in sources)
    }

    fun previous() { offset.value = offset.value + 1 }
    fun next() { offset.value = (offset.value - 1).coerceAtLeast(0) }
    fun setQuery(q: String) { query.value = q }

    /** What is typed in the search box right now (the list follows after a short pause). */
    val queryText: StateFlow<String> get() = query
    fun toggle(f: String) {
        if (f.startsWith(CATEGORY_CHIP_PREFIX)) {
            category.value = null
            return
        }
        filters.value = if (f in filters.value) filters.value - f else filters.value + f
    }

    /** Segmented single-select for the transaction type; source and clean-up filters are kept. */
    fun setType(f: String?) {
        filters.value = filters.value.filterNot { it in TYPE_FILTERS }.toSet() +
            (f?.let { setOf(it) } ?: emptySet())
    }

    /** Clears every active filter (type, source, clean-up, category); the search text stays. */
    fun clearFilters() {
        filters.value = emptySet()
        category.value = null
    }
    /** Opened from Home or an insight: show this budgeting month's [key] transactions, matching the figure tapped. */
    fun filterCategory(key: String) {
        offset.value = 0
        filters.value = emptySet()
        query.value = ""
        category.value = key
    }
    fun only(f: String) { filters.value = setOf(f) }

    /** Deletes the transaction (remembering an imported one so a rescan doesn't bring it back) and returns it for Undo. */
    suspend fun delete(id: Long): TransactionEntity? = c.tombstones.deleteTransaction(id)

    fun restore(t: TransactionEntity) = viewModelScope.launch { c.tombstones.undoDelete(t) }
    fun markReviewed(id: Long) = viewModelScope.launch { c.transactions.markReviewed(id) }
}

@Composable
fun TransactionsScreen(nav: NavController) {
    val vm = acuranzaViewModel { TransactionsViewModel(it) }
    val state by vm.state.collectAsStateWithLifecycle()
    val typed by vm.queryText.collectAsStateWithLifecycle()
    androidx.compose.runtime.LaunchedEffect(Unit) {
        ActivityFilterRequest.consume()?.let(vm::only)
        ActivityFilterRequest.consumeCategory()?.let(vm::filterCategory)
    }
    val hidden = LocalHideAmounts.current
    val snackbar = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()
    val haptic = rememberHaptic()
    TransactionsContent(
        state = state.copy(query = typed),
        hidden = hidden,
        snackbar = snackbar,
        onPrevious = vm::previous,
        onNext = vm::next,
        onQuery = vm::setQuery,
        onToggleFilter = { f -> if (f == F_OTHER) nav.navigate(Routes.UNCATEGORISED) else vm.toggle(f) },
        onSelectType = vm::setType,
        onClearFilters = vm::clearFilters,
        onFixUncategorised = { nav.navigate(Routes.UNCATEGORISED) },
        onDelete = { t ->
            haptic()
            scope.launch {
                val removed = vm.delete(t.txn.id) ?: return@launch
                val r = snackbar.showSnackbar("Transaction deleted", "Undo", duration = SnackbarDuration.Short)
                if (r == SnackbarResult.ActionPerformed) vm.restore(removed)
            }
        },
        onReviewed = { t ->
            haptic()
            vm.markReviewed(t.txn.id)
        },
        onOpen = { t -> nav.navigate(Routes.txn(t.txn.id)) },
    )
}

/** Chakra Petch — bundled under res/font (SIL OFL). Shared display face across home/settings/activity. */
val Chakra = FontFamily(
    Font(R.font.chakrapetch_medium, FontWeight.Medium),
    Font(R.font.chakrapetch_semibold, FontWeight.SemiBold),
    Font(R.font.chakrapetch_bold, FontWeight.Bold),
)

/** Chakra-flavoured text style. */
private fun chakraText(weight: FontWeight, size: Int, lineHeight: Int, letterSpacingSp: Float = 0f): TextStyle =
    TextStyle(fontFamily = Chakra, fontWeight = weight, fontSize = size.sp, lineHeight = lineHeight.sp, letterSpacing = letterSpacingSp.sp)

/** One stat: tracking-caps label over a bold compact amount, in a hairline-separated row. */
@Composable
private fun StatPill(label: String, paise: Long, color: Color, modifier: Modifier = Modifier, hidden: Boolean) {
    Column {
        Text(
            label.uppercase(),
            style = chakraText(FontWeight.SemiBold, 10, 12, 1.0f),
            color = SumvaloTheme.colors.tertiaryText,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
        Spacer(Modifier.height(2.dp))
        AmountText(
            paise,
            style = chakraText(FontWeight.Bold, 16, 20),
            compact = true,
            hidden = hidden,
            color = if (paise < 0 && label == "Net") SumvaloTheme.colors.expense else color,
            modifier = Modifier.weight(1f),
        )
    }
}

/** Search field styled per the reference: compact 40dp field, hairline border, focus ring. */
@Composable
private fun ActivitySearchField(query: String, onQueryChange: (String) -> Unit) {
    val colors = SumvaloTheme.colors
    val interaction = remember { MutableInteractionSource() }
    val focused by interaction.collectIsFocusedAsState()
    val shape = RoundedCornerShape(10.dp)
    Row(
        Modifier
            .fillMaxWidth()
            .height(40.dp)
            .clip(shape)
            .background(colors.cardSurface)
            .border(1.dp, if (focused) colors.tertiaryText else colors.hairline, shape)
            .padding(horizontal = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Icon(Icons.Outlined.Search, contentDescription = null, tint = colors.tertiaryText, modifier = Modifier.size(16.dp))
        Spacer(Modifier.width(8.dp))
        BasicTextField(
            value = query,
            onValueChange = onQueryChange,
            singleLine = true,
            textStyle = chakraText(FontWeight.Normal, 13, 18).copy(color = colors.ink),
            cursorBrush = SolidColor(colors.ink),
            interactionSource = interaction,
            modifier = Modifier.weight(1f),
            decorationBox = { inner ->
                Box(Modifier.fillMaxWidth(), contentAlignment = Alignment.CenterStart) {
                    if (query.isEmpty()) {
                        Text(
                            "Search merchant, note, category or #tag",
                            style = chakraText(FontWeight.Normal, 13, 18),
                            color = colors.tertiaryText,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                        )
                    }
                    inner()
                }
            },
        )
    }
}

/** Stateless Activity screen; used by the app and by screenshot tests. */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TransactionsContent(
    state: TxnListState,
    hidden: Boolean,
    snackbar: SnackbarHostState = remember { SnackbarHostState() },
    onPrevious: () -> Unit = {},
    onNext: () -> Unit = {},
    onQuery: (String) -> Unit = {},
    onToggleFilter: (String) -> Unit = {},
    onSelectType: (String?) -> Unit = {},
    onClearFilters: () -> Unit = {},
    onDelete: (TransactionItem) -> Unit = {},
    onReviewed: (TransactionItem) -> Unit = {},
    onOpen: (TransactionItem) -> Unit = {},
    onFixUncategorised: () -> Unit = {},
) {
    val canGoNext = !state.period.endExclusive.isAfter(Time.today())
    val colors = SumvaloTheme.colors
    var showFilters by rememberSaveable { mutableStateOf(false) }
    val selectedType = listOf(F_EXPENSE, F_INCOME, F_TRANSFER).firstOrNull { it in state.filters }
    Box(Modifier.fillMaxSize()) {
        LazyColumn(Modifier.fillMaxSize(), contentPadding = PaddingValues(bottom = 8.dp)) {
            item(key = "head") {
                Column(Modifier.fillMaxWidth().padding(horizontal = 16.dp).padding(top = 12.dp)) {
                    Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            "Activity",
                            style = chakraText(FontWeight.Bold, 22, 28),
                            color = colors.ink,
                            modifier = Modifier.weight(1f),
                        )
                        Row(
                            Modifier
                                .clip(RoundedCornerShape(10.dp))
                                .background(colors.cardSurface)
                                .border(1.dp, colors.hairline, RoundedCornerShape(10.dp))
                                .padding(horizontal = 2.dp, vertical = 2.dp),
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            PeriodChevron(Icons.AutoMirrored.Filled.KeyboardArrowLeft, "Previous period", onPrevious)
                            Text(
                                Fmt.periodLabel(state.period),
                                style = chakraText(FontWeight.SemiBold, 13, 18),
                                color = colors.ink,
                                modifier = Modifier.padding(horizontal = 8.dp),
                            )
                            PeriodChevron(Icons.AutoMirrored.Filled.KeyboardArrowRight, "Next period") { if (canGoNext) onNext() }
                        }
                    }
                    Spacer(Modifier.height(12.dp))
                    // Totals: Income / Spent / Net with hairline separators.
                    Row(Modifier.fillMaxWidth().height(IntrinsicSize.Min)) {
                        StatPill("Income", state.income, colors.income, Modifier.weight(1f), hidden)
                        Spacer(Modifier.width(8.dp))
                        Box(Modifier.width(1.dp).fillMaxHeight().background(colors.hairline))
                        Spacer(Modifier.width(8.dp))
                        StatPill("Spent", state.expense, colors.expense, Modifier.weight(1f), hidden)
                        Spacer(Modifier.width(8.dp))
                        Box(Modifier.width(1.dp).fillMaxHeight().background(colors.hairline))
                        Spacer(Modifier.width(8.dp))
                        StatPill("Net", state.income - state.expense, colors.ink, Modifier.weight(1f), hidden)
                    }
                    Spacer(Modifier.height(12.dp))
                    ActivitySearchField(state.query, onQuery)
                }
            }
            item(key = "filters") {
                Row(
                    Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp)
                        .padding(top = 10.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    TypeSegments(selectedType, onSelect = onSelectType, modifier = Modifier.weight(1f))
                    Box(
                        Modifier
                            .size(36.dp)
                            .clip(RoundedCornerShape(10.dp))
                            .background(colors.cardSurface)
                            .border(1.dp, colors.hairline, RoundedCornerShape(10.dp))
                            .clickable(role = Role.Button) { showFilters = true },
                        contentAlignment = Alignment.Center,
                    ) {
                        Icon(
                            Icons.Outlined.FilterAlt,
                            contentDescription = "More filters",
                            tint = colors.subtleText,
                            modifier = Modifier.size(16.dp),
                        )
                    }
                }
            }
            if (state.categoryName != null) {
                item(key = "category-chip") {
                    val chip = "$CATEGORY_CHIP_PREFIX${state.categoryName}"
                    FilterChipsRow(
                        options = listOf(chip),
                        selected = setOf(chip),
                        onToggle = onToggleFilter,
                        modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
                    )
                }
            }
            if (state.uncategorised > com.acuranza.app.ui.home.UNCATEGORISED_BANNER_MIN) {
                item(key = "uncategorised") {
                    UncategorisedBanner(state.uncategorised, onFixUncategorised, Modifier.padding(horizontal = 16.dp, vertical = 4.dp))
                }
            }
            if (state.loaded && state.groups.isEmpty()) {
                item(key = "empty") {
                    EmptyState(
                        title = if (state.query.isNotBlank() || state.narrowed) "No matches" else "Nothing here yet",
                        message = if (state.query.isNotBlank() || state.narrowed) {
                            "Try a different search or clear the filters."
                        } else {
                            "Transactions for ${Fmt.month(state.period.label)} will show up here. Tap + to add one."
                        },
                        icon = if (state.query.isNotBlank()) Icons.Outlined.SearchOff else Icons.AutoMirrored.Outlined.ReceiptLong,
                        modifier = Modifier.padding(start = 16.dp, top = 40.dp, end = 16.dp, bottom = 0.dp),
                    )
                }
            }
            state.groups.forEach { g ->
                item(key = "d" + g.date) {
                    DateHeader(Fmt.dayLabel(g.date), Modifier.padding(horizontal = 16.dp), netPaise = g.net, hidden = hidden)
                }
                itemsIndexed(g.items, key = { _, t -> t.txn.id }) { index, t ->
                    SwipeActionsRow(
                        modifier = Modifier.padding(horizontal = 16.dp),
                        onDelete = {
                            onDelete(t)
                            true
                        },
                        onConfirm = if (!t.txn.reviewed) {
                            {
                                onReviewed(t)
                                false
                            }
                        } else {
                            null
                        },
                    ) {
                        TxnRow(t, hidden, Modifier.fillMaxWidth()) { onOpen(t) }
                    }
                    // Inset hairline between the rows of one day, aligned with the text column.
                    if (index < g.items.lastIndex) {
                        Box(
                            Modifier
                                .padding(start = 54.dp, end = 16.dp)
                                .fillMaxWidth()
                                .height(1.dp)
                                .background(colors.divider),
                        )
                    }
                }
            }
        }
        SnackbarHost(snackbar, Modifier.align(Alignment.BottomCenter).padding(bottom = 100.dp))
        if (showFilters) {
            FilterSheet(
                state = state,
                onDismiss = { showFilters = false },
                onToggleFilter = onToggleFilter,
                onClearFilters = onClearFilters,
            )
        }
    }
}

/** The reference's compact month chevrons: 28dp tappable squares inside the period pill. */
@Composable
private fun PeriodChevron(icon: ImageVector, description: String, onClick: () -> Unit) {
    Box(
        Modifier
            .size(28.dp)
            .clip(RoundedCornerShape(6.dp))
            .clickable(role = Role.Button, onClick = onClick),
        contentAlignment = Alignment.Center,
    ) {
        Icon(icon, contentDescription = description, tint = SumvaloTheme.colors.subtleText, modifier = Modifier.size(16.dp))
    }
}

/** The type segmented control: All / Expenses / Income / Transfers, single-select like the reference. */
@Composable
private fun TypeSegments(selected: String?, onSelect: (String?) -> Unit, modifier: Modifier = Modifier) {
    val colors = SumvaloTheme.colors
    val haptic = rememberHaptic()
    val options = remember {
        listOf<String?>(null, F_EXPENSE, F_INCOME, F_TRANSFER).zip(listOf("All", "Expenses", "Income", "Transfers"))
    }
    Row(
        modifier
            .clip(RoundedCornerShape(10.dp))
            .border(1.dp, colors.hairline, RoundedCornerShape(10.dp))
            .background(colors.cardSurface)
            .padding(2.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        options.forEach { (f, label) ->
            val active = selected == f
            Box(
                Modifier
                    .weight(1f)
                    .clip(RoundedCornerShape(8.dp))
                    .background(if (active) colors.ink else Color.Transparent)
                    .clickable(role = Role.Button) { if (!active) { haptic.tick(); onSelect(f) } }
                    .semantics { this.selected = active }
                    .padding(vertical = 7.dp),
                contentAlignment = Alignment.Center,
            ) {
                Text(
                    label,
                    style = chakraText(if (active) FontWeight.SemiBold else FontWeight.Medium, 12, 16),
                    color = if (active) colors.onInk else colors.tertiaryText,
                    maxLines = 1,
                )
            }
        }
    }
}

/** Source, manual-entry and clean-up filters that don't fit the segmented row. */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun FilterSheet(
    state: TxnListState,
    onDismiss: () -> Unit,
    onToggleFilter: (String) -> Unit,
    onClearFilters: () -> Unit,
) {
    val colors = SumvaloTheme.colors
    ModalBottomSheet(onDismissRequest = onDismiss) {
        Column(
            Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp)
                .padding(bottom = 24.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            Text("More filters", style = chakraText(FontWeight.SemiBold, 16, 20), color = colors.ink)
            FilterChipsRow(
                options = listOf(F_SMS, F_MANUAL, F_OTHER),
                selected = state.filters,
                onToggle = onToggleFilter,
            )
            state.categoryName?.let { name ->
                val chip = "$CATEGORY_CHIP_PREFIX$name"
                FilterChipsRow(
                    options = listOf(chip),
                    selected = setOf(chip),
                    onToggle = onToggleFilter,
                )
            }
            if (state.narrowed) {
                TextButton(onClick = onClearFilters) {
                    Text("Clear all filters", color = colors.accent)
                }
            }
        }
    }
}

