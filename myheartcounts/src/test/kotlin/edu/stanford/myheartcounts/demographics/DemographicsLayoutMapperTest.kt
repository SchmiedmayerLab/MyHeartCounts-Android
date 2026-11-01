//
// This source file is part of the My Heart Counts Android open-source project
//
// SPDX-FileCopyrightText: 2026 Stanford University and the project authors (see CONTRIBUTORS.md)
//
// SPDX-License-Identifier: MIT

package edu.stanford.myheartcounts.demographics

import com.google.common.truth.Truth.assertThat
import edu.stanford.myheartcounts.MHCStrings
import edu.stanford.myheartcounts.demographics.ui.DemographicsLayout
import edu.stanford.myheartcounts.model.demographics.BloodType
import edu.stanford.myheartcounts.model.demographics.Comorbidities
import edu.stanford.myheartcounts.model.demographics.NHSNumber
import edu.stanford.myheartcounts.model.demographics.UKRegion
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import org.grovealliance.ui.ActionSource
import org.grovealliance.ui.StringResource
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class DemographicsLayoutMapperTest {
    private val pushedActions = mutableListOf<DemographicsAction>()
    private val actionSink = ActionSource<DemographicsAction> { pushedActions.add(it) }.sink<DemographicsAction>()
    private val response = MutableStateFlow(DemographicsResponse.empty)
    private val completion = MutableStateFlow<Map<DemographicsField, DemographicsCompletion>>(emptyMap())
    private val scope = TestScope(UnconfinedTestDispatcher())
    private val mapper = DemographicsLayoutMapperImpl(
        catalog = DemographicsCatalogFixtures.create(),
        optionIdMapper = DemographicsOptionIdMapperImpl(),
        answerMapper = DemographicsAnswerMapperImpl(optionIdMapper = DemographicsOptionIdMapperImpl()),
        rules = DemographicsRules(),
    )

    @Test
    fun `the US form groups its questions into sections in display order`() {
        // when
        val layout = mapper.map(input = input(region = DemographicsRegion.US))

        // then
        assertThat(layout.sections.map { section -> section.rows.map { it.field } }).containsExactly(
            listOf(
                DemographicsField.DateOfBirth,
                DemographicsField.GenderIdentity,
                DemographicsField.SexAtBirth,
                DemographicsField.BloodType,
            ),
            listOf(DemographicsField.Height, DemographicsField.Weight),
            listOf(DemographicsField.RaceEthnicity, DemographicsField.LatinoStatus),
            listOf(DemographicsField.Comorbidities),
            listOf(DemographicsField.USRegion, DemographicsField.USEducation, DemographicsField.USIncome),
            listOf(DemographicsField.StageOfChange),
            listOf(DemographicsField.ReferralSource),
        ).inOrder()
    }

    @Test
    fun `the UK form asks for the UK background and the NHS number instead`() {
        // when
        val layout = mapper.map(input = input(region = DemographicsRegion.UK))

        // then
        assertThat(layout.sections.map { section -> section.rows.map { it.field } }).containsExactly(
            listOf(
                DemographicsField.DateOfBirth,
                DemographicsField.GenderIdentity,
                DemographicsField.SexAtBirth,
                DemographicsField.BloodType,
            ),
            listOf(DemographicsField.Height, DemographicsField.Weight),
            listOf(DemographicsField.RaceEthnicity),
            listOf(DemographicsField.Comorbidities),
            listOf(DemographicsField.UKRegion, DemographicsField.UKEducation, DemographicsField.UKIncome),
            listOf(DemographicsField.NHSNumber),
            listOf(DemographicsField.StageOfChange),
            listOf(DemographicsField.ReferralSource),
        ).inOrder()
    }

    @Test
    fun `an answer shows on a required question without the optional note`() {
        // given
        response.value = DemographicsResponse.empty.copy(bloodType = BloodType.A_POSITIVE)
        completion.value = mapOf(DemographicsField.BloodType to completion(isRequired = true))

        // when
        val row = mapper.map(input = input()).row(field = DemographicsField.BloodType)

        // then
        assertThat(row.value.value).isEqualTo(
            DemographicsLayout.Value(
                text = StringResource(MHCStrings.demographics_blood_type_1),
                isOptional = false,
            ),
        )
    }

    @Test
    fun `a missing answer shows its placeholder and an optional question says so`() {
        // given
        completion.value = mapOf(DemographicsField.RaceEthnicity to completion(isRequired = false))

        // when
        val row = mapper.map(input = input()).row(field = DemographicsField.RaceEthnicity)

        // then
        assertThat(row.value.value).isEqualTo(
            DemographicsLayout.Value(
                text = StringResource(MHCStrings.demographics_value_no_selection),
                isOptional = true,
            ),
        )
    }

    @Test
    fun `rows follow changes to the answers and whether they are required`() {
        // given
        completion.value = mapOf(DemographicsField.Height to completion(isRequired = false))
        val row = mapper.map(input = input()).row(field = DemographicsField.Height)

        // when
        response.value = DemographicsResponse.empty.copy(heightInCm = 172.5)
        completion.value = mapOf(DemographicsField.Height to completion(isRequired = true))

        // then
        assertThat(row.value.value).isEqualTo(
            DemographicsLayout.Value(
                text = StringResource(MHCStrings.demographics_value_cm, "172.5"),
                isOptional = false,
            ),
        )
    }

    @Test
    fun `the UK region shows its nation and county`() {
        // given
        response.value = DemographicsResponse.empty.copy(
            ukRegion = UKRegion.Wales(county = UKRegion.County(name = "Powys")),
        )

        // when
        val row = mapper.map(input = input(region = DemographicsRegion.UK)).row(field = DemographicsField.UKRegion)

        // then
        assertThat(row.value.value.text).isEqualTo(StringResource(MHCStrings.demographics_uk_region_wales_county, "Powys"))
    }

    @Test
    fun `the NHS number shows as entered`() {
        // given
        response.value = DemographicsResponse.empty.copy(nhsNumber = NHSNumber(stringValue = "9434765919"))

        // when
        val row = mapper.map(input = input(region = DemographicsRegion.UK)).row(field = DemographicsField.NHSNumber)

        // then
        assertThat(row.value.value.text).isEqualTo(StringResource("9434765919"))
    }

    @Test
    fun `no comorbidities shows as none`() {
        // given
        response.value = DemographicsResponse.empty.copy(comorbidities = Comorbidities(entries = emptyMap()))

        // when
        val row = mapper.map(input = input()).row(field = DemographicsField.Comorbidities)

        // then
        assertThat(row.value.value.text).isEqualTo(StringResource(MHCStrings.demographics_comorbidities_none))
    }

    @Test
    fun `a tapped question is reported through the action sink`() {
        // given
        val row = mapper.map(input = input()).row(field = DemographicsField.USIncome)

        // when
        row.onClick()

        // then
        assertThat(pushedActions).containsExactly(DemographicsAction.QuestionSelected(field = DemographicsField.USIncome))
    }

    @Test
    fun `the health import is reported through the action sink`() {
        // given
        val layout = mapper.map(input = input())

        // when
        layout.healthImport.onClick()

        // then
        assertThat(pushedActions).containsExactly(DemographicsAction.HealthImportSelected)
    }

    private fun input(region: DemographicsRegion = DemographicsRegion.US) = DemographicsLayoutInputFixtures.create(
        scope = scope,
        actionSink = actionSink,
        region = region,
        response = response,
        completion = completion,
    )

    private fun DemographicsLayout.row(field: DemographicsField) = sections.flatMap { it.rows }.single { it.field == field }

    private fun completion(isRequired: Boolean) = DemographicsCompletion(
        isAnswered = false,
        isRequired = isRequired,
    )
}
