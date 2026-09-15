//
// This source file is part of the My Heart Counts Android open-source project
//
// SPDX-FileCopyrightText: 2026 Stanford University and the project authors (see CONTRIBUTORS.md)
//
// SPDX-License-Identifier: MIT

package edu.stanford.myheartcounts.standard.health

import androidx.health.connect.client.records.StepsRecord
import androidx.health.connect.client.records.metadata.Metadata
import com.google.common.truth.Truth.assertThat
import org.grovealliance.health.RecordType
import org.junit.Test
import java.time.Instant
import java.time.ZoneOffset

class HealthRetentionTest {

    private val mapper = HealthObservationMapper()
    private val now: Instant = Instant.parse("2026-01-10T12:00:00Z")
    private val cutoff: Long = now
        .minusMillis(HealthUploadSchedule.RETENTION_OFFSET_DAYS * MILLIS_PER_DAY)
        .toEpochMilli()

    @Test
    fun `it should hold a fresh sample back and release one past the retention window`() {
        val fresh = observationFor(start = now.minusSeconds(60), end = now)
        val old = observationFor(
            start = now.minusMillis(5 * MILLIS_PER_DAY),
            end = now.minusMillis(5 * MILLIS_PER_DAY).plusSeconds(60),
        )

        assertThat(fresh.effectiveEndMillis()!!).isGreaterThan(cutoff)
        assertThat(old.effectiveEndMillis()!!).isLessThan(cutoff)
    }

    @Test
    fun `it should not let an old sample in the same batch release a fresh one`() {
        // The regression this guards: staging once used one effective time for the whole batch, so
        // a single old record dragged every sample beside it — including ones recorded seconds ago —
        // past the cutoff and straight out to the backend.
        val batch = listOf(
            observationFor(
                start = now.minusMillis(5 * MILLIS_PER_DAY),
                end = now.minusMillis(5 * MILLIS_PER_DAY).plusSeconds(60),
            ),
            observationFor(start = now.minusSeconds(60), end = now),
        )

        val released = batch.filter { it.effectiveEndMillis()!! <= cutoff }

        assertThat(released).hasSize(1)
    }

    @Test
    fun `it should judge an interval by its end rather than its start`() {
        // A sleep session that began before the cutoff but ended after it is not yet old enough; the
        // participant has had no chance to delete the part recorded this morning.
        val spanning = observationFor(
            start = now.minusMillis(4 * MILLIS_PER_DAY),
            end = now.minusMillis(MILLIS_PER_DAY),
        )

        assertThat(spanning.effectiveEndMillis()!!).isGreaterThan(cutoff)
    }

    private fun observationFor(start: Instant, end: Instant) = mapper.map(
        record = StepsRecord(
            startTime = start,
            startZoneOffset = ZoneOffset.UTC,
            endTime = end,
            endZoneOffset = ZoneOffset.UTC,
            count = 100,
            metadata = Metadata.manualEntry(),
        ),
        sampleType = HealthSampleType.of(recordType = RecordType.steps),
        issuedAt = now,
    ).single()

    private companion object {
        const val MILLIS_PER_DAY = 24L * 60 * 60 * 1000
    }
}
