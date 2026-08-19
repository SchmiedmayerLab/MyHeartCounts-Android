//
// This source file is part of the My Heart Counts Android open-source project
//
// SPDX-FileCopyrightText: 2026 Stanford University and the project authors (see CONTRIBUTORS.md)
//
// SPDX-License-Identifier: MIT

package org.grovealliance.consent

/**
 * The source from which a consent document is loaded.
 */
sealed interface ConsentDocument {
    /**
     * Loads the document from an asset file at [filename] (relative to the app's assets root).
     */
    data class Asset(val filename: String) : ConsentDocument

    /**
     * Uses the given markdown [text] directly as the document source.
     */
    data class Text(val text: String) : ConsentDocument
}
