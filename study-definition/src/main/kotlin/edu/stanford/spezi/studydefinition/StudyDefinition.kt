//
// This source file is part of the My Heart Counts open-source project
//
// SPDX-FileCopyrightText: 2026 Stanford University and the project authors (see CONTRIBUTORS.md)
//
// SPDX-License-Identifier: MIT

package edu.stanford.spezi.studydefinition

import kotlinx.serialization.Serializable
import java.util.UUID

/**
 * A study, defined as its metadata, its components, and the schedules that activate them.
 *
 * A study is identified by its [id], which is stable for the study's entire lifetime. [studyRevision]
 * increments whenever a new version of the study is released.
 */
@Serializable
data class StudyDefinition(
    val studyRevision: UInt,
    val metadata: Metadata,
    val components: List<Component>,
    val componentSchedules: List<ComponentSchedule>,
) {
    /**
     * The study's stable identifier.
     */
    val id: UUID get() = metadata.id

    /**
     * The component with the given [componentId], or `null` if none exists.
     */
    fun component(componentId: UUID): Component? = components.firstOrNull { it.id == componentId }

    /**
     * All health-data-collection components of the study.
     */
    val healthDataCollectionComponents: List<Component.HealthDataCollection>
        get() = components.filterIsInstance<Component.HealthDataCollection>()
}
