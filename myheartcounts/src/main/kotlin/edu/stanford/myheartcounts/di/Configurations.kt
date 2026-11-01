//
// This source file is part of the My Heart Counts Android open-source project
//
// SPDX-FileCopyrightText: 2026 Stanford University and the project authors (see CONTRIBUTORS.md)
//
// SPDX-License-Identifier: MIT

package edu.stanford.myheartcounts.di

import android.os.Build
import edu.stanford.myheartcounts.BuildConfig
import edu.stanford.myheartcounts.StudyAppBarProvider
import edu.stanford.myheartcounts.StudyAppBarProviderImpl
import edu.stanford.myheartcounts.account.PostTrialNudges
import edu.stanford.myheartcounts.account.PostTrialNudgesImpl
import edu.stanford.myheartcounts.account.StudyWithdrawal
import edu.stanford.myheartcounts.account.StudyWithdrawalImpl
import edu.stanford.myheartcounts.account.feedback.MHCFeedback
import edu.stanford.myheartcounts.account.feedback.MHCFeedbackImpl
import edu.stanford.myheartcounts.account.overview.MHCAccountOverviewMapper
import edu.stanford.myheartcounts.account.overview.MHCAccountOverviewMapperImpl
import edu.stanford.myheartcounts.customsamples.CustomSampleDecoder
import edu.stanford.myheartcounts.customsamples.CustomSampleDecoderImpl
import edu.stanford.myheartcounts.customsamples.CustomSampleSource
import edu.stanford.myheartcounts.customsamples.CustomSampleSourceImpl
import edu.stanford.myheartcounts.dashboard.HeartHealthContentMapper
import edu.stanford.myheartcounts.dashboard.HeartHealthContentMapperImpl
import edu.stanford.myheartcounts.dashboard.HeartHealthExplainerSource
import edu.stanford.myheartcounts.dashboard.HeartHealthExplainerSourceImpl
import edu.stanford.myheartcounts.dashboard.HeartHealthQueryRanges
import edu.stanford.myheartcounts.dashboard.HeartHealthQueryRangesImpl
import edu.stanford.myheartcounts.dashboard.HeartHealthScoreCalculator
import edu.stanford.myheartcounts.dashboard.HeartHealthScoreCalculatorImpl
import edu.stanford.myheartcounts.dashboard.HeartHealthScoreSource
import edu.stanford.myheartcounts.dashboard.HeartHealthScoreSourceImpl
import edu.stanford.myheartcounts.dashboard.HeartScoreDateFormatter
import edu.stanford.myheartcounts.dashboard.HeartScoreDateFormatterImpl
import edu.stanford.myheartcounts.featureflag.AppEnvironment
import edu.stanford.myheartcounts.firebase.FirebaseSchema
import edu.stanford.myheartcounts.firebase.MHCCloudFunctions
import edu.stanford.myheartcounts.firebase.MHCCloudFunctionsImpl
import edu.stanford.myheartcounts.firebase.MHCFirestore
import edu.stanford.myheartcounts.healthstats.HealthStatsCheckpointStore
import edu.stanford.myheartcounts.healthstats.HealthStatsCheckpointStoreImpl
import edu.stanford.myheartcounts.healthstats.HealthStatsDocumentDecoder
import edu.stanford.myheartcounts.healthstats.HealthStatsDocumentDecoderImpl
import edu.stanford.myheartcounts.healthstats.HealthStatsDocumentEncoder
import edu.stanford.myheartcounts.healthstats.HealthStatsDocumentEncoderImpl
import edu.stanford.myheartcounts.healthstats.HealthStatsEntryDecoder
import edu.stanford.myheartcounts.healthstats.HealthStatsEntryDecoderImpl
import edu.stanford.myheartcounts.healthstats.HealthStatsHistory
import edu.stanford.myheartcounts.healthstats.HealthStatsHistoryImpl
import edu.stanford.myheartcounts.healthstats.HealthStatsIntervalReducer
import edu.stanford.myheartcounts.healthstats.HealthStatsIntervalReducerImpl
import edu.stanford.myheartcounts.healthstats.HealthStatsMonthCollector
import edu.stanford.myheartcounts.healthstats.HealthStatsMonthCollectorImpl
import edu.stanford.myheartcounts.healthstats.HealthStatsMonthlySync
import edu.stanford.myheartcounts.healthstats.HealthStatsMonthlySyncImpl
import edu.stanford.myheartcounts.healthstats.HealthStatsMonths
import edu.stanford.myheartcounts.healthstats.HealthStatsMonthsImpl
import edu.stanford.myheartcounts.healthstats.HealthStatsPersistence
import edu.stanford.myheartcounts.healthstats.HealthStatsPersistenceImpl
import edu.stanford.myheartcounts.healthstats.HealthStatsProcessor
import edu.stanford.myheartcounts.healthstats.HealthStatsProcessorImpl
import edu.stanford.myheartcounts.healthstats.HealthStatsRefresher
import edu.stanford.myheartcounts.healthstats.HealthStatsRefresherImpl
import edu.stanford.myheartcounts.healthstats.HealthStatsSourceSelector
import edu.stanford.myheartcounts.healthstats.HealthStatsSourceSelectorImpl
import edu.stanford.myheartcounts.healthstats.HealthStatsStore
import edu.stanford.myheartcounts.healthstats.HealthStatsStoreImpl
import edu.stanford.myheartcounts.healthstats.HealthStatsSync
import edu.stanford.myheartcounts.healthstats.HealthStatsSyncConfiguration
import edu.stanford.myheartcounts.healthstats.HealthStatsSyncEnvironment
import edu.stanford.myheartcounts.healthstats.HealthStatsSyncEnvironmentImpl
import edu.stanford.myheartcounts.healthstats.HealthStatsSyncImpl
import edu.stanford.myheartcounts.healthstats.HealthStatsSyncScheduler
import edu.stanford.myheartcounts.healthstats.HealthStatsSyncSchedulerImpl
import edu.stanford.myheartcounts.healthstats.HealthStatsWorkoutMapper
import edu.stanford.myheartcounts.healthstats.HealthStatsWorkoutMapperImpl
import edu.stanford.myheartcounts.healthstats.HealthStatsWriter
import edu.stanford.myheartcounts.healthstats.HealthStatsWriterImpl
import edu.stanford.myheartcounts.home.FirebaseHomeSuggestionsSource
import edu.stanford.myheartcounts.home.HomeContentMapper
import edu.stanford.myheartcounts.home.HomeContentMapperImpl
import edu.stanford.myheartcounts.home.HomeSuggestionsSource
import edu.stanford.myheartcounts.home.HomeTasksSource
import edu.stanford.myheartcounts.home.HomeTasksSourceImpl
import edu.stanford.myheartcounts.missed.MissedTasksMapper
import edu.stanford.myheartcounts.missed.MissedTasksMapperImpl
import edu.stanford.myheartcounts.navigation.Navigator
import edu.stanford.myheartcounts.navigation.NavigatorImpl
import edu.stanford.myheartcounts.notification.MHCNotificationTracking
import edu.stanford.myheartcounts.notification.MHCNudgeNotifications
import edu.stanford.myheartcounts.notification.MHCPushTokenSynchronizer
import edu.stanford.myheartcounts.onboarding.OnboardingCompletion
import edu.stanford.myheartcounts.onboarding.OnboardingCompletionImpl
import edu.stanford.myheartcounts.onboarding.OnboardingProgressTracker
import edu.stanford.myheartcounts.onboarding.OnboardingProgressTrackerImpl
import edu.stanford.myheartcounts.onboarding.OnboardingStepLayoutMapper
import edu.stanford.myheartcounts.onboarding.OnboardingStepLayoutMapperImpl
import edu.stanford.myheartcounts.onboarding.OnboardingStepProvider
import edu.stanford.myheartcounts.onboarding.OnboardingStepProviderImpl
import edu.stanford.myheartcounts.onboarding.comprehension.ConsentSurveyLayoutMapper
import edu.stanford.myheartcounts.onboarding.comprehension.ConsentSurveyLayoutMapperImpl
import edu.stanford.myheartcounts.onboarding.eligibility.EligibilityLayoutMapper
import edu.stanford.myheartcounts.onboarding.eligibility.EligibilityLayoutMapperImpl
import edu.stanford.myheartcounts.standard.MHCAppRevision
import edu.stanford.myheartcounts.standard.MHCEnvironmentTracker
import edu.stanford.myheartcounts.standard.MHCQuestionnaireUploader
import edu.stanford.myheartcounts.standard.QuestionnaireResponseUploader
import edu.stanford.myheartcounts.standard.health.HealthAccess
import edu.stanford.myheartcounts.standard.health.HealthAccessImpl
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
import edu.stanford.myheartcounts.study.StudyQuestionnaireSource
import edu.stanford.myheartcounts.study.StudyQuestionnaireSourceImpl
import edu.stanford.myheartcounts.study.StudyVariantProvider
import edu.stanford.myheartcounts.study.StudyVariantProviderImpl
import edu.stanford.myheartcounts.study.StudyVariantSynchronizer
import edu.stanford.myheartcounts.task.EventPerformer
import edu.stanford.myheartcounts.task.EventPerformerImpl
import edu.stanford.myheartcounts.task.EventTileMapper
import edu.stanford.myheartcounts.task.EventTileMapperImpl
import edu.stanford.myheartcounts.task.MissedEventsSource
import edu.stanford.myheartcounts.task.MissedEventsSourceImpl
import edu.stanford.myheartcounts.task.TaskPerformer
import edu.stanford.myheartcounts.task.TaskPerformerImpl
import edu.stanford.myheartcounts.ui.ArticleMapper
import edu.stanford.myheartcounts.ui.ArticleMapperImpl
import edu.stanford.myheartcounts.upcoming.UpcomingTasksMapper
import edu.stanford.myheartcounts.upcoming.UpcomingTasksMapperImpl
import edu.stanford.myheartcounts.upcoming.UpcomingTasksSource
import edu.stanford.myheartcounts.upcoming.UpcomingTasksSourceImpl
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
    demographicsConfigurations()
    participationStatsConfigurations()
    singleton<Navigator> { NavigatorImpl() }
    homeConfigurations()
    taskConfigurations()
    singleton<StudyAppBarProvider> {
        StudyAppBarProviderImpl(navigator = dependency())
    }

    permissionConfigurations()
    singleton<OnboardingCompletion> {
        OnboardingCompletionImpl(storage = dependency())
    }
    factory<OnboardingProgressTracker> {
        OnboardingProgressTrackerImpl(
            account = dependency(),
            accountService = dependency(),
        )
    }

    factory<OnboardingStepProvider> {
        OnboardingStepProviderImpl(
            account = dependency(),
            notificationPermissionHandler = dependency(),
            consentSigning = dependency(),
            featureFlags = dependency(),
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

    // Every build, release builds included, runs as Testing for now, so that test builds keep the
    // changeable feature flags and the debug sheet opened by shaking the device. Keep it as is
    // until the switch back to Production before the actual release (ROADMAP T65).
    singleton { AppEnvironment.Testing }
    featureFlagConfigurations()
    debugConfigurations()
    studyConfigurations()
    firebaseConfigurations()
    healthConfigurations()
    healthStatsConfigurations()
    customSampleConfigurations()
}

/**
 * Registers the Firebase-backed services the standard delivers data through.
 */
private fun ConfigurationBuilder.firebaseConfigurations() {
    module { AppLifecycle() }
    module {
        MHCFirestore(
            account = dependency(),
            firebaseAppConfiguration = dependency(),
        )
    }
    singleton<MHCCloudFunctions> {
        MHCCloudFunctionsImpl(firebaseAppConfiguration = dependency())
    }
    accountOverviewConfigurations()
    notificationConfigurations()
    singleton {
        MHCAppRevision(
            version = BuildConfig.VERSION_NAME,
            build = BuildConfig.VERSION_CODE,
            bundleIdentifier = BuildConfig.APPLICATION_ID,
            osVersion = Build.VERSION.RELEASE,
            deviceModel = Build.MODEL,
            deviceType = Build.DEVICE,
        )
    }
    module<QuestionnaireResponseUploader> {
        MHCQuestionnaireUploader(
            firestore = dependency(),
            appRevision = dependency(),
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

    consentConfigurations()
}

/**
 * Registers what the account overview offers beyond the account itself.
 */
private fun ConfigurationBuilder.accountOverviewConfigurations() {
    factory<MHCAccountOverviewMapper> {
        MHCAccountOverviewMapperImpl(appRevision = dependency())
    }
    factory<StudyWithdrawal> {
        StudyWithdrawalImpl(
            cloudFunctions = dependency(),
            accountService = dependency(),
        )
    }
    factory<PostTrialNudges> { PostTrialNudgesImpl(account = dependency()) }
    factory<MHCFeedback> {
        MHCFeedbackImpl(
            firestore = dependency(),
            appRevision = dependency(),
            timeProvider = dependency(),
        )
    }
}

/**
 * Registers remote notifications: the push token, displaying nudges, and tracking their opening.
 */
private fun ConfigurationBuilder.notificationConfigurations() {
    module {
        MHCPushTokenSynchronizer(
            account = dependency(),
            firebaseAppConfiguration = dependency(),
        )
    }
    module {
        MHCNudgeNotifications(
            context = appContext(),
            notificationPermissionHandler = dependency(),
        )
    }
    module {
        MHCNotificationTracking(
            firestore = dependency(),
            timeProvider = dependency(),
        )
    }
}

/**
 * Registers Health Connect access and the collection pipeline: staging, FHIR mapping, and upload.
 */
private fun ConfigurationBuilder.healthConfigurations() {
    module<HealthAccess> {
        HealthAccessImpl(
            health = dependency(),
            application = application(),
        )
    }
    singleton { HealthStagingDatabase.create(context = appContext()) }
    singleton { HealthObservationMapper() }
    singleton {
        ManagedFileUpload(
            context = appContext(),
            firestore = dependency(),
            concurrency = dependency(),
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
                    appRevision = dependency(),
                    studyId = enrollment?.studyId?.toString(),
                    studyRevision = enrollment?.studyRevision?.toInt(),
                )
            },
        )
    }
}

/**
 * Registers the reading of the participant's monthly health statistics.
 */
private fun ConfigurationBuilder.healthStatsConfigurations() {
    factory<HealthStatsEntryDecoder> { HealthStatsEntryDecoderImpl() }
    factory<HealthStatsDocumentDecoder> { HealthStatsDocumentDecoderImpl(entryDecoder = dependency()) }
    factory<HealthStatsMonths> { HealthStatsMonthsImpl(timeProvider = dependency()) }
    factory<HealthStatsIntervalReducer> { HealthStatsIntervalReducerImpl(timeProvider = dependency()) }
    factory<HealthStatsSourceSelector> { HealthStatsSourceSelectorImpl() }
    factory<HealthStatsProcessor> {
        HealthStatsProcessorImpl(
            sourceSelector = dependency(),
            intervalReducer = dependency(),
        )
    }
    singleton<HealthStatsStore> {
        HealthStatsStoreImpl(
            account = dependency(),
            firestore = dependency(),
            months = dependency(),
            decoder = dependency(),
            processor = dependency(),
            processingDispatcher = dependency<Concurrency>().defaultDispatcher(),
        )
    }
    healthStatsPersistenceConfigurations()
    healthStatsCollectionConfigurations()
    healthStatsSyncConfigurations()
}

/**
 * Registers the reading of the participant's survey-derived and entered health values.
 */
private fun ConfigurationBuilder.customSampleConfigurations() {
    factory<CustomSampleDecoder> { CustomSampleDecoderImpl(timeProvider = dependency()) }
    singleton<CustomSampleSource> {
        CustomSampleSourceImpl(
            account = dependency(),
            firestore = dependency(),
            decoder = dependency(),
            processingDispatcher = dependency<Concurrency>().defaultDispatcher(),
        )
    }
}

/**
 * Registers study bundle loading and enrollment.
 */
private fun ConfigurationBuilder.studyConfigurations() {
    singleton<StudyVariantProvider> { StudyVariantProviderImpl(storageFactory = dependency()) }
    module {
        StudyVariantSynchronizer(
            account = dependency(),
            accountService = dependency(),
            variantProvider = dependency(),
            studyManager = dependency(),
            scope = dependency<Concurrency>().ioCoroutineScope(),
        )
    }
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
            variantProvider = dependency(),
            ioDispatcher = dependency<Concurrency>().ioDispatcher(),
        )
    }

    factory<StudyQuestionnaireSource> {
        StudyQuestionnaireSourceImpl(
            studyBundleProvider = dependency(),
            variantProvider = dependency(),
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
 * Registers how tasks are listed and performed, shared by every screen that offers them.
 */
private fun ConfigurationBuilder.taskConfigurations() {
    factory<ArticleMapper> {
        ArticleMapperImpl(
            timeProvider = dependency(),
            variantProvider = dependency(),
        )
    }
    singleton<TaskPerformer> { TaskPerformerImpl(navigator = dependency()) }
    factory<EventTileMapper> {
        EventTileMapperImpl(
            scheduleCalculator = dependency(),
            timeProvider = dependency(),
        )
    }
    factory<MissedEventsSource> {
        MissedEventsSourceImpl(
            scheduler = dependency(),
            scheduleCalculator = dependency(),
            timeProvider = dependency(),
        )
    }
    factory<EventPerformer> {
        EventPerformerImpl(
            articleSource = dependency(),
            articleMapper = dependency(),
            questionnaireSource = dependency(),
            taskPerformer = dependency(),
            scheduler = dependency(),
        )
    }
}

/**
 * Registers the Home tab's data sources and layout mapper.
 */
private fun ConfigurationBuilder.homeConfigurations() {
    factory<HomeContentMapper> { HomeContentMapperImpl(tileMapper = dependency()) }
    factory<HomeTasksSource> {
        HomeTasksSourceImpl(
            studyManager = dependency(),
            scheduler = dependency(),
            missedEventsSource = dependency(),
            timeProvider = dependency(),
        )
    }
    factory<MissedTasksMapper> { MissedTasksMapperImpl(tileMapper = dependency()) }
    factory<UpcomingTasksSource> {
        UpcomingTasksSourceImpl(
            scheduler = dependency(),
            timeProvider = dependency(),
        )
    }
    factory<UpcomingTasksMapper> { UpcomingTasksMapperImpl(tileMapper = dependency()) }
    singleton<HomeSuggestionsSource> {
        FirebaseHomeSuggestionsSource(
            firestore = dependency(),
            timeProvider = dependency(),
        )
    }
    heartHealthConfigurations()
}

/**
 * Registers the Heart Health tab's score and explainer sources and its layout mapper.
 */
private fun ConfigurationBuilder.heartHealthConfigurations() {
    factory<HeartHealthQueryRanges> { HeartHealthQueryRangesImpl(timeProvider = dependency()) }
    factory<HeartHealthScoreCalculator> { HeartHealthScoreCalculatorImpl(timeProvider = dependency()) }
    singleton<HeartHealthScoreSource> {
        HeartHealthScoreSourceImpl(
            account = dependency(),
            stats = dependency(),
            customSamples = dependency(),
            health = dependency(),
            calculator = dependency(),
            ranges = dependency(),
        )
    }
    factory<HeartHealthExplainerSource> {
        HeartHealthExplainerSourceImpl(
            studyBundleProvider = dependency(),
            variantProvider = dependency(),
            ioDispatcher = dependency<Concurrency>().ioDispatcher(),
        )
    }
    factory<HeartScoreDateFormatter> { HeartScoreDateFormatterImpl(timeProvider = dependency()) }
    factory<HeartHealthContentMapper> { HeartHealthContentMapperImpl(dateFormatter = dependency()) }
    heartScoreDetailConfigurations()
}

private fun ConfigurationBuilder.healthStatsPersistenceConfigurations() {
    singleton {
        HealthStatsSyncConfiguration(
            sourceId = FirebaseSchema.StatsSource.HEALTH_CONNECT,
            deriveExerciseTime = true,
            deriveBodyMassIndex = true,
            sleepDataOrigin = null,
        )
    }
    factory<HealthStatsDocumentEncoder> {
        HealthStatsDocumentEncoderImpl(
            timeProvider = dependency(),
            entryDecoder = dependency(),
        )
    }
    factory<HealthStatsHistory> { HealthStatsHistoryImpl(timeProvider = dependency()) }
    singleton<HealthStatsWriter> {
        HealthStatsWriterImpl(
            firestore = dependency(),
            encoder = dependency(),
            account = dependency(),
        )
    }
    singleton<HealthStatsCheckpointStore> {
        HealthStatsCheckpointStoreImpl(
            storage = dependency<KeyValueStorageFactory>().create(fileName = "mhc-health-stats-sync", type = KeyValueStorageType.ENCRYPTED),
            account = dependency(),
        )
    }
}

private fun ConfigurationBuilder.healthStatsCollectionConfigurations() {
    factory<HealthStatsWorkoutMapper> { HealthStatsWorkoutMapperImpl() }
    factory<HealthStatsMonthCollector> {
        HealthStatsMonthCollectorImpl(
            health = dependency(),
            workoutMapper = dependency(),
            configuration = dependency(),
        )
    }
    singleton<HealthStatsPersistence> {
        HealthStatsPersistenceImpl(
            writer = dependency(),
            checkpoints = dependency(),
            account = dependency(),
        )
    }
    factory<HealthStatsSyncEnvironment> {
        HealthStatsSyncEnvironmentImpl(
            health = dependency(),
            timeProvider = dependency(),
            appLifecycle = dependency(),
            storage = dependency<KeyValueStorageFactory>().create(
                fileName = "mhc-health-read-access",
                type = KeyValueStorageType.UNENCRYPTED,
            ),
        )
    }
    factory<HealthStatsMonthlySync> {
        HealthStatsMonthlySyncImpl(
            health = dependency(),
            collector = dependency(),
            persistence = dependency(),
            environment = dependency(),
            configuration = dependency(),
            account = dependency(),
        )
    }
}

private fun ConfigurationBuilder.healthStatsSyncConfigurations() {
    singleton<HealthStatsRefresher> {
        HealthStatsRefresherImpl(
            account = dependency(),
            history = dependency(),
            health = dependency(),
            monthlySync = dependency(),
            configuration = dependency(),
            writer = dependency(),
        )
    }
    factory<HealthStatsSyncScheduler> { HealthStatsSyncSchedulerImpl(context = appContext()) }
    singleton<HealthStatsSync> {
        HealthStatsSyncImpl(
            account = dependency(),
            studyManager = dependency(),
            appLifecycle = dependency(),
            refresher = dependency(),
            scheduler = dependency(),
            persistence = dependency(),
            scope = dependency<Concurrency>().ioCoroutineScope(),
        )
    }
}
