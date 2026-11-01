//
// This source file is part of the My Heart Counts Android open-source project
//
// SPDX-FileCopyrightText: 2026 Stanford University and the project authors (see CONTRIBUTORS.md)
//
// SPDX-License-Identifier: MIT

package edu.stanford.myheartcounts.dashboard.detail

import com.google.common.truth.Truth.assertThat
import edu.stanford.myheartcounts.customsamples.CustomQuantitySampleType
import edu.stanford.myheartcounts.dashboard.FakeHeartHealthCustomSampleSource
import edu.stanford.myheartcounts.dashboard.FakeHeartHealthStatsStore
import edu.stanford.myheartcounts.dashboard.HeartHealthScoreFixtures
import edu.stanford.myheartcounts.dashboard.HeartScoreComponent
import edu.stanford.myheartcounts.healthstats.HealthStatsAggregation
import edu.stanford.myheartcounts.healthstats.HealthStatsRequest
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import org.grovealliance.account.FakeAccount
import org.grovealliance.core.time.FakeTimeProvider
import org.grovealliance.health.FakeHealth
import org.grovealliance.health.RecordType
import org.junit.Test
import java.time.Duration
import java.time.ZoneOffset

@OptIn(ExperimentalCoroutinesApi::class)
class HeartScoreChartSourceTest {
    private val account = FakeAccount()
    private val stats = FakeHeartHealthStatsStore()
    private val custom = FakeHeartHealthCustomSampleSource()
    private val health = FakeHealth()
    private val ranges = FakeHeartScoreDetailRanges()
    private val time = FakeTimeProvider().apply { setZone(ZoneOffset.UTC) }
    private val source = HeartScoreChartSourceImpl(
        stats = stats,
        customSamples = custom,
        health = health,
        account = account,
        ranges = ranges,
        timeProvider = time,
    )

    @Test
    fun `signed out queries no participant data`() = runTest {
        // when
        val data = source.updates(component = HeartScoreComponent.StepCount, selection = HeartScoreChartRange.Week).first().getOrThrow()

        // then
        assertThat(data.series.single().points).isEmpty()
        assertThat(stats.requests).isEmpty()
    }

    @Test
    fun `steps use daily sums even for a year`() = runTest {
        // given
        signIn()

        // when
        source.updates(component = HeartScoreComponent.StepCount, selection = HeartScoreChartRange.Year).first()

        // then
        val request = stats.requests.single() as HealthStatsRequest.Quantity
        assertThat(request.interval?.length).isEqualTo(1L)
        assertThat(request.aggregation).isEqualTo(HealthStatsAggregation.Sum)
    }

    @Test
    fun `long exercise history uses weekly sums`() = runTest {
        // given
        signIn()
        val range = HeartScoreDetailFixtures.RANGE
        ranges.setRange(range = range.copy(start = range.start.minus(Duration.ofDays(100))))

        // when
        source.updates(component = HeartScoreComponent.ExerciseMinutes, selection = HeartScoreChartRange.Year).first()

        // then
        val request = stats.requests.single() as HealthStatsRequest.Quantity
        assertThat(request.interval?.length).isEqualTo(7L)
        assertThat(request.aggregation).isEqualTo(HealthStatsAggregation.Sum)
    }

    @Test
    fun `pressure keeps separate systolic and diastolic series`() = runTest {
        // given
        signIn()
        stats.setSnapshot(metricId = "blood-pressure", snapshot = HeartHealthScoreFixtures.snapshot(
            elements = listOf(HeartHealthScoreFixtures.pressure(systolic = 122.0, diastolic = 82.0)),
        ))

        // when
        val data = source.updates(component = HeartScoreComponent.BloodPressure, selection = HeartScoreChartRange.Fortnight)
            .first().getOrThrow()

        // then
        assertThat(data.series.map { it.kind }).containsExactly(HeartScoreChartSeries.Kind.Systolic, HeartScoreChartSeries.Kind.Diastolic)
        assertThat(data.series.map { it.points.single().value }).containsExactly(122.0, 82.0).inOrder()
    }

    @Test
    fun `sleep retains hours and uses a full calendar day bar`() = runTest {
        // given
        signIn()
        stats.setSnapshot(metricId = "sleep", snapshot = HeartHealthScoreFixtures.snapshot(
            elements = listOf(HeartHealthScoreFixtures.sleep(value = 8.5)),
        ))

        // when
        val point = source.updates(component = HeartScoreComponent.Sleep, selection = HeartScoreChartRange.Fortnight)
            .first().getOrThrow().series.single().points.single()

        // then
        assertThat(point.value).isEqualTo(8.5)
        assertThat(point.timeRange.start.atZone(ZoneOffset.UTC).hour).isEqualTo(0)
        assertThat(point.timeRange.endExclusive.epochSecond - point.timeRange.start.epochSecond).isEqualTo(86400L)
    }

    @Test
    fun `sleep adds sessions ending on the same calendar day`() = runTest {
        // given
        signIn()
        stats.setSnapshot(metricId = "sleep", snapshot = HeartHealthScoreFixtures.snapshot(
            elements = listOf(HeartHealthScoreFixtures.sleep(value = 7.0), HeartHealthScoreFixtures.sleep(value = 1.5)),
        ))

        // when
        val points = source.updates(component = HeartScoreComponent.Sleep, selection = HeartScoreChartRange.Week)
            .first().getOrThrow().series.single().points

        // then
        assertThat(points.single().value).isEqualTo(8.5)
    }

    @Test
    fun `WHO5 charts preserve individual raw readings in the same day`() = runTest {
        // given
        signIn()
        custom.setSamples(type = CustomQuantitySampleType.MentalWellbeingScore, samples = listOf(
            HeartHealthScoreFixtures.custom(type = CustomQuantitySampleType.MentalWellbeingScore, value = 10.0),
            HeartHealthScoreFixtures.custom(type = CustomQuantitySampleType.MentalWellbeingScore, value = 20.0),
        ))

        // when
        val data = source.updates(component = HeartScoreComponent.MentalWellbeing, selection = HeartScoreChartRange.Fortnight)
            .first().getOrThrow()

        // then
        assertThat(data.series.single().points.map { it.value }).containsExactly(10.0, 20.0)
    }

    @Test
    fun `glucose reads the native type and converts to mg per dL`() = runTest {
        // given
        signIn()
        health.setRecords(
            type = RecordType.bloodGlucose,
            records = listOf(HeartHealthScoreFixtures.glucoseRecord(millimolesPerLiter = 5.0)),
        )

        // when
        val data = source.updates(component = HeartScoreComponent.BloodGlucose, selection = HeartScoreChartRange.Fortnight)
            .first().getOrThrow()

        // then
        assertThat(data.series.single().points.single().value).isWithin(0.01).of(90.0)
    }

    @Test
    fun `read failures are surfaced for the chart retry state`() = runTest {
        // given
        signIn()
        stats.fail(metricId = "bmi")

        // when
        val value = source.updates(component = HeartScoreComponent.BodyMassIndex, selection = HeartScoreChartRange.Fortnight).first()

        // then
        assertThat(value.isFailure).isTrue()
    }

    @Test
    fun `calendar changes restart absolute month queries`() = runTest {
        // given
        signIn()
        backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) {
            source.updates(component = HeartScoreComponent.StepCount, selection = HeartScoreChartRange.Fortnight).collect {}
        }
        runCurrent()

        // when
        val range = HeartScoreDetailFixtures.RANGE
        val extended = range.copy(endExclusive = range.endExclusive.plus(Duration.ofDays(1)))
        ranges.setRange(range = extended)
        ranges.refresh()
        runCurrent()

        // then
        assertThat(stats.requests.last().timeRange).isEqualTo(extended)
    }

    private fun signIn() { account.supplyUserDetails(details = HeartHealthScoreFixtures.account()) }
}
