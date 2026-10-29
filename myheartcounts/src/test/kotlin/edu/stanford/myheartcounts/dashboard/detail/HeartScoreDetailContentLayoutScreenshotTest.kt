//
// This source file is part of the My Heart Counts Android open-source project
//
// SPDX-FileCopyrightText: 2026 Stanford University and the project authors (see CONTRIBUTORS.md)
//
// SPDX-License-Identifier: MIT

package edu.stanford.myheartcounts.dashboard.detail

import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.ui.Modifier
import edu.stanford.myheartcounts.MHCStrings
import edu.stanford.myheartcounts.dashboard.ui.ColorBandList
import edu.stanford.myheartcounts.dashboard.ui.DashboardColorPalette
import edu.stanford.myheartcounts.dashboard.ui.DashboardSection
import edu.stanford.myheartcounts.dashboard.ui.Gauge
import edu.stanford.myheartcounts.dashboard.ui.GaugeHeader
import edu.stanford.myheartcounts.dashboard.ui.chart.CalendarTimeAxisFormat
import edu.stanford.myheartcounts.dashboard.ui.chart.CartesianChart
import edu.stanford.myheartcounts.dashboard.ui.chart.ChartCard
import edu.stanford.myheartcounts.dashboard.ui.chart.MenuPicker
import edu.stanford.myheartcounts.dashboard.ui.chart.NumberAxisFormat
import edu.stanford.myheartcounts.dashboard.ui.chart.TimeCoordinates
import edu.stanford.myheartcounts.dashboard.ui.chart.cartesianChart
import edu.stanford.myheartcounts.ui.ExternalLink
import kotlinx.coroutines.flow.MutableStateFlow
import org.grovealliance.markdown.MarkdownDocument
import org.grovealliance.testing.screenshot.ScreenshotTest
import org.grovealliance.ui.ComposableContent
import org.grovealliance.ui.GroveScaffoldState
import org.grovealliance.ui.StringResource
import org.grovealliance.ui.groveAppBar
import org.grovealliance.ui.theme.ColorToken
import org.junit.Test
import java.time.Duration
import java.time.Instant
import java.time.ZoneOffset
import java.util.Locale

class HeartScoreDetailContentLayoutScreenshotTest : ScreenshotTest() {

    @Test
    fun `HeartScoreDetailScreenContent screenshot`() {
        val screen = HeartScoreDetailScreenContent(
            scaffoldState = GroveScaffoldState(
                appBar = groveAppBar {
                    back {}
                    title(MHCStrings.heart_score_blood_lipids)
                },
            ),
            state = MutableStateFlow(
                HeartScoreDetailLayoutState.Content(
                    layout = HeartScoreDetailContentLayout(
                        title = StringResource(MHCStrings.heart_score_blood_lipids),
                        sections = listOf(
                            section(content = header()),
                            section(content = chart()),
                            section(title = StringResource(MHCStrings.heart_detail_score_result), content = bands()),
                            section(
                                title = StringResource(MHCStrings.heart_detail_about, StringResource(MHCStrings.heart_score_blood_lipids)),
                                content = HeartScoreAboutContent(
                                    document = MarkdownDocument.process(text = EXPLAINER),
                                    links = listOf(
                                        ExternalLink(
                                            text = StringResource(MHCStrings.heart_detail_learn_more_host, "heart.org"),
                                            onClicked = {},
                                        ),
                                    ),
                                ),
                            ),
                        ),
                    ),
                ),
            ),
        )

        screenshot {
            screen.Content(modifier = Modifier.fillMaxSize())
        }
    }

    private fun section(content: ComposableContent, title: StringResource? = null) = DashboardSection(
        title = title,
        content = content,
        footnote = null,
    )

    private fun header() = GaugeHeader(
        label = StringResource(MHCStrings.heart_detail_most_recent_sample),
        value = GaugeHeader.Value.Measured(
            parts = listOf(
                GaugeHeader.Part(
                    text = StringResource("110"),
                    unit = StringResource("mg/dL"),
                ),
            ),
        ),
        footnote = StringResource("12:00 PM"),
        gauge = Gauge(
            progress = SCORE,
            colors = DashboardColorPalette.redToGreen,
            lineWidth = Gauge.defaultLineWidth * GAUGE_LINE_WIDTH,
            size = Gauge.Size.Regular,
            label = Gauge.Label(
                value = StringResource("60"),
                caption = null,
            ),
        ),
    )

    private fun bands(): ColorBandList {
        val (red, orange, yellow, green) = DashboardColorPalette.redToGreen
        return ColorBandList(
            bands = listOf(
                band(label = "70 – 99", score = "80", color = green),
                band(label = "100 – 129", score = "60", color = yellow, isHighlighted = true),
                band(label = "130 – 159", score = "40", color = orange),
                band(label = "190+", score = "0", color = red),
            ),
        )
    }

    private fun band(
        label: String,
        score: String,
        color: ColorToken,
        isHighlighted: Boolean = false,
    ) = ColorBandList.Band(
        leadingText = StringResource(label),
        trailingText = StringResource(score),
        fillColors = listOf(color),
        isHighlighted = isHighlighted,
    )

    private fun chart(): ChartCard {
        val start = Instant.parse("2025-12-22T00:00:00Z")
        val end = Instant.parse("2026-01-05T00:00:00Z")
        val coordinates = TimeCoordinates(origin = start)
        val chart = cartesianChart(
            emptyText = StringResource(MHCStrings.heart_detail_no_data),
            description = StringResource("Recent LDL cholesterol measurements"),
            accessibilityLabels = CartesianChart.AccessibilityLabels(
                nextPoint = StringResource(MHCStrings.heart_detail_next_point),
                previousPoint = StringResource(MHCStrings.heart_detail_previous_point),
            ),
        ) {
            xAxis(
                range = coordinates.range(start = start, end = end),
                format = CalendarTimeAxisFormat(
                    coordinates = coordinates,
                    zone = ZoneOffset.UTC,
                    locale = Locale.US,
                ),
            )
            yAxis(
                range = CartesianChart.Range.fitting(values = HISTORY, includeZero = false),
                format = NumberAxisFormat(locale = Locale.US),
            )
            series(style = CartesianChart.Style.Line, color = DashboardColorPalette.blue) {
                HISTORY.forEachIndexed { index, value ->
                    val day = start.plus(Duration.ofDays(index * HISTORY_STEP_DAYS + 1))
                    point(point = coordinates.point(start = day, end = day.plus(Duration.ofDays(1)), value = value))
                }
            }
        }
        return ChartCard(
            title = StringResource(MHCStrings.heart_detail_recent_values),
            picker = MutableStateFlow(
                MenuPicker(
                    label = StringResource(MHCStrings.heart_detail_time_range),
                    options = HeartScoreChartRange.entries.map { StringResource(it.title) },
                    selectedIndex = HeartScoreChartRange.Fortnight.ordinal,
                    onSelected = {},
                ),
            ),
            chart = MutableStateFlow(chart),
        )
    }

    private companion object {
        const val SCORE = 0.6f
        const val GAUGE_LINE_WIDTH = 1.5f
        const val HISTORY_STEP_DAYS = 4L
        val HISTORY = listOf(128.0, 121.0, 117.0, 110.0)
        const val EXPLAINER = "LDL cholesterol is the main source of cholesterol buildup in the arteries."
    }
}
