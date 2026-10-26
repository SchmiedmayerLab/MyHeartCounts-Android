//
// This source file is part of the My Heart Counts Android open-source project
//
// SPDX-FileCopyrightText: 2026 Stanford University and the project authors (see CONTRIBUTORS.md)
//
// SPDX-License-Identifier: MIT

package edu.stanford.myheartcounts.upcoming

import edu.stanford.myheartcounts.task.todayRanges
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flatMapLatest
import org.grovealliance.core.time.TimeProvider
import org.grovealliance.scheduler.Event
import org.grovealliance.scheduler.InstantRange
import org.grovealliance.scheduler.Scheduler

/**
 * Supplies the tasks coming up for the participant.
 */
interface UpcomingTasksSource {
    /**
     * A live stream of the events occurring from the start of today through the following
     * [UPCOMING_DAYS] days, sorted by occurrence. The window moves on as each day ends.
     */
    fun upcomingEvents(): Flow<List<Event>>

    companion object {
        /**
         * How many days, today included, the upcoming tasks span.
         */
        const val UPCOMING_DAYS = 14L
    }
}

/**
 * Reads the upcoming tasks from the scheduler.
 */
@OptIn(ExperimentalCoroutinesApi::class)
class UpcomingTasksSourceImpl(
    private val scheduler: Scheduler,
    private val timeProvider: TimeProvider,
) : UpcomingTasksSource {

    override fun upcomingEvents(): Flow<List<Event>> {
        val zone = timeProvider.currentZone()
        return timeProvider.todayRanges().flatMapLatest { today ->
            scheduler.queryEvents(
                range = InstantRange(
                    start = today.start,
                    endExclusive = today.start.atZone(zone).plusDays(UpcomingTasksSource.UPCOMING_DAYS).toInstant(),
                ),
            )
        }
    }
}
