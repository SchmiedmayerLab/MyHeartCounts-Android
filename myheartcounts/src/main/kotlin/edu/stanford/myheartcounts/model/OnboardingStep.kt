//
// This source file is part of the My Heart Counts open-source project
//
// SPDX-FileCopyrightText: 2026 Stanford University and the project authors (see CONTRIBUTORS.md)
//
// SPDX-License-Identifier: MIT

@file:Suppress("SpacingBetweenDeclarationsWithAnnotations")

package edu.stanford.myheartcounts.model

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/**
 * Identifies a step in the onboarding flow.
 */
@Serializable
enum class OnboardingStep {
    @SerialName("welcome") WELCOME,
    @SerialName("eligibility") ELIGIBILITY,
    @SerialName("login") LOGIN,
    @SerialName("disclaimer1") DISCLAIMER_1,
    @SerialName("disclaimer2") DISCLAIMER_2,
    @SerialName("disclaimer3") DISCLAIMER_3,
    @SerialName("disclaimer4") DISCLAIMER_4,
    @SerialName("comprehension") COMPREHENSION,
    @SerialName("consent") CONSENT,
    @SerialName("healthAccess") HEALTH_ACCESS,
    @SerialName("healthRecords") HEALTH_RECORDS,
    @SerialName("workoutPreference") WORKOUT_PREFERENCE,
    @SerialName("notifications") NOTIFICATIONS,
    @SerialName("demographics") DEMOGRAPHICS,
    @SerialName("finalStep") FINAL_STEP,
}
