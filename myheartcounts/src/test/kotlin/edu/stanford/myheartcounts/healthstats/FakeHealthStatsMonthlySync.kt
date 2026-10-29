//
// This source file is part of the My Heart Counts Android open-source project
//
// SPDX-FileCopyrightText: 2026 Stanford University and the project authors (see CONTRIBUTORS.md)
//
// SPDX-License-Identifier: MIT

package edu.stanford.myheartcounts.healthstats

class FakeHealthStatsMonthlySync : HealthStatsMonthlySync {
    private val refreshed = mutableListOf<HealthStatsSyncMetric>()
    private var failure: Exception? = null
    private var afterRefresh: (() -> Unit)? = null
    val metrics: List<HealthStatsSyncMetric> get() = refreshed.toList()

    fun setFailure(error: Exception) {
        failure = error
    }

    fun setAfterRefresh(action: () -> Unit) {
        afterRefresh = action
    }

    override suspend fun refresh(metric: HealthStatsSyncMetric, month: HealthStatsMonth, force: Boolean): Result<Unit> {
        refreshed += metric
        afterRefresh?.invoke()
        return failure?.let { Result.failure(it) } ?: Result.success(Unit)
    }
}
