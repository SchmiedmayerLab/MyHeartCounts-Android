//
// This source file is part of the My Heart Counts open-source project
//
// SPDX-FileCopyrightText: 2026 Stanford University and the project authors (see CONTRIBUTORS.md)
//
// SPDX-License-Identifier: MIT

package edu.stanford.spezi.sample.app.home

import androidx.compose.runtime.Composable
import edu.stanford.spezi.core.viewmodel.speziViewModel

@Composable
fun HomeScreen() {
    val viewModel = speziViewModel<HomeViewModel>()
    viewModel.content.Content()
}
