//
// This source file is part of the My Heart Counts Android open-source project
//
// SPDX-FileCopyrightText: 2026 Stanford University and the project authors (see CONTRIBUTORS.md)
//
// SPDX-License-Identifier: MIT

package edu.stanford.myheartcounts.dashboard.ui.chart

import androidx.compose.ui.unit.dp
import com.google.common.truth.Truth.assertThat
import org.grovealliance.ui.theme.ColorToken
import org.grovealliance.ui.theme.SizeToken
import org.grovealliance.ui.theme.SpacingToken
import org.junit.Assert.assertThrows
import org.junit.Test

class CartesianChartConfigTest {
    @Test
    fun `configuration is a value object that can be copied without changing the base`() {
        val base = CartesianChartConfig()
        val changed = base.copy(gridColor = ColorToken.Primary, highlight = base.highlight.copy(lineAlpha = 1f))
        assertThat(base).isEqualTo(CartesianChartConfig())
        assertThat(changed.gridColor).isEqualTo(ColorToken.Primary)
        assertThat(changed.highlight.lineAlpha).isEqualTo(1f)
    }

    @Test
    fun `plot border is shown by default and can be disabled`() {
        val config = CartesianChartConfig()
        assertThat(config.showBorder).isTrue()
        assertThat(config.copy(showBorder = false).showBorder).isFalse()
    }

    @Test
    fun `invalid mark sizes and fractions are rejected`() {
        assertThrows(IllegalArgumentException::class.java) { CartesianChartConfig.Drawing(pointRadius = (-1).dp) }
        assertThrows(IllegalArgumentException::class.java) { CartesianChartConfig.Drawing(minimumBarWidth = 0.dp) }
        assertThrows(IllegalArgumentException::class.java) { CartesianChartConfig.Drawing(barWidthFraction = Float.NaN) }
        assertThrows(IllegalArgumentException::class.java) { CartesianChartConfig.Drawing(barWidthFraction = 0f) }
        assertThrows(IllegalArgumentException::class.java) { CartesianChartConfig.Drawing(barWidthFraction = 2f) }
        assertThrows(IllegalArgumentException::class.java) { CartesianChartConfig.Drawing(xGridDashLength = 0.dp) }
    }

    @Test
    fun `invalid highlight alpha and dot sizes are rejected`() {
        assertThrows(IllegalArgumentException::class.java) { CartesianChartConfig.Highlight(lineAlpha = -1f) }
        assertThrows(IllegalArgumentException::class.java) { CartesianChartConfig.Highlight(lineAlpha = Float.NaN) }
        assertThrows(IllegalArgumentException::class.java) { CartesianChartConfig.Highlight(lineAlpha = 2f) }
        assertThrows(IllegalArgumentException::class.java) { CartesianChartConfig.Highlight(dotRadius = (-1).dp) }
    }

    @Test
    fun `invalid custom theme dimensions are rejected before drawing`() {
        assertThrows(IllegalArgumentException::class.java) {
            CartesianChartConfig.Drawing(gridWidth = SizeToken.Custom(size = 0.dp))
        }
        assertThrows(IllegalArgumentException::class.java) {
            CartesianChartConfig.Drawing(lineWidth = SizeToken.Custom(size = (-1).dp))
        }
        assertThrows(IllegalArgumentException::class.java) {
            CartesianChartConfig.Drawing(labelSpacing = SpacingToken.Custom(spacing = (-1).dp))
        }
        assertThrows(IllegalArgumentException::class.java) {
            CartesianChartConfig.Highlight(padding = SpacingToken.Custom(spacing = (-1).dp))
        }
    }

    @Test
    fun `solid grids and hidden dots are valid configurations`() {
        val drawing = CartesianChartConfig.Drawing(xGridDashLength = null, pointRadius = 0.dp)
        assertThat(drawing.xGridDashLength).isNull()
        assertThat(drawing.pointRadius).isEqualTo(0.dp)
    }
}
