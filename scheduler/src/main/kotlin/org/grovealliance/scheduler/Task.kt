//
// This source file is part of the My Heart Counts Android open-source project
//
// SPDX-FileCopyrightText: 2026 Stanford University and the project authors (see CONTRIBUTORS.md)
//
// SPDX-License-Identifier: MIT

package org.grovealliance.scheduler

import java.time.Instant

/**
 * Some action or work a user is supposed to perform, possibly repeatedly.
 *
 * A single occurrence of a task is an [Event], derived from the task's [schedule].
 *
 * Tasks are stored in an append-only, versioned form: changing a task's content creates a new
 * version effective from a given date, leaving past occurrences untouched. A value of this type
 * represents one version. Version navigation is provided by the [Scheduler]
 * ([Scheduler.latestVersion], [Scheduler.allVersions]) rather than by inter-object references.
 */
data class Task(
    val id: String,
    val title: String,
    val instructions: String,
    val category: TaskCategory?,
    val schedule: Schedule,
    val completionPolicy: AllowedCompletionPolicy,
    val tags: List<String>,
    val effectiveFrom: Instant,
    val context: TaskContext,
    /**
     * Whether local notifications are automatically delivered for this task's upcoming events.
     */
    val scheduleNotifications: Boolean,
    /**
     * How this task's event notifications are grouped.
     */
    val notificationThread: NotificationThread,
    /**
     * The time of day at which notifications are delivered, overriding each event's start time.
     * `null` delivers timed events at their start and all-day events at the scheduler's default time.
     */
    val notificationTime: NotificationTime?,
    /**
     * The [effectiveFrom] of the next version of this task, beyond which this version no longer
     * produces occurrences. `null` when this is the latest version.
     */
    val nextVersionEffectiveFrom: Instant?,
) {
    /**
     * Whether this is the most recent version of the task.
     */
    val isLatestVersion: Boolean get() = nextVersionEffectiveFrom == null
}

/**
 * The content of a task to create or update.
 *
 * Carries everything that identifies and describes a task version. The versioning metadata
 * ([Task.effectiveFrom], [Task.nextVersionEffectiveFrom]) is supplied and derived by the [Scheduler].
 */
data class TaskDraft(
    val id: String,
    val title: String,
    val instructions: String,
    val schedule: Schedule,
    val category: TaskCategory? = null,
    val completionPolicy: AllowedCompletionPolicy = AllowedCompletionPolicy.SAME_DAY,
    val tags: List<String> = emptyList(),
    val context: TaskContext = TaskContext(),
    val scheduleNotifications: Boolean = false,
    val notificationThread: NotificationThread = NotificationThread.None,
    val notificationTime: NotificationTime? = null,
)

/**
 * The result of creating or updating a task.
 */
data class TaskUpdateResult(
    val task: Task,
    val didChange: Boolean,
)
