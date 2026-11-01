//
// This source file is part of the My Heart Counts Android open-source project
//
// SPDX-FileCopyrightText: 2026 Stanford University and the project authors (see CONTRIBUTORS.md)
//
// SPDX-License-Identifier: MIT

package edu.stanford.myheartcounts.study

import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.withContext
import org.grovealliance.core.coroutines.runCatchingAsync
import org.grovealliance.markdown.MarkdownDocument
import org.grovealliance.scheduler.Event
import org.grovealliance.study.ScheduledTaskAction
import org.grovealliance.study.ScheduledTaskActionKey

/**
 * Supplies the article a participant is asked to read.
 */
interface StudyArticleSource {
    /**
     * The article [event] asks the participant to read.
     *
     * Fails when the event carries no article, or when the study bundle does not hold one for it.
     */
    suspend fun article(event: Event): Result<MarkdownDocument>
}

/**
 * Reads articles from the study bundle, resolving each in the [StudyVariantProvider.contentLocale].
 */
class StudyArticleSourceImpl(
    private val studyBundleProvider: MHCStudyBundleProvider,
    private val variantProvider: StudyVariantProvider,
    private val ioDispatcher: CoroutineDispatcher,
) : StudyArticleSource {

    override suspend fun article(event: Event): Result<MarkdownDocument> = runCatchingAsync {
        val action = event.task.context[ScheduledTaskActionKey]
        require(action is ScheduledTaskAction.PresentInformational) {
            "The task ${event.task.id} does not present an article."
        }
        val bundle = studyBundleProvider.get().getOrThrow()
        withContext(ioDispatcher) {
            val file = bundle.resolve(
                fileRef = action.component.fileRef,
                locale = variantProvider.contentLocale(),
            )
            requireNotNull(file?.takeIf { it.exists() }) {
                "The study bundle holds no article for the task ${event.task.id}."
            }.readText()
        }.let { text -> MarkdownDocument.process(text = text) }
    }
}
