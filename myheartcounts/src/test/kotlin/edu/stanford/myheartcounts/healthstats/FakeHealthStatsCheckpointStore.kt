//
// This source file is part of the My Heart Counts Android open-source project
//
// SPDX-FileCopyrightText: 2026 Stanford University and the project authors (see CONTRIBUTORS.md)
//
// SPDX-License-Identifier: MIT

package edu.stanford.myheartcounts.healthstats

import java.time.YearMonth

class FakeHealthStatsCheckpointStore : HealthStatsCheckpointStore {
    private val checkpoints = mutableMapOf<Pair<String, YearMonth>, HealthStatsCheckpoint>()

    override fun get(metricId: String, month: YearMonth): HealthStatsCheckpoint? = checkpoints[metricId to month]

    override fun put(metricId: String, month: YearMonth, checkpoint: HealthStatsCheckpoint) {
        checkpoints[metricId to month] = checkpoint
    }

    override fun clear() = checkpoints.clear()
}
