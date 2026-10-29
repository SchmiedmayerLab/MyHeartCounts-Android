//
// This source file is part of the My Heart Counts Android open-source project
//
// SPDX-FileCopyrightText: 2026 Stanford University and the project authors (see CONTRIBUTORS.md)
//
// SPDX-License-Identifier: MIT

package edu.stanford.myheartcounts.demographics

import androidx.annotation.StringRes
import edu.stanford.myheartcounts.MHCStrings
import edu.stanford.myheartcounts.demographics.ui.DemographicsDatePicker
import edu.stanford.myheartcounts.demographics.ui.DemographicsLayout
import edu.stanford.myheartcounts.demographics.ui.DemographicsMenu
import edu.stanford.myheartcounts.demographics.ui.DemographicsMonthYearPicker
import edu.stanford.myheartcounts.demographics.ui.HealthImportCard
import edu.stanford.myheartcounts.model.demographics.RaceEthnicity
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import org.grovealliance.ui.ActionSink
import org.grovealliance.ui.GroveScaffoldState
import org.grovealliance.ui.StringResource

/**
 * What the [DemographicsLayout] is built from.
 *
 * @param region the regional form asked
 * @param response the participant's answers; every row follows it
 * @param completion whether each question is answered and needs to be; every row follows it
 * @param menu the menu of answers currently open, if any; it opens on the row of its question
 * @param datePicker the date picker currently open, if any
 * @param monthYearPicker the month and year picker currently open, if any
 * @param scaffoldState presents the sheets opened from the layout
 * @param scope the scope the rows follow [response] and [completion] in
 * @param actionSink receives every interaction
 */
data class DemographicsLayoutInput(
    val region: DemographicsRegion,
    val response: StateFlow<DemographicsResponse>,
    val completion: StateFlow<Map<DemographicsField, DemographicsCompletion>>,
    val menu: StateFlow<DemographicsMenu?>,
    val datePicker: StateFlow<DemographicsDatePicker?>,
    val monthYearPicker: StateFlow<DemographicsMonthYearPicker?>,
    val scaffoldState: GroveScaffoldState,
    val scope: CoroutineScope,
    val actionSink: ActionSink<DemographicsAction>,
)

/**
 * Builds the [DemographicsLayout].
 */
interface DemographicsLayoutMapper {

    /**
     * Maps [input] into the layout of its region's questions. Its rows follow
     * [DemographicsLayoutInput.response] and [DemographicsLayoutInput.completion], so the result
     * stays valid as the answers change.
     */
    fun map(input: DemographicsLayoutInput): DemographicsLayout
}

/**
 * Default [DemographicsLayoutMapper] implementation, reading answers in the wording of [catalog] and
 * [answerMapper] and asking the questions [rules] select.
 */
class DemographicsLayoutMapperImpl(
    private val catalog: DemographicsCatalog,
    private val optionIdMapper: DemographicsOptionIdMapper,
    private val answerMapper: DemographicsAnswerMapper,
    private val rules: DemographicsRules,
) : DemographicsLayoutMapper {

    override fun map(input: DemographicsLayoutInput) = DemographicsLayout(
        healthImport = HealthImportCard(
            title = StringResource(MHCStrings.demographics_health_import_title),
            description = StringResource(MHCStrings.demographics_health_import_description),
            onClick = { input.actionSink.push(DemographicsAction.HealthImportSelected) },
        ),
        sections = listOf(
            personal(input = input),
            body(input = input),
            origin(input = input),
            conditions(input = input),
            usBackground(input = input),
            ukBackground(input = input),
            nhs(input = input),
            activity(input = input),
            referral(input = input),
        ).mapNotNull { section ->
            val fields = rules.fields(region = input.region)
            section.rows
                .filter { it.field in fields }
                .takeIf { it.isNotEmpty() }
                ?.let { DemographicsLayout.Section(rows = it) }
        },
        datePicker = input.datePicker,
        scaffoldState = input.scaffoldState,
    )

    private fun personal(input: DemographicsLayoutInput) = DemographicsLayout.Section(
        rows = listOf(
            row(
                field = DemographicsField.DateOfBirth,
                answer = { it.dateOfBirth?.let(answerMapper::mapText) },
                label = MHCStrings.demographics_field_dateofbirth,
                accessory = DemographicsLayout.Accessory.None,
                input = input,
            ),
            row(
                field = DemographicsField.GenderIdentity,
                answer = { it.genderIdentity?.let(answerMapper::mapText) },
                label = MHCStrings.demographics_field_genderidentity,
                accessory = DemographicsLayout.Accessory.Menu,
                input = input,
            ),
            row(
                field = DemographicsField.SexAtBirth,
                answer = { it.biologicalSexAtBirth?.let(answerMapper::mapText) },
                label = MHCStrings.demographics_label_sex_at_birth,
                accessory = DemographicsLayout.Accessory.Menu,
                input = input,
            ),
            row(
                field = DemographicsField.BloodType,
                answer = { it.bloodType?.let(answerMapper::mapText) },
                label = MHCStrings.demographics_field_bloodtype,
                accessory = DemographicsLayout.Accessory.Menu,
                input = input,
            ),
        ),
    )

    private fun body(input: DemographicsLayoutInput) = DemographicsLayout.Section(
        rows = listOf(
            row(
                field = DemographicsField.Height,
                answer = { it.heightInCm?.let(answerMapper::mapCentimetresText) },
                label = MHCStrings.demographics_field_height,
                accessory = DemographicsLayout.Accessory.None,
                input = input,
            ),
            row(
                field = DemographicsField.Weight,
                answer = { it.weightInKg?.let(answerMapper::mapKilogramsText) },
                label = MHCStrings.demographics_field_weight,
                accessory = DemographicsLayout.Accessory.None,
                input = input,
            ),
        ),
    )

    private fun origin(input: DemographicsLayoutInput) = DemographicsLayout.Section(
        rows = listOf(
            row(
                field = DemographicsField.RaceEthnicity,
                answer = { it.raceEthnicity?.let(::raceEthnicityText) },
                label = MHCStrings.demographics_field_raceethnicity,
                unanswered = MHCStrings.demographics_value_no_selection,
                accessory = DemographicsLayout.Accessory.Disclosure,
                input = input,
            ),
            row(
                field = DemographicsField.LatinoStatus,
                answer = { response ->
                    response.latinoStatus?.let {
                        optionText(
                            field = DemographicsField.LatinoStatus,
                            id = optionIdMapper.mapId(value = it),
                        )
                    }
                },
                label = MHCStrings.demographics_label_latino_status,
                unanswered = MHCStrings.demographics_value_not_set,
                accessory = DemographicsLayout.Accessory.Disclosure,
                input = input,
            ),
        ),
    )

    private fun conditions(input: DemographicsLayoutInput) = DemographicsLayout.Section(
        rows = listOf(
            row(
                field = DemographicsField.Comorbidities,
                answer = { it.comorbidities?.let(answerMapper::mapText) },
                label = MHCStrings.demographics_field_comorbidities,
                unanswered = MHCStrings.demographics_value_not_set,
                accessory = DemographicsLayout.Accessory.Disclosure,
                input = input,
            ),
        ),
    )

    private fun activity(input: DemographicsLayoutInput) = DemographicsLayout.Section(
        rows = listOf(
            row(
                field = DemographicsField.StageOfChange,
                answer = { response ->
                    response.stageOfChange?.let {
                        optionText(
                            field = DemographicsField.StageOfChange,
                            id = optionIdMapper.mapId(value = it),
                        )
                    }
                },
                label = MHCStrings.demographics_field_stageofchange,
                unanswered = MHCStrings.demographics_value_no_selection,
                accessory = DemographicsLayout.Accessory.Disclosure,
                input = input,
            ),
        ),
    )

    private fun referral(input: DemographicsLayoutInput) = DemographicsLayout.Section(
        rows = listOf(
            row(
                field = DemographicsField.ReferralSource,
                answer = { response ->
                    response.referralSource?.let { optionText(field = DemographicsField.ReferralSource, id = it) }
                },
                label = MHCStrings.demographics_label_referral_source,
                unanswered = MHCStrings.demographics_value_not_set,
                accessory = DemographicsLayout.Accessory.Disclosure,
                input = input,
            ),
        ),
    )

    private fun usBackground(input: DemographicsLayoutInput) = DemographicsLayout.Section(
        rows = listOf(
            row(
                field = DemographicsField.USRegion,
                answer = { it.usRegion?.let(answerMapper::mapText) },
                label = MHCStrings.demographics_label_us_region,
                unanswered = MHCStrings.demographics_value_no_selection,
                accessory = DemographicsLayout.Accessory.Disclosure,
                input = input,
            ),
            row(
                field = DemographicsField.USEducation,
                answer = { response ->
                    response.educationUS?.let {
                        optionText(
                            field = DemographicsField.USEducation,
                            id = optionIdMapper.mapId(value = it),
                        )
                    }
                },
                label = MHCStrings.demographics_label_education,
                unanswered = MHCStrings.demographics_value_not_set,
                accessory = DemographicsLayout.Accessory.Disclosure,
                input = input,
            ),
            row(
                field = DemographicsField.USIncome,
                answer = { response ->
                    response.householdIncomeUS?.let {
                        optionText(
                            field = DemographicsField.USIncome,
                            id = optionIdMapper.mapId(value = it),
                        )
                    }
                },
                label = MHCStrings.demographics_label_income,
                unanswered = MHCStrings.demographics_value_not_set,
                accessory = DemographicsLayout.Accessory.Disclosure,
                input = input,
            ),
        ),
    )

    private fun ukBackground(input: DemographicsLayoutInput) = DemographicsLayout.Section(
        rows = listOf(
            row(
                field = DemographicsField.UKRegion,
                answer = { it.ukRegion?.let(answerMapper::mapText) },
                label = MHCStrings.demographics_label_uk_region,
                unanswered = MHCStrings.demographics_value_not_set,
                accessory = DemographicsLayout.Accessory.Disclosure,
                input = input,
            ),
            row(
                field = DemographicsField.UKEducation,
                answer = { response ->
                    response.educationUK?.let {
                        optionText(
                            field = DemographicsField.UKEducation,
                            id = optionIdMapper.mapId(value = it),
                        )
                    }
                },
                label = MHCStrings.demographics_label_education,
                unanswered = MHCStrings.demographics_value_not_set,
                accessory = DemographicsLayout.Accessory.Disclosure,
                input = input,
            ),
            row(
                field = DemographicsField.UKIncome,
                answer = { response ->
                    response.householdIncomeUK?.let {
                        optionText(
                            field = DemographicsField.UKIncome,
                            id = optionIdMapper.mapId(value = it),
                        )
                    }
                },
                label = MHCStrings.demographics_label_income,
                unanswered = MHCStrings.demographics_value_not_set,
                accessory = DemographicsLayout.Accessory.Disclosure,
                input = input,
            ),
        ),
    )

    private fun nhs(input: DemographicsLayoutInput) = DemographicsLayout.Section(
        rows = listOf(
            row(
                field = DemographicsField.NHSNumber,
                answer = { response ->
                    response.nhsNumber?.stringValue?.takeIf { it.isNotEmpty() }?.let { StringResource(it) }
                },
                label = MHCStrings.demographics_field_nhsnumber,
                unanswered = MHCStrings.demographics_value_not_set,
                accessory = DemographicsLayout.Accessory.None,
                input = input,
            ),
        ),
    )

    private fun row(
        field: DemographicsField,
        answer: (DemographicsResponse) -> StringResource?,
        @StringRes label: Int,
        accessory: DemographicsLayout.Accessory,
        input: DemographicsLayoutInput,
        @StringRes unanswered: Int = MHCStrings.demographics_value_empty,
    ): DemographicsLayout.Row {
        fun value(
            response: DemographicsResponse,
            completion: Map<DemographicsField, DemographicsCompletion>,
        ) = DemographicsLayout.Value(
            text = answer(response) ?: StringResource(unanswered),
            isOptional = completion[field]?.isRequired == false,
        )

        return DemographicsLayout.Row(
            field = field,
            label = StringResource(label),
            value = combine(input.response, input.completion) { response, completion ->
                value(
                    response = response,
                    completion = completion,
                )
            }.stateIn(
                scope = input.scope,
                started = SharingStarted.Eagerly,
                initialValue = value(
                    response = input.response.value,
                    completion = input.completion.value,
                ),
            ),
            menu = input.menu
                .map { menu -> menu?.takeIf { it.field == field } }
                .stateIn(
                    scope = input.scope,
                    started = SharingStarted.Eagerly,
                    initialValue = input.menu.value?.takeIf { it.field == field },
                ),
            accessory = accessory,
            onClick = { input.actionSink.push(DemographicsAction.QuestionSelected(field = field)) },
        )
    }

    private fun optionText(field: DemographicsField, id: String) =
        catalog.options(field).firstOrNull { it.id == id }?.title
            ?: StringResource(MHCStrings.demographics_value_not_set)

    private fun raceEthnicityText(race: RaceEthnicity) =
        if (RaceEthnicity.PREFER_NOT_TO_STATE in race) {
            optionText(
                field = DemographicsField.RaceEthnicity,
                id = RaceEthnicity.PREFER_NOT_TO_STATE.rawValue.toString(),
            )
        } else {
            StringResource(MHCStrings.demographics_value_selected_count, race.rawValue.countOneBits())
        }
}
