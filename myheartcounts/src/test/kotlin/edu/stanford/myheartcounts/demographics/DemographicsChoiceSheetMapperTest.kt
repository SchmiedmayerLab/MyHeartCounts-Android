//
// This source file is part of the My Heart Counts Android open-source project
//
// SPDX-FileCopyrightText: 2026 Stanford University and the project authors (see CONTRIBUTORS.md)
//
// SPDX-License-Identifier: MIT

package edu.stanford.myheartcounts.demographics

import com.google.common.truth.Truth.assertThat
import edu.stanford.myheartcounts.MHCStrings
import edu.stanford.myheartcounts.demographics.ui.DemographicsChoiceSheet
import edu.stanford.myheartcounts.model.demographics.Comorbidities
import edu.stanford.myheartcounts.model.demographics.EducationStatusUK
import edu.stanford.myheartcounts.model.demographics.HouseholdIncomeUK
import edu.stanford.myheartcounts.model.demographics.RaceEthnicity
import edu.stanford.myheartcounts.model.demographics.UKRegion
import edu.stanford.myheartcounts.model.demographics.USRegion
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import org.grovealliance.core.time.FakeTimeProvider
import org.grovealliance.ui.ActionSource
import org.grovealliance.ui.StringResource
import org.junit.Before
import org.junit.Test
import java.time.LocalDate
import java.time.ZoneOffset

@OptIn(ExperimentalCoroutinesApi::class)
class DemographicsChoiceSheetMapperTest {
    private val pushedActions = mutableListOf<DemographicsAction>()
    private val actionSink = ActionSource<DemographicsAction> { pushedActions.add(it) }.sink<DemographicsAction>()
    private val response = MutableStateFlow(DemographicsResponse.empty)
    private val scope = TestScope(UnconfinedTestDispatcher())
    private val timeProvider = FakeTimeProvider()
    private val catalog = DemographicsCatalogFixtures.create()
    private val mapper = DemographicsChoiceSheetMapperImpl(
        catalog = catalog,
        optionIdMapper = DemographicsOptionIdMapperImpl(),
        groupMapper = DemographicsChoiceGroupMapperImpl(catalog = catalog),
        rules = DemographicsRules(),
        timeProvider = timeProvider,
    )

    @Before
    fun setup() {
        timeProvider.setZone(zone = ZoneOffset.UTC)
        timeProvider.setNow(instant = LocalDate.of(2026, 1, 1).atStartOfDay(ZoneOffset.UTC).toInstant())
    }

    @Test
    fun `race choices add up and stay open`() {
        // given
        response.value = DemographicsResponse.empty.copy(raceEthnicity = RaceEthnicity(rawValue = 2))
        val sheet = mapper.mapRaceEthnicitySheet(input = input())

        // when
        sheet.onSelect(sheet.option(title = StringResource(MHCStrings.demographics_race_ethnicity_4)).index)

        // then
        assertThat(pushedActions).containsExactly(DemographicsAction.RaceEthnicityChanged(value = RaceEthnicity(rawValue = 6)))
    }

    @Test
    fun `declining to state race replaces every other choice`() {
        // given
        response.value = DemographicsResponse.empty.copy(raceEthnicity = RaceEthnicity(rawValue = 6))
        val sheet = mapper.mapRaceEthnicitySheet(input = input())

        // when
        sheet.onSelect(sheet.option(title = StringResource(MHCStrings.demographics_race_ethnicity_1)).index)

        // then
        assertThat(pushedActions).containsExactly(
            DemographicsAction.RaceEthnicityChanged(value = RaceEthnicity.PREFER_NOT_TO_STATE),
        )
    }

    @Test
    fun `none is offered only while no condition is chosen`() {
        // given
        response.value = DemographicsResponse.empty.copy(comorbidities = Comorbidities(entries = emptyMap()))
        val offeredWithout = mapper.mapComorbiditiesSheet(input = input()).titles()

        // when
        response.value = DemographicsResponse.empty.copy(comorbidities = Comorbidities(entries = mapOf("diabetes" to "")))
        val offeredWith = mapper.mapComorbiditiesSheet(input = input()).titles()

        // then
        assertThat(offeredWithout).contains(StringResource(MHCStrings.demographics_comorbidities_none))
        assertThat(offeredWith).doesNotContain(StringResource(MHCStrings.demographics_comorbidities_none))
    }

    @Test
    fun `a chosen condition offers its diagnosis date`() {
        // given
        response.value = DemographicsResponse.empty.copy(comorbidities = Comorbidities(entries = mapOf("diabetes" to "2020-03")))
        val sheet = mapper.mapComorbiditiesSheet(input = input())

        // when
        val followUp = sheet.sections.value.flatMap { it.options }.single { it.isSelected }.followUp
        followUp?.onClick?.invoke()

        // then
        assertThat(followUp?.isSet).isTrue()
        assertThat(pushedActions).containsExactly(DemographicsAction.DiagnosisDateSelected(condition = "diabetes"))
    }

    @Test
    fun `the diagnosis date picker stores a year and month, a year, or an unknown date`() {
        // given
        response.value = DemographicsResponse.empty.copy(comorbidities = Comorbidities(entries = mapOf("diabetes" to "2020-03")))
        val picker = mapper.mapDiagnosisDatePicker(
            condition = "diabetes",
            input = input(),
        )

        // when
        picker.onSelect(2021, 7)
        picker.onSelect(2019, null)
        picker.onClear()

        // then
        assertThat(picker.year).isEqualTo(2020)
        assertThat(picker.month).isEqualTo(3)
        assertThat(pushedActions.map { (it as DemographicsAction.ComorbiditiesChanged).value?.entries?.get("diabetes") })
            .containsExactly("2021-07", "2019", "")
            .inOrder()
    }

    @Test
    fun `the US region sheet chooses a state and closes`() {
        // given
        val sheet = mapper.mapRegionSheet(input = input(region = DemographicsRegion.US))

        // when
        sheet.onSelect(sheet.option(detail = StringResource("DC")).index)

        // then
        assertThat(pushedActions).containsExactly(
            DemographicsAction.USRegionChanged(value = USRegion.DISTRICT_OF_COLUMBIA),
            DemographicsAction.SheetClosed,
        ).inOrder()
    }

    @Test
    fun `the UK region sheet lists the nations and opens the chosen one's counties`() {
        // given
        response.value = DemographicsResponse.empty.copy(ukRegion = UKRegion.England(county = UKRegion.County(name = "Kent")))
        val sheet = mapper.mapRegionSheet(input = input(region = DemographicsRegion.UK))
        val england = sheet.option(title = StringResource(MHCStrings.demographics_uk_region_england))

        // when
        sheet.onSelect(sheet.option(title = StringResource(MHCStrings.demographics_uk_region_wales)).index)

        // then
        assertThat(england.detail).isEqualTo(StringResource("Kent"))
        assertThat(pushedActions).containsExactly(DemographicsAction.UKNationSelected(nation = "wales"))
    }

    @Test
    fun `the county sheet stores the nation with the county and closes`() {
        // given
        val sheet = mapper.mapUKCountySheet(
            nation = "wales",
            input = input(region = DemographicsRegion.UK),
        )

        // when
        sheet.onSelect(sheet.option(title = StringResource("Powys")).index)

        // then
        assertThat(pushedActions).containsExactly(
            DemographicsAction.UKRegionChanged(value = UKRegion.Wales(county = UKRegion.County(name = "Powys"))),
            DemographicsAction.SheetClosed,
        ).inOrder()
    }

    @Test
    fun `the UK form offers the UK education and income answers`() {
        // given
        val education = mapper.mapEducationSheet(input = input(region = DemographicsRegion.UK))
        val income = mapper.mapIncomeSheet(input = input(region = DemographicsRegion.UK))

        // when
        education.onSelect(education.option(title = StringResource(MHCStrings.demographics_uk_education_master)).index)
        income.onSelect(income.option(title = StringResource(MHCStrings.demographics_uk_income_8)).index)

        // then
        assertThat(pushedActions).containsExactly(
            DemographicsAction.EducationUKChanged(value = EducationStatusUK.MASTER),
            DemographicsAction.SheetClosed,
            DemographicsAction.HouseholdIncomeUKChanged(value = HouseholdIncomeUK.ABOVE_150K),
            DemographicsAction.SheetClosed,
        ).inOrder()
    }

    @Test
    fun `a single answer sheet marks the current answer`() {
        // given
        response.value = DemographicsResponse.empty.copy(referralSource = "3")

        // when
        val sheet = mapper.mapReferralSourceSheet(input = input())

        // then
        assertThat(sheet.sections.value.flatMap { it.options }.single { it.isSelected }.title)
            .isEqualTo(StringResource(MHCStrings.demographics_referral_source_3))
    }

    private fun input(region: DemographicsRegion = DemographicsRegion.US) = DemographicsLayoutInputFixtures.create(
        scope = scope,
        actionSink = actionSink,
        region = region,
        response = response,
    )

    private fun DemographicsChoiceSheet.titles() = sections.value.flatMap { section -> section.options.map { it.title } }

    private fun DemographicsChoiceSheet.option(
        title: StringResource? = null,
        detail: StringResource? = null,
    ) = sections.value
        .flatMap { it.options }
        .first { (title == null || it.title == title) && (detail == null || it.detail == detail) }
}
