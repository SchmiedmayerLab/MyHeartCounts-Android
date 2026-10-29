//
// This source file is part of the My Heart Counts Android open-source project
//
// SPDX-FileCopyrightText: 2026 Stanford University and the project authors (see CONTRIBUTORS.md)
//
// SPDX-License-Identifier: MIT

package edu.stanford.myheartcounts.healthstats

import edu.stanford.myheartcounts.model.HealthUnit
import org.grovealliance.scheduler.InstantRange
import java.time.Instant

/**
 * One month of statistics for one metric, holding each source's contribution separately.
 *
 * @property version the format version; only `0` is understood
 * @property metric the identifier of the metric the entries belong to
 * @property entriesBySourceId the entries each source contributed, keyed by the source's identifier
 * @property malformedEntryCount the number of entries that could not be read and were left out
 */
data class HealthStatsDocument(
    val version: Int = 0,
    val metric: String,
    val entriesBySourceId: Map<String, List<HealthStatsEntry>>,
    val malformedEntryCount: Int = 0,
)

/**
 * A weighted mean, kept as numerator and denominator so that means of the same [weighting] can be
 * combined exactly.
 *
 * @property weighting names the averaging method; only averages of the same method combine
 */
data class HealthStatsAverage(
    val numerator: Double,
    val denominator: Double,
    val weighting: String,
) {
    /**
     * Whether the average can take part in a combination: finite parts, a positive denominator
     * and a named method.
     */
    val isValid: Boolean
        get() = numerator.isFinite() && denominator.isFinite() && denominator > 0 && weighting.isNotEmpty()
}

/**
 * A single value a source contributed to a month of statistics.
 */
sealed interface HealthStatsEntry {

    /**
     * The unit the entry's values are recorded in.
     */
    val unit: HealthUnit

    /**
     * The time the entry covers; empty for an individual observation, `null` when the entry's
     * bounds are inverted.
     */
    val timeRange: InstantRange?

    /**
     * A summary of everything recorded in an interval, such as an hour's step count or a whole
     * sleep session.
     */
    data class Aggregate(
        val start: Instant,
        val end: Instant,
        override val unit: HealthUnit,
        val values: Values,
    ) : HealthStatsEntry {
        override val timeRange: InstantRange?
            get() = if (start < end) {
                InstantRange(
                    start = start,
                    endExclusive = end,
                )
            } else {
                null
            }

        /**
         * What an [Aggregate] summarizes its interval by.
         */
        sealed interface Values {

            /**
             * The total recorded in the interval.
             */
            data class Sum(val sum: Double) : Values

            /**
             * The extremes and mean recorded in the interval, optionally with the weights the mean
             * was computed from.
             */
            data class MinMaxAvg(
                val min: Double,
                val max: Double,
                val avg: Double,
                val average: HealthStatsAverage? = null,
            ) : Values
        }
    }

    /**
     * One individual reading.
     */
    data class Quantity(
        val date: Instant,
        override val unit: HealthUnit,
        val value: Double,
    ) : HealthStatsEntry {
        override val timeRange: InstantRange
            get() = instant(date)
    }

    /**
     * One systolic and diastolic reading pair.
     */
    data class BloodPressure(
        val date: Instant,
        override val unit: HealthUnit,
        val systolic: Double,
        val diastolic: Double,
    ) : HealthStatsEntry {
        override val timeRange: InstantRange
            get() = instant(date)
    }

    /**
     * One workout, keeping its active duration apart from the time between its start and end.
     *
     * @property durationSeconds the seconds of active exercise, pauses excluded
     * @property activityType the code of the kind of workout
     */
    data class Workout(
        val id: String,
        val date: Instant,
        val endDate: Instant,
        val durationSeconds: Double,
        val activityType: Long,
    ) : HealthStatsEntry {
        override val unit: HealthUnit
            get() = HealthUnit.Second

        override val timeRange: InstantRange
            get() = instant(date)
    }

    /**
     * One completed electrocardiogram recording.
     */
    data class Electrocardiogram(
        val id: String,
        val date: Instant,
        val endDate: Instant,
    ) : HealthStatsEntry {
        override val unit: HealthUnit
            get() = HealthUnit.Count

        override val timeRange: InstantRange
            get() = instant(date)
    }
}

private fun instant(date: Instant) = InstantRange(
    start = date,
    endExclusive = date,
)
