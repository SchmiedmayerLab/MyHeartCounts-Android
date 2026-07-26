//
// This source file is part of the My Heart Counts open-source project
//
// SPDX-FileCopyrightText: 2026 Stanford University and the project authors (see CONTRIBUTORS.md)
//
// SPDX-License-Identifier: MIT

package edu.stanford.spezi.markdown

import edu.stanford.spezi.markdown.internal.MarkdownParser

/**
 * A processed markdown document, consisting of optional [metadata] and a series of content [blocks].
 *
 * Processing splits the input into markdown text blocks and extracts any custom HTML-style elements whose tag
 * names were declared up front. It does not perform full markdown parsing of the text blocks themselves.
 *
 * @property metadata The document's metadata.
 * @property blocks The document's content blocks.
 */
data class MarkdownDocument(
    val metadata: MarkdownMetadata,
    val blocks: List<MarkdownBlock>,
) {
    companion object {
        /**
         * Processes markdown [text] into a [MarkdownDocument].
         *
         * @param customElementNames Tag names that should be extracted as [MarkdownBlock.Element]s. Any element
         *   whose tag name is not listed is left in the surrounding markdown text verbatim.
         * @throws MarkdownParseException if the input cannot be processed.
         */
        fun process(text: String, customElementNames: Set<String> = emptySet()): MarkdownDocument =
            MarkdownParser(text, customElementNames).parse()
    }
}
