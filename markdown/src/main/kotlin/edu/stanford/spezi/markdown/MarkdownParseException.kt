//
// This source file is part of the My Heart Counts Android open-source project
//
// SPDX-FileCopyrightText: 2026 Stanford University and the project authors (see CONTRIBUTORS.md)
//
// SPDX-License-Identifier: MIT

package edu.stanford.spezi.markdown

/**
 * An error raised while processing markdown text into a [MarkdownDocument].
 *
 * @property kind The kind of error that occurred.
 * @property location The source location at which the error occurred.
 */
class MarkdownParseException(
    val kind: Kind,
    val location: SourceLocation,
) : Exception("Markdown parse error ($kind) at $location") {

    /**
     * The kind of a [MarkdownParseException].
     */
    sealed interface Kind {
        /**
         * The parser reached the end of the input unexpectedly.
         */
        data object Eof : Kind

        /**
         * The parser encountered an unexpected character.
         */
        data object UnexpectedCharacter : Kind

        /**
         * Some other issue, described by [message].
         */
        data class Other(val message: String) : Kind
    }

    /**
     * A line and column position within a markdown document, both starting at 0.
     */
    data class SourceLocation(val line: Int, val column: Int)
}
