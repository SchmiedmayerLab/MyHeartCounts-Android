//
// This source file is part of the My Heart Counts Android open-source project
//
// SPDX-FileCopyrightText: 2026 Stanford University and the project authors (see CONTRIBUTORS.md)
//
// SPDX-License-Identifier: MIT

package edu.stanford.myheartcounts.dashboard.detail

import edu.stanford.myheartcounts.dashboard.HeartScoreComponent
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow

class FakeHeartScoreChartSource : HeartScoreChartSource {
    val requests = mutableListOf<HeartScoreChartRange>()
    private val values = HeartScoreChartRange.entries.associateWith {
        MutableStateFlow(Result.success(HeartScoreDetailFixtures.chart(points = emptyList())))
    }
    override fun updates(
        component: HeartScoreComponent,
        selection: HeartScoreChartRange,
    ): Flow<Result<HeartScoreChartData>> {
        requests += selection
        return values.getValue(selection)
    }
    fun fail(selection: HeartScoreChartRange) {
        values.getValue(selection).value = Result.failure(IllegalStateException("history unavailable"))
    }
    fun set(selection: HeartScoreChartRange, data: HeartScoreChartData) {
        values.getValue(selection).value = Result.success(data)
    }
}
