//
// This source file is part of the My Heart Counts Android open-source project
//
// SPDX-FileCopyrightText: 2026 Stanford University and the project authors (see CONTRIBUTORS.md)
//
// SPDX-License-Identifier: MIT

package edu.stanford.myheartcounts.home

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import edu.stanford.myheartcounts.ui.ExternalLink
import org.grovealliance.ui.ComposableContent
import org.grovealliance.ui.GroveCard
import org.grovealliance.ui.GroveErrorLayout
import org.grovealliance.ui.LoadingLayout
import org.grovealliance.ui.scheduler.MissedTasksRow
import org.grovealliance.ui.scheduler.NudgeBanner
import org.grovealliance.ui.scheduler.PromptedActionsCard
import org.grovealliance.ui.scheduler.TaskListLayout
import org.grovealliance.ui.theme.Spacings

/**
 * The loading, failed, or ready state of the Home tab.
 */
sealed interface HomeLayoutState {
    /**
     * The content to render for this state.
     */
    val layout: ComposableContent

    /**
     * The study bundle is being loaded and the participant's tasks are not yet known.
     */
    data object Loading : HomeLayoutState {
        override val layout = LoadingLayout()
    }

    /**
     * Loading failed; [layout] offers a retry.
     */
    data class Error(override val layout: GroveErrorLayout) : HomeLayoutState

    /**
     * Tasks are available and rendered by [layout].
     */
    data class Content(override val layout: HomeContentLayout) : HomeLayoutState
}

/**
 * The Home tab's sections, in the order they are shown.
 *
 * [nudge] and [promptedActions] are absent whenever there is nothing to surface, which is the case
 * until notification history and account-backed suggestions are available.
 */
data class HomeContentLayout(
    val nudge: NudgeBanner?,
    val promptedActions: PromptedActionsCard?,
    val tasks: TaskListLayout,
    val missedTasks: MissedTasksRow?,
    val learnMore: ExternalLink,
) : ComposableContent {
    @Composable
    override fun Content(modifier: Modifier) {
        Column(
            modifier = modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(Spacings.medium),
            verticalArrangement = Arrangement.spacedBy(Spacings.medium),
        ) {
            nudge?.Content(modifier = Modifier.fillMaxWidth())
            promptedActions?.Content()
            tasks.Content()
            missedTasks?.Content()
            GroveCard {
                learnMore.Content(modifier = Modifier.padding(Spacings.medium))
            }
        }
    }
}
