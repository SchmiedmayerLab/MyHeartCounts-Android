//
// This source file is part of the My Heart Counts Android open-source project
//
// SPDX-FileCopyrightText: 2026 Stanford University and the project authors (see CONTRIBUTORS.md)
//
// SPDX-License-Identifier: MIT

package edu.stanford.myheartcounts.dashboard

import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccountCircle
import androidx.compose.material.icons.filled.MilitaryTech
import androidx.compose.ui.Modifier
import edu.stanford.myheartcounts.MHCStrings
import edu.stanford.myheartcounts.dashboard.ui.DashboardColorPalette
import edu.stanford.myheartcounts.dashboard.ui.DashboardSection
import edu.stanford.myheartcounts.dashboard.ui.Gauge
import edu.stanford.myheartcounts.dashboard.ui.GaugeCard
import edu.stanford.myheartcounts.dashboard.ui.GaugeTile
import edu.stanford.myheartcounts.dashboard.ui.GaugeTileGrid
import edu.stanford.myheartcounts.ui.MarkdownCard
import edu.stanford.myheartcounts.ui.NavigationCard
import kotlinx.coroutines.flow.MutableStateFlow
import org.grovealliance.markdown.MarkdownDocument
import org.grovealliance.testing.screenshot.ScreenshotTest
import org.grovealliance.ui.GroveScaffoldState
import org.grovealliance.ui.ImageResource
import org.grovealliance.ui.StringResource
import org.grovealliance.ui.groveAppBar
import org.grovealliance.ui.theme.ColorToken
import org.junit.Test

class HeartHealthContentLayoutScreenshotTest : ScreenshotTest() {

    @Test
    fun `HeartHealthScreenContent screenshot`() {
        val screen = HeartHealthScreenContent(
            scaffoldState = GroveScaffoldState(
                appBar = groveAppBar {
                    centerAlign(value = false)
                    title(MHCStrings.home_tab_heart_health)
                    action(imageResource = ImageResource(image = Icons.Default.MilitaryTech)) {}
                    action(imageResource = ImageResource(image = Icons.Default.AccountCircle)) {}
                },
            ),
            state = MutableStateFlow(
                HeartHealthLayoutState.Content(
                    layout = HeartHealthContentLayout(
                        sections = listOf(
                            DashboardSection(
                                title = null,
                                content = GaugeCard(gauge = overallGauge()),
                                footnote = StringResource(MHCStrings.heart_health_score_footnote),
                            ),
                            DashboardSection(
                                title = StringResource(MHCStrings.heart_health_components_title),
                                content = GaugeTileGrid(
                                    tiles = listOf(
                                        tile(title = MHCStrings.heart_score_exercise_minutes, score = 1.0, caption = "12:00 AM"),
                                        tile(title = MHCStrings.heart_score_diet, score = 0.85, caption = "Jan 1, 2026"),
                                        tile(title = MHCStrings.heart_score_blood_lipids, score = 0.6, caption = null),
                                        tile(title = MHCStrings.heart_score_blood_glucose, score = null, caption = null),
                                    ),
                                ),
                                footnote = StringResource(MHCStrings.heart_health_components_footnote),
                            ),
                            DashboardSection(
                                title = StringResource(MHCStrings.heart_health_explainer_title),
                                content = MarkdownCard(document = MarkdownDocument.process(text = EXPLAINER)),
                                footnote = null,
                            ),
                            DashboardSection(
                                title = StringResource(MHCStrings.heart_health_past_data_title),
                                content = NavigationCard(
                                    title = StringResource(MHCStrings.heart_health_past_results),
                                    onClick = {},
                                ),
                                footnote = null,
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

    private fun overallGauge() = Gauge(
        progress = OVERALL_SCORE,
        colors = DashboardColorPalette.redToGreen,
        lineWidth = Gauge.defaultLineWidth * LARGE_LINE_WIDTH,
        size = Gauge.Size.Large,
        label = Gauge.Label(
            value = StringResource("86"),
            caption = Gauge.Caption.Range(
                minimum = StringResource("0"),
                maximum = StringResource("100"),
            ),
        ),
    )

    /**
     * A factor's tile; without a [score] its gauge is empty and it invites the participant to learn
     * more, and without a [caption] it notes yesterday.
     */
    private fun tile(title: Int, score: Double?, caption: String?) = GaugeTile(
        title = StringResource(title),
        gauge = Gauge(
            progress = score?.toFloat(),
            colors = if (score == null) listOf(ColorToken.Outline) else DashboardColorPalette.redToGreen,
            lineWidth = Gauge.defaultLineWidth * REGULAR_LINE_WIDTH,
            size = Gauge.Size.Regular,
            label = Gauge.Label(
                value = StringResource(score?.let { "${(it * SCORE_SCALE).toInt()}" } ?: "—"),
                caption = Gauge.Caption.Description(
                    text = when {
                        score == null -> StringResource(MHCStrings.heart_health_learn_more)
                        caption == null -> StringResource(MHCStrings.heart_health_measured_yesterday)
                        else -> StringResource(caption)
                    },
                ),
            ),
        ),
        onClick = {},
    )

    private companion object {
        const val OVERALL_SCORE = 0.86f
        const val SCORE_SCALE = 100
        const val LARGE_LINE_WIDTH = 2f
        const val REGULAR_LINE_WIDTH = 1.5f
        const val EXPLAINER = """#### What is this score?
Your My Heart Counts Health Score provides a snapshot of your overall cardiovascular health."""
    }
}
