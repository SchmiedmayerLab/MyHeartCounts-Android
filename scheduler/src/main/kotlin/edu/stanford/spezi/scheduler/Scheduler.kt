//
// This source file is part of the My Heart Counts open-source project
//
// SPDX-FileCopyrightText: 2026 Stanford University and the project authors (see CONTRIBUTORS.md)
//
// SPDX-License-Identifier: MIT

@file:Suppress("detekt:TooManyFunctions")

package edu.stanford.spezi.scheduler

import edu.stanford.spezi.core.Module
import kotlinx.coroutines.flow.Flow
import java.time.Instant

/**
 * Schedules and queries tasks and their events.
 *
 * The scheduler maintains a versioned, append-only store of [Task]s. Modifying a task's content
 * creates a new version effective from a given date, so that occurrences of the past are preserved.
 * Use [createOrUpdateTask] to keep a task up to date, and [queryEvents] to retrieve the resulting
 * occurrences together with their [Outcome]s.
 */
interface Scheduler : Module {

    /**
     * Emits each [Outcome] as it is recorded.
     */
    val newOutcomes: Flow<Outcome>

    /**
     * Adds [draft] as a task, or adds a new version of it if one with the same [TaskDraft.id] already
     * exists and its content changed.
     *
     * @param effectiveFrom the date from which [draft] takes effect.
     * @param shadowedOutcomesHandling how to react when the update would shadow outcomes already
     *   recorded at or after [effectiveFrom].
     * @return the resulting latest version and whether anything changed, or a failure when
     *   [shadowedOutcomesHandling] is [ShadowedOutcomesHandling.THROW_ERROR] and the update would
     *   shadow existing outcomes.
     */
    suspend fun createOrUpdateTask(
        draft: TaskDraft,
        effectiveFrom: Instant,
        shadowedOutcomesHandling: ShadowedOutcomesHandling = ShadowedOutcomesHandling.THROW_ERROR,
    ): Result<TaskUpdateResult>

    /**
     * The task versions effective within [range] that satisfy [predicate].
     *
     * May return more than one version of the same task if a version boundary falls inside [range].
     * The result is ordered by [sortBy] when given, then always by [Task.effectiveFrom] ascending,
     * and truncated to [fetchLimit] entries when non-null.
     */
    suspend fun queryTasks(
        range: InstantRange,
        predicate: (Task) -> Boolean = { true },
        sortBy: Comparator<Task>? = null,
        fetchLimit: Int? = null,
    ): List<Task>

    /**
     * A live stream of the events occurring within [range] for tasks satisfying [predicate], sorted
     * by occurrence.
     *
     * The flow re-emits whenever a task version or outcome changes, so observers always see the
     * current state of the store.
     */
    fun queryEvents(
        range: InstantRange,
        predicate: (Task) -> Boolean = { true },
    ): Flow<List<Event>>

    /**
     * A live stream of the events within [range] that are missed: not completed and whose occurrence
     * has fully elapsed.
     */
    fun queryMissedEvents(
        range: InstantRange,
        predicate: (Task) -> Boolean = { true },
    ): Flow<List<Event>>

    /**
     * A live stream of the events of the task with [taskId] occurring within [range].
     */
    fun queryEvents(taskId: String, range: InstantRange): Flow<List<Event>>

    /**
     * A live stream of the events of the specific task version [task] occurring within [range].
     *
     * Occurrences are bounded by [Task.nextVersionEffectiveFrom], so only the window in which [task]
     * is effective contributes events.
     */
    fun queryEvents(task: Task, range: InstantRange): Flow<List<Event>>

    /**
     * The latest version of the task with [taskId], or `null` if no such task exists.
     */
    suspend fun latestVersion(taskId: String): Task?

    /**
     * All versions of the task with [taskId], ordered from oldest to newest.
     */
    suspend fun allVersions(taskId: String): List<Task>

    /**
     * Records an [Outcome] completing [event], storing [context] on it.
     *
     * If the event is already completed, the existing outcome is updated. Fails with an
     * [IllegalStateException] if the task's completion policy disallows completion at this time and
     * [ignoreCompletionPolicy] is `false`.
     */
    suspend fun complete(
        event: Event,
        ignoreCompletionPolicy: Boolean = false,
        context: OutcomeContext = OutcomeContext(),
    ): Result<Outcome>

    /**
     * Permanently deletes the given task [version][task] and all later versions of the same task,
     * together with their outcomes.
     *
     * Earlier versions are kept; the newest remaining version becomes effective again. To stop a task
     * from recurring without removing history, instead add a new version whose schedule produces no
     * further occurrences.
     */
    suspend fun deleteTask(task: Task)

    /**
     * Applies [deleteTask] to each task in [tasks].
     */
    suspend fun deleteTasks(tasks: Collection<Task>)

    /**
     * Permanently deletes all versions of the task with [taskId], and their outcomes.
     */
    suspend fun deleteAllVersions(taskId: String)
}

/**
 * Permanently deletes all versions of [task] and their outcomes.
 */
suspend fun Scheduler.deleteAllVersions(task: Task): Unit = deleteAllVersions(task.id)

/**
 * How an update that would shadow already-recorded outcomes is handled.
 */
enum class ShadowedOutcomesHandling {
    /**
     * The update fails with an [IllegalStateException].
     */
    THROW_ERROR,

    /**
     * The shadowed outcomes are deleted.
     */
    DELETE,
}
