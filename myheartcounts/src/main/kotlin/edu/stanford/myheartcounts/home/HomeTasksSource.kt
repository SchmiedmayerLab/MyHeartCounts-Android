//
// This source file is part of the My Heart Counts Android open-source project
//
// SPDX-FileCopyrightText: 2026 Stanford University and the project authors (see CONTRIBUTORS.md)
//
// SPDX-License-Identifier: MIT

package edu.stanford.myheartcounts.home

import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.map
import org.grovealliance.core.time.TimeProvider
import org.grovealliance.scheduler.Event
import org.grovealliance.scheduler.InstantRange
import org.grovealliance.scheduler.Scheduler
import org.grovealliance.study.StudyManager
import java.time.Duration
import java.time.Instant
import java.time.ZoneId
import java.time.temporal.ChronoUnit

/**
 * The tasks the Home tab shows: today's events, and how many were missed recently.
 */
data class HomeTasks(
    val events: List<Event>,
    val missedEventCount: Int,
)

/**
 * Supplies the participant's current tasks, and records their completion.
 */
interface HomeTasksSource {
    /**
     * A live stream of the participant's tasks.
     */
    suspend fun tasks(): Flow<HomeTasks>

    /**
     * Records [event] as completed.
     */
    suspend fun complete(event: Event): Result<Unit>
}

/**
 * Reads today's tasks from the scheduler, anchoring the query to the participant's enrollment so
 * that outstanding one-off components stay visible.
 */
@OptIn(ExperimentalCoroutinesApi::class)
class HomeTasksSourceImpl(
    private val studyManager: StudyManager,
    private val scheduler: Scheduler,
    private val timeProvider: TimeProvider,
) : HomeTasksSource {

    override suspend fun tasks(): Flow<HomeTasks> {
        val enrollmentDate = studyManager.enrollments.first().firstOrNull()?.enrollmentDate
            ?: timeProvider.nowInstant()

        val zone = timeProvider.currentZone()

        return today(zone).flatMapLatest { today ->
            combine(
                queryTodaysEvents(today = today, enrollmentDate = enrollmentDate, zone = zone),
                scheduler.queryMissedEvents(range = missedRange(today)),
            ) { events, missed ->
                HomeTasks(
                    events = events,
                    missedEventCount = missed.size,
                )
            }
        }
    }

    /**
     * The day currently being shown, re-emitted as each day ends so that a session left open
     * overnight moves on rather than holding yesterday.
     */
    private fun today(zone: ZoneId): Flow<InstantRange> = flow {
        while (true) {
            val today = todayRange(zone)
            emit(today)
            val untilTomorrow = Duration.between(timeProvider.nowInstant(), today.endExclusive)
            delay(untilTomorrow.toMillis().coerceAtLeast(1))
        }
    }

    override suspend fun complete(event: Event): Result<Unit> =
        scheduler.complete(event = event).map { }

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
        zone: ZoneId,
    ): Flow<List<Event>> {
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

    private fun todayRange(zone: ZoneId): InstantRange {
        val startOfToday = timeProvider.nowInstant().atZone(zone).toLocalDate().atStartOfDay(zone)
        return InstantRange(
            start = startOfToday.toInstant(),
            endExclusive = startOfToday.plusDays(1).toInstant(),
        )
    }

    /**
     * The window searched for missed tasks: the two weeks leading up to [today].
     */
    private fun missedRange(today: InstantRange) = InstantRange(
        start = today.start.minus(MISSED_TASKS_DAYS, ChronoUnit.DAYS),
        endExclusive = today.start,
    )

    private companion object {
        const val MISSED_TASKS_DAYS = 14L
    }
}
