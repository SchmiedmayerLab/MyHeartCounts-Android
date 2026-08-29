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
    const val ALGORITHM = "uuid-v5-framed-identifier-v0"
    const val ENTRY_IDENTIFIER_EXTENSION = HealthConnectContract.GROVE_EXCHANGE_ENTRY_NODE_KEY

    private val namespace = UUID.fromString("43df4575-bff7-5a57-9a80-2472cd2b0623")

    /** Returns the deterministic lowercase UUID URN for one complete business Identifier. */
    fun fullUrl(identifier: Identifier): String {
        FhirIdentifierKey.from(identifier)
        return "urn:uuid:${uuidV5(namespace, identifierName(identifier))}"
    }

    /** Returns the required Bundle.entry extension containing the exact derivation Identifier. */
    fun entryIdentifierExtension(identifier: Identifier): Extension {
        FhirIdentifierKey.from(identifier)
        return Extension(ENTRY_IDENTIFIER_EXTENSION, identifier.copy())
    }

    internal fun identifierName(system: String, value: String): ByteArray =
        identifierName(Identifier().setSystem(system).setValue(value))

    /**
     * The UUID-v5 name is the protocol's length-framed UTF-8 `[system, value]` pair.
     */
    private fun identifierName(identifier: Identifier): ByteArray {
        GroveUnicode.requireScalarText(identifier.system, "Identifier.system")
        GroveUnicode.requireScalarText(identifier.value, "Identifier.value")
        return GroveExchangeProtocol.frameFields(listOf(identifier.system, identifier.value))
    }

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
