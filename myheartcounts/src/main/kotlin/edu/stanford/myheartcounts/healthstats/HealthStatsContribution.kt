//
// This source file is part of the My Heart Counts Android open-source project
//
// SPDX-FileCopyrightText: 2026 Stanford University and the project authors (see CONTRIBUTORS.md)
//
// SPDX-License-Identifier: MIT

package edu.stanford.myheartcounts.healthstats

import edu.stanford.myheartcounts.firebase.FirebaseSchema
import org.grovealliance.scheduler.InstantRange
import java.time.YearMonth

/**
 * A source's complete contribution to one metric and month.
 */
data class HealthStatsContribution(
    val metricId: String,
    val month: YearMonth,
    val collection: HealthStatsEntryCollection,
    val sourceId: String,
    val entries: List<HealthStatsEntry>,
)

/**
 * How a contribution represents time.
 */
enum class HealthStatsEntryCollection(val field: String) {
    Hourly(field = FirebaseSchema.StatsDocument.HOURLY),
    Sessions(field = FirebaseSchema.StatsDocument.SESSIONS),
    Samples(field = FirebaseSchema.StatsDocument.SAMPLES),
}

/**
 * A local calendar month and its start-inclusive, end-exclusive bounds.
 */
data class HealthStatsMonth(
    val month: YearMonth,
    val timeRange: InstantRange,
)
