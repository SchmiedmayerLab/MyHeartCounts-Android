//
// This source file is part of the My Heart Counts Android open-source project
//
// SPDX-FileCopyrightText: 2026 Stanford University and the project authors (see CONTRIBUTORS.md)
//
// SPDX-License-Identifier: MIT

package edu.stanford.myheartcounts.promptedactions

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/**
 * A [PromptedActionsSource] emitting the actions set through [setActions], none by default.
 */
class FakePromptedActionsSource : PromptedActionsSource {
    private val state = MutableStateFlow<List<PromptedAction>>(emptyList())

    override val actions: StateFlow<List<PromptedAction>> = state.asStateFlow()

    /**
     * Emits [actions].
     */
    fun setActions(actions: List<PromptedAction>) {
        state.value = actions
    }
}
