//
// This source file is part of the My Heart Counts Android open-source project
//
// SPDX-FileCopyrightText: 2026 Stanford University and the project authors (see CONTRIBUTORS.md)
//
// SPDX-License-Identifier: MIT

package org.grovealliance.studydefinition.fixtures

import org.grovealliance.foundation.fixtures.UUIDFixtures
import org.grovealliance.studydefinition.EnrollmentConditions
import org.grovealliance.studydefinition.FileReference
import org.grovealliance.studydefinition.LocalizedText
import org.grovealliance.studydefinition.Metadata
import org.grovealliance.studydefinition.ParticipationCriterion
import java.util.UUID

/**
 * Fixture for [Metadata].
 */
object MetadataFixtures {
    fun create(
        id: UUID = UUIDFixtures.zero,
        title: LocalizedText = LocalizedText.empty,
        shortTitle: LocalizedText = LocalizedText.empty,
        icon: Metadata.Icon? = null,
        explanationText: LocalizedText = LocalizedText.empty,
        shortExplanationText: LocalizedText = LocalizedText.empty,
        studyDependency: UUID? = null,
        participationCriterion: ParticipationCriterion = ParticipationCriterionFixtures.create(),
        enrollmentConditions: EnrollmentConditions? = null,
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
