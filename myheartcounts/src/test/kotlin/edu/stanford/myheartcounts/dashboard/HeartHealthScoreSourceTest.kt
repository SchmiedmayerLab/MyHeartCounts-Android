//
// This source file is part of the My Heart Counts Android open-source project
//
// SPDX-FileCopyrightText: 2026 Stanford University and the project authors (see CONTRIBUTORS.md)
//
// SPDX-License-Identifier: MIT

package edu.stanford.myheartcounts.dashboard

import com.google.common.truth.Truth.assertThat
import edu.stanford.myheartcounts.account.RaceEthnicityKey
import edu.stanford.myheartcounts.customsamples.CustomQuantitySampleType
import edu.stanford.myheartcounts.healthstats.HealthStatsAggregation
import edu.stanford.myheartcounts.healthstats.HealthStatsAggregationInterval
import edu.stanford.myheartcounts.healthstats.HealthStatsMetric
import edu.stanford.myheartcounts.healthstats.HealthStatsRequest
import edu.stanford.myheartcounts.model.demographics.RaceEthnicity
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.advanceTimeBy
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import org.grovealliance.account.FakeAccount
import org.grovealliance.core.time.FakeTimeProvider
import org.grovealliance.health.FakeHealth
import org.grovealliance.health.RecordType
import org.junit.Test
import java.io.IOException
import java.time.Instant
import java.time.temporal.ChronoUnit

@OptIn(ExperimentalCoroutinesApi::class)
class HeartHealthScoreSourceTest {
    private val fixtures = HeartHealthScoreFixtures
    private val account = FakeAccount()
    private val stats = FakeHeartHealthStatsStore()
    private val custom = FakeHeartHealthCustomSampleSource()
    private val health = FakeHealth()
    private val ranges = FakeHeartHealthQueryRanges()
    private val calculator = FakeHeartHealthScoreCalculator()
    private val source = HeartHealthScoreSourceImpl(
        account = account,
        stats = stats,
        customSamples = custom,
        health = health,
        calculator = calculator,
        ranges = ranges,
    )

    @Test
    fun `real calculator updates composite from stats custom observations and glucose then clears on logout`() = runTest {
        // given
        account.supplyUserDetails(details = fixtures.account())
        val realSource = HeartHealthScoreSourceImpl(
            account = account,
            stats = stats,
            customSamples = custom,
            health = health,
            calculator = HeartHealthScoreCalculatorImpl(
                timeProvider = FakeTimeProvider().apply { setNow(fixtures.NOW) },
            ),
            ranges = ranges,
        )
        val values = mutableListOf<HeartHealthScores>()
        backgroundScope.launch(context = UnconfinedTestDispatcher(testScheduler)) {
            realSource.scores.collect { values += it }
        }
        stats.setSnapshot(metricId = HealthStatsMetric.ExerciseTime.id, snapshot = fixtures.snapshot(
            elements = listOf(fixtures.quantity(value = 150.0)),
        ))
        stats.setSnapshot(metricId = HealthStatsMetric.BodyMassIndex.id, snapshot = fixtures.snapshot(
            elements = listOf(fixtures.quantity(value = 22.0)),
        ))
        custom.setSamples(type = CustomQuantitySampleType.DietScore, samples = listOf(
            fixtures.custom(type = CustomQuantitySampleType.DietScore, value = 17.0),
        ))
        custom.setSamples(type = CustomQuantitySampleType.BloodLipids, samples = listOf(
            fixtures.custom(type = CustomQuantitySampleType.BloodLipids, value = 110.0),
        ))
        custom.setSamples(type = CustomQuantitySampleType.MentalWellbeingScore, samples = listOf(
            fixtures.custom(type = CustomQuantitySampleType.MentalWellbeingScore, value = 20.0),
        ))
        runCurrent()
        assertThat(values.last().overall).isNull()
        assertThat(values.last().components.single { it.component == HeartScoreComponent.MentalWellbeing }.score)
            .isWithin(0.000001).of(0.8)

        // when
        custom.setSamples(type = CustomQuantitySampleType.NicotineExposure, samples = listOf(
            fixtures.custom(type = CustomQuantitySampleType.NicotineExposure, value = 0.0),
        ))
        runCurrent()

        // then
        assertThat(values.last().overall).isWithin(0.000001).of(0.92)

        // when
        health.setRecords(
            type = RecordType.bloodGlucose,
            records = listOf(fixtures.glucoseRecord(millimolesPerLiter = 5.0)),
        )
        advanceTimeBy(delayTimeMillis = 15000)
        runCurrent()

        // then
        assertThat(values.last().overall).isWithin(0.000001).of(5.5 / 6.0)

        // when
        custom.setSamples(type = CustomQuantitySampleType.BloodLipids, samples = emptyList())
        runCurrent()

        // then
        assertThat(values.last().overall).isWithin(0.000001).of(0.98)

        // when
        account.removeUserDetails()
        runCurrent()

        // then
        assertThat(values.last().overall).isNull()
        assertThat(values.last().components.mapNotNull { it.score }).isEmpty()
    }

    @Test
    fun `signed out scores are empty and no participant data is queried`() = runTest {
        // given
        val values = mutableListOf<HeartHealthScores>()

        // when
        backgroundScope.launch(context = UnconfinedTestDispatcher(testScheduler)) { source.scores.collect { values += it } }
        runCurrent()

        // then
        assertThat(calculator.inputs.last().exercise).isEmpty()
        assertThat(stats.requests).isEmpty()
        assertThat(custom.ranges).isEmpty()
    }

    @Test
    fun `uses the expected calendar requests and daily step aggregation`() = runTest {
        // given
        account.supplyUserDetails(details = fixtures.account())

        // when
        backgroundScope.launch(context = UnconfinedTestDispatcher(testScheduler)) { source.scores.collect() }
        runCurrent()

        // then
        val quantities = stats.requests.filterIsInstance<HealthStatsRequest.Quantity>()
        val exercise = quantities.single { it.metric == HealthStatsMetric.ExerciseTime }
        val steps = quantities.single { it.metric == HealthStatsMetric.Steps }
        val height = quantities.single { it.metric == HealthStatsMetric.Height }
        assertThat(exercise.aggregation).isEqualTo(HealthStatsAggregation.Sum)
        assertThat(exercise.timeRange.start).isEqualTo(Instant.parse("2025-12-28T00:00:00Z"))
        assertThat(exercise.timeRange.endExclusive).isEqualTo(Instant.parse("2026-01-04T00:00:00Z"))
        assertThat(steps.interval?.unit).isEqualTo(ChronoUnit.DAYS)
        assertThat(steps.interval?.alignmentPolicy).isEqualTo(HealthStatsAggregationInterval.AlignmentPolicy.Approximate)
        assertThat(height.timeRange.start).isEqualTo(Instant.MIN)
        assertThat(custom.ranges.first()?.start).isEqualTo(Instant.parse("2025-12-01T00:00:00Z"))
    }

    @Test
    fun `custom observations update scores live while another input fails`() = runTest {
        // given
        account.supplyUserDetails(details = fixtures.account())
        val values = mutableListOf<HeartHealthScores>()
        backgroundScope.launch(context = UnconfinedTestDispatcher(testScheduler)) { source.scores.collect { values += it } }
        runCurrent()

        // when
        custom.fail(type = CustomQuantitySampleType.MentalWellbeingScore)
        custom.setSamples(type = CustomQuantitySampleType.BloodLipids, samples = listOf(
            fixtures.custom(type = CustomQuantitySampleType.BloodLipids, value = 110.0),
        ))
        runCurrent()

        // then
        assertThat(calculator.inputs.last().custom[CustomQuantitySampleType.BloodLipids]?.single()?.value).isEqualTo(110.0)
        assertThat(calculator.inputs.last().custom[CustomQuantitySampleType.MentalWellbeingScore]).isEmpty()

        // when
        custom.setSamples(type = CustomQuantitySampleType.BloodLipids, samples = listOf(
            fixtures.custom(type = CustomQuantitySampleType.BloodLipids, value = 50.0),
        ))
        runCurrent()

        // then
        assertThat(calculator.inputs.last().custom[CustomQuantitySampleType.BloodLipids]?.single()?.value).isEqualTo(50.0)
    }

    @Test
    fun `stats switch steps to exercise and recover after a failed input`() = runTest {
        // given
        account.supplyUserDetails(details = fixtures.account())
        stats.setSnapshot(metricId = HealthStatsMetric.Steps.id, snapshot = fixtures.snapshot(
            elements = listOf(fixtures.quantity(value = 10000.0)),
        ))
        val values = mutableListOf<HeartHealthScores>()
        backgroundScope.launch(context = UnconfinedTestDispatcher(testScheduler)) { source.scores.collect { values += it } }
        runCurrent()
        assertThat(calculator.inputs.last().steps?.elements?.single()?.value).isEqualTo(10000.0)

        // when
        stats.fail(metricId = HealthStatsMetric.ExerciseTime.id)
        stats.setSnapshot(metricId = HealthStatsMetric.ExerciseTime.id, snapshot = fixtures.snapshot(
            elements = listOf(fixtures.quantity(value = 150.0)),
        ))
        runCurrent()

        // then
        assertThat(calculator.inputs.last().exercise.single().value).isEqualTo(150.0)
        assertThat(calculator.inputs.last().exercise.single().value).isEqualTo(150.0)
    }

    @Test
    fun `sign out clears scores and account changes emit empty scores first`() = runTest {
        // given
        account.supplyUserDetails(details = fixtures.account())
        stats.setSnapshot(metricId = HealthStatsMetric.ExerciseTime.id, snapshot = fixtures.snapshot(
            elements = listOf(fixtures.quantity(value = 150.0)),
        ))
        val values = mutableListOf<HeartHealthScores>()
        backgroundScope.launch(context = UnconfinedTestDispatcher(testScheduler)) { source.scores.collect { values += it } }
        runCurrent()
        assertThat(calculator.inputs.last().exercise.single().value).isEqualTo(150.0)

        // when
        val previousCount = calculator.inputs.size
        account.supplyUserDetails(details = fixtures.account(id = "another-participant"))
        runCurrent()

        // then
        assertThat(calculator.inputs[previousCount].exercise).isEmpty()

        // when
        account.removeUserDetails()
        runCurrent()

        // then
        assertThat(calculator.inputs.last().exercise).isEmpty()
    }

    @Test
    fun `account ethnicity selects the Asian BMI table`() = runTest {
        // given
        account.supplyUserDetails(details = fixtures.account().apply { this[RaceEthnicityKey::class] = RaceEthnicity.CHINESE })
        stats.setSnapshot(metricId = HealthStatsMetric.BodyMassIndex.id, snapshot = fixtures.snapshot(
            elements = listOf(fixtures.quantity(value = 24.0)),
        ))
        val values = mutableListOf<HeartHealthScores>()

        // when
        backgroundScope.launch(context = UnconfinedTestDispatcher(testScheduler)) { source.scores.collect { values += it } }
        runCurrent()

        // then
        assertThat(calculator.inputs.last().usesAsianBodyMassIndex).isTrue()
    }

    @Test
    fun `calendar rollover rebuilds query windows while same-day checks retain listeners`() = runTest {
        // given
        account.supplyUserDetails(details = fixtures.account())
        backgroundScope.launch(context = UnconfinedTestDispatcher(testScheduler)) { source.scores.collect() }
        runCurrent()
        val initialCount = stats.requests.size

        // when
        advanceTimeBy(delayTimeMillis = 60000)
        runCurrent()

        // then
        assertThat(stats.requests).hasSize(initialCount)

        // when
        ranges.setExerciseRange(value = org.grovealliance.scheduler.InstantRange(
            start = Instant.parse("2025-12-29T00:00:00Z"),
            endExclusive = Instant.parse("2026-01-05T00:00:00Z"),
        ))
        advanceTimeBy(delayTimeMillis = 60000)
        runCurrent()

        // then
        val exercise = stats.requests.filterIsInstance<HealthStatsRequest.Quantity>().last { it.metric == HealthStatsMetric.ExerciseTime }
        assertThat(exercise.timeRange.endExclusive).isEqualTo(Instant.parse("2026-01-05T00:00:00Z"))
        assertThat(stats.requests).hasSize(initialCount * 2)
    }

    @Test
    fun `glucose is converted from mmol per liter and refreshed after device data changes`() = runTest {
        // given
        account.supplyUserDetails(details = fixtures.account())
        health.setRecords(
            type = RecordType.bloodGlucose,
            records = listOf(fixtures.glucoseRecord(millimolesPerLiter = 5.0)),
        )
        val values = mutableListOf<HeartHealthScores>()
        backgroundScope.launch(context = UnconfinedTestDispatcher(testScheduler)) { source.scores.collect { values += it } }
        runCurrent()

        // then
        assertThat(calculator.inputs.last().glucose?.value).isWithin(0.01).of(90.0)
        assertThat(health.queries.first().start).isEqualTo(Instant.parse("2025-12-22T00:00:00Z"))
        assertThat(health.queries.first().end).isEqualTo(Instant.parse("2026-01-05T00:00:00Z"))

        // when
        health.setRecords(
            type = RecordType.bloodGlucose,
            records = listOf(fixtures.glucoseRecord(millimolesPerLiter = 8.0)),
        )
        advanceTimeBy(delayTimeMillis = 15000)
        runCurrent()

        // then
        assertThat(calculator.inputs.last().glucose?.value).isWithin(0.01).of(144.0)
    }

    @Test
    fun `explicit refresh reads a newly saved glucose value without waiting for the polling interval`() = runTest {
        // given
        account.supplyUserDetails(details = fixtures.account())
        backgroundScope.launch(context = UnconfinedTestDispatcher(testScheduler)) { source.scores.collect {} }
        runCurrent()
        health.setRecords(type = RecordType.bloodGlucose, records = listOf(fixtures.glucoseRecord(millimolesPerLiter = 5.0)))

        // when
        source.refresh()
        runCurrent()

        // then
        assertThat(calculator.inputs.last().glucose?.value).isWithin(0.01).of(90.0)
    }

    @Test
    fun `explicit refresh keeps the previous inputs until new values arrive`() = runTest {
        // given
        account.supplyUserDetails(details = fixtures.account())
        backgroundScope.launch(context = UnconfinedTestDispatcher(testScheduler)) { source.scores.collect {} }
        custom.setSamples(type = CustomQuantitySampleType.BloodLipids, samples = listOf(
            fixtures.custom(type = CustomQuantitySampleType.BloodLipids, value = 110.0),
        ))
        runCurrent()
        val before = calculator.inputs.size

        // when
        source.refresh()
        runCurrent()

        // then
        val after = calculator.inputs.drop(before)
        assertThat(after).isNotEmpty()
        assertThat(after.map { it.custom[CustomQuantitySampleType.BloodLipids]?.singleOrNull()?.value }.toSet()).containsExactly(110.0)
    }

    @Test
    fun `glucose read failures leave other scores usable and recover on the next read`() = runTest {
        // given
        account.supplyUserDetails(details = fixtures.account())
        health.setQueryFailure(error = IOException("temporarily unavailable"))
        custom.setSamples(type = CustomQuantitySampleType.BloodLipids, samples = listOf(
            fixtures.custom(type = CustomQuantitySampleType.BloodLipids, value = 110.0),
        ))
        val values = mutableListOf<HeartHealthScores>()
        backgroundScope.launch(context = UnconfinedTestDispatcher(testScheduler)) { source.scores.collect { values += it } }
        runCurrent()

        // then
        assertThat(calculator.inputs.last().glucose).isNull()
        assertThat(calculator.inputs.last().custom[CustomQuantitySampleType.BloodLipids]?.single()?.value).isEqualTo(110.0)

        // when
        health.setRecords(
            type = RecordType.bloodGlucose,
            records = listOf(fixtures.glucoseRecord(millimolesPerLiter = 5.0)),
        )
        advanceTimeBy(delayTimeMillis = 15000)
        runCurrent()

        // then
        assertThat(calculator.inputs.last().glucose?.value).isWithin(0.01).of(90.0)
    }
}
