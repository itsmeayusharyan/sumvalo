package com.acuranza.app.tools

import android.content.Context
import android.widget.RemoteViews
import com.acuranza.app.R
import com.acuranza.app.model.Money

/**
 * The single rendering path for every widget: builds the exact RemoteViews each provider ships,
 * from plain data. The updaters feed real figures; the in-app Add Widgets screen feeds SAMPLE
 * figures — previews are the actual widget rendering, so they can never drift from it.
 */
object WidgetViews {
    private const val HIDDEN = "₹ ••••"

    /**
     * Raw-pixel bitmap sizes were the core of "preview looks right, launcher doesn't": a bitmap
     * drawn at 200px scales to a different physical size on every device density. Widget charts
     * are therefore drawn at the exact pixel size of the box the layout reserves (dp × the
     * device density), so they render crisp and identical everywhere the preview does.
     */
    private fun dpToPx(context: Context, dp: Float): Int = (dp * context.resources.displayMetrics.density).toInt()

    /**
     * The ring the month and goal widgets draw. Shared with the widget screenshot tests, which
     * must reproduce the exact bitmap the launcher path uses (see the tests for why they set it
     * by hand after apply).
     */
    internal fun ringBitmap(context: Context, progressColor: Int, progress: Float): android.graphics.Bitmap =
        WidgetCharts.ring(
            sizePx = dpToPx(context, 76f),
            progress = progress,
            trackColor = context.getColor(R.color.widget_track),
            progressColor = progressColor,
            strokeWidthPx = dpToPx(context, 8f).toFloat(),
        )

    /** The category donut, shared with the widget screenshot tests for the same reason. */
    internal fun donutBitmap(context: Context, slices: List<Pair<Int, Float>>): android.graphics.Bitmap =
        WidgetCharts.donut(
            sizePx = dpToPx(context, 84f),
            slices = slices,
            strokeWidthPx = dpToPx(context, 10f).toFloat(),
            gapDegrees = 3f,
        )

    // ---- Render models ----
    // The values each widget displays, independent of the RemoteViews mechanism: the builders
    // below turn a model into RemoteViews for launchers; the widget screenshot tests apply the
    // same model to the same layout inflated directly (the JVM render harness cannot replay
    // RemoteViews' reflection actions). One definition of what a widget shows — no drift.

    /** What the This Month widget displays. */
    internal data class MonthModel(
        val title: String,
        val saved: String,
        val savedColor: Int,
        val rate: String,
        val income: String,
        val spent: String,
        val caption: String,
        val ringFraction: Float,
    )

    /** What the Savings Goal widget displays; [empty] selects the setup prompt over the body. */
    internal data class GoalModel(
        val empty: Boolean,
        val saved: String,
        val caption: String,
        val target: String,
        val ringFraction: Float,
    )

    /** What the Spending by Category widget displays; [empty] selects the empty state. */
    internal data class CategoriesModel(
        val empty: Boolean,
        val total: String,
        val legend: String,
        val slices: List<Pair<Int, Float>>,
    )

    internal fun monthModel(
        context: Context,
        f: WidgetFigures,
        show: Boolean,
        title: String,
        ringFraction: Float,
        budgetCaption: String,
    ): MonthModel {
        val rate = f.ratePercent
        return MonthModel(
            title = title,
            saved = if (show) Money.format(f.saved, showPaise = false) else HIDDEN,
            savedColor = context.getColor(if (show && f.saved < 0) R.color.widget_overdue else R.color.widget_ink),
            rate = when {
                !show -> ""
                rate == null -> "No income yet"
                f.saved < 0 -> "Overspent"
                else -> "$rate% saved"
            },
            income = if (show) Money.compact(f.income) else HIDDEN,
            spent = if (show) Money.compact(f.spent) else HIDDEN,
            caption = if (show) budgetCaption else "",
            ringFraction = if (show) ringFraction else 0f,
        )
    }

    internal fun goalModel(context: Context, goal: GoalProgress?, show: Boolean): GoalModel {
        if (goal == null) return GoalModel(empty = true, saved = "", caption = "", target = "", ringFraction = 0f)
        return GoalModel(
            empty = false,
            saved = if (show) Money.format(goal.saved, showPaise = false) else HIDDEN,
            caption = when {
                !show -> ""
                goal.percent >= 100 -> "Goal reached"
                else -> "${Money.compact(goal.remaining)} to go"
            },
            target = if (show) "of ${Money.compact(goal.target)} · ${goal.percent}%" else HIDDEN,
            ringFraction = if (show) goal.percent / 100f else 0f,
        )
    }

    internal fun categoriesModel(context: Context, slices: List<CategorySlice>, show: Boolean): CategoriesModel {
        if (slices.isEmpty()) return CategoriesModel(empty = true, total = "", legend = "", slices = emptyList())
        val total = slices.sumOf { it.amount }
        val legend = slices.joinToString("\n") { slice ->
            val amount = if (show) Money.compact(slice.amount) else HIDDEN
            "${slice.name} · $amount"
        }
        return CategoriesModel(
            empty = false,
            total = if (show) Money.compact(total) else HIDDEN,
            legend = legend,
            slices = slices.map { it.color to it.fraction },
        )
    }

    // ---- RemoteViews builders (the single path the providers and the preview screen use) ----

    fun month(
        context: Context,
        f: WidgetFigures,
        show: Boolean,
        title: String,
        ringFraction: Float,
        budgetCaption: String,
    ): RemoteViews {
        val m = monthModel(context, f, show, title, ringFraction, budgetCaption)
        val views = RemoteViews(context.packageName, R.layout.widget_month)
        views.setTextViewText(R.id.widget_title, m.title)
        views.setTextViewText(R.id.widget_saved, m.saved)
        views.setTextColor(R.id.widget_saved, m.savedColor)
        views.setTextViewText(R.id.widget_rate, m.rate)
        views.setTextViewText(R.id.widget_income, m.income)
        views.setTextViewText(R.id.widget_spent, m.spent)
        views.setTextViewText(R.id.widget_bar_caption, m.caption)
        views.setImageViewBitmap(R.id.widget_ring, ringBitmap(context, context.getColor(R.color.widget_accent), m.ringFraction))
        return views
    }

    fun goal(context: Context, goal: GoalProgress?, show: Boolean): RemoteViews {
        val m = goalModel(context, goal, show)
        val views = RemoteViews(context.packageName, R.layout.widget_goal)
        views.setViewVisibility(R.id.widget_goal_body, if (m.empty) android.view.View.GONE else android.view.View.VISIBLE)
        views.setViewVisibility(R.id.widget_empty, if (m.empty) android.view.View.VISIBLE else android.view.View.GONE)
        if (m.empty) return views
        views.setTextViewText(R.id.widget_goal_saved, m.saved)
        views.setTextViewText(R.id.widget_goal_caption, m.caption)
        views.setTextViewText(R.id.widget_goal_target, m.target)
        views.setImageViewBitmap(R.id.widget_goal_ring, ringBitmap(context, context.getColor(R.color.widget_income), m.ringFraction))
        return views
    }

    fun categories(context: Context, slices: List<CategorySlice>, show: Boolean): RemoteViews {
        val m = categoriesModel(context, slices, show)
        val views = RemoteViews(context.packageName, R.layout.widget_categories)
        views.setViewVisibility(R.id.widget_donut_frame, if (m.empty) android.view.View.GONE else android.view.View.VISIBLE)
        views.setViewVisibility(R.id.widget_cat_legend, if (m.empty) android.view.View.GONE else android.view.View.VISIBLE)
        views.setViewVisibility(R.id.widget_cat_empty, if (m.empty) android.view.View.VISIBLE else android.view.View.GONE)
        if (m.empty) return views
        views.setTextViewText(R.id.widget_cat_total, m.total)
        views.setTextViewText(R.id.widget_cat_legend, m.legend)
        views.setImageViewBitmap(R.id.widget_donut, donutBitmap(context, m.slices))
        return views
    }

    fun quickAdd(context: Context): RemoteViews {
        val views = RemoteViews(context.packageName, R.layout.widget_quick_add)
        views.setOnClickPendingIntent(R.id.widget_add_expense, QuickLaunch.pending(context, QuickLaunch.ROUTE_ADD_EXPENSE, 7_201))
        views.setOnClickPendingIntent(R.id.widget_add_income, QuickLaunch.pending(context, QuickLaunch.ROUTE_ADD_INCOME, 7_202))
        views.setOnClickPendingIntent(R.id.widget_add_transfer, QuickLaunch.pending(context, QuickLaunch.ROUTE_TRANSFER, 7_203))
        views.setOnClickPendingIntent(R.id.widget_add_transactions, QuickLaunch.pending(context, QuickLaunch.ROUTE_TRANSACTIONS, 7_204))
        return views
    }

    fun shortcuts(context: Context): RemoteViews {
        val views = RemoteViews(context.packageName, R.layout.widget_shortcuts)
        views.setOnClickPendingIntent(R.id.widget_shortcut_bills, QuickLaunch.pending(context, QuickLaunch.ROUTE_BILLS, 7_301))
        views.setOnClickPendingIntent(R.id.widget_shortcut_budgets, QuickLaunch.pending(context, QuickLaunch.ROUTE_PLANNING, 7_302))
        views.setOnClickPendingIntent(R.id.widget_shortcut_reports, QuickLaunch.pending(context, QuickLaunch.ROUTE_REPORTS, 7_303))
        views.setOnClickPendingIntent(R.id.widget_shortcut_accounts, QuickLaunch.pending(context, QuickLaunch.ROUTE_ACCOUNTS, 7_304))
        return views
    }
}
