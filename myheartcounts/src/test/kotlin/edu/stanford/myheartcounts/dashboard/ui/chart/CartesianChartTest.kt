//
// This source file is part of the My Heart Counts Android open-source project
//
// SPDX-FileCopyrightText: 2026 Stanford University and the project authors (see CONTRIBUTORS.md)
//
// SPDX-License-Identifier: MIT

package edu.stanford.myheartcounts.dashboard.ui.chart

import com.google.common.truth.Truth.assertThat
import org.grovealliance.ui.StringResource
import org.junit.Assert.assertThrows
import org.junit.Test
import java.time.Instant
import java.util.Locale

class CartesianChartTest {
    @Test
    fun `either axis accepts the same numeric configuration`() {
        val axis = CartesianChart.Axis(
            range = CartesianChart.Range(start = -2.0, end = 2.0),
            format = NumberAxisFormat(locale = Locale.US),
        )
        assertThat(axis.format.ticks(axis.range).map { it.at }).containsExactly(-2.0, 0.0, 2.0).inOrder()
    }

    @Test
    fun `fitted ranges can omit zero for either positive or negative values`() {
        val positive = CartesianChart.Range.fitting(values = listOf(120.0, 145.0), includeZero = false)
        val negative = CartesianChart.Range.fitting(values = listOf(-145.0, -120.0), includeZero = false)
        assertThat(positive.start).isGreaterThan(0.0)
        assertThat(positive.start).isAtMost(120.0)
        assertThat(positive.end).isAtLeast(145.0)
        assertThat(negative.end).isLessThan(0.0)
        assertThat(negative.start).isAtMost(-145.0)
        assertThat(negative.end).isAtLeast(-120.0)
    }

    @Test
    fun `fitted ranges explicitly include zero when requested`() {
        assertThat(CartesianChart.Range.fitting(values = listOf(120.0, 145.0), includeZero = true).start).isEqualTo(0.0)
    }

    @Test
    fun `equal values and empty data produce usable fitted ranges`() {
        listOf(-4.0, 0.0, 4.0).forEach { value ->
            val range = CartesianChart.Range.fitting(values = listOf(value), includeZero = false)
            assertThat(range.start).isLessThan(value)
            assertThat(range.end).isGreaterThan(value)
        }
        assertThat(CartesianChart.Range.fitting(values = emptyList(), includeZero = false))
            .isEqualTo(CartesianChart.Range(start = 0.0, end = 1.0))
    }

    @Test
    fun `invalid ranges and nonfinite coordinates are rejected`() {
        assertThrows(IllegalArgumentException::class.java) { CartesianChart.Range(start = 2.0, end = 2.0) }
        assertThrows(IllegalArgumentException::class.java) { CartesianChart.Range(start = 3.0, end = 2.0) }
        assertThrows(IllegalArgumentException::class.java) { CartesianChart.Range(start = 0.0, end = Double.POSITIVE_INFINITY) }
        assertThrows(IllegalArgumentException::class.java) { CartesianChart.Point(x = Double.NaN, y = 1.0, xEnd = null) }
        assertThrows(IllegalArgumentException::class.java) { CartesianChart.Point(x = 1.0, y = Double.NaN, xEnd = null) }
        assertThrows(IllegalArgumentException::class.java) { CartesianChart.Point(x = 1.0, y = 1.0, xEnd = 0.0) }
        assertThrows(IllegalArgumentException::class.java) { CartesianChart.Point(x = 1.0, y = 1.0, xEnd = Double.POSITIVE_INFINITY) }
    }

    @Test
    fun `number ticks stay within an explicit range that excludes zero`() {
        val range = CartesianChart.Range(start = 120.0, end = 145.0)
        val ticks = NumberAxisFormat(locale = Locale.US).ticks(range)
        assertThat(ticks.map { it.at }).containsExactly(120.0, 130.0, 140.0).inOrder()
        assertThat(ticks.first().label).isEqualTo(StringResource("120"))
    }

    @Test
    fun `fractional and negative ticks are numeric on either axis`() {
        val ticks = NumberAxisFormat(locale = Locale.US).ticks(CartesianChart.Range(start = -0.5, end = 1.0))
        assertThat(ticks.map { it.at }).containsExactly(-0.5, 0.0, 0.5, 1.0).inOrder()
        assertThat(ticks.first().label).isEqualTo(StringResource("-0.5"))
    }

    @Test
    fun `fitted ranges keep every number tick`() {
        // given
        val range = CartesianChart.Range.fitting(values = listOf(1.24, 1.71), includeZero = false)

        // when
        val ticks = NumberAxisFormat(locale = Locale.US).ticks(range)

        // then
        assertThat(ticks.map { it.at }).containsExactly(1.2, 1.4, 1.6, 1.8).inOrder()
    }

    @Test
    fun `fitted ranges cover the extremes of the doubles`() {
        // when
        val largest = CartesianChart.Range.fitting(values = listOf(Double.MAX_VALUE), includeZero = false)
        val smallest = CartesianChart.Range.fitting(values = listOf(0.0, Double.MIN_VALUE), includeZero = false)

        // then
        assertThat(largest.end).isEqualTo(Double.MAX_VALUE)
        assertThat(smallest.end).isEqualTo(Double.MIN_VALUE)
    }

    @Test
    fun `time coordinates beyond the instants are limited to them`() {
        // given
        val coordinates = TimeCoordinates(origin = Instant.parse("2026-01-01T00:00:00Z"))

        // when / then
        assertThat(coordinates.instant(1e17)).isEqualTo(Instant.MAX)
        assertThat(coordinates.instant(-1e17)).isEqualTo(Instant.MIN)
    }

    @Test
    fun `time adapter handles both sides of its origin and submillisecond intervals`() {
        val origin = Instant.parse("2026-10-25T00:00:00Z")
        val coordinates = TimeCoordinates(origin = origin)
        listOf(origin.minusNanos(250_000), origin, origin.plusNanos(250_000), origin.plusSeconds(86_400)).forEach { instant ->
            assertThat(coordinates.instant(coordinates.coordinate(instant))).isEqualTo(instant)
        }
        assertThat(coordinates.range(start = origin, end = origin.plusSeconds(10)))
            .isEqualTo(CartesianChart.Range(start = 0.0, end = 10.0))
        assertThat(coordinates.point(start = origin, end = origin, value = 7.0))
            .isEqualTo(CartesianChart.Point(x = 0.0, y = 7.0, xEnd = null))
    }
}
