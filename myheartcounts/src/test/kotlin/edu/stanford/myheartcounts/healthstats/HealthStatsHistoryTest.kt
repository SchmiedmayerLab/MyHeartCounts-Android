//
// This source file is part of the My Heart Counts Android open-source project
//
// SPDX-FileCopyrightText: 2026 Stanford University and the project authors (see CONTRIBUTORS.md)
//
// SPDX-License-Identifier: MIT

package edu.stanford.myheartcounts.healthstats

import com.google.common.truth.Truth.assertThat
import org.grovealliance.core.time.FakeTimeProvider
import org.junit.Test
import java.time.Instant
import java.time.YearMonth
import java.time.ZoneId

class HealthStatsHistoryTest {
    private val timeProvider = FakeTimeProvider().apply {
        setZone(ZoneId.of("UTC"))
        setNow(HealthStatsWritingFixtures.NOW)
    }
    private val history = HealthStatsHistoryImpl(timeProvider = timeProvider)

    @Test
    fun `new enrollment still maintains at least twelve months of history`() {
        // when
        val months = history.months(enrolledAt = HealthStatsWritingFixtures.NOW)

        // then
        assertThat(months.first().month).isEqualTo(YearMonth.of(2025, 1))
        assertThat(months.last().month).isEqualTo(YearMonth.of(2026, 1))
    }

    @Test
    fun `longer enrollment keeps every enrollment month`() {
        // when
        val months = history.months(enrolledAt = Instant.parse("2023-02-15T12:00:00Z"))

        // then
        assertThat(months.first().month).isEqualTo(YearMonth.of(2023, 2))
        assertThat(months.last().month).isEqualTo(YearMonth.of(2026, 1))
    }

    @Test
    fun `month bounds are local and start inclusive end exclusive`() {
        // when
        val month = history.month(containing = HealthStatsWritingFixtures.NOW)

        // then
        assertThat(month.timeRange.start).isEqualTo(Instant.parse("2026-01-01T00:00:00Z"))
        assertThat(month.timeRange.endExclusive).isEqualTo(Instant.parse("2026-02-01T00:00:00Z"))
    }
}
