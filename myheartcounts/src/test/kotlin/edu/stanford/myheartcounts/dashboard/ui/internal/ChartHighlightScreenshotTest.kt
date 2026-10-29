//
// This source file is part of the My Heart Counts Android open-source project
//
// SPDX-FileCopyrightText: 2026 Stanford University and the project authors (see CONTRIBUTORS.md)
//
// SPDX-License-Identifier: MIT

package edu.stanford.myheartcounts.dashboard.ui.internal

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.unit.dp
import edu.stanford.myheartcounts.dashboard.ui.DashboardColorPalette
import edu.stanford.myheartcounts.dashboard.ui.chart.CartesianChart
import edu.stanford.myheartcounts.dashboard.ui.chart.CartesianChartConfig
import org.grovealliance.testing.screenshot.ScreenshotTest
import org.grovealliance.ui.theme.ColorToken
import org.grovealliance.ui.theme.Spacings
import org.grovealliance.ui.theme.resolve
import org.junit.Test

class ChartHighlightScreenshotTest : ScreenshotTest() {
    private val start = 0.0
    private val base = Scenario(
        position = 50,
        time = "18 Oct",
        values = listOf("8 hr 45 min"),
        style = CartesianChart.Style.Bar,
        config = CartesianChartConfig(),
    )

    @Test
    fun `ChartHighlight single bar screenshot`() {
        screenshot(scenario = base)
    }

    @Test
    fun `ChartHighlight two line series screenshot`() {
        screenshot(scenario = base.copy(values = listOf("Systolic 128 mmHg", "Diastolic 80 mmHg"), style = CartesianChart.Style.Line))
    }

    @Test
    fun `ChartHighlight clamped at left edge screenshot`() {
        screenshot(scenario = base.copy(position = 0))
    }

    @Test
    fun `ChartHighlight clamped at right edge screenshot`() {
        screenshot(scenario = base.copy(position = 100))
    }

    @Test
    fun `ChartHighlight without time screenshot`() {
        screenshot(scenario = base.copy(time = null))
    }

    @Test
    fun `ChartHighlight custom appearance screenshot`() {
        screenshot(
            scenario = base.copy(
                config = base.config.copy(
                    gridColor = ColorToken.Secondary,
                    highlight = base.config.highlight.copy(
                        backgroundColor = ColorToken.PrimaryContainer,
                        lineColor = ColorToken.Primary,
                        titleColor = ColorToken.OnPrimaryContainer,
                        valueColor = ColorToken.OnPrimaryContainer,
                        lineAlpha = 0.8f,
                        padding = org.grovealliance.ui.theme.SpacingToken.Small,
                        dotRadius = 4.dp,
                    ),
                ),
            ),
        )
    }

    private fun screenshot(scenario: Scenario) {
        val config = scenario.config
        screenshot {
            val measurer = rememberTextMeasurer()
            val labelStyle = config.typography.highlightTitle.resolve().copy(color = config.highlight.titleColor.resolve())
            val valueStyle = config.typography.highlightValue.resolve().copy(
                color = config.highlight.valueColor.resolve(),
                fontWeight = config.typography.highlightValueWeight,
            )
            val colors = listOf(DashboardColorPalette.blue.resolve(), DashboardColorPalette.red.resolve())
            val grid = config.gridColor.resolve()
            val line = config.highlight.lineColor.resolve()
            val background = config.highlight.backgroundColor.resolve()
            val shape = config.highlight.shape
            val gap = config.drawing.labelSpacing.resolve()
            val gridWidth = config.drawing.gridWidth.resolve()
            val lineWidth = config.drawing.lineWidth.resolve()
            val padding = config.highlight.padding.resolve()
            Canvas(modifier = Modifier.padding(Spacings.medium).fillMaxWidth().height(180.dp)) {
                val strokes = ChartStrokes(grid = gridWidth.toPx(), line = lineWidth.toPx(), gap = gap.toPx())
                val plot = ChartPlot(
                    area = Rect(left = 0f, top = 0f, right = size.width, bottom = size.height),
                    xRange = CartesianChart.Range(start = 0.0, end = 100.0),
                    yRange = CartesianChart.Range(start = 0.0, end = 10.0),
                )
                val time = (start + scenario.position)
                drawYAxis(plot = plot, labels = listOf(0.0, 5.0, 10.0).map { it to null }, color = grid, strokes = strokes, alpha = 1f)
                drawHighlightLine(plot = plot, x = time, color = line, alpha = config.highlight.lineAlpha, strokes = strokes)
                scenario.values.forEachIndexed { index, _ ->
                    val points = points(index = index)
                    drawSeries(
                        plot = plot,
                        style = scenario.style,
                        points = points,
                        connected = points,
                        color = colors[index],
                        strokes = strokes,
                        appearance = config.drawing,
                        reveal = ChartReveal.Complete,
                    )
                }
                drawHighlightBox(
                    plot = plot,
                    x = time,
                    title = scenario.time?.let { measurer.measure(text = it, style = labelStyle) },
                    values = scenario.values.mapIndexed { index, text ->
                        colors[index] to measurer.measure(text = text, style = valueStyle)
                    },
                    shape = shape,
                    background = background,
                    padding = padding.toPx(),
                    dotRadius = config.highlight.dotRadius,
                )
            }
        }
    }

    private fun points(index: Int): List<CartesianChart.Point> = listOf(
        CartesianChart.Point(x = (start + 10), xEnd = (start + 20), y = 4.0 + index),
        CartesianChart.Point(x = (start + 45), xEnd = (start + 55), y = 8.0 - index),
        CartesianChart.Point(x = (start + 80), xEnd = (start + 90), y = 6.0 + index),
    )

    private data class Scenario(
        val position: Long,
        val time: String?,
        val values: List<String>,
        val style: CartesianChart.Style,
        val config: CartesianChartConfig,
    )
}
