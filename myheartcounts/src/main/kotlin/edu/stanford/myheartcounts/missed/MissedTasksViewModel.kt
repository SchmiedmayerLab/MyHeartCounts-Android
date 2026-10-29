//
// This source file is part of the My Heart Counts Android open-source project
//
// SPDX-FileCopyrightText: 2026 Stanford University and the project authors (see CONTRIBUTORS.md)
//
// SPDX-License-Identifier: MIT

package edu.stanford.myheartcounts.missed

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import edu.stanford.myheartcounts.MHCStrings
import edu.stanford.myheartcounts.navigation.MHCRoute
import edu.stanford.myheartcounts.navigation.NavigationEvent
import edu.stanford.myheartcounts.navigation.Navigator
import edu.stanford.myheartcounts.task.EventPerformer
import edu.stanford.myheartcounts.task.MissedEventsSource
import edu.stanford.myheartcounts.task.TaskListScreenContent
import edu.stanford.myheartcounts.task.TaskListScreenState
import edu.stanford.myheartcounts.task.todayRange
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import org.grovealliance.core.logging.groveLogger
import org.grovealliance.core.time.TimeProvider
import org.grovealliance.ui.ActionSource
import org.grovealliance.ui.AsyncTextButton
import org.grovealliance.ui.GroveErrorLayout
import org.grovealliance.ui.StringResource
import org.grovealliance.ui.groveAppBar
import org.grovealliance.ui.mutableScaffoldState

/**
 * Backs the missed tasks screen: lists the tasks missed in the two weeks before the day it opened
 * on, keeping the list current as they are caught up on.
 */
class MissedTasksViewModel(
    private val navigator: Navigator,
    private val missedEventsSource: MissedEventsSource,
    private val mapper: MissedTasksMapper,
    private val timeProvider: TimeProvider,
    private val eventPerformer: EventPerformer,
) : ViewModel() {
    private val logger by groveLogger()
    private val actionSource = ActionSource(::onAction)
    private val actionSink = actionSource.sink<MissedTasksAction>()

    private val scaffoldState = mutableScaffoldState(
        appBar = groveAppBar {
            back { navigator.push(event = NavigationEvent.Close(route = MHCRoute.MissedTasks)) }
            title(MHCStrings.missed_tasks_title)
        },
    )

    private val layoutState = MutableStateFlow<TaskListScreenState>(TaskListScreenState.Loading)

    /**
     * The screen's content.
     */
    val screen = TaskListScreenContent(
        scaffoldState = scaffoldState.asScaffoldState(),
        state = layoutState.asStateFlow(),
    )

    init {
        load()
    }

    private fun load() {
        layoutState.update { TaskListScreenState.Loading }
        viewModelScope.launch {
            runCatching { observeMissedTasks() }
                .onFailure { throwable ->
                    logger.e(throwable) { "Failed to load the missed tasks" }
                    layoutState.update { TaskListScreenState.Error(errorLayout()) }
                }
        }
    }

    /**
     * Collects the missed events until the view model is cleared, rebuilding the layout on every
     * change.
     */
    private suspend fun observeMissedTasks() {
        missedEventsSource.missedEvents(today = timeProvider.todayRange()).collect { events ->
            val layout = mapper.map(
                input = MissedTasksInput(
                    events = events,
                    actionSink = actionSink,
                )
            )
            layoutState.update { TaskListScreenState.Content(layout) }
        }
    }

    private fun onAction(action: MissedTasksAction) {
        when (action) {
            is MissedTasksAction.PerformTaskSelected -> viewModelScope.launch {
                eventPerformer.perform(
                    event = action.event,
                    completesEvent = action.completesEvent,
                    scaffoldState = scaffoldState,
                )
            }
            is MissedTasksAction.AlwaysAvailableTaskSelected ->
                logger.i { "Always-available task selected: ${action.task}" }
        }
    }

    private fun errorLayout() = GroveErrorLayout(
        title = StringResource(MHCStrings.missed_tasks_error_title),
        message = StringResource(MHCStrings.home_error_message),
        primaryButton = AsyncTextButton(
            title = StringResource(MHCStrings.home_error_retry),
            action = { load() },
        ),
    )
}
