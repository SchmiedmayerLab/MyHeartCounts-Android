//
// This source file is part of the My Heart Counts Android open-source project
//
// SPDX-FileCopyrightText: 2026 Stanford University and the project authors (see CONTRIBUTORS.md)
//
// SPDX-License-Identifier: MIT

package edu.stanford.myheartcounts.dashboard.detail

import edu.stanford.myheartcounts.dashboard.HeartScoreComponent
import org.grovealliance.markdown.MarkdownDocument

/**
 * A [HeartScoreExplainerSource] returning the explanation last set, or failing with the failure
 * last set.
 */
class FakeHeartScoreExplainerSource : HeartScoreExplainerSource {

    private var document: MarkdownDocument? = null
    private var failure: Throwable? = null

    override suspend fun document(component: HeartScoreComponent): MarkdownDocument? {
        failure?.let { throw it }
        return document
    }

    /**
     * Returns [document] from now on; `null` stands for a study without an explanation.
     */
    fun setDocument(document: MarkdownDocument?) {
        this.document = document
    }

    /**
     * Fails with [failure] from now on, or stops failing when it is `null`.
     */
    fun setFailure(failure: Throwable?) {
        this.failure = failure
    }
}
