//
// This source file is part of the My Heart Counts Android open-source project
//
// SPDX-FileCopyrightText: 2026 Stanford University and the project authors (see CONTRIBUTORS.md)
//
// SPDX-License-Identifier: MIT

package edu.stanford.myheartcounts.task

import androidx.compose.runtime.Composable
import org.grovealliance.core.viewmodel.groveViewModel

/**
 * The screen of the task being performed; see [PerformTaskViewModel].
 */
@Composable
fun PerformTaskScreen() {
    val viewModel = groveViewModel<PerformTaskViewModel>()
    viewModel.screen.layout.Content()
}
