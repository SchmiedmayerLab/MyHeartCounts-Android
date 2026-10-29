//
// This source file is part of the My Heart Counts Android open-source project
//
// SPDX-FileCopyrightText: 2026 Stanford University and the project authors (see CONTRIBUTORS.md)
//
// SPDX-License-Identifier: MIT

package edu.stanford.myheartcounts.dashboard.ui.internal

import androidx.compose.ui.geometry.Rect
import edu.stanford.myheartcounts.dashboard.ui.chart.CartesianChart

/**
 * Maps numeric coordinates onto a chart's plot rectangle.
 *
 * Coordinates far outside the bounds are placed at most [FRACTION_LIMIT] plot sizes beyond them, so
 * every position is finite.
 *
 * @param area the drawing bounds
 * @param xRange the horizontal numeric bounds
 * @param yRange the vertical numeric bounds
 */
internal class ChartPlot(
    val area: Rect,
    private val xRange: CartesianChart.Range,
    val yRange: CartesianChart.Range,
) {
    /**
     * The horizontal position of [value].
     */
    fun x(
        value: Double,
    ): Float = area.left + fraction(value = value, range = xRange) * area.width

    /**
     * The vertical position of [value].
     */
    fun y(
        value: Double,
    ): Float = area.bottom - fraction(value = value, range = yRange) * area.height

    private fun fraction(
        value: Double,
        range: CartesianChart.Range,
    ): Float = ((value - range.start) / (range.end - range.start)).coerceIn(-FRACTION_LIMIT, FRACTION_LIMIT).toFloat()

    private companion object {
        /**
         * The most plot sizes a position is placed beyond the bounds.
         */
        const val FRACTION_LIMIT = 1_000_000.0
    }
}
