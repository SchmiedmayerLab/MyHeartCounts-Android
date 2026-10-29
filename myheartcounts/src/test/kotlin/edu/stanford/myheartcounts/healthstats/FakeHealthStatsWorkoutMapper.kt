//
// This source file is part of the My Heart Counts Android open-source project
//
// SPDX-FileCopyrightText: 2026 Stanford University and the project authors (see CONTRIBUTORS.md)
//
// SPDX-License-Identifier: MIT

package edu.stanford.myheartcounts.healthstats

import androidx.health.connect.client.records.ExerciseSessionRecord

class FakeHealthStatsWorkoutMapper : HealthStatsWorkoutMapper {
    override fun map(record: ExerciseSessionRecord): HealthStatsEntry.Workout = HealthStatsEntry.Workout(
        id = "workout",
        date = record.startTime,
        endDate = record.endTime,
        durationSeconds = 0.0,
        activityType = 3000,
    )
}
