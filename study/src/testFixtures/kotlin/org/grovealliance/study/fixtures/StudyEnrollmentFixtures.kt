//
// This source file is part of the My Heart Counts Android open-source project
//
// SPDX-FileCopyrightText: 2026 Stanford University and the project authors (see CONTRIBUTORS.md)
//
// SPDX-License-Identifier: MIT

package org.grovealliance.study.fixtures

import org.grovealliance.foundation.fixtures.InstantFixtures
import org.grovealliance.foundation.fixtures.UUIDFixtures
import org.grovealliance.study.StudyEnrollment
import java.time.Instant
import java.util.UUID

/**
 * Fixture for [StudyEnrollment].
 */
object StudyEnrollmentFixtures {
    fun create(
        id: UUID = UUIDFixtures.zero,
        enrollmentDate: Instant = InstantFixtures.reference,
        studyId: UUID = UUIDFixtures.zero,
        studyRevision: UInt = 0u,
    ): StudyEnrollment = StudyEnrollment(
        id = id,
        enrollmentDate = enrollmentDate,
        studyId = studyId,
        studyRevision = studyRevision,
    )
}
