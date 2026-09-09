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
import edu.stanford.myheartcounts.navigation.MHCRoute
import edu.stanford.myheartcounts.navigation.NavigationEvent
import edu.stanford.myheartcounts.navigation.Navigator
import edu.stanford.myheartcounts.notification.MHCNotificationTracking
import edu.stanford.myheartcounts.onboarding.OnboardingScreen
import edu.stanford.myheartcounts.splash.SplashScreen
import edu.stanford.myheartcounts.study.StudyScreen
import edu.stanford.myheartcounts.ui.MHCAppTheme
import kotlinx.coroutines.launch
import org.grovealliance.account.AccountOverviewScreen
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
    private val notificationTracking by dependency<MHCNotificationTracking>()
    private val concurrency by dependency<Concurrency>()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        trackNotificationOpen(intent = intent)

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

        concurrency.ioCoroutineScope().launch {
            notificationTracking.trackDidOpen(notificationId = notificationId, payload = payload)
        }
    }

    @Composable
    private fun AppContent() {
        val viewModel = groveViewModel<MainActivityViewModel>()
        val state by viewModel.state.collectAsStateWithLifecycle()
        val backStack = rememberNavBackStack(state.initialRoot)
        NavigationEvents(backStack = backStack)

        NavDisplay(
            backStack = backStack,
            transitionSpec = { horizontalSlideForward },
            popTransitionSpec = { horizontalSlideBackward },
            predictivePopTransitionSpec = { horizontalSlideBackward },
            entryDecorators = listOf(
                rememberSaveableStateHolderNavEntryDecorator(),
                rememberViewModelStoreNavEntryDecorator(),
            ),
            entryProvider = entryProvider {
                entry<MHCRoute.Root.Splash> {
                    SplashScreen()
                }
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
                    AccountOverviewScreen(
                        onDismiss = { backStack.remove(MHCRoute.AccountOverview) }
                    )
                }
            }
        )
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

                is NavigationEvent.OpenUrl -> {
                    val intent = Intent(Intent.ACTION_VIEW, Uri.parse(event.url)).apply {
                        addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                    }
                    startActivity(intent)
                }

                is NavigationEvent.NavigateTo -> backStack.add(event.route)
            }
        }
    }
}

private const val FCM_MESSAGE_ID_EXTRA = "google.message_id"

/**
 * Firebase's own bookkeeping extras, which say nothing about the nudge itself.
 */
private const val FCM_INTERNAL_EXTRA_PREFIX = "google."
