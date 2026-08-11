//
// This source file is part of the My Heart Counts Android open-source project
//
// SPDX-FileCopyrightText: 2026 Stanford University and the project authors (see CONTRIBUTORS.md)
//
// SPDX-License-Identifier: MIT

@file:Suppress("detekt:LongParameterList", "detekt:TooManyFunctions")

package edu.stanford.spezi.scheduler

import java.time.DayOfWeek
import java.time.Instant

/**
 * Computes the time-derived facts of a schedule — how its occurrences fall, and when an event may be
 * completed — against the graph's calendar zone and current-time source.
 *
 * A single shared instance binds the calendar context once, so that every consumer resolves
 * occurrences and completion windows consistently.
 */
interface ScheduleCalculator {
    /**
     * The occurrences of [schedule] that start within [range], in ascending order.
     */
    fun occurrences(schedule: Schedule, range: InstantRange): List<Occurrence>

    /**
     * The occurrences of [schedule] that start on the calendar day of [date].
     */
    fun occurrencesInDay(schedule: Schedule, date: Instant): List<Occurrence>

    /**
     * The occurrence of [schedule] whose start exactly matches [startDate], if one exists.
     */
    fun occurrence(schedule: Schedule, startDate: Instant): Occurrence?

    /**
     * The first occurrence of [schedule] that starts at or after [from], if any.
     */
    fun nextOccurrence(schedule: Schedule, from: Instant): Occurrence?

    /**
     * Whether [event] may be completed now, per its task's completion policy.
     */
    fun isAllowedToComplete(event: Event): Boolean

    /**
     * The instant at which completion of [event] becomes allowed, or `null` if it is already allowed
     * or will never become allowed.
     */
    fun dateOnceCompletionIsAllowed(event: Event): Instant?

    /**
     * The instant at which completion of [event] stops being allowed, or `null` if it does not become
     * disallowed in the future.
     */
    fun dateOnceCompletionBecomesDisallowed(event: Event): Instant?

    /**
     * A schedule that repeats every [interval] hours, at the given [minute] and [second].
     */
    fun hourly(
        interval: Int = 1,
        minute: Int,
        second: Int = 0,
        startingAt: Instant,
        end: RecurrenceEnd = RecurrenceEnd.Never,
        duration: ScheduleDuration = ScheduleDuration.TillEndOfDay,
    ): Schedule

    /**
     * A schedule that repeats every [interval] days, at the given time of day.
     */
    fun daily(
        interval: Int = 1,
        hour: Int,
        minute: Int,
        second: Int = 0,
        startingAt: Instant,
        end: RecurrenceEnd = RecurrenceEnd.Never,
        duration: ScheduleDuration = ScheduleDuration.TillEndOfDay,
    ): Schedule

    /**
     * A schedule that repeats every [interval] weeks, on [weekday] (or the start date's weekday when
     * `null`).
     */
    fun weekly(
        interval: Int = 1,
        weekday: DayOfWeek? = null,
        hour: Int,
        minute: Int,
        second: Int = 0,
        startingAt: Instant,
        end: RecurrenceEnd = RecurrenceEnd.Never,
        duration: ScheduleDuration = ScheduleDuration.TillEndOfDay,
    ): Schedule

    /**
     * A schedule that repeats every [interval] months, on the given [day] of the month.
     */
    fun monthly(
        interval: Int = 1,
        day: Int,
        hour: Int,
        minute: Int,
        second: Int = 0,
        startingAt: Instant,
        end: RecurrenceEnd = RecurrenceEnd.Never,
        duration: ScheduleDuration = ScheduleDuration.TillEndOfDay,
    ): Schedule

    /**
     * A schedule that repeats every [interval] years, on the given [month] and [day].
     */
    fun yearly(
        interval: Int = 1,
        month: Int,
        day: Int,
        hour: Int,
        minute: Int,
        second: Int = 0,
        startingAt: Instant,
        end: RecurrenceEnd = RecurrenceEnd.Never,
        duration: ScheduleDuration = ScheduleDuration.TillEndOfDay,
    ): Schedule
}
