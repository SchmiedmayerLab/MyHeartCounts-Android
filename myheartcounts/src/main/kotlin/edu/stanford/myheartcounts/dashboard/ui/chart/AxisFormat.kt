//
// This source file is part of the My Heart Counts Android open-source project
//
// SPDX-FileCopyrightText: 2026 Stanford University and the project authors (see CONTRIBUTORS.md)
//
// SPDX-License-Identifier: MIT

package edu.stanford.myheartcounts.dashboard.ui.chart

import org.grovealliance.ui.StringResource

/**
 * The marks and labels of either numeric chart axis.
 *
 * Implementations should be data classes or objects so charts using them can be compared.
 */
interface AxisFormat {
    /**
     * Tick positions and labels for [range]. Marks outside it are ignored; colliding labels are dropped.
     */
    fun ticks(range: CartesianChart.Range): List<Tick>

    /**
     * A labeled numeric position on an axis.
     *
     * @param at the coordinate marked
     * @param label the text beside the mark
     */
    data class Tick(
        val at: Double,
        val label: StringResource,
    ) {
        init {
            require(at.isFinite()) { "tick coordinate must be finite" }
        }
    }
}
