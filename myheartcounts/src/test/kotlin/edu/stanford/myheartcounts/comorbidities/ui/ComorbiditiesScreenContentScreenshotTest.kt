//
// This source file is part of the My Heart Counts Android open-source project
//
// SPDX-FileCopyrightText: 2026 Stanford University and the project authors (see CONTRIBUTORS.md)
//
// SPDX-License-Identifier: MIT

package edu.stanford.myheartcounts.comorbidities.ui

import edu.stanford.myheartcounts.MHCStrings
import edu.stanford.myheartcounts.ui.choice.ChoiceFollowUpRow
import edu.stanford.myheartcounts.ui.choice.ChoiceRow
import kotlinx.coroutines.flow.MutableStateFlow
import org.grovealliance.testing.screenshot.ScreenshotTest
import org.grovealliance.ui.StringResource
import org.grovealliance.ui.groveAppBar
import org.junit.Test

class ComorbiditiesScreenContentScreenshotTest : ScreenshotTest() {
    private val condition = ChoiceRow(
        icon = null,
        title = StringResource(MHCStrings.comorbidities_condition_diabetes),
        subtitle = null,
        detail = null,
        isSelected = false,
        followUp = null,
        onClick = {},
    )
    private val since = ChoiceFollowUpRow(
        label = StringResource(MHCStrings.comorbidities_since),
        value = StringResource(MHCStrings.comorbidities_since_unknown),
        isSet = false,
        onClick = {},
    )
    private val primary = listOf(
        MHCStrings.comorbidities_condition_diabetes,
        MHCStrings.comorbidities_condition_heartfailure,
        MHCStrings.comorbidities_condition_coronaryarterydisease,
        MHCStrings.comorbidities_condition_pulmonaryarterialhypertension,
        MHCStrings.comorbidities_condition_adultcongenitalheartdisease,
        MHCStrings.comorbidities_condition_congenitalheartdisease2,
    ).map { condition.copy(title = StringResource(it)) }
    private val secondary = listOf(
        MHCStrings.comorbidities_condition_abdominalaorticaneurysm,
        MHCStrings.comorbidities_condition_angina,
        MHCStrings.comorbidities_condition_afib,
        MHCStrings.comorbidities_condition_carotidarteryblockagestenosis,
        MHCStrings.comorbidities_condition_carotidarterysurgeryorstent,
    ).map { condition.copy(title = StringResource(it)) } +
        condition.copy(
            title = StringResource(MHCStrings.comorbidities_condition_chronickidneydisease),
            subtitle = StringResource(MHCStrings.comorbidities_condition_chronickidneydisease_detail),
        ) +
        listOf(
            MHCStrings.comorbidities_condition_congenitalheartdefect,
            MHCStrings.comorbidities_condition_coronaryblockagestenosis,
            MHCStrings.comorbidities_condition_coronarystentangioplasty,
            MHCStrings.comorbidities_condition_erectiledysfunction,
            MHCStrings.comorbidities_condition_heartattackmyocardialinfarction,
            MHCStrings.comorbidities_condition_heartbypasssurgery,
            MHCStrings.comorbidities_condition_heartfailureorchf,
            MHCStrings.comorbidities_condition_highcoronarycalciumscore,
            MHCStrings.comorbidities_condition_migraines,
            MHCStrings.comorbidities_condition_otherautoimmunedisease,
        ).map { condition.copy(title = StringResource(it)) } +
        condition.copy(
            title = StringResource(MHCStrings.comorbidities_condition_peripheralvasculardisease),
            subtitle = StringResource(MHCStrings.comorbidities_condition_peripheralvasculardisease_detail),
        ) +
        listOf(
            MHCStrings.comorbidities_condition_pulmonaryhypertension,
            MHCStrings.comorbidities_condition_dilatedcardiomyopathy,
            MHCStrings.comorbidities_condition_hypertrophiccardiomyopathy,
            MHCStrings.comorbidities_condition_raynaudsphenomenon,
            MHCStrings.comorbidities_condition_rheumatoidarthritis,
            MHCStrings.comorbidities_condition_stroke,
            MHCStrings.comorbidities_condition_systemiclupuserythematosus,
            MHCStrings.comorbidities_condition_systemicsclerosisorscleroderma,
            MHCStrings.comorbidities_condition_transientischemicattack,
        ).map { condition.copy(title = StringResource(it)) }
    private val none = condition.copy(title = StringResource(MHCStrings.comorbidities_none))
    private val chosen = primary.toMutableList().apply {
        this[0] = this[0].copy(
            isSelected = true,
            followUp = since.copy(
                value = StringResource("Mar 2020"),
                isSet = true,
            ),
        )
        this[1] = this[1].copy(
            isSelected = true,
            followUp = since.copy(
                value = StringResource("2018"),
                isSet = true,
            ),
        )
        this[2] = this[2].copy(
            isSelected = true,
            followUp = since,
        )
    }
    private val base = ComorbiditiesScreenContent(
        appBar = groveAppBar {
            back {}
            title(MHCStrings.comorbidities_title)
        },
        sections = MutableStateFlow(
            listOf(
                ComorbiditiesScreenContent.Section(conditions = primary),
                ComorbiditiesScreenContent.Section(conditions = secondary),
                ComorbiditiesScreenContent.Section(conditions = listOf(none)),
            ),
        ),
        diagnosisDatePicker = MutableStateFlow(null),
    )

    @Test
    fun `ComorbiditiesScreenContent nothing reported`() {
        // when / then
        screenshot { base.Content() }
    }

    @Test
    fun `ComorbiditiesScreenContent conditions chosen`() {
        // given
        val content = base.copy(
            sections = MutableStateFlow(
                listOf(
                    ComorbiditiesScreenContent.Section(conditions = chosen),
                    ComorbiditiesScreenContent.Section(conditions = secondary),
                ),
            ),
        )

        // when / then
        screenshot { content.Content() }
    }
}
