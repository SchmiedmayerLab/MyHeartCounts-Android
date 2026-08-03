//
// This source file is part of the My Heart Counts open-source project
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
import edu.stanford.spezi.ui.ComposableContent
import edu.stanford.spezi.ui.LoadingLayout
import edu.stanford.spezi.ui.SpeziCard
import edu.stanford.spezi.ui.SpeziErrorLayout
import edu.stanford.spezi.ui.scheduler.MissedTasksRow
import edu.stanford.spezi.ui.scheduler.NudgeBanner
import edu.stanford.spezi.ui.scheduler.PromptedActionsCard
import edu.stanford.spezi.ui.scheduler.TaskListLayout
import edu.stanford.spezi.ui.theme.Spacings

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
    data class Error(override val layout: SpeziErrorLayout) : HomeLayoutState

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
            SpeziCard {
                learnMore.Content(modifier = Modifier.padding(Spacings.medium))
            }
        }
    }
}
