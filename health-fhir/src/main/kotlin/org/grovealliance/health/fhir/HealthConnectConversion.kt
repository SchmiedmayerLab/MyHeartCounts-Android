//
// This source file belongs to the My Heart Counts Android project
//
// SPDX-FileCopyrightText: 2026 Stanford University and the project authors (see CONTRIBUTORS.md)
//
// SPDX-License-Identifier: MIT

package org.grovealliance.health.fhir

import org.hl7.fhir.r4.model.Bundle
import org.hl7.fhir.r4.model.Identifier
import org.hl7.fhir.r4.model.Observation
import org.hl7.fhir.r4.model.Provenance
import org.hl7.fhir.r4.model.Specimen
import java.time.Instant

/**
 * The resources and synchronization facts derived from one Health Connect record.
 *
 * HAPI FHIR model objects are mutable. This result owns defensive snapshots and returns fresh
 * copies from its public accessors, so a caller cannot invalidate a graph after its identities,
 * profile claims, and references have been checked.
 */
@Suppress("LongParameterList")
class HealthConnectConversion internal constructor(
    val conversionContractVersion: String,
    sourceRecordIdentifier: Identifier,
    val sourceRecordType: String,
    val sourceLastModified: Instant,
    observations: List<Observation>,
    provenance: Provenance?,
    bundle: Bundle,
) {
    private val sourceRecordIdentifierSnapshot = sourceRecordIdentifier.copy()
    private val observationSnapshots = observations.map(Observation::copy)
    private val provenanceSnapshot = provenance?.copy()
    private val bundleSnapshot = bundle.copy()

    val sourceRecordIdentifier: Identifier
        get() = sourceRecordIdentifierSnapshot.copy()

    val observations: List<Observation>
        get() = observationSnapshots.map(Observation::copy)

    val provenance: Provenance?
        get() = provenanceSnapshot?.copy()

    val bundle: Bundle
        get() = bundleSnapshot.copy()

    init {
        require(conversionContractVersion.isNotBlank()) {
            "The conversion-contract version must not be blank."
        }
        require(
            sourceRecordIdentifierSnapshot.hasSystem() &&
                sourceRecordIdentifierSnapshot.hasValue() &&
                sourceRecordIdentifierSnapshot.hasGroveRole(GroveIdentifierRole.SOURCE_RECORD),
        ) { "The source record identifier must contain a complete typed source-record pair." }
        require(sourceRecordType.isNotBlank()) { "The source record type must not be blank." }
        require(
            observationSnapshots.isNotEmpty() ||
                sourceRecordType in HealthConnectCatalog.zeroOutputRecordTypeIdentifiers,
        ) {
            "Only an admitted zero-output source type may have a successful zero-output conversion."
        }
        require((observationSnapshots.isEmpty()) == (provenanceSnapshot == null)) {
            "Zero-output conversions omit Provenance; conversions with outputs require it."
        }
        require(
            observationSnapshots.all { observation ->
                observation.identifier.count {
                    it.hasGroveRole(GroveIdentifierRole.SOURCE_RECORD) && it.hasSystem() && it.hasValue()
                } == 1 &&
                    observation.identifier.count {
                        it.hasGroveRole(GroveIdentifierRole.SOURCE_OUTPUT) && it.hasSystem() && it.hasValue()
                    } == 1
            },
        ) { "Every converted Observation must contain one exact source and one exact output identifier." }
        require(
            observationSnapshots.all { observation ->
                observation.identifier.single {
                    it.hasGroveRole(GroveIdentifierRole.SOURCE_RECORD)
                }.let {
                    it.system == sourceRecordIdentifierSnapshot.system &&
                        it.value == sourceRecordIdentifierSnapshot.value
                }
            },
        ) { "Every converted Observation must identify its exact source Record." }
        require(
            observationSnapshots.all { observation ->
                !observation.hasIdElement() &&
                    observation.meta.profile.map { it.value }.let { profiles ->
                        val sharedAndAdapter = profiles.size == 2 &&
                            profiles[0] in HealthConnectContract.sharedMeasurementProfiles &&
                            profiles[1] == HealthConnectContract.HEALTH_CONNECT_OBSERVATION_PROFILE
                        val adapterSpecific = profiles.size == 1 &&
                            profiles.single() in HealthConnectContract.adapterSpecificObservationProfiles
                        sharedAndAdapter || adapterSpecific
                    }
            },
        ) {
            "Every output must omit producer-derived Resource.id and use its exact admitted profile-claim mode."
        }
        require(
            observationSnapshots.all { observation ->
                observation.getExtensionsByUrl(
                    HealthConnectContract.HEALTH_CONNECT_RECORD_TYPE_EXTENSION,
                ).singleOrNull()?.value?.primitiveValue() == sourceRecordType
            },
        ) {
            "Every output must preserve its exact AndroidX Health Connect Record class as typed lineage."
        }
        require(
            observationIdentifiers.map { "${it.system}|${it.value}" }.distinct().size ==
                observationSnapshots.size,
        ) {
            "Every converted Observation must have a distinct output identifier."
        }
        require(bundleSnapshot.type == Bundle.BundleType.COLLECTION && bundleSnapshot.entry.isNotEmpty()) {
            "A conversion must contain a non-empty collection Bundle."
        }
        require(!bundleSnapshot.hasIdElement()) { "The producer must not assign a Bundle Resource.id." }
        // The exchange Bundle is created by the export, so it is named in the deployment's own
        // namespace rather than one this guide owns; only its role-suffixed shape is fixed here.
        require(
            bundleSnapshot.identifier.let {
                it.hasSystem() && it.hasValue() && it.hasGroveRole(GroveIdentifierRole.EVENT) &&
                    EVENT_IDENTITY_VALUE.matches(it.value)
            },
        ) { "The Bundle must carry one deployment-namespaced exchange-bundle business identifier." }
        require(
            bundleSnapshot.meta.profile.map { it.value } ==
                listOf(HealthConnectContract.MOBILE_EXCHANGE_BUNDLE_PROFILE),
        ) { "The collection Bundle must claim only the Grove Mobile exchange profile directly." }
        require(bundleSnapshot.entry.all { it.hasFullUrl() && it.hasResource() }) {
            "Every collection Bundle entry must contain a fullUrl and resource."
        }
        require(
            bundleSnapshot.entry.map { it.fullUrl }.distinct().size == bundleSnapshot.entry.size,
        ) {
            "Bundle fullUrl values must be unique."
        }
        bundleSnapshot.requireGroveEntryIdentitySelection()
        bundleSnapshot.requireGroveReferencePolicy()
        require(
            bundleSnapshot.entry.all { entry ->
                val device = entry.resource as? org.hl7.fhir.r4.model.Device ?: return@all true
                val entryIdentifier = (
                    entry.extension.single {
                        it.url == GroveExchangeIdentity.ENTRY_IDENTIFIER_EXTENSION
                    }.value as Identifier
                    )
                when {
                    device.meta.profile.any {
                        it.value == HealthConnectContract.MOBILE_APPLICATION_DEVICE_PROFILE
                    } -> device.identifier.count {
                        it.hasGroveRole(GroveIdentifierRole.DEVICE_SNAPSHOT)
                    } == 1 && device.identifier.single {
                        it.hasGroveRole(GroveIdentifierRole.DEVICE_SNAPSHOT)
                    }.samePair(entryIdentifier)
                    device.meta.profile.any {
                        it.value == HealthConnectContract.MOBILE_HOST_DEVICE_PROFILE
                    } -> device.identifier.size == 1 && device.identifier.singleOrNull {
                        it.hasGroveRole(GroveIdentifierRole.DEVICE_SNAPSHOT)
                    }?.samePair(entryIdentifier) == true
                    device.meta.profile.any {
                        it.value == HealthConnectContract.MOBILE_RECORDING_DEVICE_PROFILE
                    } -> device.identifier.size == 2 &&
                        device.identifier.count {
                            it.hasGroveRole(GroveIdentifierRole.RECORDING_DEVICE)
                        } == 1 && device.identifier.singleOrNull {
                            it.hasGroveRole(GroveIdentifierRole.DEVICE_SNAPSHOT)
                        }?.samePair(entryIdentifier) == true
                    else -> true
                }
            },
        ) {
            "Grove Devices must expose their exact closed typed identities and select the event snapshot as entry key."
        }
        val bundledObservations = bundleSnapshot.entry.mapNotNull { it.resource as? Observation }
        require(
            bundledObservations.size == observationSnapshots.size &&
                bundledObservations.sortedBy(::completeOutputIdentifierKey)
                    .zip(observationSnapshots.sortedBy(::completeOutputIdentifierKey))
                    .all { (bundled, converted) -> bundled.equalsDeep(converted) },
        ) { "The Bundle Observation set must exactly match the converted output set." }
        val specimenEntries = bundleSnapshot.entry.filter { it.resource is Specimen }
        require(
            specimenEntries.all { entry ->
                val specimen = entry.resource as Specimen
                val sourceIdentifier = specimen.identifier.singleOrNull {
                    it.hasGroveRole(GroveIdentifierRole.SOURCE_RECORD)
                }
                val outputIdentifier = specimen.identifier.singleOrNull {
                    it.hasGroveRole(GroveIdentifierRole.SOURCE_OUTPUT)
                }
                val entryIdentifier = (
                    entry.extension.single {
                        it.url == GroveExchangeIdentity.ENTRY_IDENTIFIER_EXTENSION
                    }.value as Identifier
                    )
                specimen.hasIdElement().not() &&
                    specimen.meta.profile.map { it.value } ==
                    listOf(HealthConnectContract.HEALTH_CONNECT_SPECIMEN_PROFILE) &&
                    specimen.identifier.size == 2 &&
                    sourceIdentifier?.samePair(sourceRecordIdentifierSnapshot) == true &&
                    outputIdentifier?.samePair(entryIdentifier) == true
            },
        ) {
            "Every Health Connect Specimen must contain exactly its typed source and specimen-output identities."
        }
        val specimenReferences = bundledObservations
            .filter(Observation::hasSpecimen)
            .map { it.specimen.reference }
        require(
            specimenReferences.size == specimenEntries.size &&
                specimenReferences.toSet() == specimenEntries.map { it.fullUrl }.toSet() &&
                (sourceRecordType == "BloodGlucoseRecord") == (specimenEntries.size == 1),
        ) {
            "A supported BloodGlucoseRecord must have one referenced Specimen and no other conversion may emit one."
        }
        val bundledProvenances = bundleSnapshot.entry.mapNotNull { it.resource as? Provenance }
        require(
            if (provenanceSnapshot == null) {
                bundledProvenances.isEmpty()
            } else {
                bundledProvenances.size == 1 && bundledProvenances.single().equalsDeep(provenanceSnapshot)
            },
        ) { "The Bundle must contain exactly the conversion result's Provenance, when present." }
        val outputEntries = bundleSnapshot.entry.mapNotNull { entry ->
            val identifier = entry.extension.single {
                it.url == GroveExchangeIdentity.ENTRY_IDENTIFIER_EXTENSION
            }.value as Identifier
            identifier.takeIf { it.hasGroveRole(GroveIdentifierRole.SOURCE_OUTPUT) }
                ?.key()
                ?.let { it to entry }
        }.toMap()
        val provenanceTargets = provenanceSnapshot?.target.orEmpty()
        require(
            outputEntries.size == outputIdentifiers.size &&
                provenanceTargets.size == outputEntries.size &&
                provenanceTargets.mapNotNull { target ->
                    target.identifier.takeIf { it.hasSystem() && it.hasValue() }?.key()
                }.distinct().size == provenanceTargets.size &&
                provenanceTargets.all { target ->
                    val key = target.identifier.takeIf { it.hasSystem() && it.hasValue() }?.key()
                    val outputEntry = key?.let(outputEntries::get)
                    outputEntry != null && target.reference == outputEntry.fullUrl &&
                        target.type == outputEntry.resource.fhirType()
                },
        ) {
            "Conversion Provenance must literally target every exact output entry with its typed identifier and resource type."
        }
        provenanceSnapshot?.let { conversionProvenance ->
            require(!conversionProvenance.hasIdElement()) {
                "The producer must not assign a Provenance Resource.id."
            }
            require(
                conversionProvenance.meta.profile.map { it.value } == listOf(
                    HealthConnectContract.HEALTH_CONNECT_PROVENANCE_PROFILE,
                ),
            ) { "Conversion Provenance must directly claim exactly the Health Connect child profile." }
            val provenanceEntry = bundleSnapshot.entry.single { it.resource is Provenance }
            val entryIdentifier = provenanceEntry.extension.single {
                it.url == GroveExchangeIdentity.ENTRY_IDENTIFIER_EXTENSION
            }.value as Identifier
            require(
                !entryIdentifier.system.isNullOrEmpty() &&
                    entryIdentifier.hasGroveRole(GroveIdentifierRole.ENTRY_NODE) &&
                    ENTRY_NODE_IDENTITY_VALUE.matches(entryIdentifier.value.orEmpty()),
            ) { "The Provenance entry must use a deployment-namespaced conversion-provenance identifier." }
        }
    }

    val observationIdentifiers: List<Identifier>
        get() = observationSnapshots.map { observation -> observationIdentity(observation).copy() }

    /** Every addressable active output node, including synthesized Specimens and source artifacts. */
    val outputIdentifiers: List<Identifier>
        get() = bundleSnapshot.groveOutputIdentifiers().map(Identifier::copy)
}

/** Non-throwing public conversion boundary; exceptions remain reserved for producer/configuration bugs. */
sealed interface HealthConnectConversionOutcome {
    data class Converted(val conversion: HealthConnectConversion) : HealthConnectConversionOutcome

    data class Unsupported(val sourceType: String, val reason: String) : HealthConnectConversionOutcome

    data class Rejected(val reason: String) : HealthConnectConversionOutcome
}

/**
 * The identity of one emitted Observation.
 *
 * Every Observation has one source-output identity. This remains true for one-to-one mappings so a
 * later retraction can target the exact output independently of the source-record identity.
 */
internal fun observationIdentity(observation: Observation): Identifier =
    observation.identifier.single { it.hasGroveRole(GroveIdentifierRole.SOURCE_OUTPUT) }

/** Selected entry identities for every active semantic or source-preservation output node. */
internal fun Bundle.groveOutputIdentifiers(): List<Identifier> = entry.mapNotNull { bundleEntry ->
    bundleEntry.resource
        .takeIf { it.fhirType() in HealthConnectContract.activeOutputResourceTypes }
        ?.typedGroveIdentifiers("${bundleEntry.resource.fhirType()} output")
        ?.get(GroveIdentifierRole.SOURCE_OUTPUT)
}

private val EVENT_IDENTITY_VALUE =
    Regex("""e2:[0-9a-f]{8}-[0-9a-f]{4}-[1-5][0-9a-f]{3}-[89ab][0-9a-f]{3}-[0-9a-f]{12}:[1-9][0-9]*""")
private val ENTRY_NODE_IDENTITY_VALUE =
    Regex("""n2:[a-z][a-z0-9-]*:(0|[1-9][0-9]*):[A-Za-z0-9_-]{43}""")

/**
 * Why one Health Connect record cannot be converted.
 *
 * Every rejection is one of these, so the export path catches the base and cannot let a new
 * refusal reach the caller as an unhandled crash.
 *
 * Producer invariants deliberately stay outside this hierarchy and crash the export: the
 * `require` calls in [HealthConnectConversion]'s initializer, the `check` calls guarding graph
 * assembly, and a malformed event sequence all indicate a bug here rather than a bad record. A
 * condition that depends on record data belongs in this hierarchy instead, so it reaches the
 * journal as a rejection.
 */
sealed class HealthConnectRecordRejected(message: String, cause: Throwable? = null) :
    IllegalArgumentException(message, cause)

/** A record type outside the published inventory; the producer emits nothing for it. */
class UnsupportedHealthConnectRecord(val recordType: String) :
    HealthConnectRecordRejected("Unsupported Health Connect record type: $recordType")

open class InvalidHealthConnectRecord(message: String, cause: Throwable? = null) :
    HealthConnectRecordRejected(message, cause)

private fun completeOutputIdentifierKey(observation: Observation): String =
    observationIdentity(observation)
        .let { "${it.system.length}:${it.system}\u0000${it.value.length}:${it.value}" }

private fun Identifier.samePair(other: Identifier): Boolean =
    system == other.system && value == other.value
