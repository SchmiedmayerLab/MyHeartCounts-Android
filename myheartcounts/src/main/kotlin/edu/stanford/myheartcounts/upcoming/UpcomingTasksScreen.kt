//
// This source file is part of the My Heart Counts Android open-source project
//
// SPDX-FileCopyrightText: 2026 Stanford University and the project authors (see CONTRIBUTORS.md)
//
// SPDX-License-Identifier: MIT

package edu.stanford.myheartcounts.upcoming

import androidx.compose.runtime.Composable
import edu.stanford.spezi.core.viewmodel.speziViewModel

/**
 * The upcoming tasks tab, listing the participant's scheduled activities.
 */
@Composable
fun UpcomingTasksScreen() {
    val viewModel = speziViewModel<UpcomingTasksViewModel>()
    viewModel.screen.Content()
}
