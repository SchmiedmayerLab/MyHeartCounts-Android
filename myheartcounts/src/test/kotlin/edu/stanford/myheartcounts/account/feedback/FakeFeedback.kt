//
// This source file is part of the My Heart Counts Android open-source project
//
// SPDX-FileCopyrightText: 2026 Stanford University and the project authors (see CONTRIBUTORS.md)
//
// SPDX-License-Identifier: MIT

package edu.stanford.myheartcounts.account.feedback

/**
 * A [MHCFeedback] that records the messages it is asked to send and answers with a configured result.
 */
class FakeFeedback : MHCFeedback {
    private var result: Result<Unit> = Result.success(Unit)
    private val _messages = mutableListOf<String>()

    /**
     * The messages passed to [submit], in call order.
     */
    val messages: List<String> get() = _messages

    /**
     * Sets what [submit] returns.
     */
    fun setResult(result: Result<Unit>) {
        this.result = result
    }

    override suspend fun submit(message: String): Result<Unit> {
        _messages.add(message)
        return result
    }
}
