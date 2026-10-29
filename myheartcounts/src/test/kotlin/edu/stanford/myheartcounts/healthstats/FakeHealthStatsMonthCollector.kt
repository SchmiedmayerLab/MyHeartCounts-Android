//
// This source file is part of the My Heart Counts Android open-source project
//
// SPDX-FileCopyrightText: 2026 Stanford University and the project authors (see CONTRIBUTORS.md)
//
// SPDX-License-Identifier: MIT

package edu.stanford.myheartcounts.healthstats

class FakeHealthStatsMonthCollector : HealthStatsMonthCollector {
    private var snapshot = HealthStatsWritingFixtures.snapshot(entries = emptyList(), ids = emptySet())
    private var count = 0
    private var afterCollect: (() -> Unit)? = null
    val collectionCount: Int get() = count

    fun setSnapshot(value: HealthStatsCollectedMonth) {
        snapshot = value
    }

    fun setAfterCollect(action: () -> Unit) {
        afterCollect = action
    }

    override suspend fun collect(metric: HealthStatsSyncMetric, month: HealthStatsMonth): Result<HealthStatsCollectedMonth> {
        count += 1
        afterCollect?.invoke()
        return Result.success(snapshot)
    }
}
