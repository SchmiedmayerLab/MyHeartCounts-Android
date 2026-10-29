//
// This source file is part of the My Heart Counts Android open-source project
//
// SPDX-FileCopyrightText: 2026 Stanford University and the project authors (see CONTRIBUTORS.md)
//
// SPDX-License-Identifier: MIT

package edu.stanford.myheartcounts.task

import com.google.common.truth.Truth.assertThat
import edu.stanford.myheartcounts.MHCPlurals
import edu.stanford.myheartcounts.MHCStrings
import kotlinx.coroutines.test.runTest
import org.grovealliance.core.time.FakeTimeProvider
import org.grovealliance.scheduler.Event
import org.grovealliance.scheduler.FakeScheduleCalculator
import org.grovealliance.scheduler.Schedule
import org.grovealliance.scheduler.ScheduleDuration
import org.grovealliance.scheduler.fixtures.EventFixtures
import org.grovealliance.scheduler.fixtures.OccurrenceFixtures
import org.grovealliance.scheduler.fixtures.TaskFixtures
import org.grovealliance.study.StudyTaskCategories
import org.grovealliance.ui.StringResource
import org.junit.Test
import java.time.Duration
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
    fun `shows no time for an occurrence lasting all of today`() {
        // given
        val midnight = Instant.parse("2026-08-01T00:00:00Z")

        // when
        val timing = timingOf(start = midnight)

        // then
        assertThat(timing).isNull()
    }

    @Test
    fun `shows the start and end time of an occurrence with a fixed duration`() {
        // given
        val end = nineAm.plusSeconds(SECONDS_PER_HOUR)

        // when
        val timing = timingOf(
            start = nineAm,
            duration = ScheduleDuration.Fixed(duration = Duration.ofHours(1)),
            end = end,
        )

        // then
        assertThat(timing).isEqualTo(StringResource(MHCStrings.task_timing_time_range, time(nineAm), time(end)))
    }

    @Test
    fun `shows the hours to an occurrence on another day less than a day away`() {
        // given
        val start = Instant.parse("2026-07-31T22:00:00Z")

        // when
        val timing = timingOf(start = start)

        // then
        assertThat(timing).isEqualTo(
            withDistance(distance = StringResource.quantity(MHCPlurals.task_timing_hours_ago, 12, 12), start = start),
        )
    }

    @Test
    fun `shows the calendar days to an occurrence within a week`() {
        // given
        val tomorrow = Instant.parse("2026-08-02T12:00:00Z")
        val threeDaysAgo = Instant.parse("2026-07-29T09:00:00Z")

        // when
        val tomorrowTiming = timingOf(start = tomorrow)
        val threeDaysAgoTiming = timingOf(start = threeDaysAgo)

        // then
        assertThat(tomorrowTiming).isEqualTo(withDistance(distance = StringResource(MHCStrings.task_timing_tomorrow), start = tomorrow))
        assertThat(threeDaysAgoTiming).isEqualTo(
            withDistance(distance = StringResource.quantity(MHCPlurals.task_timing_days_ago, 3, 3), start = threeDaysAgo),
        )
    }

    @Test
    fun `shows the calendar weeks to an occurrence a week or more away, with its time even at midnight`() {
        // given
        val lastWeek = Instant.parse("2026-07-25T00:00:00Z")
        val twoWeeksAgo = Instant.parse("2026-07-18T09:00:00Z")

        // when
        val lastWeekTiming = timingOf(start = lastWeek)
        val twoWeeksAgoTiming = timingOf(start = twoWeeksAgo)

        // then
        assertThat(lastWeekTiming).isEqualTo(withDistance(distance = StringResource(MHCStrings.task_timing_last_week), start = lastWeek))
        assertThat(twoWeeksAgoTiming).isEqualTo(
            withDistance(distance = StringResource.quantity(MHCPlurals.task_timing_weeks_ago, 2, 2), start = twoWeeksAgo),
        )
    }

    @Test
    fun `shows the calendar months to an occurrence four weeks or more away`() {
        // given
        val start = Instant.parse("2026-06-15T09:00:00Z")

        // when
        val timing = timingOf(start = start)

        // then
        assertThat(timing).isEqualTo(
            withDistance(distance = StringResource.quantity(MHCPlurals.task_timing_months_ago, 2, 2), start = start),
        )
    }

    @Test
    fun `shows the year of an occurrence months away in another year`() {
        // given
        val start = Instant.parse("2025-12-01T09:00:00Z")

        // when
        val timing = timingOf(start = start)

        // then
        assertThat(timing).isEqualTo(withDistance(distance = StringResource("2025"), start = start))
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

    private fun timingOf(
        start: Instant,
        duration: ScheduleDuration = ScheduleDuration.TillEndOfDay,
        end: Instant = start.plusSeconds(SECONDS_PER_HOUR),
    ): StringResource? = mapper.tile(
        event = eventAt(
            start = start,
            duration = duration,
            end = end,
        ),
        onPerform = { _, _ -> },
    ).header.timing

    private fun withDistance(distance: StringResource, start: Instant): StringResource =
        StringResource(MHCStrings.task_timing_with_offset, distance, StringResource(time(start)))

    private fun time(instant: Instant): String =
        DateTimeFormatter.ofLocalizedTime(FormatStyle.SHORT).withZone(zone).format(instant)

    private fun eventAt(
        start: Instant,
        duration: ScheduleDuration = ScheduleDuration.TillEndOfDay,
        end: Instant = start.plusSeconds(SECONDS_PER_HOUR),
    ): Event {
        val schedule = Schedule.once(
            at = start,
            duration = duration,
        )
        return EventFixtures.create(
            task = TaskFixtures.create(
                category = StudyTaskCategories.questionnaire,
                schedule = schedule,
            ),
            occurrence = OccurrenceFixtures.create(
                start = start,
                end = end,
                schedule = schedule,
            ),
        )
    }

    private companion object {
        const val SECONDS_PER_DAY = 86_400L
        const val SECONDS_PER_HOUR = 3_600L
        const val DAYS_BEYOND_LATE_WINDOW = 20L
    }
}
