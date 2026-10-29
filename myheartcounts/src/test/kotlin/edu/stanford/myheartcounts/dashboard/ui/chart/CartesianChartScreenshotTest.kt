//
// This source file is part of the My Heart Counts Android open-source project
//
// SPDX-FileCopyrightText: 2026 Stanford University and the project authors (see CONTRIBUTORS.md)
//
// SPDX-License-Identifier: MIT

package edu.stanford.myheartcounts.dashboard.ui.chart

import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import edu.stanford.myheartcounts.dashboard.ui.DashboardColorPalette
import org.grovealliance.testing.screenshot.ScreenshotTest
import org.grovealliance.ui.StringResource
import org.grovealliance.ui.theme.ColorToken
import org.grovealliance.ui.theme.SizeToken
import org.grovealliance.ui.theme.SpacingToken
import org.grovealliance.ui.theme.Spacings
import org.grovealliance.ui.theme.TextStyleToken
import org.junit.Test
import java.time.Duration
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneOffset
import java.util.Locale
import kotlin.math.sin

class CartesianChartScreenshotTest : ScreenshotTest() {

    private val start = Instant.parse("2026-10-11T00:00:00Z")
    private val coordinates = TimeCoordinates(origin = start)

    private val base = CartesianChart(
        series = listOf(
            CartesianChart.Series(
                style = CartesianChart.Style.Bar,
                color = DashboardColorPalette.blue,
                points = daily(from = start, days = 14) { day -> 6.5 + 2 * sin(day * 0.9) },
                highlightFormat = null,
            ),
        ),
        xAxis = CartesianChart.Axis(
            range = coordinates.range(
                start = start,
                end = start.plus(Duration.ofDays(15)),
            ),
            format = CalendarTimeAxisFormat(
                coordinates = coordinates,
                zone = ZoneOffset.UTC,
                locale = Locale.US,
            ),
        ),
        yAxis = CartesianChart.Axis(
            range = CartesianChart.Range(
                start = 0.0,
                end = 10.0,
            ),
            format = NumberAxisFormat(locale = Locale.US),
        ),
        emptyText = StringResource("No Data"),
        description = StringResource("Description"),
        accessibilityLabels = CartesianChart.AccessibilityLabels(
            nextPoint = StringResource("Next point"),
            previousPoint = StringResource("Previous point"),
        ),
    )

    @Test
    fun `CartesianChart bars over two weeks screenshot`() {
        screenshot(chart = base)
    }

    @Test
    fun `CartesianChart line over a week screenshot`() {
        screenshot(
            chart = base.copy(
                series = listOf(
                    base.series.single().copy(
                        style = CartesianChart.Style.Line,
                        color = DashboardColorPalette.green,
                        points = daily(from = start, days = 7) { day -> 30 + 20 * sin(day * 1.3) },
                    ),
                ),
                xAxis = base.xAxis.copy(range = range(from = start, days = 7)),
            ),
        )
    }

    @Test
    fun `CartesianChart points over a week screenshot`() {
        screenshot(
            chart = base.copy(
                series = listOf(
                    base.series.single().copy(
                        style = CartesianChart.Style.Point,
                        color = DashboardColorPalette.red,
                        points = (0 until 7 * 24).map { hour ->
                            val time = start.plus(Duration.ofHours(hour.toLong()))
                            coordinates.point(
                                start = time,
                                end = time,
                                value = 75 + 40 * sin(hour * 0.26) + 15 * sin(hour * 1.7),
                            )
                        },
                    ),
                ),
                xAxis = base.xAxis.copy(range = range(from = start, days = 7)),
            ),
        )
    }

    @Test
    fun `CartesianChart two line series over three months screenshot`() {
        val from = Instant.parse("2026-07-25T00:00:00Z")
        val days = (0 until 92 step 3)
        screenshot(
            chart = base.copy(
                series = listOf(
                    base.series.single().copy(
                        style = CartesianChart.Style.Line,
                        color = DashboardColorPalette.red,
                        points = days.map { day -> moment(from = from, day = day, value = 125 + 8 * sin(day * 0.4)) },
                    ),
                    base.series.single().copy(
                        style = CartesianChart.Style.Line,
                        color = DashboardColorPalette.blue,
                        points = days.map { day -> moment(from = from, day = day, value = 80 + 6 * sin(day * 0.5)) },
                    ),
                ),
                xAxis = base.xAxis.copy(range = range(from = from, days = 92)),
            ),
        )
    }

    @Test
    fun `CartesianChart bars over six months screenshot`() {
        val from = Instant.parse("2026-04-25T00:00:00Z")
        screenshot(
            chart = base.copy(
                series = listOf(
                    base.series.single().copy(
                        color = DashboardColorPalette.orange,
                        points = (0 until 26).map { week ->
                            coordinates.point(
                                start = from.plus(Duration.ofDays(week * 7L)),
                                end = from.plus(Duration.ofDays(week * 7L + 7)),
                                value = 8_000 + 3_500 * sin(week * 0.6),
                            )
                        },
                    ),
                ),
                xAxis = base.xAxis.copy(range = range(from = from, days = 183)),
            ),
        )
    }

    @Test
    fun `CartesianChart line over a year screenshot`() {
        val from = LocalDate.of(2025, 10, 25)
        screenshot(
            chart = base.copy(
                series = listOf(
                    base.series.single().copy(
                        style = CartesianChart.Style.Line,
                        color = DashboardColorPalette.red,
                        points = (0 until 12).map { month ->
                            val monthStart = from.plusMonths(month.toLong()).atStartOfDay(ZoneOffset.UTC)
                            coordinates.point(
                                start = monthStart.toInstant(),
                                end = monthStart.plusMonths(1).toInstant(),
                                value = 110 + 15 * sin(month * 0.8),
                            )
                        },
                    ),
                ),
                xAxis = base.xAxis.copy(range = range(from = from.atStartOfDay(ZoneOffset.UTC).toInstant(), days = 365)),
            ),
        )
    }

    @Test
    fun `CartesianChart fractional values screenshot`() {
        screenshot(
            chart = base.copy(
                series = listOf(
                    base.series.single().copy(
                        style = CartesianChart.Style.Line,
                        points = daily(from = start, days = 14) { day -> 0.5 + 0.3 * sin(day * 0.7) },
                    ),
                ),
            ),
        )
    }

    @Test
    fun `CartesianChart line with gaps screenshot`() {
        screenshot(
            chart = base.copy(
                series = listOf(
                    base.series.single().copy(
                        style = CartesianChart.Style.Line,
                        points = daily(from = start, days = 14) { day -> 6.5 + 2 * sin(day * 0.9) }
                            .filterIndexed { index, _ -> index !in 4..7 },
                    ),
                ),
            ),
        )
    }

    @Test
    fun `CartesianChart without data screenshot`() {
        screenshot(chart = base.copy(series = emptyList()))
    }

    @Test
    fun `CartesianChart with points outside its range screenshot`() {
        screenshot(
            chart = base.copy(
                series = listOf(
                    base.series.single().copy(
                        points = daily(from = start.minus(Duration.ofDays(5)), days = 25) { day -> 6.5 + 2 * sin(day * 0.9) },
                    ),
                ),
            ),
        )
    }

    @Test
    fun `CartesianChart line running past both edges screenshot`() {
        screenshot(
            chart = base.copy(
                series = listOf(
                    base.series.single().copy(
                        style = CartesianChart.Style.Line,
                        points = daily(from = start.minus(Duration.ofDays(5)), days = 25) { day -> 6.5 + 2 * sin(day * 0.9) },
                    ),
                ),
            ),
        )
    }

    @Test
    fun `CartesianChart tiny screenshot`() {
        screenshot(
            chart = base,
            width = TINY_WIDTH,
        )
    }

    @Test
    fun `CartesianChart narrow screenshot`() {
        screenshot(
            chart = base,
            width = NARROW_WIDTH,
        )
    }

    @Test
    fun `CartesianChart numeric distance and elevation screenshot`() {
        screenshot(
            chart = base.copy(
                series = listOf(
                    base.series.single().copy(
                        style = CartesianChart.Style.Line,
                        points = listOf(
                            CartesianChart.Point(x = 0.0, y = 120.0, xEnd = null),
                            CartesianChart.Point(x = 1.5, y = 145.0, xEnd = null),
                            CartesianChart.Point(x = 3.0, y = 130.0, xEnd = null),
                            CartesianChart.Point(x = 5.0, y = 155.0, xEnd = null),
                        ),
                    ),
                ),
                xAxis = CartesianChart.Axis(range = CartesianChart.Range(start = 0.0, end = 5.0), format = DistanceFormat),
                yAxis = CartesianChart.Axis(range = CartesianChart.Range(start = 100.0, end = 160.0), format = ElevationFormat),
            ),
            fitY = false,
        )
    }

    @Test
    fun `CartesianChart negative fractional coordinates screenshot`() {
        screenshot(
            chart = base.copy(
                series = listOf(
                    base.series.single().copy(
                        points = listOf(
                            CartesianChart.Point(x = -1.0, y = -3.0, xEnd = -0.5),
                            CartesianChart.Point(x = -0.25, y = 4.0, xEnd = 0.25),
                            CartesianChart.Point(x = 0.5, y = -2.0, xEnd = 1.0),
                        ),
                    ),
                ),
                xAxis = CartesianChart.Axis(
                    range = CartesianChart.Range(start = -1.0, end = 1.0),
                    format = NumberAxisFormat(locale = Locale.US),
                ),
                yAxis = CartesianChart.Axis(
                    range = CartesianChart.Range(start = -5.0, end = 5.0),
                    format = NumberAxisFormat(locale = Locale.US),
                ),
            ),
            fitY = false,
        )
    }

    @Test
    fun `CartesianChart calendar labels on vertical axis screenshot`() {
        screenshot(
            chart = base.copy(
                series = listOf(
                    base.series.single().copy(
                        style = CartesianChart.Style.Point,
                        points = listOf(
                            CartesianChart.Point(x = 20.0, y = 0.0, xEnd = null),
                            CartesianChart.Point(x = 40.0, y = Duration.ofDays(5).seconds.toDouble(), xEnd = null),
                            CartesianChart.Point(x = 60.0, y = Duration.ofDays(10).seconds.toDouble(), xEnd = null),
                        ),
                    ),
                ),
                xAxis = CartesianChart.Axis(
                    range = CartesianChart.Range(start = 0.0, end = 80.0),
                    format = NumberAxisFormat(locale = Locale.US),
                ),
                yAxis = base.xAxis,
            ),
            fitY = false,
        )
    }

    private object DistanceFormat : AxisFormat {
        override fun ticks(range: CartesianChart.Range): List<AxisFormat.Tick> = listOf(
            AxisFormat.Tick(at = 0.0, label = StringResource("0 km")),
            AxisFormat.Tick(at = 2.5, label = StringResource("2.5 km")),
            AxisFormat.Tick(at = 5.0, label = StringResource("5 km")),
        )
    }

    private object ElevationFormat : AxisFormat {
        override fun ticks(range: CartesianChart.Range): List<AxisFormat.Tick> = listOf(
            AxisFormat.Tick(at = 100.0, label = StringResource("100 m")),
            AxisFormat.Tick(at = 120.0, label = StringResource("120 m")),
            AxisFormat.Tick(at = 140.0, label = StringResource("140 m")),
            AxisFormat.Tick(at = 160.0, label = StringResource("160 m")),
        )
    }

    @Test
    fun `CartesianChart custom styling screenshot`() {
        screenshot(
            chart = base.copy(
                config = base.config.copy(
                    backgroundColor = ColorToken.SurfaceContainer,
                    plotBackgroundColor = ColorToken.PrimaryContainer,
                    gridColor = ColorToken.Primary,
                    axisTextColor = ColorToken.OnPrimaryContainer,
                    typography = base.config.typography.copy(axis = TextStyleToken.LabelMedium),
                    drawing = base.config.drawing.copy(
                        gridWidth = SizeToken.Border.Medium,
                        labelSpacing = SpacingToken.Small,
                        barWidthFraction = 0.7f,
                        xGridDashLength = null,
                    ),
                ),
            ),
        )
    }

    @Test
    fun `CartesianChart custom empty styling screenshot`() {
        screenshot(
            chart = base.copy(
                series = emptyList(),
                config = base.config.copy(
                    backgroundColor = ColorToken.SurfaceContainer,
                    gridColor = ColorToken.Secondary,
                    emptyTextColor = ColorToken.Primary,
                    typography = base.config.typography.copy(empty = TextStyleToken.TitleLarge),
                ),
            ),
        )
    }

    @Test
    fun `CartesianChart without border screenshot`() {
        screenshot(chart = base.copy(config = base.config.copy(showBorder = false)))
    }

    private fun screenshot(
        chart: CartesianChart,
        width: Dp? = null,
        fitY: Boolean = true,
    ) {
        val content = if (!fitY) {
            chart
        } else {
            chart.copy(
                yAxis = chart.yAxis.copy(
                    range = CartesianChart.Range.fitting(
                        values = chart.series.flatMap { series ->
                            series.points.filter { it.overlaps(chart.xAxis.range) }.map { it.y }
                        },
                        includeZero = true,
                    ),
                ),
            )
        }
        screenshot {
            content.Content(
                modifier = Modifier
                    .padding(Spacings.medium)
                    .then(if (width == null) Modifier.fillMaxWidth() else Modifier.width(width))
                    .height(CHART_HEIGHT),
            )
        }
    }

    private fun daily(
        from: Instant,
        days: Int,
        value: (Int) -> Double,
    ): List<CartesianChart.Point> {
        return (0 until days).map { day ->
            coordinates.point(
                start = from.plus(Duration.ofDays(day.toLong())),
                end = from.plus(Duration.ofDays(day + 1L)),
                value = value(day),
            )
        }
    }

    private fun moment(
        from: Instant,
        day: Int,
        value: Double,
    ): CartesianChart.Point {
        val time = from.plus(Duration.ofDays(day.toLong())).plus(Duration.ofHours(9))
        return coordinates.point(
            start = time,
            end = time,
            value = value,
        )
    }

    private fun range(
        from: Instant,
        days: Long,
    ): CartesianChart.Range {
        return coordinates.range(
            start = from,
            end = from.plus(Duration.ofDays(days)),
        )
    }

    private companion object {
        val CHART_HEIGHT = 180.dp
        val NARROW_WIDTH = 160.dp
        val TINY_WIDTH = 24.dp
    }
}
