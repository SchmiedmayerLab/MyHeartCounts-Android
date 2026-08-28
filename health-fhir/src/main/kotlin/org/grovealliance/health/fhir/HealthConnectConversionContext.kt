//
// This source file belongs to the My Heart Counts Android project
//
// SPDX-FileCopyrightText: 2026 Stanford University and the project authors (see CONTRIBUTORS.md)
//
// SPDX-License-Identifier: MIT

package org.grovealliance.health.fhir

import androidx.health.connect.client.records.metadata.Device
import org.hl7.fhir.r4.model.Identifier
import org.hl7.fhir.r4.model.Observation
import org.hl7.fhir.r4.model.Patient
import org.hl7.fhir.r4.model.Provenance
import org.hl7.fhir.r4.model.Reference
import org.hl7.fhir.r4.model.ResearchStudy
import org.hl7.fhir.r4.model.Resource

/** A resource and the complete business identity used to derive its exchange Bundle fullUrl. */
data class HealthConnectBundleResource<out T : Resource>(
    val entryIdentifier: Identifier,
    val resource: T,
) {
    val fullUrl: String = GroveExchangeIdentity.fullUrl(entryIdentifier)

    init {
        require(entryIdentifier.hasSystem() && entryIdentifier.hasValue()) {
            "A bundle resource must have a complete entry Identifier."
        }
    }

    fun reference(): Reference = Reference(fullUrl)

    internal fun requireStableEntryIdentity() {
        require(fullUrl == GroveExchangeIdentity.fullUrl(entryIdentifier)) {
            "A bundle resource Identifier must not change after its fullUrl is derived."
        }
    }
}

/**
 * Caller attestation for the identity used by a physical recording Device.
 *
 * This admission decision is never serialized into FHIR. A deployment-scoped identity is the
 * exchange default. A broader hardware identity is admitted only when the caller has independently
 * established that disclosing it is authorized.
 */
enum class HealthConnectRecordingDeviceIdentityAdmission {
    DEPLOYMENT_SCOPED,
    CALLER_AUTHORIZED_HARDWARE,
}

/** A physical recording Device plus the explicit admission for every identity it discloses. */
data class HealthConnectRecordingDeviceResource(
    val bundleResource: HealthConnectBundleResource<org.hl7.fhir.r4.model.Device>,
    val identityAdmission: HealthConnectRecordingDeviceIdentityAdmission,
) {
    init {
        requireAdmittedIdentityShape()
    }

    internal fun admittedBundleResource(): HealthConnectBundleResource<org.hl7.fhir.r4.model.Device> {
        requireAdmittedIdentityShape()
        return bundleResource
    }

    private fun requireAdmittedIdentityShape() {
        require(!bundleResource.resource.hasSerialNumber()) {
            "Recording Device.serialNumber is not an exchange field; use a complete Identifier under explicit admission."
        }
    }
}

/**
 * Complete FHIR context supplied around a source-record conversion.
 *
 * The adapter never invents patient, study, application, or physical-device identities. Every
 * returned reference points to a concrete resource that is included in the emitted Bundle. Source
 * enterer and converter remain separate Provenance roles even when the same application fills both.
 */
data class HealthConnectConversionContext(
    val subject: HealthConnectBundleResource<Patient>,
    val assembler: HealthConnectBundleResource<org.hl7.fhir.r4.model.Device>,
    val researchStudies: List<HealthConnectBundleResource<ResearchStudy>> = emptyList(),
    val supportingResources: List<HealthConnectBundleResource<Resource>> = emptyList(),
    /**
     * The namespace this deployment owns for graph nodes the export creates.
     *
     * The conversion Provenance and the exchange Bundle are minted here rather than in a namespace
     * this guide owns, because they record an export event rather than anything read from Health
     * Connect. Two deployments converting the same Records agree on every Record-derived
     * identifier and are expected to differ on these.
     */
    val graphIdentifierSystem: String,
    val sourceApplication: (packageName: String) -> HealthConnectBundleResource<org.hl7.fhir.r4.model.Device>,
    val recordingDevice: (device: Device) -> HealthConnectRecordingDeviceResource,
) {
    init {
        validateStaticContext()
    }

    internal fun resolve(metadata: androidx.health.connect.client.records.metadata.Metadata): ResolvedFhirContext {
        validateStaticContext()
        val source = sourceApplication(metadata.dataOrigin.packageName)
        val admittedRecorder = metadata.device?.let(recordingDevice)
        val recorder = admittedRecorder?.admittedBundleResource()

        // Caller callbacks run arbitrary code and HAPI resources are mutable. Revalidate the
        // static graph after both callbacks have returned so a callback cannot invalidate an
        // already-checked assembler, subject, study, or supporting resource.
        validateStaticContext()
        requireCompleteIdentifiers(source.resource, required = true)
        require(
            source.resource.identifier.count {
                it.system == HealthConnectContract.ANDROID_PACKAGE_IDENTIFIER &&
                    it.value == metadata.dataOrigin.packageName
            } == 1,
        ) { "The source application must carry its exact Android package-name identifier." }
        recorder?.let {
            // Re-run admission after all callbacks in case a later callback mutated the recorder.
            admittedRecorder.admittedBundleResource()
            requireProfile(it.resource, HealthConnectContract.MOBILE_RECORDING_DEVICE_PROFILE)
            requireCompleteIdentifiers(it.resource, required = false)
            require(it.fullUrl != source.fullUrl && it.fullUrl != assembler.fullUrl) {
                "A physical recorder must not reuse a software-application Bundle identity."
            }
        }

        val resources = listOf(subject, assembler) + researchStudies + supportingResources +
            listOfNotNull(source, recorder)
        val snapshots = distinctResources(resources).map(HealthConnectBundleResource<Resource>::snapshot)
        return ResolvedFhirContext(
            subject = subject.reference(),
            assembler = assembler.reference(),
            researchStudies = researchStudies.map(HealthConnectBundleResource<ResearchStudy>::reference),
            sourceApplication = source.reference(),
            recordingDevice = recorder?.reference(),
            resources = snapshots,
        )
    }

    private fun validateStaticContext() {
        require(supportingResources.none {
            it.resource is Observation || it.resource is Provenance
        }) {
            "Supporting resources cannot inject adapter-owned Observations or Provenance."
        }
        distinctResources(
            listOf(subject, assembler) + researchStudies + supportingResources,
        )
        requireProfile(assembler.resource, HealthConnectContract.MOBILE_APPLICATION_DEVICE_PROFILE)
        requireCompleteIdentifiers(assembler.resource, required = true)
        require(
            assembler.resource.identifier.count {
                it.system == HealthConnectContract.ANDROID_PACKAGE_IDENTIFIER && it.hasValue()
            } == 1,
        ) { "The converter application must carry one exact Android package-name identifier." }
        require(
            assembler.resource.deviceName.count {
                it.type == org.hl7.fhir.r4.model.Device.DeviceNameType.USERFRIENDLYNAME &&
                    it.name.isNotBlank()
            } == 1,
        ) { "The converter application must carry one exact user-friendly application name." }
        require(
            assembler.resource.version.count { version ->
                version.value.isNotBlank() && version.type.coding.any {
                    it.system == HealthConnectContract.MDC &&
                        it.code == HealthConnectContract.APPLICATION_SOFTWARE_VERSION
                }
            } == 1,
        ) { "The converter application must carry one exact application software version." }
    }

    private fun distinctResources(
        resources: List<HealthConnectBundleResource<Resource>>,
    ): List<HealthConnectBundleResource<Resource>> {
        resources.forEach(HealthConnectBundleResource<Resource>::requireStableEntryIdentity)
        resources.groupBy { it.fullUrl }.forEach { (fullUrl, matches) ->
            require(matches.drop(1).all { it.resource.equalsDeep(matches.first().resource) }) {
                "Bundle fullUrl $fullUrl identifies conflicting context resources."
            }
        }
        return resources.distinctBy { it.fullUrl }
    }

    private fun requireProfile(resource: Resource, canonical: String) {
        require(resource.meta.profile.any { it.value == canonical }) {
            "${resource.fhirType()} must declare required profile $canonical."
        }
    }

    private fun requireCompleteIdentifiers(resource: org.hl7.fhir.r4.model.Device, required: Boolean) {
        require(!required || resource.identifier.isNotEmpty()) {
            "${resource.fhirType()} must carry a stable business identifier."
        }
        require(resource.identifier.all { it.hasSystem() && it.hasValue() }) {
            "${resource.fhirType()} identifiers must contain both system and value."
        }
        require(resource.identifier.map { it.system to it.value }.distinct().size == resource.identifier.size) {
            "${resource.fhirType()} must not repeat an identifier system and value pair."
        }
    }
}

internal data class ResolvedFhirContext(
    val subject: Reference,
    val assembler: Reference,
    val researchStudies: List<Reference>,
    val sourceApplication: Reference,
    val recordingDevice: Reference?,
    val resources: List<HealthConnectBundleResource<Resource>>,
)

private fun HealthConnectBundleResource<Resource>.snapshot(): HealthConnectBundleResource<Resource> =
    HealthConnectBundleResource(entryIdentifier.copy(), resource.copy())
