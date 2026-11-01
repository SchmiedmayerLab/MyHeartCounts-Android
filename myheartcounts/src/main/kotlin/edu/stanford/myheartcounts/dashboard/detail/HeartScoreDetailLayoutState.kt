//
// This source file is part of the My Heart Counts Android open-source project
//
// SPDX-FileCopyrightText: 2026 Stanford University and the project authors (see CONTRIBUTORS.md)
//
// SPDX-License-Identifier: MIT

package edu.stanford.myheartcounts.dashboard.detail

import org.grovealliance.ui.ComposableContent
import org.grovealliance.ui.GroveErrorLayout
import org.grovealliance.ui.LoadingLayout

/**
 * What a factor's details screen shows.
 */
sealed interface HeartScoreDetailLayoutState {

    /**
     * The content shown for this state.
     */
    val layout: ComposableContent

    /**
     * The details are being read.
     */
    data object Loading : HeartScoreDetailLayoutState {
        override val layout = LoadingLayout()
    }

    /**
     * The details were read.
     */
    data class Content(override val layout: HeartScoreDetailContentLayout) : HeartScoreDetailLayoutState

    /**
     * The details could not be read; [layout] offers to try again.
     */
    data class Error(override val layout: GroveErrorLayout) : HeartScoreDetailLayoutState
}
