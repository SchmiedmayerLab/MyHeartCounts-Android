//
// This source file is part of the My Heart Counts Android open-source project
//
// SPDX-FileCopyrightText: 2026 Stanford University and the project authors (see CONTRIBUTORS.md)
//
// SPDX-License-Identifier: MIT

package edu.stanford.myheartcounts.healthstats

import edu.stanford.myheartcounts.model.HealthUnit
import org.grovealliance.scheduler.InstantRange

/**
 * What a request asks of a month's entries.
 */
data class HealthStatsInput(
    val metricId: String,
    val timeRange: InstantRange,
    val sourcePolicy: HealthStatsSourcePolicy,
    val unit: HealthUnit,
    val aggregation: HealthStatsAggregation,
)

/**
 * One entry's contribution to a result, in the requested unit.
 *
 * @property secondaryAmount the diastolic value of a blood pressure reading
 * @property event the workout or recording the value stands for
 */
data class HealthStatsValue(
    val range: InstantRange,
    val amount: Double,
    val secondaryAmount: Double? = null,
    val average: HealthStatsAverage? = null,
    val sources: Set<String>,
    val event: HealthStatsEntry? = null,
)

/**
 * Values together with the remarks made while producing them.
 */
data class HealthStatsValues(
    val values: List<HealthStatsValue>,
    val diagnostics: List<HealthStatsDiagnostic>,
)

/**
 * Whether the range holds no time, standing for the single instant at its start.
 */
val InstantRange.isEmpty: Boolean
    get() = start >= endExclusive

/**
 * Whether two ranges share time; an empty range counts as the instant at its start.
 */
fun InstantRange.overlaps(other: InstantRange): Boolean = when {
    isEmpty -> if (other.isEmpty) start == other.start else start in other
    other.isEmpty -> other.start in this
    else -> start < other.endExclusive && other.start < endExclusive
}

/**
 * The exact combination of the values' averages, when all of them carry valid weights of the same
 * method.
 */
fun List<HealthStatsValue>.combinedAverage(): HealthStatsAverage? {
    val weighting = firstOrNull()?.average?.weighting ?: return null
    if (!all { it.average?.weighting == weighting }) return null
    return HealthStatsAverage(
        numerator = sumOf { it.average?.numerator ?: 0.0 },
        denominator = sumOf { it.average?.denominator ?: 0.0 },
        weighting = weighting,
    ).takeIf { it.isValid }
}
