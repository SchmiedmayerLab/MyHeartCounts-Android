//
// This source file is part of the My Heart Counts Android open-source project
//
// SPDX-FileCopyrightText: 2026 Stanford University and the project authors (see CONTRIBUTORS.md)
//
// SPDX-License-Identifier: MIT

package edu.stanford.myheartcounts.dashboard.ui.internal

import java.math.BigDecimal
import kotlin.math.abs
import kotlin.math.ceil
import kotlin.math.floor
import kotlin.math.log10
import kotlin.math.max
import kotlin.math.min

/**
 * The span of a value axis and the values it marks, [step] apart from [min] to [max].
 *
 * @param min the lowest value of the axis, a multiple of [step] unless the axis is marked at its
 * bounds alone
 * @param max the highest value of the axis, a multiple of [step] unless the axis is marked at its
 * bounds alone
 * @param step the distance between two marked values, one, two or five times a power of ten unless
 * the axis is marked at its bounds alone
 */
internal data class ValueScale(
    val min: Double,
    val max: Double,
    val step: Double,
) {

    /**
     * The marked values, from [min] to [max].
     */
    val ticks: List<Double>
        get() {
            val count = intervals.toInt()
            val exactMin = BigDecimal.valueOf(min)
            val exactStep = BigDecimal.valueOf(step)
            return (0..count).map { index -> exactMin.add(exactStep.multiply(BigDecimal(index))).toDouble() }
        }

    /**
     * The number of steps from [min] to [max].
     */
    private val intervals: Long
        get() = Math.round((max - min) / step)

    companion object {

        /**
         * The scale covering values from [lowest] to [highest], including zero when requested.
         *
         * The step is the smallest of one, two or five times a power of ten that covers the values
         * in at most [MAX_INTERVALS] intervals. When all values are zero, the scale runs from zero to
         * one. Values whose clean bounds cannot be represented are marked at their own bounds alone.
         */
        fun of(
            lowest: Double,
            highest: Double,
            includeZero: Boolean,
        ): ValueScale {
            require(lowest <= highest) { "lowest must not be above highest, but was $lowest and $highest" }
            require(lowest.isFinite() && highest.isFinite()) { "bounds must be finite" }
            val padding = if (lowest == highest && !includeZero) max(abs(lowest) * EQUAL_VALUE_PADDING, 1.0) else 0.0
            val low = if (includeZero) min(lowest, 0.0) else (lowest - padding).coerceAtLeast(-Double.MAX_VALUE)
            val high = if (includeZero) max(highest, 0.0).takeIf { it > low } ?: 1.0 else (highest + padding).coerceAtMost(Double.MAX_VALUE)
            return fit(
                low = low,
                high = high,
                covering = true,
            )
        }

        /**
         * The scale marking only values from [lowest] to [highest], its bounds the outermost multiples
         * of its step between them.
         *
         * The step is the smallest of one, two or five times a power of ten that marks at most
         * [MAX_INTERVALS] intervals between the values; where that marks a single value, it is the
         * next finer step instead, marking at most one interval more. Values whose multiples cannot
         * be represented are marked at their own bounds alone.
         */
        fun within(
            lowest: Double,
            highest: Double,
        ): ValueScale {
            require(lowest.isFinite() && highest.isFinite()) { "bounds must be finite" }
            require(lowest < highest) { "lowest must be below highest, but was $lowest and $highest" }
            return fit(
                low = lowest,
                high = highest,
                covering = false,
            )
        }

        private fun fit(
            low: Double,
            high: Double,
            covering: Boolean,
        ): ValueScale {
            require((high - low).isFinite() && high > low) { "bounds must have a finite positive extent" }
            val exponent = floor(log10((high - low) / MAX_INTERVALS)).coerceAtLeast(SMALLEST_EXPONENT).toInt()
            val scales = generateSequence(exponent) { it + 1 }
                .flatMap { power -> NICE_FACTORS.asSequence().map { factor -> BigDecimal(factor).scaleByPowerOfTen(power) } }
                .takeWhile { step -> step.toDouble().isFinite() }
                .mapNotNull { step ->
                    scale(
                        low = low,
                        high = high,
                        step = step,
                        covering = covering,
                    )
                }
            var finer: ValueScale? = null
            for (scale in scales) {
                if (scale.intervals <= MAX_INTERVALS) return if (covering || scale.intervals > 0) scale else finer ?: scale
                finer = scale
            }
            return ValueScale(
                min = low,
                max = high,
                step = high - low,
            )
        }

        /**
         * The scale [step] apart covering, or lying within, [low] to [high]; null when its bounds
         * cannot be represented.
         */
        private fun scale(
            low: Double,
            high: Double,
            step: BigDecimal,
            covering: Boolean,
        ): ValueScale? {
            val size = step.toDouble()
            val first = if (covering) floor(low / size + TOLERANCE) else ceil(low / size - TOLERANCE)
            val last = if (covering) ceil(high / size - TOLERANCE) else floor(high / size + TOLERANCE)
            if (last - first !in 0.0..Double.MAX_VALUE) return null
            val min = BigDecimal(first).multiply(step).toDouble()
            val max = BigDecimal(last).multiply(step).toDouble()
            return ValueScale(
                min = min,
                max = max,
                step = size,
            ).takeIf { min.isFinite() && max.isFinite() }
        }

        /**
         * The most intervals a scale is divided into, so it marks at most one more value.
         */
        private const val MAX_INTERVALS = 3

        /**
         * The padding on either side of equal values when zero is not included.
         */
        private const val EQUAL_VALUE_PADDING = 0.1

        /**
         * The exponent of the finest power of ten a step is tried at, below the smallest positive
         * double.
         */
        private const val SMALLEST_EXPONENT = -324.0

        /**
         * The multiples of a power of ten a step is chosen from, from the finest.
         */
        private val NICE_FACTORS = listOf(1, 2, 5)

        /**
         * The rounding error ignored when fitting values to a step.
         */
        private const val TOLERANCE = 1e-9
    }
}
