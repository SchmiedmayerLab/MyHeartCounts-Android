//
// This source file is part of the My Heart Counts Android open-source project
//
// SPDX-FileCopyrightText: 2026 Stanford University and the project authors (see CONTRIBUTORS.md)
//
// SPDX-License-Identifier: MIT

package edu.stanford.myheartcounts.promptedactions

/**
 * An intent raised by the prompted actions screen.
 */
sealed interface PromptedActionsAction {

    /**
     * The participant closed the screen.
     */
    data object Close : PromptedActionsAction

    /**
     * The participant chose to take the step of [type].
     */
    data class PerformSelected(val type: PromptedActionType) : PromptedActionsAction

    /**
     * The participant asked to stop being suggested the step of [type], which still needs confirming.
     */
    data class StopSuggestingSelected(val type: PromptedActionType) : PromptedActionsAction

    /**
     * The participant confirmed to stop being suggested the step of [type].
     */
    data class StopSuggestingConfirmed(val type: PromptedActionType) : PromptedActionsAction
}
