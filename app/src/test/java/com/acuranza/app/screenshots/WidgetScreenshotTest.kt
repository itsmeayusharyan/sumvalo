package com.acuranza.app.screenshots

import android.view.LayoutInflater
import android.view.View
import android.view.View.MeasureSpec
import android.widget.FrameLayout
import android.widget.ImageView
import android.widget.TextView
import app.cash.paparazzi.DeviceConfig
import app.cash.paparazzi.Paparazzi
import com.acuranza.app.R
import com.acuranza.app.tools.CategorySlice
import com.acuranza.app.tools.GoalProgress
import com.acuranza.app.tools.WidgetFigures
import com.acuranza.app.tools.WidgetViews
import org.junit.Rule
import org.junit.Test

/**
 * Goldens of the ACTUAL home-screen widgets (day configuration), not the in-app preview: each
 * test inflates the real widget layout and applies the real render MODEL the RemoteViews
 * builder consumes — [WidgetViews.monthModel]/[goalModel]/[categoriesModel] — into a frame
 * sized like a real launcher cell box, at the configured device density. The models are the
 * single source of what each widget displays (the RemoteViews builders are built FROM them),
 * so these goldens cannot drift from what a launcher shows; only the trivial findViewById
 * plumbing differs, because this JVM harness cannot replay RemoteViews' reflection actions.
 *
 * Quick Add and Shortcuts carry no data and are inflated directly.
 *
 * Night variants live in [WidgetNightScreenshotTest] — layoutlib switches night resources via
 * the device configuration, not createConfigurationContext (null in this environment).
 */
class WidgetScreenshotTest {
    @get:Rule
    val paparazzi = Paparazzi(deviceConfig = DeviceConfig.PIXEL_6, maxPercentDifference = 0.1)

    /** PIXEL_6 is 420dpi; launcher boxes are measured in dp, so px = dp * 2.625 here. */
    private val density: Float get() = paparazzi.context.resources.displayMetrics.density

    private fun dp(v: Int): Int = (v * density).toInt()

    private fun figures() = WidgetFigures(income = 85_000_00, spent = 52_400_00, barPercent = 62)

    private fun slices() = listOf(
        CategorySlice("Food", 18_400_00, 0.35f, 0xFF2563EB.toInt()),
        CategorySlice("Transport", 10_100_00, 0.19f, 0xFF0D9488.toInt()),
        CategorySlice("Shopping", 8_600_00, 0.17f, 0xFF9333EA.toInt()),
        CategorySlice("Bills", 7_300_00, 0.14f, 0xFFEA580C.toInt()),
        CategorySlice("Other", 8_000_00, 0.15f, 0xFF94A3B8.toInt()),
    )

    private fun frameWith(layout: Int): FrameLayout {
        val frame = FrameLayout(paparazzi.context)
        LayoutInflater.from(paparazzi.context).inflate(layout, frame, true)
        return frame
    }

    private fun snap(name: String, frame: FrameLayout, widthDp: Int, heightDp: Int) {
        val w = dp(widthDp)
        val h = dp(heightDp)
        frame.measure(MeasureSpec.makeMeasureSpec(w, MeasureSpec.EXACTLY), MeasureSpec.makeMeasureSpec(h, MeasureSpec.EXACTLY))
        frame.layout(0, 0, w, h)
        paparazzi.snapshot(frame, name)
    }

    private fun text(frame: FrameLayout, id: Int, value: String) {
        frame.findViewById<TextView>(id)?.text = value
    }

    private fun visibility(frame: FrameLayout, id: Int, visible: Boolean) {
        frame.findViewById<View>(id)?.visibility = if (visible) View.VISIBLE else View.GONE
    }

    @Test
    fun widgetMonth() {
        val ctx = paparazzi.context
        val m = WidgetViews.monthModel(ctx, figures(), show = true, title = "This month", ringFraction = 0.62f, budgetCaption = "62% of ₹80K budget")
        val frame = frameWith(R.layout.widget_month)
        text(frame, R.id.widget_title, m.title)
        text(frame, R.id.widget_saved, m.saved)
        frame.findViewById<TextView>(R.id.widget_saved)?.setTextColor(m.savedColor)
        text(frame, R.id.widget_rate, m.rate)
        text(frame, R.id.widget_income, m.income)
        text(frame, R.id.widget_spent, m.spent)
        text(frame, R.id.widget_bar_caption, m.caption)
        frame.findViewById<ImageView>(R.id.widget_ring)?.setImageBitmap(
            WidgetViews.ringBitmap(ctx, ctx.getColor(R.color.widget_accent), m.ringFraction),
        )
        snap("widget_month_small", frame, 220, 150)
        snap("widget_month_large", frame, 300, 220)
    }

    @Test
    fun widgetGoal() {
        val ctx = paparazzi.context
        val m = WidgetViews.goalModel(ctx, GoalProgress(30_000_00, 40_000_00), show = true)
        val frame = frameWith(R.layout.widget_goal)
        visibility(frame, R.id.widget_goal_body, !m.empty)
        visibility(frame, R.id.widget_empty, m.empty)
        text(frame, R.id.widget_goal_saved, m.saved)
        text(frame, R.id.widget_goal_caption, m.caption)
        text(frame, R.id.widget_goal_target, m.target)
        frame.findViewById<ImageView>(R.id.widget_goal_ring)?.setImageBitmap(
            WidgetViews.ringBitmap(ctx, ctx.getColor(R.color.widget_income), m.ringFraction),
        )
        snap("widget_goal", frame, 220, 150)
    }

    @Test
    fun widgetGoalEmpty() {
        val ctx = paparazzi.context
        val m = WidgetViews.goalModel(ctx, null, show = true)
        val frame = frameWith(R.layout.widget_goal)
        visibility(frame, R.id.widget_goal_body, !m.empty)
        visibility(frame, R.id.widget_empty, m.empty)
        snap("widget_goal_empty", frame, 220, 150)
    }

    @Test
    fun widgetCategories() {
        val ctx = paparazzi.context
        val m = WidgetViews.categoriesModel(ctx, slices(), show = true)
        val frame = frameWith(R.layout.widget_categories)
        visibility(frame, R.id.widget_donut_frame, !m.empty)
        visibility(frame, R.id.widget_cat_legend, !m.empty)
        visibility(frame, R.id.widget_cat_empty, m.empty)
        text(frame, R.id.widget_cat_total, m.total)
        text(frame, R.id.widget_cat_legend, m.legend)
        frame.findViewById<ImageView>(R.id.widget_donut)?.setImageBitmap(WidgetViews.donutBitmap(ctx, m.slices))
        snap("widget_categories", frame, 220, 150)
    }

    @Test
    fun widgetCategoriesEmpty() {
        val ctx = paparazzi.context
        val m = WidgetViews.categoriesModel(ctx, emptyList(), show = true)
        val frame = frameWith(R.layout.widget_categories)
        visibility(frame, R.id.widget_donut_frame, !m.empty)
        visibility(frame, R.id.widget_cat_legend, !m.empty)
        visibility(frame, R.id.widget_cat_empty, m.empty)
        snap("widget_categories_empty", frame, 220, 150)
    }

    @Test
    fun widgetQuickAdd() {
        snap("widget_quick_add", frameWith(R.layout.widget_quick_add), 220, 70)
    }

    @Test
    fun widgetShortcuts() {
        snap("widget_shortcuts", frameWith(R.layout.widget_shortcuts), 220, 150)
    }
}
