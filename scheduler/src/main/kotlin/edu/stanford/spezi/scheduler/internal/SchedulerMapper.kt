//
// This source file is part of the My Heart Counts Android open-source project
//
// SPDX-FileCopyrightText: 2026 Stanford University and the project authors (see CONTRIBUTORS.md)
//
// SPDX-License-Identifier: MIT

package edu.stanford.spezi.scheduler.internal

import edu.stanford.spezi.scheduler.Outcome
import edu.stanford.spezi.scheduler.Task
import edu.stanford.spezi.scheduler.TaskDraft
import java.time.Instant
import java.util.UUID

/**
 * Translates between the persisted entities and the public scheduler models.
 *
 * The entities already hold the scheduler's value types (Room handles their storage via
 * `SchedulerTypeConverters`), so mapping is field copying plus bridging the persisted
 * epoch-millisecond timestamps to [Instant].
 */
internal class SchedulerMapper {

    fun mapTask(entity: TaskEntity, nextVersionEffectiveFrom: Instant?): Task = Task(
        id = entity.logicalId,
        title = entity.title,
        instructions = entity.instructions,
        category = entity.category,
        schedule = entity.schedule,
        completionPolicy = entity.completionPolicy,
        tags = entity.tags,
        effectiveFrom = Instant.ofEpochMilli(entity.effectiveFromMillis),
        context = entity.context,
        scheduleNotifications = entity.scheduleNotifications,
        notificationThread = entity.notificationThread,
        notificationTime = entity.notificationTime,
        nextVersionEffectiveFrom = nextVersionEffectiveFrom,
    )

    fun mapTaskEntity(
        versionId: String,
        draft: TaskDraft,
        effectiveFrom: Instant,
    ): TaskEntity = TaskEntity(
        versionId = versionId,
        logicalId = draft.id,
        effectiveFromMillis = effectiveFrom.toEpochMilli(),
        title = draft.title,
        instructions = draft.instructions,
        category = draft.category,
        schedule = draft.schedule,
        completionPolicy = draft.completionPolicy,
        tags = draft.tags,
        context = draft.context,
        scheduleNotifications = draft.scheduleNotifications,
        notificationThread = draft.notificationThread,
        notificationTime = draft.notificationTime,
    )

    fun mapOutcome(entity: OutcomeEntity): Outcome = Outcome(
        id = UUID.fromString(entity.id),
        taskId = entity.taskId,
        occurrenceStartDate = Instant.ofEpochMilli(entity.occurrenceStartMillis),
        completionDate = Instant.ofEpochMilli(entity.completionDateMillis),
        context = entity.context,
    )

    fun mapOutcomeEntity(outcome: Outcome): OutcomeEntity = OutcomeEntity(
        id = outcome.id.toString(),
        taskId = outcome.taskId,
        occurrenceStartMillis = outcome.occurrenceStartDate.toEpochMilli(),
        completionDateMillis = outcome.completionDate.toEpochMilli(),
        context = outcome.context,
    )
}
