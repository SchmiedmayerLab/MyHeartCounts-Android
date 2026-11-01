//
// This source file is part of the My Heart Counts Android open-source project
//
// SPDX-FileCopyrightText: 2026 Stanford University and the project authors (see CONTRIBUTORS.md)
//
// SPDX-License-Identifier: MIT

package edu.stanford.myheartcounts.demographics

import androidx.compose.ui.text.input.KeyboardType
import com.google.common.truth.Truth.assertThat
import edu.stanford.myheartcounts.MHCStrings
import edu.stanford.myheartcounts.model.demographics.BloodType
import edu.stanford.myheartcounts.model.demographics.MHCGenderIdentity
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import org.grovealliance.core.time.FakeTimeProvider
import org.grovealliance.ui.ActionSource
import org.grovealliance.ui.GroveAppBar
import org.grovealliance.ui.StringResource
import org.junit.Before
import org.junit.Test
import java.time.LocalDate
import java.time.ZoneOffset

@OptIn(ExperimentalCoroutinesApi::class)
class DemographicsPickerMapperTest {
    private val pushedActions = mutableListOf<DemographicsAction>()
    private val actionSink = ActionSource<DemographicsAction> { pushedActions.add(it) }.sink<DemographicsAction>()
    private val response = MutableStateFlow(DemographicsResponse.empty)
    private val scope = TestScope(UnconfinedTestDispatcher())
    private val timeProvider = FakeTimeProvider()
    private val mapper = DemographicsPickerMapperImpl(
        answerMapper = DemographicsAnswerMapperImpl(optionIdMapper = DemographicsOptionIdMapperImpl()),
        timeProvider = timeProvider,
    )

    @Before
    fun setup() {
        timeProvider.setZone(zone = ZoneOffset.UTC)
        timeProvider.setNow(instant = today.atStartOfDay(ZoneOffset.UTC).toInstant())
    }

    @Test
    fun `the gender identity menu offers no answer followed by every identity`() {
        // when
        val menu = mapper.mapGenderIdentityMenu(input = input())

        // then
        assertThat(menu.field).isEqualTo(DemographicsField.GenderIdentity)
        assertThat(menu.options).hasSize(MHCGenderIdentity.entries.size + 1)
        assertThat(menu.options.first().label).isEqualTo(StringResource(MHCStrings.demographics_value_empty))
        assertThat(menu.options.first().isFollowedByDivider).isTrue()
        assertThat(menu.options.first().isSelected).isTrue()
    }

    @Test
    fun `a menu marks the current answer`() {
        // given
        response.value = DemographicsResponse.empty.copy(genderIdentity = MHCGenderIdentity.FEMALE)

        // when
        val menu = mapper.mapGenderIdentityMenu(input = input())

        // then
        assertThat(menu.options.single { it.isSelected }.label).isEqualTo(StringResource(MHCStrings.demographics_gender_identity_2))
    }

    @Test
    fun `choosing in a menu reports the answer, and no answer as clearing it`() {
        // given
        val menu = mapper.mapGenderIdentityMenu(input = input())
        val female = menu.options.single { it.label == StringResource(MHCStrings.demographics_gender_identity_2) }

        // when
        menu.onSelect(female.index)
        menu.onSelect(0)
        menu.onDismiss()

        // then
        assertThat(pushedActions).containsExactly(
            DemographicsAction.GenderIdentityChanged(value = MHCGenderIdentity.FEMALE),
            DemographicsAction.GenderIdentityChanged(value = null),
            DemographicsAction.MenuDismissed,
        ).inOrder()
    }

    @Test
    fun `the blood type menu offers only known blood types`() {
        // when
        val menu = mapper.mapBloodTypeMenu(input = input())

        // then
        assertThat(menu.options.map { it.label }).doesNotContain(StringResource(MHCStrings.demographics_value_not_set))
        assertThat(menu.options).hasSize(BloodType.entries.size)
    }

    @Test
    fun `the date of birth picker opens on the answer and allows no date after today`() {
        // given
        response.value = DemographicsResponse.empty.copy(dateOfBirth = birthday)

        // when
        val picker = mapper.mapDateOfBirthPicker(input = input())

        // then
        assertThat(picker.selection).isEqualTo(birthday)
        assertThat(picker.latest).isEqualTo(today)
    }

    @Test
    fun `the date of birth picker reports the chosen date and dismissal`() {
        // given
        val picker = mapper.mapDateOfBirthPicker(input = input())

        // when
        picker.onSelect(birthday)
        picker.onDismiss()

        // then
        assertThat(pushedActions).containsExactly(
            DemographicsAction.DateOfBirthChanged(value = birthday),
            DemographicsAction.DatePickerDismissed,
        ).inOrder()
    }

    @Test
    fun `measurement sheets ask for a decimal in their unit`() {
        // when
        val sheet = entrySheet(entry = DemographicsEntry.Height)

        // then
        assertThat(sheet.unit).isEqualTo(StringResource(MHCStrings.demographics_unit_cm))
        assertThat(sheet.keyboardType).isEqualTo(KeyboardType.Decimal)
        assertThat(sheet.help).isNull()
    }

    @Test
    fun `the NHS number sheet asks for digits and links to finding the number`() {
        // given
        val sheet = entrySheet(entry = DemographicsEntry.NHSNumber)

        // when
        sheet.help?.onClick?.invoke()
        sheet.onEntryChange("943")

        // then
        assertThat(sheet.unit).isNull()
        assertThat(sheet.keyboardType).isEqualTo(KeyboardType.Number)
        assertThat(sheet.help?.title).isEqualTo(StringResource(MHCStrings.demographics_nhs_number_help))
        assertThat(pushedActions).containsExactly(
            DemographicsAction.NHSNumberHelpSelected,
            DemographicsAction.EntryChanged(entry = "943"),
        ).inOrder()
    }

    @Test
    fun `the entry app bar offers saving only while allowed`() {
        // given
        val unsavable = mapper.mapEntryAppBar(
            entry = DemographicsEntry.Weight,
            canSave = false,
            input = input(),
        )

        // when
        val savable = mapper.mapEntryAppBar(
            entry = DemographicsEntry.Weight,
            canSave = true,
            input = input(),
        )

        // then
        assertThat(savable.actions).hasSize(1)
        assertThat(savable).isNotEqualTo(unsavable)
    }

    private fun input() = DemographicsLayoutInputFixtures.create(
        scope = scope,
        actionSink = actionSink,
        response = response,
    )

    private fun entrySheet(entry: DemographicsEntry) = mapper.mapEntrySheet(
        entry = entry,
        text = MutableStateFlow(""),
        appBar = MutableStateFlow(GroveAppBar.Empty),
        input = input(),
    )

    private companion object {
        val today: LocalDate = LocalDate.of(2026, 1, 1)
        val birthday: LocalDate = LocalDate.of(1980, 5, 17)
    }
}
