//
// This source file is part of the My Heart Counts Android open-source project
//
// SPDX-FileCopyrightText: 2026 Stanford University and the project authors (see CONTRIBUTORS.md)
//
// SPDX-License-Identifier: MIT

package edu.stanford.myheartcounts.promptedactions.ui

import androidx.compose.runtime.Composable
import edu.stanford.myheartcounts.promptedactions.PromptedActionsMode
import edu.stanford.myheartcounts.promptedactions.PromptedActionsViewModel
import org.grovealliance.core.viewmodel.groveViewModel

/**
 * The prompted actions of [mode], each offering to take it.
 */
@Composable
fun PromptedActionsScreen(mode: PromptedActionsMode) {
    val viewModel = groveViewModel<PromptedActionsViewModel> {
        putString(PromptedActionsViewModel.KEY_MODE, mode.name)
    }
    viewModel.screen.Content()
}
