//
// This source file is part of the My Heart Counts Android open-source project
//
// SPDX-FileCopyrightText: 2026 Stanford University and the project authors (see CONTRIBUTORS.md)
//
// SPDX-License-Identifier: MIT

package edu.stanford.spezi.study

import java.time.Instant
import java.util.UUID

/**
 * A participant's enrollment into a study.
 *
 * [enrollmentDate] is the reference point for all relative scheduling; [studyRevision] is the study
 * revision this enrollment was last updated to.
 */
data class StudyEnrollment(
    val id: UUID,
    val enrollmentDate: Instant,
    val studyId: UUID,
    val studyRevision: UInt,
)
