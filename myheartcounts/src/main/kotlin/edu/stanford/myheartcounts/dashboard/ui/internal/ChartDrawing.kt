//
// This source file is part of the My Heart Counts Android open-source project
//
// SPDX-FileCopyrightText: 2026 Stanford University and the project authors (see CONTRIBUTORS.md)
//
// SPDX-License-Identifier: MIT

package edu.stanford.myheartcounts.dashboard.ui.internal

import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.geometry.RoundRect
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.clipRect
import androidx.compose.ui.text.TextLayoutResult
import androidx.compose.ui.text.drawText
import androidx.compose.ui.unit.Dp
import edu.stanford.myheartcounts.dashboard.ui.chart.CartesianChart
import edu.stanford.myheartcounts.dashboard.ui.chart.CartesianChartConfig
import kotlin.math.abs
import kotlin.math.max
import kotlin.math.min

/**
 * The widths and lengths a chart is drawn with, in pixels.
 *
 * @param grid the width of grid lines
 * @param line the width of the lines joining points
 * @param gap the space between the plot and its labels, and the length of a tick mark
 */
internal data class ChartStrokes(
    val grid: Float,
    val line: Float,
    val gap: Float,
)

/**
 * Draws the vertical axis: a solid grid line across the plot at each marked value, with its label
 * beyond the end of the plot, vertically centered on the line, both at [alpha].
 */
internal fun DrawScope.drawYAxis(
    plot: ChartPlot,
    labels: List<Pair<Double, TextLayoutResult?>>,
    color: Color,
    strokes: ChartStrokes,
    alpha: Float,
) {
    var previousLabelTop = Float.POSITIVE_INFINITY
    labels.forEach { (value, label) ->
        val position = plot.y(value)
        drawLine(
            color = color,
            start = Offset(x = plot.area.left, y = position),
            end = Offset(x = plot.area.right, y = position),
            strokeWidth = strokes.grid,
            alpha = alpha,
        )
        if (label != null && position + label.size.height / 2f + strokes.gap <= previousLabelTop) {
            previousLabelTop = position - label.size.height / 2f
            drawText(
                textLayoutResult = label,
                topLeft = Offset(
                    x = plot.area.right + strokes.gap,
                    y = position - label.size.height / 2f,
                ),
                alpha = alpha,
            )
        }
    }
}

/**
 * Draws the horizontal axis: the baseline along the bottom of the plot, and at each mark a dashed grid
 * line, a tick mark and its label, starting just after the mark, all but the baseline at [alpha]. A
 * label overlapping the previous one or running past the right edge is left out, while its mark is
 * drawn.
 */
@Suppress("LongParameterList")
internal fun DrawScope.drawXAxis(
    plot: ChartPlot,
    ticks: List<Pair<Double, TextLayoutResult>>,
    dashLength: Dp?,
    color: Color,
    strokes: ChartStrokes,
    alpha: Float,
) {
    val dash = dashLength?.let { PathEffect.dashPathEffect(floatArrayOf(it.toPx(), it.toPx())) }
    var labelsEnd = Float.NEGATIVE_INFINITY
    ticks.forEach { (value, label) ->
        val position = plot.x(value)
        drawLine(
            color = color,
            start = Offset(x = position, y = plot.area.top),
            end = Offset(x = position, y = plot.area.bottom),
            strokeWidth = strokes.grid,
            pathEffect = dash,
            alpha = alpha,
        )
        drawLine(
            color = color,
            start = Offset(x = position, y = plot.area.bottom),
            end = Offset(x = position, y = plot.area.bottom + strokes.gap),
            strokeWidth = strokes.grid,
            alpha = alpha,
        )
        val labelStart = position + strokes.gap
        val labelEnd = labelStart + label.size.width
        if (labelStart >= labelsEnd + strokes.gap && labelEnd <= size.width) {
            drawText(
                textLayoutResult = label,
                topLeft = Offset(x = labelStart, y = plot.area.bottom + strokes.gap),
                alpha = alpha,
            )
            labelsEnd = labelEnd
        }
    }
    drawLine(
        color = color,
        start = Offset(x = plot.area.left, y = plot.area.bottom),
        end = Offset(x = plot.area.right, y = plot.area.bottom),
        strokeWidth = strokes.grid,
    )
}

/**
 * Frames the plot with the same color and stroke width as its grid, when [shown].
 */
internal fun DrawScope.drawPlotBorder(
    plot: ChartPlot,
    shown: Boolean,
    color: Color,
    strokes: ChartStrokes,
) {
    if (!shown) return
    drawRect(
        color = color,
        topLeft = plot.area.topLeft,
        size = plot.area.size,
        style = Stroke(width = strokes.grid),
    )
}

/**
 * Draws the [points] of a series in its style, as far as [reveal] has drawn them in: bars from zero
 * spanning each point's X interval, rounded at their value's end, a line through the middle of each
 * of the [connected] points' X intervals with a dot on each of the [points], or dots alone. A line's
 * dots are at least as wide as the line, so a single point stays visible.
 */
@Suppress("LongParameterList")
internal fun DrawScope.drawSeries(
    plot: ChartPlot,
    style: CartesianChart.Style,
    points: List<CartesianChart.Point>,
    connected: List<CartesianChart.Point>,
    appearance: CartesianChartConfig.Drawing,
    color: Color,
    strokes: ChartStrokes,
    reveal: ChartReveal,
) {
    when (style) {
        CartesianChart.Style.Bar -> points.forEach { point ->
            drawBar(
                plot = plot,
                point = point,
                color = color,
                appearance = appearance,
                growth = reveal.growth(position = plot.x(point.middle), plot = plot),
            )
        }
        CartesianChart.Style.Line -> clipRect(right = reveal.lineEnd(plot = plot)) {
            val path = Path()
            connected.forEachIndexed { index, point ->
                val position = plot.position(point)
                if (index == 0) path.moveTo(position.x, position.y) else path.lineTo(position.x, position.y)
            }
            drawPath(
                path = path,
                color = color,
                style = Stroke(
                    width = strokes.line,
                    cap = StrokeCap.Round,
                    join = StrokeJoin.Round,
                ),
            )
            drawDots(
                plot = plot,
                points = points,
                color = color,
                radius = max(appearance.pointRadius.toPx(), strokes.line / 2f),
                reveal = reveal,
            )
        }
        CartesianChart.Style.Point -> drawDots(
            plot = plot,
            points = points,
            color = color,
            radius = appearance.pointRadius.toPx(),
            reveal = reveal,
        )
    }
}

/**
 * Draws [text] centered in the plot.
 */
internal fun DrawScope.drawCentered(
    plot: ChartPlot,
    text: TextLayoutResult,
) {
    drawText(
        textLayoutResult = text,
        topLeft = Offset(
            x = plot.area.center.x - text.size.width / 2f,
            y = plot.area.center.y - text.size.height / 2f,
        ),
    )
}

private fun DrawScope.drawDots(
    plot: ChartPlot,
    points: List<CartesianChart.Point>,
    color: Color,
    radius: Float,
    reveal: ChartReveal,
) {
    points.forEach { point ->
        val center = plot.position(point)
        val grown = radius * reveal.growth(position = center.x, plot = plot)
        if (grown > 0f) {
            drawCircle(
                color = color,
                radius = grown,
                center = center,
            )
        }
    }
}

private fun DrawScope.drawBar(
    plot: ChartPlot,
    point: CartesianChart.Point,
    color: Color,
    appearance: CartesianChartConfig.Drawing,
    growth: Float,
) {
    val spanStart = plot.x(point.x)
    val spanEnd = plot.x(point.end)
    val width = max((spanEnd - spanStart) * appearance.barWidthFraction, appearance.minimumBarWidth.toPx())
    val zero = plot.y(0.0)
    val top = zero + (plot.y(point.y) - zero) * growth
    val height = abs(zero - top)
    if (height == 0f) return
    val corner = CornerRadius(min(width, height) / 2f)
    val above = point.y >= 0
    val bar = RoundRect(
        rect = Rect(
            offset = Offset(x = (spanStart + spanEnd - width) / 2f, y = min(zero, top)),
            size = Size(width = width, height = height),
        ),
        topLeft = if (above) corner else CornerRadius.Zero,
        topRight = if (above) corner else CornerRadius.Zero,
        bottomRight = if (above) CornerRadius.Zero else corner,
        bottomLeft = if (above) CornerRadius.Zero else corner,
    )
    drawPath(
        path = Path().apply { addRoundRect(bar) },
        color = color,
    )
}

private fun ChartPlot.position(point: CartesianChart.Point): Offset {
    val middle = point.middle
    return Offset(x = x(middle), y = y(point.y))
}
