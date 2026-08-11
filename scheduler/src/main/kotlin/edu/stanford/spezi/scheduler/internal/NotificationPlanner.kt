//
// This source file is part of the My Heart Counts Android open-source project
//
// SPDX-FileCopyrightText: 2026 Stanford University and the project authors (see CONTRIBUTORS.md)
//
// SPDX-License-Identifier: MIT

package edu.stanford.spezi.scheduler.internal

import edu.stanford.spezi.core.time.TimeProvider
import edu.stanford.spezi.scheduler.NotificationThread
import edu.stanford.spezi.scheduler.NotificationTime
import edu.stanford.spezi.scheduler.SchedulerNotificationsConfiguration
import kotlinx.serialization.Serializable
import java.time.Instant

/**
 * Turns upcoming events into concrete notification requests.
 *
 * Events are taken round-robin across tasks (so no single frequent task starves the others) up to the
 * configured limit, and any request whose delivery time is not in the future is dropped.
 */
internal class NotificationPlanner(
    private val timeProvider: TimeProvider,
    private val configuration: SchedulerNotificationsConfiguration,
) {
    /**
     * Plans notifications from [eventsByTask] — each inner list is one task's upcoming, uncompleted
     * events in ascending start order — relative to the current time.
     */
    fun plan(eventsByTask: List<List<NotifiableEvent>>): List<PlannedNotification> {
        val now = timeProvider.nowInstant()
        val queues = eventsByTask.map { ArrayDeque(it) }.filterTo(mutableListOf()) { it.isNotEmpty() }
        val planned = mutableListOf<PlannedNotification>()
        while (planned.size < configuration.limit && queues.isNotEmpty()) {
            val iterator = queues.iterator()
            while (iterator.hasNext() && planned.size < configuration.limit) {
                val queue = iterator.next()
                planForEvent(now, queue.removeFirst())?.let { planned.add(it) }
                if (queue.isEmpty()) iterator.remove()
            }
        }
        return planned
    }

    private fun planForEvent(now: Instant, event: NotifiableEvent): PlannedNotification? {
        val fireTime = fireTime(event)
        if (!fireTime.isAfter(now)) return null
        return PlannedNotification(
            id = notificationId(event.taskId, event.occurrenceStart),
            taskId = event.taskId,
            fireTimeMillis = fireTime.toEpochMilli(),
            title = event.title,
            body = event.body,
            threadId = threadId(event.taskId, event.thread),
            timeSensitive = !event.isAllDay,
        )
    }

    private fun fireTime(event: NotifiableEvent): Instant {
        val zone = timeProvider.currentZone()
        return when {
            event.notificationTime != null -> event.notificationTime.onDayOf(event.occurrenceStart, zone)
            event.isAllDay -> configuration.allDayNotificationTime.onDayOf(event.occurrenceStart, zone)
            else -> event.occurrenceStart
        }
    }

    private fun threadId(taskId: String, thread: NotificationThread): String? = when (thread) {
        NotificationThread.None -> null
        NotificationThread.Global -> BASE_THREAD
        NotificationThread.PerTask -> "$BASE_THREAD.$taskId"
        is NotificationThread.Custom -> thread.id
    }

    companion object {
        const val BASE_THREAD = "edu.stanford.spezi.scheduler.notifications"
        private const val ID_PREFIX = "edu.stanford.spezi.scheduler.notification"

        fun notificationId(taskId: String, occurrenceStart: Instant): String =
            "$ID_PREFIX.$taskId.${occurrenceStart.toEpochMilli()}"
    }
}

/**
 * One upcoming, uncompleted event that is eligible for a notification.
 */
internal data class NotifiableEvent(
    val taskId: String,
    val title: String,
    val body: String,
    val thread: NotificationThread,
    val notificationTime: NotificationTime?,
    val occurrenceStart: Instant,
    val isAllDay: Boolean,
)

/**
 * A single notification request to be delivered at [fireTimeMillis].
 *
 * Persisted (as JSON) so pending requests can be re-armed after a reboot without re-querying the store.
 */
@Serializable
internal data class PlannedNotification(
    val id: String,
    val taskId: String,
    val fireTimeMillis: Long,
    val title: String,
    val body: String,
    val threadId: String?,
    val timeSensitive: Boolean,
)
