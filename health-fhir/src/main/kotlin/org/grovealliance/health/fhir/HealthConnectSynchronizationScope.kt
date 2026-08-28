//
// This source file belongs to the My Heart Counts Android project
//
// SPDX-FileCopyrightText: 2026 Stanford University and the project authors (see CONTRIBUTORS.md)
//
// SPDX-License-Identifier: MIT

package org.grovealliance.health.fhir

import java.nio.charset.StandardCharsets
import java.util.UUID

/**
 * Stable, local scope for one Health Connect repository and one synchronized projection.
 *
 * Health Connect's `Metadata.id` is scoped to a repository. The repository scope is a complete
 * deployment-owned Identifier pair that the caller must persist for the lifetime of exactly one
 * repository. [generateRepositoryScope] creates an opaque UUIDv4-based pair, but a deployment may
 * supply an equivalent governed pair. A scope must never be cloned into a different repository.
 * [configurationFingerprint] identifies the exact source/filter configuration used by the full
 * reader and must change when that projection changes. The adapter-owned conversion-contract
 * version is included automatically, so an adapter upgrade cannot resume an old cursor without a
 * new baseline. None of these inputs is serialized into FHIR or the exchange Bundle.
 */
class HealthConnectSynchronizationScope private constructor(
    internal val repositoryScope: FhirIdentifierKey,
    internal val producerInstance: String,
    configurationFingerprint: String,
    internal val conversionContractVersion: String,
    internal val identityKey: GroveHmacIdentityKey,
) {
    /** Opaque local key shared by every projection over this repository. */
    val repositoryScopeKey: ScopeKey = ScopeKey(
        digest(
            "repository\u0000${repositoryScope.system.sized()}\u0000${repositoryScope.value.sized()}",
        ),
    )

    /** Opaque local key for token state and one exact full-read/filter configuration. */
    val projectionScopeKey: ScopeKey = ScopeKey(
        digest(
            "projection\u0000${repositoryScope.system.sized()}\u0000${repositoryScope.value.sized()}" +
                "\u0000${configurationFingerprint.sized()}\u0000${conversionContractVersion.sized()}" +
                "\u0000${identityKey.identifierSystemFamily.sized()}" +
                "\u0000${identityKey.keyId.sized()}\u0000${identityKey.epoch.sized()}",
        ),
    )

    init {
        require(configurationFingerprint.isNotBlank()) {
            "An explicit Health Connect filter/configuration fingerprint is required."
        }
        require(producerInstance.isCanonicalProducerUuid()) {
            "The producer instance must be a durable canonical lowercase RFC 4122 UUID."
        }
        require(conversionContractVersion.isNotBlank()) {
            "The adapter conversion-contract version must not be blank."
        }
    }

    /** Produces a typed opaque source-record Identifier without exposing any native input field. */
    internal fun sourceRecordIdentifier(
        recordTypeToken: String,
        healthConnectId: String,
    ) = HealthConnectIdentity.record(identityKey, repositoryScope, recordTypeToken, healthConnectId)

    override fun equals(other: Any?): Boolean =
        other is HealthConnectSynchronizationScope &&
            repositoryScopeKey == other.repositoryScopeKey &&
            projectionScopeKey == other.projectionScopeKey

    override fun hashCode(): Int = 31 * repositoryScopeKey.hashCode() + projectionScopeKey.hashCode()

    override fun toString(): String =
        "HealthConnectSynchronizationScope(repositoryScopeKey=$repositoryScopeKey, " +
            "projectionScopeKey=$projectionScopeKey)"

    companion object {
        /** Generates a repository scope that the caller must durably bind to exactly one repository. */
        fun generateRepositoryScope(): FhirIdentifierKey =
            FhirIdentifierKey("urn:uuid:${UUID.randomUUID()}", DEFAULT_REPOSITORY_PARTITION)

        fun create(
            repositoryScope: FhirIdentifierKey,
            producerInstance: String,
            configurationFingerprint: String,
            identityKey: GroveHmacIdentityKey,
        ): HealthConnectSynchronizationScope =
            HealthConnectSynchronizationScope(
                repositoryScope,
                producerInstance,
                configurationFingerprint,
                HealthConnectContract.CONVERSION_CONTRACT_VERSION,
                identityKey,
            )

        /** Test-only seam proving an adapter contract upgrade changes the mandatory projection scope. */
        internal fun createForContractVersion(
            repositoryScope: FhirIdentifierKey,
            producerInstance: String,
            configurationFingerprint: String,
            conversionContractVersion: String,
            identityKey: GroveHmacIdentityKey,
        ): HealthConnectSynchronizationScope =
            HealthConnectSynchronizationScope(
                repositoryScope,
                producerInstance,
                configurationFingerprint,
                conversionContractVersion,
                identityKey,
            )
    }
}

private fun String.sized(): String = "${toByteArray(StandardCharsets.UTF_8).size}:$this"

private fun digest(preimage: String): String = "v1:${HealthConnectWireFormat.sha256(preimage)}"

private fun String.isCanonicalProducerUuid(): Boolean = runCatching {
    val uuid = UUID.fromString(this)
    uuid.toString() == this &&
        uuid.version() in MIN_RFC_4122_VERSION..MAX_RFC_4122_VERSION &&
        uuid.variant() == RFC_4122_VARIANT
}.getOrDefault(false)

private const val DEFAULT_REPOSITORY_PARTITION = "default"
private const val MIN_RFC_4122_VERSION = 1
private const val MAX_RFC_4122_VERSION = 5
private const val RFC_4122_VARIANT = 2
