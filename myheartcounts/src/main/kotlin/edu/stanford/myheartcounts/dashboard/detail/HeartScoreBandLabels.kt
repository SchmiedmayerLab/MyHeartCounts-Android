//
// This source file is part of the My Heart Counts Android open-source project
//
// SPDX-FileCopyrightText: 2026 Stanford University and the project authors (see CONTRIBUTORS.md)
//
// SPDX-License-Identifier: MIT

@file:Suppress("detekt:MagicNumber")

package edu.stanford.myheartcounts.dashboard.detail

import androidx.annotation.StringRes
import edu.stanford.myheartcounts.MHCStrings
import edu.stanford.myheartcounts.dashboard.HeartScoreScale
import org.grovealliance.ui.StringResource
import java.text.NumberFormat

/**
 * Names the ranges of values the bands of a score hold.
 */
interface HeartScoreBandLabels {

    /**
     * The bands of [scale] to show, best first, each with the label of the values it holds; bands
     * not listed are not shown.
     */
    fun labels(scale: HeartScoreScale): List<Pair<Int, StringResource>>

    /**
     * The values from [lower] up to and including [upper].
     */
    fun range(lower: Double, upper: Double): StringResource

    /**
     * The values below [bound].
     */
    fun below(bound: Double): StringResource
}

/**
 * Labels whole-number bands by their first and last value, and body mass index bands also by their
 * weight category.
 */
class HeartScoreBandLabelsImpl : HeartScoreBandLabels {

    override fun labels(scale: HeartScoreScale): List<Pair<Int, StringResource>> = when (scale) {
        HeartScoreScale.ExerciseMinutes -> exercise()
        HeartScoreScale.StepCount -> steps()
        HeartScoreScale.Sleep -> sleep()
        HeartScoreScale.BodyMassIndex -> bodyMassIndex()
        HeartScoreScale.AsianBodyMassIndex -> asianBodyMassIndex()
        HeartScoreScale.BloodLipids -> lipids()
        HeartScoreScale.BloodGlucose -> glucose()
        HeartScoreScale.SystolicPressure, HeartScoreScale.DiastolicPressure -> emptyList()
    }

    override fun range(lower: Double, upper: Double) = StringResource(
        MHCStrings.heart_detail_band_range,
        number(value = lower),
        number(value = upper),
    )

    override fun below(bound: Double) = at(bound = bound, format = MHCStrings.heart_detail_band_below)

    private fun exercise() = listOf(
        6 to at(bound = 150.0, format = MHCStrings.heart_detail_band_at_least_spaced),
        5 to range(lower = 120.0, upper = 149.0),
        4 to range(lower = 90.0, upper = 119.0),
        3 to range(lower = 60.0, upper = 89.0),
        2 to range(lower = 30.0, upper = 59.0),
        1 to range(lower = 1.0, upper = 29.0),
    )

    private fun steps() = listOf(
        6 to at(bound = 10_000.0, format = MHCStrings.heart_detail_band_at_least_spaced),
        5 to range(lower = 8_000.0, upper = 9_999.0),
        4 to range(lower = 6_000.0, upper = 7_999.0),
        3 to range(lower = 4_000.0, upper = 5_999.0),
        2 to range(lower = 2_000.0, upper = 3_999.0),
        1 to below(bound = 2_000.0),
    )

    private fun sleep() = listOf(
        4 to hours(lower = 7.0, upper = 9.0),
        5 to hours(lower = 9.0, upper = 10.0),
        3 to hours(lower = 6.0, upper = 7.0),
        2 to hours(lower = 5.0, upper = 6.0),
        6 to at(bound = 10.0, format = MHCStrings.heart_detail_sleep_band_at_least),
        1 to hours(lower = 4.0, upper = 5.0),
    )

    private fun bodyMassIndex() = listOf(
        0 to weight(range = below(bound = 16.0), category = MHCStrings.heart_detail_bmi_severely_underweight),
        1 to weight(range = range(lower = 16.0, upper = 18.4), category = MHCStrings.heart_detail_bmi_underweight),
        2 to weight(range = range(lower = 18.5, upper = 24.9), category = MHCStrings.heart_detail_bmi_normal),
        3 to weight(range = range(lower = 25.0, upper = 29.9), category = MHCStrings.heart_detail_bmi_overweight),
        4 to weight(range = range(lower = 30.0, upper = 34.9), category = MHCStrings.heart_detail_bmi_obesity_1),
        5 to weight(range = range(lower = 35.0, upper = 39.9), category = MHCStrings.heart_detail_bmi_obesity_2),
        6 to weight(
            range = at(bound = 40.0, format = MHCStrings.heart_detail_band_at_least),
            category = MHCStrings.heart_detail_bmi_obesity_3,
        ),
    )

    private fun asianBodyMassIndex() = listOf(
        0 to weight(range = below(bound = 16.0), category = MHCStrings.heart_detail_bmi_severely_underweight),
        1 to weight(range = range(lower = 16.0, upper = 18.4), category = MHCStrings.heart_detail_bmi_underweight),
        2 to weight(range = range(lower = 18.5, upper = 22.9), category = MHCStrings.heart_detail_bmi_normal),
        3 to weight(range = range(lower = 23.0, upper = 24.9), category = MHCStrings.heart_detail_bmi_at_risk),
        4 to weight(range = range(lower = 25.0, upper = 29.9), category = MHCStrings.heart_detail_bmi_obesity_1),
        5 to weight(
            range = at(bound = 30.0, format = MHCStrings.heart_detail_band_at_least),
            category = MHCStrings.heart_detail_bmi_obesity_2,
        ),
    )

    private fun lipids() = listOf(
        0 to below(bound = 55.0),
        1 to range(lower = 55.0, upper = 69.0),
        2 to range(lower = 70.0, upper = 99.0),
        3 to range(lower = 100.0, upper = 129.0),
        4 to range(lower = 130.0, upper = 159.0),
        5 to range(lower = 160.0, upper = 189.0),
        6 to at(bound = 190.0, format = MHCStrings.heart_detail_band_at_least),
    )

    private fun glucose() = listOf(
        0 to below(bound = 85.0),
        1 to range(lower = 85.0, upper = 99.0),
        2 to range(lower = 100.0, upper = 109.0),
        3 to range(lower = 110.0, upper = 125.0),
        4 to range(lower = 126.0, upper = 139.0),
        5 to at(bound = 140.0, format = MHCStrings.heart_detail_band_at_least),
    )

    private fun at(bound: Double, @StringRes format: Int) = StringResource(format, number(value = bound))

    private fun hours(lower: Double, upper: Double) = StringResource(
        MHCStrings.heart_detail_sleep_band_range,
        number(value = lower),
        number(value = upper),
    )

    private fun weight(range: StringResource, @StringRes category: Int) = StringResource(
        MHCStrings.heart_detail_bmi_band,
        range,
        StringResource(category),
    )

    private fun number(value: Double): String = NumberFormat.getNumberInstance().apply {
        maximumFractionDigits = 1
    }.format(value)
}
