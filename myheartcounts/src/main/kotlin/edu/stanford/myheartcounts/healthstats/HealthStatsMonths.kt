//
// This source file is part of the My Heart Counts Android open-source project
//
// SPDX-FileCopyrightText: 2026 Stanford University and the project authors (see CONTRIBUTORS.md)
//
// SPDX-License-Identifier: MIT

package edu.stanford.myheartcounts.healthstats

import org.grovealliance.core.time.TimeProvider
import org.grovealliance.scheduler.InstantRange
import java.time.DateTimeException
import java.time.Instant
import java.time.YearMonth
import java.util.Locale

/**
 * Decides which monthly statistics documents a request reads.
 */
interface HealthStatsMonths {

    /**
     * The identifiers (`yyyy-MM`) of the first and last month to read for [range], or `null` when
     * the range holds no month.
     *
     * One month is added on either side, as a document may have been written in another time zone,
     * and nothing after the current month is read.
     */
    fun documentIds(range: InstantRange): ClosedRange<String>?
}

class HealthStatsMonthsImpl(
    private val timeProvider: TimeProvider,
) : HealthStatsMonths {

    @Suppress("detekt:ReturnCount")
    override fun documentIds(range: InstantRange): ClosedRange<String>? {
        if (range.isEmpty) return null
        val lower = monthId(date = maxOf(range.start, EARLIEST), offset = -1) ?: return null
        val upper = monthId(date = minOf(range.endExclusive, timeProvider.nowInstant()), offset = 1) ?: return null
        return (lower..upper).takeIf { lower <= upper }
    }

    private fun monthId(date: Instant, offset: Long): String? = try {
        val month = YearMonth.from(date.atZone(timeProvider.currentZone())).plusMonths(offset)
        String.format(Locale.ROOT, "%04d-%02d", month.year, month.monthValue)
    } catch (_: DateTimeException) {
        null
    }

    private companion object {
        /**
         * The earliest instant considered, standing in for a range without a start.
         */
        val EARLIEST: Instant = Instant.parse("0001-01-01T00:00:00Z")
    }
}
