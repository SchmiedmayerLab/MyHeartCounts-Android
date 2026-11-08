//
// This source file is part of the My Heart Counts Android open-source project
//
// SPDX-FileCopyrightText: 2026 Stanford University and the project authors (see CONTRIBUTORS.md)
//
// SPDX-License-Identifier: MIT

package edu.stanford.myheartcounts.workoutpreference

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import edu.stanford.myheartcounts.MHCStrings
import edu.stanford.myheartcounts.workoutpreference.ui.WorkoutPreferenceScreenContent
import edu.stanford.myheartcounts.workoutpreference.ui.WorkoutPreferenceScreenMode
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import org.grovealliance.core.logging.groveLogger
import org.grovealliance.ui.ActionSource
import org.grovealliance.ui.EventSink
import org.grovealliance.ui.EventSourceFlow
import org.grovealliance.ui.StringResource
import org.grovealliance.ui.mutableScaffoldState
import org.grovealliance.ui.showErrorToast

/**
 * Coordinates the workout preference screen in the [WorkoutPreferenceScreenMode] passed in
 * [savedStateHandle] under [KEY_MODE]: edits the participant's preference through the [editor],
 * starting over from the stored one each time the screen is displayed, and stores it when confirmed
 * with Continue or, when standalone, the app bar's Save action, which is enabled only once the
 * preference differs from the one the screen was displayed with.
 * Leaving and storing are reported as [events].
 */
class WorkoutPreferenceViewModel(
    savedStateHandle: SavedStateHandle,
    private val editor: WorkoutPreferenceEditor,
    mapper: WorkoutPreferenceLayoutMapper,
) : ViewModel() {

    private val logger by groveLogger()
    private val actionSink = ActionSource(::onAction).sink<WorkoutPreferenceAction>()
    private val eventSink = EventSink<WorkoutPreferenceEvent>()
    private val mode = WorkoutPreferenceScreenMode.valueOf(requireNotNull(savedStateHandle.get<String>(KEY_MODE)))
    private val isTimePickerOpen = MutableStateFlow(false)
    private val openedPreference = MutableStateFlow(editor.state.value)
    private val input = WorkoutPreferenceLayoutInput(
        mode = mode,
        preference = editor.state,
        isTimePickerOpen = isTimePickerOpen.asStateFlow(),
        hasChanges = combine(editor.state, openedPreference) { edited, opened -> edited != opened }.stateIn(
            scope = viewModelScope,
            started = SharingStarted.Eagerly,
            initialValue = false,
        ),
        scope = viewModelScope,
        actionSink = actionSink,
        onConfirm = ::save,
        onDismiss = { eventSink.push(WorkoutPreferenceEvent.Dismissed) },
    )
    private val scaffoldState = mutableScaffoldState(appBar = mapper.mapAppBar(input = input))

    /**
     * Reports leaving the screen and storing the preference.
     */
    val events: EventSourceFlow<WorkoutPreferenceEvent> = eventSink.source()

    /**
     * The screen: the preference form beneath its app bar.
     */
    val screen = WorkoutPreferenceScreenContent(
        scaffoldState = scaffoldState.asScaffoldState(),
        layout = mapper.map(input = input),
        onOpened = { actionSink.push(WorkoutPreferenceAction.Opened) },
    )

    private fun onAction(action: WorkoutPreferenceAction) {
        when (action) {
            WorkoutPreferenceAction.Opened -> {
                editor.load()
                openedPreference.value = editor.state.value
            }
            is WorkoutPreferenceAction.TypeToggled -> editor.toggle(type = action.type)
            WorkoutPreferenceAction.TimePickerRequested -> isTimePickerOpen.value = true
            is WorkoutPreferenceAction.TimeSelected -> {
                editor.select(time = action.time)
                isTimePickerOpen.value = false
            }
            WorkoutPreferenceAction.TimePickerDismissed -> isTimePickerOpen.value = false
        }
    }

    private suspend fun save() {
        viewModelScope.launch {
            editor.save()
                .onSuccess { eventSink.push(WorkoutPreferenceEvent.Saved) }
                .onFailure {
                    logger.e(it) { "Storing the workout preference failed." }
                    scaffoldState.showErrorToast(message = StringResource(MHCStrings.workout_preference_save_failed))
                }
        }.join()
    }

    companion object {
        const val KEY_MODE = "mode"
    }
}
