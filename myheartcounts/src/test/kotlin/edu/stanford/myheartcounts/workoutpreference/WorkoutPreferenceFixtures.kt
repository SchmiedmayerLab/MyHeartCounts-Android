//
// This source file is part of the My Heart Counts Android open-source project
//
// SPDX-FileCopyrightText: 2026 Stanford University and the project authors (see CONTRIBUTORS.md)
//
// SPDX-License-Identifier: MIT

package edu.stanford.myheartcounts.workoutpreference

import edu.stanford.myheartcounts.model.workout.NotificationTime
import edu.stanford.myheartcounts.model.workout.WorkoutType

/**
 * Builds [WorkoutPreference]s; by default no workout type is preferred and notifications come at
 * 09:00.
 */
object WorkoutPreferenceFixtures {
    fun create(
        workoutTypes: Set<WorkoutType> = emptySet(),
        notificationTime: NotificationTime = NotificationTime(
            hour = 9,
            minute = 0,
        ),
    ) = WorkoutPreference(
        workoutTypes = workoutTypes,
        notificationTime = notificationTime,
    )
}
