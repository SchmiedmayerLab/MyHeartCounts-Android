//
// This source file is part of the My Heart Counts Android open-source project
//
// SPDX-FileCopyrightText: 2026 Stanford University and the project authors (see CONTRIBUTORS.md)
//
// SPDX-License-Identifier: MIT

package edu.stanford.myheartcounts.dashboard.detail

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.map
import org.grovealliance.scheduler.InstantRange

/**
 * A [HeartScoreDetailRanges] returning the range last set for every selection.
 */
class FakeHeartScoreDetailRanges : HeartScoreDetailRanges {

    private val revision = MutableStateFlow(0)
    private var range = HeartScoreDetailFixtures.RANGE

    override val changes: Flow<Unit> = revision.map { }

    override fun range(selection: HeartScoreChartRange): InstantRange = range

    /**
     * Returns [range] for every selection from now on, without announcing a change.
     */
    fun setRange(range: InstantRange) {
        this.range = range
    }

    /**
     * Announces that the ranges changed.
     */
    fun refresh() {
        revision.value++
    }
}
