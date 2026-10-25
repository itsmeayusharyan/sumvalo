package com.acuranza.app.tools

import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.RectF

/**
 * Canvas-drawn chart bitmaps for RemoteViews — RemoteViews can't draw arcs natively, so the
 * widgets render progress rings and category donuts into small bitmaps at update time and set
 * them with setImageViewBitmap. Bitmaps are tiny (widget resolution), drawn once per refresh.
 */
object WidgetCharts {

    /**
     * A progress ring: full track circle with the progress arc on top, rounded caps, starting at
     * 12 o'clock. [progress] clamps to 0..1. Returns a transparent-backed square bitmap.
     */
    fun ring(
        sizePx: Int,
        progress: Float,
        trackColor: Int,
        progressColor: Int,
        strokeWidthPx: Float,
    ): Bitmap {
        val bmp = Bitmap.createBitmap(sizePx, sizePx, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(bmp)
        val inset = strokeWidthPx / 2f + 1f
        val rect = RectF(inset, inset, sizePx - inset, sizePx - inset)

        val track = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            style = Paint.Style.STROKE
            strokeWidth = strokeWidthPx
            color = trackColor
        }
        canvas.drawArc(rect, 0f, 360f, false, track)

        val p = progress.coerceIn(0f, 1f)
        if (p > 0f) {
            val arc = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                style = Paint.Style.STROKE
                strokeWidth = strokeWidthPx
                color = progressColor
                strokeCap = Paint.Cap.ROUND
            }
            canvas.drawArc(rect, -90f, 360f * p, false, arc)
        }
        return bmp
    }

    /**
     * A donut chart: one arc per slice (colour + fraction of the whole, fractions must sum to
     * <= 1), drawn clockwise from 12 o'clock. Slices with a zero fraction are skipped.
     */
    fun donut(
        sizePx: Int,
        slices: List<Pair<Int, Float>>,
        strokeWidthPx: Float,
        gapDegrees: Float = 0f,
    ): Bitmap {
        val bmp = Bitmap.createBitmap(sizePx, sizePx, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(bmp)
        val inset = strokeWidthPx / 2f + 1f
        val rect = RectF(inset, inset, sizePx - inset, sizePx - inset)
        val paint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            style = Paint.Style.STROKE
            strokeWidth = strokeWidthPx
            strokeCap = Paint.Cap.BUTT
        }
        var start = -90f
        slices.forEach { (color, fraction) ->
            val sweep = 360f * fraction.coerceIn(0f, 1f)
            if (sweep > 0.5f) {
                paint.color = color
                // Small gaps between slices read as segments instead of one merged blob.
                val drawn = (sweep - gapDegrees).coerceAtLeast(0.5f)
                canvas.drawArc(rect, start + gapDegrees / 2f, drawn, false, paint)
            }
            start += sweep
        }
        return bmp
    }
}
