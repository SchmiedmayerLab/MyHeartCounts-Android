//
// This source file is part of the My Heart Counts Android open-source project
//
// SPDX-FileCopyrightText: 2026 Stanford University and the project authors (see CONTRIBUTORS.md)
//
// SPDX-License-Identifier: MIT

package edu.stanford.myheartcounts.dashboard.detail

import com.google.common.truth.Truth.assertThat
import edu.stanford.myheartcounts.dashboard.HeartHealthScoreCalculatorImpl
import edu.stanford.myheartcounts.dashboard.HeartHealthScoreFixtures
import edu.stanford.myheartcounts.dashboard.HeartScoreComponent
import edu.stanford.myheartcounts.dashboard.HeartScoreMeasurement
import org.grovealliance.core.time.FakeTimeProvider
import org.junit.Test

class HeartScoreMeasurementTest {

    private val calculator = HeartHealthScoreCalculatorImpl(timeProvider = FakeTimeProvider())

    @Test
    fun `keeps the WHO-5 value as its percentage`() {
        // given
        val input = HeartHealthScoreFixtures.componentInput(component = HeartScoreComponent.MentalWellbeing, value = 20.0)

        // when
        val result = calculator.calculate(input = input).components.single { it.component == HeartScoreComponent.MentalWellbeing }

        // then
        assertThat(result.measurement).isEqualTo(HeartScoreMeasurement.Quantity(value = 80.0))
        assertThat(result.score).isEqualTo(0.8)
    }

    @Test
    fun `keeps the blood pressure reading the score is based on`() {
        // given
        val reading = HeartHealthScoreFixtures.pressure(systolic = 122.0, diastolic = 82.0)
        val input = HeartHealthScoreFixtures.input().copy(bloodPressure = listOf(reading))

        // when
        val result = calculator.calculate(input = input).components.single { it.component == HeartScoreComponent.BloodPressure }

        // then
        assertThat(result.measurement).isEqualTo(
            HeartScoreMeasurement.BloodPressure(
                systolic = 122.0,
                diastolic = 82.0,
            ),
        )
    }
}
