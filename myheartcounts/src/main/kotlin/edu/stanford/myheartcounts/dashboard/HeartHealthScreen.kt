//
// This source file is part of the My Heart Counts Android open-source project
//
// SPDX-FileCopyrightText: 2026 Stanford University and the project authors (see CONTRIBUTORS.md)
//
// SPDX-License-Identifier: MIT

package edu.stanford.myheartcounts.dashboard

import androidx.compose.runtime.Composable
import org.grovealliance.core.viewmodel.groveViewModel

/**
 * The heart health dashboard tab, visualizing the participant's health metrics.
 */
@Composable
fun HeartHealthScreen() {
    val viewModel = groveViewModel<HeartHealthViewModel>()
    viewModel.screen.Content()
}
