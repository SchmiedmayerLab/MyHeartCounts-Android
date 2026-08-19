//
// This source file is part of the My Heart Counts Android open-source project
//
// SPDX-FileCopyrightText: 2026 Stanford University and the project authors (see CONTRIBUTORS.md)
//
// SPDX-License-Identifier: MIT

package org.grovealliance.scheduler.internal

import org.grovealliance.core.time.TimeProvider
import org.grovealliance.scheduler.InstantRange
import org.grovealliance.scheduler.Occurrence
import org.grovealliance.scheduler.Recurrence
import org.grovealliance.scheduler.RecurrenceEnd
import org.grovealliance.scheduler.RecurrenceFrequency
import org.grovealliance.scheduler.Schedule
import org.grovealliance.scheduler.ScheduleDuration
import java.time.DayOfWeek
import java.time.Instant
import java.time.ZoneId
import java.time.ZonedDateTime

/**
 * Generates the occurrences of a [Schedule] against the calendar zone reported by [timeProvider].
 *
 * Calendar arithmetic resolves against that zone, so an occurrence's wall-clock time stays stable
 * across daylight-saving transitions.
 */
internal class OccurrenceGenerator(private val timeProvider: TimeProvider) {

    private val zone: ZoneId get() = timeProvider.currentZone()

    /**
     * Builds a recurring [Schedule] whose start is aligned to the requested time of day and aligning
     * calendar fields.
     */
    @Suppress("LongParameterList")
    fun build(
        frequency: RecurrenceFrequency,
        interval: Int,
        start: Instant,
        hour: Int?,
        minute: Int,
        second: Int,
        weekday: DayOfWeek? = null,
        dayOfMonth: Int? = null,
        month: Int? = null,
        duration: ScheduleDuration,
        end: RecurrenceEnd,
    ): Schedule {
        require(interval >= 1) { "Recurrence interval must be at least 1." }
        var aligned = start.atZone(zone)
            .let { if (hour != null) it.withHour(hour) else it }
            .withMinute(minute)
            .withSecond(second)
            .withNano(0)
        if (duration == ScheduleDuration.AllDay) {
            aligned = aligned.toLocalDate().atStartOfDay(zone)
        }
        return Schedule(
            start = aligned.toInstant(),
            duration = duration,
            recurrence = Recurrence(
                frequency = frequency,
                interval = interval,
                weekday = weekday,
                dayOfMonth = dayOfMonth,
                month = month,
                end = end,
            ),
        )
    }

    /**
     * The occurrences of [schedule] whose start falls within [range], in ascending order.
     */
    fun occurrences(schedule: Schedule, range: InstantRange): Sequence<Occurrence> =
        rawStarts(schedule)
            .dropWhile { it < range.start }
            .takeWhile { it < range.endExclusive }
            .map { occurrence(schedule, it) }

    /**
     * The half-open range covering the whole calendar day of [date].
     */
    fun dayRange(date: Instant): InstantRange {
        val startOfDay = date.atZone(zone).toLocalDate().atStartOfDay(zone)
        return InstantRange(
            start = startOfDay.toInstant(),
            endExclusive = startOfDay.plusDays(1).toInstant(),
        )
    }

    /**
     * Resolves the start/end extent of an occurrence beginning at [start] under the schedule's duration.
     */
    fun occurrence(schedule: Schedule, start: Instant): Occurrence {
        val startZdt = start.atZone(zone)
        return when (val duration = schedule.duration) {
            ScheduleDuration.AllDay -> {
                val startOfDay = startZdt.toLocalDate().atStartOfDay(zone)
                Occurrence(
                    start = startOfDay.toInstant(),
                    end = startOfDay.plusDays(1).minusSeconds(1).toInstant(),
                    schedule = schedule,
                )
            }
            ScheduleDuration.TillEndOfDay -> {
                val endOfDay = startZdt.toLocalDate().atStartOfDay(zone).plusDays(1).minusSeconds(1)
                Occurrence(
                    start = start,
                    end = endOfDay.toInstant(),
                    schedule = schedule,
                )
            }
            is ScheduleDuration.Fixed ->
                Occurrence(
                    start = start,
                    end = start.plus(duration.duration),
                    schedule = schedule,
                )
        }
    }

    /**
     * The unbounded, ascending sequence of occurrence start instants, with the recurrence end applied.
     *
     * Monthly and yearly recurrences skip periods whose target day does not exist (e.g. the 31st of a
     * 30-day month, or Feb 29 in a common year), matching the calendar's recurrence behaviour.
     */
    private fun rawStarts(schedule: Schedule): Sequence<Instant> {
        val recurrence = schedule.recurrence ?: return sequenceOf(schedule.start)
        val start = schedule.start.atZone(zone)
        val interval = recurrence.interval
        val periods: Sequence<ZonedDateTime> = when (recurrence.frequency) {
            RecurrenceFrequency.HOURLY -> generateSequence(start) { it.plusHours(interval.toLong()) }
            RecurrenceFrequency.DAILY -> generateSequence(start) { it.plusDays(interval.toLong()) }
            RecurrenceFrequency.WEEKLY -> {
                val first = recurrence.weekday?.let { alignToWeekday(start, it) } ?: start
                generateSequence(first) { it.plusWeeks(interval.toLong()) }
            }
            RecurrenceFrequency.MONTHLY -> monthlyStarts(start, recurrence.dayOfMonth ?: start.dayOfMonth, interval)
            RecurrenceFrequency.YEARLY ->
                yearlyStarts(start, recurrence.month ?: start.monthValue, recurrence.dayOfMonth ?: start.dayOfMonth, interval)
        }
        return applyEnd(periods, recurrence.end).map { it.toInstant() }
    }

    private fun applyEnd(periods: Sequence<ZonedDateTime>, end: RecurrenceEnd): Sequence<ZonedDateTime> =
        when (end) {
            RecurrenceEnd.Never -> periods
            is RecurrenceEnd.AfterOccurrences -> periods.take(end.count)
            is RecurrenceEnd.UntilDate -> periods.takeWhile { !it.toInstant().isAfter(end.date) }
        }

    private fun monthlyStarts(start: ZonedDateTime, day: Int, interval: Int): Sequence<ZonedDateTime> {
        val baseMonth = start.withDayOfMonth(1)
        return (0 until MAX_PERIODS).asSequence()
            .map { baseMonth.plusMonths((it * interval).toLong()) }
            .mapNotNull { month -> if (day <= month.toLocalDate().lengthOfMonth()) month.withDayOfMonth(day) else null }
            .filter { !it.isBefore(start) }
    }

    private fun yearlyStarts(start: ZonedDateTime, month: Int, day: Int, interval: Int): Sequence<ZonedDateTime> {
        val baseYear = start.withDayOfMonth(1).withMonth(1)
        return (0 until MAX_PERIODS).asSequence()
            .map { baseYear.plusYears((it * interval).toLong()).withMonth(month) }
            .mapNotNull { date -> if (day <= date.toLocalDate().lengthOfMonth()) date.withDayOfMonth(day) else null }
            .filter { !it.isBefore(start) }
    }

    private fun alignToWeekday(start: ZonedDateTime, weekday: DayOfWeek): ZonedDateTime {
        val delta = ((weekday.value - start.dayOfWeek.value) + DAYS_PER_WEEK) % DAYS_PER_WEEK
        return start.plusDays(delta.toLong())
    }

    private companion object {
        const val DAYS_PER_WEEK = 7
        const val MAX_PERIODS = 4_000
    }
}
