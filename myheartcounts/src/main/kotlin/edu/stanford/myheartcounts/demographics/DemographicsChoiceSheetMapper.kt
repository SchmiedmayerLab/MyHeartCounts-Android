//
// This source file is part of the My Heart Counts Android open-source project
//
// SPDX-FileCopyrightText: 2026 Stanford University and the project authors (see CONTRIBUTORS.md)
//
// SPDX-License-Identifier: MIT

package edu.stanford.myheartcounts.demographics

import edu.stanford.myheartcounts.MHCStrings
import edu.stanford.myheartcounts.comorbidities.ComorbiditiesConditionsProvider
import edu.stanford.myheartcounts.comorbidities.ComorbiditiesRules
import edu.stanford.myheartcounts.comorbidities.DiagnosisDateMapper
import edu.stanford.myheartcounts.demographics.DemographicsChoiceGroupMapper.Choice
import edu.stanford.myheartcounts.demographics.DemographicsChoiceGroupMapper.Group
import edu.stanford.myheartcounts.demographics.ui.DemographicsChoiceSheet
import edu.stanford.myheartcounts.model.demographics.Comorbidities
import edu.stanford.myheartcounts.model.demographics.EducationStatusUK
import edu.stanford.myheartcounts.model.demographics.EducationStatusUS
import edu.stanford.myheartcounts.model.demographics.HouseholdIncomeUK
import edu.stanford.myheartcounts.model.demographics.HouseholdIncomeUS
import edu.stanford.myheartcounts.model.demographics.LatinoStatusOption
import edu.stanford.myheartcounts.model.demographics.RaceEthnicity
import edu.stanford.myheartcounts.model.demographics.StageOfChangeOption
import edu.stanford.myheartcounts.model.demographics.UKRegion
import edu.stanford.myheartcounts.model.demographics.USRegion
import edu.stanford.myheartcounts.ui.choice.ChoiceFollowUpRow
import edu.stanford.myheartcounts.ui.choice.MonthYearPicker
import org.grovealliance.ui.StringResource

/**
 * Builds the [DemographicsChoiceSheet] answering each question chosen from a list. Every sheet marks
 * the current answers in [DemographicsLayoutInput.response] and reports through
 * [DemographicsLayoutInput.actionSink]; a sheet allowing a single answer closes once it is chosen.
 */
interface DemographicsChoiceSheetMapper {

    /**
     * Maps the race / ethnicity sheet, allowing several answers or declining to state.
     */
    fun mapRaceEthnicitySheet(input: DemographicsLayoutInput): DemographicsChoiceSheet

    /**
     * Maps the Hispanic / Latino sheet.
     */
    fun mapLatinoStatusSheet(input: DemographicsLayoutInput): DemographicsChoiceSheet

    /**
     * Maps the comorbidities sheet, allowing several conditions or none.
     */
    fun mapComorbiditiesSheet(input: DemographicsLayoutInput): DemographicsChoiceSheet

    /**
     * Maps the sheet of the states and territories, or of the UK nations, depending on the form's
     * region. Choosing a UK nation asks for its county.
     */
    fun mapRegionSheet(input: DemographicsLayoutInput): DemographicsChoiceSheet

    /**
     * Maps the sheet of the counties of the UK nation identified by [nation].
     */
    fun mapUKCountySheet(nation: String, input: DemographicsLayoutInput): DemographicsChoiceSheet

    /**
     * Maps the education level sheet of the form's region.
     */
    fun mapEducationSheet(input: DemographicsLayoutInput): DemographicsChoiceSheet

    /**
     * Maps the household income sheet of the form's region.
     */
    fun mapIncomeSheet(input: DemographicsLayoutInput): DemographicsChoiceSheet

    /**
     * Maps the stage of change sheet, each clause on a card of its own.
     */
    fun mapStageOfChangeSheet(input: DemographicsLayoutInput): DemographicsChoiceSheet

    /**
     * Maps the referral source sheet.
     */
    fun mapReferralSourceSheet(input: DemographicsLayoutInput): DemographicsChoiceSheet

    /**
     * Maps the picker of the date of diagnosis of the comorbidity [condition], opening on its current
     * date and offering the past hundred years.
     */
    fun mapDiagnosisDatePicker(condition: String, input: DemographicsLayoutInput): MonthYearPicker
}

/**
 * Default [DemographicsChoiceSheetMapper] implementation, presenting the options of [catalog] as the
 * answers [optionIdMapper] identify, in sheets mapped by [groupMapper] and changed as [rules] decide.
 */
class DemographicsChoiceSheetMapperImpl(
    private val catalog: DemographicsCatalog,
    private val optionIdMapper: DemographicsOptionIdMapper,
    private val groupMapper: DemographicsChoiceGroupMapper,
    private val rules: DemographicsRules,
    private val comorbiditiesRules: ComorbiditiesRules,
    private val diagnosisDateMapper: DiagnosisDateMapper,
) : DemographicsChoiceSheetMapper {

    override fun mapRaceEthnicitySheet(input: DemographicsLayoutInput): DemographicsChoiceSheet {
        val choices = groupMapper.mapChoices(field = DemographicsField.RaceEthnicity) { RaceEthnicity(rawValue = it.toLong()) }
        val (declined, races) = choices.partition { it.value == RaceEthnicity.PREFER_NOT_TO_STATE }
        return groupMapper.mapSheet(
            title = MHCStrings.demographics_field_raceethnicity,
            groups = listOf(
                Group(
                    choices = races,
                    footer = StringResource(MHCStrings.demographics_race_footer),
                ),
                Group(choices = declined),
            ),
            isSelected = { response, race -> response.raceEthnicity?.contains(race) == true },
            change = { response, race ->
                DemographicsAction.RaceEthnicityChanged(
                    value = rules.toggled(
                        current = response.raceEthnicity,
                        race = race,
                    ),
                )
            },
            closesOnSelect = false,
            input = input,
        )
    }

    override fun mapLatinoStatusSheet(input: DemographicsLayoutInput) = groupMapper.mapDeclinableSheet(
        title = MHCStrings.demographics_label_latino_status,
        choices = groupMapper.mapChoices(field = DemographicsField.LatinoStatus) { id ->
            LatinoStatusOption.entries.firstOrNull { optionIdMapper.mapId(value = it) == id }
        },
        declined = LatinoStatusOption.PREFER_NOT_TO_STATE,
        current = { it.latinoStatus },
        change = { DemographicsAction.LatinoStatusChanged(value = it) },
        input = input,
    )

    override fun mapComorbiditiesSheet(input: DemographicsLayoutInput): DemographicsChoiceSheet {
        val (primary, secondary) = catalog.conditions()
            .partition { it.group == ComorbiditiesConditionsProvider.Group.Primary }
            .let { (primary, secondary) -> choices(conditions = primary) to choices(conditions = secondary) }
        val none = Choice(
            value = NO_CONDITIONS,
            option = DemographicsOption(
                id = NO_CONDITIONS,
                title = StringResource(MHCStrings.comorbidities_none),
            ),
        )
        return groupMapper.mapSheet(
            title = MHCStrings.comorbidities_title,
            groups = listOf(
                Group(choices = primary),
                Group(choices = secondary),
                Group(choices = listOf(none)),
            ),
            isSelected = { response, condition ->
                if (condition == NO_CONDITIONS) {
                    comorbiditiesRules.isNone(current = response.comorbidities)
                } else {
                    response.comorbidities?.entries?.containsKey(condition) == true
                }
            },
            change = { response, condition ->
                DemographicsAction.ComorbiditiesChanged(
                    value = if (condition == NO_CONDITIONS) {
                        Comorbidities(entries = emptyMap())
                    } else {
                        comorbiditiesRules.toggled(
                            current = response.comorbidities,
                            condition = condition,
                        )
                    },
                )
            },
            closesOnSelect = false,
            input = input,
            followUp = { response, condition ->
                if (condition == NO_CONDITIONS) null else diagnosisFollowUp(condition = condition, response = response, input = input)
            },
            isOffered = { response, condition ->
                condition != NO_CONDITIONS || comorbiditiesRules.offersNone(current = response.comorbidities)
            },
        )
    }

    private fun choices(conditions: List<ComorbiditiesConditionsProvider.Condition>) = conditions.map {
        Choice(
            value = it.id,
            option = DemographicsOption(
                id = it.id,
                title = it.title,
                subtitle = it.subtitle,
            ),
        )
    }

    private fun usRegionSheet(input: DemographicsLayoutInput): DemographicsChoiceSheet {
        val regions = groupMapper.mapChoices(field = DemographicsField.USRegion) { id ->
            USRegion.entries.firstOrNull { optionIdMapper.mapId(value = it) == id }
        }
            .map { it.copy(detail = StringResource(it.option.id)) }
        val (territories, states) = regions.partition { it.value in TERRITORIES }
        val notSet = Choice(
            value = USRegion.NOT_SET,
            option = DemographicsOption(
                id = optionIdMapper.mapId(value = USRegion.NOT_SET),
                title = StringResource(MHCStrings.demographics_value_not_set),
            ),
        )
        return groupMapper.mapSingleSheet(
            title = MHCStrings.demographics_us_region_sheet_title,
            groups = listOf(
                Group(choices = states),
                Group(
                    choices = territories,
                    header = StringResource(MHCStrings.demographics_us_territories),
                ),
                Group(choices = listOf(notSet)),
            ),
            current = { it.usRegion },
            change = { DemographicsAction.USRegionChanged(value = it) },
            input = input,
        )
    }

    private fun ukNationSheet(input: DemographicsLayoutInput) = groupMapper.mapSheet(
        title = MHCStrings.demographics_uk_region_sheet_title,
        groups = listOf(
            Group(
                choices = groupMapper.mapChoices(field = DemographicsField.UKRegion) { it },
                header = StringResource(MHCStrings.demographics_label_uk_region),
            ),
        ),
        isSelected = { _, _ -> false },
        change = null,
        detail = { response, nation ->
            response.ukRegion
                ?.takeIf { optionIdMapper.mapNationId(region = it) == nation }
                ?.county
                ?.let { StringResource(it.name) }
        },
        closesOnSelect = false,
        onSelect = { nation -> input.actionSink.push(DemographicsAction.UKNationSelected(nation = nation)) },
        input = input,
    )

    override fun mapUKCountySheet(nation: String, input: DemographicsLayoutInput) = groupMapper.mapSingleSheet(
        title = MHCStrings.demographics_uk_region_sheet_title,
        groups = listOf(
            Group(
                choices = catalog.counties(nation = nation).mapNotNull { name ->
                    optionIdMapper.mapUKRegion(
                        nationId = nation,
                        county = UKRegion.County(name = name),
                    )?.let {
                        Choice(
                            value = it,
                            option = DemographicsOption(
                                id = name,
                                title = StringResource(name),
                            ),
                        )
                    }
                },
                header = catalog.options(DemographicsField.UKRegion).firstOrNull { it.id == nation }?.title,
            ),
        ),
        current = { it.ukRegion },
        change = { DemographicsAction.UKRegionChanged(value = it) },
        input = input,
    )

    override fun mapRegionSheet(input: DemographicsLayoutInput) = when (input.region) {
        DemographicsRegion.US -> usRegionSheet(input = input)
        DemographicsRegion.UK -> ukNationSheet(input = input)
    }

    override fun mapEducationSheet(input: DemographicsLayoutInput) = when (input.region) {
        DemographicsRegion.US -> groupMapper.mapDeclinableSheet(
            title = MHCStrings.demographics_label_education,
            choices = groupMapper.mapChoices(field = DemographicsField.USEducation) { id ->
                EducationStatusUS.entries.firstOrNull { optionIdMapper.mapId(value = it) == id }
            },
            declined = EducationStatusUS.PREFER_NOT_TO_STATE,
            current = { it.educationUS },
            change = { DemographicsAction.EducationUSChanged(value = it) },
            input = input,
        )
        DemographicsRegion.UK -> groupMapper.mapDeclinableSheet(
            title = MHCStrings.demographics_label_education,
            choices = groupMapper.mapChoices(field = DemographicsField.UKEducation) { id ->
                EducationStatusUK.entries.firstOrNull { optionIdMapper.mapId(value = it) == id }
            },
            declined = EducationStatusUK.PREFER_NOT_TO_STATE,
            current = { it.educationUK },
            change = { DemographicsAction.EducationUKChanged(value = it) },
            input = input,
        )
    }

    override fun mapIncomeSheet(input: DemographicsLayoutInput) = when (input.region) {
        DemographicsRegion.US -> groupMapper.mapDeclinableSheet(
            title = MHCStrings.demographics_label_income,
            choices = groupMapper.mapChoices(field = DemographicsField.USIncome) { id ->
                HouseholdIncomeUS.entries.firstOrNull { optionIdMapper.mapId(value = it) == id }
            },
            declined = HouseholdIncomeUS.PREFER_NOT_TO_STATE,
            current = { it.householdIncomeUS },
            change = { DemographicsAction.HouseholdIncomeUSChanged(value = it) },
            input = input,
        )
        DemographicsRegion.UK -> groupMapper.mapDeclinableSheet(
            title = MHCStrings.demographics_label_income,
            choices = groupMapper.mapChoices(field = DemographicsField.UKIncome) { id ->
                HouseholdIncomeUK.entries.firstOrNull { optionIdMapper.mapId(value = it) == id }
            },
            declined = HouseholdIncomeUK.PREFER_NOT_TO_STATE,
            current = { it.householdIncomeUK },
            change = { DemographicsAction.HouseholdIncomeUKChanged(value = it) },
            input = input,
        )
    }

    override fun mapStageOfChangeSheet(input: DemographicsLayoutInput) = groupMapper.mapSingleSheet(
        title = MHCStrings.demographics_field_stageofchange,
        prompt = StringResource(MHCStrings.demographics_stage_of_change_prompt),
        groups = groupMapper.mapChoices(field = DemographicsField.StageOfChange) { id ->
            StageOfChangeOption.entries.firstOrNull { optionIdMapper.mapId(value = it) == id }
        }.map { Group(choices = listOf(it)) },
        current = { it.stageOfChange },
        change = { DemographicsAction.StageOfChangeChanged(value = it) },
        input = input,
    )

    override fun mapReferralSourceSheet(input: DemographicsLayoutInput) = groupMapper.mapSingleSheet(
        title = MHCStrings.demographics_label_referral_source,
        prompt = StringResource(MHCStrings.demographics_referral_source_prompt),
        groups = listOf(Group(choices = groupMapper.mapChoices(field = DemographicsField.ReferralSource) { it })),
        current = { it.referralSource },
        change = { DemographicsAction.ReferralSourceChanged(value = it) },
        input = input,
    )

    override fun mapDiagnosisDatePicker(condition: String, input: DemographicsLayoutInput): MonthYearPicker =
        diagnosisDateMapper.mapPicker(
            condition = condition,
            date = input.response.value.comorbidities?.entries?.get(condition).orEmpty(),
            onChange = { date ->
                input.actionSink.push(
                    DemographicsAction.ComorbiditiesChanged(
                        value = comorbiditiesRules.diagnosed(
                            current = input.response.value.comorbidities,
                            condition = condition,
                            date = date,
                        ),
                    ),
                )
            },
            onDismiss = { input.actionSink.push(DemographicsAction.MonthYearPickerDismissed) },
        )

    private fun diagnosisFollowUp(
        condition: String,
        response: DemographicsResponse,
        input: DemographicsLayoutInput,
    ): ChoiceFollowUpRow {
        val diagnosis = response.comorbidities?.entries?.get(condition).orEmpty()
        return ChoiceFollowUpRow(
            label = StringResource(MHCStrings.comorbidities_since),
            value = diagnosisDateMapper.mapText(date = diagnosis),
            isSet = diagnosis.isNotEmpty(),
            onClick = { input.actionSink.push(DemographicsAction.DiagnosisDateSelected(condition = condition)) },
        )
    }

    private companion object {
        const val NO_CONDITIONS = "none"
        val TERRITORIES = setOf(
            USRegion.AMERICAN_SAMOA,
            USRegion.GUAM,
            USRegion.NORTHERN_MARIANA_ISLANDS,
            USRegion.PUERTO_RICO,
            USRegion.TRUST_TERRITORIES,
            USRegion.VIRGIN_ISLANDS,
        )
    }
}
