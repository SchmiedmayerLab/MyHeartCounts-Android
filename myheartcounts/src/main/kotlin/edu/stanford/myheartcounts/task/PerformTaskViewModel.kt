//
// This source file is part of the My Heart Counts Android open-source project
//
// SPDX-FileCopyrightText: 2026 Stanford University and the project authors (see CONTRIBUTORS.md)
//
// SPDX-License-Identifier: MIT

package edu.stanford.myheartcounts.task

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import edu.stanford.myheartcounts.MHCStrings
import edu.stanford.myheartcounts.navigation.MHCRoute
import edu.stanford.myheartcounts.navigation.NavigationEvent
import edu.stanford.myheartcounts.navigation.Navigator
import edu.stanford.myheartcounts.standard.QuestionnaireResponseUploader
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import org.grovealliance.core.logging.groveLogger
import org.grovealliance.questionnaire.QuestionnaireResult
import org.grovealliance.ui.AsyncTextButton
import org.grovealliance.ui.GroveErrorLayout
import org.grovealliance.ui.StringResource
import org.grovealliance.ui.groveAppBar
import org.grovealliance.ui.mutableScaffoldState

/**
 * Backs the task screen: renders the [TaskPerformer]'s active task and finishes it with the
 * participant's outcome.
 *
 * Ending the task from the screen closes it, as does the task ending elsewhere. Opened without
 * one, as after the process was recreated, it shows an error offering to close it. Leaving the screen
 * any other way ends the task as not completed.
 *
 * @param uploadScope The scope uploads run in, outliving the screen so that closing it does not
 * cancel them.
 */
class PerformTaskViewModel(
    taskPerformer: TaskPerformer,
    private val navigator: Navigator,
    private val responseUploader: QuestionnaireResponseUploader,
    private val uploadScope: CoroutineScope,
) : ViewModel() {
    private val logger by groveLogger()
    private val activeTask = taskPerformer.activeTask.value
    private var isClosed = false

    /**
     * What the screen shows: the active task, or an error when it opened without one.
     */
    val screen: PerformTaskScreenState = activeTask
        ?.let { PerformTaskScreenState.Content(layout = content(it)) }
        ?: PerformTaskScreenState.Error(layout = errorLayout())

    init {
        if (activeTask != null) {
            viewModelScope.launch {
                taskPerformer.activeTask.first { it !== activeTask }
                closeScreen()
            }
        }
    }

    override fun onCleared() {
        activeTask?.finish(didSucceed = false)
    }

    private fun content(activeTask: ActiveTask): QuestionnaireTaskContent = when (val task = activeTask.task) {
        is PerformableTask.AnswerQuestionnaire -> QuestionnaireTaskContent(
            scaffoldState = mutableScaffoldState(
                appBar = groveAppBar {
                    task.questionnaire.questionnaire.title?.let { title(it) }
                    close { finish(activeTask = activeTask, didSucceed = false) }
                },
            ).asScaffoldState(),
            questionnaire = task.questionnaire.questionnaire,
            onResult = { result -> onQuestionnaireResult(activeTask = activeTask, task = task, result = result) },
        )
    }

    private fun errorLayout() = GroveErrorLayout(
        title = StringResource(MHCStrings.perform_task_error_title),
        message = StringResource(MHCStrings.perform_task_error_message),
        primaryButton = AsyncTextButton(
            title = StringResource(MHCStrings.perform_task_error_close),
            action = { closeScreen() },
        ),
    )

    /**
     * Finishes [activeTask] with [didSucceed] and closes the screen.
     */
    private fun finish(activeTask: ActiveTask, didSucceed: Boolean) {
        activeTask.finish(didSucceed = didSucceed)
        closeScreen()
    }

    private fun closeScreen() {
        if (isClosed) return
        isClosed = true
        navigator.push(event = NavigationEvent.Close(route = MHCRoute.PerformTask))
    }

    /**
     * Finishes [activeTask] with [result] and closes the screen. A completed response is uploaded without holding the
     * screen open, since the write is kept locally until it reaches the server.
     */
    private fun onQuestionnaireResult(
        activeTask: ActiveTask,
        task: PerformableTask.AnswerQuestionnaire,
        result: QuestionnaireResult,
    ) {
        when (result) {
            is QuestionnaireResult.Completed -> {
                val response = task.questionnaire.responseFor(rendered = result.response)
                uploadScope.launch {
                    responseUploader.upload(
                        response = response,
                        questionnaire = task.questionnaire.questionnaire,
                    )
                }
                finish(activeTask = activeTask, didSucceed = true)
            }
            QuestionnaireResult.Cancelled -> finish(activeTask = activeTask, didSucceed = false)
            QuestionnaireResult.Failed -> {
                logger.e { "The questionnaire could not be rendered." }
                finish(activeTask = activeTask, didSucceed = false)
            }
        }
    }
}
