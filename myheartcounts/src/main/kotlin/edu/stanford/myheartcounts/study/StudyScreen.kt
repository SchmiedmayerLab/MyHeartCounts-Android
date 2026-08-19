//
// This source file is part of the My Heart Counts Android open-source project
//
// SPDX-FileCopyrightText: 2026 Stanford University and the project authors (see CONTRIBUTORS.md)
//
// SPDX-License-Identifier: MIT

package edu.stanford.myheartcounts.study

import androidx.compose.runtime.Composable
import org.grovealliance.core.viewmodel.groveViewModel

/**
 * The study content shown after onboarding, hosting the bottom-navigation tabs.
 */
@Composable
fun StudyScreen() {
    val viewModel = groveViewModel<StudyViewModel>()
    viewModel.screen.Content()
}
