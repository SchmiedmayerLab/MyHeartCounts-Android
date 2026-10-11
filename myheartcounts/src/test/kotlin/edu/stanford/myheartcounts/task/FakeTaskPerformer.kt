//
// This source file is part of the My Heart Counts Android open-source project
//
// SPDX-FileCopyrightText: 2026 Stanford University and the project authors (see CONTRIBUTORS.md)
//
// SPDX-License-Identifier: MIT

package edu.stanford.myheartcounts.task

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow

/**
 * A [TaskPerformer] whose active task is set and cleared directly, and which records how each one
 * finished. Finishing a task leaves it active, as the screen performing it is what ends it here.
 */
class FakeTaskPerformer : TaskPerformer {
    private val mutableActiveTask = MutableStateFlow<ActiveTask?>(null)
    private val _outcomes = mutableListOf<Boolean>()
    private val _performedTasks = mutableListOf<PerformableTask>()
    private var performResult = false

    /**
     * How each task set through [setActiveTask] finished, in order.
     */
    val outcomes: List<Boolean> get() = _outcomes

    /**
     * Every task passed to [perform], in order.
     */
    val performedTasks: List<PerformableTask> get() = _performedTasks

    override val activeTask: StateFlow<ActiveTask?> = mutableActiveTask

    /**
     * Makes [task] the active task, recording its outcome once it finishes.
     */
    fun setActiveTask(task: PerformableTask) {
        mutableActiveTask.value = ActiveTask(
            task = task,
            onFinished = { didSucceed -> _outcomes += didSucceed },
        )
    }

    /**
     * Ends the active task without it finishing, as when whoever asked for it stops waiting.
     */
    fun clearActiveTask() {
        mutableActiveTask.value = null
    }

    /**
     * Sets what [perform] returns.
     */
    fun setPerformResult(didSucceed: Boolean) {
        performResult = didSucceed
    }

    override suspend fun perform(task: PerformableTask): Boolean {
        _performedTasks += task
        return performResult
    }
}
