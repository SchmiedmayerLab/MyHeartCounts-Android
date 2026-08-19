//
// This source file is part of the My Heart Counts Android open-source project
//
// SPDX-FileCopyrightText: 2026 Stanford University and the project authors (see CONTRIBUTORS.md)
//
// SPDX-License-Identifier: MIT

package org.grovealliance.studydefinition.fixtures

import org.grovealliance.studydefinition.Component
import org.grovealliance.studydefinition.ComponentSchedule
import org.grovealliance.studydefinition.Metadata
import org.grovealliance.studydefinition.StudyDefinition

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
