//
// This source file is part of the My Heart Counts Android open-source project
//
// SPDX-FileCopyrightText: 2026 Stanford University and the project authors (see CONTRIBUTORS.md)
//
// SPDX-License-Identifier: MIT

package edu.stanford.myheartcounts.workoutpreference.ui

import androidx.activity.compose.BackHandler
import androidx.compose.runtime.Composable
import edu.stanford.myheartcounts.workoutpreference.WorkoutPreferenceEvent
import edu.stanford.myheartcounts.workoutpreference.WorkoutPreferenceViewModel
import org.grovealliance.core.viewmodel.groveViewModel
import org.grovealliance.ui.ConsumeEvents

/**
 * The participant's workout preference, presented as [mode] describes. [onDismiss] is called when
 * the participant goes back without saving, [onSaved] once the preference is stored.
 */
@Composable
fun WorkoutPreferenceScreen(
    mode: WorkoutPreferenceScreenMode,
    onDismiss: () -> Unit,
    onSaved: () -> Unit,
) {
    val viewModel = groveViewModel<WorkoutPreferenceViewModel> {
        putString(WorkoutPreferenceViewModel.KEY_MODE, mode.name)
    }
    BackHandler(onBack = onDismiss)
    ConsumeEvents(viewModel.events) {
        when (it) {
            WorkoutPreferenceEvent.Dismissed -> onDismiss()
            WorkoutPreferenceEvent.Saved -> onSaved()
        }
    }
    viewModel.screen.Content()
}

/**
 * How the [WorkoutPreferenceScreen] is presented and confirmed.
 */
enum class WorkoutPreferenceScreenMode {
    /**
     * One step of a sequence, confirmed with Continue at the end of the form.
     */
    Step,

    /**
     * On its own, saved with the Save action of the app bar.
     */
    Standalone,
}
