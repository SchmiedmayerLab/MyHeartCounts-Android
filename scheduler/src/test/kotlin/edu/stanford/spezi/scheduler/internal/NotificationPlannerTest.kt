//
// This source file is part of the My Heart Counts open-source project
//
// SPDX-FileCopyrightText: 2026 Stanford University and the project authors (see CONTRIBUTORS.md)
//
// SPDX-License-Identifier: MIT

package edu.stanford.spezi.scheduler.internal

import com.google.common.truth.Truth.assertThat
import edu.stanford.spezi.core.time.FakeTimeProvider
import edu.stanford.spezi.scheduler.NotificationThread
import edu.stanford.spezi.scheduler.NotificationTime
import edu.stanford.spezi.scheduler.SchedulerNotificationsConfiguration
import org.junit.Test
import java.time.Duration
import java.time.Instant
import java.time.ZoneId
import java.time.ZonedDateTime

class NotificationPlannerTest {

    private val zone: ZoneId = ZoneId.of("America/Los_Angeles")
    private val now = ZonedDateTime.of(2026, 6, 14, 8, 0, 0, 0, zone).toInstant()

    @Test
    fun `plans a timed event at its occurrence start`() {
        // given
        val event = timedEvent(taskId = "t", start = at(2026, 6, 15, 9))

        // when
        val planned = planner().plan(eventsByTask = listOf(listOf(event)))

        // then
        assertThat(planned).hasSize(1)
        assertThat(planned.single().fireTimeMillis).isEqualTo(at(2026, 6, 15, 9).toEpochMilli())
        assertThat(planned.single().timeSensitive).isTrue()
    }

    @Test
    fun `all-day event fires at the configured all-day time`() {
        // given
        val event = timedEvent(taskId = "t", start = at(2026, 6, 15, 0), isAllDay = true)

        // when
        val planned = planner().plan(eventsByTask = listOf(listOf(event)))

        // then — 9:00 in the zone, not midnight; not time-sensitive
        assertThat(planned.single().fireTimeMillis).isEqualTo(at(2026, 6, 15, 9).toEpochMilli())
        assertThat(planned.single().timeSensitive).isFalse()
    }

    @Test
    fun `a per-task notification time overrides the occurrence start`() {
        // given
        val event = timedEvent(taskId = "t", start = at(2026, 6, 15, 9), time = NotificationTime(hour = 7, minute = 30))

        // when
        val planned = planner().plan(eventsByTask = listOf(listOf(event)))

        // then
        assertThat(planned.single().fireTimeMillis).isEqualTo(at(2026, 6, 15, 7, 30).toEpochMilli())
    }

    @Test
    fun `drops events whose fire time is not in the future`() {
        // given — an occurrence earlier today, before now
        val past = timedEvent(taskId = "t", start = at(2026, 6, 14, 7))
        val future = timedEvent(taskId = "t", start = at(2026, 6, 15, 9))

        // when
        val planned = planner().plan(eventsByTask = listOf(listOf(past, future)))

        // then
        assertThat(planned.map { it.fireTimeMillis }).containsExactly(at(2026, 6, 15, 9).toEpochMilli())
    }

    @Test
    fun `respects the limit, taking events round-robin across tasks`() {
        // given — two tasks, three future events each; limit of 4
        val taskA = (1..3).map { timedEvent(taskId = "a", start = at(2026, 6, 14 + it, 9)) }
        val taskB = (1..3).map { timedEvent(taskId = "b", start = at(2026, 6, 14 + it, 10)) }

        // when
        val planned = planner(limit = 4).plan(eventsByTask = listOf(taskA, taskB))

        // then — 4 total, alternating a, b, a, b
        assertThat(planned).hasSize(4)
        assertThat(planned.map { it.taskId }).containsExactly("a", "b", "a", "b").inOrder()
    }

    @Test
    fun `thread ids follow the grouping mode`() {
        // when / then
        assertThat(threadIdFor(NotificationThread.Global)).isEqualTo(NotificationPlanner.BASE_THREAD)
        assertThat(threadIdFor(NotificationThread.PerTask)).isEqualTo("${NotificationPlanner.BASE_THREAD}.t")
        assertThat(threadIdFor(NotificationThread.Custom("grp"))).isEqualTo("grp")
        assertThat(threadIdFor(NotificationThread.None)).isNull()
    }

    private fun threadIdFor(thread: NotificationThread): String? {
        val event = timedEvent(taskId = "t", start = at(2026, 6, 15, 9), thread = thread)
        return planner().plan(eventsByTask = listOf(listOf(event))).single().threadId
    }

    private fun planner(limit: Int = SchedulerNotificationsConfiguration.DEFAULT_LIMIT) = NotificationPlanner(
        timeProvider = FakeTimeProvider().apply {
            setNow(instant = now)
            setZone(zone = zone)
        },
        configuration = SchedulerNotificationsConfiguration(
            limit = limit,
            window = Duration.ofDays(28),
            allDayNotificationTime = NotificationTime(hour = 9),
        ),
    )

    private fun timedEvent(
        taskId: String,
        start: Instant,
        isAllDay: Boolean = false,
        time: NotificationTime? = null,
        thread: NotificationThread = NotificationThread.None,
    ) = NotifiableEvent(
        taskId = taskId,
        title = "Title",
        body = "Body",
        thread = thread,
        notificationTime = time,
        occurrenceStart = start,
        isAllDay = isAllDay,
    )

    private fun at(year: Int, month: Int, day: Int, hour: Int, minute: Int = 0) =
        ZonedDateTime.of(year, month, day, hour, minute, 0, 0, zone).toInstant()
}
