//
// This source file is part of the My Heart Counts Android open-source project
//
// SPDX-FileCopyrightText: 2026 Stanford University and the project authors (see CONTRIBUTORS.md)
//
// SPDX-License-Identifier: MIT

package edu.stanford.myheartcounts.dashboard.detail

import androidx.compose.ui.graphics.lerp
import edu.stanford.myheartcounts.MHCStrings
import edu.stanford.myheartcounts.dashboard.DietScoreBand
import edu.stanford.myheartcounts.dashboard.HeartScoreComponent
import edu.stanford.myheartcounts.dashboard.HeartScoreMeasurement
import edu.stanford.myheartcounts.dashboard.HeartScoreResult
import edu.stanford.myheartcounts.dashboard.HeartScoreScale
import edu.stanford.myheartcounts.dashboard.NicotineExposureCategory
import edu.stanford.myheartcounts.dashboard.ui.ColorBandList
import edu.stanford.myheartcounts.dashboard.ui.DashboardColorPalette
import org.grovealliance.ui.StringResource
import org.grovealliance.ui.theme.ColorToken

/**
 * Explains how a factor's value turns into its score, as one colored band per range of values.
 */
interface HeartScoreBandMapper {

    /**
     * The bands of [result]'s factor, the one holding its value highlighted; `null` for a factor
     * whose score needs no explanation beyond its gauge.
     */
    fun map(result: HeartScoreResult): ColorBandList?
}

/**
 * Lists each factor's bands best first, labelled by [labels] with the values they hold and with their
 * score out of 100, and colored from red to green by that score. Blood pressure bands carry no score, as its
 * score combines its two readings, and none of them is highlighted.
 */
class HeartScoreBandMapperImpl(
    private val labels: HeartScoreBandLabels,
) : HeartScoreBandMapper {

    override fun map(result: HeartScoreResult): ColorBandList? {
        val value = (result.measurement as? HeartScoreMeasurement.Quantity)?.value
        val bands = when (result.component) {
            HeartScoreComponent.MentalWellbeing -> return null
            HeartScoreComponent.BloodPressure -> pressure()
            HeartScoreComponent.Diet -> diet(value = value)
            HeartScoreComponent.NicotineExposure -> nicotine(value = value)
            HeartScoreComponent.ExerciseMinutes -> scale(scale = HeartScoreScale.ExerciseMinutes, value = value)
            HeartScoreComponent.StepCount -> scale(scale = HeartScoreScale.StepCount, value = value)
            HeartScoreComponent.Sleep -> scale(scale = HeartScoreScale.Sleep, value = value)
            HeartScoreComponent.BloodLipids -> scale(scale = HeartScoreScale.BloodLipids, value = value)
            HeartScoreComponent.BloodGlucose -> scale(scale = HeartScoreScale.BloodGlucose, value = value)
            HeartScoreComponent.BodyMassIndex -> scale(
                scale = if (result.usesAsianBodyMassIndex) HeartScoreScale.AsianBodyMassIndex else HeartScoreScale.BodyMassIndex,
                value = value,
            )
        }
        return ColorBandList(bands = bands)
    }

    /**
     * The bands of [scale] in the order [labels] lists them.
     */
    private fun scale(scale: HeartScoreScale, value: Double?): List<ColorBandList.Band> {
        val matched = value?.let { scale.band(value = it) }
        return labels.labels(scale = scale).map { (index, label) ->
            band(
                label = label,
                score = scale.scores[index],
                selected = matched == index,
            )
        }
    }

    private fun diet(value: Double?): List<ColorBandList.Band> {
        val matched = value?.let { DietScoreBand.of(value = it) }
        return DietScoreBand.entries.map { band ->
            band(
                label = labels.range(lower = band.range.start, upper = band.range.endInclusive),
                score = band.score,
                selected = matched == band,
            )
        } + band(
            label = labels.below(bound = DietScoreBand.LOWEST_BOUND),
            score = 0.0,
            selected = value != null && value < DietScoreBand.LOWEST_BOUND,
        )
    }

    private fun nicotine(value: Double?): List<ColorBandList.Band> {
        val matched = value?.let { NicotineExposureCategory.of(value = it) }
        return NicotineExposureCategory.entries.map { category ->
            band(
                label = StringResource(category.title),
                score = category.score,
                selected = matched == category,
            )
        }
    }

    private fun pressure(): List<ColorBandList.Band> = listOf(
        MHCStrings.heart_detail_pressure_band_1 to PRESSURE_IDEAL,
        MHCStrings.heart_detail_pressure_band_2 to PRESSURE_ELEVATED,
        MHCStrings.heart_detail_pressure_band_3 to PRESSURE_STAGE_1,
        MHCStrings.heart_detail_pressure_band_4 to PRESSURE_STAGE_2,
    ).map { (label, score) ->
        ColorBandList.Band(
            leadingText = StringResource(label),
            trailingText = null,
            fillColors = listOf(color(score = score)),
            isHighlighted = false,
        )
    }

    private fun band(label: StringResource, score: Double, selected: Boolean) = ColorBandList.Band(
        leadingText = label,
        trailingText = StringResource((score * SCORE_SCALE).toInt().toString()),
        fillColors = listOf(color(score = score)),
        isHighlighted = selected,
    )

    /**
     * The color at [score] along the palette's red-to-green gradient.
     */
    private fun color(score: Double): ColorToken {
        val colors = DashboardColorPalette.redToGreen
        val position = score.coerceIn(0.0, 1.0) * colors.lastIndex
        val low = position.toInt()
        val high = (low + 1).coerceAtMost(colors.lastIndex)
        val amount = (position - low).toFloat()
        return ColorToken.Custom(
            light = lerp(colors[low].light, colors[high].light, amount),
            dark = lerp(colors[low].dark, colors[high].dark, amount),
        )
    }

    private companion object {
        /**
         * The points a full score is shown as.
         */
        const val SCORE_SCALE = 100

        /**
         * The positions of the blood pressure bands on the red-to-green gradient.
         */
        const val PRESSURE_IDEAL = 1.0
        const val PRESSURE_ELEVATED = 0.8
        const val PRESSURE_STAGE_1 = 0.5
        const val PRESSURE_STAGE_2 = 0.1
    }
}
