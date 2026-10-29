//
// This source file is part of the My Heart Counts Android open-source project
//
// SPDX-FileCopyrightText: 2026 Stanford University and the project authors (see CONTRIBUTORS.md)
//
// SPDX-License-Identifier: MIT

package edu.stanford.myheartcounts.task

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import org.grovealliance.core.time.TimeProvider
import org.grovealliance.scheduler.Event
import org.grovealliance.scheduler.InstantRange
import org.grovealliance.scheduler.ScheduleCalculator
import org.grovealliance.scheduler.Scheduler

/**
 * Supplies the tasks a participant missed recently and may still catch up on.
 */
interface MissedEventsSource {
    /**
     * A live stream of the events missed in the [MISSED_DAYS] days before [today], sorted by
     * occurrence.
     *
     * An event counts as missed when it was not completed and is the latest outstanding occurrence of
     * its task: its next occurrence falls within the [MISSED_DAYS] days from the start of [today] and
     * has not been completed either. Tasks that do not recur are never missed.
     *
     * @param today The day the window is anchored to, in the current zone.
     */
    fun missedEvents(today: InstantRange): Flow<List<Event>>

    companion object {
        /**
         * How many days before and after the start of today the window reaches.
         */
        const val MISSED_DAYS = 14L
    }
}

/**
 * Derives missed events from the scheduler's events before and after the start of today.
 */
class MissedEventsSourceImpl(
    private val scheduler: Scheduler,
    private val scheduleCalculator: ScheduleCalculator,
    private val timeProvider: TimeProvider,
) : MissedEventsSource {

    override fun missedEvents(today: InstantRange): Flow<List<Event>> {
        val startOfToday = today.start.atZone(timeProvider.currentZone())
        val past = InstantRange(
            start = startOfToday.minusDays(MissedEventsSource.MISSED_DAYS).toInstant(),
            endExclusive = today.start,
        )
        val upcoming = InstantRange(
            start = today.start,
            endExclusive = startOfToday.plusDays(MissedEventsSource.MISSED_DAYS).toInstant(),
        )
        return combine(
            scheduler.queryEvents(range = past),
            scheduler.queryEvents(range = upcoming),
        ) { pastEvents, upcomingEvents ->
            val upcomingById = upcomingEvents.associateBy { it.id }
            pastEvents.filter { event ->
                isMissed(event = event, upcoming = upcoming, upcomingById = upcomingById)
            }
        }
    }

    /**
     * Whether [event] is the latest outstanding occurrence of its task: not completed, and followed
     * by an occurrence within [upcoming] that is not completed either.
     */
    private fun isMissed(
        event: Event,
        upcoming: InstantRange,
        upcomingById: Map<Event.Id, Event>,
    ): Boolean {
        if (event.isCompleted) return false
        val next = scheduleCalculator.nextOccurrence(
            schedule = event.occurrence.schedule,
            from = event.occurrence.start.plusSeconds(1),
        )?.takeIf { it.start in upcoming } ?: return false
        val nextEvent = upcomingById[Event.Id(taskId = event.task.id, occurrenceStart = next.start)]
        return nextEvent?.isCompleted == false
    }
}
