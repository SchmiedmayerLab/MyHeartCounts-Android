//
// This source file is part of the My Heart Counts Android open-source project
//
// SPDX-FileCopyrightText: 2026 Stanford University and the project authors (see CONTRIBUTORS.md)
//
// SPDX-License-Identifier: MIT

package edu.stanford.myheartcounts.task

import com.google.common.truth.Truth.assertThat
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.grovealliance.core.time.FakeTimeProvider
import org.grovealliance.scheduler.Event
import org.grovealliance.scheduler.FakeScheduleCalculator
import org.grovealliance.scheduler.FakeScheduler
import org.grovealliance.scheduler.InstantRange
import org.grovealliance.scheduler.Recurrence
import org.grovealliance.scheduler.RecurrenceFrequency
import org.grovealliance.scheduler.Schedule
import org.grovealliance.scheduler.Task
import org.grovealliance.scheduler.fixtures.EventFixtures
import org.grovealliance.scheduler.fixtures.OccurrenceFixtures
import org.grovealliance.scheduler.fixtures.OutcomeFixtures
import org.grovealliance.scheduler.fixtures.TaskFixtures
import org.junit.Test
import java.time.Instant
import java.time.ZoneId

class MissedEventsSourceTest {

    private val zone: ZoneId = ZoneId.of("UTC")
    private val startOfToday: Instant = Instant.parse("2026-08-01T00:00:00Z")
    private val today = InstantRange(
        start = startOfToday,
        endExclusive = startOfToday.plusSeconds(SECONDS_PER_DAY),
    )

    private val scheduler = FakeScheduler()
    private val source = MissedEventsSourceImpl(
        scheduler = scheduler,
        scheduleCalculator = FakeScheduleCalculator().apply { setZone(zone) },
        timeProvider = FakeTimeProvider().apply { setZone(zone) },
    )

    @Test
    fun `keeps only the latest outstanding occurrence of a recurring task`() = runTest {
        // given
        val task = dailyTask(firstDay = -3)
        scheduler.setEvents((-3L..0L).map { day -> eventOn(task, day = day) })

        // when
        val missed = source.missedEvents(today = today).first()

        // then
        assertThat(missed).containsExactly(eventOn(task, day = -1))
    }

    @Test
    fun `drops an occurrence whose next occurrence is already completed`() = runTest {
        // given
        val task = dailyTask(firstDay = -1)
        scheduler.setEvents(listOf(eventOn(task, day = -1), eventOn(task, day = 0).completed()))

        // when
        val missed = source.missedEvents(today = today).first()

        // then
        assertThat(missed).isEmpty()
    }

    @Test
    fun `drops a completed occurrence`() = runTest {
        // given
        val task = dailyTask(firstDay = -1)
        scheduler.setEvents(listOf(eventOn(task, day = -1).completed(), eventOn(task, day = 0)))

        // when
        val missed = source.missedEvents(today = today).first()

        // then
        assertThat(missed).isEmpty()
    }

    @Test
    fun `never counts a task that does not recur`() = runTest {
        // given
        val start = startOfToday.minusSeconds(SECONDS_PER_DAY).plusSeconds(NINE_AM)
        val schedule = Schedule.once(at = start)
        val task = TaskFixtures.create(schedule = schedule)
        scheduler.setEvents(listOf(eventAt(task, start = start)))

        // when
        val missed = source.missedEvents(today = today).first()

        // then
        assertThat(missed).isEmpty()
    }

    @Test
    fun `ignores occurrences from before the two weeks preceding today`() = runTest {
        // given
        val task = weeklyTask(firstDay = -WEEKS_BEFORE_WINDOW_DAYS)
        scheduler.setEvents(listOf(eventOn(task, day = -WEEKS_BEFORE_WINDOW_DAYS)))

        // when
        val missed = source.missedEvents(today = today).first()

        // then
        assertThat(missed).isEmpty()
    }

    private fun dailyTask(firstDay: Long) = recurringTask(firstDay = firstDay, frequency = RecurrenceFrequency.DAILY)

    private fun weeklyTask(firstDay: Long) = recurringTask(firstDay = firstDay, frequency = RecurrenceFrequency.WEEKLY)

    private fun recurringTask(firstDay: Long, frequency: RecurrenceFrequency): Task = TaskFixtures.create(
        schedule = Schedule(
            start = dayStart(day = firstDay).plusSeconds(NINE_AM),
            recurrence = Recurrence(frequency = frequency),
        ),
    )

    private fun eventOn(task: Task, day: Long): Event = eventAt(task = task, start = dayStart(day = day).plusSeconds(NINE_AM))

    private fun eventAt(task: Task, start: Instant): Event = EventFixtures.create(
        task = task,
        occurrence = OccurrenceFixtures.create(start = start, schedule = task.schedule),
    )

    private fun dayStart(day: Long): Instant = startOfToday.plusSeconds(SECONDS_PER_DAY * day)

    private fun Event.completed() = copy(
        outcome = OutcomeFixtures.create(
            occurrenceStartDate = occurrence.start,
            completionDate = occurrence.start,
        )
    )

    private companion object {
        const val SECONDS_PER_DAY = 86_400L
        const val NINE_AM = 9 * 3_600L
        const val WEEKS_BEFORE_WINDOW_DAYS = 21L
    }
}
