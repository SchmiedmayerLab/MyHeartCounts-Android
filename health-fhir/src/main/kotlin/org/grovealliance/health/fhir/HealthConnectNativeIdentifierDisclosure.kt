//
// This source file is part of the My Heart Counts Android open-source project
//
// SPDX-FileCopyrightText: 2026 Stanford University and the project authors (see CONTRIBUTORS.md)
//
// SPDX-License-Identifier: MIT

package org.grovealliance.health.fhir

import org.hl7.fhir.r4.model.CodeableConcept
import org.hl7.fhir.r4.model.Coding
import org.hl7.fhir.r4.model.Identifier

/** One immutable coding in an optional source-native [Identifier.type]. */
data class HealthConnectNativeIdentifierTypeCoding(
    val system: String,
    val code: String,
    val display: String? = null,
) {
    init {
        GroveUnicode.requireScalarText(system, "Native Identifier.type Coding.system")
        GroveUnicode.requireScalarText(code, "Native Identifier.type Coding.code")
        display?.let {
            GroveUnicode.requireScalarText(it, "Native Identifier.type Coding.display")
            require(it.isNotBlank()) { "Native Identifier.type Coding.display must not be blank." }
        }
        require(system.isAbsoluteAsciiUri()) {
            "Native Identifier.type Coding.system must be an absolute ASCII RFC 3986 URI."
        }
        require(system != HealthConnectContract.GROVE_IDENTIFIER_ROLE) {
            "A source-native Identifier.type cannot claim a Grove graph identity role."
        }
        require(FHIR_CODE.matches(code)) {
            "Native Identifier.type Coding.code must have no leading, trailing, consecutive, or control whitespace."
        }
    }

    internal fun coding(): Coding = Coding(system, code, display)

    private companion object {
        val FHIR_CODE = Regex("""[^\s\p{Cc}]+(?: [^\s\p{Cc}]+)*""")
    }
}

/** Immutable narrow CodeableConcept input for an optional source-native [Identifier.type]. */
class HealthConnectNativeIdentifierType(
    codings: List<HealthConnectNativeIdentifierTypeCoding> = emptyList(),
    val text: String? = null,
) {
    private val codingSnapshot = codings.toList()

    val codings: List<HealthConnectNativeIdentifierTypeCoding>
        get() = codingSnapshot.toList()

    init {
        text?.let {
            GroveUnicode.requireScalarText(it, "Native Identifier.type text")
            require(it.isNotBlank()) { "Native Identifier.type text must not be blank." }
        }
        require(codingSnapshot.isNotEmpty() || text != null) {
            "Native Identifier.type requires at least one coding or nonblank text."
        }
    }

    internal fun concept(): CodeableConcept = CodeableConcept().apply {
        codingSnapshot.forEach { addCoding(it.coding()) }
        text = this@HealthConnectNativeIdentifierType.text
    }
}

/**
 * Explicit opt-in to disclose Health Connect [androidx.health.connect.client.records.metadata.Metadata.id].
 *
 * [system] must name the caller-governed repository/store key space in which that native value is
 * unique. The native value itself is taken directly from Metadata at conversion time, so callers
 * cannot accidentally disclose a different identifier. This Identifier supplements Grove's
 * mandatory opaque identities and is never used as an entry, event, or retraction key.
 */
class HealthConnectNativeIdentifierDisclosure(
    val system: String,
    val type: HealthConnectNativeIdentifierType? = null,
) {
    init {
        GroveUnicode.requireScalarText(system, "Native Identifier.system")
        require(system.isAbsoluteAsciiUri()) {
            "Native Identifier.system must be a caller-owned absolute ASCII RFC 3986 URI."
        }
        require(system != HealthConnectContract.GROVE_IDENTIFIER_ROLE) {
            "Native Identifier.system cannot be the Grove graph-role CodeSystem."
        }
    }

    internal fun identifier(
        nativeId: String,
        eventIdentifierSystem: String,
        entryNodeIdentifierSystem: String,
        identityKey: GroveHmacIdentityKey,
    ): Identifier {
        GroveUnicode.requireScalarText(nativeId, "Metadata.id")
        require(nativeId.isNotBlank()) { "Metadata.id must not be blank." }
        val reservedSystems = buildSet {
            add(eventIdentifierSystem)
            add(entryNodeIdentifierSystem)
            GroveOpaqueIdentityKind.entries.forEach { add(identityKey.identifierSystem(it)) }
        }
        require(system !in reservedSystems) {
            "Native Identifier.system requires its own repository/store namespace, never a Grove identity system."
        }
        return Identifier().apply {
            system = this@HealthConnectNativeIdentifierDisclosure.system
            value = nativeId
            this@HealthConnectNativeIdentifierDisclosure.type?.let { type = it.concept() }
        }
    }
}
