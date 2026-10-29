//
// This source file is part of the My Heart Counts Android open-source project
//
// SPDX-FileCopyrightText: 2026 Stanford University and the project authors (see CONTRIBUTORS.md)
//
// SPDX-License-Identifier: MIT

package edu.stanford.myheartcounts.dashboard.ui.internal

import androidx.compose.ui.geometry.Rect
import com.google.common.truth.Truth.assertThat
import edu.stanford.myheartcounts.dashboard.ui.chart.CartesianChart
import org.junit.Test

class ChartRevealTest {
    private val plot = ChartPlot(
        area = Rect(left = 0f, top = 0f, right = 100f, bottom = 50f),
        xRange = CartesianChart.Range(start = 0.0, end = 1.0),
        yRange = CartesianChart.Range(start = 0.0, end = 1.0),
    )

    @Test
    fun `marks further right start later`() {
        // given
        val reveal = ChartReveal(progress = 0.5f, stagger = 0.5f)

        // when
        val left = reveal.growth(position = 0f, plot = plot)
        val right = reveal.growth(position = 100f, plot = plot)

        // then
        assertThat(left).isEqualTo(1f)
        assertThat(right).isEqualTo(0f)
    }

    @Test
    fun `a complete reveal draws every mark and the whole line`() {
        // when
        val reveal = ChartReveal(progress = 1f, stagger = 0.5f)

        // then
        assertThat(listOf(0f, 50f, 100f).map { reveal.growth(position = it, plot = plot) }).containsExactly(1f, 1f, 1f)
        assertThat(reveal.lineEnd(plot = plot)).isEqualTo(100f)
        assertThat(reveal.axes).isEqualTo(1f)
    }

    @Test
    fun `nothing is drawn before the reveal starts`() {
        // when
        val reveal = ChartReveal(progress = 0f, stagger = 0.5f)

        // then
        assertThat(reveal.growth(position = 0f, plot = plot)).isEqualTo(0f)
        assertThat(reveal.lineEnd(plot = plot)).isEqualTo(0f)
        assertThat(reveal.axes).isEqualTo(0f)
    }
}
