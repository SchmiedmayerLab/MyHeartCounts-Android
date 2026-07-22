//
// This source file is part of the My Heart Counts open-source project
//
// SPDX-FileCopyrightText: 2026 Stanford University and the project authors (see CONTRIBUTORS.md)
//
// SPDX-License-Identifier: MIT

package edu.stanford.spezi.studydefinition

import edu.stanford.spezi.studydefinition.internal.EnrollmentConditionsSerializer
import edu.stanford.spezi.studydefinition.internal.ParticipationCriterionSerializer
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/**
 * A criterion which must be satisfied for a person to be able to participate in a study.
 *
 * Criteria form a tree of leaf conditions combined via [Not], [All], and [Any]. Evaluate a tree
 * against an [EvaluationEnvironment] with [evaluate].
 */
@Serializable(with = ParticipationCriterionSerializer::class)
sealed interface ParticipationCriterion {
    /**
     * Satisfied when the participant is at least [years] old.
     */
    @Serializable
    @SerialName("ageAtLeast")
    data class AgeAtLeast(val years: Int) : ParticipationCriterion

    /**
     * Satisfied when the participant is from [region] (an ISO region code).
     */
    @Serializable
    @SerialName("isFromRegion")
    data class IsFromRegion(val region: String) : ParticipationCriterion

    /**
     * Satisfied when the participant speaks [language] (an ISO language code).
     */
    @Serializable
    @SerialName("speaksLanguage")
    data class SpeaksLanguage(val language: String) : ParticipationCriterion

    /**
     * Satisfied when the environment has [key] enabled.
     */
    @Serializable
    @SerialName("custom")
    data class Custom(val key: CustomCriterionKey) : ParticipationCriterion

    /**
     * Satisfied when its [inner] criterion is not satisfied.
     */
    @Serializable
    @SerialName("not")
    data class Not(val inner: ParticipationCriterion) : ParticipationCriterion

    /**
     * Satisfied when all [criteria] are satisfied. An empty list is satisfied.
     */
    @Serializable
    @SerialName("all")
    data class All(val criteria: List<ParticipationCriterion>) : ParticipationCriterion

    /**
     * Satisfied when any of [criteria] is satisfied. An empty list is not satisfied.
     */
    @Serializable
    @SerialName("any")
    data class Any(val criteria: List<ParticipationCriterion>) : ParticipationCriterion

    /**
     * Determines whether the criterion is satisfied within [environment].
     */
    fun evaluate(environment: EvaluationEnvironment): Boolean = when (this) {
        is AgeAtLeast -> environment.age?.let { it >= years } ?: false
        is IsFromRegion -> environment.region?.let { it == region } ?: false
        is SpeaksLanguage -> environment.language == language
        is Custom -> key in environment.enabledCustomKeys
        is Not -> !inner.evaluate(environment)
        is All -> criteria.all { it.evaluate(environment) }
        is Any -> criteria.any { it.evaluate(environment) }
    }

    /**
     * Whether the criterion is a leaf, i.e. contains no nested criteria.
     */
    val isLeaf: Boolean
        get() = when (this) {
            is AgeAtLeast, is IsFromRegion, is SpeaksLanguage, is Custom -> true
            is Not, is All, is Any -> false
        }
}

/**
 * Identifies a custom participation criterion.
 */
@Serializable
data class CustomCriterionKey(
    val keyValue: String,
    val displayTitle: String,
)

/**
 * The context against which [ParticipationCriterion]s are evaluated.
 */
data class EvaluationEnvironment(
    val age: Int?,
    val region: String?,
    val language: String,
    val enabledCustomKeys: Set<CustomCriterionKey>,
)

/**
 * Defines how enrollment into a study is gated once the participation criteria are satisfied.
 */
@Serializable(with = EnrollmentConditionsSerializer::class)
sealed interface EnrollmentConditions {
    /**
     * No additional conditions apply to enrollment.
     */
    @Serializable
    @SerialName("none")
    data object None : EnrollmentConditions

    /**
     * Enrollment is gated by an invitation code verified against [verificationEndpoint].
     */
    @Serializable
    @SerialName("requiresInvitation")
    data class RequiresInvitation(val verificationEndpoint: String) : EnrollmentConditions
}
