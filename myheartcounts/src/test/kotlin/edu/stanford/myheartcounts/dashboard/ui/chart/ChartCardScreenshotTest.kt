//
// This source file is part of the My Heart Counts Android open-source project
//
// SPDX-FileCopyrightText: 2026 Stanford University and the project authors (see CONTRIBUTORS.md)
//
// SPDX-License-Identifier: MIT

package edu.stanford.myheartcounts.dashboard.ui.chart

import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.ui.Modifier
import edu.stanford.myheartcounts.dashboard.ui.DashboardColorPalette
import kotlinx.coroutines.flow.MutableStateFlow
import org.grovealliance.testing.screenshot.ScreenshotTest
import org.grovealliance.ui.StringResource
import org.grovealliance.ui.theme.Spacings
import org.junit.Test
import java.time.Duration
import java.time.Instant
import java.time.ZoneOffset
import java.util.Locale

class ChartCardScreenshotTest : ScreenshotTest() {
    private val start = Instant.parse("2026-10-11T00:00:00Z")
    private val coordinates = TimeCoordinates(origin = start)
    private val picker = MenuPicker(
        label = StringResource("Time range"),
        options = listOf(StringResource("Last 7 days"), StringResource("Last 14 days")),
        selectedIndex = 1,
        onSelected = {},
    )
    private val chart = CartesianChart(
        series = listOf(
            CartesianChart.Series(
                style = CartesianChart.Style.Bar,
                color = DashboardColorPalette.blue,
                points = (0 until 14).map { day ->
                    coordinates.point(
                        start = start.plus(Duration.ofDays(day.toLong())),
                        end = start.plus(Duration.ofDays(day + 1L)),
                        value = 6.0 + day % 4,
                    )
                },
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
        description = StringResource("Hours asleep over the last two weeks"),
        accessibilityLabels = CartesianChart.AccessibilityLabels(
            nextPoint = StringResource("Next point"),
            previousPoint = StringResource("Previous point"),
        ),
    )
    private val base = ChartCard(
        title = StringResource("Sleep"),
        picker = MutableStateFlow(picker),
        chart = MutableStateFlow(chart),
    )

    @Test
    fun `ChartCard screenshot`() {
        screenshot { base.Content(modifier = Modifier.padding(Spacings.medium).fillMaxWidth()) }
    }

    @Test
    fun `ChartCard without picker screenshot`() {
        screenshot { base.copy(picker = MutableStateFlow(null)).Content(modifier = Modifier.padding(Spacings.medium).fillMaxWidth()) }
    }

    @Test
    fun `ChartCard empty screenshot`() {
        screenshot {
            base.copy(chart = MutableStateFlow(chart.copy(series = emptyList())))
                .Content(modifier = Modifier.padding(Spacings.medium).fillMaxWidth())
        }
    }

    @Test
    fun `ChartCard long title screenshot`() {
        screenshot {
            base.copy(title = StringResource("A title long enough to wrap onto two lines beside the range picker"))
                .Content(modifier = Modifier.padding(Spacings.medium).fillMaxWidth())
        }
    }
}
