//
// This source file is part of the My Heart Counts Android open-source project
//
// SPDX-FileCopyrightText: 2026 Stanford University and the project authors (see CONTRIBUTORS.md)
//
// SPDX-License-Identifier: MIT

package edu.stanford.myheartcounts.comorbidities

import edu.stanford.myheartcounts.MHCStrings
import edu.stanford.myheartcounts.comorbidities.ComorbiditiesConditionsProvider.Condition
import edu.stanford.myheartcounts.comorbidities.ComorbiditiesConditionsProvider.Group
import org.grovealliance.ui.StringResource

/**
 * Provides the conditions a participant can report as comorbidities, the primary ones first.
 */
interface ComorbiditiesConditionsProvider {

    /**
     * The reportable conditions, in display order.
     */
    fun conditions(): List<Condition>

    /**
     * A reportable condition, identified by [id] in the participant's comorbidities, with an optional
     * [subtitle] explaining it.
     */
    data class Condition(
        val id: String,
        val title: StringResource,
        val subtitle: StringResource? = null,
        val group: Group,
    )

    /**
     * The group a [Condition] is presented in.
     */
    enum class Group {
        Primary,
        Secondary,
    }
}

/**
 * Default [ComorbiditiesConditionsProvider] implementation.
 */
class ComorbiditiesConditionsProviderImpl : ComorbiditiesConditionsProvider {

    override fun conditions(): List<Condition> = conditions

    private val conditions = listOf(
        Condition(
            id = "diabetes",
            title = StringResource(MHCStrings.comorbidities_condition_diabetes),
            group = Group.Primary,
        ),
        Condition(
            id = "heartFailure",
            title = StringResource(MHCStrings.comorbidities_condition_heartfailure),
            group = Group.Primary,
        ),
        Condition(
            id = "coronaryArteryDisease",
            title = StringResource(MHCStrings.comorbidities_condition_coronaryarterydisease),
            group = Group.Primary,
        ),
        Condition(
            id = "pulmonaryArterialHypertension",
            title = StringResource(MHCStrings.comorbidities_condition_pulmonaryarterialhypertension),
            group = Group.Primary,
        ),
        Condition(
            id = "adultCongenitalHeartDisease",
            title = StringResource(MHCStrings.comorbidities_condition_adultcongenitalheartdisease),
            group = Group.Primary,
        ),
        Condition(
            id = "congenitalHeartDisease2",
            title = StringResource(MHCStrings.comorbidities_condition_congenitalheartdisease2),
            group = Group.Primary,
        ),
        Condition(
            id = "abdominalAorticAneurysm",
            title = StringResource(MHCStrings.comorbidities_condition_abdominalaorticaneurysm),
            group = Group.Secondary,
        ),
        Condition(
            id = "angina",
            title = StringResource(MHCStrings.comorbidities_condition_angina),
            group = Group.Secondary,
        ),
        Condition(
            id = "aFib",
            title = StringResource(MHCStrings.comorbidities_condition_afib),
            group = Group.Secondary,
        ),
        Condition(
            id = "carotidArteryBlockageStenosis",
            title = StringResource(MHCStrings.comorbidities_condition_carotidarteryblockagestenosis),
            group = Group.Secondary,
        ),
        Condition(
            id = "carotidArterySurgeryOrStent",
            title = StringResource(MHCStrings.comorbidities_condition_carotidarterysurgeryorstent),
            group = Group.Secondary,
        ),
        Condition(
            id = "chronicKidneyDisease",
            title = StringResource(MHCStrings.comorbidities_condition_chronickidneydisease),
            subtitle = StringResource(MHCStrings.comorbidities_condition_chronickidneydisease_detail),
            group = Group.Secondary,
        ),
        Condition(
            id = "congenitalHeartDefect",
            title = StringResource(MHCStrings.comorbidities_condition_congenitalheartdefect),
            group = Group.Secondary,
        ),
        Condition(
            id = "coronaryBlockageStenosis",
            title = StringResource(MHCStrings.comorbidities_condition_coronaryblockagestenosis),
            group = Group.Secondary,
        ),
        Condition(
            id = "coronaryStentAngioplasty",
            title = StringResource(MHCStrings.comorbidities_condition_coronarystentangioplasty),
            group = Group.Secondary,
        ),
        Condition(
            id = "erectileDysfunction",
            title = StringResource(MHCStrings.comorbidities_condition_erectiledysfunction),
            group = Group.Secondary,
        ),
        Condition(
            id = "heartAttackMyocardialInfarction",
            title = StringResource(MHCStrings.comorbidities_condition_heartattackmyocardialinfarction),
            group = Group.Secondary,
        ),
        Condition(
            id = "heartBypassSurgery",
            title = StringResource(MHCStrings.comorbidities_condition_heartbypasssurgery),
            group = Group.Secondary,
        ),
        Condition(
            id = "heartFailureOrCHF",
            title = StringResource(MHCStrings.comorbidities_condition_heartfailureorchf),
            group = Group.Secondary,
        ),
        Condition(
            id = "highCoronaryCalciumScore",
            title = StringResource(MHCStrings.comorbidities_condition_highcoronarycalciumscore),
            group = Group.Secondary,
        ),
        Condition(
            id = "migraines",
            title = StringResource(MHCStrings.comorbidities_condition_migraines),
            group = Group.Secondary,
        ),
        Condition(
            id = "otherAutoImmuneDisease",
            title = StringResource(MHCStrings.comorbidities_condition_otherautoimmunedisease),
            group = Group.Secondary,
        ),
        Condition(
            id = "peripheralVascularDisease",
            title = StringResource(MHCStrings.comorbidities_condition_peripheralvasculardisease),
            subtitle = StringResource(MHCStrings.comorbidities_condition_peripheralvasculardisease_detail),
            group = Group.Secondary,
        ),
        Condition(
            id = "pulmonaryHypertension",
            title = StringResource(MHCStrings.comorbidities_condition_pulmonaryhypertension),
            group = Group.Secondary,
        ),
        Condition(
            id = "dilatedCardiomyopathy",
            title = StringResource(MHCStrings.comorbidities_condition_dilatedcardiomyopathy),
            group = Group.Secondary,
        ),
        Condition(
            id = "hypertrophicCardiomyopathy",
            title = StringResource(MHCStrings.comorbidities_condition_hypertrophiccardiomyopathy),
            group = Group.Secondary,
        ),
        Condition(
            id = "raynaudsPhenomenon",
            title = StringResource(MHCStrings.comorbidities_condition_raynaudsphenomenon),
            group = Group.Secondary,
        ),
        Condition(
            id = "rheumatoidArthritis",
            title = StringResource(MHCStrings.comorbidities_condition_rheumatoidarthritis),
            group = Group.Secondary,
        ),
        Condition(
            id = "stroke",
            title = StringResource(MHCStrings.comorbidities_condition_stroke),
            group = Group.Secondary,
        ),
        Condition(
            id = "systemicLupusErythematosus",
            title = StringResource(MHCStrings.comorbidities_condition_systemiclupuserythematosus),
            group = Group.Secondary,
        ),
        Condition(
            id = "systemicSclerosisOrScleroderma",
            title = StringResource(MHCStrings.comorbidities_condition_systemicsclerosisorscleroderma),
            group = Group.Secondary,
        ),
        Condition(
            id = "transientIschemicAttack",
            title = StringResource(MHCStrings.comorbidities_condition_transientischemicattack),
            group = Group.Secondary,
        ),
    )
}
