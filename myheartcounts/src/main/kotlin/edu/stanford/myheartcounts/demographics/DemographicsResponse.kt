//
// This source file is part of the My Heart Counts Android open-source project
//
// SPDX-FileCopyrightText: 2026 Stanford University and the project authors (see CONTRIBUTORS.md)
//
// SPDX-License-Identifier: MIT

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
import edu.stanford.myheartcounts.model.demographics.NHSNumber
import edu.stanford.myheartcounts.model.demographics.RaceEthnicity
import edu.stanford.myheartcounts.model.demographics.StageOfChangeOption
import edu.stanford.myheartcounts.model.demographics.UKRegion
import edu.stanford.myheartcounts.model.demographics.USRegion
import java.time.LocalDate

/**
 * The participant's answers to the demographics questions. A `null` answer is one not given yet.
 */
data class DemographicsResponse(
    val dateOfBirth: LocalDate?,
    val genderIdentity: MHCGenderIdentity?,
    val biologicalSexAtBirth: BiologicalSex?,
    val bloodType: BloodType?,
    val heightInCm: Double?,
    val weightInKg: Double?,
    val raceEthnicity: RaceEthnicity?,
    val latinoStatus: LatinoStatusOption?,
    val comorbidities: Comorbidities?,
    val usRegion: USRegion?,
    val ukRegion: UKRegion?,
    val educationUS: EducationStatusUS?,
    val educationUK: EducationStatusUK?,
    val householdIncomeUS: HouseholdIncomeUS?,
    val householdIncomeUK: HouseholdIncomeUK?,
    val nhsNumber: NHSNumber?,
    val stageOfChange: StageOfChangeOption?,
    val referralSource: String?,
) {

    companion object {

        /**
         * A response without any answers.
         */
        val empty = DemographicsResponse(
            dateOfBirth = null,
            genderIdentity = null,
            biologicalSexAtBirth = null,
            bloodType = null,
            heightInCm = null,
            weightInKg = null,
            raceEthnicity = null,
            latinoStatus = null,
            comorbidities = null,
            usRegion = null,
            ukRegion = null,
            educationUS = null,
            educationUK = null,
            householdIncomeUS = null,
            householdIncomeUK = null,
            nhsNumber = null,
            stageOfChange = null,
            referralSource = null,
        )
    }
}
