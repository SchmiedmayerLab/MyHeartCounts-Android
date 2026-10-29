//
// This source file is part of the My Heart Counts Android open-source project
//
// SPDX-FileCopyrightText: 2026 Stanford University and the project authors (see CONTRIBUTORS.md)
//
// SPDX-License-Identifier: MIT

package edu.stanford.myheartcounts.dashboard.ui.internal

import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.drawOutline
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.translate
import androidx.compose.ui.input.pointer.PointerEventPass
import androidx.compose.ui.input.pointer.PointerInputChange
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.semantics.CustomAccessibilityAction
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.customActions
import androidx.compose.ui.semantics.liveRegion
import androidx.compose.ui.semantics.stateDescription
import androidx.compose.ui.text.TextLayoutResult
import androidx.compose.ui.text.drawText
import androidx.compose.ui.unit.Dp
import edu.stanford.myheartcounts.dashboard.ui.chart.CartesianChart
import edu.stanford.myheartcounts.dashboard.ui.chart.Highlight
import kotlin.math.abs
import kotlin.math.max

/**
 * A point of a chart that can be highlighted.
 *
 * @param series the index of the point's series
 * @param point the point itself
 */
internal data class ChartCandidate(
    val series: Int,
    val point: CartesianChart.Point,
) {

    /**
     * The middle of the point's X interval, where it is drawn and highlighted.
     */
    val middle: Double
        get() = point.middle
}

/**
 * The plot of a chart as last drawn, read by its gestures to find the point under a touch.
 *
 * Not observed: drawing updates it without causing recomposition.
 */
internal class ChartPlotHolder {
    var plot: ChartPlot? = null
}

/**
 * The index of the candidate nearest to the horizontal position [x], measured from each
 * candidate's X interval; a candidate whose interval spans [x] is nearest. The first of equally near
 * candidates wins.
 */
internal fun List<ChartCandidate>.nearestTo(
    x: Float,
    plot: ChartPlot,
): Int? {
    return indices.minByOrNull { index ->
        val start = plot.x(this[index].point.x)
        val end = plot.x(this[index].point.end)
        when {
            x < start -> start - x
            x > end -> x - end
            else -> 0f
        }
    }
}

/**
 * The points of [series] that can be highlighted, those of series with a highlight format among
 * [visiblePoints], in X order.
 */
internal fun highlightCandidates(
    series: List<CartesianChart.Series>,
    visiblePoints: List<List<CartesianChart.Point>>,
): List<ChartCandidate> {
    return series
        .flatMapIndexed { index, item ->
            if (item.highlightFormat == null) {
                emptyList()
            } else {
                visiblePoints[index].map { point -> ChartCandidate(series = index, point = point) }
            }
        }
        .sortedBy { it.middle }
}

/**
 * The highlight of each series with a highlight format at [anchor]'s X coordinate, in series order, with
 * the series' color: [anchor] itself for its series, and the point spanning its X coordinate for the others.
 * Series without such a point are left out.
 */
internal fun highlightsAt(
    anchor: ChartCandidate,
    series: List<CartesianChart.Series>,
    visiblePoints: List<List<CartesianChart.Point>>,
    colors: List<Color>,
): List<Pair<Color, Highlight>> {
    return series.mapIndexedNotNull { index, item ->
        val format = item.highlightFormat
        val point = if (index == anchor.series) anchor.point else visiblePoints[index].firstOrNull { it.spans(anchor.middle) }
        if (format == null || point == null) null else colors[index] to format.label(point)
    }
}

/**
 * The index [delta] candidates on from [selected], kept within the [count] candidates; stepping
 * forward from no selection starts at the first, stepping back at the last.
 */
internal fun stepSelection(
    selected: Int?,
    delta: Int,
    count: Int,
): Int {
    val from = selected ?: if (delta > 0) -1 else count
    return (from + delta).coerceIn(0, count - 1)
}

/**
 * Makes a chart a single element for screen readers, described by [description] and, when given,
 * [state], which is announced as it changes. With [stepLabels], it offers actions stepping to the
 * next and the previous point, which call [onStep] with `1` and `-1`.
 */
internal fun Modifier.chartSemantics(
    description: String,
    state: String?,
    stepLabels: Pair<String, String>?,
    onStep: (Int) -> Unit,
): Modifier {
    return clearAndSetSemantics {
        contentDescription = description
        if (state != null) stateDescription = state
        if (stepLabels != null) {
            liveRegion = LiveRegionMode.Polite
            customActions = listOf(
                CustomAccessibilityAction(label = stepLabels.first) {
                    onStep(1)
                    true
                },
                CustomAccessibilityAction(label = stepLabels.second) {
                    onStep(-1)
                    true
                },
            )
        }
    }
}

/**
 * Highlights the point under a touch while the finger is down: [onSelect] is called with the
 * touch's horizontal position on touch down and while it is dragged sideways, and [onRelease] once
 * the finger lifts or a scrolling parent takes over. Only sideways drags are claimed, so vertical
 * drags still scroll the page. The gesture restarts whenever [key] changes.
 */
internal fun Modifier.chartHighlightGestures(
    key: Any,
    enabled: Boolean,
    onSelect: (Float) -> Unit,
    onRelease: () -> Unit,
): Modifier {
    if (!enabled) return this
    return pointerInput(key) {
        awaitEachGesture {
            val down = awaitFirstDown(requireUnconsumed = false)
            onSelect(down.position.x)
            var dragging = false
            var finished = false
            try {
                while (!finished) {
                    val change = awaitPointerEvent().changes.firstOrNull { it.id == down.id }
                    if (change == null || change.endsHighlight(dragging)) break
                    val moved = change.position - down.position
                    if (!dragging && abs(moved.x) > viewConfiguration.touchSlop && abs(moved.x) > abs(moved.y)) {
                        dragging = true
                    }
                    if (dragging) {
                        change.consume()
                        onSelect(change.position.x)
                    } else {
                        val finalChange = awaitPointerEvent(PointerEventPass.Final).changes.firstOrNull { it.id == down.id }
                        finished = finalChange == null || finalChange.isConsumed
                    }
                }
            } finally {
                onRelease()
            }
        }
    }
}

/**
 * Whether this change ends a highlight: the finger lifted, or, before a sideways drag started, a
 * scrolling parent took the gesture over.
 */
private fun PointerInputChange.endsHighlight(dragging: Boolean): Boolean = !pressed || (!dragging && isConsumed)

/**
 * Draws the line marking the highlighted X coordinate across the plot, kept within the plot;
 * nothing when there is no [x].
 */
internal fun DrawScope.drawHighlightLine(
    plot: ChartPlot,
    x: Double?,
    color: Color,
    alpha: Float,
    strokes: ChartStrokes,
) {
    if (x == null) return
    val position = plot.x(x).coerceIn(plot.area.left, plot.area.right)
    drawLine(
        color = color.copy(alpha = color.alpha * alpha),
        start = Offset(x = position, y = plot.area.top),
        end = Offset(x = position, y = plot.area.bottom),
        strokeWidth = strokes.line,
    )
}

/**
 * Draws the box describing the highlighted points at the top of the plot, centered on [x] and
 * kept within the chart: an optional heading above one value per series, each led by a dot in its
 * series' color. Nothing is drawn when there is no [x] or there are no [values].
 */
@Suppress("LongParameterList")
internal fun DrawScope.drawHighlightBox(
    plot: ChartPlot,
    x: Double?,
    title: TextLayoutResult?,
    values: List<Pair<Color, TextLayoutResult>>,
    shape: Shape,
    background: Color,
    padding: Float,
    dotRadius: Dp,
) {
    if (x == null || values.isEmpty()) return
    val dot = dotRadius.toPx()
    val valuesWidth = values.maxOfOrNull { (_, label) -> dot * 2 + padding + label.size.width } ?: 0f
    val width = max(title?.size?.width?.toFloat() ?: 0f, valuesWidth) + padding * 2
    val height = (title?.size?.height ?: 0) + values.sumOf { it.second.size.height } + padding * 2
    val left = (plot.x(x) - width / 2f).coerceIn(0f, max(size.width - width, 0f))
    translate(left = left, top = plot.area.top) {
        drawOutline(
            outline = shape.createOutline(Size(width = width, height = height), layoutDirection, this),
            color = background,
        )
        var top = padding
        if (title != null) {
            drawText(textLayoutResult = title, topLeft = Offset(x = padding, y = top))
            top += title.size.height
        }
        values.forEach { (color, label) ->
            drawCircle(
                color = color,
                radius = dot,
                center = Offset(x = padding + dot, y = top + label.size.height / 2f),
            )
            drawText(textLayoutResult = label, topLeft = Offset(x = padding + dot * 2 + padding, y = top))
            top += label.size.height
        }
    }
}
