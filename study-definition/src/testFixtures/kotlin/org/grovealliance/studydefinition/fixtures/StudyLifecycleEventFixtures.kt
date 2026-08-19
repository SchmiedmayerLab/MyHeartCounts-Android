//
// This source file is part of the My Heart Counts Android open-source project
//
// SPDX-FileCopyrightText: 2026 Stanford University and the project authors (see CONTRIBUTORS.md)
//
// SPDX-License-Identifier: MIT

package org.grovealliance.studydefinition.fixtures

import org.grovealliance.foundation.fixtures.UUIDFixtures
import org.grovealliance.studydefinition.StudyLifecycleEvent
import java.util.UUID

/**
 * Fixtures for [StudyLifecycleEvent]. [create] returns [StudyLifecycleEvent.Enrollment].
 */
object StudyLifecycleEventFixtures {
    fun create(): StudyLifecycleEvent = StudyLifecycleEvent.Enrollment

    fun createCompletedTask(componentId: UUID = UUIDFixtures.zero): StudyLifecycleEvent.CompletedTask =
        StudyLifecycleEvent.CompletedTask(componentId)
}
