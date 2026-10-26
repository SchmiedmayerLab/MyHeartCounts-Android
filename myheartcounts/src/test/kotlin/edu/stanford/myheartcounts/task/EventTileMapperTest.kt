//
// This source file is part of the My Heart Counts Android open-source project
//
// SPDX-FileCopyrightText: 2026 Stanford University and the project authors (see CONTRIBUTORS.md)
//
// SPDX-License-Identifier: MIT

package edu.stanford.myheartcounts.task

import com.google.common.truth.Truth.assertThat
import edu.stanford.myheartcounts.MHCPlurals
import kotlinx.coroutines.test.runTest
import org.grovealliance.core.time.FakeTimeProvider
import org.grovealliance.scheduler.Event
import org.grovealliance.scheduler.FakeScheduleCalculator
import org.grovealliance.scheduler.Schedule
import org.grovealliance.scheduler.fixtures.EventFixtures
import org.grovealliance.scheduler.fixtures.OccurrenceFixtures
import org.grovealliance.scheduler.fixtures.TaskFixtures
import org.grovealliance.study.StudyTaskCategories
import org.grovealliance.ui.StringResource
import org.junit.Test
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.time.format.FormatStyle

class EventTileMapperTest {

    private val zone: ZoneId = ZoneId.of("UTC")
    private val now: Instant = Instant.parse("2026-08-01T10:00:00Z")
    private val nineAm: Instant = Instant.parse("2026-08-01T09:00:00Z")
    private val timeProvider = FakeTimeProvider().apply {
        setZone(zone)
        setNow(now)
    }
    private val mapper = EventTileMapperImpl(
        scheduleCalculator = FakeScheduleCalculator().apply {
            setZone(zone)
            setNow(now)
        },
        timeProvider = timeProvider,
    )

    @Test
    fun `shows only the time of an occurrence today`() {
        // when
        val timing = timingOf(start = nineAm)

        // then
        assertThat(timing).isEqualTo(StringResource(time(nineAm)))
    }

    @Test
    fun `shows how many days ahead a later occurrence is`() {
        // given
        val start = nineAm.plusSeconds(SECONDS_PER_DAY * 3)

        // when
        val timing = timingOf(start = start)

        // then
        assertThat(timing).isEqualTo(StringResource.quantity(MHCPlurals.task_timing_days_ahead, 3, 3, time(start)))
    }

    @Test
    fun `shows how many days ago an earlier occurrence was`() {
        // given
        val start = nineAm.minusSeconds(SECONDS_PER_DAY)

        // when
        val timing = timingOf(start = start)

        // then
        assertThat(timing).isEqualTo(StringResource.quantity(MHCPlurals.task_timing_days_ago, 1, 1, time(start)))
    }

    @Test
    fun `completes an open event when it is performed, however far away it is`() = runTest {
        // given
        val event = eventAt(start = nineAm.plusSeconds(SECONDS_PER_DAY * DAYS_BEYOND_LATE_WINDOW))
        val performed = mutableListOf<Boolean>()
        val tile = mapper.tile(
            event = event,
            onPerform = { _, completesEvent -> performed += completesEvent },
        )

        // when
        tile.action?.action?.invoke()

        // then
        assertThat(performed).containsExactly(true)
    }

    @Test
    fun `puts the events of each day in their own section`() {
        // given
        val events = listOf(
            eventAt(start = nineAm),
            eventAt(start = nineAm.plusSeconds(SECONDS_PER_DAY)),
        )

        // when
        val sections = mapper.daySections(
            events = events,
            onPerform = { _, _ -> },
        )

        // then
        assertThat(sections).hasSize(2)
    }

    private fun timingOf(start: Instant): StringResource? = mapper.tile(
        event = eventAt(start = start),
        onPerform = { _, _ -> },
    ).header.timing

    private fun time(instant: Instant): String =
        DateTimeFormatter.ofLocalizedTime(FormatStyle.SHORT).withZone(zone).format(instant)

    private fun eventAt(start: Instant): Event {
        val schedule = Schedule.once(at = start)
        return EventFixtures.create(
            task = TaskFixtures.create(
                category = StudyTaskCategories.questionnaire,
                schedule = schedule,
            ),
            occurrence = OccurrenceFixtures.create(start = start, schedule = schedule),
        )
    }

    private companion object {
        const val SECONDS_PER_DAY = 86_400L
        const val DAYS_BEYOND_LATE_WINDOW = 20L
    }
}
