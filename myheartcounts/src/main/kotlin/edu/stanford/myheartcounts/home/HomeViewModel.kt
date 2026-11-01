//
// This source file is part of the My Heart Counts Android open-source project
//
// SPDX-FileCopyrightText: 2026 Stanford University and the project authors (see CONTRIBUTORS.md)
//
// SPDX-License-Identifier: MIT

package edu.stanford.myheartcounts.home

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import edu.stanford.myheartcounts.MHCStrings
import edu.stanford.myheartcounts.StudyAppBarProvider
import edu.stanford.myheartcounts.navigation.MHCRoute
import edu.stanford.myheartcounts.navigation.NavigationEvent
import edu.stanford.myheartcounts.navigation.Navigator
import edu.stanford.myheartcounts.task.AlwaysAvailableTask
import edu.stanford.myheartcounts.task.EventPerformer
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import org.grovealliance.core.coroutines.runCatchingAsync
import org.grovealliance.core.logging.groveLogger
import org.grovealliance.scheduler.Event
import org.grovealliance.ui.ActionSink
import org.grovealliance.ui.ActionSource
import org.grovealliance.ui.AsyncTextButton
import org.grovealliance.ui.GroveErrorLayout
import org.grovealliance.ui.StringResource
import org.grovealliance.ui.mutableScaffoldState

/**
 * Backs the Home tab: waits for the participant's tasks to become available, then renders them and
 * the sections around them, keeping the list current as events are completed.
 */
class HomeViewModel(
    studyAppBarProvider: StudyAppBarProvider,
    private val navigator: Navigator,
    private val tasksSource: HomeTasksSource,
    private val mapper: HomeContentMapper,
    private val suggestionsSource: HomeSuggestionsSource,
    private val eventPerformer: EventPerformer,
) : ViewModel() {
    private val logger by groveLogger()
    private val actionSource = ActionSource(::onAction)
    private val actionSink = actionSource.sink<HomeAction>()

    private val scaffoldState = mutableScaffoldState(
        appBar = studyAppBarProvider.create(showsParticipationStats = true) { title(MHCStrings.app_name) },
    )

    private val layoutState = MutableStateFlow<HomeLayoutState>(HomeLayoutState.Loading)

    val screen = HomeScreenContent(
        scaffoldState = scaffoldState.asScaffoldState(),
        state = layoutState.asStateFlow(),
    )

    init {
        load()
    }

    private fun load() {
        layoutState.update { HomeLayoutState.Loading }
        viewModelScope.launch {
            runCatchingAsync { observeTasks() }
                .onFailure { throwable ->
                    logger.e(throwable) { "Failed to load the Home tab" }
                    layoutState.update { HomeLayoutState.Error(errorLayout()) }
                }
        }
    }

    /**
     * Collects the participant's tasks until the view model is cleared, rebuilding the layout on
     * every change.
     */
    private suspend fun observeTasks() {
        combine(
            tasksSource.tasks(),
            suggestionsSource.nudge,
            suggestionsSource.pendingActions,
        ) { tasks, nudge, promptedActions ->
            mapper.map(
                input = HomeContentInput(
                    events = tasks.events,
                    missedEventCount = tasks.missedEventCount,
                    nudge = nudge,
                    promptedActions = promptedActions,
                    actionSink = actionSink,
                )
            )
        }.collect { layout -> layoutState.update { HomeLayoutState.Content(layout) } }
    }

    private fun onAction(action: HomeAction) {
        when (action) {
            HomeAction.MissedTasksSelected -> showMissedTasks()
            HomeAction.PromptedActionsSelected -> showPromptedActions()
            HomeAction.LearnMoreSelected -> showStudyWebsite()
            is HomeAction.AlwaysAvailableTaskSelected -> performAlwaysAvailableTask(action.task)
            is HomeAction.PerformTaskSelected -> performTask(action)
        }
    }

    private fun performTask(action: HomeAction.PerformTaskSelected) {
        viewModelScope.launch {
            eventPerformer.perform(
                event = action.event,
                completesEvent = action.completesEvent,
                scaffoldState = scaffoldState,
            )
        }
    }

    private fun errorLayout() = GroveErrorLayout(
        title = StringResource(MHCStrings.home_error_title),
        message = StringResource(MHCStrings.home_error_message),
        primaryButton = AsyncTextButton(
            title = StringResource(MHCStrings.home_error_retry),
            action = { load() },
        ),
    )

    private fun showMissedTasks() {
        navigator.push(event = NavigationEvent.NavigateTo(route = MHCRoute.MissedTasks))
    }

    private fun showPromptedActions() {
        logger.i { "Prompted actions selected." }
    }

    private fun performAlwaysAvailableTask(task: AlwaysAvailableTask) {
        logger.i { "Always-available task selected: $task" }
    }

    private fun showStudyWebsite() {
        navigator.push(event = NavigationEvent.OpenStudyHomepage)
    }
}

/**
 * The data the Home tab is built from.
 */
data class HomeContentInput(
    val events: List<Event>,
    val missedEventCount: Int,
    val nudge: DailyNudge?,
    val promptedActions: List<PromptedAction>,
    val actionSink: ActionSink<HomeAction>,
)

/**
 * An intent raised by the Home tab.
 */
sealed interface HomeAction {
    /**
     * The participant opened the list of tasks they missed.
     */
    data object MissedTasksSelected : HomeAction

    /**
     * The participant opened the outstanding prompted actions.
     */
    data object PromptedActionsSelected : HomeAction

    /**
     * The participant asked to read more about the study.
     */
    data object LearnMoreSelected : HomeAction

    /**
     * The participant chose to perform [event]; [completesEvent] states whether performing it
     * completes the event.
     */
    data class PerformTaskSelected(
        val event: Event,
        val completesEvent: Boolean,
    ) : HomeAction

    /**
     * The participant chose one of the tasks that may be performed at any time.
     */
    data class AlwaysAvailableTaskSelected(val task: AlwaysAvailableTask) : HomeAction
}
