package com.acuranza.app.ui.planning

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Add
import androidx.compose.material.icons.outlined.Flag
import androidx.compose.material.icons.outlined.PieChart
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.Modifier
import androidx.compose.foundation.Canvas
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.ViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewModelScope
import androidx.navigation.NavController
import com.acuranza.app.R
import com.acuranza.app.data.CategoryEntity
import com.acuranza.app.data.GoalEntity
import com.acuranza.app.designsystem.components.AmountText
import com.acuranza.app.designsystem.components.CategoryIcon
import com.acuranza.app.designsystem.components.CategoryIcons
import com.acuranza.app.designsystem.components.EmptyState
import com.acuranza.app.designsystem.components.PrimaryButton
import com.acuranza.app.designsystem.components.SumvaloCard
import com.acuranza.app.designsystem.theme.AmountStyles
import com.acuranza.app.designsystem.theme.SumvaloText
import com.acuranza.app.designsystem.theme.SumvaloTheme
import com.acuranza.app.designsystem.theme.staggeredEntrance
import com.acuranza.app.di.AppContainer
import com.acuranza.app.model.Money
import com.acuranza.app.model.TxnType
import com.acuranza.app.repo.BudgetStatus
import com.acuranza.app.repo.GoalStatus
import com.acuranza.app.finance.GoalStanding
import com.acuranza.app.finance.planGoal
import com.acuranza.app.ui.common.AmountField
import com.acuranza.app.ui.common.AmountSheet
import com.acuranza.app.ui.common.ChoiceField
import com.acuranza.app.ui.common.ConfirmDialog
import com.acuranza.app.ui.common.DateField
import com.acuranza.app.ui.common.Fmt
import com.acuranza.app.ui.common.FormColumn
import com.acuranza.app.ui.common.SumvaloTextField
import com.acuranza.app.ui.common.LocalHideAmounts
import com.acuranza.app.ui.common.color
import com.acuranza.app.ui.common.acuranzaViewModel
import com.acuranza.app.ui.common.launchGuarded
import com.acuranza.app.ui.common.rememberHaptic
import com.acuranza.app.util.Time
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.time.LocalDate
import kotlin.math.roundToInt

data class PlanningState(
    val budgets: List<BudgetStatus> = emptyList(),
    val goals: List<GoalStatus> = emptyList(),
    val categories: List<CategoryEntity> = emptyList(),
    val loaded: Boolean = false,
    /** Monte Carlo outlook per goal id (goals not yet reached and with time left). */
    val outlooks: Map<Long, com.acuranza.app.repo.GoalOutlook> = emptyMap(),
    /** Zero-based view of this month, when envelope mode is on. */
    val envelope: com.acuranza.app.repo.EnvelopeStatus? = null,
    val envelopeMode: Boolean = false,
)

class PlanningViewModel(private val c: AppContainer) : ViewModel() {
    val state: StateFlow<PlanningState> = combine(
        c.budgets.observeStatuses(),
        c.goals.observeStatuses(),
        c.categories.observeByType(TxnType.EXPENSE),
        c.goalOutlooks.observe(),
        combine(c.budgets.observeEnvelope(), c.featurePrefs.settings) { e, f -> e to f.envelopeMode },
    ) { b, g, cats, outlooks, env ->
        PlanningState(b, g, cats, loaded = true, outlooks = outlooks, envelope = env.first.takeIf { env.second }, envelopeMode = env.second)
    }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), PlanningState())

    fun setBudget(categoryKey: String?, limit: Long, rollover: Boolean? = null) = viewModelScope.launch { c.budgets.upsert(categoryKey, limit, rollover) }
    fun setEnvelopeMode(on: Boolean) = viewModelScope.launch { c.featurePrefs.setEnvelopeMode(on) }
    fun deleteBudget(s: BudgetStatus) = viewModelScope.launch { c.budgets.delete(s.budget) }
    private val guard = com.acuranza.app.ui.common.ActionGuard()

    fun saveGoal(goal: GoalEntity) = launchGuarded(guard) { c.goals.upsert(goal) }
    fun contribute(id: Long, amount: Long) = launchGuarded(guard) { c.goals.contribute(id, amount) }
    fun deleteGoal(goal: GoalEntity) = viewModelScope.launch { c.goals.delete(goal) }
    fun setInflation(goalId: Long, pct: Double) = viewModelScope.launch { c.goalOutlooks.setInflation(goalId, pct) }
}

/** Lets other screens (e.g. an insight's action) open Plan on a given tab: 0 = Budgets, 1 = Goals. */
object PlanningTabRequest {
    var pending by mutableStateOf<Int?>(null)
}

@Composable
fun PlanningScreen(@Suppress("UNUSED_PARAMETER") nav: NavController) {
    val vm = acuranzaViewModel { PlanningViewModel(it) }
    val state by vm.state.collectAsStateWithLifecycle()
    var tab by rememberSaveable { mutableIntStateOf(0) }
    val requestedTab = PlanningTabRequest.pending
    androidx.compose.runtime.LaunchedEffect(requestedTab) {
        if (requestedTab != null) {
            tab = requestedTab
            PlanningTabRequest.pending = null
        }
    }
    var addBudget by remember { mutableStateOf(false) }
    var editGoal by remember { mutableStateOf<GoalEntity?>(null) }
    var newGoal by remember { mutableStateOf(false) }

    PlanningContent(
        state = state,
        tab = tab,
        onTab = { tab = it },
        onAddBudget = { addBudget = true },
        onNewGoal = { newGoal = true },
        onSetBudget = { key, limit, roll -> vm.setBudget(key, limit, roll) },
        onEnvelopeMode = vm::setEnvelopeMode,
        onDeleteBudget = vm::deleteBudget,
        onOpenGoal = { editGoal = it },
        onContribute = vm::contribute,
        onInflation = vm::setInflation,
    )

    if (addBudget) {
        AddBudgetSheet(
            categories = state.categories.filter { c -> state.budgets.none { it.budget.categoryKey == c.key } && c.key != "transfer" },
            hasOverall = state.budgets.any { it.isOverall },
            onDismiss = { addBudget = false },
            onSave = { key, limit ->
                vm.setBudget(key, limit)
                addBudget = false
            },
        )
    }
    if (newGoal || editGoal != null) {
        GoalSheet(
            goal = editGoal,
            inflationPct = editGoal?.let { g -> state.goals.firstOrNull { it.goal.id == g.id }?.inflationPct } ?: 0.0,
            onDismiss = {
                newGoal = false
                editGoal = null
            },
            onSave = {
                vm.saveGoal(it)
                newGoal = false
                editGoal = null
            },
            onDelete = editGoal?.let { g ->
                {
                    vm.deleteGoal(g)
                    editGoal = null
                }
            },
        )
    }
}

/** Stateless Plan screen (header, tabs and lists, without sheets); used by the app and screenshot tests. */
@Composable
fun PlanningContent(
    state: PlanningState,
    tab: Int,
    onTab: (Int) -> Unit = {},
    onAddBudget: () -> Unit = {},
    onNewGoal: () -> Unit = {},
    onSetBudget: (String?, Long, Boolean?) -> Unit = { _, _, _ -> },
    onEnvelopeMode: (Boolean) -> Unit = {},
    onDeleteBudget: (BudgetStatus) -> Unit = {},
    onOpenGoal: (GoalEntity) -> Unit = {},
    onContribute: (Long, Long) -> Unit = { _, _ -> },
    onInflation: (Long, Double) -> Unit = { _, _ -> },
) {
    Column(Modifier.fillMaxSize()) {
        Column(Modifier.padding(start = 20.dp, end = 12.dp, top = 12.dp, bottom = 8.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text("Plan", style = SumvaloText.screenTitle, modifier = Modifier.weight(1f))
                TextButton(onClick = { if (tab == 0) onAddBudget() else onNewGoal() }) {
                    Icon(Icons.Outlined.Add, contentDescription = null, tint = SumvaloTheme.colors.ink, modifier = Modifier.size(18.dp))
                    Spacer(Modifier.width(4.dp))
                    Text(if (tab == 0) "Budget" else "Goal", color = SumvaloTheme.colors.ink)
                }
            }
            TypeToggle(tab = tab, onTab = onTab)
        }
        if (tab == 0) {
            BudgetsTab(state, onAdd = onAddBudget, onSet = onSetBudget, onDelete = onDeleteBudget, onEnvelopeMode = onEnvelopeMode)
        } else {
            GoalsTab(state, onAdd = onNewGoal, onOpen = onOpenGoal, onContribute = onContribute, onInflation = onInflation)
        }
    }
}

/** Chakra Petch text styles for the redesigned plan cards. */
private val PlanChakra = FontFamily(
    Font(R.font.chakrapetch_medium, FontWeight.Medium),
    Font(R.font.chakrapetch_semibold, FontWeight.SemiBold),
    Font(R.font.chakrapetch_bold, FontWeight.Bold),
)

internal fun planText(weight: FontWeight, size: Int, lineHeight: Int = size + 4, letterSpacingSp: Float = 0f): TextStyle =
    TextStyle(fontFamily = PlanChakra, fontWeight = weight, fontSize = size.sp, lineHeight = lineHeight.sp, letterSpacing = letterSpacingSp.sp)

/** Budgets / Goals pill toggle at the top of the Plan tab. */
@Composable
private fun TypeToggle(tab: Int, onTab: (Int) -> Unit) {
    val colors = SumvaloTheme.colors
    Row(
        Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(50))
            .border(1.dp, colors.hairline, RoundedCornerShape(50))
            .background(colors.fill)
            .padding(4.dp),
        horizontalArrangement = Arrangement.spacedBy(4.dp),
    ) {
        listOf("Budgets", "Goals").forEachIndexed { i, label ->
            val active = i == tab
            Box(
                Modifier
                    .weight(1f)
                    .height(36.dp)
                    .clip(RoundedCornerShape(50))
                    .background(if (active) colors.ink else Color.Transparent)
                    .clickable(role = Role.Button) { if (!active) onTab(i) },
                contentAlignment = Alignment.Center,
            ) {
                Text(
                    label,
                    style = planText(if (active) FontWeight.SemiBold else FontWeight.Medium, 13, 18),
                    color = if (active) colors.onInk else colors.subtleText,
                )
            }
        }
    }
}

/** One budget card: ring / icon tile, name + spent of limit, right percentage, pace bar + pace footer. */
@Composable
private fun BudgetCard(
    s: BudgetStatus,
    p: com.acuranza.app.finance.BudgetProjection,
    hidden: Boolean,
    dayFraction: Float,
    barDp: Float,
    markerDp: Float,
    onClick: () -> Unit,
) {
    val colors = SumvaloTheme.colors
    val willExceed = p.willExceed
    val barColor = if (willExceed) colors.alert else colors.income
    val fraction = p.usedFraction.coerceIn(0.0, 1.0)
    SumvaloCard(contentPadding = PaddingValues(16.dp)) {
        Column {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                if (s.isOverall) {
                    Box(
                        Modifier
                            .size(44.dp)
                            .clip(RoundedCornerShape(50))
                            .background(colors.fill),
                        contentAlignment = Alignment.Center,
                    ) {
                        Canvas(Modifier.fillMaxSize()) {
                            val stroke = 4.dp.toPx()
                            drawArc(
                                color = colors.hairline,
                                startAngle = 0f,
                                sweepAngle = 360f,
                                useCenter = false,
                                topLeft = Offset(stroke / 2, stroke / 2),
                                size = Size((size.width - stroke).toFloat(), (size.height - stroke).toFloat()),
                                style = Stroke(width = stroke, cap = StrokeCap.Round),
                            )
                            val sweep = (p.usedFraction.coerceIn(0.0, 1.0) * 360f).coerceAtLeast(1.0).toFloat()
                            drawArc(
                                color = colors.ink,
                                startAngle = -90f,
                                sweepAngle = sweep,
                                useCenter = false,
                                topLeft = Offset(stroke / 2, stroke / 2),
                                size = Size((size.width - stroke).toFloat(), (size.height - stroke).toFloat()),
                                style = Stroke(width = stroke, cap = StrokeCap.Round),
                            )
                        }
                        Text(
                            "%.0f%%".format(p.usedFraction * 100),
                            style = planText(FontWeight.Bold, 11, 14),
                            color = colors.ink,
                        )
                    }
                } else {
                    Box(
                        Modifier
                            .size(40.dp)
                            .clip(RoundedCornerShape(10.dp))
                            .background(colors.fill),
                        contentAlignment = Alignment.Center,
                    ) {
                        Icon(
                            CategoryIcons.forName(s.category?.icon ?: "category"),
                            contentDescription = null,
                            tint = colors.ink,
                            modifier = Modifier.size(18.dp),
                        )
                    }
                }
                Spacer(Modifier.width(12.dp))
                Column(Modifier.weight(1f)) {
                    Text(
                        if (s.isOverall) "Overall budget" else s.category?.name ?: s.budget.categoryKey.orEmpty(),
                        style = planText(FontWeight.SemiBold, 15, 20),
                        color = colors.ink,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        AmountText(
                            s.spent,
                            style = planText(FontWeight.SemiBold, 14, 18),
                            hidden = hidden,
                            color = colors.ink,
                        )
                        Text(" of ", style = planText(FontWeight.Normal, 13, 18), color = colors.subtleText)
                        AmountText(
                            s.limit,
                            style = planText(FontWeight.Normal, 13, 18),
                            hidden = hidden,
                            color = colors.subtleText,
                        )
                        if (s.budget.rollover && s.carry != 0L) {
                            Text(
                                if (s.carry > 0) " · +${if (hidden) "₹ ••••" else Money.format(s.carry, showPaise = false)} rolled over"
                                else " · −${if (hidden) "₹ ••••" else Money.format(-s.carry, showPaise = false)} carried",
                                style = planText(FontWeight.Normal, 13, 18),
                                color = colors.subtleText,
                            )
                        }
                    }
                }
                Text(
                    "%.0f%%".format(p.usedFraction * 100),
                    style = planText(FontWeight.SemiBold, 15, 20),
                    color = if (willExceed) colors.ink else colors.subtleText,
                )
            }
            Spacer(Modifier.height(10.dp))
            Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                Box(
                    Modifier
                        .fillMaxWidth()
                        .height(barDp.dp)
                        .clip(RoundedCornerShape(50))
                        .background(colors.fill),
                ) {
                    Box(
                        Modifier
                            .fillMaxWidth(fraction.toFloat())
                            .fillMaxHeight()
                            .clip(RoundedCornerShape(50))
                            .background(barColor),
                    )
                    Box(
                        Modifier
                            .fillMaxWidth()
                            .height((barDp + markerDp).dp),
                    ) {
                        Canvas(Modifier.fillMaxSize()) {
                            val barTop = markerDp.dp.toPx()
                            // track
                            drawRect(
                                color = colors.fill,
                                topLeft = Offset(0f, barTop),
                                size = Size(size.width, barDp.dp.toPx()),
                            )
                            // progress fill
                            drawRect(
                                color = barColor,
                                topLeft = Offset(0f, barTop),
                                size = Size(size.width * fraction.toFloat(), barDp.dp.toPx()),
                            )
                            // today marker (1dp wide, sticks 1dp above the bar)
                            val markerX = dayFraction * size.width - 0.5f.dp.toPx()
                            drawRect(
                                color = colors.subtleText,
                                topLeft = Offset(markerX.coerceAtLeast(0f), barTop - 1f.dp.toPx()),
                                size = Size(1f.dp.toPx(), markerDp.dp.toPx()),
                            )
                        }
                    }
                }
            }
            Spacer(Modifier.height(10.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(4.dp),
            ) {
                if (p.isExceeded) {
                    Text("Over by", style = planText(FontWeight.Medium, 12, 16), color = colors.subtleText)
                    Text(
                        Money.format(-p.remaining, showPaise = false),
                        style = planText(FontWeight.SemiBold, 15, 20),
                        color = colors.alert,
                    )
                } else if (willExceed) {
                    Text("On pace for", style = planText(FontWeight.Medium, 12, 16), color = colors.subtleText)
                    Text(
                        Money.format(p.projected, showPaise = false),
                        style = planText(FontWeight.SemiBold, 15, 20),
                        color = colors.alert,
                    )
                    Text(
                        " · keep under ${Money.format(p.dailyAllowance, showPaise = false)}/day",
                        style = planText(FontWeight.Normal, 12, 16),
                        color = colors.subtleText,
                    )
                } else {
                    Text(
                        Money.format(p.remaining, showPaise = false),
                        style = planText(FontWeight.SemiBold, 15, 20),
                        color = if (s.spent == 0L) colors.subtleText else colors.income,
                    )
                    Text(" · ", style = planText(FontWeight.Normal, 12, 16), color = colors.subtleText)
                    Text(
                        "${Money.format(p.dailyAllowance, showPaise = false)}/day",
                        style = planText(FontWeight.Normal, 12, 16),
                        color = colors.subtleText,
                    )
                }
            }
        }
    }
}

/** The redesigned Budgets tab: overall card first, category cards next, envelope toggle last. */
@Composable
private fun BudgetsTab(
    state: PlanningState,
    onAdd: () -> Unit,
    onSet: (String?, Long, Boolean?) -> Unit,
    onDelete: (BudgetStatus) -> Unit,
    onEnvelopeMode: (Boolean) -> Unit = {},
) {
    val colors = SumvaloTheme.colors
    val hidden = LocalHideAmounts.current
    var editing by remember { mutableStateOf<BudgetStatus?>(null) }
    var deleting by remember { mutableStateOf<BudgetStatus?>(null) }
    if (state.loaded && state.budgets.isEmpty()) {
        EmptyState(
            "No budgets yet",
            "Set a monthly limit overall or per category. Sumvalo tracks your pace and warns you before you overspend.",
            Modifier.padding(top = 40.dp),
            icon = Icons.Outlined.PieChart,
            actionLabel = "Create a budget",
            onAction = onAdd,
        )
        return
    }
    val dayFraction = state.budgets.mapNotNull { it.projection }.firstOrNull()?.let {
        it.daysElapsed.toFloat() / it.daysInMonth.coerceAtLeast(1)
    } ?: 0f
    LazyColumn(contentPadding = PaddingValues(16.dp, 8.dp, 16.dp, 16.dp), verticalArrangement = Arrangement.spacedBy(14.dp)) {
        state.budgets.forEachIndexed { i, s ->
            val p = s.projection
            item(key = s.budget.id.toString()) {
                BudgetCard(
                    s = s,
                    p = p,
                    hidden = hidden,
                    dayFraction = dayFraction,
                    barDp = if (s.isOverall) 8f else 6f,
                    markerDp = 4f,
                    onClick = { editing = s },
                )
            }
        }
        item(key = "envelope-mode") {
            EnvelopeToggle(state.envelopeMode, onEnvelopeMode)
        }
    }


    editing?.let { s ->
        EditBudgetSheet(
            s,
            onDismiss = { editing = null },
            onSave = { limit, roll ->
                onSet(s.budget.categoryKey, limit, roll)
                editing = null
            },
            onDelete = {
                deleting = s
                editing = null
            },
        )
    }
    deleting?.let { s ->
        ConfirmDialog("Delete budget?", "The limit is removed; your transactions are not affected.", "Delete", onConfirm = {
            onDelete(s)
            deleting = null
        }, onDismiss = { deleting = null }, destructive = true)
    }
}

/** Envelope-mode toggle card at the bottom of the Budgets tab. */
@Composable
private fun EnvelopeToggle(on: Boolean, onToggle: (Boolean) -> Unit) {
    val colors = SumvaloTheme.colors
    SumvaloCard(contentPadding = PaddingValues(16.dp)) {
        Row(
            Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Column(Modifier.weight(1f)) {
                Text("Envelope mode", style = planText(FontWeight.SemiBold, 14, 20), color = colors.ink)
                Text(
                    "Assign this month's income to budgets until nothing is left over",
                    style = planText(FontWeight.Normal, 12, 16),
                    color = colors.subtleText,
                )
            }
            Spacer(Modifier.weight(1f))
            Box(
                Modifier
                    .size(11.dp, 6.dp)
                    .clip(RoundedCornerShape(50))
                    .background(if (on) colors.ink else colors.fill)
                    .clickable(role = Role.Button, onClick = { onToggle(!on) }),
            ) {
                Box(
                    Modifier
                        .size(5.dp)
                        .clip(RoundedCornerShape(50))
                        .background(if (on) Color.White else Color(0xFF8A8A8A))
                        .offset(x = if (on) 6.dp else 0.dp),
                )
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun EditBudgetSheet(s: BudgetStatus, onDismiss: () -> Unit, onSave: (Long, Boolean) -> Unit, onDelete: () -> Unit) {
    var limit by remember { mutableStateOf<Long?>(s.budget.monthlyLimit) }
    var rollover by remember { mutableStateOf(s.budget.rollover) }
    ModalBottomSheet(onDismissRequest = onDismiss) {
        Column(Modifier.padding(horizontal = 20.dp).navigationBarsPadding()) {
            Text(
                if (s.isOverall) "Overall budget" else s.category?.name ?: "Budget",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold,
            )
            Spacer(Modifier.height(12.dp))
            FormColumn {
                AmountField("Monthly limit", limit, { limit = it }, allowZero = false)
                com.acuranza.app.ui.common.SwitchRow(
                    "Roll over",
                    "Unspent money adds to next month; overspending comes out of it (up to one month's limit)",
                    rollover,
                    { rollover = it },
                )
                PrimaryButton("Save", enabled = (limit ?: 0) > 0, modifier = Modifier.fillMaxWidth(), onClick = { onSave(limit!!, rollover) })
                TextButton(onClick = onDelete, modifier = Modifier.fillMaxWidth()) { Text("Delete budget", color = SumvaloTheme.colors.alert) }
            }
            Spacer(Modifier.height(16.dp))
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun AddBudgetSheet(
    categories: List<CategoryEntity>,
    hasOverall: Boolean,
    onDismiss: () -> Unit,
    onSave: (String?, Long) -> Unit,
) {
    val options: List<CategoryEntity?> = (if (hasOverall) emptyList<CategoryEntity?>() else listOf(null)) + categories
    var selected by remember { mutableStateOf(options.firstOrNull()) }
    var limit by remember { mutableStateOf<Long?>(null) }
    ModalBottomSheet(onDismissRequest = onDismiss) {
        Column(Modifier.padding(horizontal = 20.dp).navigationBarsPadding()) {
            Text("New budget", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
            Spacer(Modifier.height(12.dp))
            FormColumn {
                ChoiceField(
                    "For",
                    options,
                    selected,
                    { it?.name ?: "All spending (overall)" },
                    { selected = it },
                    leading = { c -> if (c != null) CategoryIcon(c.icon, c.color(), size = 28.dp) },
                )
                AmountField("Monthly limit", limit, { limit = it }, allowZero = false)
                PrimaryButton("Save budget", enabled = (limit ?: 0) > 0 && options.isNotEmpty(), modifier = Modifier.fillMaxWidth(), onClick = {
                    onSave(selected?.key, limit!!)
                })
            }
            Spacer(Modifier.height(16.dp))
        }
    }
}

@Composable
private fun GoalsTab(
    state: PlanningState,
    onAdd: () -> Unit,
    onOpen: (GoalEntity) -> Unit,
    onContribute: (Long, Long) -> Unit,
    onInflation: (Long, Double) -> Unit = { _, _ -> },
) {
    val hidden = LocalHideAmounts.current
    var contributing by remember { mutableStateOf<GoalStatus?>(null) }
    if (state.loaded && state.goals.isEmpty()) {
        EmptyState(
            "No goals yet",
            "An emergency fund, a trip, a new phone: set a target and a date, and Sumvalo tells you how much to save each month.",
            Modifier.padding(top = 40.dp),
            icon = Icons.Outlined.Flag,
            actionLabel = "Create a goal",
            onAction = onAdd,
        )
        return
    }
    LazyColumn(contentPadding = PaddingValues(20.dp, 8.dp, 20.dp, 110.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
        items(state.goals, key = { it.goal.id }) { g ->
            GoalStatusCard(
                g = g,
                hidden = hidden,
                outlook = state.outlooks[g.goal.id],
                onOpen = { onOpen(g.goal) },
                onInflation = { pct -> onInflation(g.goal.id, pct) },
                onContribute = { contributing = g },
            )
        }
    }
    contributing?.let { g ->
        AmountSheet("Add to ${g.goal.name}", null, onDismiss = { contributing = null }, onConfirm = {
            onContribute(g.goal.id, it)
            contributing = null
        }, type = TxnType.INCOME)
    }
}

/** One goal card: 64dp donut, name + saved of target, save plan line, outlook row and Add savings. */
@Composable
private fun GoalStatusCard(
    g: GoalStatus,
    hidden: Boolean,
    outlook: com.acuranza.app.repo.GoalOutlook?,
    onOpen: () -> Unit,
    onInflation: (Double) -> Unit,
    onContribute: () -> Unit,
) {
    val colors = SumvaloTheme.colors
    val plan = g.plan
    val money = { p: Long -> if (hidden) "₹ ••••" else Money.compact(p) }
    val full = { p: Long -> if (hidden) "₹ ••••" else Money.format(p, showPaise = false) }
    val pct = (plan.progress * 100).roundToInt().coerceIn(0, 100)
    // What has to be saved: the price at the date (with any rise) less the loan, so the ring and "reached" agree.
    val target = plan.target.takeIf { it > 0 } ?: g.goal.target
    SumvaloCard(contentPadding = PaddingValues(16.dp), onClick = onOpen) {
        Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                Box(Modifier.size(64.dp), contentAlignment = Alignment.Center) {
                    Canvas(Modifier.fillMaxSize()) {
                        val stroke = 5.dp.toPx()
                        drawArc(
                            color = colors.hairline,
                            startAngle = 0f,
                            sweepAngle = 360f,
                            useCenter = false,
                            topLeft = Offset(stroke / 2, stroke / 2),
                            size = Size(size.width - stroke, size.height - stroke),
                            style = Stroke(width = stroke),
                        )
                        if (pct > 0) {
                            drawArc(
                                color = colors.ink,
                                startAngle = -90f,
                                sweepAngle = (plan.progress * 360.0).coerceAtLeast(1.0).toFloat(),
                                useCenter = false,
                                topLeft = Offset(stroke / 2, stroke / 2),
                                size = Size(size.width - stroke, size.height - stroke),
                                style = Stroke(width = stroke),
                            )
                        }
                    }
                    Text("$pct%", style = planText(FontWeight.Bold, 15, 20), color = colors.ink)
                }
                Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    Text(
                        g.goal.name,
                        style = planText(FontWeight.SemiBold, 16, 22),
                        color = colors.ink,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(money(g.goal.saved), style = planText(FontWeight.Bold, 20, 24), color = colors.ink)
                        Text(" of ", style = planText(FontWeight.Medium, 13, 20), color = colors.subtleText)
                        Text(money(target), style = planText(FontWeight.Medium, 13, 20), color = colors.subtleText)
                    }
                }
            }
            Text(
                planLine(g, full, colors.income),
                style = planText(FontWeight.Medium, 12, 16),
                color = colors.subtleText,
            )
            if (outlook != null && !plan.reached) {
                Box(
                    Modifier
                        .fillMaxWidth()
                        .height(1.dp)
                        .background(colors.hairline),
                )
                GoalOutlookSection(outlook, g.deadline, hidden, onInflation = onInflation)
            }
            if (!plan.reached) {
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
                    Row(
                        Modifier
                            .clip(RoundedCornerShape(50))
                            .clickable(role = Role.Button, onClick = onContribute)
                            .padding(horizontal = 8.dp, vertical = 4.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(4.dp),
                    ) {
                        Icon(Icons.Outlined.Add, contentDescription = null, tint = colors.ink, modifier = Modifier.size(13.dp))
                        Text("Add savings", style = planText(FontWeight.SemiBold, 13, 18), color = colors.ink)
                    }
                }
            }
        }
    }
}

/**
 * The wrapped plan line under the goal title — "Save ₹X/month at 7% a year · 10 months left" — with the
 * loan, tight and off-track qualifiers the old hint carried. [green] styles the monthly amount.
 */
private fun planLine(g: GoalStatus, money: (Long) -> String, green: Color): AnnotatedString = buildAnnotatedString {
    val plan = g.plan
    when (g.standing) {
        GoalStanding.REACHED -> {
            pushStyle(SpanStyle(color = green, fontWeight = FontWeight.SemiBold))
            append("Goal reached")
            pop()
        }
        GoalStanding.OVERDUE -> append("Deadline passed · ${Fmt.date(g.deadline)} · ${money(plan.gap)} to go")
        GoalStanding.DUE_NOW -> append("${money(plan.gap)} needed by ${Fmt.date(g.deadline)}")
        else -> {
            append("Save ")
            pushStyle(SpanStyle(color = green, fontWeight = FontWeight.SemiBold))
            append("${money(plan.requiredMonthly)}/month")
            pop()
            if (g.hasLoan) {
                append(" + loan ${money(g.goal.loanAmount)} (EMI ${money(g.loanEmi)} for ${g.goal.loanTenureMonths} months)")
            } else {
                append(" at ${Fmt.pct(g.goal.expectedReturnPct, 0)} a year")
            }
            append(" · ${plan.monthsLeft} month${if (plan.monthsLeft == 1) "" else "s"} left")
            val now = (g.freeNow ?: 0).coerceAtLeast(0)
            val overall = (g.capacity ?: 0).coerceAtLeast(0)
            if (g.standing == GoalStanding.TIGHT) {
                append(if (plan.requiredMonthly > now) " · tight: ${money(now)} free now, more once earlier goals finish" else " · tight: ${money(overall)} free a month")
            }
            if (g.standing == GoalStanding.OFF_TRACK) append(" · more than the ${money(overall)} you usually have free")
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun GoalSheet(
    goal: GoalEntity?,
    onDismiss: () -> Unit,
    onSave: (GoalEntity) -> Unit,
    onDelete: (() -> Unit)?,
    inflationPct: Double = 0.0,
) {
    val haptic = rememberHaptic()
    var name by remember { mutableStateOf(goal?.name ?: "") }
    var target by remember { mutableStateOf(goal?.target) }
    var saved by remember { mutableLongStateOf(goal?.saved ?: 0L) }
    var deadline by remember { mutableStateOf(goal?.deadline?.let(LocalDate::ofEpochDay) ?: Time.today().plusYears(1)) }
    var ret by remember { mutableStateOf((goal?.expectedReturnPct ?: 7.0).toString()) }
    var loanOn by remember { mutableStateOf((goal?.loanAmount ?: 0) > 0) }
    var loanAmount by remember { mutableStateOf(goal?.loanAmount?.takeIf { it > 0 }) }
    var loanRate by remember { mutableStateOf((goal?.loanRatePct ?: 9.0).toString()) }
    var loanMonths by remember { mutableStateOf((goal?.loanTenureMonths ?: 36).toString()) }
    var confirmDelete by remember { mutableStateOf(false) }
    ModalBottomSheet(onDismissRequest = onDismiss) {
        Column(Modifier.padding(horizontal = 20.dp).navigationBarsPadding().verticalScroll(rememberScrollState())) {
            Text(if (goal == null) "New goal" else "Edit goal", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
            Spacer(Modifier.height(12.dp))
            FormColumn {
                SumvaloTextField(name, { name = it }, "Goal name", supporting = "e.g. Emergency fund, Goa trip")
                AmountField("Target amount", target, { target = it }, allowZero = false)
                AmountField("Already saved", saved, { saved = it })
                DateField("Target date", deadline, { deadline = it })
                ReturnField(ret, { ret = it })
                LoanSection(
                    target = target,
                    enabled = loanOn,
                    onEnabled = { loanOn = it },
                    amount = loanAmount,
                    onAmount = { loanAmount = it },
                    rate = loanRate,
                    onRate = { loanRate = it },
                    months = loanMonths,
                    onMonths = { loanMonths = it },
                )
                val loanForPlan = if (loanOn) (loanAmount ?: 0) else 0
                RequiredSaving(target, saved, deadline, ret.toDoubleOrNull()?.coerceIn(0.0, 30.0) ?: 7.0, loanForPlan, inflationPct)
                // A past date gives no plan; an existing goal can still be saved with its date unchanged.
                val dateOk = deadline.isAfter(Time.today()) || (goal != null && deadline.toEpochDay() == goal.deadline)
                PrimaryButton(
                    if (goal == null) "Create goal" else "Save",
                    enabled = name.isNotBlank() && (target ?: 0) > 0 && dateOk,
                    modifier = Modifier.fillMaxWidth(),
                    onClick = {
                        haptic()
                        onSave(
                            (goal ?: GoalEntity(name = "", target = 0, deadline = 0)).copy(
                                name = name.trim(),
                                target = target!!,
                                saved = saved,
                                deadline = deadline.toEpochDay(),
                                expectedReturnPct = ret.toDoubleOrNull()?.coerceIn(0.0, 30.0) ?: 7.0,
                                loanAmount = if (loanOn) (loanAmount ?: 0).coerceIn(0, target!!) else 0,
                                loanRatePct = loanRate.toDoubleOrNull()?.coerceIn(0.0, 40.0) ?: 9.0,
                                loanTenureMonths = loanMonths.toIntOrNull()?.coerceIn(1, 360) ?: 36,
                            ),
                        )
                    },
                )
                if (onDelete != null) {
                    TextButton(onClick = { confirmDelete = true }, modifier = Modifier.fillMaxWidth()) {
                        Text("Delete goal", color = SumvaloTheme.colors.alert)
                    }
                }
            }
            Spacer(Modifier.height(16.dp))
        }
    }
    if (confirmDelete && onDelete != null) {
        ConfirmDialog("Delete goal?", "This removes the goal and its saved amount.", "Delete", onConfirm = {
            confirmDelete = false
            onDelete()
        }, onDismiss = { confirmDelete = false }, destructive = true)
    }
}

/** Typical yearly returns, offered as one-tap choices for a goal's savings. */
private val RETURN_PRESETS = listOf("Savings account" to 3.0, "FD" to 7.0, "Debt fund" to 7.0, "Equity fund" to 12.0)

/**
 * The yearly return expected on the money set aside for a goal. Sumvalo assumes each monthly saving earns this
 * return until the target date, so a higher return means a smaller monthly saving is needed.
 */
@Composable
private fun ReturnField(value: String, onValue: (String) -> Unit) {
    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
        SumvaloTextField(
            value,
            { onValue(it.filter { ch -> ch.isDigit() || ch == '.' }) },
            "Expected return on your savings (% per year)",
            keyboardType = KeyboardType.Decimal,
            suffix = "%",
        )
        Text(
            "The yearly return you expect on the money you put aside for this goal, depending on where you keep it. " +
                "Sumvalo uses it to work out how much to save each month.",
            style = MaterialTheme.typography.bodySmall,
            color = SumvaloTheme.colors.subtleText,
        )
        androidx.compose.foundation.lazy.LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            items(RETURN_PRESETS.size) { i ->
                val (label, pct) = RETURN_PRESETS[i]
                androidx.compose.material3.FilterChip(
                    selected = value.toDoubleOrNull() == pct,
                    onClick = { onValue(pct.toInt().toString()) },
                    label = { Text("$label ${pct.toInt()}%") },
                )
            }
        }
    }
}

/**
 * The loan the user is willing to take at purchase: amount (or % of the target), rate and tenure, with the
 * EMI and total interest. The goal's savings target becomes target − loan.
 */
@Composable
private fun LoanSection(
    target: Long?,
    enabled: Boolean,
    onEnabled: (Boolean) -> Unit,
    amount: Long?,
    onAmount: (Long) -> Unit,
    rate: String,
    onRate: (String) -> Unit,
    months: String,
    onMonths: (String) -> Unit,
) {
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Column(Modifier.weight(1f)) {
                Text("Loan at purchase", style = MaterialTheme.typography.bodyLarge, fontWeight = FontWeight.SemiBold)
                Text("Part of the cost you're OK to borrow when you buy", style = MaterialTheme.typography.bodySmall, color = SumvaloTheme.colors.subtleText)
            }
            androidx.compose.material3.Switch(checked = enabled, onCheckedChange = onEnabled)
        }
        if (!enabled) return@Column
        val t = target ?: 0L
        val pct = if (t > 0) ((amount ?: 0).toDouble() / t * 100).coerceIn(0.0, 80.0) else 0.0
        AmountField("Loan amount", amount, onAmount)
        if (t > 0) {
            Text("${pct.toInt()}% of the target", style = MaterialTheme.typography.labelMedium, color = SumvaloTheme.colors.subtleText)
            androidx.compose.material3.Slider(
                value = pct.toFloat(),
                onValueChange = { onAmount((t * it / 100.0).toLong() / 100 * 100) },
                valueRange = 0f..80f,
                steps = 15,
            )
        }
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            SumvaloTextField(rate, { onRate(it.filter { ch -> ch.isDigit() || ch == '.' }) }, "Interest", Modifier.weight(1f),
                keyboardType = KeyboardType.Decimal, suffix = "% p.a.")
            SumvaloTextField(months, { onMonths(it.filter(Char::isDigit).take(3)) }, "Tenure", Modifier.weight(1f),
                keyboardType = KeyboardType.Number, suffix = "months")
        }
        val principal = (amount ?: 0).coerceAtMost(t)
        val r = rate.toDoubleOrNull() ?: 9.0
        val n = months.toIntOrNull() ?: 36
        if (principal > 0 && n > 0) {
            val emi = com.acuranza.app.domain.GoalLoan.emi(principal, r, n)
            val interest = com.acuranza.app.domain.GoalLoan.totalInterest(principal, r, n)
            SumvaloCard(contentPadding = PaddingValues(14.dp)) {
                Text("EMI after purchase", style = MaterialTheme.typography.labelMedium, color = SumvaloTheme.colors.subtleText)
                Text("${Money.format(emi, showPaise = false)} a month for $n months", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
                Text("Total interest ${Money.format(interest, showPaise = false)} · you save the remaining " +
                    "${Money.format(com.acuranza.app.domain.GoalLoan.savingsTarget(t, principal), showPaise = false)}",
                    style = MaterialTheme.typography.bodySmall, color = SumvaloTheme.colors.subtleText)
            }
        }
    }
}

/** Live "Required monthly saving" for the goal as the inputs change, by the same plan the Goals screen shows. */
@Composable
private fun RequiredSaving(target: Long?, saved: Long, deadline: LocalDate, returnPct: Double, loan: Long, inflationPct: Double) {
    val t = target ?: return
    val plan = remember(t, saved, deadline, returnPct, loan, inflationPct) {
        com.acuranza.app.finance.Goals.planGoal(t, saved, deadline, Time.today(), returnPct, loan, inflationPct)
    }
    SumvaloCard(contentPadding = PaddingValues(14.dp)) {
        Text("Required monthly saving", style = MaterialTheme.typography.labelMedium, color = SumvaloTheme.colors.subtleText)
        Text(
            when {
                plan.reached -> "Already reached"
                plan.overdue || !deadline.isAfter(Time.today()) -> "Pick a date in the future"
                plan.dueNow -> Money.format(plan.gap, showPaise = false) + " needed before the date (no month-end left)"
                else -> Money.format(plan.requiredMonthly, showPaise = false) + " a month for ${plan.monthsLeft} month" +
                    if (plan.monthsLeft == 1) "" else "s"
            },
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.SemiBold,
        )
        if (!plan.reached && !plan.overdue && !plan.dueNow) {
            Text(
                "Saved at each month-end, earning ${Fmt.pct(returnPct, 0)} a year" +
                    (if (inflationPct > 0) ", for a price rising ${Fmt.pct(inflationPct, 0)} a year" else "") + ".",
                style = MaterialTheme.typography.bodySmall,
                color = SumvaloTheme.colors.subtleText,
            )
        }
    }
}

