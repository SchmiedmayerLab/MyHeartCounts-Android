//
// This source file is part of the My Heart Counts Android open-source project
//
// SPDX-FileCopyrightText: 2026 Stanford University and the project authors (see CONTRIBUTORS.md)
//
// SPDX-License-Identifier: MIT

package edu.stanford.myheartcounts.study

import androidx.activity.compose.BackHandler
import androidx.annotation.StringRes
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.consumeWindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.CalendarMonth
import androidx.compose.material.icons.outlined.Home
import androidx.compose.material.icons.outlined.MonitorHeart
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import edu.stanford.myheartcounts.MHCStrings
import edu.stanford.myheartcounts.dashboard.HeartHealthScreen
import edu.stanford.myheartcounts.home.HomeScreen
import edu.stanford.myheartcounts.upcoming.UpcomingTasksScreen
import kotlinx.coroutines.flow.StateFlow
import org.grovealliance.ui.ActionSink
import org.grovealliance.ui.ActionSource
import org.grovealliance.ui.ComposableContent

/**
 * Holds the selected [StudyTab] for the study content and builds its [StudyScreenContent].
 */
class StudyViewModel(
    private val savedStateHandle: SavedStateHandle,
) : ViewModel() {
    private val actionSource = ActionSource(::onAction)

    val screen = StudyScreenContent(
        selectedTab = savedStateHandle.getStateFlow(KEY_SELECTED_TAB, StudyTab.Home),
        tabs = StudyTab.entries,
        actionSink = actionSource.sink(),
    )

    private fun onAction(action: StudyAction) {
        when (action) {
            is StudyAction.TabClicked -> selectTab(action.tab)
            StudyAction.BackPressed -> selectTab(StudyTab.Home)
        }
    }

    private fun selectTab(tab: StudyTab) {
        savedStateHandle[KEY_SELECTED_TAB] = tab
    }

    private companion object {
        const val KEY_SELECTED_TAB = "selectedTab"
    }
}

/**
 * The bottom-navigation tabs of the study content, each backed by its own screen.
 */
enum class StudyTab(@StringRes val label: Int, val icon: ImageVector) {
    Home(label = MHCStrings.home_tab_home, icon = Icons.Outlined.Home),
    Upcoming(label = MHCStrings.home_tab_upcoming, icon = Icons.Outlined.CalendarMonth),
    HeartHealth(label = MHCStrings.home_tab_heart_health, icon = Icons.Outlined.MonitorHeart),
}

/**
 * The study content shown after onboarding: a bottom-navigation host that renders the [selectedTab]
 * screen and dispatches tab selection and back presses through [actionSink]. Back is intercepted
 * while off the first tab to return there.
 */
data class StudyScreenContent(
    val selectedTab: StateFlow<StudyTab>,
    val tabs: List<StudyTab>,
    val actionSink: ActionSink<StudyAction>,
) : ComposableContent {
    @Composable
    override fun Content(modifier: Modifier) {
        val tab by selectedTab.collectAsStateWithLifecycle()

        BackHandler(enabled = tab != StudyTab.Home) {
            actionSink.push(StudyAction.BackPressed)
        }

        Scaffold(
            modifier = modifier,
            bottomBar = {
                NavigationBar {
                    tabs.forEach { entry ->
                        val label = stringResource(entry.label)
                        NavigationBarItem(
                            selected = tab == entry,
                            onClick = { actionSink.push(StudyAction.TabClicked(entry)) },
                            icon = { Icon(imageVector = entry.icon, contentDescription = label) },
                            label = { Text(text = label) },
                        )
                    }
                }
            },
        ) { innerPadding ->
            // Reserve only the nav-bar space and consume that inset so the nested GroveScaffold's
            // systemBarsPadding doesn't re-add the bottom inset (which would gap the content above
            // the bar). The top is left untouched so tab content aligns like a standalone scaffold.
            val bottomBarHeight = innerPadding.calculateBottomPadding()
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(bottom = bottomBarHeight)
                    .consumeWindowInsets(PaddingValues(bottom = bottomBarHeight)),
            ) {
                when (tab) {
                    StudyTab.Home -> HomeScreen()
                    StudyTab.Upcoming -> UpcomingTasksScreen()
                    StudyTab.HeartHealth -> HeartHealthScreen()
                }
            }
        }
    }
}

/**
 * A user interaction within the study experience for the view model to act on.
 */
sealed interface StudyAction {

    /**
     * The user selected [tab] in the bottom navigation.
     */
    data class TabClicked(val tab: StudyTab) : StudyAction

    /**
     * The user pressed back.
     */
    data object BackPressed : StudyAction
}
