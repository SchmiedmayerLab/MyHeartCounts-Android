//
// This source file is part of the My Heart Counts Android open-source project
//
// SPDX-FileCopyrightText: 2026 Stanford University and the project authors (see CONTRIBUTORS.md)
//
// SPDX-License-Identifier: MIT

package edu.stanford.myheartcounts.dashboard.ui.internal

import java.time.DayOfWeek
import java.time.Duration
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import java.time.ZonedDateTime
import java.time.temporal.TemporalAdjusters

/**
 * The calendar unit a time axis is marked by, chosen from the length of the axis.
 *
 * @param months the number of months between two marks, or zero when marks are days apart
 * @param days the number of days between two marks, or zero when marks are months apart
 */
internal enum class CalendarStride(
    private val months: Long,
    private val days: Long,
) {
    Day(months = 0, days = 1),
    Week(months = 0, days = 7),
    Fortnight(months = 0, days = 14),
    Month(months = 1, days = 0),
    Quarter(months = 3, days = 0),
    Year(months = 12, days = 0),
    Decade(months = 120, days = 0),
    Century(months = 1_200, days = 0),
    ;

    /**
     * Whether marks are labeled with their month rather than their day.
     */
    val labelsMonths: Boolean
        get() = months > 0

    /**
     * Whether marks are labeled with their year alone.
     */
    val labelsYears: Boolean
        get() = months >= MONTHS_PER_YEAR

    /**
     * The start of the day one stride after [time]'s day.
     */
    fun next(time: ZonedDateTime): ZonedDateTime =
        time.toLocalDate().plusMonths(months).plusDays(days).atStartOfDay(time.zone)

    /**
     * The start of the day one stride before [time]'s day.
     */
    fun previous(time: ZonedDateTime): ZonedDateTime =
        time.toLocalDate().minusMonths(months).minusDays(days).atStartOfDay(time.zone)

    /**
     * The first mark at or after [time], at the start of a day, a week starting on [firstDayOfWeek],
     * or the first month of a whole number of strides since year zero.
     */
    fun firstAtOrAfter(
        time: ZonedDateTime,
        firstDayOfWeek: DayOfWeek,
    ): ZonedDateTime {
        val day = time.toLocalDate()
        val candidate = when (this) {
            Day -> day
            Week, Fortnight -> day.with(TemporalAdjusters.previousOrSame(firstDayOfWeek))
            Month, Quarter, Year, Decade, Century -> {
                val month = Math.floorDiv(day.year * MONTHS_PER_YEAR + day.monthValue - 1, months) * months
                LocalDate.of(Math.floorDiv(month, MONTHS_PER_YEAR).toInt(), Math.floorMod(month, MONTHS_PER_YEAR).toInt() + 1, 1)
            }
        }.atStartOfDay(time.zone)
        return if (candidate.isBefore(time)) next(candidate) else candidate
    }

    companion object {

        /**
         * The stride for an axis as long as [length] in local time: days up to two weeks, weeks up
         * to a month, fortnights up to three months, months up to six months, quarters up to two
         * years, years up to twenty years, decades up to two hundred years, and centuries beyond.
         */
        fun forLength(length: Duration): CalendarStride = when {
            length <= Duration.ofDays(DAYS_IN_TWO_WEEKS) -> Day
            length <= Duration.ofDays(DAYS_IN_MONTH) -> Week
            length <= Duration.ofDays(DAYS_IN_THREE_MONTHS) -> Fortnight
            length <= Duration.ofDays(DAYS_IN_SIX_MONTHS) -> Month
            length <= Duration.ofDays(DAYS_IN_TWO_YEARS) -> Quarter
            length <= Duration.ofDays(DAYS_IN_TWENTY_YEARS) -> Year
            length <= Duration.ofDays(DAYS_IN_TWO_CENTURIES) -> Decade
            else -> Century
        }

        /**
         * The days of two weeks.
         */
        private const val DAYS_IN_TWO_WEEKS = 14L

        /**
         * The days of a month, taken as its longest.
         */
        private const val DAYS_IN_MONTH = 31L

        /**
         * The days of three months, each taken as its longest.
         */
        private const val DAYS_IN_THREE_MONTHS = 93L

        /**
         * The days of six months, each taken as its longest.
         */
        private const val DAYS_IN_SIX_MONTHS = 186L

        /**
         * The days of two years, one of them a leap year.
         */
        private const val DAYS_IN_TWO_YEARS = 731L

        /**
         * The days of twenty years, five of them leap years.
         */
        private const val DAYS_IN_TWENTY_YEARS = 7_305L

        /**
         * The days of two hundred years, each taken as a fourth of four years.
         */
        private const val DAYS_IN_TWO_CENTURIES = 73_050L

        /**
         * The months of a year.
         */
        private const val MONTHS_PER_YEAR = 12L
    }
}

/**
 * The parts of a date a time axis label shows.
 */
internal enum class CalendarLabel {
    Day,
    DayMonth,
    DayMonthYear,
    Month,
    MonthYear,
    Year,
}

/**
 * A mark of a calendar time axis.
 *
 * @param at the moment marked
 * @param time [at] in the axis' time zone
 * @param label the parts of the date its label shows
 */
internal data class CalendarTick(
    val at: Instant,
    val time: ZonedDateTime,
    val label: CalendarLabel,
)

/**
 * The marks of a time axis from [start] to [end], in [zone], by the stride for its length.
 *
 * The stride follows the length of the axis in local time, so clock changes do not change it. A
 * label shows the day, adding the month when it differs from one stride earlier and the year when
 * that differs too; when marks are months apart, it shows the month, adding the year when it
 * differs from one stride earlier; when marks are years apart, it shows the year. Marks are at or
 * after [start] and before [end], and only between the years 1 and 9999.
 */
internal fun calendarTicks(
    start: Instant,
    end: Instant,
    zone: ZoneId,
    firstDayOfWeek: DayOfWeek,
): List<CalendarTick> {
    val first = start.coerceIn(EARLIEST_MARKED, LATEST_MARKED).atZone(zone)
    val last = end.coerceIn(EARLIEST_MARKED, LATEST_MARKED)
    val stride = CalendarStride.forLength(Duration.between(first.toLocalDateTime(), last.atZone(zone).toLocalDateTime()))
    return generateSequence(stride.firstAtOrAfter(first, firstDayOfWeek)) { stride.next(it) }
        .takeWhile { it.toInstant().isBefore(last) }
        .map { time ->
            CalendarTick(
                at = time.toInstant(),
                time = time,
                label = calendarLabel(
                    time = time,
                    previous = stride.previous(time),
                    stride = stride,
                ),
            )
        }
        .toList()
}

private fun calendarLabel(
    time: ZonedDateTime,
    previous: ZonedDateTime,
    stride: CalendarStride,
): CalendarLabel {
    val newYear = time.year != previous.year
    val newMonth = newYear || time.month != previous.month
    return when {
        stride.labelsYears -> CalendarLabel.Year
        stride.labelsMonths && newYear -> CalendarLabel.MonthYear
        stride.labelsMonths -> CalendarLabel.Month
        newYear -> CalendarLabel.DayMonthYear
        newMonth -> CalendarLabel.DayMonth
        else -> CalendarLabel.Day
    }
}

/**
 * The earliest moment a calendar time axis marks.
 */
private val EARLIEST_MARKED = Instant.parse("0001-01-01T00:00:00Z")

/**
 * The latest moment a calendar time axis marks.
 */
private val LATEST_MARKED = Instant.parse("9999-01-01T00:00:00Z")
