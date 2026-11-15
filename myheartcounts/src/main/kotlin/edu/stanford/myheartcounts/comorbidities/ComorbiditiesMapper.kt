//
// This source file is part of the My Heart Counts Android open-source project
//
// SPDX-FileCopyrightText: 2026 Stanford University and the project authors (see CONTRIBUTORS.md)
//
// SPDX-License-Identifier: MIT

package edu.stanford.myheartcounts.comorbidities

import edu.stanford.myheartcounts.MHCStrings
import edu.stanford.myheartcounts.comorbidities.ui.ComorbiditiesScreenContent
import edu.stanford.myheartcounts.model.demographics.Comorbidities
import edu.stanford.myheartcounts.ui.choice.ChoiceFollowUpRow
import edu.stanford.myheartcounts.ui.choice.ChoiceRow
import edu.stanford.myheartcounts.ui.choice.MonthYearPicker
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import org.grovealliance.ui.ActionSink
import org.grovealliance.ui.StringResource
import org.grovealliance.ui.groveAppBar

/**
 * Builds the comorbidities screen.
 */
interface ComorbiditiesMapper {

    /**
     * Maps [input] into the screen: the primary and the secondary conditions, then "None" while no
     * condition is chosen. Its sections follow [ComorbiditiesInput.comorbidities], so the result stays
     * valid as the comorbidities change.
     */
    fun map(input: ComorbiditiesInput): ComorbiditiesScreenContent

    /**
     * Maps the picker of the date of diagnosis of [condition], opening on its current date and
     * offering the past hundred years.
     */
    fun mapDiagnosisDatePicker(condition: String, input: ComorbiditiesInput): MonthYearPicker
}

/**
 * The data the comorbidities screen is built from.
 *
 * @param comorbidities the comorbidities as currently edited, `null` while none were reported
 * @param diagnosisDatePicker the date of diagnosis picker currently open
 * @param scope the scope the screen's state is shared in
 * @param actionSink receives every interaction with the screen
 */
data class ComorbiditiesInput(
    val comorbidities: StateFlow<Comorbidities?>,
    val diagnosisDatePicker: StateFlow<MonthYearPicker?>,
    val scope: CoroutineScope,
    val actionSink: ActionSink<ComorbiditiesAction>,
)

/**
 * Default [ComorbiditiesMapper] implementation, offering the conditions of [conditionsProvider] as
 * [rules] decide and presenting their dates of diagnosis through [diagnosisDateMapper].
 */
class ComorbiditiesMapperImpl(
    private val conditionsProvider: ComorbiditiesConditionsProvider,
    private val diagnosisDateMapper: DiagnosisDateMapper,
    private val rules: ComorbiditiesRules,
) : ComorbiditiesMapper {

    override fun map(input: ComorbiditiesInput) = ComorbiditiesScreenContent(
        appBar = groveAppBar {
            back { input.actionSink.push(ComorbiditiesAction.BackSelected) }
            title(MHCStrings.comorbidities_title)
        },
        sections = input.comorbidities
            .map { sections(comorbidities = it, input = input) }
            .stateIn(
                scope = input.scope,
                started = SharingStarted.Eagerly,
                initialValue = sections(comorbidities = input.comorbidities.value, input = input),
            ),
        diagnosisDatePicker = input.diagnosisDatePicker,
    )

    override fun mapDiagnosisDatePicker(condition: String, input: ComorbiditiesInput): MonthYearPicker =
        diagnosisDateMapper.mapPicker(
            condition = condition,
            date = input.comorbidities.value?.entries?.get(condition).orEmpty(),
            onChange = { date ->
                input.actionSink.push(
                    ComorbiditiesAction.DiagnosisDateChanged(
                        condition = condition,
                        date = date,
                    ),
                )
            },
            onDismiss = { input.actionSink.push(ComorbiditiesAction.DiagnosisDatePickerDismissed) },
        )

    private fun sections(comorbidities: Comorbidities?, input: ComorbiditiesInput): List<ComorbiditiesScreenContent.Section> {
        val entries = comorbidities?.entries
        val groups = conditionsProvider.conditions()
            .groupBy { it.group }
            .values
            .map { group ->
                ComorbiditiesScreenContent.Section(
                    conditions = group.map {
                        ChoiceRow(
                            icon = null,
                            title = it.title,
                            subtitle = it.subtitle,
                            detail = null,
                            isSelected = entries?.containsKey(it.id) == true,
                            followUp = diagnosis(
                                condition = it.id,
                                date = entries?.get(it.id).orEmpty(),
                                input = input,
                            ),
                            onClick = { input.actionSink.push(ComorbiditiesAction.ConditionSelected(condition = it.id)) },
                        )
                    },
                )
            }
        val none = ComorbiditiesScreenContent.Section(
            conditions = listOf(
                ChoiceRow(
                    icon = null,
                    title = StringResource(MHCStrings.comorbidities_none),
                    subtitle = null,
                    detail = null,
                    isSelected = rules.isNone(current = comorbidities),
                    followUp = null,
                    onClick = { input.actionSink.push(ComorbiditiesAction.NoneSelected) },
                ),
            ),
        )
        return if (rules.offersNone(current = comorbidities)) groups + none else groups
    }

    private fun diagnosis(condition: String, date: String, input: ComorbiditiesInput) = ChoiceFollowUpRow(
        label = StringResource(MHCStrings.comorbidities_since),
        value = diagnosisDateMapper.mapText(date = date),
        isSet = date.isNotEmpty(),
        onClick = { input.actionSink.push(ComorbiditiesAction.DiagnosisDateSelected(condition = condition)) },
    )
}
