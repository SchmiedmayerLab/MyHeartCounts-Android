//
// This source file is part of the My Heart Counts Android open-source project
//
// SPDX-FileCopyrightText: 2026 Stanford University and the project authors (see CONTRIBUTORS.md)
//
// SPDX-License-Identifier: MIT

package edu.stanford.myheartcounts.dashboard.ui.chart

import edu.stanford.myheartcounts.dashboard.ui.internal.ValueScale
import org.grovealliance.ui.StringResource
import java.math.BigDecimal
import java.text.NumberFormat
import java.util.Locale

/**
 * Clean numeric ticks within the range with localized grouped labels, usable on either axis.
 *
 * @param locale the locale the numbers are written in
 */
data class NumberAxisFormat(
    val locale: Locale,
) : AxisFormat {

    override fun ticks(range: CartesianChart.Range): List<AxisFormat.Tick> {
        val scale = ValueScale.within(
            lowest = range.start,
            highest = range.end,
        )
        return scale.ticks.filter { it in range.start..range.end }.map { value ->
            AxisFormat.Tick(at = value, label = label(value = value, step = scale.step))
        }
    }

    /**
     * A localized [value] with the precision needed for ticks [step] apart.
     */
    fun label(
        value: Double,
        step: Double,
    ): StringResource {
        val decimals = BigDecimal.valueOf(step).stripTrailingZeros().scale().coerceAtLeast(0)
        val format = NumberFormat.getNumberInstance(locale).apply {
            minimumFractionDigits = decimals
            maximumFractionDigits = decimals
        }
        return StringResource(format.format(value))
    }
}
