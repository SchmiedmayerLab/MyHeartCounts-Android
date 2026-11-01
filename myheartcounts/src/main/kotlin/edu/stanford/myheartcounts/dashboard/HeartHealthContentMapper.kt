//
// This source file is part of the My Heart Counts Android open-source project
//
// SPDX-FileCopyrightText: 2026 Stanford University and the project authors (see CONTRIBUTORS.md)
//
// SPDX-License-Identifier: MIT

package edu.stanford.myheartcounts.dashboard

import edu.stanford.myheartcounts.MHCStrings
import edu.stanford.myheartcounts.dashboard.ui.DashboardColorPalette
import edu.stanford.myheartcounts.dashboard.ui.DashboardSection
import edu.stanford.myheartcounts.dashboard.ui.Gauge
import edu.stanford.myheartcounts.dashboard.ui.GaugeCard
import edu.stanford.myheartcounts.dashboard.ui.GaugeTile
import edu.stanford.myheartcounts.dashboard.ui.GaugeTileGrid
import edu.stanford.myheartcounts.ui.MarkdownCard
import edu.stanford.myheartcounts.ui.NavigationCard
import org.grovealliance.markdown.MarkdownDocument
import org.grovealliance.ui.ActionSink
import org.grovealliance.ui.StringResource
import org.grovealliance.ui.theme.ColorToken
import java.text.NumberFormat

/**
 * Builds the Heart Health tab's layout from the participant's scores.
 */
interface HeartHealthContentMapper {

    /**
     * Maps [input] into the layout to render.
     */
    fun map(input: HeartHealthContentInput): HeartHealthContentLayout
}

/**
 * The data the Heart Health tab is built from.
 *
 * @param scores the participant's scores
 * @param explainer the explanation of the score; absent when the study provides none
 * @param actionSink receives the tab's intents
 */
data class HeartHealthContentInput(
    val scores: HeartHealthScores,
    val explainer: MarkdownDocument?,
    val actionSink: ActionSink<HeartHealthAction>,
)

/**
 * Shows the overall score on a large gauge from 0 to 100, and each factor on a gauge in a tile that
 * opens its details, followed by the explanation of the score when there is one. Scores are shown out
 * of 100; a factor's tile notes when its data was measured, as [dateFormatter] describes it, or
 * invites the participant to learn more without a score.
 */
class HeartHealthContentMapperImpl(
    private val dateFormatter: HeartScoreDateFormatter,
) : HeartHealthContentMapper {

    override fun map(input: HeartHealthContentInput): HeartHealthContentLayout = HeartHealthContentLayout(
        sections = listOfNotNull(
            DashboardSection(
                title = null,
                content = GaugeCard(gauge = overallGauge(input.scores.overall)),
                footnote = StringResource(MHCStrings.heart_health_score_footnote),
            ),
            DashboardSection(
                title = StringResource(MHCStrings.heart_health_components_title),
                content = GaugeTileGrid(
                    tiles = input.scores.components.map { result ->
                        componentTile(
                            result = result,
                            actionSink = input.actionSink,
                        )
                    },
                ),
                footnote = StringResource(MHCStrings.heart_health_components_footnote),
            ),
            input.explainer?.let { document ->
                DashboardSection(
                    title = StringResource(MHCStrings.heart_health_explainer_title),
                    content = MarkdownCard(document = document),
                    footnote = null,
                )
            },
            DashboardSection(
                title = StringResource(MHCStrings.heart_health_past_data_title),
                content = NavigationCard(
                    title = StringResource(MHCStrings.heart_health_past_results),
                    onClick = { input.actionSink.push(HeartHealthAction.PastDataSelected) },
                ),
                footnote = null,
            ),
        ),
    )

    private fun overallGauge(score: Double?) = Gauge(
        progress = score?.toFloat(),
        colors = gaugeColors(score),
        lineWidth = Gauge.defaultLineWidth * LARGE_LINE_WIDTH,
        size = Gauge.Size.Large,
        label = Gauge.Label(
            value = scoreText(score),
            caption = Gauge.Caption.Range(
                minimum = StringResource(NumberFormat.getIntegerInstance().format(0)),
                maximum = StringResource(NumberFormat.getIntegerInstance().format(SCORE_SCALE)),
            ),
        ),
    )

    private fun componentTile(
        result: HeartScoreResult,
        actionSink: ActionSink<HeartHealthAction>,
    ) = GaugeTile(
        title = StringResource(result.component.title),
        gauge = Gauge(
            progress = result.score?.toFloat(),
            colors = gaugeColors(result.score),
            lineWidth = Gauge.defaultLineWidth * REGULAR_LINE_WIDTH,
            size = Gauge.Size.Regular,
            label = Gauge.Label(
                value = scoreText(result.score),
                caption = Gauge.Caption.Description(text = componentCaption(result)),
            ),
        ),
        onClick = { actionSink.push(HeartHealthAction.ComponentSelected(component = result.component)) },
    )

    private fun gaugeColors(score: Double?): List<ColorToken> =
        if (score == null) listOf(ColorToken.Outline) else DashboardColorPalette.redToGreen

    private fun scoreText(score: Double?): StringResource = if (score == null) {
        StringResource(MHCStrings.heart_health_no_score)
    } else {
        StringResource(NumberFormat.getIntegerInstance().format((score * SCORE_SCALE).toInt()))
    }

    private fun componentCaption(result: HeartScoreResult): StringResource {
        val measuredAt = result.measuredAt
        return if (result.score == null || measuredAt == null) {
            StringResource(MHCStrings.heart_health_learn_more)
        } else {
            dateFormatter.measuredAt(instant = measuredAt)
        }
    }

    private companion object {
        /**
         * The points a full score is shown as.
         */
        const val SCORE_SCALE = 100

        /**
         * The thickness of the overall gauge, relative to Grove's default gauge thickness.
         */
        const val LARGE_LINE_WIDTH = 2f

        /**
         * The thickness of a factor's gauge, relative to Grove's default gauge thickness.
         */
        const val REGULAR_LINE_WIDTH = 1.5f
    }
}
