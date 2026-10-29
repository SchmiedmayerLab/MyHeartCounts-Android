//
// This source file is part of the My Heart Counts Android open-source project
//
// SPDX-FileCopyrightText: 2026 Stanford University and the project authors (see CONTRIBUTORS.md)
//
// SPDX-License-Identifier: MIT

package edu.stanford.myheartcounts.dashboard.ui.chart

import androidx.compose.foundation.ScrollState
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.mutableStateOf
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.graphics.toPixelMap
import androidx.compose.ui.semantics.SemanticsActions
import androidx.compose.ui.semantics.SemanticsProperties
import androidx.compose.ui.test.SemanticsMatcher
import androidx.compose.ui.test.assert
import androidx.compose.ui.test.captureToImage
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.performTouchInput
import androidx.compose.ui.test.swipeUp
import androidx.compose.ui.unit.dp
import com.google.common.truth.Truth.assertThat
import edu.stanford.myheartcounts.dashboard.ui.DashboardColorPalette
import org.grovealliance.ui.StringResource
import org.grovealliance.ui.theme.ColorPalette
import org.grovealliance.ui.theme.ColorToken
import org.grovealliance.ui.theme.GroveTheme
import org.junit.Rule
import org.junit.Test
import java.time.Instant
import java.time.ZoneOffset
import java.util.Locale

class ChartInteractionTest {
    @get:Rule
    val compose = createComposeRule()

    private val start = Instant.parse("2026-10-11T00:00:00Z")
    private val coordinates = TimeCoordinates(origin = start)
    private val base = CartesianChart(
        series = listOf(
            CartesianChart.Series(
                style = CartesianChart.Style.Bar,
                color = DashboardColorPalette.blue,
                points = listOf(
                    coordinates.point(start = start, end = start.plusSeconds(50), value = 8.0),
                    coordinates.point(start = start.plusSeconds(50), end = start.plusSeconds(100), value = 6.0),
                ),
                highlightFormat = Format,
            ),
        ),
        xAxis = CartesianChart.Axis(
            range = coordinates.range(
                start = start,
                end = start.plusSeconds(100),
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
        description = StringResource("Sleep chart"),
        accessibilityLabels = CartesianChart.AccessibilityLabels(
            nextPoint = StringResource("Next point"),
            previousPoint = StringResource("Previous point"),
        ),
    )

    @Test
    fun touchAndHorizontalDragHighlightUntilRelease() {
        compose.setContent { GroveTheme { base.Content(modifier = Modifier.size(width = 320.dp, height = 180.dp)) } }
        val chart = compose.onNodeWithContentDescription("Sleep chart")
        chart.performTouchInput { down(Offset(x = width * 0.2f, y = height / 2f)) }
        chart.assert(SemanticsMatcher.expectValue(SemanticsProperties.StateDescription, "11 Oct, 8.0 hr"))
        chart.performTouchInput { moveTo(Offset(x = width * 0.8f, y = height / 2f)) }
        chart.assert(SemanticsMatcher.expectValue(SemanticsProperties.StateDescription, "11 Oct, 6.0 hr"))
        chart.performTouchInput { up() }
        chart.assert(SemanticsMatcher.keyNotDefined(SemanticsProperties.StateDescription))
    }

    @Test
    fun verticalDragScrollsParentAndClearsHighlight() {
        val scroll = ScrollState(initial = 0)
        compose.setContent {
            GroveTheme {
                Column(modifier = Modifier.height(400.dp).verticalScroll(scroll)) {
                    base.Content(modifier = Modifier.fillMaxWidth().height(180.dp))
                    Spacer(modifier = Modifier.height(800.dp))
                }
            }
        }
        val chart = compose.onNodeWithContentDescription("Sleep chart")
        chart.performTouchInput { swipeUp() }
        compose.runOnIdle { assertThat(scroll.value).isGreaterThan(0) }
        chart.assert(SemanticsMatcher.keyNotDefined(SemanticsProperties.StateDescription))
    }

    @Test
    fun accessibilityActionsStepThroughValuesAndDataChangesClearSelection() {
        val content = mutableStateOf(base)
        compose.setContent { GroveTheme { content.value.Content(modifier = Modifier.size(width = 320.dp, height = 180.dp)) } }
        val chart = compose.onNodeWithContentDescription("Sleep chart")
        fun step(label: String) {
            val action = chart.fetchSemanticsNode().config[SemanticsActions.CustomActions].single { it.label == label }
            compose.runOnIdle { assertThat(action.action()).isTrue() }
        }
        step("Next point")
        chart.assert(SemanticsMatcher.expectValue(SemanticsProperties.StateDescription, "11 Oct, 8.0 hr"))
        step("Next point")
        chart.assert(SemanticsMatcher.expectValue(SemanticsProperties.StateDescription, "11 Oct, 6.0 hr"))
        step("Previous point")
        chart.assert(SemanticsMatcher.expectValue(SemanticsProperties.StateDescription, "11 Oct, 8.0 hr"))
        compose.runOnIdle {
            content.value = base.copy(
                xAxis = base.xAxis.copy(
                    range = coordinates.range(
                        start = start,
                        end = start.plusSeconds(200),
                    ),
                ),
            )
        }
        chart.assert(SemanticsMatcher.keyNotDefined(SemanticsProperties.StateDescription))
    }

    @Test
    fun genericCoordinatesHighlightWithoutCalendarValues() {
        val content = base.copy(
            series = listOf(
                base.series.single().copy(
                    style = CartesianChart.Style.Point,
                    points = listOf(
                        CartesianChart.Point(x = -1.5, y = 120.0, xEnd = null),
                        CartesianChart.Point(x = 1.5, y = 145.0, xEnd = null),
                    ),
                    highlightFormat = CoordinateFormat,
                ),
            ),
            xAxis = CartesianChart.Axis(
                range = CartesianChart.Range(start = -2.0, end = 2.0),
                format = NumberAxisFormat(locale = Locale.US),
            ),
            yAxis = CartesianChart.Axis(
                range = CartesianChart.Range(start = 100.0, end = 160.0),
                format = NumberAxisFormat(locale = Locale.US),
            ),
        )
        compose.setContent { GroveTheme { content.Content(modifier = Modifier.size(width = 320.dp, height = 180.dp)) } }
        val chart = compose.onNodeWithContentDescription("Sleep chart")
        chart.performTouchInput { down(Offset(x = width * 0.15f, y = height / 2f)) }
        chart.assert(SemanticsMatcher.expectValue(SemanticsProperties.StateDescription, "-1.5 km, 120.0 m"))
        chart.performTouchInput { moveTo(Offset(x = width * 0.75f, y = height / 2f)) }
        chart.assert(SemanticsMatcher.expectValue(SemanticsProperties.StateDescription, "1.5 km, 145.0 m"))
        chart.performTouchInput { up() }
        chart.assert(SemanticsMatcher.keyNotDefined(SemanticsProperties.StateDescription))
    }

    @Test
    fun builderChartResolvesBackgroundTokenAgainWhenTheThemeChanges() {
        val dark = mutableStateOf(false)
        val content = cartesianChart(
            emptyText = base.emptyText,
            description = base.description,
            accessibilityLabels = base.accessibilityLabels,
        ) {
            xAxis(axis = base.xAxis)
            yAxis(axis = base.yAxis)
            backgroundColor(color = ColorToken.Surface)
            series(series = base.series.single())
        }
        compose.setContent {
            GroveTheme(darkTheme = dark.value) {
                content.Content(modifier = Modifier.size(width = 320.dp, height = 180.dp))
            }
        }
        val chart = compose.onNodeWithContentDescription("Sleep chart")
        assertThat(chart.captureToImage().toPixelMap()[0, 0].toArgb())
            .isEqualTo(ColorPalette.CardinalRed.light.surface.toArgb())
        compose.runOnIdle { dark.value = true }
        assertThat(chart.captureToImage().toPixelMap()[0, 0].toArgb())
            .isEqualTo(ColorPalette.CardinalRed.dark.surface.toArgb())
    }

    private object CoordinateFormat : HighlightFormat {
        override fun label(point: CartesianChart.Point): Highlight = Highlight(
            value = StringResource("${point.y} m"),
            title = StringResource("${point.x} km"),
        )
    }

    private object Format : HighlightFormat {
        override fun label(point: CartesianChart.Point): Highlight = Highlight(
            value = StringResource("${point.y} hr"),
            title = StringResource("11 Oct"),
        )
    }
}
