//
// This source file is part of the My Heart Counts open-source project
//
// SPDX-FileCopyrightText: 2026 Stanford University and the project authors (see CONTRIBUTORS.md)
//
// SPDX-License-Identifier: MIT

package edu.stanford.myheartcounts.home

import com.google.common.truth.Truth.assertThat
import edu.stanford.spezi.core.time.FakeTimeProvider
import edu.stanford.spezi.scheduler.Event
import edu.stanford.spezi.scheduler.FakeScheduler
import edu.stanford.spezi.scheduler.Recurrence
import edu.stanford.spezi.scheduler.RecurrenceFrequency
import edu.stanford.spezi.scheduler.Schedule
import edu.stanford.spezi.scheduler.fixtures.EventFixtures
import edu.stanford.spezi.scheduler.fixtures.OccurrenceFixtures
import edu.stanford.spezi.scheduler.fixtures.OutcomeFixtures
import edu.stanford.spezi.scheduler.fixtures.TaskFixtures
import edu.stanford.spezi.study.FakeStudyManager
import edu.stanford.spezi.study.fixtures.StudyEnrollmentFixtures
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.advanceTimeBy
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import org.junit.Test
import java.time.Instant
import java.time.ZoneId

class HomeTasksSourceTest {

    private val zone: ZoneId = ZoneId.of("UTC")
    private val startOfToday: Instant = Instant.parse("2026-08-01T00:00:00Z")
    private val now: Instant = startOfToday.plusSeconds(SECONDS_PER_HOUR * 10)
    private val lastWeek: Instant = startOfToday.minusSeconds(SECONDS_PER_DAY * DAYS_IN_WEEK)

    private val scheduler = FakeScheduler()
    private val studyManager = FakeStudyManager().apply {
        setEnrollments(listOf(StudyEnrollmentFixtures.create(enrollmentDate = lastWeek)))
    }
    private val timeProvider = FakeTimeProvider().apply {
        setZone(zone)
        setNow(now)
    }
    private val source = HomeTasksSourceImpl(
        studyManager = studyManager,
        scheduler = scheduler,
        timeProvider = timeProvider,
    )

    @Test
    fun `keeps an event occurring today`() = runTest {
        // given
        scheduler.setEvents(listOf(oneOffAt(startOfToday.plusSeconds(SECONDS_PER_HOUR))))

        // when
        val tasks = source.tasks().first()

        // then
        assertThat(tasks.events).hasSize(1)
    }

    @Test
    fun `keeps an outstanding one-off event from before today`() = runTest {
        // given
        scheduler.setEvents(listOf(oneOffAt(lastWeek)))

        // when
        val tasks = source.tasks().first()

        // then
        assertThat(tasks.events).hasSize(1)
    }

    @Test
    fun `keeps a past one-off event completed today`() = runTest {
        // given
        val event = oneOffAt(lastWeek).completedAt(startOfToday.plusSeconds(SECONDS_PER_HOUR))
        scheduler.setEvents(listOf(event))

        // when
        val tasks = source.tasks().first()

        // then
        assertThat(tasks.events).hasSize(1)
    }

    @Test
    fun `drops a past one-off event completed before today`() = runTest {
        // given
        scheduler.setEvents(listOf(oneOffAt(lastWeek).completedAt(lastWeek)))

        // when
        val tasks = source.tasks().first()

        // then
        assertThat(tasks.events).isEmpty()
    }

    @Test
    fun `drops a past occurrence of a recurring event`() = runTest {
        // given
        val schedule = Schedule(
            start = lastWeek,
            recurrence = Recurrence(frequency = RecurrenceFrequency.DAILY),
        )
        scheduler.setEvents(
            listOf(
                EventFixtures.create(
                    task = TaskFixtures.create(schedule = schedule),
                    occurrence = OccurrenceFixtures.create(start = lastWeek, schedule = schedule),
                )
            )
        )

        // when
        val tasks = source.tasks().first()

        // then
        assertThat(tasks.events).isEmpty()
    }

    @Test
    fun `counts missed events from the preceding two weeks`() = runTest {
        // given
        scheduler.setMissedEvents(
            listOf(
                oneOffAt(lastWeek),
                oneOffAt(startOfToday.minusSeconds(SECONDS_PER_DAY)),
                oneOffAt(startOfToday.minusSeconds(SECONDS_PER_DAY * DAYS_BEYOND_MISSED_WINDOW)),
            )
        )

        // when
        val tasks = source.tasks().first()

        // then
        assertThat(tasks.missedEventCount).isEqualTo(2)
    }

    @Test
    fun `moves on to the next day once today ends`() = runTest {
        // given
        val tomorrow = startOfToday.plusSeconds(SECONDS_PER_DAY)
        scheduler.setEvents(listOf(recurringAt(tomorrow.plusSeconds(SECONDS_PER_HOUR * 9))))
        val seen = mutableListOf<Int>()
        val collection = backgroundScope.launch {
            source.tasks().collect { seen.add(it.events.size) }
        }
        runCurrent()

        // when
        timeProvider.setNow(tomorrow.plusSeconds(SECONDS_PER_HOUR))
        advanceTimeBy(SECONDS_PER_DAY * MILLIS_PER_SECOND)
        runCurrent()
        collection.cancel()

        // then
        assertThat(seen.first()).isEqualTo(0)
        assertThat(seen.last()).isEqualTo(1)
    }

    /**
     * A recurring event, so that the indefinite-past rule cannot keep it visible across days.
     */
    private fun recurringAt(start: Instant): Event {
        val schedule = Schedule(
            start = start,
            recurrence = Recurrence(frequency = RecurrenceFrequency.DAILY),
        )
        return EventFixtures.create(
            task = TaskFixtures.create(schedule = schedule),
            occurrence = OccurrenceFixtures.create(start = start, schedule = schedule),
        )
    }

    private fun oneOffAt(start: Instant): Event {
        val schedule = Schedule.once(at = start)
        return EventFixtures.create(
            task = TaskFixtures.create(schedule = schedule),
            occurrence = OccurrenceFixtures.create(start = start, schedule = schedule),
        )
    }

    private fun Event.completedAt(instant: Instant) = copy(
        outcome = OutcomeFixtures.create(
            occurrenceStartDate = occurrence.start,
            completionDate = instant,
        )
    )

    private companion object {
        const val SECONDS_PER_DAY = 86_400L
        const val SECONDS_PER_HOUR = 3_600L
        const val DAYS_IN_WEEK = 7L
        const val DAYS_BEYOND_MISSED_WINDOW = 20L
        const val MILLIS_PER_SECOND = 1_000L
    }
}
