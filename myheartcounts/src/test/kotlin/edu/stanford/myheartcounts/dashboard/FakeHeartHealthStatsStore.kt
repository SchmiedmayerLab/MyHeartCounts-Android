//
// This source file is part of the My Heart Counts Android open-source project
//
// SPDX-FileCopyrightText: 2026 Stanford University and the project authors (see CONTRIBUTORS.md)
//
// SPDX-License-Identifier: MIT

package edu.stanford.myheartcounts.dashboard

import edu.stanford.myheartcounts.healthstats.HealthStatsReadPolicy
import edu.stanford.myheartcounts.healthstats.HealthStatsRequest
import edu.stanford.myheartcounts.healthstats.HealthStatsSnapshot
import edu.stanford.myheartcounts.healthstats.HealthStatsStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow

class FakeHeartHealthStatsStore : HealthStatsStore {
    private val data = mutableMapOf<String, MutableStateFlow<Result<HealthStatsSnapshot<*>>>>()
    val requests = mutableListOf<HealthStatsRequest<*>>()

    fun setSnapshot(metricId: String, snapshot: HealthStatsSnapshot<*>) {
        state(metricId = metricId).value = Result.success(snapshot)
    }

    fun fail(metricId: String) {
        state(metricId = metricId).value = Result.failure(IllegalStateException("unavailable"))
    }

    @Suppress("UNCHECKED_CAST")
    override fun <T> updates(request: HealthStatsRequest<T>): Flow<Result<HealthStatsSnapshot<T>>> {
        requests += request
        return state(metricId = request.metricId) as Flow<Result<HealthStatsSnapshot<T>>>
    }

    override suspend fun <T> fetch(request: HealthStatsRequest<T>, readPolicy: HealthStatsReadPolicy): Result<HealthStatsSnapshot<T>> =
        error("Scores must use live updates")

    private fun state(metricId: String) = data.getOrPut(metricId) {
        MutableStateFlow(Result.success(HeartHealthScoreFixtures.snapshot(elements = emptyList<Nothing>())))
    }
}
