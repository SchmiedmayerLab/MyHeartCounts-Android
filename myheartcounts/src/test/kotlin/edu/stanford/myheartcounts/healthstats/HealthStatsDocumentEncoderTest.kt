//
// This source file is part of the My Heart Counts Android open-source project
//
// SPDX-FileCopyrightText: 2026 Stanford University and the project authors (see CONTRIBUTORS.md)
//
// SPDX-License-Identifier: MIT

package edu.stanford.myheartcounts.healthstats

import com.google.common.truth.Truth.assertThat
import edu.stanford.myheartcounts.model.HealthUnit
import org.grovealliance.core.time.FakeTimeProvider
import org.junit.Test
import java.time.Instant
import java.time.ZoneOffset

class HealthStatsDocumentEncoderTest {
    private val timeProvider = FakeTimeProvider()
    private val entryDecoder = HealthStatsEntryDecoderImpl()
    private val encoder = HealthStatsDocumentEncoderImpl(
        timeProvider = timeProvider,
        entryDecoder = entryDecoder,
    )
    private val fixtures = HealthStatsWritingFixtures

    @Test
    fun `encodes a complete source document with the shared hourly shape`() {
        // given
        val entry = HealthStatsFixtures.sumBucket(hour = 1.0, amount = 123.0)

        // when
        val data = encoder.encode(contribution = fixtures.contribution(entries = listOf(entry))).getOrThrow()

        // then
        assertThat(data["version"]).isEqualTo(0)
        assertThat(data["metric"]).isEqualTo("steps")
        assertThat(data.keys).containsExactly("version", "metric", "hourly")
        assertThat(decodedEntries(data = data)).containsExactly(entry)
    }

    @Test
    fun `preserves explicit offsets and fractional timestamps`() {
        // given
        timeProvider.setZone(ZoneOffset.ofHours(5))
        val entry = HealthStatsEntry.Quantity(
            date = Instant.parse("2026-01-01T12:00:00.123Z"),
            unit = HealthUnit.Kilogram,
            value = 70.0,
        )

        // when
        val data = encoder.encode(contribution = fixtures.contribution(entries = listOf(entry)).copy(
            metricId = "weight", collection = HealthStatsEntryCollection.Samples,
        )).getOrThrow()

        // then
        assertThat(data.toString()).contains("2026-01-01T17:00:00.123+05:00")
        assertThat(decodedEntries(data = data)).containsExactly(entry)
    }

    @Test
    fun `heart rate without exact weights omits average metadata`() {
        // given
        val entry = HealthStatsFixtures.bucket(hour = 1.0, amount = 72.0)

        // when
        val data = encoder.encode(contribution = fixtures.contribution(entries = listOf(entry)).copy(metricId = "heart-rate")).getOrThrow()

        // then
        assertThat(data.toString()).doesNotContain("numerator")
        assertThat(data.toString()).doesNotContain("weighting")
        assertThat(decodedEntries(data = data)).containsExactly(entry)
    }

    @Test
    fun `invalid numeric values fail before a document is written`() {
        // given
        val entry = HealthStatsFixtures.sumBucket(hour = 1.0, amount = Double.NaN)

        // when
        val result = encoder.encode(contribution = fixtures.contribution(entries = listOf(entry)))

        // then
        assertThat(result.isFailure).isTrue()
    }

    @Test
    fun `preserves all shared entry shapes when round tripping`() {
        // given
        val start = fixtures.NOW
        val entries = listOf(
            HealthStatsFixtures.bucket(hour = 1.0, amount = 70.0),
            HealthStatsFixtures.observation(hour = 2.0, amount = 80.0),
            HealthStatsEntry.BloodPressure(
                date = start,
                unit = HealthUnit.MillimeterOfMercury,
                systolic = 120.0,
                diastolic = 80.0,
            ),
            HealthStatsEntry.Workout(id = "healthconnect:workout", date = start, endDate = start.plusSeconds(
                3600,
            ), durationSeconds = 3300.0, activityType = 52),
            HealthStatsEntry.Electrocardiogram(
                id = "event",
                date = start,
                endDate = start.plusSeconds(30),
            ),
        )

        // when / then
        entries.forEach { entry ->
            val data = encoder.encode(contribution = fixtures.contribution(entries = listOf(entry))).getOrThrow()
            assertThat(decodedEntries(data = data)).containsExactly(entry)
        }
    }

    private fun decodedEntries(data: Map<String, Any>): List<HealthStatsEntry> {
        val sources = data.values.filterIsInstance<Map<*, *>>().single()
        val entries = sources.values.single() as List<*>
        return entries.mapNotNull { entryDecoder.decode(raw = it) }
    }
}
