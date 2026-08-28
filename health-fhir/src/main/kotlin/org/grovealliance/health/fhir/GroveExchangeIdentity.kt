//
// This source file belongs to the My Heart Counts Android project
//
// SPDX-FileCopyrightText: 2026 Stanford University and the project authors (see CONTRIBUTORS.md)
//
// SPDX-License-Identifier: MIT

package org.grovealliance.health.fhir

import org.hl7.fhir.r4.model.Bundle
import org.hl7.fhir.r4.model.Extension
import org.hl7.fhir.r4.model.Identifier
import org.hl7.fhir.r4.model.Resource
import java.nio.ByteBuffer
import java.security.MessageDigest
import java.util.UUID

/** Implements the Grove Mobile exchange-entry identity contract exactly. */
object GroveExchangeIdentity {
    const val ALGORITHM = "uuid-v5-composed-identifier-v1"
    const val ENTRY_IDENTIFIER_EXTENSION =
        "https://grovealliance.org/fhir/mobile/StructureDefinition/grove-exchange-entry-identifier"

    private val namespace = UUID.fromString("a9a39cf1-c944-5d15-a3c2-c395969ea101")

    /** Returns the deterministic lowercase UUID URN for one complete business Identifier. */
    fun fullUrl(identifier: Identifier): String {
        require(identifier.hasSystem() && identifier.hasValue()) {
            "A Grove exchange entry requires a complete Identifier system and value."
        }
        return "urn:uuid:${uuidV5(namespace, identifierName(identifier).toByteArray(Charsets.UTF_8))}"
    }

    /** Returns the required Bundle.entry extension containing the exact derivation Identifier. */
    fun entryIdentifierExtension(identifier: Identifier): Extension {
        require(identifier.hasSystem() && identifier.hasValue()) {
            "A Grove exchange entry requires a complete Identifier system and value."
        }
        return Extension(ENTRY_IDENTIFIER_EXTENSION, identifier.copy())
    }

    internal fun identifierName(system: String, value: String): String =
        identifierName(Identifier().setSystem(system).setValue(value))

    /**
     * The UUID-v5 name for one identifier: the system, a vertical bar, then the value.
     *
     * Only the system is barred from containing a vertical bar, so the name splits at the first
     * one. A value may contain them, because a composed identifier is built from them.
     */
    private fun identifierName(identifier: Identifier): String {
        require(!identifier.system.contains(SEPARATOR)) {
            "An identifier system must not contain a vertical bar."
        }
        listOf(identifier.system, identifier.value).forEach { text ->
            require(text.none { it.isSurrogate() }) {
                "An identifier must not contain an isolated Unicode surrogate."
            }
        }
        return "${identifier.system}$SEPARATOR${identifier.value}"
    }

    private const val SEPARATOR = "|"

    private fun uuidV5(namespace: UUID, name: ByteArray): UUID {
        val namespaceBytes = ByteBuffer.allocate(UUID_BYTE_COUNT)
            .putLong(namespace.mostSignificantBits)
            .putLong(namespace.leastSignificantBits)
            .array()
        val digest = MessageDigest.getInstance("SHA-1").apply {
            update(namespaceBytes)
            update(name)
        }.digest()
        digest[UUID_VERSION_BYTE_INDEX] = (
            digest[UUID_VERSION_BYTE_INDEX].toInt() and UUID_VERSION_CLEAR_MASK or UUID_VERSION_FIVE_BITS
            ).toByte()
        digest[UUID_VARIANT_BYTE_INDEX] = (
            digest[UUID_VARIANT_BYTE_INDEX].toInt() and UUID_VARIANT_CLEAR_MASK or UUID_IETF_VARIANT_BITS
            ).toByte()
        val buffer = ByteBuffer.wrap(digest, 0, UUID_BYTE_COUNT)
        return UUID(buffer.long, buffer.long)
    }

    private const val UUID_VERSION_BYTE_INDEX = 6
    private const val UUID_VARIANT_BYTE_INDEX = 8
    private const val UUID_VERSION_CLEAR_MASK = 0x0f
    private const val UUID_VERSION_FIVE_BITS = 0x50
    private const val UUID_VARIANT_CLEAR_MASK = 0x3f
    private const val UUID_IETF_VARIANT_BITS = 0x80
    private const val UUID_BYTE_COUNT = 16
}

internal fun Bundle.addGroveEntry(
    entryIdentifier: Identifier,
    entryResource: Resource,
): Bundle.BundleEntryComponent = addEntry().apply {
    fullUrl = GroveExchangeIdentity.fullUrl(entryIdentifier)
    addExtension(GroveExchangeIdentity.entryIdentifierExtension(entryIdentifier))
    resource = entryResource
}
