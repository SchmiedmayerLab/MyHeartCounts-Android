//
// This source file is part of the My Heart Counts Android open-source project
//
// SPDX-FileCopyrightText: 2026 Stanford University and the project authors (see CONTRIBUTORS.md)
//
// SPDX-License-Identifier: MIT

package edu.stanford.myheartcounts.dashboard.pastdata

import androidx.lifecycle.ViewModel
import edu.stanford.myheartcounts.MHCStrings
import edu.stanford.myheartcounts.navigation.MHCRoute
import edu.stanford.myheartcounts.navigation.NavigationEvent
import edu.stanford.myheartcounts.navigation.Navigator
import edu.stanford.myheartcounts.ui.PlaceholderScreenContent
import org.grovealliance.ui.StringResource
import org.grovealliance.ui.groveAppBar
import org.grovealliance.ui.mutableScaffoldState

/**
 * Provides the navigation and placeholder content for past test results.
 */
class PastTimedWalkTestResultsViewModel(
    navigator: Navigator,
) : ViewModel() {
    private val scaffoldState = mutableScaffoldState(
        appBar = groveAppBar {
            back { navigator.push(event = NavigationEvent.Close(route = MHCRoute.PastTimedWalkTestResults)) }
            title(MHCStrings.heart_health_past_results)
        },
    )

    val screen = PlaceholderScreenContent(
        scaffoldState = scaffoldState.asScaffoldState(),
        title = StringResource(MHCStrings.heart_health_past_results),
    )
}
