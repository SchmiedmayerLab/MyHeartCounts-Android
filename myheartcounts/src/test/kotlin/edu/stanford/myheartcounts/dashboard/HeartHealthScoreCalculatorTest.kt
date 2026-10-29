//
// This source file is part of the My Heart Counts Android open-source project
//
// SPDX-FileCopyrightText: 2026 Stanford University and the project authors (see CONTRIBUTORS.md)
//
// SPDX-License-Identifier: MIT

package edu.stanford.myheartcounts.dashboard

import com.google.common.truth.Truth.assertThat
import edu.stanford.myheartcounts.customsamples.CustomQuantitySampleType
import edu.stanford.myheartcounts.healthstats.HealthStatsDiagnostic
import org.grovealliance.core.time.FakeTimeProvider
import org.junit.Test
import java.time.Duration

class HeartHealthScoreCalculatorTest {
    private val fixtures = HeartHealthScoreFixtures
    private val timeProvider = FakeTimeProvider().apply { setNow(fixtures.NOW) }
    private val calculator = HeartHealthScoreCalculatorImpl(timeProvider = timeProvider)

    @Test
    fun `missing inputs produce nine empty factors and no composite`() {
        // when
        val result = calculator.calculate(input = fixtures.input())

        // then
        assertThat(result.overall).isNull()
        assertThat(result.components.map { it.component }).containsExactlyElementsIn(HeartHealthFixtures.SHOWN_COMPONENTS).inOrder()
        assertThat(result.components.mapNotNull { it.score }).isEmpty()
    }

    @Test
    fun `diet bands include their closed upper bounds and preserve fractional gaps`() {
        // when / then
        assertScores(component = HeartScoreComponent.Diet, cases = listOf(
            4.9 to 0.0, 5.0 to 0.25, 7.0 to 0.25, 8.0 to 0.5, 10.0 to 0.5,
            11.0 to 0.7, 13.0 to 0.7, 14.0 to 0.85, 16.0 to 0.85, 16.5 to 0.0,
            17.0 to 1.0, 21.0 to 1.0, 22.0 to 0.0,
        ))
    }

    @Test
    fun `exercise and step thresholds match the score bands`() {
        // when / then
        assertScores(component = HeartScoreComponent.ExerciseMinutes, cases = listOf(
            0.0 to 0.0, 1.0 to 0.2, 29.9 to 0.2, 30.0 to 0.4, 60.0 to 0.6,
            90.0 to 0.8, 120.0 to 0.9, 149.9 to 0.9, 150.0 to 1.0,
        ))
        assertScores(component = HeartScoreComponent.StepCount, cases = listOf(
            0.0 to 0.2, 1999.0 to 0.2, 2000.0 to 0.4, 4000.0 to 0.6,
            6000.0 to 0.8, 8000.0 to 0.9, 9999.0 to 0.9, 10000.0 to 1.0,
        ))
    }

    @Test
    fun `sleep rewards seven to nine hours and penalizes both extremes`() {
        // when / then
        assertScores(component = HeartScoreComponent.Sleep, cases = listOf(
            3.9 to 0.0, 4.0 to 0.2, 5.0 to 0.4, 6.0 to 0.7, 7.0 to 1.0,
            8.99 to 1.0, 9.0 to 0.9, 9.99 to 0.9, 10.0 to 0.4,
        ))
    }

    @Test
    fun `standard and Asian BMI bands differ at the specified thresholds`() {
        // when / then
        assertScores(component = HeartScoreComponent.BodyMassIndex, cases = listOf(
            15.9 to 0.4, 16.0 to 0.6, 18.5 to 1.0, 24.9 to 1.0,
            25.0 to 0.7, 30.0 to 0.5, 35.0 to 0.3, 40.0 to 0.1,
        ))
        listOf(16.0 to 0.6, 18.5 to 1.0, 23.0 to 0.75, 25.0 to 0.5, 30.0 to 0.2).forEach { (value, expected) ->
            val input = fixtures.componentInput(component = HeartScoreComponent.BodyMassIndex, value = value)
                .copy(usesAsianBodyMassIndex = true)
            assertThat(calculator.calculate(input = input).score(component = HeartScoreComponent.BodyMassIndex)).isEqualTo(expected)
        }
    }

    @Test
    fun `LDL and glucose use milligrams per deciliter bands`() {
        // when / then
        assertScores(component = HeartScoreComponent.BloodLipids, cases = listOf(
            54.9 to 1.0, 55.0 to 0.9, 70.0 to 0.8, 100.0 to 0.6,
            130.0 to 0.4, 160.0 to 0.2, 190.0 to 0.0,
        ))
        assertScores(component = HeartScoreComponent.BloodGlucose, cases = listOf(
            84.9 to 1.0, 85.0 to 0.9, 100.0 to 0.75, 110.0 to 0.5, 126.0 to 0.25, 140.0 to 0.0,
        ))
    }

    @Test
    fun `nicotine categories score in order and unknown categories are unavailable`() {
        // when / then
        assertScores(component = HeartScoreComponent.NicotineExposure, cases = listOf(
            0.0 to 1.0, 1.0 to 0.75, 2.0 to 0.5, 3.0 to 0.25, 4.0 to 0.0,
        ))
        val input = fixtures.componentInput(component = HeartScoreComponent.NicotineExposure, value = 5.0)
        assertThat(calculator.calculate(input = input).score(component = HeartScoreComponent.NicotineExposure)).isNull()
    }

    @Test
    fun `WHO5 scales by four and clamps to the score range`() {
        // when / then
        assertScores(component = HeartScoreComponent.MentalWellbeing, cases = listOf(
            -1.0 to 0.0, 0.0 to 0.0, 10.0 to 0.4, 25.0 to 1.0, 26.0 to 1.0,
        ))
    }

    @Test
    fun `blood pressure adds systolic and diastolic contributions after integer truncation`() {
        // when / then
        listOf(120.9 to 1.0, 121.0 to 0.9, 130.0 to 0.75, 140.0 to 0.5, 160.0 to 0.25).forEach { (value, expected) ->
            val input = fixtures.input().copy(bloodPressure = listOf(fixtures.pressure(systolic = value, diastolic = 80.0)))
            assertThat(calculator.calculate(input = input).score(component = HeartScoreComponent.BloodPressure)).isEqualTo(expected)
        }
        listOf(80.9 to 1.0, 81.0 to 0.9, 90.0 to 0.8, 100.0 to 0.75).forEach { (value, expected) ->
            val input = fixtures.input().copy(bloodPressure = listOf(fixtures.pressure(systolic = 120.0, diastolic = value)))
            assertThat(calculator.calculate(input = input).score(component = HeartScoreComponent.BloodPressure)).isEqualTo(expected)
        }
    }

    @Test
    fun `exercise takes priority even when its available total is zero`() {
        // given
        val input = fixtures.input().copy(
            exercise = listOf(fixtures.quantity(value = 0.0)),
            steps = fixtures.snapshot(elements = listOf(fixtures.quantity(value = 10000.0))),
        )

        // when
        val result = calculator.calculate(input = input)

        // then
        assertThat(result.components.first().component).isEqualTo(HeartScoreComponent.ExerciseMinutes)
        assertThat(result.components.first().score).isEqualTo(0.0)
    }

    @Test
    fun `steps average only days with data and round halves upward`() {
        // given
        val input = fixtures.input().copy(steps = fixtures.snapshot(elements = listOf(
            fixtures.quantity(value = 1998.0), fixtures.quantity(value = 2001.0),
        )))

        // when
        val result = calculator.calculate(input = input)

        // then
        assertThat(result.components.first().component).isEqualTo(HeartScoreComponent.StepCount)
        assertThat(result.components.first().score).isEqualTo(0.4)
        assertThat(result.components.first().measuredAt).isEqualTo(input.exerciseRange.endExclusive)
    }

    @Test
    fun `coarse step buckets cannot stand in for daily sums`() {
        // given
        val input = fixtures.input().copy(steps = fixtures.snapshot(
            elements = listOf(fixtures.quantity(value = 10000.0)),
            diagnostics = listOf(HealthStatsDiagnostic.UnalignedInterval),
        ))

        // when
        val result = calculator.calculate(input = input)

        // then
        assertThat(result.components.first().component).isEqualTo(HeartScoreComponent.StepCount)
        assertThat(result.components.first().score).isNull()
    }

    @Test
    fun `BMI prefers the newer weight calculation and uses centimeters for height`() {
        // given
        val input = fixtures.input().copy(
            bodyMassIndex = listOf(fixtures.quantity(value = 40.0, date = fixtures.NOW.minusSeconds(1))),
            weight = listOf(fixtures.quantity(value = 80.0)),
            height = listOf(fixtures.quantity(value = 200.0, date = fixtures.NOW.minus(Duration.ofDays(1000)))),
        )

        // when
        val result = calculator.calculate(input = input)

        // then
        assertThat(result.score(component = HeartScoreComponent.BodyMassIndex)).isEqualTo(1.0)
        assertThat(result.components.first { it.component == HeartScoreComponent.BodyMassIndex }.measuredAt).isEqualTo(fixtures.NOW)
    }

    @Test
    fun `newer measured BMI wins and missing or old fallback weight gives no score`() {
        // given
        val input = fixtures.input().copy(
            bodyMassIndex = listOf(fixtures.quantity(value = 30.0)),
            weight = listOf(fixtures.quantity(value = 80.0, date = fixtures.NOW.minus(Duration.ofDays(183)))),
            height = listOf(fixtures.quantity(value = 200.0)),
        )

        // when / then
        assertThat(calculator.calculate(input = input).score(component = HeartScoreComponent.BodyMassIndex)).isEqualTo(0.5)
        val fallback = calculator.calculate(input = input.copy(bodyMassIndex = emptyList()))
        assertThat(fallback.score(component = HeartScoreComponent.BodyMassIndex)).isNull()
        assertThat(calculator.calculate(input = input.copy(bodyMassIndex = emptyList(), height = emptyList())).overall).isNull()
    }

    @Test
    fun `newest sleep and custom observations supply score dates`() {
        // given
        val old = fixtures.NOW.minusSeconds(3600)
        val input = fixtures.input().copy(
            sleep = listOf(fixtures.sleep(value = 8.0), fixtures.sleep(value = 3.0, date = old)),
            custom = mapOf(CustomQuantitySampleType.BloodLipids to listOf(
                fixtures.custom(type = CustomQuantitySampleType.BloodLipids, value = 190.0, date = old),
                fixtures.custom(type = CustomQuantitySampleType.BloodLipids, value = 110.0),
            )),
        )

        // when
        val result = calculator.calculate(input = input)

        // then
        assertThat(result.score(component = HeartScoreComponent.Sleep)).isEqualTo(1.0)
        assertThat(result.score(component = HeartScoreComponent.BloodLipids)).isEqualTo(0.6)
        assertThat(result.components.first { it.component == HeartScoreComponent.Sleep }.measuredAt).isEqualTo(fixtures.NOW)
    }

    @Test
    fun `composite requires five eligible factors and excludes mental wellbeing`() {
        // given
        val input = fixtures.input().copy(
            exercise = listOf(fixtures.quantity(value = 150.0)),
            sleep = listOf(fixtures.sleep(value = 8.0)),
            bodyMassIndex = listOf(fixtures.quantity(value = 20.0)),
            glucose = fixtures.glucose(value = 80.0),
            custom = mapOf(CustomQuantitySampleType.MentalWellbeingScore to listOf(
                fixtures.custom(type = CustomQuantitySampleType.MentalWellbeingScore, value = 25.0),
            )),
        )

        // when / then
        assertThat(calculator.calculate(input = input).overall).isNull()
        val fifth = input.copy(bloodPressure = listOf(fixtures.pressure(systolic = 160.0, diastolic = 100.0)))
        assertThat(calculator.calculate(input = fifth).overall).isWithin(0.000001).of(0.8)
    }

    private fun assertScores(component: HeartScoreComponent, cases: List<Pair<Double, Double>>) {
        cases.forEach { (value, expected) ->
            val input = fixtures.componentInput(component = component, value = value)
            assertThat(calculator.calculate(input = input).score(component = component)).isEqualTo(expected)
        }
    }

    private fun HeartHealthScores.score(component: HeartScoreComponent): Double? = components.first { it.component == component }.score
}
