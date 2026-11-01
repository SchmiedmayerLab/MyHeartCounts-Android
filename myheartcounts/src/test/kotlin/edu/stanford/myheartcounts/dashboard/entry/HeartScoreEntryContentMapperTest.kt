//
// This source file is part of the My Heart Counts Android open-source project
//
// SPDX-FileCopyrightText: 2026 Stanford University and the project authors (see CONTRIBUTORS.md)
// SPDX-License-Identifier: MIT
//

package edu.stanford.myheartcounts.dashboard.entry

import androidx.compose.ui.text.input.KeyboardType
import com.google.common.truth.Truth.assertThat
import edu.stanford.myheartcounts.MHCStrings
import edu.stanford.myheartcounts.dashboard.HeartScoreComponent
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import org.grovealliance.core.time.FakeTimeProvider
import org.grovealliance.ui.ActionSource
import org.grovealliance.ui.StringResource
import org.junit.Test
import java.time.Instant
import java.time.LocalDateTime
import java.time.ZoneId
import java.time.ZoneOffset
import java.util.Locale

@OptIn(ExperimentalCoroutinesApi::class)
class HeartScoreEntryContentMapperTest {
    private val time = FakeTimeProvider()
    private val mapper = HeartScoreEntryContentMapperImpl(timeProvider = time)
    private val events = mutableListOf<HeartScoreEntryAction>()
    private val actions = ActionSource<HeartScoreEntryAction> { events += it }.sink<HeartScoreEntryAction>()
    private val form = HeartScoreEntryForm(
        component = HeartScoreComponent.BloodPressure,
        date = LocalDateTime.of(2026, 1, 2, 9, 15),
        weightInPounds = false,
        heightInFeet = false,
        locale = Locale.US,
        zone = ZoneOffset.UTC,
    )

    @Test
    fun `pressure maps to labeled unit rows and callbacks emit field changes`() = runTest {
        // given
        val state = MutableStateFlow(
            HeartScoreEntryState(
                form = form,
                canSave = false,
                bmi = null,
            ),
        )

        // when
        val mapped = mapper.map(
            input = HeartScoreEntryContentInput(
                state = state,
                scope = backgroundScope,
                actionSink = actions,
            ),
        )
        val rows = mapped.layout.panels.single().rows
        rows[0].onValueChange("120")
        rows[1].onValueChange("80")

        // then
        assertThat(rows.map { it.label }).containsExactly(
            StringResource(MHCStrings.heart_entry_systolic),
            StringResource(MHCStrings.heart_entry_diastolic),
        ).inOrder()
        assertThat(rows.map { it.unit }).containsExactly(StringResource("mmHg"), StringResource("mmHg"))
        assertThat(events).containsExactly(
            HeartScoreEntryAction.FieldChanged(
                field = HeartScoreEntryAction.Field.Value,
                value = "120",
            ),
            HeartScoreEntryAction.FieldChanged(
                field = HeartScoreEntryAction.Field.Diastolic,
                value = "80",
            ),
        ).inOrder()
        assertThat(mapped.appBar.actions).hasSize(1)
        assertThat(mapped.layout.picker).isNull()
    }

    @Test
    fun `values update through flows without replacing row content`() = runTest {
        // given
        val state = MutableStateFlow(
            HeartScoreEntryState(
                form = form,
                canSave = false,
                bmi = null,
            ),
        )
        val mapped = mapper.map(
            input = HeartScoreEntryContentInput(
                state = state,
                scope = backgroundScope,
                actionSink = actions,
            ),
        )
        val row = mapped.layout.panels.single().rows.first()
        runCurrent()

        // when
        state.value = state.value.copy(form = form.copy(value = "120"))
        runCurrent()

        // then
        assertThat(row.value.value).isEqualTo("120")
        assertThat(mapped.layout.panels.single().rows.first()).isSameInstanceAs(row)
    }

    @Test
    fun `BMI has two finished panels and height choices in imperial units`() = runTest {
        // given
        val state = MutableStateFlow(
            HeartScoreEntryState(
                form = form.copy(
                    component = HeartScoreComponent.BodyMassIndex,
                    weightInPounds = true,
                    heightInFeet = true,
                    height = "5",
                    inches = "10",
                ),
                canSave = false,
                bmi = null,
            ),
        )

        // when
        val layout = mapper.map(
            input = HeartScoreEntryContentInput(
                state = state,
                scope = backgroundScope,
                actionSink = actions,
            ),
        ).layout
        layout.picker!!.onSelected(0)
        layout.panels[1].options!!.pickers[1].onSelected(11)

        // then
        assertThat(layout.panels).hasSize(2)
        assertThat(layout.selectedPanel.value).isEqualTo(1)
        assertThat(layout.panels[0].rows.single().unit).isNull()
        assertThat(layout.panels[1].rows.single().unit).isEqualTo(StringResource("lb"))
        assertThat(layout.panels[1].options!!.pickers[1].selectedIndex.value).isEqualTo(10)
        assertThat(events).containsExactly(
            HeartScoreEntryAction.ModeChanged(direct = true),
            HeartScoreEntryAction.FieldChanged(
                field = HeartScoreEntryAction.Field.Inches,
                value = "11",
            ),
        ).inOrder()
    }

    @Test
    fun `out of range values show the accepted range below their row`() = runTest {
        // given
        val state = MutableStateFlow(
            HeartScoreEntryState(
                form = form,
                canSave = false,
                bmi = null,
            ),
        )
        val rows = mapper.map(
            input = HeartScoreEntryContentInput(
                state = state,
                scope = backgroundScope,
                actionSink = actions,
            ),
        ).layout.panels.single().rows
        runCurrent()

        // when
        state.value = state.value.copy(form = form.copy(value = "300", diastolic = "80"))
        runCurrent()

        // then
        assertThat(rows[0].validationMessage.value).isEqualTo(StringResource(MHCStrings.heart_entry_out_of_range, "60", "250"))
        assertThat(rows[1].validationMessage.value).isNull()
        assertThat(rows.map { it.keyboardType }).containsExactly(KeyboardType.Number, KeyboardType.Number)
    }

    @Test
    fun `metric weight and height accept decimals`() = runTest {
        // given
        val state = MutableStateFlow(
            HeartScoreEntryState(
                form = form.copy(component = HeartScoreComponent.BodyMassIndex),
                canSave = false,
                bmi = null,
            ),
        )

        // when
        val rows = mapper.map(
            input = HeartScoreEntryContentInput(
                state = state,
                scope = backgroundScope,
                actionSink = actions,
            ),
        ).layout.panels[1].rows

        // then
        assertThat(rows.map { it.keyboardType }).containsExactly(KeyboardType.Decimal, KeyboardType.Decimal)
    }

    @Test
    fun `calendar offers only days up to the participant's local today`() = runTest {
        // given
        time.setZone(ZoneId.of("America/Los_Angeles"))
        time.setNow(Instant.parse("2026-01-03T03:00:00Z"))
        val state = MutableStateFlow(
            HeartScoreEntryState(
                form = form,
                canSave = false,
                bmi = null,
            ),
        )

        // when
        val row = mapper.map(
            input = HeartScoreEntryContentInput(
                state = state,
                scope = backgroundScope,
                actionSink = actions,
            ),
        ).layout.dateRow

        // then
        assertThat(row.isSelectableDate(Instant.parse("2026-01-01T00:00:00Z"))).isTrue()
        assertThat(row.isSelectableDate(Instant.parse("2026-01-02T00:00:00Z"))).isTrue()
        assertThat(row.isSelectableDate(Instant.parse("2026-01-03T00:00:00Z"))).isFalse()
    }

    @Test
    fun `date and time callbacks preserve the other part of the selected timestamp`() = runTest {
        // given
        val state = MutableStateFlow(
            HeartScoreEntryState(
                form = form,
                canSave = false,
                bmi = null,
            ),
        )
        val row = mapper.map(
            input = HeartScoreEntryContentInput(
                state = state,
                scope = backgroundScope,
                actionSink = actions,
            ),
        ).layout.dateRow

        // when
        row.onDateSelected(Instant.parse("2026-01-01T00:00:00Z"))
        row.onTimeSelected(8, 30)

        // then
        assertThat(events).containsExactly(
            HeartScoreEntryAction.DateChanged(value = LocalDateTime.of(2026, 1, 1, 9, 15)),
            HeartScoreEntryAction.DateChanged(value = LocalDateTime.of(2026, 1, 2, 8, 30)),
        ).inOrder()
    }
}
