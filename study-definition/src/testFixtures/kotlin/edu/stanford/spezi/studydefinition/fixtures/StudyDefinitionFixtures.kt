//
// This source file is part of the My Heart Counts open-source project
//
// SPDX-FileCopyrightText: 2026 Stanford University and the project authors (see CONTRIBUTORS.md)
//
// SPDX-License-Identifier: MIT

package edu.stanford.spezi.studydefinition.fixtures

import edu.stanford.spezi.studydefinition.Component
import edu.stanford.spezi.studydefinition.ComponentSchedule
import edu.stanford.spezi.studydefinition.Metadata
import edu.stanford.spezi.studydefinition.StudyDefinition

/**
 * Fixture for the [StudyDefinition] data class.
 *
 * For a fully-populated example study loaded from a real bundle, use [StudyBundleFixtures] instead.
 */
object StudyDefinitionFixtures {
    fun create(
        studyRevision: UInt = 0u,
        metadata: Metadata = MetadataFixtures.create(),
        components: List<Component> = emptyList(),
        componentSchedules: List<ComponentSchedule> = emptyList(),
    ): StudyDefinition = StudyDefinition(
        studyRevision = studyRevision,
        metadata = metadata,
        components = components,
        componentSchedules = componentSchedules,
    )
}
