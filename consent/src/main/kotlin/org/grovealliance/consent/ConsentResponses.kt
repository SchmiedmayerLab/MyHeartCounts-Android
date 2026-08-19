//
// This source file is part of the My Heart Counts Android open-source project
//
// SPDX-FileCopyrightText: 2026 Stanford University and the project authors (see CONTRIBUTORS.md)
//
// SPDX-License-Identifier: MIT

package org.grovealliance.consent

/**
 * The values a user has entered for the interactive elements of a consent document, keyed by element id.
 *
 * @property toggles Responses to toggle elements.
 * @property selects Responses to select elements, holding the id of the chosen option.
 * @property signatures Signature metadata captured for signature elements.
 */
data class ConsentResponses(
    val toggles: Map<String, Boolean>,
    val selects: Map<String, String>,
    val signatures: Map<String, SignatureMetadata>,
) {

    companion object {
        /**
         * An empty set of responses, representing a user who has not interacted with any elements.
         */
        val Empty = ConsentResponses(
            toggles = emptyMap(),
            selects = emptyMap(),
            signatures = emptyMap(),
        )
    }
}

/**
 * Metadata captured by a signature element.
 *
 * @property givenName The signer's first name.
 * @property familyName The signer's last name.
 * @property strokes The strokes making up the hand-drawn signature.
 */
data class SignatureMetadata(
    val givenName: String,
    val familyName: String,
    val strokes: List<SignatureStroke>,
) {

    companion object {
        /**
         * Empty signature metadata for a user who has not entered any signature data.
         */
        val Empty = SignatureMetadata(
            givenName = "",
            familyName = "",
            strokes = emptyList(),
        )
    }
}

/**
 * A single continuous stroke of a hand-drawn signature.
 */
data class SignatureStroke(val points: List<SignaturePoint>)

/**
 * A point within a [SignatureStroke], in the signature canvas' coordinate space.
 */
data class SignaturePoint(val x: Float, val y: Float)
