//
// This source file is part of the My Heart Counts Android open-source project
//
// SPDX-FileCopyrightText: 2026 Stanford University and the project authors (see CONTRIBUTORS.md)
//
// SPDX-License-Identifier: MIT

package edu.stanford.myheartcounts.standard.consent

import edu.stanford.myheartcounts.study.MHCStudyBundleProvider
import org.grovealliance.core.Module
import org.grovealliance.markdown.MarkdownDocument
import java.util.Locale

/**
 * The consent document the study ships, as text for Grove's consent screen and as a parsed
 * [MarkdownDocument] for the PDF the participant's signed copy is rendered from.
 *
 * Both go through here so the screen and the record cannot end up showing different documents.
 */
class MHCConsentDocumentProvider(
    private val studyBundleProvider: MHCStudyBundleProvider,
) : Module {

    /**
     * The consent markdown for [locale], as Grove's consent configuration consumes it.
     *
     * @throws IllegalArgumentException if the study bundle carries no consent document.
     */
    suspend fun text(locale: Locale = Locale.getDefault()): String {
        val bundle = studyBundleProvider.get().getOrThrow()
        return requireNotNull(bundle.consentText(locale = locale)) {
            "The study bundle carries no consent document."
        }
    }

    /**
     * The consent document parsed into frontmatter and blocks, with the interactive elements
     * extracted rather than left as literal markup.
     */
    suspend fun document(locale: Locale = Locale.getDefault()): MarkdownDocument =
        MarkdownDocument.process(
            text = text(locale = locale),
            customElementNames = CONSENT_ELEMENT_NAMES,
        )

    companion object {

        /**
         * The consent element vocabulary.
         *
         * Grove keeps its own copy internal to `:consent` (`ConsentConstants.ELEMENT_NAMES`), so the
         * app cannot reference it; parsing with the same set is what makes the document this
         * produces identical to the one the consent screen rendered. Keep the two in step.
         */
        val CONSENT_ELEMENT_NAMES: Set<String> = setOf(
            TAG_TOGGLE, TAG_SELECT, TAG_SIGNATURE, TAG_OPTION, TAG_FOOTNOTE,
        )

        internal const val TAG_TOGGLE = "toggle"
        internal const val TAG_SELECT = "select"
        internal const val TAG_SIGNATURE = "signature"
        internal const val TAG_OPTION = "option"
        internal const val TAG_FOOTNOTE = "footnote"
    }
}
