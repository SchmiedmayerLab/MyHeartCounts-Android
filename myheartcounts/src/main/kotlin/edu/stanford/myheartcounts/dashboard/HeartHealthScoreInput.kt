//
// This source file is part of the My Heart Counts Android open-source project
//
// SPDX-FileCopyrightText: 2026 Stanford University and the project authors (see CONTRIBUTORS.md)
//
// SPDX-License-Identifier: MIT

package edu.stanford.myheartcounts.dashboard

import edu.stanford.myheartcounts.customsamples.CustomQuantitySample
import edu.stanford.myheartcounts.customsamples.CustomQuantitySampleType
import edu.stanford.myheartcounts.healthstats.BloodPressureStatsSample
import edu.stanford.myheartcounts.healthstats.HealthStatsQuantitySample
import edu.stanford.myheartcounts.healthstats.HealthStatsSnapshot
import edu.stanford.myheartcounts.healthstats.SleepSessionStatsSample
import org.grovealliance.scheduler.InstantRange
import java.time.Instant

/**
 * The observations available to calculate the current heart health scores.
 */
data class HeartHealthScoreInput(
    val exerciseRange: InstantRange,
    val exercise: List<HealthStatsQuantitySample>,
    val steps: HealthStatsSnapshot<HealthStatsQuantitySample>?,
    val sleep: List<SleepSessionStatsSample>,
    val bodyMassIndex: List<HealthStatsQuantitySample>,
    val weight: List<HealthStatsQuantitySample>,
    val height: List<HealthStatsQuantitySample>,
    val bloodPressure: List<BloodPressureStatsSample>,
    val custom: Map<CustomQuantitySampleType, List<CustomQuantitySample>>,
    val glucose: Glucose?,
    val usesAsianBodyMassIndex: Boolean,
) {
    /**
     * A glucose reading in milligrams per deciliter.
     */
    data class Glucose(
        val value: Double,
        val measuredAt: Instant,
    )
}
