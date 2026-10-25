package com.acuranza.app.tools
import androidx.compose.ui.graphics.toArgb

import com.acuranza.app.designsystem.theme.BrandPalette

import android.appwidget.AppWidgetManager
import android.appwidget.AppWidgetProvider
import android.content.ComponentName
import android.content.Context
import android.widget.RemoteViews
import com.acuranza.app.AcuranzaApp
import com.acuranza.app.R
import com.acuranza.app.data.TransactionEntity
import com.acuranza.app.di.AppContainer
import com.acuranza.app.domain.MoneyIn
import com.acuranza.app.domain.SmsMapping
import com.acuranza.app.model.Money
import com.acuranza.app.model.Paise
import com.acuranza.app.model.TxnType
import com.acuranza.app.util.Period
import com.acuranza.app.util.Time
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.debounce
import kotlinx.coroutines.launch
import java.time.format.DateTimeFormatter
import java.util.Locale

/**
 * What the "This month" widget shows. [barPercent] is spending against the overall budget, or
 * against income when no budget exists. [ratePercent] is the savings rate (share of income kept),
 * clamped to 0..100; null when there was no income to speak of.
 */
data class WidgetFigures(val income: Paise, val spent: Paise, val barPercent: Int) {
    val saved: Paise get() = income - spent

    val ratePercent: Int?
        get() = if (income > 0) ((saved * 100) / income).toInt().coerceIn(0, 100) else null

    companion object {
        fun of(period: Period, txns: List<TransactionEntity>, overallBudget: Paise?): WidgetFigures {
            val counted = txns.filter { SmsMapping.countsInStats(it) && Time.toDate(it.timestamp) in period }
            val spent = MoneyIn.netSpent(counted.filter { it.type == TxnType.EXPENSE }, counted.filter(MoneyIn::isGivenBack))
            val income = counted.filter(MoneyIn::isEarned).sumOf { it.amount }
            val base = overallBudget?.takeIf { it > 0 } ?: income.takeIf { it > 0 }
            val pct = if (base == null) 0 else ((spent * 100) / base).toInt().coerceIn(0, 100)
            return WidgetFigures(income, spent, pct)
        }
    }
}

/** Progress toward the user's configured savings target for the current period. */
data class GoalProgress(val saved: Paise, val target: Paise) {
    val percent: Int get() = if (target > 0) ((saved * 100) / target).toInt().coerceIn(0, 100) else 0
    val remaining: Paise get() = if (target > saved) target - saved else 0

    companion object {
        /** A null or zero target means the user has no goal configured. */
        fun of(saved: Paise, target: Paise?): GoalProgress? = target?.takeIf { it > 0 }?.let { GoalProgress(saved, it) }
    }
}

/** One legend slice of the category donut. */
data class CategorySlice(val name: String, val amount: Paise, val fraction: Float, val color: Int)

object CategorySlices {
    /** Fallback palette when a category has no colour of its own (order-stable across refreshes). */
    private val PALETTE = intArrayOf(
        BrandPalette.Blue.toArgb(), 0xFF0D9488.toInt(), 0xFF9333EA.toInt(),
        0xFFEA580C.toInt(), 0xFF0284C7.toInt(), 0xFF65A30D.toInt(),
    )

    /**
     * This period's spending split by category: the [max] largest named categories, then
     * everything else merged into an "Other" slice. Colours come from the user's categories when
     * set, else the stable palette. Expenses only — income never belongs in a spending donut.
     */
    fun of(
        txns: List<TransactionEntity>,
        categoryNames: Map<String, String>,
        categoryColors: Map<String, Long>,
        max: Int = 4,
    ): List<CategorySlice> {
        val byCategory = txns.asSequence()
            .filter { SmsMapping.countsInStats(it) && it.type == TxnType.EXPENSE && !it.excludeFromStats }
            .groupBy { it.categoryKey }
            .map { (key, txnsOfKey) -> key to txnsOfKey.sumOf { it.amount } }
            .filter { it.second > 0 }
            .sortedByDescending { it.second }
        val total = byCategory.sumOf { it.second }
        if (total <= 0) return emptyList()

        val head = byCategory.take(max)
        val tailSum = byCategory.drop(max).sumOf { it.second }
        val rows = head.mapIndexed { i, (key, amount) ->
            val color = categoryColors[key]?.toInt() ?: PALETTE[i % PALETTE.size]
            CategorySlice(categoryNames[key] ?: key, amount, amount.toFloat() / total, color)
        } + (if (tailSum > 0) {
            listOf(CategorySlice("Other", tailSum, tailSum.toFloat() / total, 0xFF94A3B8.toInt()))
        } else {
            emptyList()
        })
        // Normalise so the donut always closes the circle regardless of rounding.
        val sum = rows.sumOf { it.fraction.toDouble() }
        return if (sum > 0) rows.map { it.copy(fraction = (it.fraction / sum).toFloat()) } else rows
    }
}

object WidgetUpdater {
    private const val HIDDEN = "₹ ••••"

    /** Amounts appear only without privacy mode, and with app lock on only if the user opted in. */
    fun showAmounts(hideAmounts: Boolean, lockEnabled: Boolean, amountsWhenLocked: Boolean): Boolean =
        !hideAmounts && (!lockEnabled || amountsWhenLocked)

    /** Refreshes every data widget whenever transactions, settings, widget prefs or the plan change. */
    @OptIn(FlowPreview::class)
    fun start(app: AcuranzaApp) {
        app.appScope.launch {
            combine(
                app.container.db.transactions().observeChanges(),
                app.container.settings.settings,
                ToolsSettings.get(app).prefs,
                app.container.featurePrefs.settings,
            ) { _, _, _, _ -> Unit }
                .debounce(1_500)
                .collect { runCatching { updateAll(app) } }
        }
    }

    suspend fun updateAll(context: Context) {
        val manager = AppWidgetManager.getInstance(context)
        val c = (context.applicationContext as AcuranzaApp).container
        MonthWidgetProvider::class.java.let {
            val ids = manager.getAppWidgetIds(ComponentName(context, it)); if (ids.isNotEmpty()) updateMonth(context, manager, ids, c)
        }
        SavingsGoalWidgetProvider::class.java.let {
            val ids = manager.getAppWidgetIds(ComponentName(context, it)); if (ids.isNotEmpty()) updateSavingsGoal(context, manager, ids, c)
        }
        CategoryDonutWidgetProvider::class.java.let {
            val ids = manager.getAppWidgetIds(ComponentName(context, it)); if (ids.isNotEmpty()) updateCategoryDonut(context, manager, ids, c)
        }
    }

    /**
     * The "This month" widget, Groww-style: muted label, headline net-savings figure with a rate
     * qualifier, income/spent value pairs, a savings-rate ring, and a budget progress band.
     */
    suspend fun updateMonth(context: Context, manager: AppWidgetManager, ids: IntArray, c: AppContainer) {
        val settings = c.settings.current()
        val tools = ToolsSettings.get(context).current()
        val period = Period.containing(Time.today(), settings.payCycle)
        val txns = c.db.transactions().between(period.startMillis, period.endMillis)
        val budget = c.db.budgets().all().firstOrNull { it.categoryKey == null }?.monthlyLimit
        val f = WidgetFigures.of(period, txns, budget)
        val show = showAmounts(settings.hideAmounts, settings.biometricLockEnabled, tools.widgetAmountsWhenLocked)

        val rate = f.ratePercent
        val ringFraction = when {
            budget != null && budget > 0 -> f.barPercent / 100f
            rate != null -> rate / 100f
            else -> 0f
        }
        val caption = when {
            budget != null && budget > 0 -> "${f.barPercent}% of ${Money.compact(budget)} budget"
            f.income > 0 -> "${f.barPercent}% of income spent"
            else -> "No activity yet this month"
        }
        val views = WidgetViews.month(
            context = context,
            f = f,
            show = show,
            title = title(period, settings.monthStartDay),
            ringFraction = ringFraction,
            budgetCaption = caption,
        )
        views.setOnClickPendingIntent(
            R.id.widget_root,
            QuickLaunch.pending(context, com.acuranza.app.notify.Notifications.ROUTE_INSIGHTS, 7_100),
        )
        ids.forEach { manager.updateAppWidget(it, views) }
    }

    /** The savings-goal widget: ring progress, headline saved figure, target and remaining. */
    suspend fun updateSavingsGoal(context: Context, manager: AppWidgetManager, ids: IntArray, c: AppContainer) {
        val settings = c.settings.current()
        val tools = ToolsSettings.get(context).current()
        val period = Period.containing(Time.today(), settings.payCycle)
        val txns = c.db.transactions().between(period.startMillis, period.endMillis)
        val f = WidgetFigures.of(period, txns, null)
        val show = showAmounts(settings.hideAmounts, settings.biometricLockEnabled, tools.widgetAmountsWhenLocked)
        val goal = GoalProgress.of(f.saved, c.featurePrefs.current().savingsTarget)

        val views = WidgetViews.goal(context, goal, show)
        // Opens the real Savings Goal screen — never the app default.
        views.setOnClickPendingIntent(R.id.widget_root, QuickLaunch.pending(context, QuickLaunch.ROUTE_SAVINGS_GOAL, 7_600))
        ids.forEach { manager.updateAppWidget(it, views) }
    }

    /** The spending-by-category widget: a donut of the period's top expenses with a legend. */
    suspend fun updateCategoryDonut(context: Context, manager: AppWidgetManager, ids: IntArray, c: AppContainer) {
        val settings = c.settings.current()
        val tools = ToolsSettings.get(context).current()
        val period = Period.containing(Time.today(), settings.payCycle)
        val txns = c.db.transactions().between(period.startMillis, period.endMillis)
        val categories = c.db.categories().all()
        val names = categories.associate { it.key to it.name }
        val colors = categories.associate { it.key to it.colorArgb }
        val slices = CategorySlices.of(txns, names, colors)
        val show = showAmounts(settings.hideAmounts, settings.biometricLockEnabled, tools.widgetAmountsWhenLocked)
        val total = slices.sumOf { it.amount }

        val views = WidgetViews.categories(context, slices, show)
        views.setOnClickPendingIntent(R.id.widget_root, QuickLaunch.pending(context, QuickLaunch.ROUTE_REPORTS, 7_700))
        ids.forEach { manager.updateAppWidget(it, views) }
    }

    private fun title(period: Period, monthStartDay: Int): String {
        if (monthStartDay == 1) return "This month"
        val fmt = DateTimeFormatter.ofPattern("d MMM", Locale.getDefault())
        return "${period.start.format(fmt)} – ${period.lastDay.format(fmt)}"
    }
}

/** "This month": headline net savings, rate, income/spent pairs, progress ring and budget band. */
class MonthWidgetProvider : AppWidgetProvider() {
    override fun onUpdate(context: Context, manager: AppWidgetManager, ids: IntArray) {
        val pending = goAsync()
        CoroutineScope(Dispatchers.IO).launch {
            try {
                runCatching { WidgetUpdater.updateMonth(context, manager, ids, (context.applicationContext as AcuranzaApp).container) }
            } finally {
                pending.finish()
            }
        }
    }

    /** The launcher resized the widget — re-render at the new proportions instead of leaving stale geometry. */
    override fun onAppWidgetOptionsChanged(context: Context, manager: AppWidgetManager, id: Int, newOptions: android.os.Bundle?) {
        val pending = goAsync()
        CoroutineScope(Dispatchers.IO).launch {
            try {
                runCatching { WidgetUpdater.updateMonth(context, manager, intArrayOf(id), (context.applicationContext as AcuranzaApp).container) }
            } finally {
                pending.finish()
            }
        }
    }
}

/** "Savings goal": ring progress and figures; tapping opens the Savings Goal screen. */
class SavingsGoalWidgetProvider : AppWidgetProvider() {
    override fun onUpdate(context: Context, manager: AppWidgetManager, ids: IntArray) {
        val pending = goAsync()
        CoroutineScope(Dispatchers.IO).launch {
            try {
                runCatching { WidgetUpdater.updateSavingsGoal(context, manager, ids, (context.applicationContext as AcuranzaApp).container) }
            } finally {
                pending.finish()
            }
        }
    }

    override fun onAppWidgetOptionsChanged(context: Context, manager: AppWidgetManager, id: Int, newOptions: android.os.Bundle?) {
        val pending = goAsync()
        CoroutineScope(Dispatchers.IO).launch {
            try {
                runCatching { WidgetUpdater.updateSavingsGoal(context, manager, intArrayOf(id), (context.applicationContext as AcuranzaApp).container) }
            } finally {
                pending.finish()
            }
        }
    }
}

/** "Spending by category": a donut of this period's top expenses with a legend; opens Reports. */
class CategoryDonutWidgetProvider : AppWidgetProvider() {
    override fun onUpdate(context: Context, manager: AppWidgetManager, ids: IntArray) {
        val pending = goAsync()
        CoroutineScope(Dispatchers.IO).launch {
            try {
                runCatching { WidgetUpdater.updateCategoryDonut(context, manager, ids, (context.applicationContext as AcuranzaApp).container) }
            } finally {
                pending.finish()
            }
        }
    }

    override fun onAppWidgetOptionsChanged(context: Context, manager: AppWidgetManager, id: Int, newOptions: android.os.Bundle?) {
        val pending = goAsync()
        CoroutineScope(Dispatchers.IO).launch {
            try {
                runCatching { WidgetUpdater.updateCategoryDonut(context, manager, intArrayOf(id), (context.applicationContext as AcuranzaApp).container) }
            } finally {
                pending.finish()
            }
        }
    }
}

/**
 * "Quick add": a 2×2 grid — each action opens the corresponding form or screen. The forms create
 * transactions, so repeated taps can never double-create anything.
 */
class QuickAddWidgetProvider : AppWidgetProvider() {
    override fun onUpdate(context: Context, manager: AppWidgetManager, ids: IntArray) {
        val views = WidgetViews.quickAdd(context)
        ids.forEach { manager.updateAppWidget(it, views) }
    }

    override fun onAppWidgetOptionsChanged(context: Context, manager: AppWidgetManager, id: Int, newOptions: android.os.Bundle?) {
        manager.updateAppWidget(id, WidgetViews.quickAdd(context))
    }
}

/**
 * Re-renders every data widget when the system configuration changes (a light/dark flip):
 * chart bitmaps are pre-drawn in the app process with the colors of the mode the app last
 * saw, so without this the ring and donut keep day colors on a night launcher until the
 * next data refresh. Layout colors re-resolve in the launcher; the bitmaps need this nudge.
 */
class WidgetConfigReceiver : android.content.BroadcastReceiver() {
    override fun onReceive(context: Context, intent: android.content.Intent) {
        if (intent.action != android.content.Intent.ACTION_CONFIGURATION_CHANGED) return
        CoroutineScope(Dispatchers.IO).launch {
            runCatching { WidgetUpdater.updateAll(context) }
        }
    }
}

/** "Shortcuts": compact navigation to the most-used screens. Static — no data, no refresh needed. */
class ShortcutsWidgetProvider : AppWidgetProvider() {
    override fun onUpdate(context: Context, manager: AppWidgetManager, ids: IntArray) {
        val views = WidgetViews.shortcuts(context)
        ids.forEach { manager.updateAppWidget(it, views) }
    }

    override fun onAppWidgetOptionsChanged(context: Context, manager: AppWidgetManager, id: Int, newOptions: android.os.Bundle?) {
        manager.updateAppWidget(id, WidgetViews.shortcuts(context))
    }
}
