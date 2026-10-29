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
 * Provides the title of each demographic question and the responses offered to those answered from
 * a fixed list. Regions and conditions are provided by [DemographicsRegionsProvider] and
 * [DemographicsConditionsProvider].
 */
interface DemographicsOptionsProvider {

    /**
     * The title of [field].
     */
    fun title(field: DemographicsField): StringResource

    /**
     * The responses offered for [field], or none when it is not answered from a fixed list.
     */
    fun options(field: DemographicsField): List<DemographicsOption>
}

/**
 * Default [DemographicsOptionsProvider] implementation.
 */
class DemographicsOptionsProviderImpl : DemographicsOptionsProvider {

    override fun title(field: DemographicsField): StringResource = titles.getValue(field)

    override fun options(field: DemographicsField): List<DemographicsOption> = when (field) {
        DemographicsField.GenderIdentity -> genderIdentityOptions
        DemographicsField.SexAtBirth -> sexAtBirthOptions
        DemographicsField.BloodType -> bloodTypeOptions
        DemographicsField.RaceEthnicity -> raceEthnicityOptions
        DemographicsField.LatinoStatus -> latinoStatusOptions
        DemographicsField.USEducation -> usEducationOptions
        DemographicsField.UKEducation -> ukEducationOptions
        DemographicsField.USIncome -> usIncomeOptions
        DemographicsField.UKIncome -> ukIncomeOptions
        DemographicsField.StageOfChange -> stageOfChangeOptions
        DemographicsField.ReferralSource -> referralSourceOptions
        else -> emptyList()
    }

    private val titles = mapOf(
        DemographicsField.DateOfBirth to StringResource(MHCStrings.demographics_field_dateofbirth),
        DemographicsField.GenderIdentity to StringResource(MHCStrings.demographics_field_genderidentity),
        DemographicsField.SexAtBirth to StringResource(MHCStrings.demographics_field_sexatbirth),
        DemographicsField.BloodType to StringResource(MHCStrings.demographics_field_bloodtype),
        DemographicsField.Height to StringResource(MHCStrings.demographics_field_height),
        DemographicsField.Weight to StringResource(MHCStrings.demographics_field_weight),
        DemographicsField.RaceEthnicity to StringResource(MHCStrings.demographics_field_raceethnicity),
        DemographicsField.LatinoStatus to StringResource(MHCStrings.demographics_field_latinostatus),
        DemographicsField.Comorbidities to StringResource(MHCStrings.demographics_field_comorbidities),
        DemographicsField.USRegion to StringResource(MHCStrings.demographics_field_usregion),
        DemographicsField.UKRegion to StringResource(MHCStrings.demographics_field_ukregion),
        DemographicsField.USEducation to StringResource(MHCStrings.demographics_field_useducation),
        DemographicsField.UKEducation to StringResource(MHCStrings.demographics_field_ukeducation),
        DemographicsField.USIncome to StringResource(MHCStrings.demographics_field_usincome),
        DemographicsField.UKIncome to StringResource(MHCStrings.demographics_field_ukincome),
        DemographicsField.NHSNumber to StringResource(MHCStrings.demographics_field_nhsnumber),
        DemographicsField.StageOfChange to StringResource(MHCStrings.demographics_field_stageofchange),
        DemographicsField.ReferralSource to StringResource(MHCStrings.demographics_field_referralsource),
    )

    private val genderIdentityOptions = listOf(
        DemographicsOption(
            id = "0",
            title = StringResource(MHCStrings.demographics_gender_identity_0),
        ),
        DemographicsOption(
            id = "1",
            title = StringResource(MHCStrings.demographics_gender_identity_1),
        ),
        DemographicsOption(
            id = "2",
            title = StringResource(MHCStrings.demographics_gender_identity_2),
        ),
        DemographicsOption(
            id = "3",
            title = StringResource(MHCStrings.demographics_gender_identity_3),
        ),
        DemographicsOption(
            id = "4",
            title = StringResource(MHCStrings.demographics_gender_identity_4),
        ),
        DemographicsOption(
            id = "5",
            title = StringResource(MHCStrings.demographics_gender_identity_5),
        ),
    )

    private val sexAtBirthOptions = listOf(
        DemographicsOption(
            id = "0",
            title = StringResource(MHCStrings.demographics_sex_at_birth_0),
        ),
        DemographicsOption(
            id = "1",
            title = StringResource(MHCStrings.demographics_sex_at_birth_1),
        ),
        DemographicsOption(
            id = "2",
            title = StringResource(MHCStrings.demographics_sex_at_birth_2),
        ),
        DemographicsOption(
            id = "3",
            title = StringResource(MHCStrings.demographics_sex_at_birth_3),
        ),
    )

    private val bloodTypeOptions = listOf(
        DemographicsOption(
            id = "1",
            title = StringResource(MHCStrings.demographics_blood_type_1),
        ),
        DemographicsOption(
            id = "2",
            title = StringResource(MHCStrings.demographics_blood_type_2),
        ),
        DemographicsOption(
            id = "3",
            title = StringResource(MHCStrings.demographics_blood_type_3),
        ),
        DemographicsOption(
            id = "4",
            title = StringResource(MHCStrings.demographics_blood_type_4),
        ),
        DemographicsOption(
            id = "5",
            title = StringResource(MHCStrings.demographics_blood_type_5),
        ),
        DemographicsOption(
            id = "6",
            title = StringResource(MHCStrings.demographics_blood_type_6),
        ),
        DemographicsOption(
            id = "7",
            title = StringResource(MHCStrings.demographics_blood_type_7),
        ),
        DemographicsOption(
            id = "8",
            title = StringResource(MHCStrings.demographics_blood_type_8),
        ),
    )

    private val raceEthnicityOptions = listOf(
        DemographicsOption(
            id = "1",
            title = StringResource(MHCStrings.demographics_race_ethnicity_1),
        ),
        DemographicsOption(
            id = "2",
            title = StringResource(MHCStrings.demographics_race_ethnicity_2),
        ),
        DemographicsOption(
            id = "4",
            title = StringResource(MHCStrings.demographics_race_ethnicity_4),
        ),
        DemographicsOption(
            id = "8",
            title = StringResource(MHCStrings.demographics_race_ethnicity_8),
        ),
        DemographicsOption(
            id = "16",
            title = StringResource(MHCStrings.demographics_race_ethnicity_16),
        ),
        DemographicsOption(
            id = "32",
            title = StringResource(MHCStrings.demographics_race_ethnicity_32),
        ),
        DemographicsOption(
            id = "64",
            title = StringResource(MHCStrings.demographics_race_ethnicity_64),
        ),
        DemographicsOption(
            id = "128",
            title = StringResource(MHCStrings.demographics_race_ethnicity_128),
        ),
        DemographicsOption(
            id = "256",
            title = StringResource(MHCStrings.demographics_race_ethnicity_256),
        ),
        DemographicsOption(
            id = "512",
            title = StringResource(MHCStrings.demographics_race_ethnicity_512),
        ),
        DemographicsOption(
            id = "1024",
            title = StringResource(MHCStrings.demographics_race_ethnicity_1024),
        ),
        DemographicsOption(
            id = "2048",
            title = StringResource(MHCStrings.demographics_race_ethnicity_2048),
        ),
        DemographicsOption(
            id = "4096",
            title = StringResource(MHCStrings.demographics_race_ethnicity_4096),
        ),
    )

    private val latinoStatusOptions = listOf(
        DemographicsOption(
            id = "1",
            title = StringResource(MHCStrings.demographics_latino_status_1),
        ),
        DemographicsOption(
            id = "2",
            title = StringResource(MHCStrings.demographics_latino_status_2),
        ),
        DemographicsOption(
            id = "3",
            title = StringResource(MHCStrings.demographics_latino_status_3),
        ),
        DemographicsOption(
            id = "4",
            title = StringResource(MHCStrings.demographics_latino_status_4),
        ),
        DemographicsOption(
            id = "5",
            title = StringResource(MHCStrings.demographics_latino_status_5),
        ),
        DemographicsOption(
            id = "6",
            title = StringResource(MHCStrings.demographics_latino_status_6),
        ),
        DemographicsOption(
            id = "255",
            title = StringResource(MHCStrings.demographics_latino_status_255),
        ),
    )

    private val usEducationOptions = listOf(
        DemographicsOption(
            id = "didNotAttendSchool",
            title = StringResource(MHCStrings.demographics_us_education_didnotattendschool),
        ),
        DemographicsOption(
            id = "gradeSchool",
            title = StringResource(MHCStrings.demographics_us_education_gradeschool),
        ),
        DemographicsOption(
            id = "highSchool",
            title = StringResource(MHCStrings.demographics_us_education_highschool),
        ),
        DemographicsOption(
            id = "someCollege",
            title = StringResource(MHCStrings.demographics_us_education_somecollege),
            subtitle = StringResource(MHCStrings.demographics_us_education_somecollege_detail),
        ),
        DemographicsOption(
            id = "bachelor",
            title = StringResource(MHCStrings.demographics_us_education_bachelor),
            subtitle = StringResource(MHCStrings.demographics_us_education_bachelor_detail),
        ),
        DemographicsOption(
            id = "master",
            title = StringResource(MHCStrings.demographics_us_education_master),
        ),
        DemographicsOption(
            id = "doctoralDegree",
            title = StringResource(MHCStrings.demographics_us_education_doctoraldegree),
            subtitle = StringResource(MHCStrings.demographics_us_education_doctoraldegree_detail),
        ),
        DemographicsOption(
            id = "preferNotToState",
            title = StringResource(MHCStrings.demographics_us_education_prefernottostate),
        ),
    )

    private val ukEducationOptions = listOf(
        DemographicsOption(
            id = "didNotAttendSchool",
            title = StringResource(MHCStrings.demographics_uk_education_didnotattendschool),
        ),
        DemographicsOption(
            id = "highSchool",
            title = StringResource(MHCStrings.demographics_uk_education_highschool),
        ),
        DemographicsOption(
            id = "vocationalTraining",
            title = StringResource(MHCStrings.demographics_uk_education_vocationaltraining),
            subtitle = StringResource(MHCStrings.demographics_uk_education_vocationaltraining_detail),
        ),
        DemographicsOption(
            id = "someCollege",
            title = StringResource(MHCStrings.demographics_uk_education_somecollege),
        ),
        DemographicsOption(
            id = "master",
            title = StringResource(MHCStrings.demographics_uk_education_master),
        ),
        DemographicsOption(
            id = "doctoralDegree",
            title = StringResource(MHCStrings.demographics_uk_education_doctoraldegree),
        ),
        DemographicsOption(
            id = "preferNotToState",
            title = StringResource(MHCStrings.demographics_uk_education_prefernottostate),
        ),
    )

    private val usIncomeOptions = listOf(
        DemographicsOption(
            id = "1",
            title = StringResource(MHCStrings.demographics_us_income_1),
        ),
        DemographicsOption(
            id = "2",
            title = StringResource(MHCStrings.demographics_us_income_2),
        ),
        DemographicsOption(
            id = "3",
            title = StringResource(MHCStrings.demographics_us_income_3),
        ),
        DemographicsOption(
            id = "4",
            title = StringResource(MHCStrings.demographics_us_income_4),
        ),
        DemographicsOption(
            id = "5",
            title = StringResource(MHCStrings.demographics_us_income_5),
        ),
        DemographicsOption(
            id = "6",
            title = StringResource(MHCStrings.demographics_us_income_6),
        ),
        DemographicsOption(
            id = "7",
            title = StringResource(MHCStrings.demographics_us_income_7),
        ),
        DemographicsOption(
            id = "8",
            title = StringResource(MHCStrings.demographics_us_income_8),
        ),
        DemographicsOption(
            id = "255",
            title = StringResource(MHCStrings.demographics_us_income_255),
        ),
    )

    private val ukIncomeOptions = listOf(
        DemographicsOption(
            id = "1",
            title = StringResource(MHCStrings.demographics_uk_income_1),
        ),
        DemographicsOption(
            id = "2",
            title = StringResource(MHCStrings.demographics_uk_income_2),
        ),
        DemographicsOption(
            id = "3",
            title = StringResource(MHCStrings.demographics_uk_income_3),
        ),
        DemographicsOption(
            id = "4",
            title = StringResource(MHCStrings.demographics_uk_income_4),
        ),
        DemographicsOption(
            id = "5",
            title = StringResource(MHCStrings.demographics_uk_income_5),
        ),
        DemographicsOption(
            id = "6",
            title = StringResource(MHCStrings.demographics_uk_income_6),
        ),
        DemographicsOption(
            id = "7",
            title = StringResource(MHCStrings.demographics_uk_income_7),
        ),
        DemographicsOption(
            id = "8",
            title = StringResource(MHCStrings.demographics_uk_income_8),
        ),
        DemographicsOption(
            id = "255",
            title = StringResource(MHCStrings.demographics_uk_income_255),
        ),
    )

    private val stageOfChangeOptions = listOf(
        DemographicsOption(
            id = "a",
            title = StringResource("A"),
            subtitle = StringResource(MHCStrings.demographics_stage_of_change_a),
        ),
        DemographicsOption(
            id = "b",
            title = StringResource("B"),
            subtitle = StringResource(MHCStrings.demographics_stage_of_change_b),
        ),
        DemographicsOption(
            id = "c",
            title = StringResource("C"),
            subtitle = StringResource(MHCStrings.demographics_stage_of_change_c),
        ),
        DemographicsOption(
            id = "d",
            title = StringResource("D"),
            subtitle = StringResource(MHCStrings.demographics_stage_of_change_d),
        ),
        DemographicsOption(
            id = "e",
            title = StringResource("E"),
            subtitle = StringResource(MHCStrings.demographics_stage_of_change_e),
        ),
        DemographicsOption(
            id = "f",
            title = StringResource("F"),
            subtitle = StringResource(MHCStrings.demographics_stage_of_change_f),
        ),
        DemographicsOption(
            id = "g",
            title = StringResource("G"),
            subtitle = StringResource(MHCStrings.demographics_stage_of_change_g),
        ),
        DemographicsOption(
            id = "h",
            title = StringResource("H"),
            subtitle = StringResource(MHCStrings.demographics_stage_of_change_h),
        ),
        DemographicsOption(
            id = "i",
            title = StringResource("I"),
            subtitle = StringResource(MHCStrings.demographics_stage_of_change_i),
        ),
    )

    private val referralSourceOptions = listOf(
        DemographicsOption(
            id = "1",
            title = StringResource(MHCStrings.demographics_referral_source_1),
        ),
        DemographicsOption(
            id = "2",
            title = StringResource(MHCStrings.demographics_referral_source_2),
        ),
        DemographicsOption(
            id = "3",
            title = StringResource(MHCStrings.demographics_referral_source_3),
        ),
        DemographicsOption(
            id = "4",
            title = StringResource(MHCStrings.demographics_referral_source_4),
        ),
        DemographicsOption(
            id = "5",
            title = StringResource(MHCStrings.demographics_referral_source_5),
        ),
        DemographicsOption(
            id = "6",
            title = StringResource(MHCStrings.demographics_referral_source_6),
        ),
        DemographicsOption(
            id = "7",
            title = StringResource(MHCStrings.demographics_referral_source_7),
        ),
        DemographicsOption(
            id = "8",
            title = StringResource(MHCStrings.demographics_referral_source_8),
        ),
        DemographicsOption(
            id = "9",
            title = StringResource(MHCStrings.demographics_referral_source_9),
        ),
        DemographicsOption(
            id = "10",
            title = StringResource(MHCStrings.demographics_referral_source_10),
        ),
        DemographicsOption(
            id = "11",
            title = StringResource(MHCStrings.demographics_referral_source_11),
        ),
    )
}
