//
// This source file is part of the My Heart Counts open-source project
//
// SPDX-FileCopyrightText: 2026 Stanford University and the project authors (see CONTRIBUTORS.md)
//
// SPDX-License-Identifier: MIT

package edu.stanford.spezi.scheduler

import edu.stanford.spezi.scheduler.ScheduleDuration.Companion.hours
import edu.stanford.spezi.scheduler.ScheduleDuration.Companion.minutes
import edu.stanford.spezi.scheduler.ScheduleDuration.Companion.seconds
import java.time.DayOfWeek
import java.time.Duration
import java.time.Instant

/**
 * Describes when, and how often, the occurrences of a task happen.
 *
 * A schedule starts at [start] and, if it carries a [recurrence], repeats according to that rule.
 * Each generated point in time is an [Occurrence], whose extent is determined by [duration].
 * Recurring schedules are built through [ScheduleCalculator], which resolves their time-of-day
 * against a fixed calendar zone.
 */
data class Schedule(
    val start: Instant,
    val duration: ScheduleDuration = ScheduleDuration.TillEndOfDay,
    val recurrence: Recurrence? = null,
) {
    /**
     * Whether the schedule produces occurrences without a defined end.
     */
    val repeatsIndefinitely: Boolean
        get() = recurrence?.end == RecurrenceEnd.Never

    companion object {
        /**
         * A schedule with a single occurrence at [at].
         */
        fun once(at: Instant, duration: ScheduleDuration = ScheduleDuration.TillEndOfDay): Schedule =
            Schedule(
                start = at,
                duration = duration,
                recurrence = null,
            )
    }
}

/**
 * The extent of a single [Occurrence].
 */
sealed interface ScheduleDuration {
    /**
     * The occurrence spans a whole calendar day, with its start pinned to the start of that day.
     */
    data object AllDay : ScheduleDuration

    /**
     * The occurrence starts at its scheduled time and ends at the end of that calendar day.
     */
    data object TillEndOfDay : ScheduleDuration

    /**
     * The occurrence starts at its scheduled time and lasts for a fixed [duration].
     */
    data class Fixed(val duration: Duration) : ScheduleDuration

    companion object {
        /**
         * A fixed-length duration of [seconds].
         */
        fun seconds(seconds: Long): Fixed = Fixed(duration = Duration.ofSeconds(seconds))

        /**
         * A fixed-length duration of [minutes].
         */
        fun minutes(minutes: Long): Fixed = Fixed(duration = Duration.ofMinutes(minutes))

        /**
         * A fixed-length duration of [hours].
         */
        fun hours(hours: Long): Fixed = Fixed(duration = Duration.ofHours(hours))
    }
}

/**
 * How frequently a [Recurrence] repeats.
 */
enum class RecurrenceFrequency {
    HOURLY,
    DAILY,
    WEEKLY,
    MONTHLY,
    YEARLY,
}

/**
 * The condition under which a [Recurrence] stops producing occurrences.
 */
sealed interface RecurrenceEnd {
    /**
     * The recurrence never ends on its own.
     */
    data object Never : RecurrenceEnd

    /**
     * The recurrence ends after producing [count] occurrences in total.
     */
    data class AfterOccurrences(val count: Int) : RecurrenceEnd

    /**
     * The recurrence produces no occurrences that start after [date].
     */
    data class UntilDate(val date: Instant) : RecurrenceEnd
}

/**
 * The rule by which a [Schedule] repeats its start point.
 *
 * The time-of-day and the aligning calendar fields (weekday, day of month, month) are already
 * reflected in the schedule's start; [weekday], [dayOfMonth], and [month] are retained to describe
 * the rule and to drive future notification matching.
 */
data class Recurrence(
    val frequency: RecurrenceFrequency,
    val interval: Int = 1,
    val weekday: DayOfWeek? = null,
    val dayOfMonth: Int? = null,
    val month: Int? = null,
    val end: RecurrenceEnd = RecurrenceEnd.Never,
)

/**
 * A half-open instant range, `[start, endExclusive)`.
 *
 * Used throughout the scheduler so that an occurrence at exactly [endExclusive] is never included.
 */
data class InstantRange(
    val start: Instant,
    val endExclusive: Instant,
) {
    operator fun contains(instant: Instant): Boolean =
        instant in start..<endExclusive
}
