//
// This source file is part of the My Heart Counts Android open-source project
//
// SPDX-FileCopyrightText: 2026 Stanford University and the project authors (see CONTRIBUTORS.md)
//
// SPDX-License-Identifier: MIT

package edu.stanford.myheartcounts.promptedactions

import org.grovealliance.ui.ActionSink
import org.grovealliance.ui.GroveAlertDialog

/**
 * [PromptedActionsMapper] mapping like [PromptedActionsMapperImpl], remembering the
 * [stopSuggestingDialogs] it built.
 */
class FakePromptedActionsMapper : PromptedActionsMapper by PromptedActionsMapperImpl() {
    val stopSuggestingDialogs = mutableListOf<GroveAlertDialog>()

    override fun mapStopSuggestingDialog(
        type: PromptedActionType,
        actionSink: ActionSink<PromptedActionsAction>,
    ): GroveAlertDialog = PromptedActionsMapperImpl()
        .mapStopSuggestingDialog(
            type = type,
            actionSink = actionSink,
        )
        .also { stopSuggestingDialogs.add(it) }
}
