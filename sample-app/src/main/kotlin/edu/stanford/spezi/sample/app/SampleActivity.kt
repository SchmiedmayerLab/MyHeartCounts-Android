package edu.stanford.spezi.sample.app

import android.os.Bundle
import androidx.activity.compose.setContent
import androidx.appcompat.app.AppCompatActivity
import androidx.compose.runtime.Composable
import androidx.lifecycle.viewmodel.navigation3.rememberViewModelStoreNavEntryDecorator
import androidx.navigation3.runtime.entryProvider
import androidx.navigation3.runtime.rememberNavBackStack
import androidx.navigation3.runtime.rememberSaveableStateHolderNavEntryDecorator
import androidx.navigation3.ui.NavDisplay
import edu.stanford.spezi.account.AccountLoginScreen
import edu.stanford.spezi.account.AccountOverviewScreen
import edu.stanford.spezi.core.dependency
import edu.stanford.spezi.sample.app.health.HealthScreen
import edu.stanford.spezi.sample.app.home.HomeScreen
import edu.stanford.spezi.ui.ConsumeEvents
import edu.stanford.spezi.ui.horizontalSlideBackward
import edu.stanford.spezi.ui.horizontalSlideForward
import edu.stanford.spezi.ui.theme.SpeziTheme
import edu.stanford.spezi.ui.verticalModalEnter
import edu.stanford.spezi.ui.verticalModalExit

class SampleActivity : AppCompatActivity() {
    private val navigator by dependency<Navigator>()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setContent {
            SpeziTheme {
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
