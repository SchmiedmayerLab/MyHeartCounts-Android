//
// This source file is part of the My Heart Counts Android open-source project
//
// SPDX-FileCopyrightText: 2026 Stanford University and the project authors (see CONTRIBUTORS.md)
//
// SPDX-License-Identifier: MIT

package edu.stanford.myheartcounts.dashboard

import com.google.common.truth.Truth.assertThat
import org.grovealliance.core.time.FakeTimeProvider
import org.junit.Test
import java.time.Instant
import java.time.ZoneId

class HeartHealthQueryRangesTest {
    private val timeProvider = FakeTimeProvider().apply {
        setNow(Instant.parse("2026-01-04T12:00:00Z"))
    }

    private val zone = ZoneId.systemDefault()
    private val today = timeProvider.nowInstant().atZone(zone).toLocalDate()

    @Test
    fun `exercise uses the seven completed local days ending at today's midnight`() {
        // when
        val ranges = HeartHealthQueryRangesImpl(timeProvider = timeProvider)

        // then
        assertThat(ranges.exercise.start).isEqualTo(today.minusDays(7).atStartOfDay(zone).toInstant())
        assertThat(ranges.exercise.endExclusive).isEqualTo(today.atStartOfDay(zone).toInstant())
    }

    @Test
    fun `fortnight includes today in the system time zone`() {
        // when
        val ranges = HeartHealthQueryRangesImpl(timeProvider = timeProvider)

        // then
        assertThat(ranges.fortnight.start).isEqualTo(today.minusDays(13).atStartOfDay(zone).toInstant())
        assertThat(ranges.fortnight.endExclusive).isEqualTo(today.plusDays(1).atStartOfDay(zone).toInstant())
    }

    @Test
    fun `month windows include the entire current calendar month`() {
        // when
        val ranges = HeartHealthQueryRangesImpl(timeProvider = timeProvider)

        // then
        assertThat(ranges.custom.start).isEqualTo(today.withDayOfMonth(1).minusMonths(1).atStartOfDay(zone).toInstant())
        assertThat(ranges.quarter.start).isEqualTo(today.withDayOfMonth(1).minusMonths(2).atStartOfDay(zone).toInstant())
        assertThat(ranges.custom.endExclusive).isEqualTo(today.withDayOfMonth(1).plusMonths(1).atStartOfDay(zone).toInstant())
        assertThat(ranges.quarter.endExclusive).isEqualTo(ranges.custom.endExclusive)
    }

    @Test
    fun `existing ranges use the current date on every read`() {
        // given
        val ranges = HeartHealthQueryRangesImpl(timeProvider = timeProvider)
        val before = ranges.exercise.endExclusive

        // when
        timeProvider.setNow(Instant.parse("2026-01-05T12:00:00Z"))
        val updatedDay = timeProvider.nowInstant().atZone(zone).toLocalDate()

        // then
        assertThat(ranges.exercise.endExclusive).isEqualTo(updatedDay.atStartOfDay(zone).toInstant())
        assertThat(ranges.exercise.endExclusive).isNotEqualTo(before)
        assertThat(ranges.custom.start).isEqualTo(updatedDay.withDayOfMonth(1).minusMonths(1).atStartOfDay(zone).toInstant())
        assertThat(ranges.quarter.start).isEqualTo(updatedDay.withDayOfMonth(1).minusMonths(2).atStartOfDay(zone).toInstant())
        assertThat(ranges.fortnight.endExclusive).isEqualTo(updatedDay.plusDays(1).atStartOfDay(zone).toInstant())
    }
}
