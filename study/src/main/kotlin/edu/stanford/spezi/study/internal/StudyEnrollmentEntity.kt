//
// This source file is part of the My Heart Counts open-source project
//
// SPDX-FileCopyrightText: 2026 Stanford University and the project authors (see CONTRIBUTORS.md)
//
// SPDX-License-Identifier: MIT

package edu.stanford.spezi.study.internal

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey
import edu.stanford.spezi.study.StudyEnrollment

/**
 * The persisted form of a [StudyEnrollment].
 *
 * UUIDs and the enrollment instant are stored as primitive columns; [studyRevision] is held as a
 * [Long] since Room has no unsigned column type.
 */
@Entity(
    tableName = "study_enrollments",
    indices = [Index("studyId")],
)
internal data class StudyEnrollmentEntity(
    @PrimaryKey val id: String,
    val enrollmentDateMillis: Long,
    val studyId: String,
    val studyRevision: Long,
)
