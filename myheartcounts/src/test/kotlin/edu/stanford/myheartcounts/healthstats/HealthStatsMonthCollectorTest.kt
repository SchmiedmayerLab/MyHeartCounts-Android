//
// This source file is part of the My Heart Counts Android open-source project
//
// SPDX-FileCopyrightText: 2026 Stanford University and the project authors (see CONTRIBUTORS.md)
//
// SPDX-License-Identifier: MIT

package edu.stanford.myheartcounts.healthstats

import androidx.health.connect.client.records.ExerciseSessionRecord
import androidx.health.connect.client.records.HeightRecord
import androidx.health.connect.client.records.SleepSessionRecord
import androidx.health.connect.client.records.StepsRecord
import androidx.health.connect.client.records.WeightRecord
import androidx.health.connect.client.records.metadata.Metadata
import androidx.health.connect.client.units.Length
import androidx.health.connect.client.units.Mass
import com.google.common.truth.Truth.assertThat
import kotlinx.coroutines.test.runTest
import org.grovealliance.health.AggregationBucket
import org.grovealliance.health.FakeHealth
import org.grovealliance.health.RecordType
import org.junit.Test
import java.time.Duration

class HealthStatsMonthCollectorTest {
    private val fixtures = HealthStatsWritingFixtures
    private val health = FakeHealth()
    private val collector = HealthStatsMonthCollectorImpl(
        health = health,
        workoutMapper = FakeHealthStatsWorkoutMapper(),
        configuration = fixtures.configuration(),
    )

    @Test
    fun `hourly totals use the framework aggregate rather than summing duplicate raw records`() = runTest {
        // given
        health.setRecords(type = RecordType.steps, records = listOf(fixtures.steps(), fixtures.steps()))
        health.setAggregationBuckets(metric = StepsRecord.COUNT_TOTAL, buckets = listOf(AggregationBucket(
            start = fixtures.NOW.minusSeconds(3600), end = fixtures.NOW, value = 100L,
        )))

        // when
        val snapshot = collector.collect(metric = HealthStatsSyncMetric.Steps, month = fixtures.month()).getOrThrow()

        // then
        assertThat((snapshot.entries.single() as HealthStatsEntry.Aggregate).values).isEqualTo(HealthStatsEntry.Aggregate.Values.Sum(
            sum = 100.0,
        ))
    }

    @Test
    fun `exercise duration aggregates are expressed in minutes`() = runTest {
        // given
        health.setAggregationBuckets(metric = ExerciseSessionRecord.EXERCISE_DURATION_TOTAL, buckets = listOf(AggregationBucket(
            start = fixtures.NOW.minusSeconds(3600), end = fixtures.NOW, value = Duration.ofMinutes(45),
        )))

        // when
        val snapshot = collector.collect(metric = HealthStatsSyncMetric.ExerciseTime, month = fixtures.month()).getOrThrow()

        // then
        assertThat((snapshot.entries.single() as HealthStatsEntry.Aggregate).values).isEqualTo(HealthStatsEntry.Aggregate.Values.Sum(
            sum = 45.0,
        ))
    }

    @Test
    fun `BMI is derived from kilograms and the latest height in meters`() = runTest {
        // given
        health.setRecords(type = RecordType.weight, records = listOf(WeightRecord(
            time = fixtures.NOW, zoneOffset = null, weight = Mass.kilograms(80.0), metadata = Metadata.manualEntry(),
        )))
        health.setRecords(type = RecordType.height, records = listOf(HeightRecord(
            time = fixtures.NOW.minusSeconds(365 * 86400L), zoneOffset = null, height = Length.meters(
                2.0,
            ), metadata = Metadata.manualEntry(),
        )))

        // when
        val snapshot = collector.collect(metric = HealthStatsSyncMetric.BodyMassIndex, month = fixtures.month()).getOrThrow()

        // then
        assertThat((snapshot.entries.single() as HealthStatsEntry.Quantity).value).isEqualTo(20.0)
    }

    @Test
    fun `overlapping sleep records form one whole session using the deduplicated duration`() = runTest {
        // given
        val start = fixtures.NOW.minusSeconds(8 * 3600)
        health.setRecords(type = RecordType.sleepSession, records = listOf(
            SleepSessionRecord(
                startTime = start,
                startZoneOffset = null,
                endTime = fixtures.NOW,
                endZoneOffset = null,
                metadata = Metadata.manualEntry(),
            ),
            SleepSessionRecord(startTime = start.plusSeconds(
                3600,
            ), startZoneOffset = null, endTime = fixtures.NOW, endZoneOffset = null, metadata = Metadata.manualEntry()),
        ))
        health.setAggregate(metric = SleepSessionRecord.SLEEP_DURATION_TOTAL, value = Duration.ofHours(7))

        // when
        val snapshot = collector.collect(metric = HealthStatsSyncMetric.Sleep, month = fixtures.month()).getOrThrow()

        // then
        assertThat(snapshot.entries).hasSize(1)
        val session = snapshot.entries.single() as HealthStatsEntry.Aggregate
        assertThat(session.start).isEqualTo(start)
        assertThat(session.end).isEqualTo(fixtures.NOW)
        assertThat(session.values).isEqualTo(HealthStatsEntry.Aggregate.Values.Sum(sum = 7.0))
    }

    @Test
    fun `sleep records less than an hour apart form one session`() = runTest {
        // given
        val start = fixtures.NOW.minusSeconds(8 * 3600)
        health.setRecords(type = RecordType.sleepSession, records = listOf(
            SleepSessionRecord(
                startTime = start,
                startZoneOffset = null,
                endTime = start.plusSeconds(4 * 3600),
                endZoneOffset = null,
                metadata = Metadata.manualEntry(),
            ),
            SleepSessionRecord(
                startTime = start.plusSeconds(4 * 3600 + 59 * 60),
                startZoneOffset = null,
                endTime = fixtures.NOW,
                endZoneOffset = null,
                metadata = Metadata.manualEntry(),
            ),
        ))
        health.setAggregate(metric = SleepSessionRecord.SLEEP_DURATION_TOTAL, value = Duration.ofHours(7))

        // when
        val snapshot = collector.collect(metric = HealthStatsSyncMetric.Sleep, month = fixtures.month()).getOrThrow()

        // then
        assertThat(snapshot.entries).hasSize(1)
        val session = snapshot.entries.single() as HealthStatsEntry.Aggregate
        assertThat(session.start).isEqualTo(start)
        assertThat(session.end).isEqualTo(fixtures.NOW)
    }

    @Test
    fun `sleep records an hour or more apart form separate sessions`() = runTest {
        // given
        val start = fixtures.NOW.minusSeconds(9 * 3600)
        health.setRecords(type = RecordType.sleepSession, records = listOf(
            SleepSessionRecord(
                startTime = start,
                startZoneOffset = null,
                endTime = start.plusSeconds(4 * 3600),
                endZoneOffset = null,
                metadata = Metadata.manualEntry(),
            ),
            SleepSessionRecord(
                startTime = start.plusSeconds(5 * 3600),
                startZoneOffset = null,
                endTime = fixtures.NOW,
                endZoneOffset = null,
                metadata = Metadata.manualEntry(),
            ),
        ))
        health.setAggregate(metric = SleepSessionRecord.SLEEP_DURATION_TOTAL, value = Duration.ofHours(4))

        // when
        val snapshot = collector.collect(metric = HealthStatsSyncMetric.Sleep, month = fixtures.month()).getOrThrow()

        // then
        assertThat(snapshot.entries).hasSize(2)
    }

    @Test
    fun `a sleep session crossing month boundary belongs to its midpoint month`() = runTest {
        // given
        val boundary = fixtures.month().timeRange.start
        health.setRecords(type = RecordType.sleepSession, records = listOf(SleepSessionRecord(
            startTime = boundary.minusSeconds(3600), startZoneOffset = null,
            endTime = boundary.plusSeconds(7 * 3600), endZoneOffset = null, metadata = Metadata.manualEntry(),
        )))
        health.setAggregate(metric = SleepSessionRecord.SLEEP_DURATION_TOTAL, value = Duration.ofHours(6))

        // when
        val snapshot = collector.collect(metric = HealthStatsSyncMetric.Sleep, month = fixtures.month()).getOrThrow()

        // then
        assertThat(snapshot.entries).hasSize(1)
        assertThat((snapshot.entries.single() as HealthStatsEntry.Aggregate).start).isLessThan(boundary)
    }
}
