//
// This source file is part of the My Heart Counts open-source project
//
// SPDX-FileCopyrightText: 2026 Stanford University and the project authors (see CONTRIBUTORS.md)
//
// SPDX-License-Identifier: MIT

package edu.stanford.spezi.studydefinition.fixtures

import edu.stanford.spezi.foundation.fixtures.UUIDFixtures
import edu.stanford.spezi.studydefinition.EnrollmentConditions
import edu.stanford.spezi.studydefinition.FileReference
import edu.stanford.spezi.studydefinition.Metadata
import edu.stanford.spezi.studydefinition.ParticipationCriterion
import java.util.UUID

/**
 * Fixture for [Metadata].
 */
object MetadataFixtures {
    fun create(
        id: UUID = UUIDFixtures.zero,
        title: String = "",
        shortTitle: String = "",
        icon: Metadata.Icon? = null,
        explanationText: String = "",
        shortExplanationText: String = "",
        studyDependency: UUID? = null,
        participationCriterion: ParticipationCriterion = ParticipationCriterionFixtures.create(),
        enrollmentConditions: EnrollmentConditions = EnrollmentConditionsFixtures.create(),
        consentFileRef: FileReference? = null,
    ): Metadata = Metadata(
        id = id,
        title = title,
        shortTitle = shortTitle,
        icon = icon,
        explanationText = explanationText,
        shortExplanationText = shortExplanationText,
        studyDependency = studyDependency,
        participationCriterion = participationCriterion,
        enrollmentConditions = enrollmentConditions,
        consentFileRef = consentFileRef,
    )
}
