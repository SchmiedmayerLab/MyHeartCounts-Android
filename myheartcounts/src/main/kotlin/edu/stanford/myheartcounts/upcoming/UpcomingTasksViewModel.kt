//
// This source file is part of the My Heart Counts Android open-source project
//
// SPDX-FileCopyrightText: 2026 Stanford University and the project authors (see CONTRIBUTORS.md)
//
// SPDX-License-Identifier: MIT

package edu.stanford.myheartcounts.upcoming

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import edu.stanford.myheartcounts.MHCStrings
import edu.stanford.myheartcounts.StudyAppBarProvider
import edu.stanford.myheartcounts.task.AlwaysAvailableTask
import edu.stanford.myheartcounts.task.EventPerformer
import edu.stanford.myheartcounts.task.EventTileMapper
import edu.stanford.myheartcounts.task.TaskListScreenContent
import edu.stanford.myheartcounts.task.TaskListScreenState
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import org.grovealliance.core.logging.groveLogger
import org.grovealliance.ui.ActionSource
import org.grovealliance.ui.AsyncTextButton
import org.grovealliance.ui.GroveErrorLayout
import org.grovealliance.ui.StringResource
import org.grovealliance.ui.mutableScaffoldState

/**
 * Backs the upcoming tasks tab: lists the next two weeks of tasks, keeping the list current as
 * they change, and offers the tasks that may be performed at any time from the app bar.
 */
class UpcomingTasksViewModel(
    studyAppBarProvider: StudyAppBarProvider,
    tileMapper: EventTileMapper,
    private val tasksSource: UpcomingTasksSource,
    private val mapper: UpcomingTasksMapper,
    private val eventPerformer: EventPerformer,
) : ViewModel() {
    private val logger by groveLogger()
    private val actionSource = ActionSource(::onAction)
    private val actionSink = actionSource.sink<UpcomingTasksAction>()

    private val scaffoldState = mutableScaffoldState(
        appBar = studyAppBarProvider.create {
            title(MHCStrings.home_tab_upcoming)
            navigation(
                navigation = tileMapper.alwaysAvailableMenu(
                    onSelected = { actionSink.push(UpcomingTasksAction.AlwaysAvailableTaskSelected(task = it)) },
                ),
            )
        },
    )

    private val layoutState = MutableStateFlow<TaskListScreenState>(TaskListScreenState.Loading)

    /**
     * The tab's content.
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
            runCatching { observeUpcomingTasks() }
                .onFailure { throwable ->
                    logger.e(throwable) { "Failed to load the upcoming tasks" }
                    layoutState.update { TaskListScreenState.Error(errorLayout()) }
                }
        }
    }

    /**
     * Collects the upcoming events until the view model is cleared, rebuilding the layout on every
     * change.
     */
    private suspend fun observeUpcomingTasks() {
        tasksSource.upcomingEvents().collect { events ->
            val layout = mapper.map(
                input = UpcomingTasksInput(
                    events = events,
                    actionSink = actionSink,
                )
            )
            layoutState.update { TaskListScreenState.Content(layout) }
        }
    }

    private fun onAction(action: UpcomingTasksAction) {
        when (action) {
            is UpcomingTasksAction.PerformTaskSelected -> viewModelScope.launch {
                eventPerformer.perform(
                    event = action.event,
                    completesEvent = action.completesEvent,
                    scaffoldState = scaffoldState,
                )
            }
            is UpcomingTasksAction.AlwaysAvailableTaskSelected -> performAlwaysAvailableTask(action.task)
        }
    }

    private fun performAlwaysAvailableTask(task: AlwaysAvailableTask) {
        logger.i { "Always-available task selected: $task" }
    }

    private fun errorLayout() = GroveErrorLayout(
        title = StringResource(MHCStrings.upcoming_tasks_error_title),
        message = StringResource(MHCStrings.home_error_message),
        primaryButton = AsyncTextButton(
            title = StringResource(MHCStrings.home_error_retry),
            action = { load() },
        ),
    )
}
