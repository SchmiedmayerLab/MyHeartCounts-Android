//
// This source file is part of the My Heart Counts open-source project
//
// SPDX-FileCopyrightText: 2026 Stanford University and the project authors (see CONTRIBUTORS.md)
//
// SPDX-License-Identifier: MIT

package edu.stanford.spezi.scheduler

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.map
import java.time.Instant
import java.util.UUID

/**
 * An in-memory [Scheduler] for tests, keeping the latest version of each task by id.
 *
 * It fully supports task creation, lookup, prefix-based queries, completion, and deletion. Occurrence
 * math is not modelled: the events returned by [queryEvents] and [queryMissedEvents] are the ones
 * supplied through [setEvents] and [setMissedEvents], narrowed to the queried range. Tests that need
 * real occurrence math should drive the production scheduler instead.
 */
class FakeScheduler : Scheduler {

    /**
     * The latest stored version of each task, keyed by task id, in insertion order.
     */
    val tasks: Map<String, Task> get() = taskStore

    /**
     * Every outcome recorded through [complete].
     */
    val outcomes: List<Outcome> get() = outcomeStore

    private val taskStore = linkedMapOf<String, Task>()
    private val outcomeStore = mutableListOf<Outcome>()

    private val eventStore = MutableStateFlow<List<Event>>(emptyList())
    private val missedEventStore = MutableStateFlow<List<Event>>(emptyList())

    private val outcomesFlow = MutableSharedFlow<Outcome>(extraBufferCapacity = OUTCOME_BUFFER)
    override val newOutcomes: Flow<Outcome> = outcomesFlow.asSharedFlow()

    /**
     * Sets the events [queryEvents] draws from.
     */
    fun setEvents(events: List<Event>) {
        eventStore.value = events
    }

    /**
     * Sets the events [queryMissedEvents] draws from.
     */
    fun setMissedEvents(events: List<Event>) {
        missedEventStore.value = events
    }

    override suspend fun createOrUpdateTask(
        draft: TaskDraft,
        effectiveFrom: Instant,
        shadowedOutcomesHandling: ShadowedOutcomesHandling,
    ): Result<TaskUpdateResult> = runCatching {
        val task = Task(
            id = draft.id,
            title = draft.title,
            instructions = draft.instructions,
            category = draft.category,
            schedule = draft.schedule,
            completionPolicy = draft.completionPolicy,
            tags = draft.tags,
            effectiveFrom = effectiveFrom,
            context = draft.context,
            scheduleNotifications = draft.scheduleNotifications,
            notificationThread = draft.notificationThread,
            notificationTime = draft.notificationTime,
            nextVersionEffectiveFrom = null,
        )
        val changed = taskStore.put(draft.id, task) != task
        TaskUpdateResult(
            task = task,
            didChange = changed,
        )
    }

    override suspend fun queryTasks(
        range: InstantRange,
        predicate: (Task) -> Boolean,
        sortBy: Comparator<Task>?,
        fetchLimit: Int?,
    ): List<Task> {
        val filtered = taskStore.values.filter(predicate)
        val sorted = sortBy?.let { filtered.sortedWith(it) } ?: filtered
        return fetchLimit?.let { sorted.take(it) } ?: sorted
    }

    override fun queryEvents(range: InstantRange, predicate: (Task) -> Boolean): Flow<List<Event>> =
        eventStore.map { events -> events.filter { it.matches(range) && predicate(it.task) } }

    override fun queryMissedEvents(range: InstantRange, predicate: (Task) -> Boolean): Flow<List<Event>> =
        missedEventStore.map { events -> events.filter { it.matches(range) && predicate(it.task) } }

    override fun queryEvents(taskId: String, range: InstantRange): Flow<List<Event>> =
        queryEvents(range = range, predicate = { it.id == taskId })

    override fun queryEvents(task: Task, range: InstantRange): Flow<List<Event>> =
        queryEvents(taskId = task.id, range = range)

    private fun Event.matches(range: InstantRange) = occurrence.start in range

    override suspend fun latestVersion(taskId: String): Task? = taskStore[taskId]

    override suspend fun allVersions(taskId: String): List<Task> = listOfNotNull(taskStore[taskId])

    override suspend fun complete(
        event: Event,
        ignoreCompletionPolicy: Boolean,
        context: OutcomeContext,
    ): Result<Outcome> = runCatching {
        val outcome = Outcome(
            id = UUID.randomUUID(),
            taskId = event.task.id,
            occurrenceStartDate = event.occurrence.start,
            completionDate = event.occurrence.start,
            context = context,
        )
        outcomeStore.add(outcome)
        outcomesFlow.tryEmit(outcome)
        outcome
    }

    override suspend fun deleteTask(task: Task) {
        taskStore.remove(task.id)
    }

    override suspend fun deleteTasks(tasks: Collection<Task>) {
        tasks.forEach { deleteTask(it) }
    }

    override suspend fun deleteAllVersions(taskId: String) {
        taskStore.remove(taskId)
        outcomeStore.removeAll { it.taskId == taskId }
    }

    private companion object {
        const val OUTCOME_BUFFER = 16
    }
}
