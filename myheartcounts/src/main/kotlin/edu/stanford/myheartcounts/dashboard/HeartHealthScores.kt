//
// This source file is part of the My Heart Counts Android open-source project
//
// SPDX-FileCopyrightText: 2026 Stanford University and the project authors (see CONTRIBUTORS.md)
//
// SPDX-License-Identifier: MIT

package edu.stanford.myheartcounts.dashboard

import java.time.Instant

/**
 * The participant's heart health score and the factors it is made of.
 *
 * @param overall the overall score, from `0` to `1`; absent while too few factors are scored
 * @param components the scored factors, in the order they are shown
 */
data class HeartHealthScores(
    val overall: Double?,
    val components: List<HeartScoreResult>,
) {
    init {
        require(overall == null || overall in 0.0..1.0) { "overall must lie within 0 and 1, but was $overall" }
    }
}

/**
 * The score of one factor of the participant's heart health.
 *
 * @param component the factor scored
 * @param score the score, from `0` to `1`; absent while there is no data to score it from
 * @param measuredAt when the data the score is based on ends; absent without a score
 * @param measurement the value the score is based on; absent without a score
 * @param usesAsianBodyMassIndex whether a body mass index is scored by the cut-offs for people of Asian
 * descent
 */
data class HeartScoreResult(
    val component: HeartScoreComponent,
    val score: Double?,
    val measuredAt: Instant?,
    val measurement: HeartScoreMeasurement?,
    val usesAsianBodyMassIndex: Boolean,
) {
    init {
        require(score == null || score in 0.0..1.0) { "score must lie within 0 and 1, but was $score" }
    }
}
