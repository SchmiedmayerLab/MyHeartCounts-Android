//
// This source file is part of the My Heart Counts Android open-source project
//
// SPDX-FileCopyrightText: 2026 Stanford University and the project authors (see CONTRIBUTORS.md)
//
// SPDX-License-Identifier: MIT

package edu.stanford.myheartcounts.dashboard.detail

import edu.stanford.myheartcounts.customsamples.CustomQuantitySample
import edu.stanford.myheartcounts.dashboard.HeartScoreComponent
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOf

class FakeHeartScoreDebugSource : HeartScoreDebugSource {
    override fun samples(component: HeartScoreComponent): Flow<List<CustomQuantitySample>?> = flowOf(null)
}
