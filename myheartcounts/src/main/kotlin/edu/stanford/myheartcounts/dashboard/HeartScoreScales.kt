//
// This source file is part of the My Heart Counts Android open-source project
//
// SPDX-FileCopyrightText: 2026 Stanford University and the project authors (see CONTRIBUTORS.md)
//
// SPDX-License-Identifier: MIT

@file:Suppress("detekt:MagicNumber")

package edu.stanford.myheartcounts.dashboard

import androidx.annotation.StringRes
import edu.stanford.myheartcounts.MHCStrings

/**
 * A score for a measured value, rising or falling in steps between fixed cut-offs.
 *
 * A value below the first of [bounds] falls into band 0, one at or above the last into the last
 * band; band `i` in between holds the values from `bounds[i - 1]` up to `bounds[i]`.
 *
 * @property bounds the cut-offs between the bands, ascending
 * @property scores the score of each band, one more than there are [bounds]
 */
enum class HeartScoreScale(
    val bounds: List<Double>,
    val scores: List<Double>,
) {
    ExerciseMinutes(
        bounds = listOf(1.0, 30.0, 60.0, 90.0, 120.0, 150.0),
        scores = listOf(0.0, 0.2, 0.4, 0.6, 0.8, 0.9, 1.0),
    ),
    StepCount(
        bounds = listOf(0.0, 2_000.0, 4_000.0, 6_000.0, 8_000.0, 10_000.0),
        scores = listOf(0.0, 0.2, 0.4, 0.6, 0.8, 0.9, 1.0),
    ),
    Sleep(
        bounds = listOf(4.0, 5.0, 6.0, 7.0, 9.0, 10.0),
        scores = listOf(0.0, 0.2, 0.4, 0.7, 1.0, 0.9, 0.4),
    ),
    BodyMassIndex(
        bounds = listOf(16.0, 18.5, 25.0, 30.0, 35.0, 40.0),
        scores = listOf(0.4, 0.6, 1.0, 0.7, 0.5, 0.3, 0.1),
    ),

    /**
     * The body mass index cut-offs for people of Asian descent.
     */
    AsianBodyMassIndex(
        bounds = listOf(16.0, 18.5, 23.0, 25.0, 30.0),
        scores = listOf(0.4, 0.6, 1.0, 0.75, 0.5, 0.2),
    ),

    /**
     * LDL cholesterol in mg/dL.
     */
    BloodLipids(
        bounds = listOf(55.0, 70.0, 100.0, 130.0, 160.0, 190.0),
        scores = listOf(1.0, 0.9, 0.8, 0.6, 0.4, 0.2, 0.0),
    ),

    /**
     * Fasting blood glucose in mg/dL.
     */
    BloodGlucose(
        bounds = listOf(85.0, 100.0, 110.0, 126.0, 140.0),
        scores = listOf(1.0, 0.9, 0.75, 0.5, 0.25, 0.0),
    ),

    /**
     * The systolic part of a blood pressure score, worth up to 0.75 of it.
     */
    SystolicPressure(
        bounds = listOf(121.0, 130.0, 140.0, 160.0),
        scores = listOf(0.75, 0.65, 0.5, 0.25, 0.0),
    ),

    /**
     * The diastolic part of a blood pressure score, worth up to 0.25 of it.
     */
    DiastolicPressure(
        bounds = listOf(81.0, 90.0, 100.0),
        scores = listOf(0.25, 0.15, 0.05, 0.0),
    ),
    ;

    /**
     * The band [value] falls into.
     */
    fun band(value: Double): Int = bounds.indexOfFirst { value < it }.takeIf { it >= 0 } ?: bounds.size

    /**
     * The score of the band [value] falls into.
     */
    fun score(value: Double): Double = scores[band(value)]
}

/**
 * The bands of the diet score; a value in none of them scores 0.
 *
 * @property range the diet scores the band holds
 */
enum class DietScoreBand(
    val range: ClosedFloatingPointRange<Double>,
    val score: Double,
) {
    Excellent(range = 17.0..21.0, score = 1.0),
    VeryGood(range = 14.0..16.0, score = 0.85),
    Good(range = 11.0..13.0, score = 0.7),
    Fair(range = 8.0..10.0, score = 0.5),
    Poor(range = 5.0..7.0, score = 0.25),
    ;

    companion object {
        /**
         * The diet scores below every band, which score 0.
         */
        const val LOWEST_BOUND = 5.0

        /**
         * The band holding [value], or `null` when no band does.
         */
        fun of(value: Double): DietScoreBand? = entries.firstOrNull { value in it.range }
    }
}

/**
 * The participant's exposure to nicotine, from never to actively smoking.
 *
 * @property title the category as shown to the participant
 */
enum class NicotineExposureCategory(
    @StringRes val title: Int,
    val score: Double,
) {
    NeverSmoked(title = MHCStrings.heart_detail_never_smoked, score = 1.0),
    QuitMoreThanFiveYearsAgo(title = MHCStrings.heart_detail_quit_over_5_years, score = 0.75),
    QuitOneToFiveYearsAgo(title = MHCStrings.heart_detail_quit_1_to_5_years, score = 0.5),
    QuitWithinLastYearOrUsingNicotineDelivery(title = MHCStrings.heart_detail_recent_quit, score = 0.25),
    ActivelySmoking(title = MHCStrings.heart_detail_smoking, score = 0.0),
    ;

    companion object {
        /**
         * The category a stored nicotine exposure value stands for, or `null` for an unknown one.
         */
        fun of(value: Double): NicotineExposureCategory? = entries.getOrNull(value.toInt())
    }
}
