//
// This source file is part of the My Heart Counts Android open-source project
//
// SPDX-FileCopyrightText: 2026 Stanford University and the project authors (see CONTRIBUTORS.md)
//
// SPDX-License-Identifier: MIT

package edu.stanford.myheartcounts.dashboard.ui.internal

import com.google.common.truth.Truth.assertThat
import org.junit.Assert.assertThrows
import org.junit.Test

class ValueScaleTest {

    @Test
    fun `a few hours are marked every five`() {
        // when
        val scale = ValueScale.of(lowest = 3.5, highest = 8.75, includeZero = true)

        // then
        assertThat(scale.ticks).containsExactly(0.0, 5.0, 10.0).inOrder()
    }

    @Test
    fun `a value on a step is the top mark`() {
        // when
        val scale = ValueScale.of(lowest = 2.0, highest = 10.0, includeZero = true)

        // then
        assertThat(scale.ticks).containsExactly(0.0, 5.0, 10.0).inOrder()
    }

    @Test
    fun `heart rates are marked every hundred`() {
        // when
        val scale = ValueScale.of(lowest = 58.0, highest = 170.0, includeZero = true)

        // then
        assertThat(scale.ticks).containsExactly(0.0, 100.0, 200.0).inOrder()
    }

    @Test
    fun `values straddling a step move on to the next power of ten`() {
        // when
        val scale = ValueScale.of(lowest = 5.0, highest = 34.7, includeZero = false)

        // then
        assertThat(scale.ticks).containsExactly(0.0, 20.0, 40.0).inOrder()
    }

    @Test
    fun `step counts are marked in thousands`() {
        // when
        val scale = ValueScale.of(lowest = 1_200.0, highest = 12_000.0, includeZero = true)

        // then
        assertThat(scale.step).isEqualTo(5_000.0)
        assertThat(scale.ticks).containsExactly(0.0, 5_000.0, 10_000.0, 15_000.0).inOrder()
    }

    @Test
    fun `fractions are marked without rounding errors`() {
        // when
        val scale = ValueScale.of(lowest = 0.1, highest = 0.3, includeZero = true)

        // then
        assertThat(scale.ticks).containsExactly(0.0, 0.1, 0.2, 0.3).inOrder()
    }

    @Test
    fun `small values are marked in halves`() {
        // when
        val scale = ValueScale.of(lowest = 0.2, highest = 0.8, includeZero = true)

        // then
        assertThat(scale.ticks).containsExactly(0.0, 0.5, 1.0).inOrder()
    }

    @Test
    fun `all zero values span zero to one`() {
        // when
        val scale = ValueScale.of(lowest = 0.0, highest = 0.0, includeZero = true)

        // then
        assertThat(scale.ticks).containsExactly(0.0, 0.5, 1.0).inOrder()
    }

    @Test
    fun `equal values span zero to the value`() {
        // when
        val scale = ValueScale.of(lowest = 4.0, highest = 4.0, includeZero = true)

        // then
        assertThat(scale.min).isEqualTo(0.0)
        assertThat(scale.max).isAtLeast(4.0)
    }

    @Test
    fun `negative values are covered below zero`() {
        // when
        val scale = ValueScale.of(lowest = -3.0, highest = 7.0, includeZero = true)

        // then
        assertThat(scale.ticks).containsExactly(-5.0, 0.0, 5.0, 10.0).inOrder()
    }

    @Test
    fun `scales mark three or four values and cover the data`() {
        // given
        val highests = (1..2_000).map { it * 0.37 }

        // when / then
        highests.forEach { highest ->
            val scale = ValueScale.of(lowest = 0.0, highest = highest, includeZero = true)
            assertThat(scale.ticks.size).isIn(3..4)
            assertThat(scale.max).isAtLeast(highest)
            assertThat(scale.min).isEqualTo(0.0)
        }
    }

    @Test
    fun `fitted fractions keep their top mark`() {
        // when
        val scale = ValueScale.of(lowest = 1.24, highest = 1.71, includeZero = false)

        // then
        assertThat(scale.min).isEqualTo(1.2)
        assertThat(scale.max).isEqualTo(1.8)
        assertThat(scale.ticks).containsExactly(1.2, 1.4, 1.6, 1.8).inOrder()
    }

    @Test
    fun `fitted scales end on their last mark`() {
        // given
        val lowests = (1..2_000).map { it * 0.0137 }

        // when / then
        lowests.forEach { lowest ->
            val scale = ValueScale.of(lowest = lowest, highest = lowest * 1.7 + 0.01, includeZero = false)
            assertThat(scale.ticks.first()).isEqualTo(scale.min)
            assertThat(scale.ticks.last()).isEqualTo(scale.max)
        }
    }

    @Test
    fun `steps of tiny values are exact`() {
        // when
        val scale = ValueScale.within(lowest = 0.0, highest = 3e-5)

        // then
        assertThat(scale.step).isEqualTo(1e-5)
        assertThat(scale.ticks).containsExactly(0.0, 1e-5, 2e-5, 3e-5).inOrder()
    }

    @Test
    fun `marks within bounds lie between them`() {
        // when
        val whole = ValueScale.within(lowest = 101.0, highest = 199.0)
        val fractional = ValueScale.within(lowest = 0.11, highest = 0.19)

        // then
        assertThat(whole.ticks).containsExactly(120.0, 140.0, 160.0, 180.0).inOrder()
        assertThat(fractional.ticks).containsExactly(0.12, 0.14, 0.16, 0.18).inOrder()
    }

    @Test
    fun `scales within bounds mark two to five values between them`() {
        // given
        val bounds = (1..2_000).map { it * 0.37 to it * 0.37 + it * 0.011 + 0.05 }

        // when / then
        bounds.forEach { (lowest, highest) ->
            val ticks = ValueScale.within(lowest = lowest, highest = highest).ticks
            assertThat(ticks.size).isIn(2..5)
            assertThat(ticks.all { it in lowest..highest }).isTrue()
        }
    }

    @Test
    fun `the largest values are marked at their bounds`() {
        // when
        val scale = ValueScale.of(lowest = 0.0, highest = Double.MAX_VALUE, includeZero = true)

        // then
        assertThat(scale.ticks).containsExactly(0.0, Double.MAX_VALUE).inOrder()
    }

    @Test
    fun `equal largest values are padded within the doubles`() {
        // when
        val scale = ValueScale.of(lowest = Double.MAX_VALUE, highest = Double.MAX_VALUE, includeZero = false)

        // then
        assertThat(scale.min).isLessThan(Double.MAX_VALUE)
        assertThat(scale.max).isEqualTo(Double.MAX_VALUE)
    }

    @Test
    fun `the smallest positive value is marked`() {
        // when
        val scale = ValueScale.of(lowest = 0.0, highest = Double.MIN_VALUE, includeZero = false)

        // then
        assertThat(scale.ticks).containsExactly(0.0, Double.MIN_VALUE).inOrder()
    }

    @Test
    fun `a lowest value above the highest is rejected`() {
        // when / then
        assertThrows(IllegalArgumentException::class.java) {
            ValueScale.of(lowest = 2.0, highest = 1.0, includeZero = true)
        }
    }
}
