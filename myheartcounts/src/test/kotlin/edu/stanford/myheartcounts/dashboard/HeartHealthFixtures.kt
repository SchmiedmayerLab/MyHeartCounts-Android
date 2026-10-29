//
// This source file is part of the My Heart Counts Android open-source project
//
// SPDX-FileCopyrightText: 2026 Stanford University and the project authors (see CONTRIBUTORS.md)
//
// SPDX-License-Identifier: MIT

package edu.stanford.myheartcounts.dashboard

import java.time.Instant

/**
 * Heart health scores for tests: every shown factor, with [scored] factors scored at [measuredAt].
 */
object HeartHealthFixtures {

    /**
     * The factors shown when exercise minutes are available.
     */
    val SHOWN_COMPONENTS = HeartScoreComponent.entries - HeartScoreComponent.StepCount

    /**
     * Scores with [overall] and the factors in [scored] scored [score] at [measuredAt]; the others unscored.
     */
    fun scores(
        overall: Double?,
        scored: Set<HeartScoreComponent>,
        score: Double,
        measuredAt: Instant,
    ): HeartHealthScores = HeartHealthScores(
        overall = overall,
        components = SHOWN_COMPONENTS.map { component ->
            val isScored = component in scored
            HeartScoreResult(
                component = component,
                score = if (isScored) score else null,
                measuredAt = if (isScored) measuredAt else null,
                measurement = null,
                usesAsianBodyMassIndex = false,
            )
        },
    )
}
