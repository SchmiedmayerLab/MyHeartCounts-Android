//
// This source file is part of the My Heart Counts Android open-source project
//
// SPDX-FileCopyrightText: 2026 Stanford University and the project authors (see CONTRIBUTORS.md)
//
// SPDX-License-Identifier: MIT

package edu.stanford.myheartcounts.workoutpreference

import androidx.lifecycle.SavedStateHandle
import com.google.common.truth.Truth.assertThat
import edu.stanford.myheartcounts.account.FakeAccountService
import edu.stanford.myheartcounts.account.PreferredWorkoutTypesKey
import edu.stanford.myheartcounts.model.workout.WorkoutType
import edu.stanford.myheartcounts.model.workout.WorkoutTypes
import edu.stanford.myheartcounts.workoutpreference.ui.WorkoutPreferenceLayout
import edu.stanford.myheartcounts.workoutpreference.ui.WorkoutPreferenceScreenMode
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.grovealliance.account.AccountDetails
import org.grovealliance.account.FakeAccount
import org.grovealliance.ui.consumeAll
import org.junit.After
import org.junit.Before
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class WorkoutPreferenceViewModelTest {

    private val account = FakeAccount()
    private val accountService = FakeAccountService()
    private val editor = WorkoutPreferenceEditorImpl(
        account = account,
        accountService = accountService,
    )
    private val mapper = RecordingWorkoutPreferenceMapper()

    @Before
    fun setup() {
        Dispatchers.setMain(UnconfinedTestDispatcher())
        account.supplyUserDetails(details = AccountDetails())
    }

    @After
    fun teardown() {
        Dispatchers.resetMain()
    }

    @Test
    fun `opening starts over from the stored preference`() {
        // given
        val viewModel = viewModel()
        editor.toggle(type = WorkoutType.RUN)
        val details = AccountDetails()
        details[PreferredWorkoutTypesKey::class] = WorkoutTypes(elements = setOf(WorkoutType.SWIM))
        account.supplyUserDetails(details = details)

        // when
        viewModel.screen.onOpened()

        // then
        assertThat(editor.state.value.workoutTypes).containsExactly(WorkoutType.SWIM)
    }

    @Test
    fun `tapping a workout type toggles it`() {
        // given
        val viewModel = viewModel()
        viewModel.screen.onOpened()

        // when
        viewModel.screen.layout.workoutTypes.value.options[WorkoutType.STRENGTH.ordinal].onClick()

        // then
        assertThat(editor.state.value.workoutTypes).containsExactly(WorkoutType.STRENGTH)
    }

    @Test
    fun `the time picker opens from the badge and closes on a selection`() = runTest {
        // given
        val viewModel = viewModel()
        viewModel.screen.onOpened()
        backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) { viewModel.screen.layout.timePicker.collect {} }

        // when
        viewModel.screen.layout.notificationTime.badge.value.onClick()
        val picker = requireNotNull(viewModel.screen.layout.timePicker.value)
        picker.onSelect(picker.selection.copy(hour = 20))

        // then
        assertThat(viewModel.screen.layout.timePicker.value).isNull()
        assertThat(editor.state.value.notificationTime.description).isEqualTo("20:00")
    }

    @Test
    fun `confirming stores the preference and reports it`() = runTest {
        // given
        val viewModel = viewModel()
        viewModel.screen.onOpened()
        viewModel.screen.layout.workoutTypes.value.options[WorkoutType.WALK.ordinal].onClick()

        // when
        requireNotNull(viewModel.screen.layout.continueButton).action()

        // then
        val stored = accountService.updates.single().modifiedDetails[PreferredWorkoutTypesKey::class]
        assertThat(stored?.elements).containsExactly(WorkoutType.WALK)
        assertThat(viewModel.events.consumeAll()).containsExactly(WorkoutPreferenceEvent.Saved)
    }

    @Test
    fun `a failed save reports nothing`() = runTest {
        // given
        accountService.setUpdateResult(result = Result.failure(IllegalStateException("offline")))
        val viewModel = viewModel()
        viewModel.screen.onOpened()

        // when
        requireNotNull(viewModel.screen.layout.continueButton).action()

        // then
        assertThat(viewModel.events.consumeAll()).isEmpty()
    }

    @Test
    fun `changes are tracked against the preference the screen opened with`() {
        // given
        val viewModel = viewModel(mode = WorkoutPreferenceScreenMode.Standalone)
        viewModel.screen.onOpened()
        val unchanged = mapper.input.hasChanges.value

        // when
        editor.toggle(type = WorkoutType.SPORT)
        val changed = mapper.input.hasChanges.value
        editor.toggle(type = WorkoutType.SPORT)

        // then
        assertThat(unchanged).isFalse()
        assertThat(changed).isTrue()
        assertThat(mapper.input.hasChanges.value).isFalse()
    }

    @Test
    fun `reopening takes the stored preference as the new starting point`() {
        // given
        val viewModel = viewModel(mode = WorkoutPreferenceScreenMode.Standalone)
        viewModel.screen.onOpened()
        val details = AccountDetails()
        details[PreferredWorkoutTypesKey::class] = WorkoutTypes(elements = setOf(WorkoutType.RUN))
        account.supplyUserDetails(details = details)

        // when
        viewModel.screen.onOpened()

        // then
        assertThat(mapper.input.hasChanges.value).isFalse()
    }

    @Test
    fun `a standalone screen has no Continue button`() {
        // when
        val viewModel = viewModel(mode = WorkoutPreferenceScreenMode.Standalone)

        // then
        assertThat(viewModel.screen.layout.continueButton).isNull()
    }

    private fun viewModel(mode: WorkoutPreferenceScreenMode = WorkoutPreferenceScreenMode.Step) = WorkoutPreferenceViewModel(
        savedStateHandle = SavedStateHandle(mapOf(WorkoutPreferenceViewModel.KEY_MODE to mode.name)),
        editor = editor,
        mapper = mapper,
    )

    private class RecordingWorkoutPreferenceMapper : WorkoutPreferenceLayoutMapper {
        private val mapper = WorkoutPreferenceLayoutMapperImpl()
        private var current: WorkoutPreferenceLayoutInput? = null
        val input: WorkoutPreferenceLayoutInput
            get() = requireNotNull(current)

        override fun map(input: WorkoutPreferenceLayoutInput): WorkoutPreferenceLayout {
            current = input
            return mapper.map(input = input)
        }

        override fun mapAppBar(input: WorkoutPreferenceLayoutInput) = mapper.mapAppBar(input = input)
    }
}
