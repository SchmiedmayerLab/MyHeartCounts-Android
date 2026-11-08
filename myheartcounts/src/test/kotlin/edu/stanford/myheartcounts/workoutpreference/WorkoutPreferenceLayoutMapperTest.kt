//
// This source file is part of the My Heart Counts Android open-source project
//
// SPDX-FileCopyrightText: 2026 Stanford University and the project authors (see CONTRIBUTORS.md)
//
// SPDX-License-Identifier: MIT

package edu.stanford.myheartcounts.workoutpreference

import com.google.common.truth.Truth.assertThat
import edu.stanford.myheartcounts.MHCStrings
import edu.stanford.myheartcounts.model.workout.NotificationTime
import edu.stanford.myheartcounts.model.workout.WorkoutType
import edu.stanford.myheartcounts.ui.AppBarTextAction
import edu.stanford.myheartcounts.workoutpreference.ui.NotificationTimePicker
import edu.stanford.myheartcounts.workoutpreference.ui.WorkoutPreferenceScreenMode
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import org.grovealliance.ui.ActionSource
import org.grovealliance.ui.GroveAppBarWidgets
import org.grovealliance.ui.StringResource
import org.junit.Test
import java.util.Locale

@OptIn(ExperimentalCoroutinesApi::class)
class WorkoutPreferenceLayoutMapperTest {

    private val mapper = WorkoutPreferenceLayoutMapperImpl()
    private val pushedActions = mutableListOf<WorkoutPreferenceAction>()
    private var confirmations = 0
    private val actionSink = ActionSource<WorkoutPreferenceAction> { pushedActions.add(it) }.sink<WorkoutPreferenceAction>()
    private val preference = MutableStateFlow(WorkoutPreferenceFixtures.create())
    private val isTimePickerOpen = MutableStateFlow(false)
    private val hasChanges = MutableStateFlow(false)
    private val onConfirm: suspend () -> Unit = { confirmations++ }

    @Test
    fun `offers every workout type in order and checks the preferred ones`() = runTest {
        // given
        preference.value = WorkoutPreferenceFixtures.create(workoutTypes = setOf(WorkoutType.RUN, WorkoutType.OTHER))

        // when
        val section = mapper.map(input = input(scope = backgroundScope)).workoutTypes.value

        // then
        assertThat(section.header).isEqualTo(StringResource(MHCStrings.workout_preference_types_header))
        assertThat(section.options.map { it.title }).containsExactly(
            StringResource(MHCStrings.workout_preference_type_walk),
            StringResource(MHCStrings.workout_preference_type_run),
            StringResource(MHCStrings.workout_preference_type_bicycle),
            StringResource(MHCStrings.workout_preference_type_swim),
            StringResource(MHCStrings.workout_preference_type_strength),
            StringResource(MHCStrings.workout_preference_type_hiit),
            StringResource(MHCStrings.workout_preference_type_yoga_pilates),
            StringResource(MHCStrings.workout_preference_type_sport),
            StringResource(MHCStrings.workout_preference_type_other),
        ).inOrder()
        assertThat(section.options.filter { it.isSelected }.map { it.title }).containsExactly(
            StringResource(MHCStrings.workout_preference_type_run),
            StringResource(MHCStrings.workout_preference_type_other),
        )
    }

    @Test
    fun `tapping a workout type toggles it`() = runTest {
        // given
        val section = mapper.map(input = input(scope = backgroundScope)).workoutTypes.value

        // when
        section.options[WorkoutType.SWIM.ordinal].onClick()

        // then
        assertThat(pushedActions).containsExactly(WorkoutPreferenceAction.TypeToggled(type = WorkoutType.SWIM))
    }

    @Test
    fun `the selection follows the edited preference`() = runTest {
        // given
        val layout = mapper.map(input = input(scope = backgroundScope))
        subscribe(flow = layout.workoutTypes)

        // when
        preference.value = WorkoutPreferenceFixtures.create(workoutTypes = setOf(WorkoutType.WALK))
        runCurrent()

        // then
        assertThat(layout.workoutTypes.value.options.filter { it.isSelected }.map { it.title })
            .containsExactly(StringResource(MHCStrings.workout_preference_type_walk))
    }

    @Test
    fun `the notification time badge shows the time and opens the picker`() = runTest {
        // given
        val previous = Locale.getDefault()
        Locale.setDefault(Locale.UK)
        val badge = try {
            mapper.map(input = input(scope = backgroundScope)).notificationTime.badge.value
        } finally {
            Locale.setDefault(previous)
        }

        // when
        badge.onClick()

        // then
        assertThat(badge.text).isEqualTo(StringResource("09:00"))
        assertThat(pushedActions).containsExactly(WorkoutPreferenceAction.TimePickerRequested)
    }

    @Test
    fun `the picker opens on the chosen time and reports the selection`() = runTest {
        // given
        preference.value = WorkoutPreferenceFixtures.create(
            notificationTime = NotificationTime(
                hour = 18,
                minute = 45,
            ),
        )
        val layout = mapper.map(input = input(scope = backgroundScope))
        subscribe(flow = layout.timePicker)

        // when
        isTimePickerOpen.value = true
        runCurrent()
        val picker = requireNotNull(layout.timePicker.value)
        picker.onSelect(
            NotificationTimePicker.State(
                hour = 7,
                minute = 30,
            ),
        )
        picker.onDismiss()

        // then
        assertThat(picker.selection).isEqualTo(
            NotificationTimePicker.State(
                hour = 18,
                minute = 45,
            ),
        )
        assertThat(pushedActions).containsExactly(
            WorkoutPreferenceAction.TimeSelected(
                time = NotificationTime(
                    hour = 7,
                    minute = 30,
                ),
            ),
            WorkoutPreferenceAction.TimePickerDismissed,
        ).inOrder()
    }

    @Test
    fun `the picker is closed until requested`() = runTest {
        // when
        val layout = mapper.map(input = input(scope = backgroundScope))
        runCurrent()

        // then
        assertThat(layout.timePicker.value).isNull()
    }

    @Test
    fun `the button confirms the preference`() = runTest {
        // given
        val layout = mapper.map(input = input(scope = backgroundScope))

        // when
        requireNotNull(layout.continueButton).action()

        // then
        assertThat(confirmations).isEqualTo(1)
    }

    @Test
    fun `a standalone app bar saves while there are changes`() = runTest {
        // when
        val appBar = mapper.mapAppBar(
            input = input(
                scope = backgroundScope,
                mode = WorkoutPreferenceScreenMode.Standalone,
            ),
        )

        // then
        assertThat(appBar.actions).containsExactly(
            GroveAppBarWidgets.action(
                content = AppBarTextAction(
                    title = StringResource(MHCStrings.workout_preference_save),
                    enabled = hasChanges,
                    action = onConfirm,
                ),
            ),
        )
    }

    @Test
    fun `a step app bar has no action`() = runTest {
        // when
        val appBar = mapper.mapAppBar(input = input(scope = backgroundScope))

        // then
        assertThat(appBar.actions.orEmpty()).isEmpty()
    }

    @Test
    fun `only a step ends with Continue`() = runTest {
        // when
        val step = mapper.map(input = input(scope = backgroundScope)).continueButton
        val standalone = mapper.map(
            input = input(
                scope = backgroundScope,
                mode = WorkoutPreferenceScreenMode.Standalone,
            ),
        ).continueButton

        // then
        assertThat(step?.title).isEqualTo(StringResource(MHCStrings.workout_preference_continue))
        assertThat(standalone).isNull()
    }

    private fun TestScope.subscribe(flow: Flow<*>) {
        backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) { flow.collect {} }
    }

    private fun input(
        scope: CoroutineScope,
        mode: WorkoutPreferenceScreenMode = WorkoutPreferenceScreenMode.Step,
    ) = WorkoutPreferenceLayoutInput(
        mode = mode,
        preference = preference,
        isTimePickerOpen = isTimePickerOpen,
        hasChanges = hasChanges,
        scope = scope,
        actionSink = actionSink,
        onConfirm = onConfirm,
        onDismiss = {},
    )
}
