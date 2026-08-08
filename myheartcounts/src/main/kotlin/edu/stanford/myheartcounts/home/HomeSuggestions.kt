//
// This source file is part of the My Heart Counts open-source project
//
// SPDX-FileCopyrightText: 2026 Stanford University and the project authors (see CONTRIBUTORS.md)
//
// SPDX-License-Identifier: MIT

package edu.stanford.myheartcounts.home

import edu.stanford.spezi.ui.StringResource
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOf

/**
 * Supplies what the Home tab surfaces above the participant's tasks.
 */
interface HomeSuggestionsSource {
    /**
     * The nudge to show, or `null` when there is none.
     */
    val nudge: Flow<DailyNudge?>

    /**
     * The outstanding prompted actions, empty when there is nothing to prompt.
     */
    val pendingActions: Flow<List<PromptedAction>>
}

/**
 * A short prompt shown at the top of the Home tab, derived from the notifications a participant has
 * most recently been sent.
 */
data class DailyNudge(
    val title: StringResource,
    val message: StringResource,
)

/**
 * An action the participant is encouraged to take, outside of their scheduled study tasks.
 */
data class PromptedAction(
    val id: String,
    val title: StringResource,
)

// TODO: Replace with the Firebase-backed source once notification history and account state exist.
/**
 * Surfaces nothing above the participant's tasks.
 *
 * Nudges are derived from the notifications a participant has been sent, and prompted actions from
 * account state and device capabilities; the app records neither yet.
 */
class NoHomeSuggestionsSource : HomeSuggestionsSource {
    override val nudge: Flow<DailyNudge?> = flowOf(null)
    override val pendingActions: Flow<List<PromptedAction>> = flowOf(emptyList())
}
