//
// This source file is part of the My Heart Counts Android open-source project
//
// SPDX-FileCopyrightText: 2026 Stanford University and the project authors (see CONTRIBUTORS.md)
//
// SPDX-License-Identifier: MIT

package edu.stanford.myheartcounts.upcoming

import com.google.common.truth.Truth.assertThat
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.grovealliance.core.time.FakeTimeProvider
import org.grovealliance.scheduler.Event
import org.grovealliance.scheduler.FakeScheduler
import org.grovealliance.scheduler.Schedule
import org.grovealliance.scheduler.fixtures.EventFixtures
import org.grovealliance.scheduler.fixtures.OccurrenceFixtures
import org.grovealliance.scheduler.fixtures.TaskFixtures
import org.junit.Test
import java.time.Instant
import java.time.ZoneId

class UpcomingTasksSourceTest {

    private val zone: ZoneId = ZoneId.of("UTC")
    private val startOfToday: Instant = Instant.parse("2026-08-01T00:00:00Z")
    private val scheduler = FakeScheduler()
    private val timeProvider = FakeTimeProvider().apply {
        setZone(zone)
        setNow(startOfToday.plusSeconds(SECONDS_PER_HOUR * 10))
    }
    private val source = UpcomingTasksSourceImpl(
        scheduler = scheduler,
        timeProvider = timeProvider,
    )

    @Test
    fun `lists the events from the start of today through the next two weeks`() = runTest {
        // given
        val earlierToday = oneOffAt(startOfToday.plusSeconds(SECONDS_PER_HOUR))
        val lastDay = oneOffAt(startOfToday.plusSeconds(SECONDS_PER_DAY * LAST_DAY))
        scheduler.setEvents(
            listOf(
                oneOffAt(startOfToday.minusSeconds(SECONDS_PER_HOUR)),
                earlierToday,
                lastDay,
                oneOffAt(startOfToday.plusSeconds(SECONDS_PER_DAY * (LAST_DAY + 1))),
            )
        )

        // when
        val events = source.upcomingEvents().first()

        // then
        assertThat(events).containsExactly(earlierToday, lastDay).inOrder()
    }

    private fun oneOffAt(start: Instant): Event {
        val schedule = Schedule.once(at = start)
        return EventFixtures.create(
            task = TaskFixtures.create(schedule = schedule),
            occurrence = OccurrenceFixtures.create(start = start, schedule = schedule),
        )
    }

    private companion object {
        const val SECONDS_PER_DAY = 86_400L
        const val SECONDS_PER_HOUR = 3_600L
        const val LAST_DAY = 13L
    }
}
