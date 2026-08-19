//
// This source file is part of the My Heart Counts Android open-source project
//
// SPDX-FileCopyrightText: 2026 Stanford University and the project authors (see CONTRIBUTORS.md)
//
// SPDX-License-Identifier: MIT

package edu.stanford.myheartcounts.home

import androidx.compose.runtime.Composable
import org.grovealliance.core.viewmodel.groveViewModel

/**
 * The main content shown after onboarding completes.
 */
@Composable
fun HomeScreen() {
    val viewModel = groveViewModel<HomeViewModel>()
    viewModel.screen.Content()
}
