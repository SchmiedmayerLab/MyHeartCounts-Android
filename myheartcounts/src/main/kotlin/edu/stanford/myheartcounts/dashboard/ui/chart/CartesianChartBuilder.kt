//
// This source file is part of the My Heart Counts Android open-source project
//
// SPDX-FileCopyrightText: 2026 Stanford University and the project authors (see CONTRIBUTORS.md)
//
// SPDX-License-Identifier: MIT

package edu.stanford.myheartcounts.dashboard.ui.chart

import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import org.grovealliance.ui.StringResource
import org.grovealliance.ui.theme.ColorToken
import org.grovealliance.ui.theme.SizeToken
import org.grovealliance.ui.theme.SpacingToken
import org.grovealliance.ui.theme.TextStyleToken
import java.util.Locale

/**
 * Keeps nested chart and series configuration in their own DSL scopes.
 */
@DslMarker
annotation class CartesianChartDsl

/**
 * Builds an immutable chart outside composition, using the same model as direct construction.
 *
 * Both axes start at zero to one with localized numeric labels. An empty series list is valid.
 *
 * @param emptyText the empty-state text
 * @param description the screen reader summary
 * @param accessibilityLabels the screen reader actions for stepping through points
 * @param block the axes and series to build
 */
fun cartesianChart(
    emptyText: StringResource,
    description: StringResource,
    accessibilityLabels: CartesianChart.AccessibilityLabels,
    block: CartesianChartBuilder.() -> Unit,
): CartesianChart {
    return CartesianChartBuilder(
        emptyText = emptyText,
        description = description,
        accessibilityLabels = accessibilityLabels,
    ).apply(block).build()
}

/**
 * Collects axes, ordered series and appearance for [cartesianChart].
 *
 * Styling methods override individual fields of a default configuration; successive calls retain
 * earlier choices, with the last call to a styling method winning. Both axes default to zero to one
 * with numeric labels in the default locale.
 *
 * @param emptyText the empty-state text
 * @param description the screen reader summary
 * @param accessibilityLabels the screen reader actions for stepping through points
 */
@CartesianChartDsl
@Suppress("TooManyFunctions") // Each appearance setting has its own flat DSL method.
class CartesianChartBuilder internal constructor(
    private val emptyText: StringResource,
    private val description: StringResource,
    private val accessibilityLabels: CartesianChart.AccessibilityLabels,
) {
    private var config = CartesianChartConfig()
    private var horizontalAxis = CartesianChart.Axis(
        range = CartesianChart.Range(
            start = 0.0,
            end = 1.0,
        ),
        format = NumberAxisFormat(locale = Locale.getDefault()),
    )
    private var verticalAxis = horizontalAxis
    private val series = mutableListOf<CartesianChart.Series>()

    /**
     * Sets the horizontal axis to an existing [axis].
     */
    fun xAxis(axis: CartesianChart.Axis) {
        horizontalAxis = axis
    }

    /**
     * Sets the horizontal numeric [range] and its tick [format].
     */
    fun xAxis(range: CartesianChart.Range, format: AxisFormat) {
        xAxis(
            axis = CartesianChart.Axis(
                range = range,
                format = format,
            ),
        )
    }

    /**
     * Sets the vertical axis to an existing [axis].
     */
    fun yAxis(axis: CartesianChart.Axis) {
        verticalAxis = axis
    }

    /**
     * Sets the vertical numeric [range] and its tick [format].
     */
    fun yAxis(range: CartesianChart.Range, format: AxisFormat) {
        yAxis(
            axis = CartesianChart.Axis(
                range = range,
                format = format,
            ),
        )
    }

    /**
     * Appends an existing [series], taking a snapshot of its points.
     */
    fun series(series: CartesianChart.Series) {
        this.series += series.copy(points = series.points.toList())
    }

    /**
     * Appends a series with [style], [color] and optional [highlightFormat], collecting points in [block].
     */
    fun series(
        style: CartesianChart.Style,
        color: ColorToken,
        highlightFormat: HighlightFormat? = null,
        block: CartesianChartSeriesBuilder.() -> Unit,
    ) {
        series += CartesianChart.Series(
            style = style,
            color = color,
            points = CartesianChartSeriesBuilder().apply(block).build(),
            highlightFormat = highlightFormat,
        )
    }

    /**
     * Appends a line series in [color], optionally labeled by [highlightFormat], with points from [block].
     */
    fun line(
        color: ColorToken,
        highlightFormat: HighlightFormat? = null,
        block: CartesianChartSeriesBuilder.() -> Unit,
    ) {
        series(
            style = CartesianChart.Style.Line,
            color = color,
            highlightFormat = highlightFormat,
            block = block,
        )
    }

    /**
     * Appends a bar series in [color], optionally labeled by [highlightFormat], with points from [block].
     *
     * Bars grow from zero, so give the vertical axis a range including zero, such as
     * `CartesianChart.Range.fitting(values, includeZero = true)`.
     */
    fun bar(
        color: ColorToken,
        highlightFormat: HighlightFormat? = null,
        block: CartesianChartSeriesBuilder.() -> Unit,
    ) {
        series(
            style = CartesianChart.Style.Bar,
            color = color,
            highlightFormat = highlightFormat,
            block = block,
        )
    }

    /**
     * Appends unconnected points in [color], optionally labeled by [highlightFormat], collected in [block].
     */
    fun points(
        color: ColorToken,
        highlightFormat: HighlightFormat? = null,
        block: CartesianChartSeriesBuilder.() -> Unit,
    ) {
        series(
            style = CartesianChart.Style.Point,
            color = color,
            highlightFormat = highlightFormat,
            block = block,
        )
    }

    /**
     * Sets the whole chart background.
     */
    fun backgroundColor(color: ColorToken) {
        config = config.copy(backgroundColor = color)
    }

    /**
     * Sets the plot background.
     */
    fun plotBackgroundColor(color: ColorToken) {
        config = config.copy(plotBackgroundColor = color)
    }

    /**
     * Sets the grid, baseline, tick and plot border color.
     */
    fun gridColor(color: ColorToken) {
        config = config.copy(gridColor = color)
    }

    /**
     * Sets whether a border frames the plot, using the grid color and width.
     */
    fun showBorder(show: Boolean) {
        config = config.copy(showBorder = show)
    }

    /**
     * Sets both axes’ label color.
     */
    fun axisTextColor(color: ColorToken) {
        config = config.copy(axisTextColor = color)
    }

    /**
     * Sets the empty-state text color.
     */
    fun emptyTextColor(color: ColorToken) {
        config = config.copy(emptyTextColor = color)
    }

    /**
     * Sets both axes’ label style.
     */
    fun axisTextStyle(style: TextStyleToken) {
        config = config.copy(typography = config.typography.copy(axis = style))
    }

    /**
     * Sets the empty-state text style.
     */
    fun emptyTextStyle(style: TextStyleToken) {
        config = config.copy(typography = config.typography.copy(empty = style))
    }

    /**
     * Sets the highlight heading style.
     */
    fun highlightTitleTextStyle(style: TextStyleToken) {
        config = config.copy(typography = config.typography.copy(highlightTitle = style))
    }

    /**
     * Sets the highlighted value style.
     */
    fun highlightValueTextStyle(style: TextStyleToken) {
        config = config.copy(typography = config.typography.copy(highlightValue = style))
    }

    /**
     * Sets the highlighted value weight.
     */
    fun highlightValueFontWeight(weight: FontWeight) {
        config = config.copy(typography = config.typography.copy(highlightValueWeight = weight))
    }

    /**
     * Sets the grid, tick and baseline width.
     */
    fun gridWidth(width: SizeToken) {
        config = config.copy(drawing = config.drawing.copy(gridWidth = width))
    }

    /**
     * Sets the series and highlight line width.
     */
    fun lineWidth(width: SizeToken) {
        config = config.copy(drawing = config.drawing.copy(lineWidth = width))
    }

    /**
     * Sets the label gap and tick length.
     */
    fun labelSpacing(spacing: SpacingToken) {
        config = config.copy(drawing = config.drawing.copy(labelSpacing = spacing))
    }

    /**
     * Sets the line and point dot radius.
     */
    fun pointRadius(radius: Dp) {
        config = config.copy(drawing = config.drawing.copy(pointRadius = radius))
    }

    /**
     * Sets the minimum bar width.
     */
    fun minimumBarWidth(width: Dp) {
        config = config.copy(drawing = config.drawing.copy(minimumBarWidth = width))
    }

    /**
     * Sets the share of each X interval occupied by its bar.
     */
    fun barWidthFraction(fraction: Float) {
        config = config.copy(drawing = config.drawing.copy(barWidthFraction = fraction))
    }

    /**
     * Sets the X grid dash length; null draws solid lines.
     */
    fun xGridDashLength(length: Dp?) {
        config = config.copy(drawing = config.drawing.copy(xGridDashLength = length))
    }

    /**
     * Sets the highlight box background.
     */
    fun highlightBackgroundColor(color: ColorToken) {
        config = config.copy(highlight = config.highlight.copy(backgroundColor = color))
    }

    /**
     * Sets the highlight line color.
     */
    fun highlightLineColor(color: ColorToken) {
        config = config.copy(highlight = config.highlight.copy(lineColor = color))
    }

    /**
     * Sets the highlight heading color.
     */
    fun highlightTitleColor(color: ColorToken) {
        config = config.copy(highlight = config.highlight.copy(titleColor = color))
    }

    /**
     * Sets the highlighted value color.
     */
    fun highlightValueColor(color: ColorToken) {
        config = config.copy(highlight = config.highlight.copy(valueColor = color))
    }

    /**
     * Sets the highlight line opacity multiplier.
     */
    fun highlightLineAlpha(alpha: Float) {
        config = config.copy(highlight = config.highlight.copy(lineAlpha = alpha))
    }

    /**
     * Sets the highlight box shape.
     */
    fun highlightShape(shape: Shape) {
        config = config.copy(highlight = config.highlight.copy(shape = shape))
    }

    /**
     * Sets the highlight box inset and dot spacing.
     */
    fun highlightPadding(padding: SpacingToken) {
        config = config.copy(highlight = config.highlight.copy(padding = padding))
    }

    /**
     * Sets the highlight series dot radius.
     */
    fun highlightDotRadius(radius: Dp) {
        config = config.copy(highlight = config.highlight.copy(dotRadius = radius))
    }

    internal fun build(): CartesianChart {
        return CartesianChart(
            series = series.toList(),
            xAxis = horizontalAxis,
            yAxis = verticalAxis,
            emptyText = emptyText,
            description = description,
            accessibilityLabels = accessibilityLabels,
            config = config,
        )
    }
}

/**
 * Collects numeric points in ascending X order for one chart series.
 */
@CartesianChartDsl
class CartesianChartSeriesBuilder internal constructor() {
    private val points = mutableListOf<CartesianChart.Point>()

    /**
     * Appends a numeric [x]/[y] pair, optionally spanning to [xEnd].
     */
    fun point(x: Double, y: Double, xEnd: Double? = null) {
        point(
            point = CartesianChart.Point(
                x = x,
                y = y,
                xEnd = xEnd,
            ),
        )
    }

    /**
     * Appends an existing [point], including points produced by a time adapter.
     */
    fun point(point: CartesianChart.Point) {
        points += point
    }

    /**
     * Appends existing [points] in their supplied order.
     */
    fun points(points: List<CartesianChart.Point>) {
        this.points += points
    }

    internal fun build(): List<CartesianChart.Point> = points.toList()
}
