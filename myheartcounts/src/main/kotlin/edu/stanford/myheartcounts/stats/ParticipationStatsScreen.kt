//
// This source file is part of the My Heart Counts Android open-source project
//
// SPDX-FileCopyrightText: 2026 Stanford University and the project authors (see CONTRIBUTORS.md)
//
// SPDX-License-Identifier: MIT

package edu.stanford.myheartcounts.stats

import androidx.compose.runtime.Composable
import org.grovealliance.core.viewmodel.groveViewModel

/**
 * The participant's study participation statistics and achievements.
 */
@Composable
fun ParticipationStatsScreen() {
    val viewModel = groveViewModel<ParticipationStatsViewModel>()
    viewModel.screen.Content()
}
