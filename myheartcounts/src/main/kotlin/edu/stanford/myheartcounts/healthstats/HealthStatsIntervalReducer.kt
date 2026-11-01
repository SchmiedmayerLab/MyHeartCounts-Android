//
// This source file is part of the My Heart Counts Android open-source project
//
// SPDX-FileCopyrightText: 2026 Stanford University and the project authors (see CONTRIBUTORS.md)
//
// SPDX-License-Identifier: MIT

package edu.stanford.myheartcounts.healthstats

import org.grovealliance.core.time.TimeProvider
import org.grovealliance.scheduler.InstantRange
import java.time.DateTimeException
import java.time.Duration
import java.time.Instant
import java.time.ZoneId

/**
 * Groups a quantity's selected values into calendar intervals in the participant's time zone.
 */
interface HealthStatsIntervalReducer {

    /**
     * The intervals laid out by [interval] that together cover [range], or
     * [HealthStatsProcessingException.InvalidAggregationInterval] when they cannot be laid out.
     */
    fun ranges(interval: HealthStatsAggregationInterval, range: InstantRange): Result<List<InstantRange>>

    /**
     * [values] grouped into [interval]s and reduced by the input's aggregation, with the remarks
     * made while grouping; the values unchanged when they cannot be grouped and the interval's
     * alignment policy allows keeping them.
     */
    fun reduce(
        values: List<HealthStatsValue>,
        input: HealthStatsInput,
        interval: HealthStatsAggregationInterval,
    ): Result<HealthStatsValues>
}

class HealthStatsIntervalReducerImpl(
    private val timeProvider: TimeProvider,
) : HealthStatsIntervalReducer {

    @Suppress("detekt:ReturnCount")
    override fun ranges(interval: HealthStatsAggregationInterval, range: InstantRange): Result<List<InstantRange>> {
        val layout = Layout(
            interval = interval,
            zone = timeProvider.currentZone(),
        )
        val firstBoundary = layout.boundary(index = 1)
        if (firstBoundary == null || firstBoundary <= interval.anchor) return invalidInterval()
        var index = layout.index(containing = range.start) ?: return invalidInterval()
        var start = layout.boundary(index = index) ?: return invalidInterval()
        val ranges = mutableListOf<InstantRange>()
        while (start < range.endExclusive) {
            if (ranges.size >= MAX_INTERVALS || index == Long.MAX_VALUE) return invalidInterval()
            index++
            val end = layout.boundary(index = index)
            if (end == null || end <= start) return invalidInterval()
            ranges.add(
                InstantRange(
                    start = start,
                    endExclusive = end,
                )
            )
            start = end
        }
        return Result.success(ranges)
    }

    @Suppress("detekt:ReturnCount")
    override fun reduce(
        values: List<HealthStatsValue>,
        input: HealthStatsInput,
        interval: HealthStatsAggregationInterval,
    ): Result<HealthStatsValues> {
        if (values.isEmpty()) return Result.success(HealthStatsValues(values = emptyList(), diagnostics = emptyList()))
        val lower = maxOf(values.minOf { it.range.start }, input.timeRange.start)
        val upper = minOf(values.maxOf { it.range.endExclusive }, input.timeRange.endExclusive)
        val covered = InstantRange(
            start = lower,
            endExclusive = upper.plusNanos(1),
        )
        val ranges = ranges(interval = interval, range = covered)
            .getOrElse { return Result.failure(it) }
            .filter { it.overlaps(input.timeRange) }
        val unchanged = HealthStatsValues(values = values, diagnostics = listOf(HealthStatsDiagnostic.UnalignedInterval))
        val groups = grouped(values = values, ranges = ranges, timeRange = input.timeRange, approximating = null)
            ?: when {
                interval.alignmentPolicy == HealthStatsAggregationInterval.AlignmentPolicy.RequireExact ||
                    input.sourcePolicy == HealthStatsSourcePolicy.MergeCompatible ->
                    return Result.failure(HealthStatsProcessingException.UnalignedAggregationInterval())
                interval.alignmentPolicy != HealthStatsAggregationInterval.AlignmentPolicy.Approximate ->
                    return Result.success(unchanged)
                else -> grouped(
                    values = values,
                    ranges = ranges,
                    timeRange = input.timeRange,
                    approximating = input.aggregation,
                ) ?: return Result.success(unchanged)
            }

        val diagnostics = mutableListOf<HealthStatsDiagnostic>()
        val reduced = mutableListOf<HealthStatsValue>()
        for ((index, range) in ranges.withIndex()) {
            val contained = groups.values[index]
            if (contained.isEmpty()) continue
            if (index in groups.approximatedIndices) {
                diagnostics.add(HealthStatsDiagnostic.ApproximateInterval(timeRange = range))
            }
            val average = contained.combinedAverage()
            val amount = reducedAmount(
                values = contained,
                average = average,
                range = range,
                input = input,
                diagnostics = diagnostics,
            ).getOrElse { return Result.failure(it) }
            reduced.add(
                contained.first().copy(
                    range = range,
                    sources = contained.flatMapTo(mutableSetOf()) { it.sources },
                    average = average,
                    amount = amount,
                )
            )
        }
        return Result.success(HealthStatsValues(values = reduced, diagnostics = diagnostics))
    }

    /**
     * Assigns the chronological [values] to the chronological [ranges]. Without [approximating], a
     * value straddling a boundary or the requested time makes grouping impossible; with it, such a
     * value is split across the ranges it overlaps, unless a range is shorter than the value.
     */
    @Suppress("detekt:ReturnCount")
    private fun grouped(
        values: List<HealthStatsValue>,
        ranges: List<InstantRange>,
        timeRange: InstantRange,
        approximating: HealthStatsAggregation?,
    ): IntervalGroups? {
        val groups = IntervalGroups(values = List(ranges.size) { mutableListOf() })
        var index = 0
        for (value in values) {
            val start = maxOf(value.range.start, timeRange.start)
            val end = minOf(value.range.endExclusive, timeRange.endExclusive)
            while (index < ranges.size && start >= ranges[index].endExclusive) index++
            if (index >= ranges.size || start < ranges[index].start) return null
            val clipped = InstantRange(
                start = start,
                endExclusive = end,
            )
            if (end <= ranges[index].endExclusive && value.range == clipped) {
                groups.values[index].add(value)
                continue
            }
            val aggregation = approximating ?: return null
            var target = index
            while (target < ranges.size && ranges[target].start < end) {
                if (value.range.duration > ranges[target].duration) return null
                val bounds = InstantRange(
                    start = maxOf(start, ranges[target].start),
                    endExclusive = minOf(end, ranges[target].endExclusive),
                )
                groups.values[target].add(
                    fragment(
                        value = value,
                        range = bounds,
                        aggregation = aggregation,
                    )
                )
                groups.approximatedIndices.add(target)
                target++
            }
        }
        return groups
    }

    /**
     * The part of [value] falling into [range]: a sum in proportion to the overlap, any other value
     * whole. Neither a part's exact mean nor its weights can be recovered, so the weights are dropped.
     */
    private fun fragment(
        value: HealthStatsValue,
        range: InstantRange,
        aggregation: HealthStatsAggregation,
    ): HealthStatsValue {
        val amount = if (aggregation == HealthStatsAggregation.Sum) {
            value.amount * range.duration.toNanos().toDouble() / value.range.duration.toNanos().toDouble()
        } else {
            value.amount
        }
        return value.copy(
            range = range,
            amount = amount,
            average = null,
        )
    }

    private fun reducedAmount(
        values: List<HealthStatsValue>,
        average: HealthStatsAverage?,
        range: InstantRange,
        input: HealthStatsInput,
        diagnostics: MutableList<HealthStatsDiagnostic>,
    ): Result<Double> = when (input.aggregation) {
        HealthStatsAggregation.Sum -> Result.success(values.sumOf { it.amount })
        HealthStatsAggregation.Min -> Result.success(values.minOf { it.amount })
        HealthStatsAggregation.Max -> Result.success(values.maxOf { it.amount })
        HealthStatsAggregation.Avg -> when {
            average != null -> Result.success(average.numerator / average.denominator)
            values.size > 1 && values.any { !it.range.isEmpty } &&
                input.sourcePolicy == HealthStatsSourcePolicy.MergeCompatible -> Result.failure(
                HealthStatsProcessingException.IncompatibleSources(
                    timeRange = range,
                    reason = "Missing compatible average weights",
                )
            )
            else -> {
                if (values.size > 1 && values.any { !it.range.isEmpty }) {
                    diagnostics.add(HealthStatsDiagnostic.ApproximateAverage(timeRange = range))
                }
                Result.success(values.sumOf { it.amount } / values.size)
            }
        }
    }

    private fun invalidInterval(): Result<List<InstantRange>> =
        Result.failure(HealthStatsProcessingException.InvalidAggregationInterval())

    private val InstantRange.duration: Duration
        get() = Duration.between(start, endExclusive)

    /**
     * The values falling into each interval, and which intervals hold approximated parts.
     */
    private data class IntervalGroups(
        val values: List<MutableList<HealthStatsValue>>,
        val approximatedIndices: MutableSet<Int> = mutableSetOf(),
    )

    /**
     * The boundaries of [interval] in [zone], numbered from its anchor at index 0.
     */
    private class Layout(
        private val interval: HealthStatsAggregationInterval,
        private val zone: ZoneId,
    ) {
        /**
         * The boundary [index] intervals after the anchor; `null` when it cannot be represented.
         */
        fun boundary(index: Long): Instant? {
            if (interval.length <= 0) return null
            return try {
                val amount = Math.multiplyExact(interval.length, index)
                interval.anchor.atZone(zone).plus(amount, interval.unit).toInstant()
            } catch (_: ArithmeticException) {
                null
            } catch (_: DateTimeException) {
                null
            }
        }

        /**
         * The index of the interval holding [date], found in logarithmic time even far from the
         * anchor.
         */
        @Suppress("detekt:ReturnCount")
        fun index(containing: Instant): Long? {
            var lower = 0L
            var upper = 0L
            if (containing >= interval.anchor) {
                upper = 1
                while ((boundary(upper) ?: return null) <= containing) {
                    lower = upper
                    upper = doubled(upper) ?: return null
                }
            } else {
                lower = -1
                while ((boundary(lower) ?: return null) > containing) {
                    upper = lower
                    lower = doubled(lower) ?: return null
                }
            }
            while (upper - lower > 1) {
                val middle = lower + (upper - lower) / 2
                if ((boundary(middle) ?: return null) <= containing) lower = middle else upper = middle
            }
            return lower
        }

        private fun doubled(value: Long): Long? = try {
            Math.multiplyExact(value, 2L)
        } catch (_: ArithmeticException) {
            null
        }
    }

    private companion object {
        const val MAX_INTERVALS = 100_000
    }
}
