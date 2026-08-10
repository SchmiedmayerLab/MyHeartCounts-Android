//
// This source file is part of the My Heart Counts Android open-source project
//
// SPDX-FileCopyrightText: 2026 Stanford University and the project authors (see CONTRIBUTORS.md)
//
// SPDX-License-Identifier: MIT

package edu.stanford.myheartcounts.onboarding

import androidx.compose.runtime.Composable
import edu.stanford.spezi.core.viewmodel.speziViewModel

/**
 * Entry point for the onboarding flow, rendering the content owned by the [OnboardingViewModel].
 */
@Composable
fun OnboardingScreen() {
    val viewModel = speziViewModel<OnboardingViewModel>()
    viewModel.content.Content()
}
