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
 * An interaction with the workout preference screen.
 */
sealed interface WorkoutPreferenceAction {

    /**
     * The screen was displayed.
     */
    data object Opened : WorkoutPreferenceAction

    /**
     * The participant tapped [type] to prefer it, or to stop preferring it.
     */
    data class TypeToggled(val type: WorkoutType) : WorkoutPreferenceAction

    /**
     * The participant asked to change the notification time.
     */
    data object TimePickerRequested : WorkoutPreferenceAction

    /**
     * The participant chose [time] as the notification time.
     */
    data class TimeSelected(val time: NotificationTime) : WorkoutPreferenceAction

    /**
     * The participant closed the time picker without choosing.
     */
    data object TimePickerDismissed : WorkoutPreferenceAction
}
