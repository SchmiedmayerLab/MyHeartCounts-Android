//
// This source file is part of the My Heart Counts Android open-source project
//
// SPDX-FileCopyrightText: 2026 Stanford University and the project authors (see CONTRIBUTORS.md)
//
// SPDX-License-Identifier: MIT

package edu.stanford.myheartcounts.dashboard.detail

import edu.stanford.myheartcounts.MHCStrings
import edu.stanford.myheartcounts.customsamples.CustomQuantitySample
import edu.stanford.myheartcounts.dashboard.HeartScoreComponent
import edu.stanford.myheartcounts.dashboard.HeartScoreDateFormatter
import edu.stanford.myheartcounts.dashboard.HeartScoreMeasurement
import edu.stanford.myheartcounts.dashboard.HeartScoreResult
import edu.stanford.myheartcounts.dashboard.NicotineExposureCategory
import edu.stanford.myheartcounts.dashboard.ui.DashboardColorPalette
import edu.stanford.myheartcounts.dashboard.ui.DashboardSection
import edu.stanford.myheartcounts.dashboard.ui.Gauge
import edu.stanford.myheartcounts.dashboard.ui.GaugeHeader
import edu.stanford.myheartcounts.dashboard.ui.chart.ChartCard
import edu.stanford.myheartcounts.ui.ExternalLink
import org.grovealliance.markdown.MarkdownDocument
import org.grovealliance.ui.ActionSink
import org.grovealliance.ui.ComposableContent
import org.grovealliance.ui.StringResource
import org.grovealliance.ui.theme.ColorToken
import java.net.URI
import java.text.NumberFormat

/**
 * What a factor's details show.
 *
 * @property chart the factor's history chart, or `null` for a factor without one
 * @property explainer the study's explanation of the factor, when it provides one
 * @property debugSamples every stored value of the factor, shown in debug mode only
 */
data class HeartScoreDetailContentInput(
    val result: HeartScoreResult,
    val chart: ChartCard?,
    val explainer: MarkdownDocument?,
    val debugSamples: List<CustomQuantitySample>?,
    val actionSink: ActionSink<HeartScoreDetailAction>,
)

/**
 * Lays out a factor's details.
 */
interface HeartScoreDetailContentMapper {

    /**
     * The details [input] describes.
     */
    fun map(input: HeartScoreDetailContentInput): HeartScoreDetailContentLayout
}

/**
 * Shows, in order: the most recent value beside the factor's gauge, its history chart, the bands
 * explaining its score, the study's explanation with its links, and the stored values in debug mode.
 */
class HeartScoreDetailContentMapperImpl(
    private val dateFormatter: HeartScoreDateFormatter,
    private val bandMapper: HeartScoreBandMapper,
) : HeartScoreDetailContentMapper {

    override fun map(input: HeartScoreDetailContentInput): HeartScoreDetailContentLayout = HeartScoreDetailContentLayout(
        title = StringResource(input.result.component.title),
        sections = listOfNotNull(
            section(title = null, content = header(result = input.result)),
            input.chart?.let { section(title = null, content = it) },
            bandMapper.map(result = input.result)?.let { bands ->
                DashboardSection(
                    title = StringResource(MHCStrings.heart_detail_score_result),
                    content = bands,
                    footnote = StringResource(MHCStrings.heart_detail_exercise_footer)
                        .takeIf { input.result.component == HeartScoreComponent.ExerciseMinutes },
                )
            },
            input.explainer?.let { document ->
                section(
                    title = StringResource(MHCStrings.heart_detail_about, StringResource(input.result.component.title)),
                    content = HeartScoreAboutContent(
                        document = document,
                        links = links(document = document, actions = input.actionSink),
                    ),
                )
            },
            input.debugSamples?.let { samples ->
                section(
                    title = StringResource(MHCStrings.heart_detail_debug_samples),
                    content = HeartScoreDebugContent(samples = samples),
                )
            },
        ),
    )

    private fun header(result: HeartScoreResult) = GaugeHeader(
        label = StringResource(headerLabel(result = result)),
        value = value(result = result),
        footnote = result.measuredAt?.let { dateFormatter.measuredAt(instant = it) },
        gauge = Gauge(
            progress = result.score?.toFloat(),
            colors = if (result.score == null) listOf(ColorToken.Outline) else DashboardColorPalette.redToGreen,
            lineWidth = Gauge.defaultLineWidth * GAUGE_LINE_WIDTH,
            size = Gauge.Size.Regular,
            label = Gauge.Label(
                value = result.score?.let { StringResource(number(value = (it * SCORE_SCALE).toInt().toDouble())) }
                    ?: StringResource(MHCStrings.heart_health_no_score),
                caption = null,
            ),
        ),
    )

    /**
     * What the most recent value stands for: the period it covers, or the kind of record it is.
     */
    private fun headerLabel(result: HeartScoreResult): Int = when (result.component) {
        HeartScoreComponent.ExerciseMinutes -> MHCStrings.heart_detail_header_last_7_days
        HeartScoreComponent.StepCount -> MHCStrings.heart_detail_daily_steps
        HeartScoreComponent.Sleep -> if (result.measurement != null) {
            MHCStrings.heart_detail_most_recent_night
        } else {
            MHCStrings.heart_detail_last_night
        }
        HeartScoreComponent.Diet -> MHCStrings.heart_detail_most_recent_score
        HeartScoreComponent.NicotineExposure, HeartScoreComponent.MentalWellbeing -> MHCStrings.heart_detail_most_recent_response
        HeartScoreComponent.BloodPressure,
        HeartScoreComponent.BloodLipids,
        HeartScoreComponent.BloodGlucose,
        HeartScoreComponent.BodyMassIndex,
        -> MHCStrings.heart_detail_most_recent_sample
    }

    private fun value(result: HeartScoreResult): GaugeHeader.Value {
        val parts = when (val measurement = result.measurement) {
            null -> emptyList()
            is HeartScoreMeasurement.BloodPressure -> listOf(
                part(
                    value = StringResource(
                        MHCStrings.heart_detail_pressure_value,
                        number(value = measurement.systolic),
                        number(value = measurement.diastolic),
                    ),
                    unit = PRESSURE_UNIT,
                ),
            )
            is HeartScoreMeasurement.Quantity -> quantity(result = result, value = measurement.value)
        }
        return if (parts.isEmpty()) {
            GaugeHeader.Value.Missing(text = StringResource(MHCStrings.heart_detail_no_data))
        } else {
            GaugeHeader.Value.Measured(parts = parts)
        }
    }

    private fun quantity(result: HeartScoreResult, value: Double): List<GaugeHeader.Part> = when (result.component) {
        HeartScoreComponent.Sleep -> {
            val minutes = (value * MINUTES_PER_HOUR).toInt()
            listOfNotNull(
                (minutes / MINUTES_PER_HOUR).takeIf { it > 0 }?.let {
                    part(value = StringResource(number(value = it.toDouble())), unit = HOUR_UNIT)
                },
                (minutes % MINUTES_PER_HOUR).takeIf { it > 0 }?.let {
                    part(value = StringResource(number(value = it.toDouble())), unit = MINUTE_UNIT)
                },
            )
        }
        HeartScoreComponent.NicotineExposure -> listOf(
            part(
                value = StringResource(NicotineExposureCategory.of(value = value)?.title ?: MHCStrings.heart_detail_no_data),
                unit = null,
            ),
        )
        else -> listOf(part(value = StringResource(number(value = value)), unit = result.component.displayUnit))
    }

    private fun part(value: StringResource, unit: String?) = GaugeHeader.Part(
        text = value,
        unit = unit?.let { StringResource(it) },
    )

    /**
     * The explanation's `link…` front-matter entries in key order, the first labelled "Learn More";
     * entries that are not web addresses are left out.
     */
    private fun links(document: MarkdownDocument, actions: ActionSink<HeartScoreDetailAction>): List<ExternalLink> =
        document.metadata.values
            .filterKeys { it.startsWith(LINK_PREFIX) }
            .toSortedMap()
            .values
            .mapNotNull { value -> runCatching { URI(value) }.getOrNull()?.takeIf { it.scheme in WEB_SCHEMES && it.host != null } }
            .mapIndexed { index, uri ->
                val host = uri.host.removePrefix(WWW_PREFIX)
                ExternalLink(
                    text = if (index == 0) StringResource(MHCStrings.heart_detail_learn_more_host, host) else StringResource(host),
                    onClicked = { actions.push(HeartScoreDetailAction.LinkSelected(url = uri.toString())) },
                )
            }

    private fun number(value: Double): String = NumberFormat.getNumberInstance().apply {
        maximumFractionDigits = MAX_FRACTION_DIGITS
    }.format(value)

    private fun section(title: StringResource?, content: ComposableContent) = DashboardSection(
        title = title,
        content = content,
        footnote = null,
    )

    private companion object {
        /**
         * The points a full score is shown as.
         */
        const val SCORE_SCALE = 100

        /**
         * The thickness of the header's gauge, relative to Grove's default gauge thickness.
         */
        const val GAUGE_LINE_WIDTH = 1.5f

        const val MAX_FRACTION_DIGITS = 2
        const val MINUTES_PER_HOUR = 60
        const val PRESSURE_UNIT = "mmHg"
        const val HOUR_UNIT = "hr"
        const val MINUTE_UNIT = "min"
        const val LINK_PREFIX = "link"
        const val WWW_PREFIX = "www."
        val WEB_SCHEMES = setOf("https", "http")
    }
}
