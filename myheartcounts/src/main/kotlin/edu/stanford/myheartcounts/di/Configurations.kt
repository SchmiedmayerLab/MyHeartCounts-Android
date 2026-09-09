//
// This source file is part of the My Heart Counts Android open-source project
//
// SPDX-FileCopyrightText: 2026 Stanford University and the project authors (see CONTRIBUTORS.md)
//
// SPDX-License-Identifier: MIT

package edu.stanford.myheartcounts.di

import edu.stanford.myheartcounts.BuildConfig
import edu.stanford.myheartcounts.StudyAppBarProvider
import edu.stanford.myheartcounts.StudyAppBarProviderImpl
import edu.stanford.myheartcounts.firebase.MHCCloudFunctions
import edu.stanford.myheartcounts.firebase.MHCCloudFunctionsImpl
import edu.stanford.myheartcounts.firebase.MHCFirebaseRegionInitializer
import edu.stanford.myheartcounts.firebase.MHCFirebaseRegionInitializerImpl
import edu.stanford.myheartcounts.firebase.MHCFirestore
import edu.stanford.myheartcounts.home.FirebaseHomeSuggestionsSource
import edu.stanford.myheartcounts.home.HomeContentMapper
import edu.stanford.myheartcounts.home.HomeContentMapperImpl
import edu.stanford.myheartcounts.home.HomeSuggestionsSource
import edu.stanford.myheartcounts.home.HomeTasksSource
import edu.stanford.myheartcounts.home.HomeTasksSourceImpl
import edu.stanford.myheartcounts.navigation.Navigator
import edu.stanford.myheartcounts.navigation.NavigatorImpl
import edu.stanford.myheartcounts.notification.MHCNotificationTracking
import edu.stanford.myheartcounts.notification.MHCPushTokenSynchronizer
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
import edu.stanford.myheartcounts.standard.MHCEnvironmentTracker
import edu.stanford.myheartcounts.standard.MHCQuestionnaireUploader
import edu.stanford.myheartcounts.standard.consent.ConsentPdfRenderer
import edu.stanford.myheartcounts.standard.consent.MHCConsentDocumentProvider
import edu.stanford.myheartcounts.standard.consent.MHCConsentUploader
import edu.stanford.myheartcounts.standard.health.HealthObservationMapper
import edu.stanford.myheartcounts.standard.health.HealthObservationProvenance
import edu.stanford.myheartcounts.standard.health.HealthStagingDatabase
import edu.stanford.myheartcounts.standard.health.MHCHealthDataHandler
import edu.stanford.myheartcounts.standard.health.ManagedFileUpload
import edu.stanford.myheartcounts.study.MHCStudyBundleProvider
import edu.stanford.myheartcounts.study.MHCStudyBundleProviderImpl
import edu.stanford.myheartcounts.study.StudyArticleSource
import edu.stanford.myheartcounts.study.StudyArticleSourceImpl
import edu.stanford.myheartcounts.study.StudyEnroller
import edu.stanford.myheartcounts.study.StudyEnrollerImpl
import org.grovealliance.core.ConfigurationBuilder
import org.grovealliance.core.GroveDsl
import org.grovealliance.core.coroutines.Concurrency
import org.grovealliance.core.lifecycle.AppLifecycle
import org.grovealliance.storage.local.KeyValueStorageFactory
import org.grovealliance.storage.local.KeyValueStorageType
import org.grovealliance.study.StudyManager

/**
 * Registers the app's services: navigation, notifications, onboarding, and storage.
 */
@GroveDsl
fun ConfigurationBuilder.appConfigurations() {
    homeConfigurations()
    singleton<Navigator> { NavigatorImpl() }
    singleton<StudyAppBarProvider> {
        StudyAppBarProviderImpl(navigator = dependency())
    }

    singleton<MHCFirebaseRegionInitializer> {
        MHCFirebaseRegionInitializerImpl(
            context = appContext(),
            firebaseAppConfiguration = dependency(),
        )
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
    firebaseConfigurations()
    healthConfigurations()
}

/**
 * Registers the Firebase-backed services the standard delivers data through.
 */
private fun ConfigurationBuilder.firebaseConfigurations() {
    module { AppLifecycle() }
    module { MHCFirestore() }
    singleton<MHCCloudFunctions> {
        MHCCloudFunctionsImpl(firebaseAppConfiguration = dependency())
    }
    module {
        MHCPushTokenSynchronizer(
            account = dependency(),
            firebaseAppConfiguration = dependency(),
        )
    }
    module {
        MHCNotificationTracking(
            firestore = dependency(),
            timeProvider = dependency(),
        )
    }
    module {
        MHCQuestionnaireUploader(
            firestore = dependency(),
            appRevision = BuildConfig.VERSION_NAME,
        )
    }
    singleton {
        MHCEnvironmentTracker(
            context = appContext(),
            account = dependency(),
            appLifecycle = dependency(),
            timeProvider = dependency(),
        )
    }

    module { MHCConsentDocumentProvider(studyBundleProvider = dependency()) }
    singleton { ConsentPdfRenderer() }
    module {
        MHCConsentUploader(
            context = appContext(),
            account = dependency(),
            firestore = dependency(),
            renderer = dependency(),
            timeProvider = dependency(),
        )
    }
}

/**
 * Registers the Health Connect collection pipeline: staging, FHIR mapping, and upload.
 */
private fun ConfigurationBuilder.healthConfigurations() {
    singleton { HealthStagingDatabase.create(context = appContext()) }
    singleton { HealthObservationMapper() }
    singleton {
        ManagedFileUpload(
            context = appContext(),
            firestore = dependency(),
        )
    }
    module {
        MHCHealthDataHandler(
            staging = dependency(),
            mapper = dependency(),
            managedUpload = dependency(),
            firestore = dependency(),
            timeProvider = dependency(),
            provenance = {
                // Read per batch rather than captured once, so an enrollment or a study revision
                // that changes mid-session shows up on the samples uploaded after it.
                val enrollment = dependency<StudyManager>().studyEnrollments().firstOrNull()
                HealthObservationProvenance(
                    appRevision = BuildConfig.VERSION_NAME,
                    studyId = enrollment?.studyId?.toString(),
                    studyRevision = enrollment?.studyRevision?.toInt(),
                )
            },
        )
    }
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
    singleton<HomeSuggestionsSource> {
        FirebaseHomeSuggestionsSource(
            firestore = dependency(),
            timeProvider = dependency(),
        )
    }
}
