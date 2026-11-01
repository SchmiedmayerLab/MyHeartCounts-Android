//
// This source file is part of the My Heart Counts Android open-source project
//
// SPDX-FileCopyrightText: 2026 Stanford University and the project authors (see CONTRIBUTORS.md)
//
// SPDX-License-Identifier: MIT

package edu.stanford.myheartcounts.demographics

import edu.stanford.myheartcounts.MHCStrings
import org.grovealliance.ui.StringResource

/**
 * Provides the conditions a participant can report, the primary ones first.
 */
interface DemographicsConditionsProvider {

    /**
     * The reportable conditions, each grouped as primary or secondary.
     */
    fun conditions(): List<DemographicsOption>
}

/**
 * Default [DemographicsConditionsProvider] implementation.
 */
class DemographicsConditionsProviderImpl : DemographicsConditionsProvider {

    override fun conditions(): List<DemographicsOption> = conditions

    private val conditions = listOf(
        DemographicsOption(
            id = "diabetes",
            title = StringResource(MHCStrings.demographics_condition_diabetes),
            group = StringResource(MHCStrings.demographics_conditions_primary),
        ),
        DemographicsOption(
            id = "heartFailure",
            title = StringResource(MHCStrings.demographics_condition_heartfailure),
            group = StringResource(MHCStrings.demographics_conditions_primary),
        ),
        DemographicsOption(
            id = "coronaryArteryDisease",
            title = StringResource(MHCStrings.demographics_condition_coronaryarterydisease),
            group = StringResource(MHCStrings.demographics_conditions_primary),
        ),
        DemographicsOption(
            id = "pulmonaryArterialHypertension",
            title = StringResource(MHCStrings.demographics_condition_pulmonaryarterialhypertension),
            group = StringResource(MHCStrings.demographics_conditions_primary),
        ),
        DemographicsOption(
            id = "adultCongenitalHeartDisease",
            title = StringResource(MHCStrings.demographics_condition_adultcongenitalheartdisease),
            group = StringResource(MHCStrings.demographics_conditions_primary),
        ),
        DemographicsOption(
            id = "congenitalHeartDisease2",
            title = StringResource(MHCStrings.demographics_condition_congenitalheartdisease2),
            group = StringResource(MHCStrings.demographics_conditions_primary),
        ),
        DemographicsOption(
            id = "abdominalAorticAneurysm",
            title = StringResource(MHCStrings.demographics_condition_abdominalaorticaneurysm),
            group = StringResource(MHCStrings.demographics_conditions_secondary),
        ),
        DemographicsOption(
            id = "angina",
            title = StringResource(MHCStrings.demographics_condition_angina),
            group = StringResource(MHCStrings.demographics_conditions_secondary),
        ),
        DemographicsOption(
            id = "aFib",
            title = StringResource(MHCStrings.demographics_condition_afib),
            group = StringResource(MHCStrings.demographics_conditions_secondary),
        ),
        DemographicsOption(
            id = "carotidArteryBlockageStenosis",
            title = StringResource(MHCStrings.demographics_condition_carotidarteryblockagestenosis),
            group = StringResource(MHCStrings.demographics_conditions_secondary),
        ),
        DemographicsOption(
            id = "carotidArterySurgeryOrStent",
            title = StringResource(MHCStrings.demographics_condition_carotidarterysurgeryorstent),
            group = StringResource(MHCStrings.demographics_conditions_secondary),
        ),
        DemographicsOption(
            id = "chronicKidneyDisease",
            title = StringResource(MHCStrings.demographics_condition_chronickidneydisease),
            subtitle = StringResource(MHCStrings.demographics_condition_chronickidneydisease_detail),
            group = StringResource(MHCStrings.demographics_conditions_secondary),
        ),
        DemographicsOption(
            id = "congenitalHeartDefect",
            title = StringResource(MHCStrings.demographics_condition_congenitalheartdefect),
            group = StringResource(MHCStrings.demographics_conditions_secondary),
        ),
        DemographicsOption(
            id = "coronaryBlockageStenosis",
            title = StringResource(MHCStrings.demographics_condition_coronaryblockagestenosis),
            group = StringResource(MHCStrings.demographics_conditions_secondary),
        ),
        DemographicsOption(
            id = "coronaryStentAngioplasty",
            title = StringResource(MHCStrings.demographics_condition_coronarystentangioplasty),
            group = StringResource(MHCStrings.demographics_conditions_secondary),
        ),
        DemographicsOption(
            id = "erectileDysfunction",
            title = StringResource(MHCStrings.demographics_condition_erectiledysfunction),
            group = StringResource(MHCStrings.demographics_conditions_secondary),
        ),
        DemographicsOption(
            id = "heartAttackMyocardialInfarction",
            title = StringResource(MHCStrings.demographics_condition_heartattackmyocardialinfarction),
            group = StringResource(MHCStrings.demographics_conditions_secondary),
        ),
        DemographicsOption(
            id = "heartBypassSurgery",
            title = StringResource(MHCStrings.demographics_condition_heartbypasssurgery),
            group = StringResource(MHCStrings.demographics_conditions_secondary),
        ),
        DemographicsOption(
            id = "heartFailureOrCHF",
            title = StringResource(MHCStrings.demographics_condition_heartfailureorchf),
            group = StringResource(MHCStrings.demographics_conditions_secondary),
        ),
        DemographicsOption(
            id = "highCoronaryCalciumScore",
            title = StringResource(MHCStrings.demographics_condition_highcoronarycalciumscore),
            group = StringResource(MHCStrings.demographics_conditions_secondary),
        ),
        DemographicsOption(
            id = "migraines",
            title = StringResource(MHCStrings.demographics_condition_migraines),
            group = StringResource(MHCStrings.demographics_conditions_secondary),
        ),
        DemographicsOption(
            id = "otherAutoImmuneDisease",
            title = StringResource(MHCStrings.demographics_condition_otherautoimmunedisease),
            group = StringResource(MHCStrings.demographics_conditions_secondary),
        ),
        DemographicsOption(
            id = "peripheralVascularDisease",
            title = StringResource(MHCStrings.demographics_condition_peripheralvasculardisease),
            subtitle = StringResource(MHCStrings.demographics_condition_peripheralvasculardisease_detail),
            group = StringResource(MHCStrings.demographics_conditions_secondary),
        ),
        DemographicsOption(
            id = "pulmonaryHypertension",
            title = StringResource(MHCStrings.demographics_condition_pulmonaryhypertension),
            group = StringResource(MHCStrings.demographics_conditions_secondary),
        ),
        DemographicsOption(
            id = "dilatedCardiomyopathy",
            title = StringResource(MHCStrings.demographics_condition_dilatedcardiomyopathy),
            group = StringResource(MHCStrings.demographics_conditions_secondary),
        ),
        DemographicsOption(
            id = "hypertrophicCardiomyopathy",
            title = StringResource(MHCStrings.demographics_condition_hypertrophiccardiomyopathy),
            group = StringResource(MHCStrings.demographics_conditions_secondary),
        ),
        DemographicsOption(
            id = "raynaudsPhenomenon",
            title = StringResource(MHCStrings.demographics_condition_raynaudsphenomenon),
            group = StringResource(MHCStrings.demographics_conditions_secondary),
        ),
        DemographicsOption(
            id = "rheumatoidArthritis",
            title = StringResource(MHCStrings.demographics_condition_rheumatoidarthritis),
            group = StringResource(MHCStrings.demographics_conditions_secondary),
        ),
        DemographicsOption(
            id = "stroke",
            title = StringResource(MHCStrings.demographics_condition_stroke),
            group = StringResource(MHCStrings.demographics_conditions_secondary),
        ),
        DemographicsOption(
            id = "systemicLupusErythematosus",
            title = StringResource(MHCStrings.demographics_condition_systemiclupuserythematosus),
            group = StringResource(MHCStrings.demographics_conditions_secondary),
        ),
        DemographicsOption(
            id = "systemicSclerosisOrScleroderma",
            title = StringResource(MHCStrings.demographics_condition_systemicsclerosisorscleroderma),
            group = StringResource(MHCStrings.demographics_conditions_secondary),
        ),
        DemographicsOption(
            id = "transientIschemicAttack",
            title = StringResource(MHCStrings.demographics_condition_transientischemicattack),
            group = StringResource(MHCStrings.demographics_conditions_secondary),
        ),
    )
}
