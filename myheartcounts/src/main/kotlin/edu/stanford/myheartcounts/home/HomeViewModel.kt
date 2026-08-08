//
// This source file is part of the My Heart Counts open-source project
//
// SPDX-FileCopyrightText: 2026 Stanford University and the project authors (see CONTRIBUTORS.md)
//
// SPDX-License-Identifier: MIT

package edu.stanford.myheartcounts.home

import androidx.annotation.StringRes
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import edu.stanford.myheartcounts.MHCStrings
import edu.stanford.myheartcounts.StudyAppBarProvider
import edu.stanford.myheartcounts.navigation.NavigationEvent
import edu.stanford.myheartcounts.navigation.Navigator
import edu.stanford.myheartcounts.study.StudyArticleSource
import edu.stanford.myheartcounts.ui.MarkdownSheet
import edu.stanford.spezi.core.logging.speziLogger
import edu.stanford.spezi.core.time.TimeProvider
import edu.stanford.spezi.markdown.MarkdownDocument
import edu.stanford.spezi.scheduler.Event
import edu.stanford.spezi.ui.ActionSink
import edu.stanford.spezi.ui.ActionSource
import edu.stanford.spezi.ui.AsyncTextButton
import edu.stanford.spezi.ui.SpeziErrorLayout
import edu.stanford.spezi.ui.StringResource
import edu.stanford.spezi.ui.mutableScaffoldState
import edu.stanford.spezi.ui.showErrorToast
import edu.stanford.spezi.ui.speziAppBar
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.time.Instant
import java.time.ZoneId

private const val STUDY_WEBSITE_URL = "https://myheartcounts.stanford.edu"

/**
 * Backs the Home tab: waits for the participant's tasks to become available, then renders them and
 * the sections around them, keeping the list current as events are completed.
 */
class HomeViewModel @Suppress("detekt:LongParameterList") constructor(
    studyAppBarProvider: StudyAppBarProvider,
    private val navigator: Navigator,
    private val tasksSource: HomeTasksSource,
    private val mapper: HomeContentMapper,
    private val timeProvider: TimeProvider,
    private val suggestionsSource: HomeSuggestionsSource,
    private val articleSource: StudyArticleSource,
) : ViewModel() {
    private val logger by speziLogger()
    private val actionSource = ActionSource(::onAction)
    private val actionSink = actionSource.sink<HomeAction>()

    private val scaffoldState = mutableScaffoldState(
        appBar = studyAppBarProvider.create { title(MHCStrings.app_name) },
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
            runCatching { observeTasks() }
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
                    now = timeProvider.nowInstant(),
                    zone = timeProvider.currentZone(),
                    actionSink = actionSink,
                    onCompleteEvent = ::completeEvent,
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
            is HomeAction.ReadArticleSelected -> readArticle(action)
        }
    }

    /**
     * Presents the article of [action], completing its event once the sheet is shown when the
     * action states that presenting it does.
     */
    private fun readArticle(action: HomeAction.ReadArticleSelected) {
        viewModelScope.launch {
            articleSource.article(action.event)
                .onSuccess { document -> showArticleSheet(document = document, action = action) }
                .onFailure { throwable ->
                    logger.e(throwable) { "Failed to load the article of ${action.event.task.id}" }
                    scaffoldState.showErrorToast(message = StringResource(MHCStrings.home_article_error))
                }
        }
    }

    private fun showArticleSheet(document: MarkdownDocument, action: HomeAction.ReadArticleSelected) {
        val sheet = MarkdownSheet(
            appBar = speziAppBar {
                close { scaffoldState.dismissBottomSheet() }
            },
            document = document,
            onDisplayed = {
                if (action.completesEvent) {
                    viewModelScope.launch { completeEvent(action.event) }
                }
            },
        )
        scaffoldState.showBottomSheet(sheet = sheet)
    }

    private suspend fun completeEvent(event: Event) {
        tasksSource.complete(event)
            .onFailure { logger.e(it) { "Failed to complete ${event.task.id}" } }
    }

    private fun errorLayout() = SpeziErrorLayout(
        title = StringResource(MHCStrings.home_error_title),
        message = StringResource(MHCStrings.home_error_message),
        primaryButton = AsyncTextButton(
            title = StringResource(MHCStrings.home_error_retry),
            action = { load() },
        ),
    )

    private fun showMissedTasks() {
        // Missed tasks are listed on the Upcoming Tasks tab, which does not yet accept a destination.
        logger.i { "Missed tasks selected." }
    }

    private fun showPromptedActions() {
        logger.i { "Prompted actions selected." }
    }

    private fun performAlwaysAvailableTask(task: AlwaysAvailableTask) {
        logger.i { "Always-available task selected: $task" }
    }

    private fun showStudyWebsite() {
        navigator.push(event = NavigationEvent.OpenUrl(url = STUDY_WEBSITE_URL))
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
    val now: Instant,
    val zone: ZoneId,
    val actionSink: ActionSink<HomeAction>,
    val onCompleteEvent: suspend (Event) -> Unit,
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
     * The participant opened the article of [event]; [completesEvent] states whether presenting it
     * completes the event.
     */
    data class ReadArticleSelected(
        val event: Event,
        val completesEvent: Boolean,
    ) : HomeAction

    /**
     * The participant chose one of the tasks that may be performed at any time.
     */
    data class AlwaysAvailableTaskSelected(val task: AlwaysAvailableTask) : HomeAction
}

/**
 * A task a participant may perform whenever they choose, independently of their schedule.
 */
enum class AlwaysAvailableTask(
    @get:StringRes val titleId: Int,
    @get:StringRes val subtitleId: Int?,
    @get:StringRes val actionTitleId: Int,
) {
    ELECTROCARDIOGRAM(
        titleId = MHCStrings.always_available_ecg_title,
        subtitleId = MHCStrings.always_available_ecg_subtitle,
        actionTitleId = MHCStrings.task_action_take_ecg,
    ),
    SIX_MINUTE_WALK(
        titleId = MHCStrings.always_available_six_minute_walk_title,
        subtitleId = null,
        actionTitleId = MHCStrings.task_action_take_test,
    ),
    TWELVE_MINUTE_RUN(
        titleId = MHCStrings.always_available_twelve_minute_run_title,
        subtitleId = null,
        actionTitleId = MHCStrings.task_action_take_test,
    ),
}
