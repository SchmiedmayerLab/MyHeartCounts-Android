//
// This source file is part of the My Heart Counts Android open-source project
//
// SPDX-FileCopyrightText: 2026 Stanford University and the project authors (see CONTRIBUTORS.md)
// SPDX-License-Identifier: MIT
//

package edu.stanford.myheartcounts.dashboard.entry

import androidx.lifecycle.SavedStateHandle
import com.google.common.truth.Truth.assertThat
import edu.stanford.myheartcounts.MHCStrings
import edu.stanford.myheartcounts.dashboard.HeartHealthScoreFixtures
import edu.stanford.myheartcounts.dashboard.HeartScoreComponent
import edu.stanford.myheartcounts.navigation.FakeNavigator
import edu.stanford.myheartcounts.navigation.MHCRoute
import edu.stanford.myheartcounts.navigation.NavigationEvent
import edu.stanford.myheartcounts.standard.health.HealthAccess
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.grovealliance.account.FakeAccount
import org.grovealliance.core.time.FakeTimeProvider
import org.grovealliance.health.HealthConnectAvailability
import org.grovealliance.ui.StringResource
import org.junit.After
import org.junit.Before
import org.junit.Test
import java.time.Instant
import java.time.LocalDateTime

@OptIn(ExperimentalCoroutinesApi::class)
class HeartScoreEntryViewModelTest {
    private val account = FakeAccount()
    private val navigator = FakeNavigator()
    private val time = FakeTimeProvider().apply { setNow(Instant.parse("2025-10-26T12:00:00Z")) }
    private val writer = FakeEntryWriter()
    private val mapper = RecordingEntryMapper()
    private val access = FakeEntryHealthAccess()
    private val saved = SavedStateHandle(mapOf(HeartScoreEntryViewModel.KEY_COMPONENT to HeartScoreComponent.BloodGlucose.name))

    @Before
    fun setup() {
        Dispatchers.setMain(UnconfinedTestDispatcher())
        account.supplyUserDetails(details = HeartHealthScoreFixtures.account())
    }

    @After
    fun teardown() {
        Dispatchers.resetMain()
    }

    @Test
    fun `incomplete and out of range entries cannot save`() = runTest {
        // given
        viewModel()

        // when
        mapper.input.actionSink.push(
            HeartScoreEntryAction.FieldChanged(
                field = HeartScoreEntryAction.Field.Value,
                value = "400",
            ),
        )
        mapper.input.onSave()

        // then
        assertThat(mapper.input.state.value.canSave).isFalse()
        assertThat(writer.entries).isEmpty()
        assertThat(access.requests).isEqualTo(0)
    }

    @Test
    fun `successful native save requests permissions and closes`() = runTest {
        // given
        viewModel()
        mapper.input.actionSink.push(
            HeartScoreEntryAction.FieldChanged(
                field = HeartScoreEntryAction.Field.Value,
                value = "90",
            ),
        )

        // when
        mapper.input.onSave()

        // then
        assertThat(writer.entries).hasSize(1)
        assertThat(access.requests).isEqualTo(1)
        val route = MHCRoute.HeartScoreEntry(component = HeartScoreComponent.BloodGlucose)
        assertThat(navigator.pushedEvents).containsExactly(NavigationEvent.Close(route = route))
    }

    @Test
    fun `permission denial keeps the form editable with specific recovery guidance`() = runTest {
        // given
        writer.result = Result.failure(SecurityException())
        viewModel()
        mapper.input.actionSink.push(
            HeartScoreEntryAction.FieldChanged(
                field = HeartScoreEntryAction.Field.Value,
                value = "90",
            ),
        )

        // when
        mapper.input.onSave()

        // then
        assertThat(mapper.input.state.value.error).isEqualTo(StringResource(MHCStrings.heart_entry_permission_error))
        assertThat(mapper.input.state.value.form.value).isEqualTo("90")
        assertThat(mapper.input.state.value.saving).isFalse()
        assertThat(navigator.pushedEvents).isEmpty()
    }

    @Test
    fun `a picker stays open until a date is chosen or it is dismissed`() {
        // given
        viewModel()

        // when
        mapper.input.actionSink.push(HeartScoreEntryAction.PickerRequested(picker = HeartScoreEntryState.Picker.Date))
        val opened = mapper.input.state.value.picker
        mapper.input.actionSink.push(HeartScoreEntryAction.DateChanged(value = LocalDateTime.of(2025, 10, 25, 8, 0)))
        val afterSelection = mapper.input.state.value.picker
        mapper.input.actionSink.push(HeartScoreEntryAction.PickerRequested(picker = HeartScoreEntryState.Picker.Time))
        mapper.input.actionSink.push(HeartScoreEntryAction.PickerDismissed)

        // then
        assertThat(opened).isEqualTo(HeartScoreEntryState.Picker.Date)
        assertThat(afterSelection).isNull()
        assertThat(mapper.input.state.value.picker).isNull()
    }

    @Test
    fun `restoring the screen retains the date fields and retry identity`() {
        // given
        viewModel()
        mapper.input.actionSink.push(
            HeartScoreEntryAction.FieldChanged(
                field = HeartScoreEntryAction.Field.Value,
                value = "90",
            ),
        )
        val expected = mapper.input.state.value.form

        // when
        viewModel()

        // then
        assertThat(mapper.input.state.value.form).isEqualTo(expected)
    }

    @Test
    fun `whole-number fields ignore input other than digits`() {
        // given
        viewModel()
        mapper.input.actionSink.push(
            HeartScoreEntryAction.FieldChanged(
                field = HeartScoreEntryAction.Field.Value,
                value = "90",
            ),
        )

        // when
        mapper.input.actionSink.push(
            HeartScoreEntryAction.FieldChanged(
                field = HeartScoreEntryAction.Field.Value,
                value = "90.5",
            ),
        )

        // then
        assertThat(mapper.input.state.value.form.value).isEqualTo("90")
    }

    @Test
    fun `LDL saves without requesting native write access`() = runTest {
        // given
        saved[HeartScoreEntryViewModel.KEY_COMPONENT] = HeartScoreComponent.BloodLipids.name
        viewModel()
        mapper.input.actionSink.push(
            HeartScoreEntryAction.FieldChanged(
                field = HeartScoreEntryAction.Field.Value,
                value = "110",
            ),
        )

        // when
        mapper.input.onSave()

        // then
        assertThat(access.requests).isEqualTo(0)
        assertThat(writer.entries).hasSize(1)
    }

    private fun viewModel() = HeartScoreEntryViewModel(
        savedStateHandle = saved,
        navigator = navigator,
        writer = writer,
        healthAccess = access,
        account = account,
        timeProvider = time,
        mapper = mapper,
    )

    private inner class RecordingEntryMapper : HeartScoreEntryContentMapper {
        private var current: HeartScoreEntryContentInput? = null
        val input: HeartScoreEntryContentInput
            get() = requireNotNull(current)

        override fun map(input: HeartScoreEntryContentInput): HeartScoreEntryMappedLayout {
            current = input
            return HeartScoreEntryContentMapperImpl(timeProvider = time).map(input = input)
        }
    }

    private class FakeEntryWriter : HeartScoreEntryWriter {
        val entries = mutableListOf<HeartScoreEntry>()
        var result: Result<Unit> = Result.success(Unit)

        override suspend fun save(entry: HeartScoreEntry): Result<Unit> {
            entries += entry
            return result
        }
    }

    private class FakeEntryHealthAccess : HealthAccess {
        override val availability = MutableStateFlow<HealthConnectAvailability>(HealthConnectAvailability.Available)
        var requests = 0

        override suspend fun requestPermissions(): Boolean {
            requests++
            return true
        }

        override fun installHealthConnect(): Boolean = false
    }
}
