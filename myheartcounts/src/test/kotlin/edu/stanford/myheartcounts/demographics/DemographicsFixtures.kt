//
// This source file is part of the My Heart Counts Android open-source project
//
// SPDX-FileCopyrightText: 2026 Stanford University and the project authors (see CONTRIBUTORS.md)
//
// SPDX-License-Identifier: MIT

package edu.stanford.myheartcounts.demographics

import edu.stanford.myheartcounts.demographics.ui.DemographicsDatePicker
import edu.stanford.myheartcounts.demographics.ui.DemographicsMenu
import edu.stanford.myheartcounts.demographics.ui.DemographicsMonthYearPicker
import edu.stanford.myheartcounts.model.demographics.BiologicalSex
import edu.stanford.myheartcounts.model.demographics.BloodType
import edu.stanford.myheartcounts.model.demographics.Comorbidities
import edu.stanford.myheartcounts.model.demographics.EducationStatusUK
import edu.stanford.myheartcounts.model.demographics.EducationStatusUS
import edu.stanford.myheartcounts.model.demographics.HouseholdIncomeUK
import edu.stanford.myheartcounts.model.demographics.HouseholdIncomeUS
import edu.stanford.myheartcounts.model.demographics.LatinoStatusOption
import edu.stanford.myheartcounts.model.demographics.MHCGenderIdentity
import edu.stanford.myheartcounts.model.demographics.NHSNumber
import edu.stanford.myheartcounts.model.demographics.RaceEthnicity
import edu.stanford.myheartcounts.model.demographics.StageOfChangeOption
import edu.stanford.myheartcounts.model.demographics.UKRegion
import edu.stanford.myheartcounts.model.demographics.USRegion
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import org.grovealliance.ui.ActionSink
import org.grovealliance.ui.GroveScaffoldState
import java.time.LocalDate

/**
 * Builds [DemographicsResponse]s; by default no question is answered.
 */
object DemographicsResponseFixtures {
    @Suppress("detekt:LongParameterList")
    fun create(
        dateOfBirth: LocalDate? = null,
        genderIdentity: MHCGenderIdentity? = null,
        biologicalSexAtBirth: BiologicalSex? = null,
        bloodType: BloodType? = null,
        heightInCm: Double? = null,
        weightInKg: Double? = null,
        raceEthnicity: RaceEthnicity? = null,
        latinoStatus: LatinoStatusOption? = null,
        comorbidities: Comorbidities? = null,
        usRegion: USRegion? = null,
        ukRegion: UKRegion? = null,
        educationUS: EducationStatusUS? = null,
        educationUK: EducationStatusUK? = null,
        householdIncomeUS: HouseholdIncomeUS? = null,
        householdIncomeUK: HouseholdIncomeUK? = null,
        nhsNumber: NHSNumber? = null,
        stageOfChange: StageOfChangeOption? = null,
        referralSource: String? = null,
    ) = DemographicsResponse(
        dateOfBirth = dateOfBirth,
        genderIdentity = genderIdentity,
        biologicalSexAtBirth = biologicalSexAtBirth,
        bloodType = bloodType,
        heightInCm = heightInCm,
        weightInKg = weightInKg,
        raceEthnicity = raceEthnicity,
        latinoStatus = latinoStatus,
        comorbidities = comorbidities,
        usRegion = usRegion,
        ukRegion = ukRegion,
        educationUS = educationUS,
        educationUK = educationUK,
        householdIncomeUS = householdIncomeUS,
        householdIncomeUK = householdIncomeUK,
        nhsNumber = nhsNumber,
        stageOfChange = stageOfChange,
        referralSource = referralSource,
    )

    /**
     * A response answering every question of both regional forms.
     */
    fun answered() = create(
        dateOfBirth = LocalDate.of(1990, 6, 15),
        genderIdentity = MHCGenderIdentity.TRANS_FEMALE,
        biologicalSexAtBirth = BiologicalSex.MALE,
        bloodType = BloodType.A_POSITIVE,
        heightInCm = 172.5,
        weightInKg = 72.0,
        raceEthnicity = RaceEthnicity(rawValue = 130),
        latinoStatus = LatinoStatusOption.PREFER_NOT_TO_STATE,
        comorbidities = Comorbidities(
            entries = mapOf(
                "diabetes" to "2020-03",
                "heartFailure" to "2019",
                "angina" to "",
            ),
        ),
        usRegion = USRegion.DISTRICT_OF_COLUMBIA,
        ukRegion = UKRegion.England(county = UKRegion.County(name = "Kent")),
        educationUS = EducationStatusUS.BACHELOR,
        educationUK = EducationStatusUK.VOCATIONAL_TRAINING,
        householdIncomeUS = HouseholdIncomeUS.FROM_35K_TO_49999,
        householdIncomeUK = HouseholdIncomeUK.ABOVE_150K,
        nhsNumber = NHSNumber(stringValue = "9434765919"),
        stageOfChange = StageOfChangeOption.C,
        referralSource = "11",
    )
}

/**
 * Builds [DemographicsState]s; by default nobody is signed in and nothing is answered on the US form.
 */
object DemographicsStateFixtures {
    fun create(
        accountId: String? = null,
        region: DemographicsRegion = DemographicsRegion.US,
        optedIntoTrial: Boolean = false,
        response: DemographicsResponse = DemographicsResponseFixtures.create(),
        pending: Set<DemographicsField> = emptySet(),
    ) = DemographicsState(
        accountId = accountId,
        region = region,
        optedIntoTrial = optedIntoTrial,
        response = response,
        pending = pending,
    )
}

/**
 * Builds [DemographicsLayoutInput]s; by default nothing is answered and nothing is open.
 */
object DemographicsLayoutInputFixtures {
    @Suppress("detekt:LongParameterList")
    fun create(
        scope: CoroutineScope,
        actionSink: ActionSink<DemographicsAction>,
        region: DemographicsRegion = DemographicsRegion.US,
        response: StateFlow<DemographicsResponse> = MutableStateFlow(DemographicsResponseFixtures.create()),
        completion: StateFlow<Map<DemographicsField, DemographicsCompletion>> = MutableStateFlow(emptyMap()),
        menu: StateFlow<DemographicsMenu?> = MutableStateFlow(null),
        datePicker: StateFlow<DemographicsDatePicker?> = MutableStateFlow(null),
        monthYearPicker: StateFlow<DemographicsMonthYearPicker?> = MutableStateFlow(null),
    ) = DemographicsLayoutInput(
        region = region,
        response = response,
        completion = completion,
        menu = menu,
        datePicker = datePicker,
        monthYearPicker = monthYearPicker,
        scaffoldState = GroveScaffoldState(),
        scope = scope,
        actionSink = actionSink,
    )
}

/**
 * Builds the [DemographicsCatalog] of the shipped questions, regions and conditions.
 */
object DemographicsCatalogFixtures {
    fun create(): DemographicsCatalog = DemographicsCatalogImpl(
        options = DemographicsOptionsProviderImpl(),
        regions = DemographicsRegionsProviderImpl(),
        conditions = DemographicsConditionsProviderImpl(),
    )
}
