//
// This source file is part of the My Heart Counts Android open-source project
//
// SPDX-FileCopyrightText: 2026 Stanford University and the project authors (see CONTRIBUTORS.md)
//
// SPDX-License-Identifier: MIT

package edu.stanford.myheartcounts.dashboard

import org.grovealliance.ui.ComposableContent
import org.grovealliance.ui.GroveErrorLayout
import org.grovealliance.ui.GroveSectionList
import org.grovealliance.ui.LoadingLayout

/**
 * The loading, failed, or ready state of the Heart Health tab.
 */
sealed interface HeartHealthLayoutState {

    /**
     * The content to render for this state.
     */
    val layout: ComposableContent

    /**
     * The participant's scores are not yet known.
     */
    data object Loading : HeartHealthLayoutState {
        override val layout = LoadingLayout()
    }

    /**
     * Loading failed; [layout] offers a retry.
     */
    data class Error(override val layout: GroveErrorLayout) : HeartHealthLayoutState

    /**
     * The scores are known and rendered by [layout].
     */
    data class Content(override val layout: GroveSectionList) : HeartHealthLayoutState
}
