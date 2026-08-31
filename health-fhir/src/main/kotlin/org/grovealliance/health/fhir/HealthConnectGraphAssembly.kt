//
// This source file is part of the My Heart Counts Android open-source project
//
// SPDX-FileCopyrightText: 2026 Stanford University and the project authors (see CONTRIBUTORS.md)
//
// SPDX-License-Identifier: MIT

package org.grovealliance.health.fhir

import androidx.health.connect.client.records.metadata.Metadata
import org.hl7.fhir.r4.model.Bundle
import org.hl7.fhir.r4.model.CodeType
import org.hl7.fhir.r4.model.DateTimeType
import org.hl7.fhir.r4.model.Extension
import org.hl7.fhir.r4.model.InstantType
import org.hl7.fhir.r4.model.Observation
import org.hl7.fhir.r4.model.Period
import org.hl7.fhir.r4.model.Provenance
import org.hl7.fhir.r4.model.Reference
import org.hl7.fhir.r4.model.Resource
import org.hl7.fhir.r4.model.Type
import java.time.Instant

@Suppress("LongParameterList")
internal fun HealthConnectConverter.conversion(
    metadata: Metadata,
    recordType: String,
    source: HealthConnectSourceIdentity,
    observations: List<Observation>,
    convertedAt: Instant,
    eventSequence: EventSequence,
    resolvedContext: ResolvedFhirContext,
    conversionResources: List<HealthConnectBundleResource<Resource>> = emptyList(),
): HealthConnectConversion {
    if (convertedAt < metadata.lastModifiedTime) {
        throw InvalidHealthConnectRecord(
            "The conversion event cannot precede the source version's lastModifiedTime.",
        )
    }
    attachRecordTypeLineage(observations, recordType)
    val provenance = observations.takeIf { it.isNotEmpty() }?.let {
        conversionProvenance(
            it,
            conversionResources,
            source,
            sourceActivityTime(it),
            convertedAt,
            resolvedContext,
        )
    }
    val bundle = bundle(
        observations,
        provenance,
        convertedAt,
        eventSequence,
        resolvedContext,
        conversionResources,
    )
    return HealthConnectConversion(
        conversionContractMarker = synchronizationScope.conversionContractMarker,
        sourceRecordIdentifier = source.identifier,
        sourceRecordType = recordType,
        sourceLastModified = metadata.lastModifiedTime,
        bundle = bundle,
    )
}
internal fun HealthConnectConverter.attachRecordTypeLineage(observations: List<Observation>, recordType: String) {
    observations.forEach { observation ->
        check(
            observation.getExtensionsByUrl(
                HealthConnectContract.HEALTH_CONNECT_RECORD_TYPE_EXTENSION,
            ).isEmpty(),
        ) { "Health Connect Record-type lineage must be assigned exactly once." }
        observation.addExtension(
            Extension(
                HealthConnectContract.HEALTH_CONNECT_RECORD_TYPE_EXTENSION,
                CodeType(recordType),
            ),
        )
    }
}

internal fun HealthConnectConverter.conversionProvenance(
    outputs: List<Observation>,
    conversionResources: List<HealthConnectBundleResource<Resource>>,
    source: HealthConnectSourceIdentity,
    occurredAt: Type,
    convertedAt: Instant,
    resolvedContext: ResolvedFhirContext,
): Provenance = Provenance().apply {
    meta.addProfile(HealthConnectContract.HEALTH_CONNECT_PROVENANCE_PROFILE)
    occurred = occurredAt.copy() as Type
    recordedElement = InstantType(convertedAt.toString())
    activity = concept(
        HealthConnectContract.RECORD_LIFECYCLE,
        "transform",
        "Transform/Translate Record Lifecycle Event",
    )
    addAgent().apply {
        type = concept(HealthConnectContract.PROVENANCE_PARTICIPANT, "assembler", "Assembler")
        who = resolvedContext.assembler.copy()
    }
    outputs.forEach { observation ->
        addTarget(
            Reference().apply {
                reference = GroveExchangeIdentity.fullUrl(observationIdentity(observation))
                type = "Observation"
                identifier = observationIdentity(observation).copy()
            },
        )
    }
    conversionResources.forEach { output ->
        addTarget(
            Reference().apply {
                reference = output.fullUrl
                type = output.resource.fhirType()
                identifier = output.entryIdentifier.copy()
            },
        )
    }
    addEntity().apply {
        role = Provenance.ProvenanceEntityRole.SOURCE
        what = Reference().apply { identifier = source.identifier.copy() }
        addAgent().apply {
            type = concept(HealthConnectContract.PROVENANCE_PARTICIPANT, "enterer", "Enterer")
            who = resolvedContext.dataOriginApplication.copy()
        }
    }
}

/**
 * Derives Provenance.occurred[x] from the clinical source activity, never the source row's
 * administrative modification time. A multi-point series uses the exact emitted point span;
 * an interval output preserves its broadest exact effective Period.
 */
internal fun HealthConnectConverter.sourceActivityTime(outputs: List<Observation>): Type {
    val dateTimes = outputs.mapNotNull { it.effective as? DateTimeType }
    val periods = outputs.mapNotNull { it.effective as? Period }
    check(dateTimes.size + periods.size == outputs.size) {
        "Every Health Connect output requires an effective dateTime or Period."
    }
    if (periods.isNotEmpty()) {
        val starts = periods.map { it.startElement } + dateTimes
        val ends = periods.map { it.endElement } + dateTimes
        return Period().apply {
            startElement = starts.minWith(HealthConnectConverter.EFFECTIVE_DATE_TIME_ORDER).copy()
            endElement = ends.maxWith(HealthConnectConverter.EFFECTIVE_DATE_TIME_ORDER).copy()
        }
    }
    check(dateTimes.isNotEmpty()) { "Conversion Provenance requires at least one source activity time." }
    val sorted = dateTimes.sortedWith(HealthConnectConverter.EFFECTIVE_DATE_TIME_ORDER)
    return if (sorted.first().valueAsString == sorted.last().valueAsString) {
        sorted.first().copy()
    } else {
        Period().apply {
            startElement = sorted.first().copy()
            endElement = sorted.last().copy()
        }
    }
}

@Suppress("LongParameterList")
internal fun HealthConnectConverter.bundle(
    observations: List<Observation>,
    provenance: Provenance?,
    convertedAt: Instant,
    eventSequence: EventSequence,
    resolvedContext: ResolvedFhirContext,
    conversionResources: List<HealthConnectBundleResource<Resource>>,
): Bundle = Bundle().apply {
    identifier = bundleIdentifier(eventSequence)
    meta.addProfile(HealthConnectContract.MOBILE_EXCHANGE_BUNDLE_PROFILE)
    type = Bundle.BundleType.COLLECTION
    timestampElement = InstantType(convertedAt.toString())
    resolvedContext.resources.forEach { resolved ->
        addGroveEntry(resolved.entryIdentifier, resolved.resource.copy())
    }
    conversionResources.forEach { resolved ->
        addGroveEntry(resolved.entryIdentifier, resolved.resource.copy())
    }
    observations.sortedBy { observationIdentity(it).value }.forEach { observation ->
        addGroveEntry(observationIdentity(observation), observation.copy())
    }
    provenance?.let {
        addGroveEntry(
            HealthConnectIdentity.conversionNode(
                context.entryNodeIdentifierSystem,
                identifier,
            ),
            it.copy(),
        )
    }
    check(entry.map { it.fullUrl }.distinct().size == entry.size) {
        "A Grove exchange Bundle cannot contain duplicate fullUrl values."
    }
}

internal fun HealthConnectConverter.sourceIdentity(
    metadata: Metadata,
    recordTypeToken: String,
): HealthConnectSourceIdentity {
    val packageName = metadata.dataOrigin.packageName
    val invalidReason = when {
        metadata.id.isBlank() ->
            "Health Connect metadata.id is absent; convert records only after reading them."
        packageName.isBlank() ->
            "Health Connect dataOrigin.packageName is absent."
        !metadata.lastModifiedTime.isAfter(Instant.EPOCH) ->
            "Health Connect lastModifiedTime must be a post-insertion instant after the Unix epoch."
        metadata.lastModifiedTime > HealthConnectWireFormat.MAX_FHIR_INSTANT ->
            "Health Connect lastModifiedTime must have a four-digit FHIR year no later than 9999."
        else -> null
    }
    invalidReason?.let { throw InvalidHealthConnectRecord(it) }
    requireSourceScalarText(metadata.id, "Health Connect metadata.id")
    requireSourceScalarText(packageName, "Health Connect dataOrigin.packageName")
    metadata.validatedClientRecordId()
    return synchronizationScope.sourceRecordIdentifier(recordTypeToken, metadata.id)
}
