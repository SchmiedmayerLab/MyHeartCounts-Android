//
// This source file is part of the My Heart Counts Android open-source project
//
// SPDX-FileCopyrightText: 2026 Stanford University and the project authors (see CONTRIBUTORS.md)
//
// SPDX-License-Identifier: MIT

package edu.stanford.myheartcounts.dashboard

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import edu.stanford.myheartcounts.dashboard.ui.DashboardSection
import org.grovealliance.ui.ComposableContent
import org.grovealliance.ui.GroveErrorLayout
import org.grovealliance.ui.LoadingLayout
import org.grovealliance.ui.theme.Spacings

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
    data class Content(override val layout: HeartHealthContentLayout) : HeartHealthLayoutState
}

/**
 * The Heart Health tab's sections, in the order they are shown: the overall score, the factors it is
 * made of, an optional explanation of the score, and a link to past test results.
 */
data class HeartHealthContentLayout(
    val sections: List<DashboardSection>,
) : ComposableContent {

    @Composable
    override fun Content(modifier: Modifier) {
        LazyColumn(
            modifier = modifier.fillMaxSize(),
            contentPadding = PaddingValues(Spacings.medium),
            verticalArrangement = Arrangement.spacedBy(Spacings.large),
        ) {
            items(items = sections) { section ->
                section.Content(modifier = Modifier.fillMaxWidth())
            }
        }
    }
}
