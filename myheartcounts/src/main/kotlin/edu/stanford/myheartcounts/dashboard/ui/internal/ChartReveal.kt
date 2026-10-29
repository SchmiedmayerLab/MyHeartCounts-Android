//
// This source file is part of the My Heart Counts Android open-source project
//
// SPDX-FileCopyrightText: 2026 Stanford University and the project authors (see CONTRIBUTORS.md)
//
// SPDX-License-Identifier: MIT

package edu.stanford.myheartcounts.dashboard.ui.internal

/**
 * How far a chart's series are drawn in, from nothing at zero to complete at one.
 *
 * @param progress the share of the animation passed, within zero to one
 * @param stagger the share of the animation by which the rightmost mark starts after the leftmost,
 * within zero to below one
 */
internal data class ChartReveal(
    val progress: Float,
    val stagger: Float,
) {

    /**
     * The opacity of axis labels and grid lines, reaching one over the first part of the animation.
     */
    val axes: Float
        get() = (progress / AXES_SHARE).coerceIn(0f, 1f)

    /**
     * The horizontal position up to which lines are revealed.
     */
    fun lineEnd(plot: ChartPlot): Float = plot.area.left + plot.area.width * progress

    /**
     * How far the mark at horizontal [position] has grown, from zero to one; marks further right
     * start later.
     */
    fun growth(
        position: Float,
        plot: ChartPlot,
    ): Float {
        val fraction = if (plot.area.width > 0f) ((position - plot.area.left) / plot.area.width).coerceIn(0f, 1f) else 0f
        return ((progress - fraction * stagger) / (1f - stagger)).coerceIn(0f, 1f)
    }

    companion object {
        /**
         * Series drawn complete.
         */
        val Complete = ChartReveal(
            progress = 1f,
            stagger = 0f,
        )

        /**
         * The share of the animation over which axis labels and grid lines fade in.
         */
        private const val AXES_SHARE = 0.3f
    }
}
