//
// This source file is part of the My Heart Counts Android open-source project
//
// SPDX-FileCopyrightText: 2026 Stanford University and the project authors (see CONTRIBUTORS.md)
//
// SPDX-License-Identifier: MIT

package edu.stanford.myheartcounts.healthstats

import edu.stanford.myheartcounts.firebase.FirebaseSchema
import org.grovealliance.scheduler.InstantRange

/**
 * A query for a participant's health statistics, yielding elements of type [T].
 *
 * Requests are plain values: equal requests read the same documents and produce the same result.
 */
sealed interface HealthStatsRequest<T> {

    /**
     * The identifier of the metric whose documents are read.
     */
    val metricId: String

    /**
     * The time the result covers; documents of the months around it are read.
     */
    val timeRange: InstantRange

    /**
     * Which sources take part in the result.
     */
    val sourcePolicy: HealthStatsSourcePolicy

    /**
     * A quantity's values, reduced by [aggregation] and optionally grouped into [interval]s.
     */
    data class Quantity(
        val metric: HealthStatsMetric,
        override val timeRange: InstantRange,
        val aggregation: HealthStatsAggregation,
        override val sourcePolicy: HealthStatsSourcePolicy = HealthStatsSourcePolicy.Automatic,
        val interval: HealthStatsAggregationInterval? = null,
    ) : HealthStatsRequest<HealthStatsQuantitySample> {
        override val metricId: String
            get() = metric.id
    }

    /**
     * The sleep sessions overlapping the time range, ordered by their end.
     */
    data class SleepSessions(
        override val timeRange: InstantRange,
        override val sourcePolicy: HealthStatsSourcePolicy = HealthStatsSourcePolicy.Automatic,
    ) : HealthStatsRequest<SleepSessionStatsSample> {
        override val metricId: String
            get() = FirebaseSchema.StatsMetric.SLEEP
    }

    /**
     * The blood pressure readings taken in the time range.
     */
    data class BloodPressure(
        override val timeRange: InstantRange,
        override val sourcePolicy: HealthStatsSourcePolicy = HealthStatsSourcePolicy.Automatic,
    ) : HealthStatsRequest<BloodPressureStatsSample> {
        override val metricId: String
            get() = FirebaseSchema.StatsMetric.BLOOD_PRESSURE
    }

    /**
     * The workouts that started and ended in the time range.
     */
    data class Workouts(
        override val timeRange: InstantRange,
        override val sourcePolicy: HealthStatsSourcePolicy = HealthStatsSourcePolicy.Automatic,
    ) : HealthStatsRequest<WorkoutStatsSample> {
        override val metricId: String
            get() = FirebaseSchema.StatsMetric.WORKOUTS
    }

    /**
     * The electrocardiogram recordings that started and ended in the time range.
     */
    data class Electrocardiograms(
        override val timeRange: InstantRange,
        override val sourcePolicy: HealthStatsSourcePolicy = HealthStatsSourcePolicy.Automatic,
    ) : HealthStatsRequest<ElectrocardiogramStatsSample> {
        override val metricId: String
            get() = FirebaseSchema.StatsMetric.ELECTROCARDIOGRAMS
    }
}
