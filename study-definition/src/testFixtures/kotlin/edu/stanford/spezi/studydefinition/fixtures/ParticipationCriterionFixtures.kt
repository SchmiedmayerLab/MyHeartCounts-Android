//
// This source file is part of the My Heart Counts Android open-source project
//
// SPDX-FileCopyrightText: 2026 Stanford University and the project authors (see CONTRIBUTORS.md)
//
// SPDX-License-Identifier: MIT

package edu.stanford.spezi.studydefinition.fixtures

import edu.stanford.spezi.studydefinition.CustomCriterionKey
import edu.stanford.spezi.studydefinition.ParticipationCriterion

/**
 * Fixtures for [ParticipationCriterion]. [create] returns an always-true `all([])`.
 */
object ParticipationCriterionFixtures {
    fun create(): ParticipationCriterion = ParticipationCriterion.All(emptyList())

    fun createAgeAtLeast(years: Int = 0): ParticipationCriterion.AgeAtLeast = ParticipationCriterion.AgeAtLeast(years)

    fun createIsFromRegion(region: String = ""): ParticipationCriterion.IsFromRegion =
        ParticipationCriterion.IsFromRegion(region)

    fun createSpeaksLanguage(language: String = ""): ParticipationCriterion.SpeaksLanguage =
        ParticipationCriterion.SpeaksLanguage(language)

    fun createCustom(key: CustomCriterionKey = CustomCriterionKeyFixtures.create()): ParticipationCriterion.Custom =
        ParticipationCriterion.Custom(key)

    fun createNot(inner: ParticipationCriterion = create()): ParticipationCriterion.Not =
        ParticipationCriterion.Not(inner)

    fun createAll(criteria: List<ParticipationCriterion> = emptyList()): ParticipationCriterion.All =
        ParticipationCriterion.All(criteria)

    fun createAny(criteria: List<ParticipationCriterion> = emptyList()): ParticipationCriterion.Any =
        ParticipationCriterion.Any(criteria)
}
