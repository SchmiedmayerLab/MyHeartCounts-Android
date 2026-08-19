//
// This source file is part of the My Heart Counts Android open-source project
//
// SPDX-FileCopyrightText: 2026 Stanford University and the project authors (see CONTRIBUTORS.md)
//
// SPDX-License-Identifier: MIT

package org.grovealliance.study.internal

import androidx.room.Dao
import androidx.room.Query
import androidx.room.Upsert
import kotlinx.coroutines.flow.Flow

/**
 * Persistence access for study enrollments.
 */
@Dao
internal interface StudyEnrollmentDao {

    @Query("SELECT * FROM study_enrollments")
    suspend fun all(): List<StudyEnrollmentEntity>

    @Query("SELECT * FROM study_enrollments")
    fun observeAll(): Flow<List<StudyEnrollmentEntity>>

    @Query("SELECT * FROM study_enrollments WHERE id = :id")
    suspend fun byId(id: String): StudyEnrollmentEntity?

    @Query("SELECT * FROM study_enrollments WHERE studyId = :studyId")
    suspend fun byStudyId(studyId: String): List<StudyEnrollmentEntity>

    @Upsert
    suspend fun upsert(enrollment: StudyEnrollmentEntity)

    @Query("UPDATE study_enrollments SET studyRevision = :revision WHERE id = :id")
    suspend fun updateRevision(id: String, revision: Long)

    @Query("DELETE FROM study_enrollments WHERE id = :id")
    suspend fun delete(id: String)

    @Query("DELETE FROM study_enrollments")
    suspend fun deleteAll()
}
