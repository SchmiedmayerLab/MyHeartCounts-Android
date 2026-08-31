//
// This source file is part of the My Heart Counts Android open-source project
//
// SPDX-FileCopyrightText: 2026 Stanford University and the project authors (see CONTRIBUTORS.md)
//
// SPDX-License-Identifier: MIT

package org.grovealliance.health.fhir

import org.hl7.fhir.r4.model.Identifier
import java.net.URI

/** An immutable, complete FHIR logical identifier; neither component may be discarded. */
data class FhirIdentifierKey(
    val system: String,
    val value: String,
) : Comparable<FhirIdentifierKey> {
    init {
        GroveUnicode.requireScalarText(system, "Identifier.system")
        GroveUnicode.requireScalarText(value, "Identifier.value")
        require(system.isAbsoluteAsciiUri()) {
            "Identifier.system must be a nonblank absolute ASCII RFC 3986 URI."
        }
        require(value.isNotBlank()) { "Identifier.value must not be blank." }
    }

    fun identifier(): Identifier = Identifier().setSystem(system).setValue(value)

    override fun compareTo(other: FhirIdentifierKey): Int =
        compareValuesBy(this, other, FhirIdentifierKey::system, FhirIdentifierKey::value)

    companion object {
        fun from(identifier: Identifier): FhirIdentifierKey {
            require(identifier.hasSystem() && identifier.hasValue()) {
                "A complete Identifier.system and Identifier.value are required."
            }
            return FhirIdentifierKey(identifier.system, identifier.value)
        }
    }
}

internal fun Identifier.key(): FhirIdentifierKey = FhirIdentifierKey.from(this)

internal fun Identifier.hasGroveRole(role: GroveIdentifierRole): Boolean =
    type.coding.any {
        it.system == HealthConnectContract.GROVE_IDENTIFIER_ROLE && it.code == role.code
    }

/** FHIR identity systems use URI syntax, never an unescaped IRI or whitespace-bearing string. */
internal fun String.isAbsoluteAsciiUri(): Boolean =
    isNotBlank() && all { it.code in ASCII_URI_FIRST..ASCII_URI_LAST } &&
        runCatching { URI(this).isAbsolute }.getOrDefault(false)

private const val ASCII_URI_FIRST = 0x21
private const val ASCII_URI_LAST = 0x7e
