//
// This source file is part of the My Heart Counts Android open-source project
//
// SPDX-FileCopyrightText: 2026 Stanford University and the project authors (see CONTRIBUTORS.md)
//
// SPDX-License-Identifier: MIT

package edu.stanford.myheartcounts.healthstats

import org.grovealliance.scheduler.InstantRange
import java.time.Duration
import java.time.Instant

/**
 * A quantity's value over [timeRange], or at its start when the range is empty.
 *
 * @property value the value, in the unit of the [HealthStatsMetric] it was requested for
 */
data class HealthStatsQuantitySample(
    val timeRange: InstantRange,
    val value: Double,
)

/**
 * One sleep session.
 *
 * @property hoursAsleep the time spent asleep during the session, in hours
 */
data class SleepSessionStatsSample(
    val timeRange: InstantRange,
    val hoursAsleep: Double,
)

/**
 * One blood pressure reading, in millimeters of mercury.
 */
data class BloodPressureStatsSample(
    val date: Instant,
    val systolic: Double,
    val diastolic: Double,
)

/**
 * One workout.
 *
 * @property duration the time of active exercise, pauses excluded
 * @property activityType the code of the kind of workout
 */
data class WorkoutStatsSample(
    val id: String,
    val date: Instant,
    val endDate: Instant,
    val duration: Duration,
    val activityType: Long,
)

/**
 * One completed electrocardiogram recording.
 */
data class ElectrocardiogramStatsSample(
    val id: String,
    val date: Instant,
    val endDate: Instant,
)

/**
 * The result of a [HealthStatsRequest], with what is needed to judge whether it is complete.
 *
 * @property elements the values, in chronological order
 * @property diagnostics remarks on left-out, approximated or source-selected data
 * @property contributingSourceIds the sources whose data the elements hold
 * @property isFromCache whether the documents came from the local cache rather than the server
 * @property hasPendingWrites whether the documents hold local changes not yet on the server
 */
data class HealthStatsSnapshot<T>(
    val elements: List<T>,
    val diagnostics: List<HealthStatsDiagnostic>,
    val contributingSourceIds: Set<String>,
    val isFromCache: Boolean,
    val hasPendingWrites: Boolean,
)
