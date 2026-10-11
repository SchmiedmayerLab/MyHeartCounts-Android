//
// This source file is part of the My Heart Counts Android open-source project
//
// SPDX-FileCopyrightText: 2026 Stanford University and the project authors (see CONTRIBUTORS.md)
//
// SPDX-License-Identifier: MIT

package edu.stanford.myheartcounts.task

import org.grovealliance.ui.ComposableContent
import org.grovealliance.ui.GroveErrorLayout

/**
 * What the task screen shows.
 */
sealed interface PerformTaskScreenState {
    /**
     * The content to render for this state.
     */
    val layout: ComposableContent

    /**
     * A task is being performed and rendered by [layout].
     */
    data class Content(override val layout: QuestionnaireTaskContent) : PerformTaskScreenState

    /**
     * The screen opened without a task to perform; [layout] offers to close it.
     */
    data class Error(override val layout: GroveErrorLayout) : PerformTaskScreenState
}
