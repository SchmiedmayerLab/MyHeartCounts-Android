//
// This source file is part of the My Heart Counts Android open-source project
//
// SPDX-FileCopyrightText: 2026 Stanford University and the project authors (see CONTRIBUTORS.md)
//
// SPDX-License-Identifier: MIT

package edu.stanford.myheartcounts.task

import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import org.grovealliance.core.time.TimeProvider
import org.grovealliance.scheduler.InstantRange
import java.time.Duration
import kotlin.time.Duration.Companion.milliseconds

/**
 * The current day in the current zone, from its start up to the start of the next.
 */
fun TimeProvider.todayRange(): InstantRange {
    val zone = currentZone()
    val startOfToday = nowInstant().atZone(zone).toLocalDate().atStartOfDay(zone)
    return InstantRange(
        start = startOfToday.toInstant(),
        endExclusive = startOfToday.plusDays(1).toInstant(),
    )
}

/**
 * The current day in the current zone, re-emitted as each day ends so that a screen left open
 * overnight moves on rather than holding yesterday.
 */
fun TimeProvider.todayRanges(): Flow<InstantRange> = flow {
    while (true) {
        val today = todayRange()
        emit(today)
        val untilTomorrow = Duration.between(nowInstant(), today.endExclusive)
        delay(untilTomorrow.toMillis().coerceAtLeast(1).milliseconds)
    }
}
