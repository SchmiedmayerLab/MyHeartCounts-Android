//
// This source file is part of the My Heart Counts Android open-source project
//
// SPDX-FileCopyrightText: 2026 Stanford University and the project authors (see CONTRIBUTORS.md)
//
// SPDX-License-Identifier: MIT

package edu.stanford.myheartcounts.dashboard.ui.chart

import java.time.Duration
import java.time.Instant
import kotlin.math.roundToLong

/**
 * Converts instants to numeric seconds relative to an origin, independently of either chart axis.
 *
 * Use the same instance for point coordinates, axis ranges and calendar formatting. A nearby origin
 * preserves precision when plotting short intervals.
 *
 * @param origin the instant represented by zero
 */
data class TimeCoordinates(
    val origin: Instant,
) {
    /**
     * Numeric seconds from [origin] to [instant].
     */
    fun coordinate(instant: Instant): Double {
        val duration = Duration.between(origin, instant)
        return duration.seconds.toDouble() + duration.nano.toDouble() / NANOS_PER_SECOND
    }

    /**
     * The instant represented by numeric seconds [coordinate] from [origin], limited to the
     * instants from [Instant.MIN] to [Instant.MAX].
     */
    fun instant(coordinate: Double): Instant {
        require(coordinate.isFinite()) { "coordinate must be finite" }
        val seconds = coordinate.toLong()
        return when {
            seconds <= Duration.between(origin, Instant.MIN).seconds -> Instant.MIN
            seconds >= Duration.between(origin, Instant.MAX).seconds -> Instant.MAX
            else -> origin.plusSeconds(seconds).plusNanos(((coordinate - seconds) * NANOS_PER_SECOND).roundToLong())
        }
    }

    /**
     * A numeric range between two instants.
     */
    fun range(start: Instant, end: Instant): CartesianChart.Range = CartesianChart.Range(
        start = coordinate(start),
        end = coordinate(end),
    )

    /**
     * A chart point whose horizontal coordinate is a moment or a half-open interval of time.
     */
    fun point(start: Instant, end: Instant, value: Double): CartesianChart.Point {
        require(end >= start) { "end must not precede start" }
        return CartesianChart.Point(
            x = coordinate(start),
            y = value,
            xEnd = if (start == end) null else coordinate(end),
        )
    }

    private companion object {
        /**
         * The number of nanoseconds in a second.
         */
        const val NANOS_PER_SECOND = 1_000_000_000.0
    }
}
