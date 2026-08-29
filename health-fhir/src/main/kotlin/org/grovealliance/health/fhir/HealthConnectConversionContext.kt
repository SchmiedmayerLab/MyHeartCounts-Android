//
// This source file belongs to the My Heart Counts Android project
//
// SPDX-FileCopyrightText: 2026 Stanford University and the project authors (see CONTRIBUTORS.md)
//
// SPDX-License-Identifier: MIT

package org.grovealliance.health.fhir

import androidx.health.connect.client.records.metadata.Device
import org.hl7.fhir.r4.model.Base
import org.hl7.fhir.r4.model.Identifier
import org.hl7.fhir.r4.model.Observation
import org.hl7.fhir.r4.model.Patient
import org.hl7.fhir.r4.model.Provenance
import org.hl7.fhir.r4.model.Reference
import org.hl7.fhir.r4.model.ResearchStudy
import org.hl7.fhir.r4.model.Resource
import java.util.Collections
import java.util.IdentityHashMap
import java.util.Locale

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

/** Explicit deployment policy for user-authored Health Connect title and notes fields. */
enum class HealthConnectUserAuthoredTextPolicy {
    /** Preserve nonblank title strings in typed extensions and notes in Observation.note.text. */
    RETAIN,

    /** Deliberately omit the strings as a data-minimization decision. */
    OMIT,
}

/**
 * A physical recording Device plus the governed per-unit token used only as HMAC input.
 *
 * The token is never serialized. Grove mints both the stable typed `recording-device` Identifier
 * and the event-scoped `device-snapshot` Identifier selected as the Bundle entry key. Callers
 * cannot accidentally use a model, manufacturer, or unhashed platform value as identity.
 */
data class HealthConnectRecordingDeviceResource(
    val stablePerUnitToken: String,
    val resource: org.hl7.fhir.r4.model.Device,
) {
    init {
        requireAdmittedIdentityShape()
    }

    internal fun admittedBundleResource(
        identityKey: GroveHmacIdentityKey,
        subjectKey: FhirIdentifierKey,
        eventIdentifier: Identifier,
    ): HealthConnectBundleResource<org.hl7.fhir.r4.model.Device> {
        requireAdmittedIdentityShape()
        val recordingDeviceIdentifier = HealthConnectIdentity.recordingDevice(
            identityKey,
            subjectKey,
            stablePerUnitToken,
        )
        val eventSnapshotIdentifier = HealthConnectIdentity.deviceSnapshot(
            identityKey,
            eventIdentifier,
            RECORDING_DEVICE_ROLE,
            stablePerUnitToken,
        )
        val snapshot = resource.copy().apply {
            addIdentifier(recordingDeviceIdentifier.copy())
            addIdentifier(eventSnapshotIdentifier.copy())
        }
        return HealthConnectBundleResource(eventSnapshotIdentifier, snapshot)
    }

    private fun requireAdmittedIdentityShape() {
        GroveUnicode.requireScalarText(stablePerUnitToken, "Recording Device stable per-unit token")
        require(stablePerUnitToken.isNotBlank()) {
            "A physical recording Device requires a governed stable per-unit token."
        }
        require(!resource.hasSerialNumber()) {
            "Recording Device.serialNumber is not an exchange field."
        }
        require(resource.identifier.isEmpty()) {
            "A recording Device template cannot disclose identifiers; Grove mints its two closed opaque identities."
        }
        require(
            resource.meta.profile.map { it.value } ==
                listOf(HealthConnectContract.MOBILE_RECORDING_DEVICE_PROFILE),
        ) { "A recording Device template must directly claim exactly the Grove Recording Device profile." }
    }
}

/**
 * Immutable host facts for the device on which application snapshots ran.
 *
 * The source token is HMAC input only and is never serialized. The converter creates the one
 * event-scoped `device-snapshot` Identifier allowed by the closed Grove Host Device profile.
 */
data class HealthConnectHostDeviceResource(
    val sourceDeviceToken: String,
    val resource: org.hl7.fhir.r4.model.Device,
) {
    init {
        requireTemplateShape()
    }

    internal fun admittedBundleResource(
        identityKey: GroveHmacIdentityKey,
        eventIdentifier: Identifier,
    ): HealthConnectBundleResource<org.hl7.fhir.r4.model.Device> {
        requireTemplateShape()
        val snapshotIdentifier = HealthConnectIdentity.deviceSnapshot(
            identityKey,
            eventIdentifier,
            HOST_DEVICE_ROLE,
            sourceDeviceToken,
        )
        return HealthConnectBundleResource(
            snapshotIdentifier,
            resource.copy().apply { addIdentifier(snapshotIdentifier.copy()) },
        )
    }

    internal fun requireTemplateShape() {
        GroveUnicode.requireScalarText(sourceDeviceToken, "Host Device source token")
        require(sourceDeviceToken.isNotBlank()) { "A host Device snapshot requires a nonblank source token." }
        require(
            resource.meta.profile.map { it.value } == listOf(HealthConnectContract.MOBILE_HOST_DEVICE_PROFILE),
        ) { "A host Device template must directly claim exactly the Grove Host Device profile." }
        require(resource.identifier.isEmpty()) {
            "A host Device template cannot disclose identifiers; Grove mints its event snapshot identity."
        }
        require(!resource.hasSerialNumber()) {
            "Host Device.serialNumber is not an exchange field."
        }
        require(!resource.hasParent()) { "A Grove host Device cannot itself carry an application host parent." }
        require(
            resource.version.count { version ->
                version.value.isNotBlank() && version.type.coding.any {
                    it.system == HealthConnectContract.GROVE_APPLICATION_VERSION_TYPE &&
                        it.code == OPERATING_SYSTEM_VERSION_CODE
                }
            } == 1,
        ) { "A host Device must carry one exact typed operating-system version." }
    }
}

/** The two admitted Patient subject shapes; a logical pseudonym never fabricates a Bundle node. */
sealed class HealthConnectPatientSubject {
    /** A concrete Patient whose event-scoped snapshot is included and referenced by fullUrl. */
    data class Bundled(val patient: HealthConnectBundleResource<Patient>) : HealthConnectPatientSubject()

    /** An identifier-only logical Patient pseudonym with no literal Reference.reference. */
    data class Logical(val identifier: Identifier) : HealthConnectPatientSubject() {
        init {
            requireValid()
        }
    }

    internal fun requireValid() {
        when (this) {
            is Bundled -> patient.requireStableEntryIdentity()
            is Logical -> {
                identifier.key()
                identifier.requireLogicalPatientPseudonym("Logical Patient subject")
            }
        }
    }

    internal fun identityKey(): FhirIdentifierKey = when (this) {
        is Bundled -> patient.entryIdentifier.key()
        is Logical -> identifier.key()
    }

    internal fun logicalReference(): Reference = when (this) {
        is Bundled -> error("A bundled Patient is referenced through its event-scoped entry.")
        is Logical -> Reference().setType(PATIENT_RESOURCE_TYPE).setIdentifier(identifier.copy())
    }

    internal fun bundledResource(): HealthConnectBundleResource<Patient>? =
        (this as? Bundled)?.patient
}

/**
 * Complete FHIR context supplied around a source-record conversion.
 *
 * The caller supplies one literal-bundled or identifier-only logical Patient subject, study
 * identities, converter application and host facts, and any stable physical-unit token. The
 * adapter derives every protocol-defined opaque Device identity. Every literal reference resolves
 * inside the Bundle. Health Connect DataOrigin is carried separately as an identifier-only logical
 * Device Reference; it is an application product, not an event Device snapshot.
 */
data class HealthConnectConversionContext(
    val subject: HealthConnectPatientSubject,
    val assembler: HealthConnectBundleResource<org.hl7.fhir.r4.model.Device>,
    val assemblerHost: HealthConnectHostDeviceResource? = null,
    val researchStudies: List<HealthConnectBundleResource<ResearchStudy>> = emptyList(),
    val supportingResources: List<HealthConnectBundleResource<Resource>> = emptyList(),
    /** Deployment-owned system for clear `e0:<producer-instance>:<sequence>` event identifiers. */
    val eventIdentifierSystem: String,
    /** Deployment-owned system for deterministic `n0:` event-scoped entry-node identifiers. */
    val entryNodeIdentifierSystem: String,
    val userAuthoredTextPolicy: HealthConnectUserAuthoredTextPolicy,
    /** Optional, explicit wire disclosure of Metadata.id on a one-to-one primary Observation. */
    val nativeIdentifierDisclosure: HealthConnectNativeIdentifierDisclosure? = null,
    val recordingDevice: (device: Device) -> HealthConnectRecordingDeviceResource? = { null },
) {
    init {
        validateStaticContext()
    }

    internal fun resolve(
        metadata: androidx.health.connect.client.records.metadata.Metadata,
        identityKey: GroveHmacIdentityKey,
        eventIdentifier: Identifier,
    ): ResolvedFhirContext {
        validateStaticContext()
        val dataOriginPackageName = metadata.dataOrigin.packageName
        GroveUnicode.requireScalarText(dataOriginPackageName, "Metadata.dataOrigin.packageName")
        require(dataOriginPackageName.isNotBlank()) {
            "Metadata.dataOrigin.packageName must identify a nonblank application product."
        }
        val admittedRecorder = metadata.device?.let(recordingDevice)

        // The recording-device callback runs arbitrary code and HAPI resources are mutable.
        // Revalidate the static graph after it returns so it cannot invalidate an already-checked
        // assembler, subject, study, or supporting resource.
        validateStaticContext()
        val hostSnapshot = assemblerHost?.admittedBundleResource(identityKey, eventIdentifier)
        val assemblerPackage = assembler.resource.identifier.single {
            it.system == HealthConnectContract.ANDROID_PACKAGE_IDENTIFIER
        }.value
        val assemblerSnapshot = applicationSnapshot(
            assembler,
            assemblerPackage,
            identityKey,
            eventIdentifier,
            hostSnapshot?.reference(),
        )
        val recorder = admittedRecorder?.admittedBundleResource(
            identityKey,
            subject.identityKey(),
            eventIdentifier,
        )
        val contextNodes = eventContextNodes(eventIdentifier, assemblerSnapshot)
        recorder?.let {
            requireProfile(it.resource, HealthConnectContract.MOBILE_RECORDING_DEVICE_PROFILE)
            requireCompleteIdentifiers(it.resource, required = false)
            require(it.fullUrl != assemblerSnapshot.fullUrl) {
                "A physical recorder must not reuse a software-application Bundle identity."
            }
        }

        val resources = listOfNotNull(contextNodes.subject, hostSnapshot) + listOf(assemblerSnapshot) +
            contextNodes.studies + contextNodes.supporting +
            listOfNotNull(recorder)
        val snapshots = distinctResources(resources).map(HealthConnectBundleResource<Resource>::snapshot)
        return ResolvedFhirContext(
            subject = contextNodes.subject?.reference() ?: subject.logicalReference(),
            assembler = assemblerSnapshot.reference(),
            researchStudies = contextNodes.studies.map(HealthConnectBundleResource<Resource>::reference),
            dataOriginApplication = dataOriginApplicationReference(dataOriginPackageName),
            recordingDevice = recorder?.reference(),
            resources = snapshots,
        )
    }

    private fun eventContextNodes(
        eventIdentifier: Identifier,
        assemblerSnapshot: HealthConnectBundleResource<org.hl7.fhir.r4.model.Device>,
    ): EventContextNodes {
        val subjectTemplate = subject.bundledResource()
        val subjectSnapshot = subjectTemplate?.let {
            eventScopedContextResource(it, eventIdentifier, PATIENT_NODE_ROLE, 0)
        }
        val studyNodes = researchStudies
            .sortedBy { it.fullUrl }
            .mapIndexed { ordinal, study ->
                study to eventScopedContextResource(study, eventIdentifier, RESEARCH_STUDY_NODE_ROLE, ordinal)
            }
        val supportingNodes = supportingResources
            .groupBy { it.resource.fhirType().contextNodeRole() }
            .toSortedMap()
            .flatMap { (nodeRole, resources) ->
                resources.sortedBy { it.fullUrl }.mapIndexed { ordinal, resource ->
                    resource to eventScopedContextResource(resource, eventIdentifier, nodeRole, ordinal)
                }
            }
        val studySnapshots = studyNodes.map { it.second }
        val supportingSnapshots = supportingNodes.map { it.second }
        val referenceReplacements = buildMap {
            if (subjectTemplate != null && subjectSnapshot != null) {
                put(subjectTemplate.fullUrl, subjectSnapshot.fullUrl)
            }
            put(assembler.fullUrl, assemblerSnapshot.fullUrl)
            studyNodes.forEach { (original, snapshot) ->
                put(original.fullUrl, snapshot.fullUrl)
            }
            supportingNodes.forEach { (original, snapshot) ->
                put(original.fullUrl, snapshot.fullUrl)
            }
        }
        val remappedSubject = subjectSnapshot?.remapLiteralReferences(referenceReplacements)
        val remappedStudies = studySnapshots.map { it.remapLiteralReferences(referenceReplacements) }
        val remappedSupporting = supportingSnapshots.map { it.remapLiteralReferences(referenceReplacements) }
        return EventContextNodes(
            subject = remappedSubject,
            studies = remappedStudies,
            supporting = remappedSupporting,
        )
    }

    /** Current event-time assembler identity for an identifier-only lifecycle assertion. */
    internal fun assemblerSnapshotIdentifier(
        identityKey: GroveHmacIdentityKey,
        eventIdentifier: Identifier,
    ): Identifier {
        validateStaticContext()
        val packageName = assembler.resource.identifier.single {
            it.system == HealthConnectContract.ANDROID_PACKAGE_IDENTIFIER
        }.value
        return HealthConnectIdentity.deviceSnapshot(
            identityKey,
            eventIdentifier,
            APPLICATION_DEVICE_ROLE,
            packageName,
        )
    }

    private fun validateStaticContext() {
        subject.requireValid()
        require(
            listOf(eventIdentifierSystem, entryNodeIdentifierSystem).all {
                it.isAbsoluteAsciiUri()
            } && eventIdentifierSystem != entryNodeIdentifierSystem,
        ) {
            "Event and entry-node systems must be distinct deployment-owned absolute ASCII RFC 3986 URIs."
        }
        require(supportingResources.none {
            it.resource is Observation || it.resource is Provenance
        }) {
            "Supporting resources cannot inject adapter-owned Observations or Provenance."
        }
        require(supportingResources.all {
            it.resource.fhirType() in HealthConnectContract.activeSupportingResourceTypes
        }) {
            "Supporting resources must belong to the protocol's closed active supporting-resource type set."
        }
        require(supportingResources.none { supporting ->
            val profiles = supporting.resource.meta.profile.map { it.value }.toSet()
            profiles.any {
                it == HealthConnectContract.MOBILE_APPLICATION_DEVICE_PROFILE ||
                    it == HealthConnectContract.MOBILE_HOST_DEVICE_PROFILE ||
                    it == HealthConnectContract.MOBILE_RECORDING_DEVICE_PROFILE
            }
        }) {
            "Supporting resources cannot bypass the governed Grove Device snapshot builders."
        }
        assemblerHost?.requireTemplateShape()
        distinctResources(
            listOfNotNull(subject.bundledResource(), assembler) + researchStudies + supportingResources,
        )
        requireProfile(assembler.resource, HealthConnectContract.MOBILE_APPLICATION_DEVICE_PROFILE)
        val assemblerPackage = assembler.resource.identifier.singleOrNull {
            it.system == HealthConnectContract.ANDROID_PACKAGE_IDENTIFIER && it.hasValue()
        }?.value ?: throw IllegalArgumentException(
            "The converter application must carry one exact Android package-name identifier.",
        )
        validateApplicationTemplate(assembler.resource, assemblerPackage, requireVersion = true)
        require(
            assembler.resource.version.count { version ->
                version.value.isNotBlank() && version.type.coding.any {
                    it.system == HealthConnectContract.MDC &&
                        it.code == HealthConnectContract.APPLICATION_SOFTWARE_VERSION
                }
            } == 1,
        ) { "The converter application must carry one exact application software version." }
    }

    private fun applicationSnapshot(
        template: HealthConnectBundleResource<org.hl7.fhir.r4.model.Device>,
        packageName: String,
        identityKey: GroveHmacIdentityKey,
        eventIdentifier: Identifier,
        hostReference: Reference?,
    ): HealthConnectBundleResource<org.hl7.fhir.r4.model.Device> {
        val snapshotIdentifier = HealthConnectIdentity.deviceSnapshot(
            identityKey,
            eventIdentifier,
            APPLICATION_DEVICE_ROLE,
            packageName,
        )
        val snapshot = template.resource.copy().apply {
            addIdentifier(snapshotIdentifier.copy())
            hostReference?.let { parent = it.copy() }
        }
        return HealthConnectBundleResource(snapshotIdentifier, snapshot)
    }

    private fun dataOriginApplicationReference(packageName: String): Reference = Reference().apply {
        type = "Device"
        identifier = Identifier()
            .setSystem(HealthConnectContract.ANDROID_PACKAGE_IDENTIFIER)
            .setValue(packageName)
    }

    private fun eventScopedContextResource(
        template: HealthConnectBundleResource<Resource>,
        eventIdentifier: Identifier,
        nodeRole: String,
        ordinal: Int,
    ): HealthConnectBundleResource<Resource> = HealthConnectBundleResource(
        HealthConnectIdentity.contextNode(
            entryNodeIdentifierSystem,
            eventIdentifier,
            nodeRole,
            ordinal,
        ),
        template.resource.copy(),
    )

    private fun validateApplicationTemplate(
        resource: org.hl7.fhir.r4.model.Device,
        expectedPackageName: String,
        requireVersion: Boolean,
    ) {
        requireProfile(resource, HealthConnectContract.MOBILE_APPLICATION_DEVICE_PROFILE)
        requireCompleteIdentifiers(resource, required = true)
        require(
            resource.identifier.filter { it.system == HealthConnectContract.ANDROID_PACKAGE_IDENTIFIER }
                .singleOrNull()?.value == expectedPackageName,
        ) {
            "An application Device must carry its exact Android package-name identifier."
        }
        require(resource.identifier.none { it.hasGroveRole(GroveIdentifierRole.DEVICE_SNAPSHOT) }) {
            "The converter, not the caller, mints each event-bound application Device snapshot identity."
        }
        require(!resource.hasParent()) {
            "The converter owns event-bound application-to-host linkage; application templates cannot set parent."
        }
        require(resource.deviceName.count {
            it.type == org.hl7.fhir.r4.model.Device.DeviceNameType.USERFRIENDLYNAME && it.name.isNotBlank()
        } == 1) {
            "An application Device must carry one exact user-friendly application name."
        }
        require(!requireVersion || resource.version.isNotEmpty()) {
            "The converter application template must state its software version."
        }
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
        require(resource.meta.profile.map { it.value } == listOf(canonical)) {
            "${resource.fhirType()} must directly declare only required profile $canonical."
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
    val dataOriginApplication: Reference,
    val recordingDevice: Reference?,
    val resources: List<HealthConnectBundleResource<Resource>>,
)

private data class EventContextNodes(
    val subject: HealthConnectBundleResource<Resource>?,
    val studies: List<HealthConnectBundleResource<Resource>>,
    val supporting: List<HealthConnectBundleResource<Resource>>,
)

private fun HealthConnectBundleResource<Resource>.snapshot(): HealthConnectBundleResource<Resource> =
    HealthConnectBundleResource(entryIdentifier.copy(), resource.copy())

private fun HealthConnectBundleResource<Resource>.remapLiteralReferences(
    replacements: Map<String, String>,
): HealthConnectBundleResource<Resource> {
    val remapped = resource.copy()
    val visited = Collections.newSetFromMap(IdentityHashMap<Base, Boolean>())
    fun visit(element: Base) {
        if (!visited.add(element)) return
        if (element is Reference && element.hasReference()) {
            replacements[element.reference]?.let { element.reference = it }
        }
        element.children().flatMap { it.values }.forEach(::visit)
    }
    visit(remapped)
    return HealthConnectBundleResource(entryIdentifier.copy(), remapped)
}

private fun String.contextNodeRole(): String =
    "context-" + replace(Regex("([a-z0-9])([A-Z])"), "$1-$2").lowercase(Locale.ROOT)

private const val APPLICATION_DEVICE_ROLE = "application"
private const val HOST_DEVICE_ROLE = "host"
private const val RECORDING_DEVICE_ROLE = "recording-device"
private const val OPERATING_SYSTEM_VERSION_CODE = "os-version"
private const val PATIENT_NODE_ROLE = "patient"
private const val RESEARCH_STUDY_NODE_ROLE = "research-study"
private const val PATIENT_RESOURCE_TYPE = "Patient"
