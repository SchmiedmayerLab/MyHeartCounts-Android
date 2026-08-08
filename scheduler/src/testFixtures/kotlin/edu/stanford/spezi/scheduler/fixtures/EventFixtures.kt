//
// This source file is part of the My Heart Counts open-source project
//
// SPDX-FileCopyrightText: 2026 Stanford University and the project authors (see CONTRIBUTORS.md)
//
// SPDX-License-Identifier: MIT

package edu.stanford.spezi.scheduler.fixtures

import edu.stanford.spezi.foundation.fixtures.InstantFixtures
import edu.stanford.spezi.foundation.fixtures.UUIDFixtures
import edu.stanford.spezi.scheduler.AllowedCompletionPolicy
import edu.stanford.spezi.scheduler.Event
import edu.stanford.spezi.scheduler.NotificationThread
import edu.stanford.spezi.scheduler.NotificationTime
import edu.stanford.spezi.scheduler.Occurrence
import edu.stanford.spezi.scheduler.Outcome
import edu.stanford.spezi.scheduler.OutcomeContext
import edu.stanford.spezi.scheduler.Schedule
import edu.stanford.spezi.scheduler.Task
import edu.stanford.spezi.scheduler.TaskCategory
import edu.stanford.spezi.scheduler.TaskContext
import java.time.Instant
import java.util.UUID

/**
 * Fixture for [Task].
 */
object TaskFixtures {
    @Suppress("LongParameterList")
    fun create(
        id: String = "task",
        title: String = "Task",
        instructions: String = "",
        category: TaskCategory? = null,
        schedule: Schedule = Schedule.once(at = InstantFixtures.reference),
        completionPolicy: AllowedCompletionPolicy = AllowedCompletionPolicy.ANYTIME,
        tags: List<String> = emptyList(),
        effectiveFrom: Instant = InstantFixtures.reference,
        context: TaskContext = TaskContext(),
        scheduleNotifications: Boolean = false,
        notificationThread: NotificationThread = NotificationThread.None,
        notificationTime: NotificationTime? = null,
        nextVersionEffectiveFrom: Instant? = null,
    ): Task = Task(
        id = id,
        title = title,
        instructions = instructions,
        category = category,
        schedule = schedule,
        completionPolicy = completionPolicy,
        tags = tags,
        effectiveFrom = effectiveFrom,
        context = context,
        scheduleNotifications = scheduleNotifications,
        notificationThread = notificationThread,
        notificationTime = notificationTime,
        nextVersionEffectiveFrom = nextVersionEffectiveFrom,
    )
}

/**
 * Fixture for [Occurrence].
 */
object OccurrenceFixtures {
    fun create(
        start: Instant = InstantFixtures.reference,
        end: Instant = start.plusSeconds(SECONDS_PER_HOUR),
        schedule: Schedule = Schedule.once(at = start),
    ): Occurrence = Occurrence(
        start = start,
        end = end,
        schedule = schedule,
    )

    private const val SECONDS_PER_HOUR = 3600L
}

/**
 * Fixture for [Outcome].
 */
object OutcomeFixtures {
    fun create(
        id: UUID = UUIDFixtures.repeating('a'),
        taskId: String = "task",
        occurrenceStartDate: Instant = InstantFixtures.reference,
        completionDate: Instant = InstantFixtures.reference,
        context: OutcomeContext = OutcomeContext(),
    ): Outcome = Outcome(
        id = id,
        taskId = taskId,
        occurrenceStartDate = occurrenceStartDate,
        completionDate = completionDate,
        context = context,
    )
}

/**
 * Fixture for [Event].
 */
object EventFixtures {
    fun create(
        task: Task = TaskFixtures.create(),
        occurrence: Occurrence = OccurrenceFixtures.create(schedule = task.schedule),
        outcome: Outcome? = null,
    ): Event = Event(
        task = task,
        occurrence = occurrence,
        outcome = outcome,
    )
}
