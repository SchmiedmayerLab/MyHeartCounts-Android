//
// This source file is part of the My Heart Counts Android open-source project
//
// SPDX-FileCopyrightText: 2026 Stanford University and the project authors (see CONTRIBUTORS.md)
//
// SPDX-License-Identifier: MIT

package edu.stanford.myheartcounts.promptedactions

/**
 * Which prompted actions are presented.
 */
enum class PromptedActionsMode {

    /**
     * The pending actions the participant hasn't asked to stop seeing, which they can reject.
     */
    Pending,

    /**
     * Every pending action, rejected ones included, and the completed ones.
     */
    All,
    ;

    /**
     * The pending [actions] presented in this mode.
     */
    fun pending(actions: List<PromptedAction>): List<PromptedAction> = actions.filter { action ->
        action.state == PromptedActionState.Pending && (this == All || !action.isRejected)
    }
}
