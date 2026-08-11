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
import edu.stanford.myheartcounts.onboarding.OnboardingScreen
import edu.stanford.myheartcounts.splash.SplashScreen
import edu.stanford.myheartcounts.study.StudyScreen
import edu.stanford.myheartcounts.ui.MHCAppTheme
import edu.stanford.spezi.account.AccountOverviewScreen
import edu.stanford.spezi.core.dependency
import edu.stanford.spezi.core.viewmodel.speziViewModel
import edu.stanford.spezi.ui.ConsumeEvents
import edu.stanford.spezi.ui.crossFade
import edu.stanford.spezi.ui.horizontalSlideBackward
import edu.stanford.spezi.ui.horizontalSlideForward
import edu.stanford.spezi.ui.verticalModalEnter

/**
 * The single activity hosting the app. It renders the active top-level route in a [NavDisplay] and
 * applies navigation requests collected from the [Navigator] to the back stack.
 */
class MainActivity : AppCompatActivity() {

    private val navigator by dependency<Navigator>()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setContent {
            MHCAppTheme {
                AppContent()
            }
        }
    }

    @Composable
    private fun AppContent() {
        val viewModel = speziViewModel<MainActivityViewModel>()
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
