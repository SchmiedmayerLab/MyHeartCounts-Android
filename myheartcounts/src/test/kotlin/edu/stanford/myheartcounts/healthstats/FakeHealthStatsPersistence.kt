//
// This source file is part of the My Heart Counts Android open-source project
//
// SPDX-FileCopyrightText: 2026 Stanford University and the project authors (see CONTRIBUTORS.md)
//
// SPDX-License-Identifier: MIT

package edu.stanford.myheartcounts.healthstats

import java.time.YearMonth

class FakeHealthStatsPersistence : HealthStatsPersistence {
    private val checkpoints = mutableMapOf<Pair<String, YearMonth>, HealthStatsCheckpoint>()
    private val contributions = mutableListOf<HealthStatsContribution>()
    val writes: List<HealthStatsContribution> get() = contributions.toList()

    override fun checkpoint(metric: HealthStatsSyncMetric, month: YearMonth): HealthStatsCheckpoint? = checkpoints[metric.id to month]

    override suspend fun persist(
        contribution: HealthStatsContribution,
        checkpoint: HealthStatsCheckpoint,
        hasDeletions: Boolean,
    ): Result<Unit> {
        if (contribution.entries.isNotEmpty() || hasDeletions) contributions += contribution
        checkpoints[contribution.metricId to contribution.month] = checkpoint
        return Result.success(Unit)
    }

    override fun clear() = checkpoints.clear()
}
