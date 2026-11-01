//
// This source file is part of the My Heart Counts Android open-source project
//
// SPDX-FileCopyrightText: 2026 Stanford University and the project authors (see CONTRIBUTORS.md)
//
// SPDX-License-Identifier: MIT

package edu.stanford.myheartcounts.task

import edu.stanford.myheartcounts.MHCStrings
import edu.stanford.myheartcounts.study.StudyArticleSource
import edu.stanford.myheartcounts.study.StudyQuestionnaireSource
import edu.stanford.myheartcounts.ui.ArticleMapper
import kotlinx.coroutines.CompletableDeferred
import org.grovealliance.core.logging.groveLogger
import org.grovealliance.markdown.MarkdownDocument
import org.grovealliance.scheduler.Event
import org.grovealliance.scheduler.Scheduler
import org.grovealliance.study.ScheduledTaskAction
import org.grovealliance.study.ScheduledTaskActionKey
import org.grovealliance.ui.MutableGroveScaffoldState
import org.grovealliance.ui.StringResource
import org.grovealliance.ui.groveAppBar
import org.grovealliance.ui.showErrorToast

/**
 * Performs the action of a scheduled event: presents its article or questionnaire and, when asked
 * to, completes the event once the participant has done what it asks.
 */
interface EventPerformer {
    /**
     * Performs [event]'s action on the screen of [scaffoldState], completing [event] afterwards when
     * [completesEvent] is set and the participant finished the action. Presenting an article
     * finishes it; a questionnaire is finished once submitted.
     *
     * Suspends until the action ends. When its content cannot be loaded, an error toast is shown on
     * [scaffoldState] and [event] is left open.
     */
    suspend fun perform(
        event: Event,
        completesEvent: Boolean,
        scaffoldState: MutableGroveScaffoldState,
    )
}

/**
 * Presents articles as a bottom sheet on the calling screen and questionnaires through
 * [taskPerformer].
 */
class EventPerformerImpl(
    private val articleSource: StudyArticleSource,
    private val articleMapper: ArticleMapper,
    private val questionnaireSource: StudyQuestionnaireSource,
    private val taskPerformer: TaskPerformer,
    private val scheduler: Scheduler,
) : EventPerformer {
    private val logger by groveLogger()

    override suspend fun perform(
        event: Event,
        completesEvent: Boolean,
        scaffoldState: MutableGroveScaffoldState,
    ) {
        val didFinish = when (event.task.context[ScheduledTaskActionKey]) {
            is ScheduledTaskAction.PresentInformational -> readArticle(event = event, scaffoldState = scaffoldState)
            is ScheduledTaskAction.AnswerQuestionnaire -> answerQuestionnaire(event = event, scaffoldState = scaffoldState)
            else -> {
                logger.w { "The task ${event.task.id} cannot be performed." }
                false
            }
        }
        if (didFinish && completesEvent) {
            scheduler.complete(event = event)
                .onFailure { logger.e(it) { "Failed to complete ${event.task.id}" } }
        }
    }

    /**
     * Presents the article of [event], returning once its sheet is shown, or `false` when it cannot
     * be loaded.
     */
    private suspend fun readArticle(event: Event, scaffoldState: MutableGroveScaffoldState): Boolean {
        val document = articleSource.article(event)
            .onFailure { throwable ->
                logger.e(throwable) { "Failed to load the article of ${event.task.id}" }
                scaffoldState.showErrorToast(message = StringResource(MHCStrings.task_article_error))
            }
            .getOrNull() ?: return false
        val displayed = CompletableDeferred<Unit>()
        scaffoldState.showBottomSheet(
            sheet = articleSheet(
                document = document,
                scaffoldState = scaffoldState,
                onDisplayed = { displayed.complete(Unit) },
            ),
        )
        displayed.await()
        return true
    }

    private fun articleSheet(
        document: MarkdownDocument,
        scaffoldState: MutableGroveScaffoldState,
        onDisplayed: () -> Unit,
    ) = articleMapper.map(
        appBar = groveAppBar {
            close { scaffoldState.dismissBottomSheet() }
        },
        document = document,
        onDisplayed = onDisplayed,
    )

    /**
     * Presents the questionnaire of [event], returning whether the participant submitted it, or
     * `false` when it cannot be loaded.
     */
    private suspend fun answerQuestionnaire(event: Event, scaffoldState: MutableGroveScaffoldState): Boolean {
        val questionnaire = questionnaireSource.questionnaire(event)
            .onFailure { throwable ->
                logger.e(throwable) { "Failed to load the questionnaire of ${event.task.id}" }
                scaffoldState.showErrorToast(message = StringResource(MHCStrings.task_questionnaire_error))
            }
            .getOrNull() ?: return false
        return taskPerformer.perform(task = PerformableTask.AnswerQuestionnaire(questionnaire = questionnaire))
    }
}
