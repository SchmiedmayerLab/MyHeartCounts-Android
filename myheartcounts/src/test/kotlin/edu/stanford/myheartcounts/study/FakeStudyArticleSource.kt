//
// This source file is part of the My Heart Counts Android open-source project
//
// SPDX-FileCopyrightText: 2026 Stanford University and the project authors (see CONTRIBUTORS.md)
//
// SPDX-License-Identifier: MIT

package edu.stanford.myheartcounts.study

import org.grovealliance.markdown.MarkdownDocument
import org.grovealliance.scheduler.Event

/**
 * A [StudyArticleSource] that holds no articles.
 */
class FakeStudyArticleSource : StudyArticleSource {
    override suspend fun article(event: Event): Result<MarkdownDocument> =
        Result.failure(IllegalStateException("No article for ${event.task.id}"))
}
