//
// This source file is part of the My Heart Counts Android open-source project
//
// SPDX-FileCopyrightText: 2026 Stanford University and the project authors (see CONTRIBUTORS.md)
//
// SPDX-License-Identifier: MIT

package edu.stanford.spezi.study.internal

import edu.stanford.spezi.study.StudyEnrollment
import java.time.Instant
import java.util.UUID

/**
 * Translates between the persisted [StudyEnrollmentEntity] and the public [StudyEnrollment] model.
 */
internal class StudyEnrollmentMapper {

    fun mapModel(entity: StudyEnrollmentEntity): StudyEnrollment = StudyEnrollment(
        id = UUID.fromString(entity.id),
        enrollmentDate = Instant.ofEpochMilli(entity.enrollmentDateMillis),
        studyId = UUID.fromString(entity.studyId),
        studyRevision = entity.studyRevision.toUInt(),
    )

    fun mapEntity(model: StudyEnrollment): StudyEnrollmentEntity = StudyEnrollmentEntity(
        id = model.id.toString(),
        enrollmentDateMillis = model.enrollmentDate.toEpochMilli(),
        studyId = model.studyId.toString(),
        studyRevision = model.studyRevision.toLong(),
    )
}
