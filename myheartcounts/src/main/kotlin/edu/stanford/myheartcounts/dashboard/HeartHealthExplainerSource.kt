//
// This source file is part of the My Heart Counts Android open-source project
//
// SPDX-FileCopyrightText: 2026 Stanford University and the project authors (see CONTRIBUTORS.md)
//
// SPDX-License-Identifier: MIT

package edu.stanford.myheartcounts.dashboard

import edu.stanford.myheartcounts.study.MHCStudyBundleProvider
import edu.stanford.myheartcounts.study.StudyVariantProvider
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.withContext
import org.grovealliance.core.coroutines.runCatchingAsync
import org.grovealliance.core.logging.groveLogger
import org.grovealliance.markdown.MarkdownDocument
import org.grovealliance.studydefinition.FileReference

/**
 * Supplies the study's explanations of the heart health score.
 */
interface HeartHealthExplainerSource {

    /**
     * The explanation of what the overall score is and how it is calculated, in the study's content
     * locale; `null` when the study has none or it cannot be read.
     */
    suspend fun scoreExplainer(): MarkdownDocument?
}

/**
 * Reads the explanations from the study bundle of [studyBundleProvider], on [ioDispatcher].
 */
class HeartHealthExplainerSourceImpl(
    private val studyBundleProvider: MHCStudyBundleProvider,
    private val variantProvider: StudyVariantProvider,
    private val ioDispatcher: CoroutineDispatcher,
) : HeartHealthExplainerSource {

    private val logger by groveLogger()

    override suspend fun scoreExplainer(): MarkdownDocument? = runCatchingAsync {
        val bundle = studyBundleProvider.get().getOrThrow()
        withContext(ioDispatcher) {
            bundle.resolve(
                fileRef = SCORE_EXPLAINER,
                locale = variantProvider.contentLocale(),
            )?.takeIf { it.exists() }?.readText()
        }?.let { text -> MarkdownDocument.process(text = text) }
    }.onFailure { throwable ->
        logger.e(throwable) { "Failed to read the heart health score explainer" }
    }.getOrNull()

    private companion object {
        /**
         * The study bundle file explaining the overall score.
         */
        val SCORE_EXPLAINER = FileReference(
            category = FileReference.Category(rawValue = "hhdExplainer"),
            filename = "LearnMore",
            fileExtension = "md",
        )
    }
}
