//
// This source file is part of the My Heart Counts Android open-source project
//
// SPDX-FileCopyrightText: 2026 Stanford University and the project authors (see CONTRIBUTORS.md)
//
// SPDX-License-Identifier: MIT

package edu.stanford.spezi.study.internal

import com.google.common.truth.Truth.assertThat
import edu.stanford.spezi.core.time.FakeTimeProvider
import edu.stanford.spezi.scheduler.FakeScheduleCalculator
import edu.stanford.spezi.scheduler.RecurrenceFrequency
import edu.stanford.spezi.studydefinition.Weekday
import edu.stanford.spezi.studydefinition.fixtures.DateComponentsFixtures
import edu.stanford.spezi.studydefinition.fixtures.OneTimeScheduleFixtures
import edu.stanford.spezi.studydefinition.fixtures.RepetitionPatternFixtures
import edu.stanford.spezi.studydefinition.fixtures.ScheduleDefinitionFixtures
import edu.stanford.spezi.studydefinition.fixtures.TimeFixtures
import org.junit.Test
import java.time.DayOfWeek
import java.time.ZoneId
import java.time.ZonedDateTime

class ScheduleCompilerTest {

    // The calculator aligns time-of-day in the system default zone, so the compiler shares it.
    private val zone: ZoneId = ZoneId.systemDefault()
    private val compiler = ScheduleCompiler(FakeScheduleCalculator(), FakeTimeProvider())

    // A Wednesday.
    private val enrollment = ZonedDateTime.of(2026, 7, 1, 8, 30, 0, 0, zone).toInstant()

    @Test
    fun `compiles a daily repeated schedule`() {
        // given
        val definition = ScheduleDefinitionFixtures.createRepeated(
            pattern = RepetitionPatternFixtures.createDaily(interval = 1, hour = 9),
        )

        // when
        val schedule = compiler.schedule(definition, enrollment)

        // then
        assertThat(schedule).isNotNull()
        assertThat(schedule!!.recurrence?.frequency).isEqualTo(RecurrenceFrequency.DAILY)
        assertThat(schedule.recurrence?.interval).isEqualTo(1)
        assertThat(schedule.start.atZone(zone).hour).isEqualTo(9)
    }

    @Test
    fun `compiles a weekly schedule with an explicit weekday`() {
        // given
        val definition = ScheduleDefinitionFixtures.createRepeated(
            pattern = RepetitionPatternFixtures.createWeekly(interval = 1, weekday = Weekday.MONDAY, hour = 10),
        )

        // when
        val schedule = compiler.schedule(definition, enrollment)

        // then
        assertThat(schedule!!.recurrence?.frequency).isEqualTo(RecurrenceFrequency.WEEKLY)
        assertThat(schedule.recurrence?.weekday).isEqualTo(DayOfWeek.MONDAY)
    }

    @Test
    fun `weekly schedule without a weekday falls back to the enrollment weekday`() {
        // given
        val definition = ScheduleDefinitionFixtures.createRepeated(
            pattern = RepetitionPatternFixtures.createWeekly(interval = 1, weekday = null, hour = 10),
        )

        // when
        val schedule = compiler.schedule(definition, enrollment)

        // then
        assertThat(schedule!!.recurrence?.weekday).isEqualTo(DayOfWeek.WEDNESDAY)
    }

    @Test
    fun `applies the repeated offset to the start`() {
        // given
        val definition = ScheduleDefinitionFixtures.createRepeated(
            pattern = RepetitionPatternFixtures.createDaily(interval = 1, hour = 9),
            offset = DateComponentsFixtures.create(day = 2),
        )

        // when
        val schedule = compiler.schedule(definition, enrollment)

        // then
        assertThat(schedule!!.start.atZone(zone).dayOfMonth).isEqualTo(3)
    }

    @Test
    fun `compiles a once-at-date schedule`() {
        // given
        val definition = ScheduleDefinitionFixtures.createOnce(
            OneTimeScheduleFixtures.createDate(DateComponentsFixtures.create(year = 2026, month = 8, day = 15, hour = 9)),
        )

        // when
        val schedule = compiler.schedule(definition, enrollment)

        // then
        assertThat(schedule!!.recurrence).isNull()
        assertThat(schedule.start.atZone(zone).dayOfMonth).isEqualTo(15)
    }

    @Test
    fun `does not compile an event schedule directly`() {
        // given
        val definition = ScheduleDefinitionFixtures.createOnce(OneTimeScheduleFixtures.createEvent(offsetInDays = 0))

        // when / then
        assertThat(compiler.schedule(definition, enrollment)).isNull()
    }

    @Test
    fun `materializes an event occurrence with offset and time`() {
        // given
        val event = OneTimeScheduleFixtures.createEvent(offsetInDays = 7, time = TimeFixtures.create(hour = 9, minute = 0))

        // when
        val schedule = compiler.eventOccurrenceSchedule(event, enrollment)

        // then
        val start = schedule.start.atZone(zone)
        assertThat(start.dayOfMonth).isEqualTo(8)
        assertThat(start.hour).isEqualTo(9)
        assertThat(schedule.recurrence).isNull()
    }
}
