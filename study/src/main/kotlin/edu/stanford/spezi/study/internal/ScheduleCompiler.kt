//
// This source file is part of the My Heart Counts Android open-source project
//
// SPDX-FileCopyrightText: 2026 Stanford University and the project authors (see CONTRIBUTORS.md)
//
// SPDX-License-Identifier: MIT

package edu.stanford.spezi.study.internal

import edu.stanford.spezi.core.time.TimeProvider
import edu.stanford.spezi.scheduler.RecurrenceEnd
import edu.stanford.spezi.scheduler.Schedule
import edu.stanford.spezi.scheduler.ScheduleCalculator
import edu.stanford.spezi.scheduler.ScheduleDuration
import edu.stanford.spezi.studydefinition.DateComponents
import edu.stanford.spezi.studydefinition.OneTimeSchedule
import edu.stanford.spezi.studydefinition.RepetitionPattern
import edu.stanford.spezi.studydefinition.ScheduleDefinition
import java.time.Instant
import java.time.LocalDateTime
import java.time.ZoneId

/**
 * Translates a study's [ScheduleDefinition]s into scheduler [Schedule]s, building recurring schedules
 * through the [calculator] and aligning times of day against the zone reported by [timeProvider].
 *
 * Event-based one-time schedules are not compiled here; they are materialized only when their
 * lifecycle event fires (see [eventOccurrenceSchedule]).
 */
internal class ScheduleCompiler(
    private val calculator: ScheduleCalculator,
    private val timeProvider: TimeProvider,
) {

    private val zone: ZoneId get() = timeProvider.currentZone()

    /**
     * The scheduler schedule for a non-event schedule definition, anchored to [enrollmentDate].
     *
     * Returns `null` for event-based one-time schedules, which are handled separately.
     */
    fun schedule(definition: ScheduleDefinition, enrollmentDate: Instant): Schedule? = when (definition) {
        is ScheduleDefinition.Once -> when (val once = definition.schedule) {
            is OneTimeSchedule.Date -> Schedule.once(
                at = once.date.toInstant(),
                duration = ScheduleDuration.TillEndOfDay,
            )
            is OneTimeSchedule.Event -> null
        }
        is ScheduleDefinition.Repeated -> repeated(definition, enrollmentDate)
    }

    /**
     * A single-occurrence schedule for an event that fired at [eventDate], applying the
     * [OneTimeSchedule.Event] offset and time.
     */
    fun eventOccurrenceSchedule(event: OneTimeSchedule.Event, eventDate: Instant): Schedule {
        var occurrence = eventDate.atZone(zone).plusDays(event.offsetInDays.toLong())
        event.time?.let { time ->
            occurrence = occurrence
                .withHour(time.hour)
                .withMinute(time.minute)
                .withSecond(time.second)
                .withNano(0)
        }
        return Schedule.once(at = occurrence.toInstant())
    }

    private fun repeated(definition: ScheduleDefinition.Repeated, enrollmentDate: Instant): Schedule {
        val start = enrollmentDate.atZone(zone).plusDays(definition.offset.day.toLong())
        val startInstant = start.toInstant()
        return when (val pattern = definition.pattern) {
            is RepetitionPattern.Daily -> calculator.daily(
                interval = pattern.interval,
                hour = pattern.hour,
                minute = pattern.minute,
                second = pattern.second,
                startingAt = startInstant,
                end = RecurrenceEnd.Never,
                duration = ScheduleDuration.TillEndOfDay,
            )
            is RepetitionPattern.Weekly -> calculator.weekly(
                interval = pattern.interval,
                weekday = pattern.weekday?.toDayOfWeek() ?: start.dayOfWeek,
                hour = pattern.hour,
                minute = pattern.minute,
                second = pattern.second,
                startingAt = startInstant,
                end = RecurrenceEnd.Never,
                duration = ScheduleDuration.TillEndOfDay,
            )
            is RepetitionPattern.Monthly -> calculator.monthly(
                interval = pattern.interval,
                day = pattern.day ?: start.dayOfMonth,
                hour = pattern.hour,
                minute = pattern.minute,
                second = pattern.second,
                startingAt = startInstant,
                end = RecurrenceEnd.Never,
                duration = ScheduleDuration.TillEndOfDay,
            )
        }
    }

    private fun DateComponents.toInstant(): Instant =
        LocalDateTime.of(year, month, day, hour, minute, second).atZone(zone).toInstant()
}
