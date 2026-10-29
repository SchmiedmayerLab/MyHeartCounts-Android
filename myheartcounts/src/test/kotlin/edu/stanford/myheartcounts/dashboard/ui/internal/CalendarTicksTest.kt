//
// This source file is part of the My Heart Counts Android open-source project
//
// SPDX-FileCopyrightText: 2026 Stanford University and the project authors (see CONTRIBUTORS.md)
//
// SPDX-License-Identifier: MIT

package edu.stanford.myheartcounts.dashboard.ui.internal

import com.google.common.truth.Truth.assertThat
import org.junit.Test
import java.time.DayOfWeek
import java.time.Duration
import java.time.Instant
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.ZoneId

class CalendarTicksTest {

    private val zone = ZoneId.of("Europe/Zurich")

    @Test
    fun `strides follow the length of the axis`() {
        // when / then
        assertThat(CalendarStride.forLength(Duration.ofDays(7))).isEqualTo(CalendarStride.Day)
        assertThat(CalendarStride.forLength(Duration.ofDays(14))).isEqualTo(CalendarStride.Day)
        assertThat(CalendarStride.forLength(Duration.ofDays(15))).isEqualTo(CalendarStride.Week)
        assertThat(CalendarStride.forLength(Duration.ofDays(31))).isEqualTo(CalendarStride.Week)
        assertThat(CalendarStride.forLength(Duration.ofDays(32))).isEqualTo(CalendarStride.Fortnight)
        assertThat(CalendarStride.forLength(Duration.ofDays(93))).isEqualTo(CalendarStride.Fortnight)
        assertThat(CalendarStride.forLength(Duration.ofDays(94))).isEqualTo(CalendarStride.Month)
        assertThat(CalendarStride.forLength(Duration.ofDays(186))).isEqualTo(CalendarStride.Month)
        assertThat(CalendarStride.forLength(Duration.ofDays(187))).isEqualTo(CalendarStride.Quarter)
        assertThat(CalendarStride.forLength(Duration.ofDays(731))).isEqualTo(CalendarStride.Quarter)
        assertThat(CalendarStride.forLength(Duration.ofDays(732))).isEqualTo(CalendarStride.Year)
        assertThat(CalendarStride.forLength(Duration.ofDays(7_305))).isEqualTo(CalendarStride.Year)
        assertThat(CalendarStride.forLength(Duration.ofDays(7_306))).isEqualTo(CalendarStride.Decade)
        assertThat(CalendarStride.forLength(Duration.ofDays(73_050))).isEqualTo(CalendarStride.Decade)
        assertThat(CalendarStride.forLength(Duration.ofDays(73_051))).isEqualTo(CalendarStride.Century)
    }

    @Test
    fun `a week is marked every day at midnight, including the start`() {
        // when
        val ticks = ticks(from = LocalDate.of(2026, 10, 19), days = 7, firstDayOfWeek = DayOfWeek.MONDAY)

        // then
        assertThat(ticks.map { it.time.toLocalDateTime() })
            .containsExactlyElementsIn((19..25).map { LocalDateTime.of(2026, 10, it, 0, 0) })
            .inOrder()
    }

    @Test
    fun `the end of the axis is not marked`() {
        // when
        val ticks = ticks(from = LocalDate.of(2026, 10, 19), days = 2, firstDayOfWeek = DayOfWeek.MONDAY)

        // then
        assertThat(ticks.map { it.time.dayOfMonth }).containsExactly(19, 20).inOrder()
    }

    @Test
    fun `an axis starting mid-day is first marked the next midnight`() {
        // given
        val start = LocalDateTime.of(2026, 10, 19, 15, 30).atZone(zone).toInstant()

        // when
        val ticks = calendarTicks(
            start = start,
            end = start.plus(Duration.ofDays(3)),
            zone = zone,
            firstDayOfWeek = DayOfWeek.MONDAY,
        )

        // then
        assertThat(ticks.map { it.time.toLocalDateTime() })
            .containsExactly(
                LocalDateTime.of(2026, 10, 20, 0, 0),
                LocalDateTime.of(2026, 10, 21, 0, 0),
                LocalDateTime.of(2026, 10, 22, 0, 0),
            )
            .inOrder()
    }

    @Test
    fun `days stay at midnight across a clock change`() {
        // when
        val ticks = ticks(from = LocalDate.of(2026, 3, 27), days = 4, firstDayOfWeek = DayOfWeek.MONDAY)

        // then
        assertThat(ticks.map { it.time.toLocalDateTime() })
            .containsExactlyElementsIn((27..30).map { LocalDateTime.of(2026, 3, it, 0, 0) })
            .inOrder()
        assertThat(Duration.between(ticks[2].at, ticks[3].at)).isEqualTo(Duration.ofHours(23))
    }

    @Test
    fun `fourteen days across the end of summer time are marked daily`() {
        // when
        val ticks = ticks(from = LocalDate.of(2026, 10, 15), days = 14, firstDayOfWeek = DayOfWeek.MONDAY)

        // then
        assertThat(ticks.map { it.time.toLocalDateTime() })
            .containsExactlyElementsIn((15..28).map { LocalDateTime.of(2026, 10, it, 0, 0) })
            .inOrder()
    }

    @Test
    fun `a month across the end of summer time is marked weekly`() {
        // when
        val ticks = ticks(from = LocalDate.of(2026, 10, 1), days = 31, firstDayOfWeek = DayOfWeek.MONDAY)

        // then
        assertThat(ticks.map { it.time.dayOfMonth }).containsExactly(5, 12, 19, 26).inOrder()
    }

    @Test
    fun `days return to midnight after a clock change at midnight`() {
        // given
        val santiago = ZoneId.of("America/Santiago")
        val start = LocalDate.of(2026, 9, 4).atStartOfDay(santiago)

        // when
        val ticks = calendarTicks(
            start = start.toInstant(),
            end = start.plusDays(5).toInstant(),
            zone = santiago,
            firstDayOfWeek = DayOfWeek.MONDAY,
        )

        // then
        assertThat(ticks.map { it.time.toLocalDateTime() }).containsExactly(
            LocalDateTime.of(2026, 9, 4, 0, 0),
            LocalDateTime.of(2026, 9, 5, 0, 0),
            LocalDateTime.of(2026, 9, 6, 1, 0),
            LocalDateTime.of(2026, 9, 7, 0, 0),
            LocalDateTime.of(2026, 9, 8, 0, 0),
        ).inOrder()
    }

    @Test
    fun `weeks start on the first day of the locale's week`() {
        // when
        val monday = ticks(from = LocalDate.of(2026, 10, 1), days = 30, firstDayOfWeek = DayOfWeek.MONDAY)
        val sunday = ticks(from = LocalDate.of(2026, 10, 1), days = 30, firstDayOfWeek = DayOfWeek.SUNDAY)

        // then
        assertThat(monday.map { it.time.dayOfWeek }.distinct()).containsExactly(DayOfWeek.MONDAY)
        assertThat(monday.map { it.time.dayOfMonth }).containsExactly(5, 12, 19, 26).inOrder()
        assertThat(sunday.map { it.time.dayOfWeek }.distinct()).containsExactly(DayOfWeek.SUNDAY)
        assertThat(sunday.map { it.time.dayOfMonth }).containsExactly(4, 11, 18, 25).inOrder()
    }

    @Test
    fun `fourteen days and today are marked weekly`() {
        // when
        val ticks = ticks(from = LocalDate.of(2026, 10, 11), days = 15, firstDayOfWeek = DayOfWeek.MONDAY)

        // then
        assertThat(ticks.map { it.time.dayOfMonth }).containsExactly(12, 19).inOrder()
    }

    @Test
    fun `three months are marked every other week`() {
        // when
        val ticks = ticks(from = LocalDate.of(2026, 7, 25), days = 92, firstDayOfWeek = DayOfWeek.MONDAY)

        // then
        assertThat(ticks.zipWithNext { a, b -> Duration.between(a.time.toLocalDateTime(), b.time.toLocalDateTime()) }.distinct())
            .containsExactly(Duration.ofDays(14))
        assertThat(ticks.map { it.time.dayOfWeek }.distinct()).containsExactly(DayOfWeek.MONDAY)
    }

    @Test
    fun `six months are marked on the first of each month`() {
        // when
        val ticks = ticks(from = LocalDate.of(2026, 4, 25), days = 183, firstDayOfWeek = DayOfWeek.MONDAY)

        // then
        assertThat(ticks.map { it.time.toLocalDate() })
            .containsExactlyElementsIn((5..10).map { LocalDate.of(2026, it, 1) })
            .inOrder()
    }

    @Test
    fun `a year is marked on the first of each quarter`() {
        // when
        val ticks = ticks(from = LocalDate.of(2025, 10, 25), days = 365, firstDayOfWeek = DayOfWeek.MONDAY)

        // then
        assertThat(ticks.map { it.time.toLocalDate() })
            .containsExactly(
                LocalDate.of(2026, 1, 1),
                LocalDate.of(2026, 4, 1),
                LocalDate.of(2026, 7, 1),
                LocalDate.of(2026, 10, 1),
            )
            .inOrder()
    }

    @Test
    fun `day labels add the month when it changes from one stride earlier`() {
        // when
        val ticks = ticks(from = LocalDate.of(2026, 10, 30), days = 4, firstDayOfWeek = DayOfWeek.MONDAY)

        // then
        assertThat(ticks.map { it.label }).containsExactly(
            CalendarLabel.Day,
            CalendarLabel.Day,
            CalendarLabel.DayMonth,
            CalendarLabel.Day,
        ).inOrder()
    }

    @Test
    fun `the first label compares with one stride before the axis`() {
        // when
        val ticks = ticks(from = LocalDate.of(2026, 10, 1), days = 2, firstDayOfWeek = DayOfWeek.MONDAY)

        // then
        assertThat(ticks.first().label).isEqualTo(CalendarLabel.DayMonth)
    }

    @Test
    fun `day labels add the year when it changes`() {
        // when
        val ticks = ticks(from = LocalDate.of(2026, 12, 31), days = 2, firstDayOfWeek = DayOfWeek.MONDAY)

        // then
        assertThat(ticks.map { it.label }).containsExactly(CalendarLabel.Day, CalendarLabel.DayMonthYear).inOrder()
    }

    @Test
    fun `month labels add the year when it changes`() {
        // when
        val ticks = ticks(from = LocalDate.of(2025, 10, 25), days = 365, firstDayOfWeek = DayOfWeek.MONDAY)

        // then
        assertThat(ticks.map { it.label }).containsExactly(
            CalendarLabel.MonthYear,
            CalendarLabel.Month,
            CalendarLabel.Month,
            CalendarLabel.Month,
        ).inOrder()
    }

    @Test
    fun `five years are marked and labeled by year`() {
        // when
        val ticks = ticks(from = LocalDate.of(2026, 1, 1), days = 1_826, firstDayOfWeek = DayOfWeek.MONDAY)

        // then
        assertThat(ticks.map { it.time.toLocalDate() })
            .containsExactlyElementsIn((2026..2030).map { LocalDate.of(it, 1, 1) })
            .inOrder()
        assertThat(ticks.map { it.label }.distinct()).containsExactly(CalendarLabel.Year)
    }

    @Test
    fun `the whole span of instants is marked by century within the years one to 9999`() {
        // when
        val ticks = calendarTicks(
            start = Instant.MIN,
            end = Instant.MAX,
            zone = zone,
            firstDayOfWeek = DayOfWeek.MONDAY,
        )

        // then
        assertThat(ticks.map { it.time.year }).containsExactlyElementsIn((1..99).map { it * 100 }).inOrder()
    }

    private fun ticks(
        from: LocalDate,
        days: Long,
        firstDayOfWeek: DayOfWeek,
    ): List<CalendarTick> {
        val start = from.atStartOfDay(zone)
        return calendarTicks(
            start = start.toInstant(),
            end = start.plusDays(days).toInstant(),
            zone = zone,
            firstDayOfWeek = firstDayOfWeek,
        )
    }
}
