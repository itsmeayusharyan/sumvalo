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
import com.android.resources.NightMode
import org.junit.Rule
import org.junit.Test

/**
 * Night-configuration goldens of the actual home-screen widgets: the Paparazzi device config
 * sets NightMode.NIGHT, so every widget resource resolves from values-night exactly as a dark
 * launcher would — the layouts through RemoteViews inflation, and the chart bitmaps through the
 * same shared helpers with night-resolved colors (which is also what the providers draw when
 * the system is in dark mode).
 *
 * The same harness caveat as [WidgetScreenshotTest] applies: the bitmap action is completed by
 * hand after apply() (see that class's docs).
 */
class WidgetNightScreenshotTest {
    @get:Rule
    val paparazzi = Paparazzi(
        deviceConfig = DeviceConfig.PIXEL_6.copy(nightMode = NightMode.NIGHT),
        maxPercentDifference = 0.1,
    )

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

    @Test
    fun widgetMonthNight() {
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
        snap("widget_month_small_night", frame, 220, 150)
    }

    @Test
    fun widgetGoalNight() {
        val ctx = paparazzi.context
        val m = WidgetViews.goalModel(ctx, GoalProgress(30_000_00, 40_000_00), show = true)
        val frame = frameWith(R.layout.widget_goal)
        frame.findViewById<View>(R.id.widget_goal_body)?.visibility = if (m.empty) View.GONE else View.VISIBLE
        frame.findViewById<View>(R.id.widget_empty)?.visibility = if (m.empty) View.VISIBLE else View.GONE
        text(frame, R.id.widget_goal_saved, m.saved)
        text(frame, R.id.widget_goal_caption, m.caption)
        text(frame, R.id.widget_goal_target, m.target)
        frame.findViewById<ImageView>(R.id.widget_goal_ring)?.setImageBitmap(
            WidgetViews.ringBitmap(ctx, ctx.getColor(R.color.widget_income), m.ringFraction),
        )
        snap("widget_goal_night", frame, 220, 150)
    }

    @Test
    fun widgetCategoriesNight() {
        val ctx = paparazzi.context
        val m = WidgetViews.categoriesModel(ctx, slices(), show = true)
        val frame = frameWith(R.layout.widget_categories)
        frame.findViewById<View>(R.id.widget_donut_frame)?.visibility = if (m.empty) View.GONE else View.VISIBLE
        frame.findViewById<View>(R.id.widget_cat_legend)?.visibility = if (m.empty) View.GONE else View.VISIBLE
        frame.findViewById<View>(R.id.widget_cat_empty)?.visibility = if (m.empty) View.VISIBLE else View.GONE
        text(frame, R.id.widget_cat_total, m.total)
        text(frame, R.id.widget_cat_legend, m.legend)
        frame.findViewById<ImageView>(R.id.widget_donut)?.setImageBitmap(WidgetViews.donutBitmap(ctx, m.slices))
        snap("widget_categories_night", frame, 220, 150)
    }
}
