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

class ChartPlotTest {
    private val plot = ChartPlot(
        area = Rect(left = 10f, top = 20f, right = 210f, bottom = 120f),
        xRange = CartesianChart.Range(start = -2.5, end = 2.5),
        yRange = CartesianChart.Range(start = 100.0, end = 200.0),
    )

    @Test
    fun `independent domains map their endpoints and midpoint`() {
        assertThat(plot.x(-2.5)).isEqualTo(10f)
        assertThat(plot.x(0.0)).isEqualTo(110f)
        assertThat(plot.x(2.5)).isEqualTo(210f)
        assertThat(plot.y(100.0)).isEqualTo(120f)
        assertThat(plot.y(150.0)).isEqualTo(70f)
        assertThat(plot.y(200.0)).isEqualTo(20f)
    }

    @Test
    fun `large offsets retain fractional precision before mapping to pixels`() {
        val offset = ChartPlot(
            area = plot.area,
            xRange = CartesianChart.Range(start = 1_000_000_000.0, end = 1_000_000_001.0),
            yRange = CartesianChart.Range(start = -10.0, end = -5.0),
        )
        assertThat(offset.x(1_000_000_000.25)).isEqualTo(60f)
        assertThat(offset.y(-7.5)).isEqualTo(70f)
    }
}
