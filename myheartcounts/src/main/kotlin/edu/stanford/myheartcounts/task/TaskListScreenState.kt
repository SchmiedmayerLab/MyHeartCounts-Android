//
// This source file is part of the My Heart Counts Android open-source project
//
// SPDX-FileCopyrightText: 2026 Stanford University and the project authors (see CONTRIBUTORS.md)
//
// SPDX-License-Identifier: MIT

package edu.stanford.myheartcounts.task

import org.grovealliance.ui.ComposableContent
import org.grovealliance.ui.GroveErrorLayout
import org.grovealliance.ui.LoadingLayout
import org.grovealliance.ui.scheduler.TaskListLayout

/**
 * The loading, failed, or ready state of a screen listing the participant's tasks.
 */
sealed interface TaskListScreenState {
    /**
     * The content to render for this state.
     */
    val layout: ComposableContent

    /**
     * The tasks are not yet known.
     */
    data object Loading : TaskListScreenState {
        override val layout = LoadingLayout()
    }

    /**
     * Loading failed; [layout] offers a retry.
     */
    data class Error(override val layout: GroveErrorLayout) : TaskListScreenState

    /**
     * The tasks are known and listed by [layout].
     */
    data class Content(override val layout: TaskListLayout) : TaskListScreenState
}
