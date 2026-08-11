//
// This source file is part of the My Heart Counts Android open-source project
//
// SPDX-FileCopyrightText: 2026 Stanford University and the project authors (see CONTRIBUTORS.md)
//
// SPDX-License-Identifier: MIT

package edu.stanford.spezi.sample.app.health

import androidx.compose.runtime.Composable
import edu.stanford.spezi.core.viewmodel.speziViewModel

@Composable
fun HealthScreen() {
    val viewModel = speziViewModel<HealthViewModel>()
    viewModel.content.Content()
}
