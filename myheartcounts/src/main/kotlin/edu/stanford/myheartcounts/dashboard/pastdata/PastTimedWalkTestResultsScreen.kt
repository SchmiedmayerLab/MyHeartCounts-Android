//
// This source file is part of the My Heart Counts Android open-source project
//
// SPDX-FileCopyrightText: 2026 Stanford University and the project authors (see CONTRIBUTORS.md)
//
// SPDX-License-Identifier: MIT

package edu.stanford.myheartcounts.dashboard.pastdata

import androidx.compose.runtime.Composable
import org.grovealliance.core.viewmodel.groveViewModel

/**
 * The participant's past timed walking and running test results.
 */
@Composable
fun PastTimedWalkTestResultsScreen() {
    val viewModel = groveViewModel<PastTimedWalkTestResultsViewModel>()
    viewModel.screen.Content()
}
