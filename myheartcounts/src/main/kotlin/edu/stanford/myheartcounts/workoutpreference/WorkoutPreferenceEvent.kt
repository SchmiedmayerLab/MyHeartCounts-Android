//
// This source file is part of the My Heart Counts Android open-source project
//
// SPDX-FileCopyrightText: 2026 Stanford University and the project authors (see CONTRIBUTORS.md)
//
// SPDX-License-Identifier: MIT

package edu.stanford.myheartcounts.workoutpreference

/**
 * What the workout preference screen reports to whoever shows it.
 */
sealed interface WorkoutPreferenceEvent {

    /**
     * The participant left the screen without saving.
     */
    data object Dismissed : WorkoutPreferenceEvent

    /**
     * The preference was stored on the participant's account.
     */
    data object Saved : WorkoutPreferenceEvent
}
