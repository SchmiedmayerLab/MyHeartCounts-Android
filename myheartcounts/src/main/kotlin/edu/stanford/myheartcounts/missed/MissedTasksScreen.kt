//
// This source file is part of the My Heart Counts Android open-source project
//
// SPDX-FileCopyrightText: 2026 Stanford University and the project authors (see CONTRIBUTORS.md)
//
// SPDX-License-Identifier: MIT

package edu.stanford.myheartcounts.missed

import androidx.compose.runtime.Composable
import org.grovealliance.core.viewmodel.groveViewModel

/**
 * The tasks the participant missed in the past two weeks; see [MissedTasksViewModel].
 */
@Composable
fun MissedTasksScreen() {
    val viewModel = groveViewModel<MissedTasksViewModel>()
    viewModel.screen.Content()
}
