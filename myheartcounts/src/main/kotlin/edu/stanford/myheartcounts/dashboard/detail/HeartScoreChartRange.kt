//
// This source file is part of the My Heart Counts Android open-source project
//
// SPDX-FileCopyrightText: 2026 Stanford University and the project authors (see CONTRIBUTORS.md)
//
// SPDX-License-Identifier: MIT

package edu.stanford.myheartcounts.dashboard.detail

import androidx.annotation.StringRes
import edu.stanford.myheartcounts.MHCStrings
import edu.stanford.myheartcounts.dashboard.HeartHealthQueryRanges
import kotlinx.coroutines.flow.Flow
import org.grovealliance.core.time.TimeProvider
import org.grovealliance.scheduler.InstantRange
import java.time.temporal.ChronoUnit

/**
 * The calendar history shown in a component chart.
 */
enum class HeartScoreChartRange(
    @StringRes val title: Int,
    val amount: Long,
    val unit: ChronoUnit,
) {
    Week(title = MHCStrings.heart_detail_last_7_days, amount = 7, unit = ChronoUnit.DAYS),
    Fortnight(title = MHCStrings.heart_detail_last_14_days, amount = 14, unit = ChronoUnit.DAYS),
    Month(title = MHCStrings.heart_detail_last_month, amount = 1, unit = ChronoUnit.MONTHS),
    Quarter(title = MHCStrings.heart_detail_last_3_months, amount = 3, unit = ChronoUnit.MONTHS),
    HalfYear(title = MHCStrings.heart_detail_last_6_months, amount = 6, unit = ChronoUnit.MONTHS),
    Year(title = MHCStrings.heart_detail_last_year, amount = 12, unit = ChronoUnit.MONTHS),
}

/**
 * Turns a chart's time range into calendar days in the participant's time zone.
 */
interface HeartScoreDetailRanges {

    /**
     * The whole days [selection] covers, up to and including today.
     */
    fun range(selection: HeartScoreChartRange): InstantRange

    /**
     * Emits whenever the current day or the time zone changes, and with it every range.
     */
    val changes: Flow<Unit>
}

class HeartScoreDetailRangesImpl(
    private val timeProvider: TimeProvider,
    private val calendarChanges: HeartHealthQueryRanges,
) : HeartScoreDetailRanges {
    override val changes: Flow<Unit> get() = calendarChanges.changes

    override fun range(selection: HeartScoreChartRange): InstantRange {
        val end = timeProvider.nowInstant().atZone(timeProvider.currentZone()).toLocalDate().plusDays(1)
        val start = end.minus(selection.amount, selection.unit)
        return InstantRange(
            start = start.atStartOfDay(timeProvider.currentZone()).toInstant(),
            endExclusive = end.atStartOfDay(timeProvider.currentZone()).toInstant(),
        )
    }
}
