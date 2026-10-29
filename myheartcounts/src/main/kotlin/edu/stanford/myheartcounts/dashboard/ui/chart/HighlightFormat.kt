//
// This source file is part of the My Heart Counts Android open-source project
//
// SPDX-FileCopyrightText: 2026 Stanford University and the project authors (see CONTRIBUTORS.md)
//
// SPDX-License-Identifier: MIT

package edu.stanford.myheartcounts.dashboard.ui.chart

import org.grovealliance.ui.StringResource

/**
 * The text shown when a point of a [CartesianChart.Series] is highlighted.
 *
 * Implementations should be data classes or objects, so charts using them can be compared.
 */
interface HighlightFormat {

    /**
     * The text describing [point].
     */
    fun label(point: CartesianChart.Point): Highlight
}

/**
 * The text describing a highlighted point.
 *
 * @param value the value of the point, with its unit
 * @param title an optional shared heading, such as an X label, date or interval
 */
data class Highlight(
    val value: StringResource,
    val title: StringResource?,
)
