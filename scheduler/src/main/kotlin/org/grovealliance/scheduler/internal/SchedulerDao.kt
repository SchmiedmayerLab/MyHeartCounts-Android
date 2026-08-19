//
// This source file is part of the My Heart Counts Android open-source project
//
// SPDX-FileCopyrightText: 2026 Stanford University and the project authors (see CONTRIBUTORS.md)
//
// SPDX-License-Identifier: MIT

@file:Suppress("detekt:TooManyFunctions")

package org.grovealliance.scheduler.internal

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Upsert
import kotlinx.coroutines.flow.Flow

/**
 * Persistence access for the versioned task store and its outcomes.
 */
@Dao
internal interface SchedulerDao {

    @Query("SELECT * FROM task_versions")
    suspend fun allTaskVersions(): List<TaskEntity>

    @Query("SELECT * FROM task_versions")
    fun observeTaskVersions(): Flow<List<TaskEntity>>

    @Query("SELECT * FROM outcomes")
    fun observeOutcomes(): Flow<List<OutcomeEntity>>

    @Query("SELECT * FROM task_versions WHERE logicalId = :logicalId ORDER BY effectiveFromMillis ASC")
    suspend fun versions(logicalId: String): List<TaskEntity>

    @Upsert
    suspend fun upsertTask(task: TaskEntity)

    @Query("DELETE FROM task_versions WHERE logicalId = :logicalId")
    suspend fun deleteTaskVersions(logicalId: String)

    @Query("DELETE FROM task_versions WHERE logicalId = :logicalId AND effectiveFromMillis >= :fromMillis")
    suspend fun deleteTaskVersionsFrom(logicalId: String, fromMillis: Long)

    @Query("DELETE FROM task_versions")
    suspend fun deleteAllTaskVersions()

    @Query("SELECT * FROM outcomes")
    suspend fun allOutcomes(): List<OutcomeEntity>

    @Query("SELECT * FROM outcomes WHERE taskId = :taskId")
    suspend fun outcomesForTask(taskId: String): List<OutcomeEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertOutcome(outcome: OutcomeEntity)

    @Query("DELETE FROM outcomes WHERE taskId = :taskId")
    suspend fun deleteOutcomes(taskId: String)

    @Query("DELETE FROM outcomes WHERE taskId = :taskId AND occurrenceStartMillis >= :fromMillis")
    suspend fun deleteOutcomesFrom(taskId: String, fromMillis: Long)

    @Query("DELETE FROM outcomes")
    suspend fun deleteAllOutcomes()
}
