//
// This source file is part of the My Heart Counts Android open-source project
//
// SPDX-FileCopyrightText: 2026 Stanford University and the project authors (see CONTRIBUTORS.md)
//
// SPDX-License-Identifier: MIT

package edu.stanford.myheartcounts.dashboard.ui.chart

import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.google.common.truth.Truth.assertThat
import org.grovealliance.ui.StringResource
import org.grovealliance.ui.theme.ColorToken
import org.grovealliance.ui.theme.GroveShapes
import org.grovealliance.ui.theme.SizeToken
import org.grovealliance.ui.theme.SpacingToken
import org.grovealliance.ui.theme.TextStyleToken
import org.junit.Assert.assertThrows
import org.junit.Test
import java.time.Instant
import java.util.Locale

class CartesianChartBuilderTest {
    private val axis = CartesianChart.Axis(
        range = CartesianChart.Range(start = 0.0, end = 10.0),
        format = NumberAxisFormat(locale = Locale.US),
    )
    private val labels = CartesianChart.AccessibilityLabels(
        nextPoint = StringResource("Next point"),
        previousPoint = StringResource("Previous point"),
    )
    private val base = CartesianChart(
        series = listOf(
            CartesianChart.Series(
                style = CartesianChart.Style.Line,
                color = ColorToken.Primary,
                points = listOf(CartesianChart.Point(x = 1.0, y = 2.0, xEnd = null)),
                highlightFormat = Format,
            ),
        ),
        xAxis = axis,
        yAxis = axis,
        emptyText = StringResource("No Data"),
        description = StringResource("Chart"),
        accessibilityLabels = labels,
    )

    @Test
    fun `DSL produces the same model as direct construction including custom config`() {
        val config = CartesianChartConfig(gridColor = ColorToken.Secondary)
        val chart = build {
            gridColor(color = ColorToken.Secondary)
            xAxis(range = axis.range, format = axis.format)
            yAxis(axis = axis)
            line(color = ColorToken.Primary, highlightFormat = Format) { point(x = 1.0, y = 2.0) }
        }
        assertThat(chart).isEqualTo(base.copy(config = config))
    }

    @Test
    fun `flat colors and typography retain other overrides and the last value wins`() {
        val chart = build {
            xAxis(axis = axis)
            yAxis(axis = axis)
            gridColor(color = ColorToken.Error)
            showBorder(show = false)
            backgroundColor(color = ColorToken.Surface)
            plotBackgroundColor(color = ColorToken.SurfaceContainer)
            gridColor(color = ColorToken.Primary)
            axisTextColor(color = ColorToken.OnSurface)
            emptyTextColor(color = ColorToken.Secondary)
            axisTextStyle(style = TextStyleToken.LabelLarge)
            emptyTextStyle(style = TextStyleToken.BodyLarge)
            highlightTitleTextStyle(style = TextStyleToken.LabelMedium)
            highlightValueTextStyle(style = TextStyleToken.TitleLarge)
            highlightValueFontWeight(weight = FontWeight.Bold)
        }
        assertThat(chart.config).isEqualTo(
            CartesianChartConfig(
                backgroundColor = ColorToken.Surface,
                plotBackgroundColor = ColorToken.SurfaceContainer,
                gridColor = ColorToken.Primary,
                showBorder = false,
                axisTextColor = ColorToken.OnSurface,
                emptyTextColor = ColorToken.Secondary,
                typography = CartesianChartConfig.Typography(
                    axis = TextStyleToken.LabelLarge,
                    empty = TextStyleToken.BodyLarge,
                    highlightTitle = TextStyleToken.LabelMedium,
                    highlightValue = TextStyleToken.TitleLarge,
                    highlightValueWeight = FontWeight.Bold,
                ),
            ),
        )
    }

    @Test
    fun `flat drawing and highlight methods preserve sibling settings`() {
        val chart = build {
            xAxis(axis = axis)
            yAxis(axis = axis)
            gridWidth(width = SizeToken.Border.Medium)
            lineWidth(width = SizeToken.Custom(size = 3.dp))
            labelSpacing(spacing = SpacingToken.Small)
            pointRadius(radius = 4.dp)
            minimumBarWidth(width = 3.dp)
            barWidthFraction(fraction = 0.7f)
            xGridDashLength(length = null)
            highlightBackgroundColor(color = ColorToken.PrimaryContainer)
            highlightLineColor(color = ColorToken.Primary)
            highlightTitleColor(color = ColorToken.OnPrimaryContainer)
            highlightValueColor(color = ColorToken.OnPrimaryContainer)
            highlightLineAlpha(alpha = 0.8f)
            highlightShape(shape = GroveShapes.medium)
            highlightPadding(padding = SpacingToken.Small)
            highlightDotRadius(radius = 4.dp)
        }
        assertThat(chart.config).isEqualTo(
            CartesianChartConfig(
                drawing = CartesianChartConfig.Drawing(
                    gridWidth = SizeToken.Border.Medium,
                    lineWidth = SizeToken.Custom(size = 3.dp),
                    labelSpacing = SpacingToken.Small,
                    pointRadius = 4.dp,
                    minimumBarWidth = 3.dp,
                    barWidthFraction = 0.7f,
                    xGridDashLength = null,
                ),
                highlight = CartesianChartConfig.Highlight(
                    backgroundColor = ColorToken.PrimaryContainer,
                    lineColor = ColorToken.Primary,
                    titleColor = ColorToken.OnPrimaryContainer,
                    valueColor = ColorToken.OnPrimaryContainer,
                    lineAlpha = 0.8f,
                    shape = GroveShapes.medium,
                    padding = SpacingToken.Small,
                    dotRadius = 4.dp,
                ),
            ),
        )
    }

    @Test
    fun `series styles spans and supplied point order are preserved`() {
        val chart = build {
            xAxis(axis = axis)
            yAxis(range = axis.range, format = axis.format)
            line(color = ColorToken.Primary) { point(x = 1.0, y = 2.0) }
            bar(color = ColorToken.Secondary) { point(x = 2.0, y = 3.0, xEnd = 4.0) }
            points(color = ColorToken.Tertiary) { points(points = base.series.single().points) }
            series(series = base.series.single())
            series(style = CartesianChart.Style.Point, color = ColorToken.Error) { point(x = 5.0, y = 6.0) }
        }
        assertThat(chart.series.map { it.style }).containsExactly(
            CartesianChart.Style.Line, CartesianChart.Style.Bar, CartesianChart.Style.Point,
            CartesianChart.Style.Line, CartesianChart.Style.Point,
        ).inOrder()
        assertThat(chart.series[1].points.single()).isEqualTo(CartesianChart.Point(x = 2.0, y = 3.0, xEnd = 4.0))
        assertThat(chart.series.first().highlightFormat).isNull()
        assertThat(chart.series[3]).isEqualTo(base.series.single())
    }

    @Test
    fun `axes default to a numeric zero to one range`() {
        val chart = build {}
        val expected = CartesianChart.Axis(
            range = CartesianChart.Range(start = 0.0, end = 1.0),
            format = NumberAxisFormat(locale = Locale.getDefault()),
        )
        assertThat(chart.xAxis).isEqualTo(expected)
        assertThat(chart.yAxis).isEqualTo(expected)
    }

    @Test
    fun `overriding one axis retains the other default`() {
        val chart = build { xAxis(axis = axis) }
        assertThat(chart.xAxis).isEqualTo(axis)
        assertThat(chart.yAxis.range).isEqualTo(CartesianChart.Range(start = 0.0, end = 1.0))
    }

    @Test
    fun `empty charts are supported without inventing data`() {
        val chart = build {
            xAxis(axis = axis)
            yAxis(axis = axis)
        }
        assertThat(chart.series).isEmpty()
        assertThat(chart.config).isEqualTo(CartesianChartConfig())
    }

    @Test
    fun `model input checks are retained by the DSL`() {
        assertThrows(IllegalArgumentException::class.java) {
            build {
                xAxis(axis = axis)
                yAxis(axis = axis)
                line(color = ColorToken.Primary) {
                    point(x = 2.0, y = 1.0)
                    point(x = 1.0, y = 2.0)
                }
            }
        }
        assertThrows(IllegalArgumentException::class.java) {
            build {
                xAxis(axis = axis)
                yAxis(axis = axis)
                bar(color = ColorToken.Primary) { point(x = 2.0, y = 1.0, xEnd = 1.0) }
            }
        }
    }

    @Test
    fun `built collections are snapshots even when scopes and input lists are retained`() {
        lateinit var seriesScope: CartesianChartSeriesBuilder
        lateinit var chartScope: CartesianChartBuilder
        val source = base.series.single().points.toMutableList()
        val chart = build {
            chartScope = this
            xAxis(axis = axis)
            yAxis(axis = axis)
            series(series = base.series.single().copy(points = source))
            line(color = ColorToken.Primary) {
                seriesScope = this
                points(points = source)
            }
        }
        source.clear()
        seriesScope.point(x = 2.0, y = 3.0)
        chartScope.series(series = base.series.single())
        chartScope.gridColor(color = ColorToken.Secondary)
        assertThat(chart.config).isEqualTo(CartesianChartConfig())
        assertThat(chart.series).hasSize(2)
        chart.series.forEach { assertThat(it.points).containsExactlyElementsIn(base.series.single().points) }
    }

    @Test
    fun `existing time adapter points work without another chart API`() {
        val origin = Instant.parse("2026-10-25T00:00:00Z")
        val coordinates = TimeCoordinates(origin = origin)
        val point = coordinates.point(start = origin, end = origin.plusSeconds(5), value = 7.0)
        val chart = build {
            xAxis(range = coordinates.range(start = origin, end = origin.plusSeconds(10)), format = axis.format)
            yAxis(axis = axis)
            bar(color = ColorToken.Primary) { point(point = point) }
        }
        assertThat(chart.series.single().points).containsExactly(point)
    }

    private fun build(
        block: CartesianChartBuilder.() -> Unit,
    ): CartesianChart = cartesianChart(
        emptyText = base.emptyText,
        description = base.description,
        accessibilityLabels = labels,
        block = block,
    )

    private object Format : HighlightFormat {
        override fun label(point: CartesianChart.Point): Highlight = Highlight(
            value = StringResource("${point.y}"),
            title = null,
        )
    }
}
