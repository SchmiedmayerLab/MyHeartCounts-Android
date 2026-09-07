//
// This source file is part of the My Heart Counts Android open-source project
//
// SPDX-FileCopyrightText: 2026 Stanford University and the project authors (see CONTRIBUTORS.md)
//
// SPDX-License-Identifier: MIT

package org.grovealliance.health.fhir

import androidx.health.connect.client.records.metadata.DataOrigin
import androidx.health.connect.client.records.metadata.Metadata
import androidx.health.connect.client.testing.populatedWithTestValues
import org.hl7.fhir.r4.model.CodeableConcept
import org.hl7.fhir.r4.model.Coding
import org.hl7.fhir.r4.model.Identifier
import java.time.Instant
import org.hl7.fhir.r4.model.Device as FhirDevice

internal const val EXAMPLE_REPOSITORY_SCOPE = "1f5c58aa-6ec6-4e79-a682-829a9debd3f5"
internal const val TEST_CONTEXT_IDENTIFIER_SYSTEM = "urn:uuid:8d3fd52b-efda-5f3d-b83d-50f0a70b44aa"
internal const val TEST_SOURCE_PACKAGE = "com.example.source"
internal val TEST_SOURCE_LAST_MODIFIED: Instant = Instant.parse("2026-08-19T17:30:01Z")

internal const val TEST_PRODUCER_INSTANCE = "1f5c58aa-6ec6-4e79-a682-829a9debd3f5"
internal const val TEST_EVENT_SYSTEM = "https://conformance.grovealliance.org/fhir/NamingSystem/grove-event-v0"
internal const val TEST_ENTRY_NODE_SYSTEM = "https://conformance.grovealliance.org/fhir/NamingSystem/grove-entry-node-v0"
internal const val TEST_OPAQUE_IDENTITY_SYSTEM_FAMILY =
    "https://conformance.grovealliance.org/fhir/NamingSystem/grove-opaque-v0"

internal fun testIdentityKey(): GroveHmacIdentityKey = GroveHmacIdentityKey.forConformanceTesting(
    identifierSystemFamily = TEST_OPAQUE_IDENTITY_SYSTEM_FAMILY,
    keyId = "test-key",
    epoch = "1",
    secret = ByteArray(32) { index -> index.toByte() },
)

internal fun testSynchronizationScope(
    repositoryScope: String,
    configurationFingerprint: String,
    conversionContractMarker: String = HealthConnectContract.CONVERSION_CONTRACT_MARKER,
): HealthConnectSynchronizationScope = HealthConnectSynchronizationScope.createForContractMarker(
    repositoryScope = FhirIdentifierKey("urn:uuid:$repositoryScope", "default"),
    producerInstance = TEST_PRODUCER_INSTANCE,
    configurationFingerprint = configurationFingerprint,
    conversionContractMarker = conversionContractMarker,
    identityKey = testIdentityKey(),
)

internal fun testIdentifier(system: String, value: String): Identifier =
    Identifier().setSystem(system).setValue(value)

internal fun testMetadata(
    metadata: Metadata,
    id: String,
    lastModified: Instant = TEST_SOURCE_LAST_MODIFIED,
): Metadata = metadata.populatedWithTestValues(
    id = id,
    dataOrigin = DataOrigin(TEST_SOURCE_PACKAGE),
    lastModifiedTime = lastModified,
)

internal fun testApplication(
    name: String,
    packageName: String,
    version: String? = null,
): HealthConnectBundleResource<FhirDevice> {
    val entryIdentifier = testIdentifier(HealthConnectContract.ANDROID_PACKAGE_IDENTIFIER, packageName)
    return HealthConnectBundleResource(
        entryIdentifier,
        FhirDevice().apply {
            meta.addProfile(HealthConnectContract.MOBILE_APPLICATION_DEVICE_PROFILE)
            addIdentifier(entryIdentifier.copy())
            addDeviceName().setName(name).setType(FhirDevice.DeviceNameType.USERFRIENDLYNAME)
            version?.let {
                addVersion()
                    .setType(
                        CodeableConcept(
                            Coding(
                                HealthConnectContract.MDC,
                                HealthConnectContract.APPLICATION_SOFTWARE_VERSION,
                                "MDC_ID_PROD_SPEC_SW",
                            ),
                        ),
                    )
                    .setValue(it)
            }
        },
    )
}
