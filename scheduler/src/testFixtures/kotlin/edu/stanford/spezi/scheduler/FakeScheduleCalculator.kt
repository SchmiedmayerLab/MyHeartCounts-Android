//
// This source file is part of the My Heart Counts Android open-source project
//
// SPDX-FileCopyrightText: 2026 Stanford University and the project authors (see CONTRIBUTORS.md)
//
// SPDX-License-Identifier: MIT

@file:Suppress("detekt:LongParameterList", "detekt:TooManyFunctions")

package edu.stanford.spezi.scheduler

import edu.stanford.spezi.core.time.FakeTimeProvider
import edu.stanford.spezi.scheduler.internal.CompletionPolicyEvaluator
import edu.stanford.spezi.scheduler.internal.OccurrenceGenerator
import edu.stanford.spezi.scheduler.internal.ScheduleCalculatorImpl
import java.time.DayOfWeek
import java.time.Instant
import java.time.ZoneId

/**
 * A [ScheduleCalculator] with a controllable zone and clock for tests. It performs the real
 * occurrence and completion arithmetic; configure its calendar context with [setZone] / [setNow].
 */
class FakeScheduleCalculator : ScheduleCalculator {

    private val timeProvider = FakeTimeProvider()
    private val delegate = ScheduleCalculatorImpl(
        generator = OccurrenceGenerator(timeProvider = timeProvider),
        evaluator = CompletionPolicyEvaluator(timeProvider = timeProvider),
    )

    /**
     * Sets the zone against which occurrences and completion windows resolve.
     */
    fun setZone(zone: ZoneId) {
        timeProvider.setZone(zone)
    }

    /**
     * Sets the instant treated as "now" when evaluating completion windows.
     */
    fun setNow(instant: Instant) {
        timeProvider.setNow(instant)
    }

    override fun occurrences(schedule: Schedule, range: InstantRange): List<Occurrence> =
        delegate.occurrences(schedule = schedule, range = range)

    override fun occurrencesInDay(schedule: Schedule, date: Instant): List<Occurrence> =
        delegate.occurrencesInDay(schedule = schedule, date = date)

    override fun occurrence(schedule: Schedule, startDate: Instant): Occurrence? =
        delegate.occurrence(schedule = schedule, startDate = startDate)

    override fun nextOccurrence(schedule: Schedule, from: Instant): Occurrence? =
        delegate.nextOccurrence(schedule = schedule, from = from)

    override fun isAllowedToComplete(event: Event): Boolean =
        delegate.isAllowedToComplete(event = event)

    override fun dateOnceCompletionIsAllowed(event: Event): Instant? =
        delegate.dateOnceCompletionIsAllowed(event = event)

    override fun dateOnceCompletionBecomesDisallowed(event: Event): Instant? =
        delegate.dateOnceCompletionBecomesDisallowed(event = event)

    override fun hourly(
        interval: Int,
        minute: Int,
        second: Int,
        startingAt: Instant,
        end: RecurrenceEnd,
        duration: ScheduleDuration,
    ): Schedule = delegate.hourly(
        interval = interval,
        minute = minute,
        second = second,
        startingAt = startingAt,
        end = end,
        duration = duration,
    )

    override fun daily(
        interval: Int,
        hour: Int,
        minute: Int,
        second: Int,
        startingAt: Instant,
        end: RecurrenceEnd,
        duration: ScheduleDuration,
    ): Schedule = delegate.daily(
        interval = interval,
        hour = hour,
        minute = minute,
        second = second,
        startingAt = startingAt,
        end = end,
        duration = duration,
    )

    override fun weekly(
        interval: Int,
        weekday: DayOfWeek?,
        hour: Int,
        minute: Int,
        second: Int,
        startingAt: Instant,
        end: RecurrenceEnd,
        duration: ScheduleDuration,
    ): Schedule = delegate.weekly(
        interval = interval,
        weekday = weekday,
        hour = hour,
        minute = minute,
        second = second,
        startingAt = startingAt,
        end = end,
        duration = duration,
    )

    override fun monthly(
        interval: Int,
        day: Int,
        hour: Int,
        minute: Int,
        second: Int,
        startingAt: Instant,
        end: RecurrenceEnd,
        duration: ScheduleDuration,
    ): Schedule = delegate.monthly(
        interval = interval,
        day = day,
        hour = hour,
        minute = minute,
        second = second,
        startingAt = startingAt,
        end = end,
        duration = duration,
    )

    override fun yearly(
        interval: Int,
        month: Int,
        day: Int,
        hour: Int,
        minute: Int,
        second: Int,
        startingAt: Instant,
        end: RecurrenceEnd,
        duration: ScheduleDuration,
    ): Schedule = delegate.yearly(
        interval = interval,
        month = month,
        day = day,
        hour = hour,
        minute = minute,
        second = second,
        startingAt = startingAt,
        end = end,
        duration = duration,
    )
}
