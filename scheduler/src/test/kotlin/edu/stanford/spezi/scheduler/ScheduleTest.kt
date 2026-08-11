//
// This source file is part of the My Heart Counts Android open-source project
//
// SPDX-FileCopyrightText: 2026 Stanford University and the project authors (see CONTRIBUTORS.md)
//
// SPDX-License-Identifier: MIT

package edu.stanford.spezi.scheduler

import com.google.common.truth.Truth.assertThat
import org.junit.Test
import java.time.DayOfWeek
import java.time.ZoneId
import java.time.ZonedDateTime

class ScheduleTest {
    private val zone: ZoneId = ZoneId.systemDefault()
    private val calculator = FakeScheduleCalculator()

    @Test
    fun `once produces a single occurrence inside the range`() {
        // given
        val schedule = Schedule.once(at = at(2026, 6, 14, 9))

        // when
        val occurrences = calculator.occurrences(schedule = schedule, range = rangeOfDays(2026, 6, 14, 7))

        // then
        assertThat(occurrences.map { it.start }).containsExactly(at(2026, 6, 14, 9))
    }

    @Test
    fun `once produces nothing outside the range`() {
        // given
        val schedule = Schedule.once(at = at(2026, 6, 14, 9))

        // when
        val occurrences = calculator.occurrences(schedule = schedule, range = rangeOfDays(2026, 6, 15, 7))

        // then
        assertThat(occurrences).isEmpty()
    }

    @Test
    fun `daily produces one occurrence per day at the configured time`() {
        // given
        val schedule = calculator.daily(hour = 9, minute = 0, startingAt = at(2026, 6, 14))

        // when
        val occurrences = calculator.occurrences(schedule = schedule, range = rangeOfDays(2026, 6, 14, 3))

        // then
        assertThat(occurrences.map { it.start }).containsExactly(
            at(2026, 6, 14, 9),
            at(2026, 6, 15, 9),
            at(2026, 6, 16, 9),
        ).inOrder()
    }

    @Test
    fun `daily honors the interval`() {
        // given
        val schedule = calculator.daily(interval = 2, hour = 8, minute = 0, startingAt = at(2026, 6, 14))

        // when
        val occurrences = calculator.occurrences(schedule = schedule, range = rangeOfDays(2026, 6, 14, 5))

        // then
        assertThat(occurrences.map { it.start }).containsExactly(
            at(2026, 6, 14, 8),
            at(2026, 6, 16, 8),
            at(2026, 6, 18, 8),
        ).inOrder()
    }

    @Test
    fun `afterOccurrences limits the total count regardless of range`() {
        // given
        val schedule = calculator.daily(
            hour = 9,
            minute = 0,
            startingAt = at(2026, 6, 14),
            end = RecurrenceEnd.AfterOccurrences(3),
        )

        // when
        val occurrences = calculator.occurrences(schedule = schedule, range = rangeOfDays(2026, 6, 14, 365))

        // then
        assertThat(occurrences).hasSize(3)
    }

    @Test
    fun `weekly aligns to the requested weekday`() {
        // given
        // June 14 2026 is a Sunday; the first Wednesday at or after is June 17.
        val schedule = calculator.weekly(
            weekday = DayOfWeek.WEDNESDAY,
            hour = 7,
            minute = 30,
            startingAt = at(2026, 6, 14),
        )

        // when
        val occurrences = calculator.occurrences(schedule = schedule, range = rangeOfDays(2026, 6, 14, 14))

        // then
        assertThat(occurrences.map { it.start }).containsExactly(
            at(2026, 6, 17, 7, 30),
            at(2026, 6, 24, 7, 30),
        ).inOrder()
    }

    @Test
    fun `occurrence lookup matches an exact start`() {
        // given
        val schedule = calculator.daily(hour = 9, minute = 0, startingAt = at(2026, 6, 14))

        // when / then
        assertThat(calculator.occurrence(schedule = schedule, startDate = at(2026, 6, 16, 9))).isNotNull()
        assertThat(calculator.occurrence(schedule = schedule, startDate = at(2026, 6, 16, 10))).isNull()
    }

    private fun at(year: Int, month: Int, day: Int, hour: Int = 0, minute: Int = 0) =
        ZonedDateTime.of(year, month, day, hour, minute, 0, 0, zone).toInstant()

    private fun rangeOfDays(year: Int, month: Int, day: Int, days: Long): InstantRange {
        val start = ZonedDateTime.of(year, month, day, 0, 0, 0, 0, zone)
        return InstantRange(
            start = start.toInstant(),
            endExclusive = start.plusDays(days).toInstant(),
        )
    }
}
