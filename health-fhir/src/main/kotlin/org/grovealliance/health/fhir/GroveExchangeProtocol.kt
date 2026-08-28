//
// This source file belongs to the My Heart Counts Android project
//
// SPDX-FileCopyrightText: 2026 Stanford University and the project authors (see CONTRIBUTORS.md)
//
// SPDX-License-Identifier: MIT

package org.grovealliance.health.fhir

import org.hl7.fhir.r4.model.Bundle
import org.hl7.fhir.r4.model.Device
import org.hl7.fhir.r4.model.DocumentReference
import org.hl7.fhir.r4.model.DomainResource
import org.hl7.fhir.r4.model.Identifier
import org.hl7.fhir.r4.model.MedicationAdministration
import org.hl7.fhir.r4.model.MedicationStatement
import org.hl7.fhir.r4.model.Observation
import org.hl7.fhir.r4.model.Provenance
import org.hl7.fhir.r4.model.QuestionnaireResponse
import org.hl7.fhir.r4.model.Resource
import org.hl7.fhir.r4.model.Specimen
import org.hl7.fhir.r4.model.VisionPrescription
import java.io.ByteArrayOutputStream
import java.io.DataOutputStream

/** Shared byte-level primitives from the Grove 0.6.0 exchange protocol. */
internal object GroveExchangeProtocol {
    /** Unsigned 32-bit big-endian UTF-8 byte length followed by the exact bytes, per field. */
    fun frameFields(fields: Iterable<String>): ByteArray = ByteArrayOutputStream().use { bytes ->
        DataOutputStream(bytes).use { output ->
            fields.forEach { field ->
                GroveUnicode.requireScalarText(field, "Exchange-protocol field")
                val encoded = field.toByteArray(Charsets.UTF_8)
                output.writeInt(encoded.size)
                output.write(encoded)
            }
        }
        bytes.toByteArray()
    }
}

/**
 * Enforces the catalog-closed Mobile active graph at the immutable export boundary.
 *
 * Official StructureDefinition validation still runs in CI. These producer checks cover the
 * cross-resource and catalog constraints that a single-resource FHIR validator cannot prove.
 */
internal fun Bundle.requireGroveActiveExchangeContract(sourceRecordIdentifier: Identifier) {
    requireGroveEntryIdentitySelection()
    val outputTypes = HealthConnectContract.activeOutputResourceTypes
    val supportingTypes = HealthConnectContract.activeSupportingResourceTypes
    val admittedTypes = outputTypes + supportingTypes + HealthConnectContract.ACTIVE_LIFECYCLE_RESOURCE_TYPE
    val resourcesByFullUrl = entry.associate { it.fullUrl to it.resource }

    require(resourcesByFullUrl.size == entry.size) { "An active graph requires unique entry fullUrl values." }
    entry.forEachIndexed { index, bundleEntry ->
        val resource = bundleEntry.resource
        require(resource.fhirType() in admittedTypes) {
            "Bundle.entry[$index] ${resource.fhirType()} is not admitted by the closed active resource-type set."
        }
        require((resource as? DomainResource)?.contained.isNullOrEmpty()) {
            "Bundle.entry[$index] contains a Resource; Mobile active graphs prohibit contained resources."
        }
        resource.requireExactActiveProfileClaim(index)
    }

    val outputs = entry.filter { it.resource.fhirType() in outputTypes }
    require(outputs.isNotEmpty()) { "An exported active event must contain at least one source-derived output." }
    outputs.forEachIndexed { index, output ->
        val identifiers = output.resource.typedGroveIdentifiers("Active output[$index]")
        require(
            identifiers.keys.containsAll(
                setOf(GroveIdentifierRole.SOURCE_RECORD, GroveIdentifierRole.SOURCE_OUTPUT),
            ),
        ) { "Every active output requires one typed source-record and source-output Identifier." }
        require(identifiers.getValue(GroveIdentifierRole.SOURCE_RECORD).matchesIdentifierPair(sourceRecordIdentifier)) {
            "Every active output must identify the export batch's exact source record."
        }
    }

    val provenance = entry.map { it.resource }.filterIsInstance<Provenance>().singleOrNull()
        ?: throw IllegalArgumentException("An active event requires exactly one lifecycle Provenance.")
    provenance.requireTransformLifecycle()
    provenance.requireExactSourceEntity(sourceRecordIdentifier)
    provenance.requireTargetsExactly(outputs.map(Bundle.BundleEntryComponent::getFullUrl).toSet())
    if (provenance.meta.profile.single().value == HealthConnectContract.HEALTH_CONNECT_PROVENANCE_PROFILE) {
        provenance.requireHealthConnectDataOriginApplication()
    }

    requireConnectedSupportingResources(resourcesByFullUrl, outputs.map { it.fullUrl }.toSet())
}

private fun Resource.requireExactActiveProfileClaim(index: Int) {
    val profiles = meta.profile.map { it.value }
    require(profiles.none { it.isNullOrBlank() } && profiles.size == profiles.toSet().size) {
        "Bundle.entry[$index] ${fhirType()}.meta.profile must contain distinct nonblank canonicals."
    }
    when (this) {
        is Observation -> requireExactObservationClaim(profiles)
        is DocumentReference -> {
            val requiredRoles = HealthConnectContract.activeDocumentProfileClaims[profiles.toSet()]
            require(requiredRoles != null) {
                "Active DocumentReference must directly claim one exact admitted document profile mode."
            }
            requireExactTypedRoles(requiredRoles, allowWriter = true, label = "Active DocumentReference")
        }
        is Device -> {
            val profile = profiles.singleOrNull()
            val requiredRoles = HealthConnectContract.activeDeviceProfileClaims[profile]
            require(requiredRoles != null) {
                "Active Device must directly claim exactly one admitted Device profile mode."
            }
            requireExactTypedRoles(requiredRoles, allowWriter = false, label = "Active Device")
        }
        is QuestionnaireResponse -> require(
            profiles == listOf(HealthConnectContract.ACTIVE_QUESTIONNAIRE_RESPONSE_PROFILE),
        ) { "Active QuestionnaireResponse must directly claim exactly its Grove profile." }
        is Provenance -> require(
            profiles.size == 1 && profiles.single() in HealthConnectContract.activeConversionProvenanceProfiles,
        ) { "Active Provenance must directly claim one admitted Mobile or Health Connect profile." }
        is Specimen, is VisionPrescription, is MedicationAdministration, is MedicationStatement -> {
            val expected = HealthConnectContract.adapterOnlyOutputProfiles.getValue(fhirType())
            require(profiles == listOf(expected)) {
                "Active ${fhirType()} must directly claim exactly its adapter-only profile $expected."
            }
            requireExactTypedRoles(
                setOf(GroveIdentifierRole.SOURCE_RECORD, GroveIdentifierRole.SOURCE_OUTPUT),
                allowWriter = true,
                label = "Active ${fhirType()}",
            )
        }
    }
}

private fun Observation.requireExactObservationClaim(profiles: List<String>) {
    val semanticProfiles =
        HealthConnectContract.sharedMeasurementProfiles + HealthConnectContract.adapterSpecificObservationProfiles
    val adapterNeutral = profiles.size == 1 && profiles.single() in semanticProfiles
    val healthConnectAdapter = profiles.size == 2 &&
        profiles.count { it in HealthConnectContract.sharedMeasurementProfiles } == 1 &&
        profiles.count { it == HealthConnectContract.HEALTH_CONNECT_OBSERVATION_PROFILE } == 1
    val healthConnectExclusive = profiles.size == 1 &&
        profiles.single() in HealthConnectContract.activeHealthConnectExclusiveObservationProfiles
    require(adapterNeutral || healthConnectAdapter || healthConnectExclusive) {
        "Active Observation must carry one exact admitted semantic/profile-claim mode."
    }
    requireHealthConnectSourceMarkerClaim(profiles)
    profiles.firstNotNullOfOrNull(HealthConnectContract.quantitySemanticsByProfile::get)?.let { expected ->
        if (hasValueQuantity()) {
            require(valueQuantity.system == expected.system && valueQuantity.code == expected.code) {
                "Active Observation Quantity must preserve its catalog-fixed system/code pair."
            }
        }
    }
}

private fun Observation.requireHealthConnectSourceMarkerClaim(profiles: List<String>) {
    val markers = extension.filter {
        it.url == HealthConnectContract.HEALTH_CONNECT_RECORD_TYPE_EXTENSION
    }
    val claimsHealthConnect =
        HealthConnectContract.HEALTH_CONNECT_OBSERVATION_PROFILE in profiles ||
            profiles.any(HealthConnectContract.activeHealthConnectExclusiveObservationProfiles::contains)
    val hasOneCompleteMarker = markers.singleOrNull()?.value?.primitiveValue()?.isNotBlank() == true
    require(
        if (claimsHealthConnect) hasOneCompleteMarker else markers.isEmpty(),
    ) {
        "The Health Connect record-type marker must appear exactly once on a catalog-owned " +
            "Health Connect Observation and never on an adapter-neutral Observation."
    }
}

private fun Resource.requireExactTypedRoles(
    required: Set<GroveIdentifierRole>,
    allowWriter: Boolean,
    label: String,
) {
    val actual = typedGroveIdentifiers(label).keys
    val allowed = required + setOfNotNull(GroveIdentifierRole.WRITER_RECORD.takeIf { allowWriter })
    require(actual.containsAll(required) && actual.all { it in allowed }) {
        "$label has invalid typed Grove Identifier roles."
    }
}

private fun Provenance.requireTransformLifecycle() {
    val iso = activity.coding.filter { it.system == HealthConnectContract.RECORD_LIFECYCLE }
    val grove = activity.coding.filter { it.system == HealthConnectContract.GROVE_LIFECYCLE_EVENT }
    require(iso.size == 1 && iso.single().code == "transform" && grove.isEmpty()) {
        "The sole active Provenance requires exactly one ISO transform coding and no retraction coding."
    }
}

private fun Provenance.requireExactSourceEntity(sourceRecordIdentifier: Identifier) {
    val source = entity.singleOrNull()?.takeIf { it.role == Provenance.ProvenanceEntityRole.SOURCE }?.what
        ?: throw IllegalArgumentException("Active Provenance requires exactly one source entity.")
    require(!source.hasReference() && source.hasIdentifier()) {
        "Active Provenance source must be an identifier-only logical Reference."
    }
    require(
        source.identifier.hasGroveRole(GroveIdentifierRole.SOURCE_RECORD) &&
            source.identifier.matchesIdentifierPair(sourceRecordIdentifier),
    ) { "Active Provenance source must identify the event's exact source record." }
}

private fun Provenance.requireTargetsExactly(outputFullUrls: Set<String>) {
    val targetUrls = target.map { reference ->
        require(reference.hasReference() && !reference.reference.startsWith('#')) {
            "Active Provenance targets must be literal references to Bundle output entries."
        }
        reference.reference
    }
    require(targetUrls.size == targetUrls.toSet().size && targetUrls.toSet() == outputFullUrls) {
        "Active Provenance must target every and only source-derived output exactly once."
    }
}

private fun Provenance.requireHealthConnectDataOriginApplication() {
    val agent = entity.single().agent.singleOrNull()
        ?: throw IllegalArgumentException("Health Connect Provenance requires one DataOrigin enterer agent.")
    val participantCodings = agent.type.coding.filter {
        it.system == HealthConnectContract.PROVENANCE_PARTICIPANT
    }
    require(participantCodings.size == 1 && participantCodings.single().code == "enterer") {
        "Health Connect DataOrigin requires exactly one enterer participation Coding."
    }
    val who = agent.who
    require(
        !who.hasReference() && who.type == "Device" && who.hasIdentifier() &&
            who.identifier.system == HealthConnectContract.ANDROID_PACKAGE_IDENTIFIER &&
            who.identifier.value.isNotBlank(),
    ) {
        "Health Connect DataOrigin must be an identifier-only typed logical Device Reference " +
            "using the Android package NamingSystem."
    }
    who.identifier.key()
    GroveUnicode.requireScalarText(who.identifier.value, "Metadata.dataOrigin.packageName")
}

private fun Bundle.requireConnectedSupportingResources(
    resourcesByFullUrl: Map<String, Resource>,
    outputFullUrls: Set<String>,
) {
    val adjacency = resourcesByFullUrl.keys.associateWith { linkedSetOf<String>() }
    resourcesByFullUrl.forEach { (sourceUrl, resource) ->
        resource.groveReferenceNodes().forEach { reference ->
            val targetUrl = reference.reference.takeIf(resourcesByFullUrl::containsKey) ?: return@forEach
            adjacency.getValue(sourceUrl).add(targetUrl)
            adjacency.getValue(targetUrl).add(sourceUrl)
        }
    }
    val lifecycleUrls = entry.filter {
        it.resource.fhirType() == HealthConnectContract.ACTIVE_LIFECYCLE_RESOURCE_TYPE
    }.mapTo(linkedSetOf(), Bundle.BundleEntryComponent::getFullUrl)
    val reachable = (outputFullUrls + lifecycleUrls).toMutableSet()
    val pending = ArrayDeque(reachable)
    while (pending.isNotEmpty()) {
        adjacency.getValue(pending.removeFirst()).forEach { connected ->
            if (reachable.add(connected)) pending.addLast(connected)
        }
    }
    val disconnected = entry.filter {
        it.resource.fhirType() in HealthConnectContract.activeSupportingResourceTypes &&
            it.fullUrl !in reachable
    }
    require(disconnected.isEmpty()) {
        "Every active supporting resource must connect to an output or lifecycle Provenance."
    }
}
