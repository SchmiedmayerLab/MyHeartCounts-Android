//
// This source file is part of the My Heart Counts Android open-source project
//
// SPDX-FileCopyrightText: 2026 Stanford University and the project authors (see CONTRIBUTORS.md)
//
// SPDX-License-Identifier: MIT
//

package org.grovealliance.health.fhir

/**
 * An opaque digest naming one export scope.
 *
 * Holding this as a type rather than a `String` means the digest is checked where it is minted
 * instead of at every holder that stores it, and a repository scope can no longer be passed where
 * a projection scope belongs.
 */
@JvmInline
value class ScopeKey(val value: String) {
    init {
        require(value.matches(PATTERN)) { "A scope key must be an opaque v1 digest." }
    }

    override fun toString(): String = value

    private companion object {
        val PATTERN = Regex("v1:[0-9a-f]{64}")
    }
}

/**
 * The durable, strictly increasing position of one export event.
 *
 * The sequence participates in the exchange identity, so it is text rather than a number: a
 * deployment's counter may outgrow [Long], and its canonical decimal spelling has to survive a
 * round trip through the journal unchanged.
 */
@JvmInline
value class EventSequence(val value: String) : Comparable<EventSequence> {
    init {
        require(value.matches(PATTERN)) {
            "An event sequence must be a positive canonical decimal integer starting at one."
        }
    }

    /**
     * Numeric order over canonical decimals: a longer spelling is always the larger number, and
     * equal lengths compare lexicographically.
     */
    override fun compareTo(other: EventSequence): Int =
        compareValuesBy(this, other, { it.value.length }, { it.value })

    override fun toString(): String = value

    private companion object {
        val PATTERN = Regex("[1-9][0-9]*")
    }
}
