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
 * Health Connect's `Metadata.id` is scoped to a repository. The repository scope is a
 * cryptographically random canonical UUIDv4 that the caller must persist for the lifetime of that
 * repository. It must never be cloned or restored into a different repository.
 * [configurationFingerprint] identifies the exact source/filter configuration used by the full
 * reader and must change when that projection changes. The adapter-owned conversion-contract
 * version is included automatically, so an adapter upgrade cannot resume an old cursor without a
 * new baseline. None of these inputs is serialized into FHIR or the exchange Bundle.
 */
class HealthConnectSynchronizationScope private constructor(
    private val repositoryScope: String,
    configurationFingerprint: String,
    internal val conversionContractVersion: String,
) {
    /** Opaque local key shared by every projection over this repository. */
    val repositoryScopeKey: ScopeKey = ScopeKey(
        digest("repository\u0000${repositoryScope.sized()}"),
    )

    /** Opaque local key for token state and one exact full-read/filter configuration. */
    val projectionScopeKey: ScopeKey = ScopeKey(
        digest(
            "projection\u0000${repositoryScope.sized()}\u0000${configurationFingerprint.sized()}" +
                "\u0000${conversionContractVersion.sized()}",
        ),
    )

    init {
        require(repositoryScope.isCanonicalUuidV4()) {
            "Health Connect repository scope must be a canonical lowercase UUIDv4."
        }
        require(configurationFingerprint.isNotBlank()) {
            "An explicit Health Connect filter/configuration fingerprint is required."
        }
        require(conversionContractVersion.isNotBlank()) {
            "The adapter conversion-contract version must not be blank."
        }
    }

    /** Produces the pseudonymous source-record value without exposing either input field. */
    internal fun sourceRecordIdentifierValue(
        recordTypeToken: String,
        healthConnectId: String,
    ): String {
        return HealthConnectIdentity.recordValue(repositoryScope, recordTypeToken, healthConnectId)
    }

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
        fun generateRepositoryScope(): String = UUID.randomUUID().toString()

        fun create(
            repositoryScope: String,
            configurationFingerprint: String,
        ): HealthConnectSynchronizationScope =
            HealthConnectSynchronizationScope(
                repositoryScope,
                configurationFingerprint,
                HealthConnectContract.CONVERSION_CONTRACT_VERSION,
            )

        /** Test-only seam proving an adapter contract upgrade changes the mandatory projection scope. */
        internal fun createForContractVersion(
            repositoryScope: String,
            configurationFingerprint: String,
            conversionContractVersion: String,
        ): HealthConnectSynchronizationScope =
            HealthConnectSynchronizationScope(repositoryScope, configurationFingerprint, conversionContractVersion)
    }
}

private fun String.sized(): String = "${toByteArray(StandardCharsets.UTF_8).size}:$this"

private fun digest(preimage: String): String = "v1:${HealthConnectWireFormat.sha256(preimage)}"

private fun String.isCanonicalUuidV4(): Boolean = runCatching {
    val uuid = UUID.fromString(this)
    uuid.toString() == this && uuid.version() == UUID_VERSION_RANDOM && uuid.variant() == UUID_VARIANT_IETF
}.getOrDefault(false)

private const val UUID_VERSION_RANDOM = 4
private const val UUID_VARIANT_IETF = 2
