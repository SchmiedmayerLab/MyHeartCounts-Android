//
// This source file is part of the My Heart Counts Android open-source project
//
// SPDX-FileCopyrightText: 2026 Stanford University and the project authors (see CONTRIBUTORS.md)
//
// SPDX-License-Identifier: MIT

package edu.stanford.myheartcounts.di

import edu.stanford.myheartcounts.MainActivityViewModel
import edu.stanford.myheartcounts.dashboard.HeartHealthViewModel
import edu.stanford.myheartcounts.home.HomeViewModel
import edu.stanford.myheartcounts.onboarding.OnboardingViewModel
import edu.stanford.myheartcounts.study.StudyViewModel
import edu.stanford.myheartcounts.upcoming.UpcomingTasksViewModel
import edu.stanford.spezi.core.ConfigurationBuilder
import edu.stanford.spezi.core.SpeziDsl
import edu.stanford.spezi.core.viewmodel.viewModel

/**
 * Registers the app's ViewModels in the Spezi dependency graph.
 */
@SpeziDsl
fun ConfigurationBuilder.appViewModels() {
    viewModel {
        MainActivityViewModel(
            navigator = dependency(),
            account = dependency(),
        )
    }
    viewModel {
        OnboardingViewModel(
            navigator = dependency(),
            onboardingStepProvider = dependency(),
            onboardingStepLayoutMapper = dependency(),
            notificationPermissionHandler = dependency(),
        )
    }
    viewModel {
        StudyViewModel(
            savedStateHandle = savedStateHandle(),
        )
    }
    viewModel {
        HomeViewModel(
            studyAppBarProvider = dependency(),
        )
    }
    viewModel {
        UpcomingTasksViewModel(
            studyAppBarProvider = dependency(),
        )
    }
    viewModel {
        HeartHealthViewModel(
            studyAppBarProvider = dependency(),
        )
    }
}
