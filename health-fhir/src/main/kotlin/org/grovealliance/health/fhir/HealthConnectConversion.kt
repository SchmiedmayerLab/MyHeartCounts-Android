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
        require(sourceRecordIdentifierSnapshot.hasSystem() && sourceRecordIdentifierSnapshot.hasValue()) {
            "The source record identifier must contain a system and value."
        }
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
                    it.system == HealthConnectContract.HEALTH_CONNECT_RECORD_IDENTIFIER &&
                        it.hasValue()
                } == 1 &&
                    // At most one: a one-to-one conversion emits none, because the record
                    // identifier already identifies the single Observation it produced.
                    observation.identifier.count {
                        it.system == HealthConnectContract.HEALTH_CONNECT_OUTPUT_IDENTIFIER &&
                            it.hasValue()
                    } <= 1
            },
        ) { "Every converted Observation must contain one exact source identifier and at most one output identifier." }
        require(
            observationSnapshots.all { observation ->
                observation.identifier.single {
                    it.system == HealthConnectContract.HEALTH_CONNECT_RECORD_IDENTIFIER
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
                !it.system.isNullOrEmpty() && EXPORT_EVENT_VALUE.matches(it.value.orEmpty())
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
        require(
            bundleSnapshot.entry.all { entry ->
                val identityExtensions = entry.extension.filter {
                    it.url == GroveExchangeIdentity.ENTRY_IDENTIFIER_EXTENSION
                }
                identityExtensions.size == 1 &&
                    (identityExtensions.single().value as? Identifier)?.let { identifier ->
                        identifier.hasSystem() && identifier.hasValue() &&
                            entry.fullUrl == GroveExchangeIdentity.fullUrl(identifier)
                    } == true
            },
        ) { "Every Bundle entry fullUrl must derive from its one exact business-identity extension." }
        val bundledObservations = bundleSnapshot.entry.mapNotNull { it.resource as? Observation }
        require(
            bundledObservations.size == observationSnapshots.size &&
                bundledObservations.sortedBy(::completeOutputIdentifierKey)
                    .zip(observationSnapshots.sortedBy(::completeOutputIdentifierKey))
                    .all { (bundled, converted) -> bundled.equalsDeep(converted) },
        ) { "The Bundle Observation set must exactly match the converted output set." }
        val bundledProvenances = bundleSnapshot.entry.mapNotNull { it.resource as? Provenance }
        require(
            if (provenanceSnapshot == null) {
                bundledProvenances.isEmpty()
            } else {
                bundledProvenances.size == 1 && bundledProvenances.single().equalsDeep(provenanceSnapshot)
            },
        ) { "The Bundle must contain exactly the conversion result's Provenance, when present." }
        val outputKeys = observationIdentifiers.map { "${it.system}|${it.value}" }.toSet()
        require(
            provenanceSnapshot?.target.orEmpty().map { target ->
                "${target.identifier.system}|${target.identifier.value}"
            }.toSet() == outputKeys,
        ) { "Conversion Provenance must target every exact output identifier." }
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
                    CONVERSION_EVENT_VALUE.matches(entryIdentifier.value.orEmpty()),
            ) { "The Provenance entry must use a deployment-namespaced conversion-provenance identifier." }
        }
    }

    val observationIdentifiers: List<Identifier>
        get() = observationSnapshots.map { observation -> observationIdentity(observation).copy() }
}

/**
 * The identity of one emitted Observation.
 *
 * A Record that yields several Observations gives each an output identifier to tell them apart.
 * A one-to-one conversion emits none, because a second namespace repeating the record identifier
 * would identify nothing new, so the record identifier is the Observation's identity.
 */
internal fun observationIdentity(observation: Observation): Identifier =
    observation.identifier.firstOrNull {
        it.system == HealthConnectContract.HEALTH_CONNECT_OUTPUT_IDENTIFIER
    } ?: observation.identifier.single {
        it.system == HealthConnectContract.HEALTH_CONNECT_RECORD_IDENTIFIER
    }

private val IDENTITY_VALUE = Regex("""v1:[^|]+(\|[^|]+)+""")
private val EXPORT_EVENT_VALUE =
    Regex("""[0-9a-f]{8}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{12}\|[1-9][0-9]*\|exchange-bundle""")
private val CONVERSION_EVENT_VALUE =
    Regex("""[0-9a-f]{8}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{12}\|[1-9][0-9]*\|conversion-provenance""")

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
sealed class HealthConnectRecordRejected(message: String) : IllegalArgumentException(message)

/** A record type outside the published inventory; the producer emits nothing for it. */
class UnsupportedHealthConnectRecord(recordType: String) :
    HealthConnectRecordRejected("Unsupported Health Connect record type: $recordType")

open class InvalidHealthConnectRecord(message: String) : HealthConnectRecordRejected(message)

private fun completeOutputIdentifierKey(observation: Observation): String =
    observationIdentity(observation)
        .let { "${it.system.length}:${it.system}\u0000${it.value.length}:${it.value}" }
