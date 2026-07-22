//
// This source file is part of the My Heart Counts open-source project
//
// SPDX-FileCopyrightText: 2026 Stanford University and the project authors (see CONTRIBUTORS.md)
//
// SPDX-License-Identifier: MIT

package edu.stanford.spezi.study.fixtures

import edu.stanford.spezi.foundation.fixtures.UUIDFixtures
import edu.stanford.spezi.study.StudyContext
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
