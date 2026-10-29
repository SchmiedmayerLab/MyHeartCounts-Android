//
// This source file is part of the My Heart Counts Android open-source project
//
// SPDX-FileCopyrightText: 2026 Stanford University and the project authors (see CONTRIBUTORS.md)
//
// SPDX-License-Identifier: MIT

package edu.stanford.myheartcounts.dashboard.ui.chart

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.graphics.drawscope.clipRect
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalInspectionMode
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.tooling.preview.PreviewParameter
import androidx.compose.ui.tooling.preview.PreviewParameterProvider
import androidx.compose.ui.unit.dp
import edu.stanford.myheartcounts.dashboard.ui.DashboardColorPalette
import edu.stanford.myheartcounts.dashboard.ui.internal.ChartPlot
import edu.stanford.myheartcounts.dashboard.ui.internal.ChartPlotHolder
import edu.stanford.myheartcounts.dashboard.ui.internal.ChartReveal
import edu.stanford.myheartcounts.dashboard.ui.internal.ChartStrokes
import edu.stanford.myheartcounts.dashboard.ui.internal.ValueScale
import edu.stanford.myheartcounts.dashboard.ui.internal.chartHighlightGestures
import edu.stanford.myheartcounts.dashboard.ui.internal.chartSemantics
import edu.stanford.myheartcounts.dashboard.ui.internal.drawCentered
import edu.stanford.myheartcounts.dashboard.ui.internal.drawHighlightBox
import edu.stanford.myheartcounts.dashboard.ui.internal.drawHighlightLine
import edu.stanford.myheartcounts.dashboard.ui.internal.drawPlotBorder
import edu.stanford.myheartcounts.dashboard.ui.internal.drawSeries
import edu.stanford.myheartcounts.dashboard.ui.internal.drawXAxis
import edu.stanford.myheartcounts.dashboard.ui.internal.drawYAxis
import edu.stanford.myheartcounts.dashboard.ui.internal.highlightCandidates
import edu.stanford.myheartcounts.dashboard.ui.internal.highlightsAt
import edu.stanford.myheartcounts.dashboard.ui.internal.nearestTo
import edu.stanford.myheartcounts.dashboard.ui.internal.stepSelection
import org.grovealliance.ui.ComposableContent
import org.grovealliance.ui.StringResource
import org.grovealliance.ui.theme.ColorToken
import org.grovealliance.ui.theme.GroveTheme
import org.grovealliance.ui.theme.Spacings
import org.grovealliance.ui.theme.ThemePreviews
import org.grovealliance.ui.theme.resolve
import java.util.Locale
import kotlin.math.max

/**
 * Numeric X/Y series drawn as lines, bars or points, with independently configured axes.
 *
 * Each axis uses its supplied range and formatter. Coordinates have no implied unit or domain.
 * Bars extend from zero to Y; an optional X span determines their width. Marks outside the axis
 * ranges are clipped. The horizontal axis remains left to right in all layout directions.
 *
 * The series are drawn in as [CartesianChartConfig.Animation] describes when the chart first
 * appears and whenever its horizontal range changes; other changes are drawn at once. Previews and
 * screenshots are drawn complete.
 *
 * @param series the series, drawn in order, later series on top
 * @param xAxis the horizontal range and tick labels
 * @param yAxis the vertical range and tick labels
 * @param emptyText the text shown when no point overlaps the horizontal range
 * @param description a summary read by screen readers
 * @param accessibilityLabels the screen reader action labels for stepping through points
 * @param config the theme-aware appearance of the chart
 */
@Immutable
data class CartesianChart(
    val series: List<Series>,
    val xAxis: Axis,
    val yAxis: Axis,
    val emptyText: StringResource,
    val description: StringResource,
    val accessibilityLabels: AccessibilityLabels,
    val config: CartesianChartConfig = CartesianChartConfig(),
) : ComposableContent {

    @Composable
    override fun Content(modifier: Modifier) {
        val context = LocalContext.current
        val textMeasurer = rememberTextMeasurer(cacheSize = TEXT_LAYOUT_CACHE_SIZE)
        val labelStyle = config.typography.axis.resolve().copy(color = config.axisTextColor.resolve())
        val emptyStyle = config.typography.empty.resolve().copy(color = config.emptyTextColor.resolve())
        val gridColor = config.gridColor.resolve()
        val backgroundColor = config.backgroundColor.resolve()
        val plotBackgroundColor = config.plotBackgroundColor.resolve()
        val seriesColors = series.map { it.color.resolve() }
        val strokes = with(LocalDensity.current) {
            ChartStrokes(
                grid = config.drawing.gridWidth.resolve().toPx(),
                line = config.drawing.lineWidth.resolve().toPx(),
                gap = config.drawing.labelSpacing.resolve().toPx(),
            )
        }
        val highlightPadding = with(LocalDensity.current) { config.highlight.padding.resolve().toPx() }
        val visiblePoints = remember(series, xAxis.range) {
            series.map { it.points.filter { point -> point.overlaps(xAxis.range) } }
        }
        val connectedPoints = remember(series, xAxis.range, visiblePoints) {
            series.mapIndexed { index, item ->
                connectedPoints(
                    series = item,
                    visible = visiblePoints[index],
                    range = xAxis.range,
                )
            }
        }
        val hasData = visiblePoints.any { it.isNotEmpty() }
        val axisLabels = rememberAxisLabels(labeled = hasData)
        val yTicks = axisLabels.yTicks
        val yLabels = axisLabels.y
        val xLabels = axisLabels.x
        val candidates = remember(series, visiblePoints) {
            highlightCandidates(series = series, visiblePoints = visiblePoints)
        }
        val reveal = rememberReveal()
        val interactive = candidates.isNotEmpty() && reveal.complete
        val selectionKey = Triple(series, xAxis.range, yAxis.range)
        var selected by remember(selectionKey) { mutableStateOf<Int?>(null) }
        val plotHolder = remember { ChartPlotHolder() }
        val anchor = selected?.let { candidates.getOrNull(it) }
        val highlights = anchor?.let {
            highlightsAt(
                anchor = anchor,
                series = series,
                visiblePoints = visiblePoints,
                colors = seriesColors,
            )
        }.orEmpty()
        val highlightTitle = highlights.firstOrNull()?.second?.title?.get(context)
        val highlightValues = highlights.map { (color, highlight) -> color to highlight.value.get(context) }
        val highlightTitleStyle = config.typography.highlightTitle.resolve().copy(color = config.highlight.titleColor.resolve())
        val highlightValueStyle = config.typography.highlightValue.resolve().copy(
            color = config.highlight.valueColor.resolve(),
            fontWeight = config.typography.highlightValueWeight,
        )
        val highlightLineColor = config.highlight.lineColor.resolve()
        val highlightBackground = config.highlight.backgroundColor.resolve()
        val emptyLabel = emptyText.text()
        val highlightText = (listOfNotNull(highlightTitle) + highlightValues.map { it.second }).joinToString(separator = ", ")
        val stepLabels = accessibilityLabels.nextPoint.text() to accessibilityLabels.previousPoint.text()
        Canvas(
            modifier = modifier
                .chartSemantics(
                    description = description.text(),
                    state = if (!hasData) emptyLabel else highlightText.takeIf { highlights.isNotEmpty() },
                    stepLabels = stepLabels.takeIf { interactive },
                    onStep = { delta -> selected = stepSelection(selected = selected, delta = delta, count = candidates.size) },
                )
                .chartHighlightGestures(
                    key = selectionKey,
                    enabled = interactive,
                    onSelect = { x -> plotHolder.plot?.let { plot -> selected = candidates.nearestTo(x = x, plot = plot) } },
                    onRelease = { selected = null },
                ),
        ) {
            drawRect(color = backgroundColor)
            val revealed = reveal.current()
            val yLayouts = yLabels.map { (value, text) -> value to textMeasurer.measure(text = text, style = labelStyle) }
            val xLayouts = xLabels.map { (value, text) -> value to textMeasurer.measure(text = text, style = labelStyle) }
            val labelHeight = maxOf(
                textMeasurer.measure(text = "0", style = labelStyle).size.height,
                yLayouts.maxOfOrNull { it.second.size.height } ?: 0,
                xLayouts.maxOfOrNull { it.second.size.height } ?: 0,
            ).toFloat()
            val yAxisWidth = (yLayouts.maxOfOrNull { it.second.size.width } ?: 0) + strokes.gap
            val plotTop = labelHeight / 2f
            val plot = ChartPlot(
                area = Rect(
                    left = 0f,
                    top = plotTop,
                    right = max(size.width - yAxisWidth, 0f),
                    bottom = max(size.height - labelHeight - strokes.gap * 2, plotTop),
                ),
                xRange = xAxis.range,
                yRange = yAxis.range,
            )
            plotHolder.plot = plot
            drawRect(
                color = plotBackgroundColor,
                topLeft = plot.area.topLeft,
                size = plot.area.size,
            )
            drawYAxis(
                plot = plot,
                labels = if (!hasData) yTicks.map { it.at to null } else yLayouts,
                color = gridColor,
                strokes = strokes,
                alpha = revealed.axes,
            )
            drawXAxis(
                plot = plot,
                ticks = xLayouts,
                dashLength = config.drawing.xGridDashLength,
                color = gridColor,
                strokes = strokes,
                alpha = revealed.axes,
            )
            drawHighlightLine(
                plot = plot,
                x = anchor?.middle,
                color = highlightLineColor,
                alpha = config.highlight.lineAlpha,
                strokes = strokes,
            )
            clipRect(
                left = plot.area.left,
                top = plot.area.top,
                right = plot.area.right,
                bottom = plot.area.bottom,
            ) {
                series.forEachIndexed { index, item ->
                    drawSeries(
                        plot = plot,
                        style = item.style,
                        points = visiblePoints[index],
                        connected = connectedPoints[index],
                        color = seriesColors[index],
                        strokes = strokes,
                        appearance = config.drawing,
                        reveal = revealed,
                    )
                }
            }
            drawPlotBorder(
                plot = plot,
                shown = config.showBorder,
                color = gridColor,
                strokes = strokes,
            )
            if (!hasData) {
                drawCentered(
                    plot = plot,
                    text = textMeasurer.measure(text = emptyLabel, style = emptyStyle),
                )
            }
            drawHighlightBox(
                plot = plot,
                x = anchor?.middle,
                title = highlightTitle?.let { textMeasurer.measure(text = it, style = highlightTitleStyle) },
                values = highlightValues.map { (color, text) ->
                    color to textMeasurer.measure(text = text, style = highlightValueStyle)
                },
                shape = config.highlight.shape,
                background = highlightBackground,
                padding = highlightPadding,
                dotRadius = config.highlight.dotRadius,
            )
        }
    }

    /**
     * The marked values of both axes within their ranges with their labels, the vertical axis'
     * labels only when [labeled].
     */
    @Composable
    private fun rememberAxisLabels(labeled: Boolean): AxisLabels {
        val context = LocalContext.current
        val yTicks = remember(yAxis) {
            yAxis.format.ticks(yAxis.range).filter { it.at in yAxis.range.start..yAxis.range.end }.sortedBy { it.at }
        }
        val yLabels = remember(yTicks, labeled, context) {
            if (labeled) yTicks.map { it.at to it.label.get(context) } else emptyList()
        }
        val xLabels = remember(xAxis, context) {
            xAxis.format.ticks(xAxis.range)
                .filter { it.at in xAxis.range.start..xAxis.range.end }
                .sortedBy { it.at }
                .map { it.at to it.label.get(context) }
        }
        return AxisLabels(
            yTicks = yTicks,
            y = yLabels,
            x = xLabels,
        )
    }

    /**
     * The progress of the animation drawing the series in, restarted whenever the horizontal range
     * changes; complete at once when animations are disabled or in previews.
     */
    @Composable
    private fun rememberReveal(): RevealState {
        val animation = config.animation
        val animates = animation.enabled && !LocalInspectionMode.current
        val progress = remember { Animatable(initialValue = 1f) }
        var animatedRange by remember { mutableStateOf<Range?>(null) }
        if (animates) {
            LaunchedEffect(xAxis.range, animation) {
                progress.snapTo(targetValue = 0f)
                animatedRange = xAxis.range
                progress.animateTo(
                    targetValue = 1f,
                    animationSpec = tween(durationMillis = animation.durationMillis, easing = FastOutSlowInEasing),
                )
            }
        }
        val pending = animates && animatedRange != xAxis.range
        val finished by remember { derivedStateOf { progress.value >= 1f } }
        return RevealState(
            complete = !animates || (!pending && finished),
            current = {
                when {
                    !animates -> ChartReveal.Complete
                    pending -> ChartReveal(progress = 0f, stagger = animation.stagger)
                    else -> ChartReveal(progress = progress.value, stagger = animation.stagger)
                }
            },
        )
    }

    /**
     * The points of [series] its line runs through: its [visible] points and, for lines, the
     * neighbours on either side of [range], so the line runs on to the edges of the plot.
     */
    private fun connectedPoints(
        series: Series,
        visible: List<Point>,
        range: Range,
    ): List<Point> {
        if (series.style != Style.Line) return visible
        val points = series.points
        val from = points.indexOfLast { it.middle < range.start }.coerceAtLeast(0)
        val to = points.indexOfFirst { it.middle > range.end }.takeIf { it >= 0 } ?: points.lastIndex
        return if (from > to) visible else points.subList(from, to + 1)
    }

    /**
     * A series of a [CartesianChart].
     *
     * @param style how the points are drawn
     * @param color the color the points are drawn in
     * @param points the points, in ascending X order
     * @param highlightFormat the text shown when one of the points is highlighted; the series is not
     * highlighted when omitted
     */
    data class Series(
        val style: Style,
        val color: ColorToken,
        val points: List<Point>,
        val highlightFormat: HighlightFormat?,
    ) {
        init {
            require(points.zipWithNext().all { (previous, next) -> previous.x <= next.x }) {
                "points must be in ascending X order"
            }
        }
    }

    /**
     * One numeric X/Y pair, optionally covering a horizontal interval.
     *
     * @param x the horizontal coordinate, or the beginning of its span
     * @param y the vertical coordinate
     * @param xEnd the exclusive end of a span; null draws a single coordinate
     */
    data class Point(
        val x: Double,
        val y: Double,
        val xEnd: Double?,
    ) {
        init {
            require(x.isFinite() && y.isFinite()) { "coordinates must be finite" }
            require(xEnd == null || (xEnd.isFinite() && xEnd >= x)) { "xEnd must be finite and at least x" }
        }

        /**
         * The X coordinate at the end of the point's span, or X for a single coordinate.
         */
        internal val end: Double get() = xEnd ?: x

        /**
         * The X coordinate where the point is drawn.
         */
        internal val middle: Double get() = x / 2 + end / 2

        /**
         * Whether the point overlaps the closed axis range: a single coordinate within it, or a span
         * sharing more than a bound with it.
         */
        internal fun overlaps(range: Range): Boolean =
            if (x == end) x in range.start..range.end else x < range.end && end > range.start

        /**
         * Whether a coordinate lies in the point's half-open span, or equals its single X value.
         */
        internal fun spans(coordinate: Double): Boolean = coordinate == x || (coordinate >= x && coordinate < end)
    }

    /**
     * How the points of a series are drawn.
     */
    enum class Style {
        /**
         * Points joined by straight lines, with a dot on each point.
         */
        Line,

        /**
         * A bar from zero to each point, spanning its X interval.
         *
         * Bars always grow from zero, so the vertical range should include zero, for example through
         * [Range.fitting] with `includeZero = true`; otherwise the part of each bar outside the range
         * is clipped and its base is not shown.
         */
        Bar,

        /**
         * A dot on each point, not joined.
         */
        Point,
    }

    /**
     * The same axis configuration can be used horizontally or vertically.
     *
     * @param range the numeric bounds of the axis
     * @param format the tick positions and their labels
     */
    data class Axis(
        val range: Range,
        val format: AxisFormat,
    )

    /**
     * A closed, finite interval of numeric coordinates.
     *
     * @param start the lower bound
     * @param end the upper bound; must exceed start by a finite amount
     */
    data class Range(
        val start: Double,
        val end: Double,
    ) {
        init {
            require(start.isFinite() && end.isFinite() && start < end && (end - start).isFinite()) {
                "range must have finite, increasing bounds and a finite extent"
            }
        }

        companion object {
            /**
             * Clean bounds covering [values], optionally including zero; empty values use zero to one.
             *
             * Use `includeZero = true` for ranges of [Style.Bar] series, whose bars grow from zero.
             */
            fun fitting(values: List<Double>, includeZero: Boolean): Range {
                require(values.all { it.isFinite() }) { "values must be finite" }
                if (values.isEmpty()) return Range(start = 0.0, end = 1.0)
                val scale = ValueScale.of(
                    lowest = values.min(),
                    highest = values.max(),
                    includeZero = includeZero,
                )
                return Range(start = scale.min, end = scale.max)
            }
        }
    }

    /**
     * The labels of the actions screen readers use to step through the points of a chart.
     *
     * @param nextPoint the label of the action highlighting the next point
     * @param previousPoint the label of the action highlighting the previous point
     */
    data class AccessibilityLabels(
        val nextPoint: StringResource,
        val previousPoint: StringResource,
    )

    /**
     * The marks and labels of a chart's axes.
     *
     * @param yTicks the marked values of the vertical axis, in order
     * @param y the vertical axis' labels at their values
     * @param x the horizontal axis' labels at their values
     */
    private data class AxisLabels(
        val yTicks: List<AxisFormat.Tick>,
        val y: List<Pair<Double, String>>,
        val x: List<Pair<Double, String>>,
    )

    /**
     * The animation drawing the series in, as seen by one composition.
     *
     * @param complete whether the series are drawn complete, so points can be highlighted
     * @param current the progress to draw with, read while drawing so the animation only redraws
     */
    private data class RevealState(
        val complete: Boolean,
        val current: () -> ChartReveal,
    )

    private companion object {
        /**
         * The number of text layouts kept between frames, enough for every label of a chart.
         */
        const val TEXT_LAYOUT_CACHE_SIZE = 64
    }
}

private class CartesianChartPreviewProvider : PreviewParameterProvider<CartesianChart> {
    private val base = cartesianChart(
        emptyText = StringResource("No Data"),
        description = StringResource("Elevation over distance"),
        accessibilityLabels = CartesianChart.AccessibilityLabels(
            nextPoint = StringResource("Next point"),
            previousPoint = StringResource("Previous point"),
        ),
    ) {
        xAxis(
            range = CartesianChart.Range(start = 0.0, end = 5.0),
            format = NumberAxisFormat(locale = Locale.US),
        )
        yAxis(
            range = CartesianChart.Range(start = 100.0, end = 160.0),
            format = NumberAxisFormat(locale = Locale.US),
        )
        line(color = DashboardColorPalette.blue, highlightFormat = PreviewHighlightFormat) {
            point(x = 0.0, y = 120.0)
            point(x = 2.5, y = 145.0)
            point(x = 5.0, y = 132.0)
        }
    }

    override val values: Sequence<CartesianChart> = sequenceOf(
        base,
        base.copy(series = base.series.map { it.copy(style = CartesianChart.Style.Point) }),
        base.copy(series = emptyList()),
        base.copy(
            config = base.config.copy(
                backgroundColor = ColorToken.SurfaceContainer,
                gridColor = ColorToken.Primary,
                axisTextColor = ColorToken.Primary,
            ),
        ),
    )
}

@ThemePreviews
@Composable
private fun Preview(@PreviewParameter(CartesianChartPreviewProvider::class) content: CartesianChart) {
    GroveTheme {
        content.Content(
            modifier = Modifier.padding(Spacings.medium).fillMaxWidth().height(height = 180.dp),
        )
    }
}

/**
 * Example coordinate labels for the interactive preview.
 */
private object PreviewHighlightFormat : HighlightFormat {
    override fun label(point: CartesianChart.Point): Highlight = Highlight(
        value = StringResource("${point.y} m"),
        title = StringResource("${point.x} km"),
    )
}
