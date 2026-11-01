//
// This source file is part of the My Heart Counts Android open-source project
//
// SPDX-FileCopyrightText: 2026 Stanford University and the project authors (see CONTRIBUTORS.md)
//
// SPDX-License-Identifier: MIT

package edu.stanford.myheartcounts.stats

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Info
import androidx.lifecycle.ViewModel
import edu.stanford.myheartcounts.MHCStrings
import edu.stanford.myheartcounts.navigation.MHCRoute
import edu.stanford.myheartcounts.navigation.NavigationEvent
import edu.stanford.myheartcounts.navigation.Navigator
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import org.grovealliance.ui.ActionSource
import org.grovealliance.ui.GroveAppBar
import org.grovealliance.ui.ImageResource
import org.grovealliance.ui.StringResource
import org.grovealliance.ui.groveAppBar
import org.grovealliance.ui.mutableScaffoldState

/**
 * Backs the participation statistics sheet: the stats, whose info action opens their explanation
 * and whose enrollment card opens the list of all achievements, each a page of its own with a way
 * back to the stats.
 */
class ParticipationStatsViewModel(
    private val navigator: Navigator,
    mapper: ParticipationStatsContentMapper,
) : ViewModel() {

    private val actionSource = ActionSource(::onAction)
    private val actionSink = actionSource.sink<ParticipationStatsAction>()
    private val page = MutableStateFlow(ParticipationStatsPage.Stats)
    private val scaffoldState = mutableScaffoldState(appBar = appBar(page = page.value))

    /**
     * The screen's content.
     */
    val screen = mapper.map(
        input = ParticipationStatsContentInput(
            scaffoldState = scaffoldState.asScaffoldState(),
            page = page.asStateFlow(),
            actionSink = actionSink,
        ),
    )

    private fun onAction(action: ParticipationStatsAction) {
        when (action) {
            ParticipationStatsAction.Close -> navigator.push(event = NavigationEvent.Close(route = MHCRoute.ParticipationStats))
            ParticipationStatsAction.InfoSelected -> show(page = ParticipationStatsPage.Explainer)
            ParticipationStatsAction.AchievementsSelected -> show(page = ParticipationStatsPage.Achievements)
            ParticipationStatsAction.Back -> show(page = ParticipationStatsPage.Stats)
        }
    }

    private fun show(page: ParticipationStatsPage) {
        this.page.value = page
        scaffoldState.setAppBar(appBar = appBar(page = page))
    }

    private fun appBar(page: ParticipationStatsPage): GroveAppBar = when (page) {
        ParticipationStatsPage.Stats -> groveAppBar {
            close { actionSink.push(ParticipationStatsAction.Close) }
            title(MHCStrings.participation_stats_title)
            action(
                imageResource = ImageResource(
                    image = Icons.Outlined.Info,
                    contentDescription = StringResource(MHCStrings.participation_stats_info),
                ),
            ) {
                actionSink.push(ParticipationStatsAction.InfoSelected)
            }
        }
        ParticipationStatsPage.Explainer -> groveAppBar {
            back { actionSink.push(ParticipationStatsAction.Back) }
            title(MHCStrings.participation_stats_info)
        }
        ParticipationStatsPage.Achievements -> groveAppBar {
            back { actionSink.push(ParticipationStatsAction.Back) }
            title(MHCStrings.achievements_title)
        }
    }
}
