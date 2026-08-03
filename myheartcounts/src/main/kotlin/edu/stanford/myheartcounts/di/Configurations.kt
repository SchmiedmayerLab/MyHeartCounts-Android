//
// This source file is part of the My Heart Counts open-source project
//
// SPDX-FileCopyrightText: 2026 Stanford University and the project authors (see CONTRIBUTORS.md)
//
// SPDX-License-Identifier: MIT

package edu.stanford.myheartcounts.di

import edu.stanford.myheartcounts.StudyAppBarProvider
import edu.stanford.myheartcounts.StudyAppBarProviderImpl
import edu.stanford.myheartcounts.home.HomeContentMapper
import edu.stanford.myheartcounts.home.HomeContentMapperImpl
import edu.stanford.myheartcounts.home.HomeSuggestionsSource
import edu.stanford.myheartcounts.home.HomeTasksSource
import edu.stanford.myheartcounts.home.HomeTasksSourceImpl
import edu.stanford.myheartcounts.home.NoHomeSuggestionsSource
import edu.stanford.myheartcounts.navigation.Navigator
import edu.stanford.myheartcounts.navigation.NavigatorImpl
import edu.stanford.myheartcounts.notification.NotificationPermissionHandler
import edu.stanford.myheartcounts.notification.NotificationPermissionHandlerImpl
import edu.stanford.myheartcounts.notification.PermissionChecker
import edu.stanford.myheartcounts.notification.PermissionCheckerImpl
import edu.stanford.myheartcounts.onboarding.OnboardingStepLayoutMapper
import edu.stanford.myheartcounts.onboarding.OnboardingStepLayoutMapperImpl
import edu.stanford.myheartcounts.onboarding.OnboardingStepProvider
import edu.stanford.myheartcounts.onboarding.OnboardingStepProviderImpl
import edu.stanford.myheartcounts.onboarding.comprehension.ConsentSurveyLayoutMapper
import edu.stanford.myheartcounts.onboarding.comprehension.ConsentSurveyLayoutMapperImpl
import edu.stanford.myheartcounts.onboarding.eligibility.EligibilityLayoutMapper
import edu.stanford.myheartcounts.onboarding.eligibility.EligibilityLayoutMapperImpl
import edu.stanford.myheartcounts.study.MHCStudyBundleProvider
import edu.stanford.myheartcounts.study.MHCStudyBundleProviderImpl
import edu.stanford.myheartcounts.study.StudyArticleSource
import edu.stanford.myheartcounts.study.StudyArticleSourceImpl
import edu.stanford.myheartcounts.study.StudyEnroller
import edu.stanford.myheartcounts.study.StudyEnrollerImpl
import edu.stanford.spezi.core.ConfigurationBuilder
import edu.stanford.spezi.core.SpeziDsl
import edu.stanford.spezi.core.coroutines.Concurrency
import edu.stanford.spezi.storage.local.KeyValueStorageFactory
import edu.stanford.spezi.storage.local.KeyValueStorageType

/**
 * Registers the app's services: navigation, notifications, onboarding, and storage.
 */
@SpeziDsl
fun ConfigurationBuilder.appConfigurations() {
    homeConfigurations()
    singleton<Navigator> { NavigatorImpl() }
    singleton<StudyAppBarProvider> {
        StudyAppBarProviderImpl(navigator = dependency())
    }

    singleton<PermissionChecker> {
        PermissionCheckerImpl(
            context = appContext(),
        )
    }
    singleton<NotificationPermissionHandler> {
        NotificationPermissionHandlerImpl(
            permissionChecker = dependency(),
            storage = dependency(),
        )
    }

    factory<OnboardingStepProvider> {
        OnboardingStepProviderImpl(
            account = dependency(),
            notificationPermissionHandler = dependency(),
        )
    }
    factory<EligibilityLayoutMapper> { EligibilityLayoutMapperImpl() }
    factory<ConsentSurveyLayoutMapper> { ConsentSurveyLayoutMapperImpl() }
    factory<OnboardingStepLayoutMapper> {
        OnboardingStepLayoutMapperImpl(
            eligibilityLayoutMapper = dependency(),
            consentSurveyLayoutMapper = dependency(),
        )
    }

    singleton {
        val storageFactory = dependency<KeyValueStorageFactory>()
        storageFactory.create(
            fileName = "edu.stanford.myheartcounts.STORAGE",
            type = KeyValueStorageType.UNENCRYPTED,
        )
    }

    studyConfigurations()
}

/**
 * Registers study bundle loading and enrollment.
 */
private fun ConfigurationBuilder.studyConfigurations() {
    module<MHCStudyBundleProvider> {
        MHCStudyBundleProviderImpl(
            context = appContext(),
            studyManager = dependency(),
            scope = dependency<Concurrency>().ioCoroutineScope(),
        )
    }

    factory<StudyArticleSource> {
        StudyArticleSourceImpl(
            studyBundleProvider = dependency(),
            ioDispatcher = dependency<Concurrency>().ioDispatcher(),
        )
    }

    factory<StudyEnroller> {
        StudyEnrollerImpl(
            studyBundleProvider = dependency(),
            studyManager = dependency(),
            account = dependency(),
            timeProvider = dependency(),
        )
    }
}

/**
 * Registers the Home tab's data sources and layout mapper.
 */
private fun ConfigurationBuilder.homeConfigurations() {
    factory<HomeContentMapper> { HomeContentMapperImpl(scheduleCalculator = dependency()) }
    factory<HomeTasksSource> {
        HomeTasksSourceImpl(
            studyManager = dependency(),
            scheduler = dependency(),
            timeProvider = dependency(),
        )
    }
    singleton<HomeSuggestionsSource> { NoHomeSuggestionsSource() }
}
