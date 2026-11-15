//
// This source file is part of the My Heart Counts Android open-source project
//
// SPDX-FileCopyrightText: 2026 Stanford University and the project authors (see CONTRIBUTORS.md)
//
// SPDX-License-Identifier: MIT

package edu.stanford.myheartcounts.di

import edu.stanford.myheartcounts.workoutpreference.WorkoutPreferenceEditor
import edu.stanford.myheartcounts.workoutpreference.WorkoutPreferenceEditorImpl
import edu.stanford.myheartcounts.workoutpreference.WorkoutPreferenceLayoutMapper
import edu.stanford.myheartcounts.workoutpreference.WorkoutPreferenceLayoutMapperImpl
import org.grovealliance.core.ConfigurationBuilder

/**
 * Registers the participant's workout preference: its editing and storage, and its presentation.
 */
fun ConfigurationBuilder.workoutPreferenceConfigurations() {
    singleton<WorkoutPreferenceEditor> {
        WorkoutPreferenceEditorImpl(
            account = dependency(),
            accountService = dependency(),
        )
    }
    singleton<WorkoutPreferenceLayoutMapper> { WorkoutPreferenceLayoutMapperImpl() }
}
