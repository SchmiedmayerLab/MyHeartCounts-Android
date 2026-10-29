//
// This source file is part of the My Heart Counts Android open-source project
//
// SPDX-FileCopyrightText: 2026 Stanford University and the project authors (see CONTRIBUTORS.md)
//
// SPDX-License-Identifier: MIT

package edu.stanford.myheartcounts.dashboard.entry

import androidx.compose.runtime.Composable
import edu.stanford.myheartcounts.dashboard.HeartScoreComponent
import org.grovealliance.core.viewmodel.groveViewModel

/**
 * Enters a new measurement for [component].
 */
@Composable
fun HeartScoreEntryScreen(component: HeartScoreComponent) {
    val viewModel = groveViewModel<HeartScoreEntryViewModel> {
        putString(HeartScoreEntryViewModel.KEY_COMPONENT, component.name)
    }
    viewModel.screen.Content()
}
