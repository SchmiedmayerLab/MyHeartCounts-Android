//
// This source file is part of the My Heart Counts Android open-source project
//
// SPDX-FileCopyrightText: 2026 Stanford University and the project authors (see CONTRIBUTORS.md)
//
// SPDX-License-Identifier: MIT

package edu.stanford.myheartcounts.dashboard.detail

import androidx.compose.runtime.Composable
import edu.stanford.myheartcounts.dashboard.HeartScoreComponent
import org.grovealliance.core.viewmodel.groveViewModel

/**
 * The details of one [component] of the participant's heart health score.
 */
@Composable
fun HeartScoreDetailScreen(component: HeartScoreComponent) {
    val viewModel = groveViewModel<HeartScoreDetailViewModel> {
        putString(HeartScoreDetailViewModel.KEY_COMPONENT, component.name)
    }
    viewModel.screen.Content()
}
