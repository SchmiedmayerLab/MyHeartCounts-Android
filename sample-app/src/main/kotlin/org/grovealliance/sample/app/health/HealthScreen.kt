//
// This source file is part of the My Heart Counts Android open-source project
//
// SPDX-FileCopyrightText: 2026 Stanford University and the project authors (see CONTRIBUTORS.md)
//
// SPDX-License-Identifier: MIT

package org.grovealliance.sample.app.health

import androidx.compose.runtime.Composable
import org.grovealliance.core.viewmodel.groveViewModel

@Composable
fun HealthScreen() {
    val viewModel = groveViewModel<HealthViewModel>()
    viewModel.content.Content()
}
