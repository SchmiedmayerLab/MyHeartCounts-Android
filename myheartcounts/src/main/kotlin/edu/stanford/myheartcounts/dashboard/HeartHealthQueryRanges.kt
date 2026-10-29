//
// This source file is part of the My Heart Counts Android open-source project
//
// SPDX-FileCopyrightText: 2026 Stanford University and the project authors (see CONTRIBUTORS.md)
//
// SPDX-License-Identifier: MIT

@file:Suppress("detekt:MagicNumber")

package edu.stanford.myheartcounts.dashboard

import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.isActive
import org.grovealliance.core.time.TimeProvider
import org.grovealliance.scheduler.InstantRange
import java.time.LocalDate
import java.time.ZoneId
import kotlin.time.Duration.Companion.minutes

/**
 * Calendar-aligned windows used by the current heart health score.
 */
interface HeartHealthQueryRanges {
    val exercise: InstantRange
    val fortnight: InstantRange
    val custom: InstantRange
    val quarter: InstantRange

    /**
     * Emits on collection and when the local day or time zone changes.
     */
    val changes: Flow<Unit>
}

class HeartHealthQueryRangesImpl(
    private val timeProvider: TimeProvider,
) : HeartHealthQueryRanges {
    private val zone: ZoneId get() = timeProvider.currentZone()
    private val today: LocalDate get() = timeProvider.nowInstant().atZone(zone).toLocalDate()
    private val month: LocalDate get() = today.withDayOfMonth(1)

    override val exercise: InstantRange get() = range(start = today.minusDays(7), end = today)
    override val fortnight: InstantRange get() = range(start = today.minusDays(13), end = today.plusDays(1))
    override val custom: InstantRange get() = range(start = month.minusMonths(1), end = month.plusMonths(1))
    override val quarter: InstantRange get() = range(start = month.minusMonths(2), end = month.plusMonths(1))

    override val changes: Flow<Unit>
        get() = flow {
            while (currentCoroutineContext().isActive) {
                emit(today to zone)
                delay(CALENDAR_REFRESH)
            }
        }.distinctUntilChanged().map { }

    private fun range(start: LocalDate, end: LocalDate) = InstantRange(
        start = start.atStartOfDay(zone).toInstant(),
        endExclusive = end.atStartOfDay(zone).toInstant(),
    )

    private companion object {
        val CALENDAR_REFRESH = 1.minutes
    }
}
