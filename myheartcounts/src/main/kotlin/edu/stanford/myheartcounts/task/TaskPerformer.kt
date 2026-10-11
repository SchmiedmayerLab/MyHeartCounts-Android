//
// This source file is part of the My Heart Counts Android open-source project
//
// SPDX-FileCopyrightText: 2026 Stanford University and the project authors (see CONTRIBUTORS.md)
//
// SPDX-License-Identifier: MIT

package edu.stanford.myheartcounts.task

import edu.stanford.myheartcounts.navigation.MHCRoute
import edu.stanford.myheartcounts.navigation.NavigationEvent
import edu.stanford.myheartcounts.navigation.Navigator
import edu.stanford.myheartcounts.study.StudyQuestionnaire
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import org.grovealliance.core.logging.groveLogger
import java.util.concurrent.atomic.AtomicBoolean

/**
 * An activity a participant performs, such as answering a questionnaire.
 */
sealed interface PerformableTask {
    /**
     * Answering [questionnaire].
     */
    data class AnswerQuestionnaire(val questionnaire: StudyQuestionnaire) : PerformableTask
}

/**
 * Presents tasks for the participant to perform, one at a time, and reports how each ended.
 */
interface TaskPerformer {
    /**
     * The task being performed, or `null` while none is.
     */
    val activeTask: StateFlow<ActiveTask?>

    /**
     * Presents [task] and suspends until it ends, returning whether the participant completed it.
     *
     * Returns `false` without presenting anything while another task is being performed.
     */
    suspend fun perform(task: PerformableTask): Boolean
}

/**
 * A task being performed. [finish] ends it; only the first call counts.
 */
class ActiveTask(
    val task: PerformableTask,
    private val onFinished: (didSucceed: Boolean) -> Unit,
) {
    private val isFinished = AtomicBoolean(false)

    /**
     * Ends the task, reporting whether the participant completed it.
     */
    fun finish(didSucceed: Boolean) {
        if (isFinished.compareAndSet(false, true)) onFinished(didSucceed)
    }
}

/**
 * Presents each task on the [MHCRoute.PerformTask] screen.
 */
class TaskPerformerImpl(
    private val navigator: Navigator,
) : TaskPerformer {
    private val logger by groveLogger()
    private val mutableActiveTask = MutableStateFlow<ActiveTask?>(null)

    override val activeTask: StateFlow<ActiveTask?> = mutableActiveTask.asStateFlow()

    override suspend fun perform(task: PerformableTask): Boolean {
        val result = CompletableDeferred<Boolean>()
        val activeTask = ActiveTask(task = task, onFinished = { result.complete(it) })
        if (!mutableActiveTask.compareAndSet(expect = null, update = activeTask)) {
            logger.w { "Ignoring a task while another one is being performed." }
            return false
        }
        return try {
            navigator.push(event = NavigationEvent.NavigateTo(route = MHCRoute.PerformTask))
            result.await()
        } finally {
            mutableActiveTask.compareAndSet(expect = activeTask, update = null)
        }
    }
}
