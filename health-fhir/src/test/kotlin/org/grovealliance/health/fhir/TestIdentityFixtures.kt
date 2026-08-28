//
// This source file belongs to the My Heart Counts Android project
//
// SPDX-FileCopyrightText: 2026 Stanford University and the project authors (see CONTRIBUTORS.md)
//
// SPDX-License-Identifier: MIT

package org.grovealliance.health.fhir

internal const val TEST_PRODUCER_INSTANCE = "1f5c58aa-6ec6-4e79-a682-829a9debd3f5"
internal const val TEST_EVENT_SYSTEM = "https://conformance.grovealliance.org/fhir/NamingSystem/grove-event-v2"
internal const val TEST_ENTRY_NODE_SYSTEM = "https://conformance.grovealliance.org/fhir/NamingSystem/grove-entry-node-v2"
internal const val TEST_OPAQUE_IDENTITY_SYSTEM_FAMILY =
    "https://conformance.grovealliance.org/fhir/NamingSystem/grove-opaque-v2"

internal fun testIdentityKey(): GroveHmacIdentityKey = GroveHmacIdentityKey.forConformanceTesting(
    identifierSystemFamily = TEST_OPAQUE_IDENTITY_SYSTEM_FAMILY,
    keyId = "test-key",
    epoch = "1",
    secret = ByteArray(32) { index -> index.toByte() },
)

internal fun testSynchronizationScope(
    repositoryScope: String,
    configurationFingerprint: String,
    conversionContractVersion: String = HealthConnectContract.CONVERSION_CONTRACT_VERSION,
): HealthConnectSynchronizationScope = HealthConnectSynchronizationScope.createForContractVersion(
    repositoryScope = FhirIdentifierKey("urn:uuid:$repositoryScope", "default"),
    producerInstance = TEST_PRODUCER_INSTANCE,
    configurationFingerprint = configurationFingerprint,
    conversionContractVersion = conversionContractVersion,
    identityKey = testIdentityKey(),
)
