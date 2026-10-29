//
// This source file is part of the My Heart Counts Android open-source project
//
// SPDX-FileCopyrightText: 2026 Stanford University and the project authors (see CONTRIBUTORS.md)
//
// SPDX-License-Identifier: MIT

package edu.stanford.myheartcounts.home

import edu.stanford.myheartcounts.task.MissedEventsSource
import edu.stanford.myheartcounts.task.todayRanges
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.map
import org.grovealliance.core.time.TimeProvider
import org.grovealliance.scheduler.Event
import org.grovealliance.scheduler.InstantRange
import org.grovealliance.scheduler.Scheduler
import org.grovealliance.study.StudyManager
import java.time.Instant

/**
 * The tasks the Home tab shows: today's events, and how many were missed recently; see
 * [MissedEventsSource].
 */
data class HomeTasks(
    val events: List<Event>,
    val missedEventCount: Int,
)

/**
 * Supplies the participant's current tasks.
 */
interface HomeTasksSource {
    /**
     * A live stream of the participant's tasks.
     */
    suspend fun tasks(): Flow<HomeTasks>
}

/**
 * Reads today's tasks from the scheduler, anchoring the query to the participant's enrollment so
 * that outstanding one-off components stay visible.
 */
@OptIn(ExperimentalCoroutinesApi::class)
class HomeTasksSourceImpl(
    private val studyManager: StudyManager,
    private val scheduler: Scheduler,
    private val missedEventsSource: MissedEventsSource,
    private val timeProvider: TimeProvider,
) : HomeTasksSource {

    override suspend fun tasks(): Flow<HomeTasks> {
        val enrollmentDate = studyManager.enrollments.first().firstOrNull()?.enrollmentDate
            ?: timeProvider.nowInstant()

        return timeProvider.todayRanges().flatMapLatest { today ->
            combine(
                queryTodaysEvents(today = today, enrollmentDate = enrollmentDate),
                missedEventsSource.missedEvents(today = today),
            ) { events, missed ->
                HomeTasks(
                    events = events,
                    missedEventCount = missed.size,
                )
            }
        }
    }

    /**
     * The events worth prompting the participant to complete today.
     *
     * Beyond the events occurring within [today], this keeps non-recurring events from before it: a
     * one-off component stays visible for as long as it is outstanding, and once completed remains
     * visible for the rest of the day it was completed on. The underlying query therefore reaches
     * back to the start of the enrollment day.
     */
    private fun queryTodaysEvents(
        today: InstantRange,
        enrollmentDate: Instant,
    ): Flow<List<Event>> {
        val zone = timeProvider.currentZone()
        val enrollmentDayStart = enrollmentDate.atZone(zone).toLocalDate().atStartOfDay(zone).toInstant()
        val widened = InstantRange(
            start = minOf(today.start, enrollmentDayStart),
            endExclusive = today.endExclusive,
        )
        return scheduler.queryEvents(range = widened)
            .map { events -> events.filter { belongsOnHome(event = it, today = today) } }
    }

    /**
     * Whether [event] should appear on the Home tab for the day covered by [today].
     */
    private fun belongsOnHome(event: Event, today: InstantRange): Boolean = when {
        event.occurrence.start in today -> true
        event.occurrence.schedule.recurrence != null -> false
        else -> event.outcome?.completionDate?.let { it in today } ?: true
    }
}
