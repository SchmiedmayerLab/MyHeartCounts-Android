//
// This source file is part of the My Heart Counts open-source project
//
// SPDX-FileCopyrightText: 2026 Stanford University and the project authors (see CONTRIBUTORS.md)
//
// SPDX-License-Identifier: MIT

package edu.stanford.spezi.markdown

import edu.stanford.spezi.foundation.SemanticVersion
import kotlinx.serialization.Serializable

/**
 * A string-based key-value mapping holding the frontmatter metadata of a [MarkdownDocument].
 */
@Serializable
data class MarkdownMetadata(
    val values: Map<String, String> = emptyMap(),
) {
    /**
     * The metadata value associated with [key], if present.
     */
    operator fun get(key: String): String? = values[key]

    /**
     * The document's title, if present.
     */
    val title: String? get() = values["title"]

    /**
     * The document's version, if present and parseable as a [SemanticVersion].
     */
    val version: SemanticVersion? get() = values["version"]?.let { SemanticVersion(it) }
}
