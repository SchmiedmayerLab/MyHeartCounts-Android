//
// This source file is part of the My Heart Counts Android open-source project
//
// SPDX-FileCopyrightText: 2026 Stanford University and the project authors (see CONTRIBUTORS.md)
//
// SPDX-License-Identifier: MIT

@file:Suppress("detekt:LongParameterList", "detekt:TooManyFunctions")

package edu.stanford.spezi.scheduler.internal

import edu.stanford.spezi.scheduler.Event
import edu.stanford.spezi.scheduler.InstantRange
import edu.stanford.spezi.scheduler.Occurrence
import edu.stanford.spezi.scheduler.RecurrenceEnd
import edu.stanford.spezi.scheduler.RecurrenceFrequency
import edu.stanford.spezi.scheduler.Schedule
import edu.stanford.spezi.scheduler.ScheduleCalculator
import edu.stanford.spezi.scheduler.ScheduleDuration
import java.time.DayOfWeek
import java.time.Instant

/**
 * Resolves occurrences and completion windows by delegating to a [generator] for schedule math and
 * an [evaluator] for completion policy, each bound to the graph's calendar zone and clock.
 */
internal class ScheduleCalculatorImpl(
    private val generator: OccurrenceGenerator,
    private val evaluator: CompletionPolicyEvaluator,
) : ScheduleCalculator {

    override fun occurrences(schedule: Schedule, range: InstantRange): List<Occurrence> =
        generator.occurrences(schedule = schedule, range = range).toList()

    override fun occurrencesInDay(schedule: Schedule, date: Instant): List<Occurrence> =
        generator.occurrences(schedule = schedule, range = generator.dayRange(date)).toList()

    override fun occurrence(schedule: Schedule, startDate: Instant): Occurrence? =
        generator.occurrences(
            schedule = schedule,
            range = InstantRange(
                start = startDate,
                endExclusive = startDate.plusSeconds(1),
            ),
        ).firstOrNull { it.start == startDate }

    override fun nextOccurrence(schedule: Schedule, from: Instant): Occurrence? =
        generator.occurrences(
            schedule = schedule,
            range = InstantRange(
                start = from,
                endExclusive = Instant.MAX,
            ),
        ).firstOrNull()

    override fun isAllowedToComplete(event: Event): Boolean =
        evaluator.isAllowedToComplete(
            policy = event.task.completionPolicy,
            occurrence = event.occurrence,
        )

    override fun dateOnceCompletionIsAllowed(event: Event): Instant? =
        evaluator.dateOnceCompletionIsAllowed(
            policy = event.task.completionPolicy,
            occurrence = event.occurrence,
        )

    override fun dateOnceCompletionBecomesDisallowed(event: Event): Instant? =
        evaluator.dateOnceCompletionBecomesDisallowed(
            policy = event.task.completionPolicy,
            occurrence = event.occurrence,
        )

    override fun hourly(
        interval: Int,
        minute: Int,
        second: Int,
        startingAt: Instant,
        end: RecurrenceEnd,
        duration: ScheduleDuration,
    ): Schedule = generator.build(
        frequency = RecurrenceFrequency.HOURLY,
        interval = interval,
        start = startingAt,
        hour = null,
        minute = minute,
        second = second,
        duration = duration,
        end = end,
    )

    override fun daily(
        interval: Int,
        hour: Int,
        minute: Int,
        second: Int,
        startingAt: Instant,
        end: RecurrenceEnd,
        duration: ScheduleDuration,
    ): Schedule = generator.build(
        frequency = RecurrenceFrequency.DAILY,
        interval = interval,
        start = startingAt,
        hour = hour,
        minute = minute,
        second = second,
        duration = duration,
        end = end,
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
    ): Schedule = generator.build(
        frequency = RecurrenceFrequency.WEEKLY,
        interval = interval,
        start = startingAt,
        hour = hour,
        minute = minute,
        second = second,
        weekday = weekday,
        duration = duration,
        end = end,
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
    ): Schedule = generator.build(
        frequency = RecurrenceFrequency.MONTHLY,
        interval = interval,
        start = startingAt,
        hour = hour,
        minute = minute,
        second = second,
        dayOfMonth = day,
        duration = duration,
        end = end,
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
    ): Schedule = generator.build(
        frequency = RecurrenceFrequency.YEARLY,
        interval = interval,
        start = startingAt,
        hour = hour,
        minute = minute,
        second = second,
        dayOfMonth = day,
        month = month,
        duration = duration,
        end = end,
    )
}
