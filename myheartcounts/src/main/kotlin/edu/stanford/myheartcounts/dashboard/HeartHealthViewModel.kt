//
// This source file is part of the My Heart Counts Android open-source project
//
// SPDX-FileCopyrightText: 2026 Stanford University and the project authors (see CONTRIBUTORS.md)
//
// SPDX-License-Identifier: MIT

package edu.stanford.myheartcounts.dashboard

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import edu.stanford.myheartcounts.MHCStrings
import edu.stanford.myheartcounts.StudyAppBarProvider
import edu.stanford.myheartcounts.navigation.MHCRoute
import edu.stanford.myheartcounts.navigation.NavigationEvent
import edu.stanford.myheartcounts.navigation.Navigator
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import org.grovealliance.core.coroutines.runCatchingAsync
import org.grovealliance.core.logging.groveLogger
import org.grovealliance.ui.ActionSource
import org.grovealliance.ui.AsyncTextButton
import org.grovealliance.ui.GroveErrorLayout
import org.grovealliance.ui.StringResource
import org.grovealliance.ui.mutableScaffoldState

/**
 * Backs the Heart Health tab: renders the participant's heart health score, its factors and the
 * study's explanation of the score, keeping them current as the scores change, and opens a factor's
 * details or the participation statistics.
 */
class HeartHealthViewModel(
    studyAppBarProvider: StudyAppBarProvider,
    private val navigator: Navigator,
    private val scoreSource: HeartHealthScoreSource,
    private val explainerSource: HeartHealthExplainerSource,
    private val mapper: HeartHealthContentMapper,
) : ViewModel() {

    private val logger by groveLogger()
    private val actionSource = ActionSource(::onAction)
    private val actionSink = actionSource.sink<HeartHealthAction>()

    private val scaffoldState = mutableScaffoldState(
        appBar = studyAppBarProvider.create(showsParticipationStats = true) {
            title(MHCStrings.home_tab_heart_health)
        },
    )

    private val layoutState = MutableStateFlow<HeartHealthLayoutState>(HeartHealthLayoutState.Loading)

    /**
     * The tab's content.
     */
    val screen = HeartHealthScreenContent(
        scaffoldState = scaffoldState.asScaffoldState(),
        state = layoutState.asStateFlow(),
    )

    init {
        load()
    }

    private fun load() {
        layoutState.update { HeartHealthLayoutState.Loading }
        viewModelScope.launch {
            runCatchingAsync { observeScores() }
                .onFailure { throwable ->
                    logger.e(throwable) { "Failed to load the Heart Health tab" }
                    layoutState.update { HeartHealthLayoutState.Error(errorLayout()) }
                }
        }
    }

    /**
     * Reads the explanation of the score once, then collects the participant's scores until the view
     * model is cleared, rebuilding the layout on every change.
     */
    private suspend fun observeScores() {
        val explainer = explainerSource.scoreExplainer()
        scoreSource.scores.collect { scores ->
            val layout = mapper.map(
                input = HeartHealthContentInput(
                    scores = scores,
                    explainer = explainer,
                    actionSink = actionSink,
                )
            )
            layoutState.update { HeartHealthLayoutState.Content(layout) }
        }
    }

    private fun onAction(action: HeartHealthAction) {
        when (action) {
            is HeartHealthAction.ComponentSelected -> navigateTo(MHCRoute.HeartScoreDetail(component = action.component))
            HeartHealthAction.PastDataSelected -> navigateTo(MHCRoute.PastTimedWalkTestResults)
        }
    }

    private fun navigateTo(route: MHCRoute) {
        navigator.push(event = NavigationEvent.NavigateTo(route = route))
    }

    private fun errorLayout() = GroveErrorLayout(
        title = StringResource(MHCStrings.heart_health_error_title),
        message = StringResource(MHCStrings.heart_health_error_message),
        primaryButton = AsyncTextButton(
            title = StringResource(MHCStrings.heart_health_error_retry),
            action = { load() },
        ),
    )
}
