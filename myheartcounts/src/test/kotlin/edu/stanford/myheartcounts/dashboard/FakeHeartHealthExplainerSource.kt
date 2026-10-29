//
// This source file is part of the My Heart Counts Android open-source project
//
// SPDX-FileCopyrightText: 2026 Stanford University and the project authors (see CONTRIBUTORS.md)
//
// SPDX-License-Identifier: MIT

package edu.stanford.myheartcounts.dashboard

import org.grovealliance.markdown.MarkdownDocument

/**
 * A [HeartHealthExplainerSource] returning whatever explanation was last set, or none.
 */
class FakeHeartHealthExplainerSource : HeartHealthExplainerSource {

    private var explainer: MarkdownDocument? = null

    override suspend fun scoreExplainer(): MarkdownDocument? = explainer

    /**
     * Returns [explainer] from now on; `null` stands for a study without one.
     */
    fun setExplainer(explainer: MarkdownDocument?) {
        this.explainer = explainer
    }
}
