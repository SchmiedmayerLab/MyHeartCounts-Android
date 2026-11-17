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
 * [PromptedActionRejections] held in memory.
 */
class FakePromptedActionRejections : PromptedActionRejections {
    private val state = MutableStateFlow<Set<PromptedActionType>>(emptySet())

    override val rejected: StateFlow<Set<PromptedActionType>> = state.asStateFlow()

    override fun reject(type: PromptedActionType) {
        state.value = state.value + type
    }

    override fun clear() {
        state.value = emptySet()
    }
}
