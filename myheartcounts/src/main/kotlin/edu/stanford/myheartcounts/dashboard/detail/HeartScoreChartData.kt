//
// This source file is part of the My Heart Counts Android open-source project
//
// SPDX-FileCopyrightText: 2026 Stanford University and the project authors (see CONTRIBUTORS.md)
//
// SPDX-License-Identifier: MIT

package edu.stanford.myheartcounts.dashboard.detail

import org.grovealliance.scheduler.InstantRange

/**
 * The series and absolute calendar bounds of a component's history.
 */
data class HeartScoreChartData(
    val timeRange: InstantRange,
    val series: List<HeartScoreChartSeries>,
)

/**
 * One line or set of bars of a chart.
 */
data class HeartScoreChartSeries(
    val kind: Kind,
    val points: List<HeartScoreChartPoint>,
) {
    /**
     * What a series' values are.
     */
    enum class Kind {
        /**
         * The factor's own values.
         */
        Value,

        /**
         * The systolic readings of blood pressure.
         */
        Systolic,

        /**
         * The diastolic readings of blood pressure.
         */
        Diastolic,
    }
}

/**
 * A value over [timeRange], or at its start when the range is empty.
 */
data class HeartScoreChartPoint(
    val timeRange: InstantRange,
    val value: Double,
)
