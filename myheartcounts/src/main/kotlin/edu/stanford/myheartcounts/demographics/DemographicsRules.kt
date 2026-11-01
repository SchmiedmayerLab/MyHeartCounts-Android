//
// This source file is part of the My Heart Counts Android open-source project
//
// SPDX-FileCopyrightText: 2026 Stanford University and the project authors (see CONTRIBUTORS.md)
//
// SPDX-License-Identifier: MIT

@file:Suppress("detekt:CyclomaticComplexMethod")

package edu.stanford.myheartcounts.demographics

import edu.stanford.myheartcounts.model.demographics.BiologicalSex
import edu.stanford.myheartcounts.model.demographics.BloodType
import edu.stanford.myheartcounts.model.demographics.Comorbidities
import edu.stanford.myheartcounts.model.demographics.EducationStatusUK
import edu.stanford.myheartcounts.model.demographics.EducationStatusUS
import edu.stanford.myheartcounts.model.demographics.HouseholdIncomeUK
import edu.stanford.myheartcounts.model.demographics.HouseholdIncomeUS
import edu.stanford.myheartcounts.model.demographics.LatinoStatusOption
import edu.stanford.myheartcounts.model.demographics.MHCGenderIdentity
import edu.stanford.myheartcounts.model.demographics.RaceEthnicity
import edu.stanford.myheartcounts.model.demographics.StageOfChangeOption
import edu.stanford.myheartcounts.model.demographics.UKRegion
import edu.stanford.myheartcounts.model.demographics.USRegion

/**
 * Decides which questions a participant is asked, which of them need an answer, how choosing an
 * answer changes the others chosen with it, and the answers implied by others.
 */
class DemographicsRules {

    /**
     * The questions of the [region]'s form, in display order.
     */
    fun fields(region: DemographicsRegion): List<DemographicsField> = DemographicsField.entries.filter {
        it !in when (region) {
            DemographicsRegion.US -> UK_ONLY
            DemographicsRegion.UK -> US_ONLY
        }
    }

    /**
     * Whether each question of the participant's form is answered and needs to be.
     */
    fun completion(state: DemographicsState): Map<DemographicsField, DemographicsCompletion> {
        val required = required(
            region = state.region,
            optedIntoTrial = state.optedIntoTrial,
        )
        return fields(region = state.region).associateWith { field ->
            DemographicsCompletion(
                isAnswered = isAnswered(field = field, response = state.response),
                isRequired = field in required,
            )
        }
    }

    /**
     * [updated] with the sex at birth implied by a newly given gender identity, or the gender
     * identity implied by a newly given sex at birth, when the counterpart is unanswered.
     */
    fun inferred(previous: DemographicsResponse, updated: DemographicsResponse): DemographicsResponse {
        val gender = updated.genderIdentity
        val sex = updated.biologicalSexAtBirth
        return when {
            gender != null && gender != previous.genderIdentity && sex == null ->
                updated.copy(biologicalSexAtBirth = gender.impliedSexAtBirth)
            sex != null && sex != previous.biologicalSexAtBirth && gender == null ->
                updated.copy(genderIdentity = sex.impliedGenderIdentity)
            else -> updated
        }
    }

    /**
     * [current] with [race] chosen or unchosen, or `null` once none is left. Declining to state
     * replaces every race, and choosing a race withdraws the decline.
     */
    fun toggled(current: RaceEthnicity?, race: RaceEthnicity): RaceEthnicity? {
        val races = current ?: RaceEthnicity.NONE
        val updated = when {
            race == RaceEthnicity.PREFER_NOT_TO_STATE -> race
            race in races -> RaceEthnicity(rawValue = races.rawValue and race.rawValue.inv())
            else -> RaceEthnicity(
                rawValue = (races + race).rawValue and RaceEthnicity.PREFER_NOT_TO_STATE.rawValue.inv(),
            )
        }
        return updated.takeUnless { it.isEmpty }
    }

    /**
     * [current] with [condition] chosen without a date of diagnosis, or unchosen.
     */
    fun toggled(current: Comorbidities?, condition: String): Comorbidities {
        val entries = current?.entries.orEmpty()
        return Comorbidities(
            entries = if (condition in entries) entries - condition else entries + (condition to ""),
        )
    }

    private fun required(region: DemographicsRegion, optedIntoTrial: Boolean) = buildSet {
        addAll(ALWAYS_REQUIRED)
        if (region == DemographicsRegion.US) add(DemographicsField.LatinoStatus)
        if (optedIntoTrial) {
            add(DemographicsField.Comorbidities)
            add(DemographicsField.StageOfChange)
            add(
                when (region) {
                    DemographicsRegion.US -> DemographicsField.USEducation
                    DemographicsRegion.UK -> DemographicsField.UKEducation
                },
            )
        }
    }

    private fun isAnswered(field: DemographicsField, response: DemographicsResponse): Boolean = when (field) {
        DemographicsField.DateOfBirth -> response.dateOfBirth != null
        DemographicsField.GenderIdentity -> response.genderIdentity != null
        DemographicsField.SexAtBirth -> response.biologicalSexAtBirth != null
        DemographicsField.BloodType -> response.bloodType.isSet(notSet = BloodType.NOT_SET)
        DemographicsField.Height -> response.heightInCm != null
        DemographicsField.Weight -> response.weightInKg != null
        DemographicsField.RaceEthnicity -> response.raceEthnicity?.isEmpty == false
        DemographicsField.LatinoStatus -> response.latinoStatus.isSet(notSet = LatinoStatusOption.NOT_SET)
        DemographicsField.Comorbidities -> response.comorbidities != null
        DemographicsField.USRegion -> response.usRegion.isSet(notSet = USRegion.NOT_SET)
        DemographicsField.UKRegion -> response.ukRegion.isSet(notSet = UKRegion.NotSet)
        DemographicsField.USEducation -> response.educationUS.isSet(notSet = EducationStatusUS.NOT_SET)
        DemographicsField.UKEducation -> response.educationUK.isSet(notSet = EducationStatusUK.NOT_SET)
        DemographicsField.USIncome -> response.householdIncomeUS.isSet(notSet = HouseholdIncomeUS.NOT_SET)
        DemographicsField.UKIncome -> response.householdIncomeUK.isSet(notSet = HouseholdIncomeUK.NOT_SET)
        DemographicsField.NHSNumber -> response.nhsNumber?.stringValue?.isNotEmpty() == true
        DemographicsField.StageOfChange -> response.stageOfChange.isSet(notSet = StageOfChangeOption.NOT_SET)
        DemographicsField.ReferralSource -> response.referralSource != null
    }

    private fun <T : Any> T?.isSet(notSet: T) = this != null && this != notSet

    private val MHCGenderIdentity.impliedSexAtBirth: BiologicalSex?
        get() = when (this) {
            MHCGenderIdentity.MALE, MHCGenderIdentity.TRANS_FEMALE -> BiologicalSex.MALE
            MHCGenderIdentity.FEMALE, MHCGenderIdentity.TRANS_MALE -> BiologicalSex.FEMALE
            MHCGenderIdentity.OTHER, MHCGenderIdentity.PREFER_NOT_TO_STATE -> null
        }

    private val BiologicalSex.impliedGenderIdentity: MHCGenderIdentity?
        get() = when (this) {
            BiologicalSex.MALE -> MHCGenderIdentity.MALE
            BiologicalSex.FEMALE -> MHCGenderIdentity.FEMALE
            BiologicalSex.INTERSEX, BiologicalSex.PREFER_NOT_TO_STATE -> null
        }

    private companion object {
        val ALWAYS_REQUIRED = setOf(
            DemographicsField.DateOfBirth,
            DemographicsField.GenderIdentity,
            DemographicsField.Height,
            DemographicsField.Weight,
            DemographicsField.RaceEthnicity,
        )
        val US_ONLY = setOf(
            DemographicsField.LatinoStatus,
            DemographicsField.USRegion,
            DemographicsField.USEducation,
            DemographicsField.USIncome,
        )
        val UK_ONLY = setOf(
            DemographicsField.UKRegion,
            DemographicsField.UKEducation,
            DemographicsField.UKIncome,
            DemographicsField.NHSNumber,
        )
    }
}
