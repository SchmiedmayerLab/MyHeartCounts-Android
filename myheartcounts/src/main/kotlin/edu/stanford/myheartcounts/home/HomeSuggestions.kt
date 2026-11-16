//
// This source file is part of the My Heart Counts Android open-source project
//
// SPDX-FileCopyrightText: 2026 Stanford University and the project authors (see CONTRIBUTORS.md)
//
// SPDX-License-Identifier: MIT

package edu.stanford.myheartcounts.home

import edu.stanford.myheartcounts.promptedactions.PromptedAction
import kotlinx.coroutines.flow.Flow
import org.grovealliance.ui.StringResource

/**
 * Supplies what the Home tab surfaces above the participant's tasks.
 */
interface HomeSuggestionsSource {
    /**
     * The nudge to show, or `null` when there is none.
     */
    val nudge: Flow<DailyNudge?>

    /**
     * Every prompted action, in display order.
     */
    val promptedActions: Flow<List<PromptedAction>>
}

/**
 * A short prompt shown at the top of the Home tab, derived from the notifications a participant has
 * most recently been sent.
 */
data class DailyNudge(
    val title: StringResource,
    val message: StringResource,
)
