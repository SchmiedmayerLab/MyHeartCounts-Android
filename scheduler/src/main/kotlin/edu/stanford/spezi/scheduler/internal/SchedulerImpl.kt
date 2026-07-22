//
// This source file is part of the My Heart Counts open-source project
//
// SPDX-FileCopyrightText: 2026 Stanford University and the project authors (see CONTRIBUTORS.md)
//
// SPDX-License-Identifier: MIT

@file:Suppress("detekt:TooManyFunctions")

package edu.stanford.spezi.scheduler.internal

import edu.stanford.spezi.core.time.TimeProvider
import edu.stanford.spezi.scheduler.Event
import edu.stanford.spezi.scheduler.InstantRange
import edu.stanford.spezi.scheduler.Outcome
import edu.stanford.spezi.scheduler.OutcomeContext
import edu.stanford.spezi.scheduler.ScheduleCalculator
import edu.stanford.spezi.scheduler.Scheduler
import edu.stanford.spezi.scheduler.ShadowedOutcomesHandling
import edu.stanford.spezi.scheduler.Task
import edu.stanford.spezi.scheduler.TaskDraft
import edu.stanford.spezi.scheduler.TaskUpdateResult
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.map
import java.time.Instant
import java.util.UUID

/**
 * Room-backed implementation of the versioned task store.
 */
internal class SchedulerImpl(
    private val database: SchedulerDatabase,
    private val timeProvider: TimeProvider,
    private val mapper: SchedulerMapper,
    private val scheduleCalculator: ScheduleCalculator,
) : Scheduler {

    private val dao get() = database.dao()

    private val outcomesFlow = MutableSharedFlow<Outcome>(extraBufferCapacity = OUTCOME_BUFFER)

    override val newOutcomes: Flow<Outcome> = outcomesFlow.asSharedFlow()

    override suspend fun createOrUpdateTask(
        draft: TaskDraft,
        effectiveFrom: Instant,
        shadowedOutcomesHandling: ShadowedOutcomesHandling,
    ): Result<TaskUpdateResult> = runCatching {
        val latest = dao.versions(draft.id).maxByOrNull { it.effectiveFromMillis }

        if (latest != null && !wouldChange(latest, draft)) {
            return@runCatching TaskUpdateResult(
                task = mapper.mapTask(latest, nextVersionEffectiveFrom = null),
                didChange = false,
            )
        }

        if (latest != null) {
            val shadowed = dao.outcomesForTask(draft.id).filter { it.occurrenceStartMillis >= effectiveFrom.toEpochMilli() }
            when (shadowedOutcomesHandling) {
                ShadowedOutcomesHandling.THROW_ERROR ->
                    if (shadowed.isNotEmpty()) error("An updated task cannot shadow outcomes of a previous version.")
                ShadowedOutcomesHandling.DELETE ->
                    if (shadowed.isNotEmpty()) dao.deleteOutcomesFrom(draft.id, effectiveFrom.toEpochMilli())
            }
        }

        val entity = mapper.mapTaskEntity(
            versionId = UUID.randomUUID().toString(),
            draft = draft,
            effectiveFrom = effectiveFrom,
        )
        dao.upsertTask(entity)
        TaskUpdateResult(
            task = mapper.mapTask(entity, nextVersionEffectiveFrom = null),
            didChange = true,
        )
    }

    private fun wouldChange(latest: TaskEntity, draft: TaskDraft): Boolean =
        latest.title != draft.title ||
            latest.instructions != draft.instructions ||
            latest.category != draft.category ||
            latest.completionPolicy != draft.completionPolicy ||
            latest.schedule != draft.schedule ||
            latest.tags != draft.tags ||
            latest.context != draft.context ||
            latest.scheduleNotifications != draft.scheduleNotifications ||
            latest.notificationThread != draft.notificationThread ||
            latest.notificationTime != draft.notificationTime

    override suspend fun queryTasks(
        range: InstantRange,
        predicate: (Task) -> Boolean,
        sortBy: Comparator<Task>?,
        fetchLimit: Int?,
    ): List<Task> {
        val effectiveFromComparator = compareBy<Task> { it.effectiveFrom }
        val comparator = sortBy?.then(effectiveFromComparator) ?: effectiveFromComparator
        val tasks = effectiveVersions(dao.allTaskVersions(), range)
            .map { it.task }
            .filter(predicate)
            .sortedWith(comparator)
        return if (fetchLimit != null) tasks.take(fetchLimit) else tasks
    }

    override suspend fun latestVersion(taskId: String): Task? =
        dao.versions(taskId).maxByOrNull { it.effectiveFromMillis }
            ?.let { mapper.mapTask(it, nextVersionEffectiveFrom = null) }

    override suspend fun allVersions(taskId: String): List<Task> {
        val sorted = dao.versions(taskId).sortedBy { it.effectiveFromMillis }
        return sorted.mapIndexed { index, entity ->
            val next = sorted.getOrNull(index + 1)?.effectiveFromMillis?.let(Instant::ofEpochMilli)
            mapper.mapTask(entity, nextVersionEffectiveFrom = next)
        }
    }

    override fun queryEvents(range: InstantRange, predicate: (Task) -> Boolean): Flow<List<Event>> =
        combine(dao.observeTaskVersions(), dao.observeOutcomes()) { taskVersions, outcomes ->
            val lower = range.start.toEpochMilli()
            val upper = range.endExclusive.toEpochMilli()
            val outcomesByOccurrence = outcomes
                .filter { it.occurrenceStartMillis in lower until upper }
                .associateBy { it.taskId to it.occurrenceStartMillis }
            val versions = effectiveVersions(taskVersions, range).filter { predicate(it.task) }
            assembleEvents(range, versions, outcomesByOccurrence)
        }

    override fun queryMissedEvents(range: InstantRange, predicate: (Task) -> Boolean): Flow<List<Event>> =
        queryEvents(range, predicate).map { events ->
            val now = timeProvider.nowInstant()
            events.filter { !it.isCompleted && it.occurrence.end < now }
        }

    override fun queryEvents(taskId: String, range: InstantRange): Flow<List<Event>> =
        queryEvents(range) { it.id == taskId }

    override fun queryEvents(task: Task, range: InstantRange): Flow<List<Event>> =
        dao.observeOutcomes().map { outcomes ->
            val upper = task.nextVersionEffectiveFrom?.let { minOf(it, range.endExclusive) } ?: range.endExclusive
            val outcomesByStart = outcomes
                .filter { it.taskId == task.id }
                .associateBy { it.occurrenceStartMillis }
            val occurrenceRange = InstantRange(
                start = range.start,
                endExclusive = upper,
            )
            scheduleCalculator.occurrences(schedule = task.schedule, range = occurrenceRange).map { occurrence ->
                val outcome = outcomesByStart[occurrence.start.toEpochMilli()]?.let(mapper::mapOutcome)
                Event(
                    task = task,
                    occurrence = occurrence,
                    outcome = outcome,
                )
            }
        }

    override suspend fun complete(
        event: Event,
        ignoreCompletionPolicy: Boolean,
        context: OutcomeContext,
    ): Result<Outcome> = runCatching {
        check(ignoreCompletionPolicy || scheduleCalculator.isAllowedToComplete(event = event)) {
            "The completion policy does not allow completing this event now."
        }
        val existing = event.outcome
        val outcome = existing?.copy(context = context) ?: Outcome(
            id = UUID.randomUUID(),
            taskId = event.task.id,
            occurrenceStartDate = event.occurrence.start,
            completionDate = timeProvider.nowInstant(),
            context = context,
        )
        dao.upsertOutcome(mapper.mapOutcomeEntity(outcome))
        if (existing == null) outcomesFlow.tryEmit(outcome)
        outcome
    }

    override suspend fun deleteTask(task: Task) {
        val from = task.effectiveFrom.toEpochMilli()
        dao.deleteOutcomesFrom(task.id, from)
        dao.deleteTaskVersionsFrom(task.id, from)
    }

    override suspend fun deleteTasks(tasks: Collection<Task>) {
        // Per logical task, delete from the oldest requested version onward (which subsumes newer ones).
        tasks.groupBy { it.id }
            .forEach { (_, versions) -> versions.minByOrNull { it.effectiveFrom }?.let { deleteTask(it) } }
    }

    override suspend fun deleteAllVersions(taskId: String) {
        dao.deleteOutcomes(taskId)
        dao.deleteTaskVersions(taskId)
    }

    /**
     * The task versions effective within [range], each paired with whether an earlier version exists.
     */
    private fun effectiveVersions(allVersions: List<TaskEntity>, range: InstantRange): List<EffectiveVersion> {
        val lower = range.start.toEpochMilli()
        val upper = range.endExclusive.toEpochMilli()
        return allVersions
            .groupBy { it.logicalId }
            .values
            .flatMap { group ->
                val sorted = group.sortedBy { it.effectiveFromMillis }
                sorted.mapIndexedNotNull { index, entity ->
                    val next = sorted.getOrNull(index + 1)?.effectiveFromMillis
                    val effectiveInRange = entity.effectiveFromMillis < upper && (next == null || lower < next)
                    if (!effectiveInRange) {
                        null
                    } else {
                        EffectiveVersion(
                            task = mapper.mapTask(entity, next?.let(Instant::ofEpochMilli)),
                            hasPreviousVersion = index > 0,
                        )
                    }
                }
            }
            .sortedBy { it.task.effectiveFrom }
    }

    private fun assembleEvents(
        range: InstantRange,
        versions: List<EffectiveVersion>,
        outcomesByOccurrence: Map<Pair<String, Long>, OutcomeEntity>,
    ): List<Event> = versions
        .flatMap { version ->
            val task = version.task
            val upperBound = task.nextVersionEffectiveFrom
                ?.let { minOf(it, range.endExclusive) } ?: range.endExclusive
            val lowerBound = if (version.hasPreviousVersion) maxOf(task.effectiveFrom, range.start) else range.start
            val occurrenceRange = InstantRange(
                start = lowerBound,
                endExclusive = upperBound,
            )
            scheduleCalculator.occurrences(schedule = task.schedule, range = occurrenceRange).map { occurrence ->
                val outcome = outcomesByOccurrence[task.id to occurrence.start.toEpochMilli()]
                    ?.let(mapper::mapOutcome)
                Event(
                    task = task,
                    occurrence = occurrence,
                    outcome = outcome,
                )
            }
        }
        .sortedBy { it.occurrence.start }

    private data class EffectiveVersion(
        val task: Task,
        val hasPreviousVersion: Boolean,
    )

    private companion object {
        const val OUTCOME_BUFFER = 64
    }
}
