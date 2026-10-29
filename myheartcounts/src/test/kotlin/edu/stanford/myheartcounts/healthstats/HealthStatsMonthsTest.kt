//
// This source file is part of the My Heart Counts Android open-source project
//
// SPDX-FileCopyrightText: 2026 Stanford University and the project authors (see CONTRIBUTORS.md)
//
// SPDX-License-Identifier: MIT

package edu.stanford.myheartcounts.healthstats

import com.google.common.truth.Truth.assertThat
import org.grovealliance.core.time.FakeTimeProvider
import org.grovealliance.scheduler.InstantRange
import org.junit.Test
import java.time.Instant
import java.time.ZoneId

class HealthStatsMonthsTest {

    private val timeProvider = FakeTimeProvider().apply {
        setZone(ZoneId.of("Europe/Zurich"))
        setNow(Instant.parse("2026-01-04T10:00:00Z"))
    }
    private val months = HealthStatsMonthsImpl(timeProvider = timeProvider)

    @Test
    fun `reads the months of the range and one on either side`() {
        // when
        val documentIds = months.documentIds(
            range = InstantRange(
                start = Instant.parse("2025-11-10T00:00:00Z"),
                endExclusive = Instant.parse("2025-12-05T00:00:00Z"),
            ),
        )

        // then
        assertThat(documentIds).isEqualTo("2025-10".."2026-01")
    }

    @Test
    fun `reads nothing past the month after the current one`() {
        // when
        val documentIds = months.documentIds(
            range = InstantRange(
                start = Instant.parse("2026-01-01T00:00:00Z"),
                endExclusive = Instant.parse("2027-06-01T00:00:00Z"),
            ),
        )

        // then
        assertThat(documentIds).isEqualTo("2025-12".."2026-02")
    }

    @Test
    fun `places the bounds in the participant's time zone`() {
        // when
        val documentIds = months.documentIds(
            range = InstantRange(
                start = Instant.parse("2025-10-31T23:30:00Z"),
                endExclusive = Instant.parse("2025-11-02T00:00:00Z"),
            ),
        )

        // then
        assertThat(documentIds).isEqualTo("2025-10".."2025-12")
    }

    @Test
    fun `reads all months of a range without a start`() {
        // when
        val documentIds = months.documentIds(
            range = InstantRange(
                start = Instant.MIN,
                endExclusive = Instant.MAX,
            ),
        )

        // then
        assertThat(documentIds).isEqualTo("0000-12".."2026-02")
    }

    @Test
    fun `reads nothing for an empty range`() {
        // given
        val instant = Instant.parse("2025-11-10T00:00:00Z")

        // when
        val documentIds = months.documentIds(
            range = InstantRange(
                start = instant,
                endExclusive = instant,
            ),
        )

        // then
        assertThat(documentIds).isNull()
    }
}
