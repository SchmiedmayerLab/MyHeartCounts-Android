//
// This source file is part of the My Heart Counts Android open-source project
//
// SPDX-FileCopyrightText: 2026 Stanford University and the project authors (see CONTRIBUTORS.md)
//
// SPDX-License-Identifier: MIT

package edu.stanford.myheartcounts.promptedactions

/**
 * Builds [PromptedAction]s; by default a pending, unrejected Complete Demographics action.
 */
object PromptedActionFixtures {
    fun create(
        type: PromptedActionType = PromptedActionType.CompleteDemographics,
        state: PromptedActionState = PromptedActionState.Pending,
        isRejected: Boolean = false,
    ) = PromptedAction(
        type = type,
        state = state,
        isRejected = isRejected,
    )
}
