//
// This source file is part of the My Heart Counts Android open-source project
//
// SPDX-FileCopyrightText: 2026 Stanford University and the project authors (see CONTRIBUTORS.md)
//
// SPDX-License-Identifier: MIT

package edu.stanford.myheartcounts.onboarding

import androidx.compose.runtime.Composable
import org.grovealliance.core.viewmodel.groveViewModel

/**
 * Entry point for the onboarding flow, rendering the content owned by the [OnboardingViewModel].
 */
@Composable
fun OnboardingScreen() {
    val viewModel = groveViewModel<OnboardingViewModel>()
    viewModel.content.Content()
}
