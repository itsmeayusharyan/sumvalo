package com.acuranza.app.designsystem.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.clipRect
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.drawText
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.acuranza.app.designsystem.theme.SumvaloTheme

/**
 * The net worth trend line: a thin, un-smoothed polyline with no grid, fill or axes — just the
 * line on the card. Press or drag anywhere to inspect: a hairline crosshair marks the point, the
 * part of the line after it dims to grey, and a compact date pill rides the crosshair. Selection
 * is sticky; tapping the selected point returns to the live edge.
 *
 * @param initialSelection point highlighted on first composition (previews and tests).
 * @param onSelectionChanged reports the inspected point, or null when back at the live edge.
 */
@Composable
fun TrendChart(
    points: List<ChartPoint>,
    modifier: Modifier = Modifier,
    height: Dp = 160.dp,
    hidden: Boolean = false,
    initialSelection: Int? = null,
    onSelectionChanged: ((Int?) -> Unit)? = null,
) {
    val colors = SumvaloTheme.colors
    val progress by rememberEntrance(900)
    var selected by remember(points) { mutableStateOf(initialSelection) }
    val lineColor = colors.ink
    val restColor = colors.tertiaryText.copy(alpha = 0.55f)
    val crosshair = colors.tertiaryText
    val pillBg = colors.ink
    val pillFg = colors.onInk
    val labelStyle = TextStyle(fontSize = 10.sp, fontWeight = FontWeight.SemiBold)
    val measurer = rememberTextMeasurer()
    val description = remember(points) {
        "Trend chart from ${points.firstOrNull()?.label ?: ""} to ${points.lastOrNull()?.label ?: ""}"
    }

    Canvas(
        modifier = modifier
            .fillMaxWidth()
            .height(height)
            .semantics { contentDescription = description }
            .pointerInput(points) {
                fun pick(x: Float): Int? {
                    if (points.isEmpty()) return null
                    if (points.size == 1) return 0
                    val step = size.width.toFloat() / (points.size - 1)
                    return ((x / step) + 0.5f).toInt().coerceIn(0, points.lastIndex)
                }
                detectTapGestures(
                    onPress = { tap ->
                        val hit = pick(tap.x)
                        selected = if (hit != null && hit == selected) null else hit
                        onSelectionChanged?.invoke(selected)
                        tryAwaitRelease()
                    },
                )
            }
            .pointerInput(points) {
                detectDragGestures(
                    onDragStart = { start ->
                        selected = if (points.isEmpty()) null
                        else ((start.x / (size.width.toFloat() / (points.size - 1).coerceAtLeast(1))) + 0.5f)
                            .toInt().coerceIn(0, points.lastIndex)
                        onSelectionChanged?.invoke(selected)
                    },
                    onDragEnd = {},
                    onDragCancel = {},
                ) { change, _ ->
                    change.consume()
                    if (points.isEmpty()) return@detectDragGestures
                    val step = size.width.toFloat() / (points.size - 1)
                    val hit = ((change.position.x / step) + 0.5f).toInt().coerceIn(0, points.lastIndex)
                    if (hit != selected) {
                        selected = hit
                        onSelectionChanged?.invoke(selected)
                    }
                }
            },
    ) {
        if (points.size < 2) return@Canvas
        val values = points.map { it.value.toFloat() }
        val lo = values.min()
        val hi = values.max()
        val span = (hi - lo).takeIf { it > 0f } ?: 1f
        val vPad = 14.dp.toPx()
        val h = size.height - vPad * 2
        val step = size.width / (points.size - 1)
        val xy = values.mapIndexed { i, v -> Offset(i * step, vPad + h - (v - lo) / span * h) }
        val line = Path().apply { xy.forEachIndexed { i, p -> if (i == 0) moveTo(p.x, p.y) else lineTo(p.x, p.y) } }
        val stroke = Stroke(1.5.dp.toPx(), cap = StrokeCap.Round, join = StrokeJoin.Round)

        // The whole line, dimmed; the inspected stretch is re-drawn at full contrast on top.
        clipRect(right = size.width * progress) {
            drawPath(line, restColor, style = stroke)
        }
        val sel = selected?.coerceIn(0, points.lastIndex)
        if (sel != null) {
            val px = xy[sel].x
            clipRect(right = minOf(px, size.width * progress)) {
                drawPath(line, lineColor, style = stroke)
            }
            drawLine(crosshair, Offset(px, 0f), Offset(px, size.height), strokeWidth = 1.dp.toPx())
            drawCircle(lineColor, 3.dp.toPx(), xy[sel])

            // Compact date pill riding the crosshair, clamped to the edges.
            val layout = measurer.measure(points[sel].label, style = labelStyle.copy(color = pillFg))
            val padH = 10.dp.toPx()
            val pillW = layout.size.width + padH * 2
            val pillH = layout.size.height + 8.dp.toPx()
            val margin = 6.dp.toPx()
            val pillX = (px - pillW / 2f).coerceIn(margin, size.width - pillW - margin)
            drawRoundRect(pillBg, Offset(pillX, 0f), Size(pillW, pillH), CornerRadius(pillH / 2f))
            drawText(layout, topLeft = Offset(pillX + padH, (pillH - layout.size.height) / 2f))
        } else if (progress >= 1f) {
            drawCircle(lineColor, 2.5.dp.toPx(), xy.last())
        }
    }
}
