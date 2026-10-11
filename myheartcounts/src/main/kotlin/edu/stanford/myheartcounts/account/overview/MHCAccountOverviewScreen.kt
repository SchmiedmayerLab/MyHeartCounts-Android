//
// This source file is part of the My Heart Counts Android open-source project
//
// SPDX-FileCopyrightText: 2026 Stanford University and the project authors (see CONTRIBUTORS.md)
//
// SPDX-License-Identifier: MIT

package edu.stanford.myheartcounts.account.overview

import androidx.compose.runtime.Composable
import org.grovealliance.core.viewmodel.groveViewModel

/**
 * The account overview screen; see [MHCAccountOverview].
 */
@Composable
fun MHCAccountOverviewScreen() {
    val viewModel = groveViewModel<MHCAccountOverviewViewModel>()
    viewModel.screen.Content()
}
