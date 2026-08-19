//
// This source file is part of the My Heart Counts Android open-source project
//
// SPDX-FileCopyrightText: 2026 Stanford University and the project authors (see CONTRIBUTORS.md)
//
// SPDX-License-Identifier: MIT

package org.grovealliance.study.fixtures

import org.grovealliance.foundation.fixtures.UUIDFixtures
import org.grovealliance.study.StudyContext
import java.util.UUID

/**
 * Fixture for [StudyContext].
 */
object StudyContextFixtures {
    fun create(
        studyId: UUID = UUIDFixtures.zero,
        componentId: UUID = UUIDFixtures.zero,
        scheduleId: UUID = UUIDFixtures.zero,
        enrollmentId: UUID = UUIDFixtures.zero,
    ): StudyContext = StudyContext(
        studyId = studyId,
        componentId = componentId,
        scheduleId = scheduleId,
        enrollmentId = enrollmentId,
    )
}
