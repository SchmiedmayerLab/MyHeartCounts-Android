//
// This source file belongs to the My Heart Counts Android project
//
// SPDX-FileCopyrightText: 2026 Stanford University and the project authors (see CONTRIBUTORS.md)
//
// SPDX-License-Identifier: MIT

package org.grovealliance.health.fhir

import org.hl7.fhir.r4.model.CodeableConcept
import org.hl7.fhir.r4.model.Coding
import org.hl7.fhir.r4.model.Identifier
import java.net.URI
import java.util.Base64
import javax.crypto.Mac
import javax.crypto.spec.SecretKeySpec

/**
 * One deployment-owned Grove identity key epoch.
 *
 * The caller persists the secret and its metadata outside FHIR. Rotating either the key or its
 * epoch intentionally creates a new Identifier.system and a new identity space; migration is an
 * explicit retraction/republication operation, never an invisible in-place rewrite.
 */
class GroveHmacIdentityKey private constructor(
    val identifierSystemFamily: String,
    val keyId: String,
    val epoch: String,
    secret: ByteArray,
    allowPublicConformanceKey: Boolean,
) {
    private val secretSnapshot = secret.copyOf()

    constructor(
        identifierSystemFamily: String,
        keyId: String,
        epoch: String,
        secret: ByteArray,
    ) : this(identifierSystemFamily, keyId, epoch, secret, false)

    init {
        GroveUnicode.requireScalarText(identifierSystemFamily, "HMAC identity-system family")
        GroveUnicode.requireScalarText(keyId, "HMAC key id")
        require(
            identifierSystemFamily.isAbsoluteAsciiUri() &&
                !identifierSystemFamily.endsWith('/') &&
                runCatching {
                    val uri = URI(identifierSystemFamily)
                    uri.rawQuery == null && uri.rawFragment == null
                }.getOrDefault(false)
        ) {
            "The HMAC identity-system family must be a deployment-owned absolute ASCII " +
                "RFC 3986 URI without a trailing slash, query, or fragment."
        }
        require(KEY_ID.matches(keyId)) {
            "The HMAC key id must use only URI-safe unreserved characters."
        }
        require(POSITIVE_DECIMAL.matches(epoch)) {
            "The HMAC key epoch must be a positive canonical decimal integer."
        }
        require(secretSnapshot.size >= MINIMUM_KEY_BYTES) {
            "A Grove HMAC-SHA-256 identity key must contain at least 256 bits of secret material."
        }
        require(allowPublicConformanceKey || !secretSnapshot.contentEquals(PUBLIC_CONFORMANCE_KEY)) {
            "The published Grove conformance key is prohibited in production identity configuration."
        }
    }

    /** Builds an opaque, role-typed FHIR Identifier from unambiguous length-framed UTF-8. */
    internal fun identifier(
        identityKind: GroveOpaqueIdentityKind,
        vararg components: String,
    ): Identifier = identifier(identityKind, components.asList())

    /** List overload avoids transient arrays when a converter extends an existing component tuple. */
    internal fun identifier(
        identityKind: GroveOpaqueIdentityKind,
        components: List<String>,
    ): Identifier = Identifier().apply {
        system = identifierSystem(identityKind)
        value = value(identityKind, components)
        type = CodeableConcept(
            Coding(
                HealthConnectContract.GROVE_IDENTIFIER_ROLE,
                identityKind.identifierRole.code,
                identityKind.identifierRole.display,
            ),
        )
    }

    internal fun value(identityKind: GroveOpaqueIdentityKind, vararg components: String): String =
        value(identityKind, components.asList())

    internal fun value(identityKind: GroveOpaqueIdentityKind, components: List<String>): String {
        require(components.size == identityKind.componentCount) {
            "${identityKind.code} requires exactly ${identityKind.componentCount} ordered components."
        }
        components.forEachIndexed { index, component ->
            GroveUnicode.requireScalarText(component, "${identityKind.code} component[$index]")
            require(component.isNotEmpty()) {
                "${identityKind.code} component[$index] must not be empty."
            }
        }
        val firstComponent = components.first()
        when (identityKind) {
            GroveOpaqueIdentityKind.PROVIDER_RECORD,
            GroveOpaqueIdentityKind.PROVIDER_OUTPUT,
            GroveOpaqueIdentityKind.PROVIDER_ARTIFACT,
            -> require(firstComponent in HealthConnectContract.providerCodes) {
                "${identityKind.code} component[0] must be an exact catalog provider code."
            }

            GroveOpaqueIdentityKind.SOURCE_RECORD,
            GroveOpaqueIdentityKind.SOURCE_OUTPUT,
            GroveOpaqueIdentityKind.SOURCE_ARTIFACT,
            -> require(firstComponent !in HealthConnectContract.providerCodes) {
                "Provider coordinates require the matching provider-record, provider-output, " +
                    "or provider-artifact identity kind."
            }

            else -> Unit
        }
        val preimage = GroveExchangeProtocol.frameFields(
            buildList {
                add(DOMAIN)
                add(identityKind.code)
                addAll(components)
            },
        )
        val digest = Mac.getInstance(HMAC_SHA_256).run {
            init(SecretKeySpec(secretSnapshot, HMAC_SHA_256))
            doFinal(preimage)
        }
        val encoded = Base64.getUrlEncoder().withoutPadding().encodeToString(digest)
        return "v0:$keyId:$epoch:$encoded"
    }

    /** A system is immutable for one identity kind, key id, and key epoch. */
    internal fun identifierSystem(identityKind: GroveOpaqueIdentityKind): String =
        "$identifierSystemFamily/${identityKind.code}/$keyId/$epoch"

    override fun toString(): String =
        "GroveHmacIdentityKey(identifierSystemFamily=$identifierSystemFamily, keyId=$keyId, epoch=$epoch)"

    companion object {
        /** The public protocol vector key is reachable only from same-module conformance tests. */
        internal fun forConformanceTesting(
            identifierSystemFamily: String,
            keyId: String,
            epoch: String,
            secret: ByteArray,
        ): GroveHmacIdentityKey = GroveHmacIdentityKey(
            identifierSystemFamily,
            keyId,
            epoch,
            secret,
            true,
        )

        private const val DOMAIN = "org.grovealliance.fhir.identity.v0"
        private const val HMAC_SHA_256 = "HmacSHA256"
        private const val MINIMUM_KEY_BYTES = 32
        private val KEY_ID = Regex("[A-Za-z0-9._-]+")
        private val POSITIVE_DECIMAL = Regex("[1-9][0-9]*")
        private val PUBLIC_CONFORMANCE_KEY = ByteArray(MINIMUM_KEY_BYTES) { it.toByte() }
    }
}

private const val SOURCE_RECORD_COMPONENT_COUNT = 5
private const val SOURCE_OUTPUT_COMPONENT_COUNT = 7
private const val WRITER_RECORD_COMPONENT_COUNT = 3
private const val PROVIDER_RECORD_COMPONENT_COUNT = 5
private const val PROVIDER_OUTPUT_COMPONENT_COUNT = 7
private const val SOURCE_ARTIFACT_COMPONENT_COUNT = 7
private const val PROVIDER_ARTIFACT_COMPONENT_COUNT = 7
private const val SOURCE_CONTEXT_COMPONENT_COUNT = 5
private const val RECORDING_DEVICE_COMPONENT_COUNT = 4
private const val DEVICE_SNAPSHOT_COMPONENT_COUNT = 4

/** Closed HMAC identity shapes from the Grove FHIR exchange-protocol catalog. */
internal enum class GroveOpaqueIdentityKind(
    val code: String,
    val identifierRole: GroveIdentifierRole,
    val componentCount: Int,
) {
    SOURCE_RECORD("source-record", GroveIdentifierRole.SOURCE_RECORD, SOURCE_RECORD_COMPONENT_COUNT),
    SOURCE_OUTPUT("source-output", GroveIdentifierRole.SOURCE_OUTPUT, SOURCE_OUTPUT_COMPONENT_COUNT),
    WRITER_RECORD("writer-record", GroveIdentifierRole.WRITER_RECORD, WRITER_RECORD_COMPONENT_COUNT),
    PROVIDER_RECORD("provider-record", GroveIdentifierRole.SOURCE_RECORD, PROVIDER_RECORD_COMPONENT_COUNT),
    PROVIDER_OUTPUT("provider-output", GroveIdentifierRole.SOURCE_OUTPUT, PROVIDER_OUTPUT_COMPONENT_COUNT),
    SOURCE_ARTIFACT("source-artifact", GroveIdentifierRole.SOURCE_ARTIFACT, SOURCE_ARTIFACT_COMPONENT_COUNT),
    PROVIDER_ARTIFACT("provider-artifact", GroveIdentifierRole.SOURCE_ARTIFACT, PROVIDER_ARTIFACT_COMPONENT_COUNT),
    SOURCE_CONTEXT("source-context", GroveIdentifierRole.SOURCE_CONTEXT, SOURCE_CONTEXT_COMPONENT_COUNT),
    RECORDING_DEVICE("recording-device", GroveIdentifierRole.RECORDING_DEVICE, RECORDING_DEVICE_COMPONENT_COUNT),
    DEVICE_SNAPSHOT("device-snapshot", GroveIdentifierRole.DEVICE_SNAPSHOT, DEVICE_SNAPSHOT_COMPONENT_COUNT),
}

/** Closed role vocabulary carried in Identifier.type as well as the HMAC domain. */
enum class GroveIdentifierRole(val code: String, val display: String) {
    SOURCE_RECORD("source-record", "Source record"),
    SOURCE_OUTPUT("source-output", "Source output"),
    WRITER_RECORD("writer-record", "Writer record"),
    SOURCE_ARTIFACT("source-artifact", "Source artifact"),
    SOURCE_CONTEXT("source-context", "Source context"),
    RECORDING_DEVICE("recording-device", "Recording device"),
    DEVICE_SNAPSHOT("device-snapshot", "Device snapshot"),
    EVENT("event", "Event"),
    ENTRY_NODE("entry-node", "Entry node"),
}
