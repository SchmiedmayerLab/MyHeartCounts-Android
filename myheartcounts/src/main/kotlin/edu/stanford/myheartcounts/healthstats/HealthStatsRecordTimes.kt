//
// This source file is part of the My Heart Counts Android open-source project
//
// SPDX-FileCopyrightText: 2026 Stanford University and the project authors (see CONTRIBUTORS.md)
//
// SPDX-License-Identifier: MIT

package edu.stanford.myheartcounts.healthstats

import androidx.health.connect.client.records.ActiveCaloriesBurnedRecord
import androidx.health.connect.client.records.BloodPressureRecord
import androidx.health.connect.client.records.DistanceRecord
import androidx.health.connect.client.records.ExerciseSessionRecord
import androidx.health.connect.client.records.FloorsClimbedRecord
import androidx.health.connect.client.records.HeartRateRecord
import androidx.health.connect.client.records.HeightRecord
import androidx.health.connect.client.records.Record
import androidx.health.connect.client.records.RestingHeartRateRecord
import androidx.health.connect.client.records.SleepSessionRecord
import androidx.health.connect.client.records.StepsRecord
import androidx.health.connect.client.records.WeightRecord
import org.grovealliance.scheduler.InstantRange
import java.time.Instant

internal fun Record.statsRange(): InstantRange? = when (this) {
    is StepsRecord -> interval(start = startTime, end = endTime)
    is ActiveCaloriesBurnedRecord -> interval(start = startTime, end = endTime)
    is DistanceRecord -> interval(start = startTime, end = endTime)
    is ExerciseSessionRecord -> interval(start = startTime, end = endTime)
    is FloorsClimbedRecord -> interval(start = startTime, end = endTime)
    is HeartRateRecord -> interval(start = startTime, end = endTime)
    is SleepSessionRecord -> interval(start = startTime, end = endTime)
    is BloodPressureRecord -> interval(start = time, end = time)
    is WeightRecord -> interval(start = time, end = time)
    is HeightRecord -> interval(start = time, end = time)
    is RestingHeartRateRecord -> interval(start = time, end = time)
    else -> null
}

private fun interval(start: Instant, end: Instant) = InstantRange(
    start = start,
    endExclusive = end,
)
