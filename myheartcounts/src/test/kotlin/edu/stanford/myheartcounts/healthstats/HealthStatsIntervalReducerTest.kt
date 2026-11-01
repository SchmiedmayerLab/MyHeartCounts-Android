//
// This source file is part of the My Heart Counts Android open-source project
//
// SPDX-FileCopyrightText: 2026 Stanford University and the project authors (see CONTRIBUTORS.md)
//
// SPDX-License-Identifier: MIT

package edu.stanford.myheartcounts.healthstats

import com.google.common.truth.Truth.assertThat
import edu.stanford.myheartcounts.healthstats.HealthStatsFixtures.HEALTH_KIT
import edu.stanford.myheartcounts.healthstats.HealthStatsFixtures.WEARABLE
import edu.stanford.myheartcounts.healthstats.HealthStatsFixtures.bucket
import edu.stanford.myheartcounts.healthstats.HealthStatsFixtures.date
import edu.stanford.myheartcounts.healthstats.HealthStatsFixtures.document
import edu.stanford.myheartcounts.healthstats.HealthStatsFixtures.fields
import edu.stanford.myheartcounts.healthstats.HealthStatsFixtures.range
import edu.stanford.myheartcounts.healthstats.HealthStatsFixtures.sumBucket
import edu.stanford.myheartcounts.healthstats.HealthStatsFixtures.weightedBucket
import org.grovealliance.core.time.FakeTimeProvider
import org.grovealliance.scheduler.InstantRange
import org.junit.Test
import java.time.Duration
import java.time.Instant
import java.time.OffsetDateTime
import java.time.ZoneId
import java.time.ZoneOffset
import java.time.temporal.ChronoUnit
import kotlin.math.abs

class HealthStatsIntervalReducerTest {

    private val day = range(startHour = 0.0, endHour = 24.0)
    private val timeProvider = FakeTimeProvider().apply { setZone(ZoneOffset.UTC) }
    private val reducer = HealthStatsIntervalReducerImpl(timeProvider = timeProvider)
    private val processor = HealthStatsProcessorImpl(
        sourceSelector = HealthStatsSourceSelectorImpl(),
        intervalReducer = reducer,
    )

    @Test
    fun `falls back to one source and reports an approximate average without weights`() {
        // given
        val sources = mapOf(
            HEALTH_KIT to listOf(bucket(hour = 0.0, amount = 60.0), bucket(hour = 1.0, amount = 100.0)),
            WEARABLE to listOf(bucket(hour = 0.0, amount = 90.0)),
        )

        // when
        val output = heartRate(sources = sources, aggregation = HealthStatsAggregation.Avg, interval = interval(unit = ChronoUnit.DAYS))
            .getOrThrow()

        // then
        assertThat(output.elements.first().value).isEqualTo(80.0)
        assertThat(output.contributingSourceIds).containsExactly(HEALTH_KIT)
        assertThat(output.diagnostics).contains(HealthStatsDiagnostic.ApproximateAverage(timeRange = day))
        assertThat(output.diagnostics).hasSize(2)
    }

    @Test
    fun `keeps weights across sources and intervals`() {
        // given
        val sources = mapOf(
            HEALTH_KIT to listOf(
                weightedBucket(hour = 0.0, amount = 60.0, weight = 1.0),
                weightedBucket(hour = 1.0, amount = 100.0, weight = 2.0),
            ),
            WEARABLE to listOf(weightedBucket(hour = 0.0, amount = 90.0, weight = 3.0)),
        )

        // when
        val output = heartRate(
            sources = sources,
            aggregation = HealthStatsAggregation.Avg,
            sourcePolicy = HealthStatsSourcePolicy.MergeCompatible,
            interval = interval(unit = ChronoUnit.DAYS),
        ).getOrThrow()

        // then
        assertThat(abs(output.elements.first().value - 530.0 / 6)).isLessThan(1e-10)
        assertThat(output.diagnostics).isEmpty()
        assertThat(output.contributingSourceIds).containsExactly(HEALTH_KIT, WEARABLE)
    }

    @Test
    fun `keeps every weighted summary of a year of aligned hourly sources`() {
        // given
        val hours = 8_760
        val sources = mapOf(
            HEALTH_KIT to (0 until hours).map { weightedBucket(hour = it.toDouble(), amount = 60.0, weight = 1.0) },
            WEARABLE to (0 until hours).map { weightedBucket(hour = it.toDouble(), amount = 90.0, weight = 3.0) },
        )

        // when
        val output = heartRate(
            sources = sources,
            aggregation = HealthStatsAggregation.Avg,
            sourcePolicy = HealthStatsSourcePolicy.MergeCompatible,
            interval = interval(unit = ChronoUnit.HOURS),
            timeRange = range(startHour = 0.0, endHour = hours.toDouble()),
        ).getOrThrow()

        // then
        assertThat(output.elements).hasSize(hours)
        assertThat(output.elements.first().timeRange.start).isEqualTo(date(hour = 0.0))
        assertThat(output.elements.last().timeRange.endExclusive).isEqualTo(date(hour = hours.toDouble()))
        assertThat(output.elements.all { it.value == 82.5 }).isTrue()
        assertThat(output.contributingSourceIds).containsExactly(HEALTH_KIT, WEARABLE)
        assertThat(output.diagnostics).isEmpty()
    }

    @Test
    fun `lays out intervals near the requested time from a distant or future anchor`() {
        // given
        val requested = range(startHour = 500_000.0, endHour = 500_002.0)

        // when
        val fromPast = reducer.ranges(interval = interval(unit = ChronoUnit.HOURS), range = requested).getOrThrow()
        val fromFuture = reducer.ranges(
            interval = interval(unit = ChronoUnit.HOURS, anchor = date(hour = 600_000.0)),
            range = requested,
        ).getOrThrow()

        // then
        assertThat(fromPast).containsExactly(
            range(startHour = 500_000.0, endHour = 500_001.0),
            range(startHour = 500_001.0, endHour = 500_002.0),
        ).inOrder()
        assertThat(fromFuture).isEqualTo(fromPast)
    }

    @Test
    fun `groups only the available data of an open-ended request`() {
        // given
        val sources = mapOf(HEALTH_KIT to listOf(bucket(hour = 0.0, amount = 60.0)))

        // when
        val output = heartRate(
            sources = sources,
            aggregation = HealthStatsAggregation.Avg,
            interval = interval(unit = ChronoUnit.DAYS),
            timeRange = InstantRange(
                start = Instant.MIN,
                endExclusive = Instant.MAX,
            ),
        ).getOrThrow()

        // then
        assertThat(output.elements.single().timeRange).isEqualTo(day)
    }

    @Test
    fun `keeps month ends and daylight saving changes aligned`() {
        // given
        timeProvider.setZone(ZoneId.of("Europe/Berlin"))
        val january = instant(text = "2026-01-31T00:00:00+01:00")
        val february = instant(text = "2026-02-28T00:00:00+01:00")
        val march = instant(text = "2026-03-31T00:00:00+02:00")
        val transitionStart = instant(text = "2026-03-28T00:00:00+01:00")

        // when
        val monthly = reducer.ranges(
            interval = interval(unit = ChronoUnit.MONTHS, anchor = january),
            range = InstantRange(
                start = january,
                endExclusive = march,
            ),
        ).getOrThrow()
        val daily = reducer.ranges(
            interval = interval(unit = ChronoUnit.DAYS, anchor = transitionStart),
            range = InstantRange(
                start = transitionStart,
                endExclusive = march,
            ),
        ).getOrThrow()

        // then
        assertThat(monthly).containsExactly(
            InstantRange(
                start = january,
                endExclusive = february,
            ),
            InstantRange(
                start = february,
                endExclusive = march,
            ),
        ).inOrder()
        assertThat(daily.map { Duration.between(it.start, it.endExclusive).toHours() }).containsExactly(24L, 23L, 24L).inOrder()
    }

    @Test
    fun `keeps summaries ungrouped rather than inventing a finer or shifted resolution`() {
        // given
        val sources = mapOf(HEALTH_KIT to listOf(bucket(hour = 0.5, amount = 60.0)))
        val intervals = listOf(
            interval(unit = ChronoUnit.HOURS),
            interval(
                unit = ChronoUnit.MINUTES,
                length = 30,
                alignmentPolicy = HealthStatsAggregationInterval.AlignmentPolicy.Approximate,
            ),
        )

        // when
        val outputs = intervals.map {
            heartRate(sources = sources, aggregation = HealthStatsAggregation.Avg, interval = it).getOrThrow()
        }

        // then
        outputs.forEach { output ->
            assertThat(output.elements.first().timeRange).isEqualTo(range(startHour = 0.5, endHour = 1.5))
            assertThat(output.diagnostics).containsExactly(HealthStatsDiagnostic.UnalignedInterval)
        }
    }

    @Test
    fun `approximates daily and hourly totals of summaries written in a shifted time zone`() {
        // given
        val entry = HealthStatsEntryDecoderImpl().decode(
            raw = fields(
                json = """{"start":"1970-01-02T05:00:00+05:45","end":"1970-01-02T06:00:00+05:45","sum":100,"unit":"count"}""",
            ),
        )!!
        val documents = listOf(document(metricId = STEPS, sources = mapOf(HEALTH_KIT to listOf(entry))))

        // when
        val outputs = listOf(ChronoUnit.HOURS, ChronoUnit.DAYS).map { unit ->
            processor.process(
                request = HealthStatsRequest.Quantity(
                    metric = HealthStatsMetric.Steps,
                    timeRange = range(startHour = 0.0, endHour = 48.0),
                    aggregation = HealthStatsAggregation.Sum,
                    interval = interval(unit = unit, alignmentPolicy = HealthStatsAggregationInterval.AlignmentPolicy.Approximate),
                ),
                documents = documents,
            ).getOrThrow()
        }

        // then
        outputs.forEach { output ->
            assertThat(output.elements.map { it.value }).containsExactly(75.0, 25.0).inOrder()
            assertThat(output.diagnostics.filterIsInstance<HealthStatsDiagnostic.ApproximateInterval>()).hasSize(2)
        }
    }

    @Test
    fun `clips boundary totals of seven days of steps and keeps only days with data`() {
        // given
        val entries = listOf(
            sumBucket(hour = -0.5, amount = 100.0),
            sumBucket(hour = 24.0, amount = 300.0),
            sumBucket(hour = 167.5, amount = 200.0),
        )

        // when
        val output = processor.process(
            request = HealthStatsRequest.Quantity(
                metric = HealthStatsMetric.Steps,
                timeRange = range(startHour = 0.0, endHour = 168.0),
                aggregation = HealthStatsAggregation.Sum,
                interval = interval(unit = ChronoUnit.DAYS, alignmentPolicy = HealthStatsAggregationInterval.AlignmentPolicy.Approximate),
            ),
            documents = listOf(document(metricId = STEPS, sources = mapOf(HEALTH_KIT to entries))),
        ).getOrThrow()

        // then
        assertThat(output.elements.map { it.value }).containsExactly(50.0, 300.0, 100.0).inOrder()
        assertThat(output.elements.map { it.timeRange.start })
            .containsExactly(date(hour = 0.0), date(hour = 24.0), date(hour = 144.0)).inOrder()
        assertThat(output.elements.last().timeRange.endExclusive).isEqualTo(date(hour = 168.0))
        assertThat(output.elements.sumOf { it.value } / output.elements.size).isEqualTo(150.0)
    }

    @Test
    fun `fails on split summaries and partial requested time when alignment must be exact`() {
        // given
        val sources = mapOf(HEALTH_KIT to listOf(bucket(hour = 0.5, amount = 60.0)))
        val cases = listOf(day, range(startHour = 0.75, endHour = 1.5)).flatMap { timeRange ->
            listOf(
                Triple(timeRange, HealthStatsSourcePolicy.Automatic, HealthStatsAggregationInterval.AlignmentPolicy.RequireExact),
                Triple(timeRange, HealthStatsSourcePolicy.MergeCompatible, HealthStatsAggregationInterval.AlignmentPolicy.Approximate),
            )
        }

        // when
        val results = cases.map { (timeRange, sourcePolicy, alignmentPolicy) ->
            heartRate(
                sources = sources,
                aggregation = HealthStatsAggregation.Avg,
                sourcePolicy = sourcePolicy,
                interval = interval(unit = ChronoUnit.HOURS, alignmentPolicy = alignmentPolicy),
                timeRange = timeRange,
            )
        }

        // then
        results.forEach {
            assertThat(it.exceptionOrNull()).isInstanceOf(HealthStatsProcessingException.UnalignedAggregationInterval::class.java)
        }
    }

    @Test
    fun `does not invent weights for parts of summaries when approximating`() {
        // given
        val sources = mapOf(
            HEALTH_KIT to listOf(
                weightedBucket(hour = 0.0, amount = 100.0, weight = 100.0),
                weightedBucket(hour = 23.5, amount = 60.0, weight = 1.0),
            ),
        )
        val expected = mapOf(
            HealthStatsAggregation.Avg to 80.0,
            HealthStatsAggregation.Min to 60.0,
            HealthStatsAggregation.Max to 100.0,
        )

        // when
        val outputs = expected.keys.associateWith { aggregation ->
            heartRate(
                sources = sources,
                aggregation = aggregation,
                interval = interval(unit = ChronoUnit.DAYS, alignmentPolicy = HealthStatsAggregationInterval.AlignmentPolicy.Approximate),
                timeRange = range(startHour = 0.0, endHour = 48.0),
            ).getOrThrow()
        }

        // then
        outputs.forEach { (aggregation, output) ->
            assertThat(output.elements.map { it.value }).containsExactly(expected.getValue(aggregation), 60.0).inOrder()
            assertThat(output.diagnostics).contains(HealthStatsDiagnostic.ApproximateInterval(timeRange = day))
        }
    }

    private fun heartRate(
        sources: Map<String, List<HealthStatsEntry>>,
        aggregation: HealthStatsAggregation,
        interval: HealthStatsAggregationInterval,
        sourcePolicy: HealthStatsSourcePolicy = HealthStatsSourcePolicy.Automatic,
        timeRange: InstantRange = day,
    ) = processor.process(
        request = HealthStatsRequest.Quantity(
            metric = HealthStatsMetric.HeartRate,
            timeRange = timeRange,
            aggregation = aggregation,
            sourcePolicy = sourcePolicy,
            interval = interval,
        ),
        documents = listOf(document(metricId = HealthStatsMetric.HeartRate.id, sources = sources)),
    )

    private fun interval(
        unit: ChronoUnit,
        length: Long = 1,
        anchor: Instant = date(hour = 0.0),
        alignmentPolicy: HealthStatsAggregationInterval.AlignmentPolicy =
            HealthStatsAggregationInterval.AlignmentPolicy.PreserveBuckets,
    ) = HealthStatsAggregationInterval(
        length = length,
        unit = unit,
        anchor = anchor,
        alignmentPolicy = alignmentPolicy,
    )

    private fun instant(text: String): Instant = OffsetDateTime.parse(text).toInstant()

    private companion object {
        const val STEPS = "steps"
    }
}
