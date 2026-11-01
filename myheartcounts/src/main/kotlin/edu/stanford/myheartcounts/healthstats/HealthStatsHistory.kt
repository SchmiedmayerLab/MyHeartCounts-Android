//
// This source file is part of the My Heart Counts Android open-source project
//
// SPDX-FileCopyrightText: 2026 Stanford University and the project authors (see CONTRIBUTORS.md)
//
// SPDX-License-Identifier: MIT

package edu.stanford.myheartcounts.healthstats

import org.grovealliance.core.time.TimeProvider
import org.grovealliance.scheduler.InstantRange
import java.time.Instant
import java.time.YearMonth

/**
 * Selects the monthly health history to keep current.
 */
interface HealthStatsHistory {
    fun months(enrolledAt: Instant): List<HealthStatsMonth>
    fun month(containing: Instant): HealthStatsMonth
}

class HealthStatsHistoryImpl(
    private val timeProvider: TimeProvider,
) : HealthStatsHistory {
    override fun months(enrolledAt: Instant): List<HealthStatsMonth> {
        val now = timeProvider.nowZonedDateTime()
        val endOfToday = now.toLocalDate().plusDays(1).atStartOfDay(timeProvider.currentZone())
        val first = minOf(
            YearMonth.from(enrolledAt.atZone(timeProvider.currentZone())),
            YearMonth.from(endOfToday.minusMonths(HISTORY_MONTHS)),
        )
        val last = YearMonth.from(now)
        return generateSequence(seed = first) { it.plusMonths(1) }.takeWhile { it <= last }.map { month(it) }.toList()
    }

    override fun month(containing: Instant): HealthStatsMonth = month(YearMonth.from(containing.atZone(timeProvider.currentZone())))

    private fun month(month: YearMonth): HealthStatsMonth = HealthStatsMonth(
        month = month,
        timeRange = InstantRange(
            start = month.atDay(1).atStartOfDay(timeProvider.currentZone()).toInstant(),
            endExclusive = month.plusMonths(1).atDay(1).atStartOfDay(timeProvider.currentZone()).toInstant(),
        ),
    )

    private companion object {
        const val HISTORY_MONTHS = 12L
    }
}
