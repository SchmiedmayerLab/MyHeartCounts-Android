//
// This source file is part of the My Heart Counts Android open-source project
//
// SPDX-FileCopyrightText: 2026 Stanford University and the project authors (see CONTRIBUTORS.md)
//
// SPDX-License-Identifier: MIT

package org.grovealliance.sample.app

import android.os.Bundle
import androidx.activity.compose.setContent
import androidx.appcompat.app.AppCompatActivity
import androidx.compose.runtime.Composable
import androidx.lifecycle.viewmodel.navigation3.rememberViewModelStoreNavEntryDecorator
import androidx.navigation3.runtime.entryProvider
import androidx.navigation3.runtime.rememberNavBackStack
import androidx.navigation3.runtime.rememberSaveableStateHolderNavEntryDecorator
import androidx.navigation3.ui.NavDisplay
import org.grovealliance.account.AccountLoginScreen
import org.grovealliance.account.AccountOverviewScreen
import org.grovealliance.core.dependency
import org.grovealliance.sample.app.health.HealthScreen
import org.grovealliance.sample.app.home.HomeScreen
import org.grovealliance.ui.ConsumeEvents
import org.grovealliance.ui.horizontalSlideBackward
import org.grovealliance.ui.horizontalSlideForward
import org.grovealliance.ui.theme.GroveTheme
import org.grovealliance.ui.verticalModalEnter
import org.grovealliance.ui.verticalModalExit

class SampleActivity : AppCompatActivity() {
    private val navigator by dependency<Navigator>()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setContent {
            GroveTheme {
                AppContent()
            }
        }
    }

    @Composable
    private fun AppContent() {
        val backStack = rememberNavBackStack(Routes.Home)

        ConsumeEvents(eventFlow = navigator.events) { event ->
            when (event) {
                is NavigationEvent.PopBackStack, is NavigationEvent.NavigateUp -> backStack.removeLastOrNull()
                is NavigationEvent.Health -> backStack.add(Routes.Health)
                is NavigationEvent.AccountLogin -> backStack.add(Routes.AccountLogin)
                is NavigationEvent.AccountOverview -> backStack.add(Routes.AccountOverview)
            }
        }

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
                entry<Routes.Home> {
                    HomeScreen()
                }
                entry<Routes.Health> {
                    HealthScreen()
                }
                entry<Routes.AccountLogin>(
                    metadata = NavDisplay.transitionSpec { verticalModalEnter } +
                        NavDisplay.popTransitionSpec { verticalModalExit } +
                        NavDisplay.predictivePopTransitionSpec { verticalModalExit },
                ) {
                    AccountLoginScreen(
                        onSuccess = { backStack.removeLastOrNull() },
                        onDismiss = { backStack.removeLastOrNull() },
                    )
                }
                entry<Routes.AccountOverview> {
                    AccountOverviewScreen(
                        onDismiss = { backStack.removeLastOrNull() },
                    )
                }
            },
        )
    }
}
