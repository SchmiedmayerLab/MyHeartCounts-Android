//
// This source file is part of the My Heart Counts Android open-source project
//
// SPDX-FileCopyrightText: 2026 Stanford University and the project authors (see CONTRIBUTORS.md)
//
// SPDX-License-Identifier: MIT

package edu.stanford.myheartcounts.dashboard.detail

import edu.stanford.myheartcounts.dashboard.HeartScoreResult
import edu.stanford.myheartcounts.dashboard.ui.ColorBandList

/**
 * A [HeartScoreBandMapper] returning whatever bands were last set, or none.
 */
class FakeHeartScoreBandMapper : HeartScoreBandMapper {

    private var bands: ColorBandList? = null

    override fun map(result: HeartScoreResult): ColorBandList? = bands

    /**
     * Returns [bands] for every result from now on.
     */
    fun setBands(bands: ColorBandList?) {
        this.bands = bands
    }
}
