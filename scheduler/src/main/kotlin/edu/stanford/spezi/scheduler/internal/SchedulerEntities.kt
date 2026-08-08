//
// This source file is part of the My Heart Counts open-source project
//
// SPDX-FileCopyrightText: 2026 Stanford University and the project authors (see CONTRIBUTORS.md)
//
// SPDX-License-Identifier: MIT

package edu.stanford.spezi.scheduler.internal

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey
import edu.stanford.spezi.scheduler.AllowedCompletionPolicy
import edu.stanford.spezi.scheduler.NotificationThread
import edu.stanford.spezi.scheduler.NotificationTime
import edu.stanford.spezi.scheduler.OutcomeContext
import edu.stanford.spezi.scheduler.Schedule
import edu.stanford.spezi.scheduler.TaskCategory
import edu.stanford.spezi.scheduler.TaskContext

/**
 * A single, immutable version of a task.
 *
 * Versions of the same logical task share [logicalId] and are ordered by [effectiveFromMillis].
 * Value-typed columns are persisted via [SchedulerTypeConverters].
 */
@Entity(
    tableName = "task_versions",
    indices = [Index("logicalId"), Index("logicalId", "effectiveFromMillis")],
)
internal data class TaskEntity(
    @PrimaryKey val versionId: String,
    val logicalId: String,
    val effectiveFromMillis: Long,
    val title: String,
    val instructions: String,
    val category: TaskCategory?,
    val schedule: Schedule,
    val completionPolicy: AllowedCompletionPolicy,
    val tags: List<String>,
    val context: TaskContext,
    val scheduleNotifications: Boolean,
    val notificationThread: NotificationThread,
    val notificationTime: NotificationTime?,
)

/**
 * A recorded completion of one occurrence of a task.
 */
@Entity(
    tableName = "outcomes",
    indices = [Index("taskId"), Index("taskId", "occurrenceStartMillis")],
)
internal data class OutcomeEntity(
    @PrimaryKey val id: String,
    val taskId: String,
    val occurrenceStartMillis: Long,
    val completionDateMillis: Long,
    val context: OutcomeContext,
)
