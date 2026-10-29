//
// This source file is part of the My Heart Counts Android open-source project
//
// SPDX-FileCopyrightText: 2026 Stanford University and the project authors (see CONTRIBUTORS.md)
//
// SPDX-License-Identifier: MIT

package edu.stanford.myheartcounts.demographics

/**
 * A demographics question, answered by one value of the participant's account.
 */
enum class DemographicsField {
    DateOfBirth,
    GenderIdentity,
    SexAtBirth,
    BloodType,
    Height,
    Weight,
    RaceEthnicity,
    LatinoStatus,
    Comorbidities,
    USRegion,
    UKRegion,
    USEducation,
    UKEducation,
    USIncome,
    UKIncome,
    NHSNumber,
    StageOfChange,
    ReferralSource,
}

/**
 * The regional form displayed to the participant.
 */
enum class DemographicsRegion { US, UK }

/**
 * Whether a question of the form is answered, and whether it needs an answer to complete the form.
 */
data class DemographicsCompletion(
    val isAnswered: Boolean,
    val isRequired: Boolean,
) {

    /**
     * Whether the question still needs an answer to complete the form.
     */
    val isMissingRequired: Boolean
        get() = isRequired && !isAnswered
}

/**
 * The signed-in participant's demographic answers and which of them are not stored yet.
 *
 * @param accountId the participant the answers belong to, or `null` while nobody is signed in
 * @param region the regional form the participant answers
 * @param optedIntoTrial whether the participant takes part in the trial, which requires more answers
 * @param response the answers as currently edited
 * @param pending the questions whose answers differ from the stored ones
 */
data class DemographicsState(
    val accountId: String? = null,
    val region: DemographicsRegion = DemographicsRegion.US,
    val optedIntoTrial: Boolean = false,
    val response: DemographicsResponse = DemographicsResponse.empty,
    val pending: Set<DemographicsField> = emptySet(),
)
