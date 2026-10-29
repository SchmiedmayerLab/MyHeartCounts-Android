//
// This source file is part of the My Heart Counts Android open-source project
//
// SPDX-FileCopyrightText: 2026 Stanford University and the project authors (see CONTRIBUTORS.md)
//
// SPDX-License-Identifier: MIT

package edu.stanford.myheartcounts.di

import edu.stanford.myheartcounts.MainActivityViewModel
import edu.stanford.myheartcounts.account.overview.MHCAccountOverviewViewModel
import edu.stanford.myheartcounts.dashboard.HeartHealthViewModel
import edu.stanford.myheartcounts.home.HomeViewModel
import edu.stanford.myheartcounts.missed.MissedTasksViewModel
import edu.stanford.myheartcounts.onboarding.OnboardingViewModel
import edu.stanford.myheartcounts.study.StudyViewModel
import edu.stanford.myheartcounts.task.PerformTaskViewModel
import edu.stanford.myheartcounts.upcoming.UpcomingTasksViewModel
import org.grovealliance.core.ConfigurationBuilder
import org.grovealliance.core.GroveDsl
import org.grovealliance.core.coroutines.Concurrency
import org.grovealliance.core.viewmodel.viewModel

/**
 * Registers the app's ViewModels in the Grove dependency graph.
 */
@GroveDsl
fun ConfigurationBuilder.appViewModels() {
    viewModel {
        MainActivityViewModel(
            navigator = dependency(),
            account = dependency(),
            onboardingCompletion = dependency(),
        )
    }
    viewModel {
        OnboardingViewModel(
            navigator = dependency(),
            onboardingStepProvider = dependency(),
            onboardingStepLayoutMapper = dependency(),
            notificationPermissionHandler = dependency(),
            studyEnroller = dependency(),
            firebaseRegionInitializer = dependency(),
            cloudFunctions = dependency(),
            accountService = dependency(),
            consentUploader = dependency(),
            consentDocumentProvider = dependency(),
            onboardingCompletion = dependency(),
            healthAccess = dependency(),
        )
    }
    viewModel {
        MHCAccountOverviewViewModel(
            navigator = dependency(),
            studyWithdrawal = dependency(),
            mapper = dependency(),
        )
    }
    studyViewModels()
}

/**
 * Registers the ViewModels of the study phase: its tabs and the task screen.
 */
private fun ConfigurationBuilder.studyViewModels() {
    viewModel {
        StudyViewModel(
            savedStateHandle = savedStateHandle(),
        )
    }
    viewModel {
        HomeViewModel(
            studyAppBarProvider = dependency(),
            navigator = dependency(),
            tasksSource = dependency(),
            mapper = dependency(),
            suggestionsSource = dependency(),
            eventPerformer = dependency(),
        )
    }
    viewModel {
        MissedTasksViewModel(
            navigator = dependency(),
            missedEventsSource = dependency(),
            mapper = dependency(),
            timeProvider = dependency(),
            eventPerformer = dependency(),
        )
    }
    viewModel {
        PerformTaskViewModel(
            taskPerformer = dependency(),
            navigator = dependency(),
            responseUploader = dependency(),
            uploadScope = dependency<Concurrency>().ioCoroutineScope(),
        )
    }
    viewModel {
        UpcomingTasksViewModel(
            studyAppBarProvider = dependency(),
            tileMapper = dependency(),
            tasksSource = dependency(),
            mapper = dependency(),
            eventPerformer = dependency(),
        )
    }
    viewModel {
        HeartHealthViewModel(
            studyAppBarProvider = dependency(),
        )
    }
}
