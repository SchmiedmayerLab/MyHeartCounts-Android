//
// This source file is part of the My Heart Counts Android open-source project
//
// SPDX-FileCopyrightText: 2026 Stanford University and the project authors (see CONTRIBUTORS.md)
//
// SPDX-License-Identifier: MIT

package edu.stanford.myheartcounts.dashboard

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.flow.flow

/**
 * A [HeartHealthScoreSource] emitting whatever scores were last set, or failing once [fail] was called.
 */
class FakeHeartHealthScoreSource : HeartHealthScoreSource {

    private val current = MutableStateFlow<HeartHealthScores?>(null)
    private var failure: Throwable? = null

    override val scores: Flow<HeartHealthScores>
        get() = failure?.let { error -> flow { throw error } } ?: current.filterNotNull()

    /**
     * Emits [scores] to current and future collectors.
     */
    fun setScores(scores: HeartHealthScores) {
        current.value = scores
    }

    /**
     * Makes future collections fail with [error].
     */
    fun fail(error: Throwable) {
        failure = error
    }
}
