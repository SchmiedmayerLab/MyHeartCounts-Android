//
// This source file is part of the My Heart Counts Android open-source project
//
// SPDX-FileCopyrightText: 2026 Stanford University and the project authors (see CONTRIBUTORS.md)
//
// SPDX-License-Identifier: MIT

package edu.stanford.myheartcounts.dashboard

/**
 * The observation used to calculate a component score.
 */
sealed interface HeartScoreMeasurement {

    /**
     * A single value, in the unit the factor is shown in.
     */
    data class Quantity(val value: Double) : HeartScoreMeasurement

    /**
     * A blood pressure reading in mmHg.
     */
    data class BloodPressure(
        val systolic: Double,
        val diastolic: Double,
    ) : HeartScoreMeasurement
}
