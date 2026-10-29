//
// This source file is part of the My Heart Counts Android open-source project
//
// SPDX-FileCopyrightText: 2026 Stanford University and the project authors (see CONTRIBUTORS.md)
//
// SPDX-License-Identifier: MIT

package edu.stanford.myheartcounts

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.provider.Settings
import androidx.activity.compose.setContent
import androidx.appcompat.app.AppCompatActivity
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.navigation3.rememberViewModelStoreNavEntryDecorator
import androidx.navigation3.runtime.NavBackStack
import androidx.navigation3.runtime.NavKey
import androidx.navigation3.runtime.entryProvider
import androidx.navigation3.runtime.rememberNavBackStack
import androidx.navigation3.runtime.rememberSaveableStateHolderNavEntryDecorator
import androidx.navigation3.ui.NavDisplay
import edu.stanford.myheartcounts.account.overview.MHCAccountOverviewScreen
import edu.stanford.myheartcounts.dashboard.detail.HeartScoreDetailScreen
import edu.stanford.myheartcounts.dashboard.entry.HeartScoreEntryScreen
import edu.stanford.myheartcounts.dashboard.pastdata.PastTimedWalkTestResultsScreen
import edu.stanford.myheartcounts.debug.DebugHost
import edu.stanford.myheartcounts.demographics.ui.DemographicsScreen
import edu.stanford.myheartcounts.demographics.ui.DemographicsScreenMode
import edu.stanford.myheartcounts.missed.MissedTasksScreen
import edu.stanford.myheartcounts.navigation.BottomSheetSceneStrategy
import edu.stanford.myheartcounts.navigation.MHCRoute
import edu.stanford.myheartcounts.navigation.NavigationEvent
import edu.stanford.myheartcounts.navigation.Navigator
import edu.stanford.myheartcounts.notification.MHCNotificationTracking
import edu.stanford.myheartcounts.notification.MHCNudgeNotifications.Companion.FCM_MESSAGE_ID_EXTRA
import edu.stanford.myheartcounts.onboarding.OnboardingScreen
import edu.stanford.myheartcounts.stats.ParticipationStatsScreen
import edu.stanford.myheartcounts.study.StudyScreen
import edu.stanford.myheartcounts.study.StudyVariantProvider
import edu.stanford.myheartcounts.task.PerformTaskScreen
import edu.stanford.myheartcounts.ui.MHCAppTheme
import kotlinx.coroutines.launch
import org.grovealliance.core.coroutines.Concurrency
import org.grovealliance.core.dependency
import org.grovealliance.core.viewmodel.groveViewModel
import org.grovealliance.ui.ConsumeEvents
import org.grovealliance.ui.crossFade
import org.grovealliance.ui.horizontalSlideBackward
import org.grovealliance.ui.horizontalSlideForward
import org.grovealliance.ui.verticalModalEnter

/**
 * The single activity hosting the app. It renders the active top-level route in a [NavDisplay] and
 * applies navigation requests collected from the [Navigator] to the back stack.
 */
class MainActivity : AppCompatActivity() {

    private val navigator by dependency<Navigator>()
    private val sceneStrategies = listOf(BottomSheetSceneStrategy<NavKey>())
    private val notificationTracking by dependency<MHCNotificationTracking>()
    private val concurrency by dependency<Concurrency>()
    private val studyVariants by dependency<StudyVariantProvider>()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // A recreated activity is handed its launch intent again, and that open was already recorded.
        if (savedInstanceState == null) trackNotificationOpen(intent = intent)

        setContent {
            MHCAppTheme {
                AppContent()
            }
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        // The activity is `singleInstance`, so a notification tapped while the app is already
        // running arrives here rather than through `onCreate`.
        setIntent(intent)
        trackNotificationOpen(intent = intent)
    }

    /**
     * Records that the participant opened a remote nudge, when this launch came from one.
     *
     * Firebase Cloud Messaging puts its own extras on the launch intent of a notification it
     * displayed, so their presence is what distinguishes a tap on a nudge from an ordinary launch.
     */
    private fun trackNotificationOpen(intent: Intent?) {
        val notificationId = intent?.extras?.getString(FCM_MESSAGE_ID_EXTRA) ?: return
        val payload = intent.extras
            ?.keySet()
            .orEmpty()
            .filterNot { it.startsWith(FCM_INTERNAL_EXTRA_PREFIX) }
            .mapNotNull { key -> intent.extras?.getString(key)?.let { key to it } }
            .toMap()
        // Consumed, so that nothing handed this intent again can count the same open twice.
        intent.removeExtra(FCM_MESSAGE_ID_EXTRA)

        concurrency.ioCoroutineScope().launch {
            notificationTracking.trackDidOpen(notificationId = notificationId, payload = payload)
        }
    }

    /**
     * Starts the app's launch activity in a new task and ends this process, so the app starts over.
     */
    private fun restartApp() {
        val launch = packageManager.getLaunchIntentForPackage(packageName) ?: return
        startActivity(launch.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK))
        finishAffinity()
        Runtime.getRuntime().exit(0)
    }

    @Composable
    private fun AppContent() {
        val viewModel = groveViewModel<MainActivityViewModel>()
        val state by viewModel.state.collectAsStateWithLifecycle()
        val backStack = rememberNavBackStack(state.initialRoot)
        NavigationEvents(backStack = backStack)

        NavDisplay(
            backStack = backStack,
            sceneStrategies = sceneStrategies,
            transitionSpec = { horizontalSlideForward },
            popTransitionSpec = { horizontalSlideBackward },
            predictivePopTransitionSpec = { horizontalSlideBackward },
            entryDecorators = listOf(
                rememberSaveableStateHolderNavEntryDecorator(),
                rememberViewModelStoreNavEntryDecorator(),
            ),
            entryProvider = entryProvider {
                entry<MHCRoute.Root.Onboarding>(
                    metadata = NavDisplay.transitionSpec { crossFade },
                ) {
                    OnboardingScreen()
                }
                entry<MHCRoute.Root.Study>(
                    metadata = NavDisplay.transitionSpec { verticalModalEnter },
                ) {
                    StudyScreen()
                }
                entry<MHCRoute.AccountOverview> {
                    MHCAccountOverviewScreen()
                }
                entry<MHCRoute.Demographics> {
                    DemographicsScreen(mode = DemographicsScreenMode.Complete)
                }
                entry<MHCRoute.PerformTask> {
                    PerformTaskScreen()
                }
                entry<MHCRoute.MissedTasks> {
                    MissedTasksScreen()
                }
                entry<MHCRoute.HeartScoreDetail>(metadata = BottomSheetSceneStrategy.sheet()) { route ->
                    HeartScoreDetailScreen(component = route.component)
                }
                entry<MHCRoute.HeartScoreEntry>(metadata = BottomSheetSceneStrategy.sheet()) { route ->
                    HeartScoreEntryScreen(component = route.component)
                }
                entry<MHCRoute.PastTimedWalkTestResults> {
                    PastTimedWalkTestResultsScreen()
                }
                entry<MHCRoute.ParticipationStats>(metadata = BottomSheetSceneStrategy.sheet()) {
                    ParticipationStatsScreen()
                }
            }
        )

        DebugHost()
    }

    @Composable
    private fun NavigationEvents(backStack: NavBackStack<NavKey>) {
        ConsumeEvents(navigator.events) { event ->
            when (event) {
                is NavigationEvent.Pop -> backStack.removeLastOrNull()
                is NavigationEvent.SwitchRoot -> {
                    backStack.clear()
                    backStack.add(event.root)
                }

                is NavigationEvent.LaunchAppSettings -> {
                    val intent = Intent(event.action).apply {
                        putExtra(Settings.EXTRA_APP_PACKAGE, packageName)
                        addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                    }
                    startActivity(intent)
                }

                is NavigationEvent.OpenUrl -> openUrl(url = event.url)
                NavigationEvent.OpenStudyHomepage -> openUrl(url = studyVariants.currentVariant.homepage)
                NavigationEvent.OpenPrivacyPolicy -> openUrl(url = studyVariants.currentVariant.privacyPolicy)

                is NavigationEvent.ShareFile -> {
                    val share = Intent(Intent.ACTION_SEND).apply {
                        type = event.mimeType
                        putExtra(Intent.EXTRA_STREAM, Uri.parse(event.uri))
                        addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                    }
                    startActivity(Intent.createChooser(share, null))
                }

                NavigationEvent.RestartApp -> restartApp()

                is NavigationEvent.NavigateTo -> backStack.add(event.route)
                is NavigationEvent.Close -> backStack.remove(event.route)
            }
        }
    }

    private fun openUrl(url: String) {
        val intent = Intent(Intent.ACTION_VIEW, Uri.parse(url)).apply {
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }
        startActivity(intent)
    }
}

/**
 * Firebase's own bookkeeping extras, which say nothing about the nudge itself.
 */
private const val FCM_INTERNAL_EXTRA_PREFIX = "google."
