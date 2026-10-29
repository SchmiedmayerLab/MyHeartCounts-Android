//
// This source file is part of the My Heart Counts Android open-source project
//
// SPDX-FileCopyrightText: 2026 Stanford University and the project authors (see CONTRIBUTORS.md)
//
// SPDX-License-Identifier: MIT

package edu.stanford.myheartcounts.dashboard.detail

import com.google.common.truth.Truth.assertThat
import edu.stanford.myheartcounts.dashboard.FakeHeartHealthQueryRanges
import org.grovealliance.core.time.FakeTimeProvider
import org.junit.Test
import java.time.Duration
import java.time.Instant
import java.time.ZoneOffset

class HeartScoreDetailRangesTest {
    private val time = FakeTimeProvider().apply { setZone(ZoneOffset.UTC); setNow(HeartScoreDetailFixtures.NOW) }
    private val changes = FakeHeartHealthQueryRanges()
    private val ranges = HeartScoreDetailRangesImpl(
        timeProvider = time,
        calendarChanges = changes,
    )

    @Test
    fun `default range includes fourteen complete calendar days through today`() {
        // when
        val range = ranges.range(selection = HeartScoreChartRange.Fortnight)

        // then
        assertThat(range).isEqualTo(HeartScoreDetailFixtures.RANGE)
    }

    @Test
    fun `month selections subtract calendar months from tomorrow`() {
        // when
        val range = ranges.range(selection = HeartScoreChartRange.Month)

        // then
        assertThat(range.start).isEqualTo(Instant.parse("2025-12-05T00:00:00Z"))
    }

    @Test
    fun `every request recalculates the day boundary`() {
        // given
        val before = ranges.range(selection = HeartScoreChartRange.Year)

        // when
        time.advanceBy(Duration.ofDays(1))
        val after = ranges.range(selection = HeartScoreChartRange.Year)

        // then
        assertThat(after.endExclusive).isEqualTo(before.endExclusive.plusSeconds(86400))
    }
}
