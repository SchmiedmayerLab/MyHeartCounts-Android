//
// This source file is part of the My Heart Counts open-source project
//
// SPDX-FileCopyrightText: 2026 Stanford University and the project authors (see CONTRIBUTORS.md)
//
// SPDX-License-Identifier: MIT

package edu.stanford.spezi.scheduler.internal

import edu.stanford.spezi.core.time.TimeProvider
import edu.stanford.spezi.scheduler.Event
import edu.stanford.spezi.scheduler.InstantRange
import edu.stanford.spezi.scheduler.ScheduleDuration
import edu.stanford.spezi.scheduler.Scheduler
import edu.stanford.spezi.scheduler.SchedulerNotifications
import edu.stanford.spezi.scheduler.SchedulerNotificationsConfiguration
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

/**
 * Keeps the platform's pending task notifications in sync with the scheduler's store.
 *
 * On configuration it performs an initial refresh and then re-refreshes whenever the store changes.
 */
internal class SchedulerNotificationsImpl(
    private val scheduler: Scheduler,
    private val timeProvider: TimeProvider,
    private val scope: CoroutineScope,
    private val notificationScheduler: NotificationScheduler,
    private val planner: NotificationPlanner,
    configuration: SchedulerNotificationsConfiguration,
) : SchedulerNotifications {

    private val window = configuration.window
    private val refreshMutex = Mutex()

    override fun configure() {
        scope.launch {
            // queryEvents re-emits on any task/outcome change, so this both seeds and keeps in sync.
            val now = timeProvider.nowInstant()
            scheduler.queryEvents(InstantRange(start = now, endExclusive = now.plus(window)))
                .collect { refresh() }
        }
    }

    override fun scheduleNotificationsRefresh() {
        scope.launch { refresh() }
    }

    private suspend fun refresh() = refreshMutex.withLock {
        val now = timeProvider.nowInstant()
        val range = InstantRange(start = now, endExclusive = now.plus(window))
        val eventsByTask = scheduler.queryTasks(range = range, predicate = { it.scheduleNotifications })
            .map { task ->
                scheduler.queryEvents(task = task, range = range).first()
                    .filter { !it.isCompleted }
                    .map { it.toNotifiable() }
            }
            .filter { it.isNotEmpty() }
        notificationScheduler.schedule(planner.plan(eventsByTask = eventsByTask))
    }

    private fun Event.toNotifiable(): NotifiableEvent = NotifiableEvent(
        taskId = task.id,
        title = task.title,
        body = task.instructions,
        thread = task.notificationThread,
        notificationTime = task.notificationTime,
        occurrenceStart = occurrence.start,
        isAllDay = occurrence.schedule.duration is ScheduleDuration.AllDay,
    )
}
