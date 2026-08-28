//
// This source file belongs to the My Heart Counts Android project
//
// SPDX-FileCopyrightText: 2026 Stanford University and the project authors (see CONTRIBUTORS.md)
//
// SPDX-License-Identifier: MIT

package org.grovealliance.health.fhir

import androidx.health.connect.client.records.Record
import org.grovealliance.health.RecordType
import org.hl7.fhir.r4.formats.JsonParser
import org.hl7.fhir.r4.model.Bundle
import org.hl7.fhir.r4.model.Observation
import java.time.Instant

/** One sink-acknowledged unit of FHIR changes for a Health Connect source record. */
data class HealthConnectExportBatch(
    val eventSequence: EventSequence,
    val operation: HealthConnectExportOperation,
    val sourceRecordIdentifier: org.hl7.fhir.r4.model.Identifier,
    val sourceVersion: Instant,
    val bundle: Bundle,
    /** Exact compact FHIR JSON persisted before delivery and reused byte-for-byte on retry. */
    val bundleJson: String,
    /** Lowercase SHA-256 of the exact UTF-8 [bundleJson] string. */
    val payloadSha256: String,
    val invalidatedOutputIdentifiers: Set<String> = emptySet(),
) {
    init {
        require(sourceRecordIdentifier.hasSystem() && sourceRecordIdentifier.hasValue()) {
            "An export batch requires the exact source identifier."
        }
        require(bundle.type == Bundle.BundleType.COLLECTION && bundle.entry.isNotEmpty()) {
            "An export batch must contain a non-empty collection Bundle."
        }
        require(payloadSha256 == HealthConnectWireFormat.sha256(bundleJson)) {
            "The export batch payload checksum must match its exact UTF-8 Bundle JSON."
        }
        val parsedBundle = runCatching { JsonParser().parse(bundleJson) as? Bundle }.getOrNull()
        require(parsedBundle != null && parsedBundle.equalsDeep(bundle)) {
            "The export batch Bundle must exactly match its authoritative JSON payload."
        }
    }

    val wireOperation: String
        get() = operation.wireValue

    val wireSourceVersion: String
        get() = HealthConnectWireFormat.sourceVersion(sourceVersion)
}

/** A sink's confirmation that it durably stored the exact serialized event it was given. */
data class HealthConnectExportAcknowledgement(
    val destinationReferences: Map<String, String>,
) {
    init {
        require(destinationReferences.values.all { it.isNotBlank() }) {
            "Acknowledged destination references must not be blank."
        }
    }
}

/**
 * A sink must durably and idempotently apply a complete batch before returning.
 *
 * The sink transmits [HealthConnectExportBatch.wireOperation], the source Identifier's exact
 * `system` and `value`, [HealthConnectExportBatch.wireSourceVersion], `eventSequence`, and the exact
 * `bundleJson`. It must not reserialize [HealthConnectExportBatch.bundle]. Observations are
 * addressed by the exact `(identifier.system, identifier.value)` pair. A thrown exception is a
 * negative acknowledgement: the coordinator does not commit journal state and the Health Connect
 * changes token must not advance.
 */
fun interface HealthConnectExportSink {
    suspend fun apply(batch: HealthConnectExportBatch): HealthConnectExportAcknowledgement
}

/** Coordinates conversion, one-to-many update invalidation, deletion, replay, and full resync. */
@Suppress("TooManyFunctions")
class HealthConnectExportCoordinator(
    private val converter: HealthConnectConverter,
    private val journal: HealthConnectExportJournal,
    private val sink: HealthConnectExportSink,
) {
    private val synchronizationScope = converter.synchronizationScope

    val collectionScopeId: ScopeKey
        get() = synchronizationScope.projectionScopeKey

    val repositoryScopeId: ScopeKey
        get() = synchronizationScope.repositoryScopeKey

    @Suppress("ReturnCount")
    suspend fun upsert(record: Record, convertedAt: Instant) {
        val recordType = sourceRecordType(record)
        val healthConnectId = record.metadata.id
        journal.pending(synchronizationScope.repositoryScopeKey, recordType, healthConnectId)?.let { pending ->
            deliver(pending)
        }

        val preview = try {
            converter.preview(record, convertedAt)
        } catch (error: HealthConnectRecordRejected) {
            reject(record, recordType, convertedAt, error)
            return
        }
        val prior = journal.entry(synchronizationScope.repositoryScopeKey, preview.sourceRecordType, healthConnectId)
        if (prior.requiresExplicitContractMigration(preview, synchronizationScope.projectionScopeKey)) {
            throw HealthConnectConversionContractMigrationRequired(preview.sourceRecordType)
        }
        if (migrateSourceIdentityIfRequired(record, preview, prior, convertedAt)) return
        if (prior?.isUnchangedActiveProjection(preview, synchronizationScope.projectionScopeKey) == true) {
            return
        }

        if (storeZeroOutputLocally(record, preview, prior)) return

        upsertDraft(record, preview, prior)
        val pending = journal.stage(
            synchronizationScope.repositoryScopeKey,
            recordType,
            healthConnectId,
        ) { eventSequence ->
            val conversion = converter.convert(record, convertedAt, eventSequence)
            upsertDraft(record, conversion, prior)
        }
        if (
            pending.operation != HealthConnectExportOperation.UPSERT ||
            pending.sourceVersion != preview.sourceLastModified
        ) {
            deliver(pending)
            upsert(record, convertedAt)
            return
        }
        deliver(pending)
    }

    private suspend fun migrateSourceIdentityIfRequired(
        record: Record,
        conversion: HealthConnectConversion,
        prior: HealthConnectExportJournalEntry?,
        convertedAt: Instant,
    ): Boolean {
        if (!prior.requiresSourceIdentityRetirement(conversion, synchronizationScope.projectionScopeKey)) return false
        retirePriorSourceIdentity(requireNotNull(prior), convertedAt)
        upsert(record, convertedAt)
        return true
    }

    /** Retires an active old source identity before publishing a migration's new exchange identity. */
    private suspend fun retirePriorSourceIdentity(
        prior: HealthConnectExportJournalEntry,
        invalidatedAt: Instant,
    ) {
        if (prior.state != HealthConnectExportState.ACTIVE || prior.observations.isEmpty()) return
        val tombstones = prior.observations.map(::invalidatedCopy)
        val pending = journal.stage(
            synchronizationScope.repositoryScopeKey,
            prior.recordType,
            prior.healthConnectId,
        ) { eventSequence ->
            val deletionBundle = prior.bundle.replacingObservations(tombstones, invalidatedAt).apply {
                identifier = converter.bundleIdentifier(prior.sourceRecordIdentifier, eventSequence)
            }
            HealthConnectPendingExportDraft(
                repositoryScopeKey = synchronizationScope.repositoryScopeKey,
                projectionScopeKey = prior.projectionScopeKey,
                operation = HealthConnectExportOperation.DELETE,
                recordType = prior.recordType,
                healthConnectId = prior.healthConnectId,
                sourceRecordIdentifier = prior.sourceRecordIdentifier.copy(),
                sourceVersion = prior.sourceLastModified,
                bundle = deletionBundle,
                invalidatedOutputIdentifiers = tombstones.map(::outputIdentifierValue).toSet(),
                nextEntry = prior.copy(
                    observations = tombstones.map { it.copy() },
                    bundle = deletionBundle.copy(),
                    destinationReferences = emptyMap(),
                    state = HealthConnectExportState.INVALIDATED,
                    invalidatedAt = invalidatedAt,
                ),
            )
        }
        deliver(pending)
    }

    private suspend fun storeZeroOutputLocally(
        record: Record,
        conversion: HealthConnectConversion,
        prior: HealthConnectExportJournalEntry?,
    ): Boolean {
        val hasPriorActiveOutputs = prior?.state == HealthConnectExportState.ACTIVE && prior.observations.isNotEmpty()
        if (conversion.observations.isNotEmpty() || hasPriorActiveOutputs) return false
        journal.storeLocal(activeEntry(record, conversion, prior))
        return true
    }

    private fun upsertDraft(
        record: Record,
        conversion: HealthConnectConversion,
        prior: HealthConnectExportJournalEntry?,
    ): HealthConnectPendingExportDraft {
        val currentKeys = conversion.observations.map(::identifierKey).toSet()
        val removed = prior
            ?.takeIf { it.state == HealthConnectExportState.ACTIVE }
            ?.observations
            ?.filterNot { identifierKey(it) in currentKeys }
            .orEmpty()
            .map(::invalidatedCopy)
        val nextEntry = activeEntry(record, conversion, prior)
        val draft = HealthConnectPendingExportDraft(
            repositoryScopeKey = synchronizationScope.repositoryScopeKey,
            projectionScopeKey = synchronizationScope.projectionScopeKey,
            operation = HealthConnectExportOperation.UPSERT,
            recordType = conversion.sourceRecordType,
            healthConnectId = record.metadata.id,
            sourceRecordIdentifier = conversion.sourceRecordIdentifier.copy(),
            sourceVersion = conversion.sourceLastModified,
            bundle = conversion.bundle.withAdditionalObservations(
                removed,
                prior?.bundle?.takeIf { removed.isNotEmpty() },
            ),
            invalidatedOutputIdentifiers = removed.map(::outputIdentifierValue).toSet(),
            nextEntry = nextEntry,
        )
        return draft
    }

    private fun activeEntry(
        record: Record,
        conversion: HealthConnectConversion,
        prior: HealthConnectExportJournalEntry?,
    ): HealthConnectExportJournalEntry = HealthConnectExportJournalEntry(
        repositoryScopeKey = synchronizationScope.repositoryScopeKey,
        projectionScopeKey = synchronizationScope.projectionScopeKey,
        recordType = conversion.sourceRecordType,
        healthConnectId = record.metadata.id,
        dataOriginPackage = record.metadata.dataOrigin.packageName,
        sourceLastModified = conversion.sourceLastModified,
        conversionContractVersion = conversion.conversionContractVersion,
        sourceRecordIdentifier = conversion.sourceRecordIdentifier.copy(),
        observations = conversion.observations.map { it.copy() },
        bundle = conversion.bundle.copy(),
        destinationReferences = emptyMap(),
        lastEventSequence = prior?.lastEventSequence,
    )

    @Suppress("ReturnCount")
    suspend fun delete(recordType: String, healthConnectId: String, invalidatedAt: Instant) {
        journal.pending(synchronizationScope.repositoryScopeKey, recordType, healthConnectId)?.let { pending ->
            deliver(pending)
        }

        val prior = journal.entry(synchronizationScope.repositoryScopeKey, recordType, healthConnectId)
        if (prior == null) {
            journal.recordUnmatchedDeletion(
                HealthConnectUnmatchedDeletion(
                    synchronizationScope.repositoryScopeKey,
                    synchronizationScope.projectionScopeKey,
                    recordType,
                    healthConnectId,
                    invalidatedAt,
                ),
            )
            return
        }
        if (prior.state == HealthConnectExportState.INVALIDATED) return

        if (prior.observations.isEmpty()) {
            invalidateLocal(prior, invalidatedAt)
            return
        }

        val invalidated = prior.observations.map(::invalidatedCopy)
        val pending = journal.stage(
            synchronizationScope.repositoryScopeKey,
            recordType,
            healthConnectId,
        ) { eventSequence ->
            val deletionBundle = prior.bundle.replacingObservations(invalidated, invalidatedAt).apply {
                identifier = converter.bundleIdentifier(prior.sourceRecordIdentifier, eventSequence)
            }
            HealthConnectPendingExportDraft(
                repositoryScopeKey = synchronizationScope.repositoryScopeKey,
                projectionScopeKey = synchronizationScope.projectionScopeKey,
                operation = HealthConnectExportOperation.DELETE,
                recordType = recordType,
                healthConnectId = healthConnectId,
                sourceRecordIdentifier = prior.sourceRecordIdentifier.copy(),
                sourceVersion = prior.sourceLastModified,
                bundle = deletionBundle,
                invalidatedOutputIdentifiers = invalidated.map(::outputIdentifierValue).toSet(),
                nextEntry = prior.copy(
                    projectionScopeKey = synchronizationScope.projectionScopeKey,
                    observations = invalidated.map { it.copy() },
                    bundle = deletionBundle.copy(),
                    state = HealthConnectExportState.INVALIDATED,
                    invalidatedAt = invalidatedAt,
                ),
            )
        }
        if (pending.operation != HealthConnectExportOperation.DELETE) {
            deliver(pending)
            delete(recordType, healthConnectId, invalidatedAt)
            return
        }
        deliver(pending)
    }

    private suspend fun invalidateLocal(prior: HealthConnectExportJournalEntry, invalidatedAt: Instant) {
        HealthConnectWireFormat.requireFhirInstant(invalidatedAt, "Local invalidation time")
        journal.storeLocal(
            prior.copy(
                projectionScopeKey = synchronizationScope.projectionScopeKey,
                bundle = prior.bundle.copy().apply {
                    timestampElement = org.hl7.fhir.r4.model.InstantType(invalidatedAt.toString())
                },
                state = HealthConnectExportState.INVALIDATED,
                invalidatedAt = invalidatedAt,
            ),
        )
    }

    /**
     * Reconciles a complete, one-type read against durable state.
     *
     * Each acknowledged record is independently journaled, making a failed reconciliation safe to
     * retry. Records absent from the complete read are invalidated; a later deletion-change replay
     * is recognized by the retained tombstone.
     */
    suspend fun reconcile(recordType: String, records: List<Record>, convertedAt: Instant) {
        journal.pendingForType(synchronizationScope.repositoryScopeKey, recordType)
            .sortedWith(compareBy(HealthConnectPendingExport::eventSequence))
            .forEach { deliver(it) }

        val ordered = records.sortedBy { it.metadata.id }
        require(ordered.all { sourceRecordType(it) == recordType }) {
            "A full reconciliation may contain only $recordType records."
        }
        require(ordered.map { it.metadata.id }.distinct().size == ordered.size) {
            "A full reconciliation cannot contain the same Health Connect id twice."
        }

        ordered.forEach { upsert(it, convertedAt) }
        val presentIds = ordered.map { it.metadata.id }.toSet()
        journal.entries(synchronizationScope.repositoryScopeKey, recordType)
            .filter { it.state != HealthConnectExportState.INVALIDATED && it.healthConnectId !in presentIds }
            .sortedBy { it.healthConnectId }
            .forEach { delete(recordType, it.healthConnectId, convertedAt) }
    }

    private suspend fun reject(
        record: Record,
        recordType: String,
        observedAt: Instant,
        error: HealthConnectRecordRejected,
    ) {
        val healthConnectId = record.metadata.id
        val prior = journal.entry(synchronizationScope.repositoryScopeKey, recordType, healthConnectId)
        if (prior?.state == HealthConnectExportState.ACTIVE) {
            delete(recordType, healthConnectId, observedAt)
        }
        journal.recordRejectedRecord(
            HealthConnectRejectedRecord(
                repositoryScopeKey = synchronizationScope.repositoryScopeKey,
                projectionScopeKey = synchronizationScope.projectionScopeKey,
                recordType = recordType,
                healthConnectId = healthConnectId,
                sourceLastModified = record.metadata.lastModifiedTime,
                observedAt = observedAt,
                reason = error.message ?: "Health Connect Record conversion failed.",
            ),
        )
    }

    private fun sourceRecordType(record: Record): String = RecordType.from(record).identifier

    private fun identifierKey(observation: Observation): String =
        observationIdentity(observation).let { "${it.system}|${it.value}" }

    private fun outputIdentifierValue(observation: Observation): String =
        observationIdentity(observation).value

    private fun outputIdentifier(observation: Observation): org.hl7.fhir.r4.model.Identifier =
        observationIdentity(observation)

    private fun invalidatedCopy(observation: Observation): Observation = observation.copy().apply {
        status = Observation.ObservationStatus.ENTEREDINERROR
    }

    private suspend fun deliver(pending: HealthConnectPendingExport) {
        val acknowledgement = sink.apply(pending.batch())
        val destinationReferences = acknowledgement.destinationReferences.toMap()
        val next = pending.nextEntry
        val completedEntry = when (next.state) {
            HealthConnectExportState.ACTIVE -> {
                val expected = next.observations.map(::outputIdentifierValue).toSet()
                require(destinationReferences.keys == expected) {
                    "The sink must acknowledge one destination reference for every active output."
                }
                next.copy(destinationReferences = destinationReferences)
            }
            HealthConnectExportState.INVALIDATED,
            -> {
                require(destinationReferences.isEmpty()) {
                    "A tombstone-only event cannot acknowledge active destination references."
                }
                next
            }
        }
        journal.complete(pending, completedEntry.copy(lastEventSequence = pending.eventSequence))
    }

    private fun Bundle.withAdditionalObservations(
        observations: List<Observation>,
        priorBundle: Bundle?,
    ): Bundle = copy().apply {
        priorBundle?.entry
            ?.filterNot { it.resource is Observation || it.resource is org.hl7.fhir.r4.model.Provenance }
            ?.forEach { priorEntry ->
                if (entry.none { it.fullUrl == priorEntry.fullUrl }) {
                    addEntry(priorEntry.copy())
                }
            }
        // A superseded output still carries the source-record identity of this conversion, and the
        // adapter contract requires one Provenance to target every output produced from it.
        val provenance = entry
            .mapNotNull { it.resource as? org.hl7.fhir.r4.model.Provenance }
            .singleOrNull()
        observations.sortedBy(::identifierKey).forEach { observation ->
            addGroveEntry(outputIdentifier(observation), observation.copy())
            provenance?.addTarget(
                org.hl7.fhir.r4.model.Reference().apply {
                    reference = GroveExchangeIdentity.fullUrl(outputIdentifier(observation))
                    type = "Observation"
                    identifier = outputIdentifier(observation).copy()
                },
            )
        }
    }

    private fun Bundle.replacingObservations(
        observations: List<Observation>,
        invalidatedAt: Instant,
    ): Bundle = copy().apply {
        HealthConnectWireFormat.requireFhirInstant(invalidatedAt, "Bundle invalidation time")
        timestampElement = org.hl7.fhir.r4.model.InstantType(invalidatedAt.toString())
        // This is a caller-owned tombstone event, not a replay of the earlier conversion.
        // Keeping the prior Provenance would attach its old conversion identity and event time to
        // the new exchange, and minting a new one would claim a conversion that never happened.
        entry.removeAll {
            it.resource is Observation || it.resource is org.hl7.fhir.r4.model.Provenance
        }
        observations.sortedBy(::identifierKey).forEach { observation ->
            addGroveEntry(outputIdentifier(observation), observation.copy())
        }
    }
}

private fun HealthConnectExportJournalEntry.semanticallyEquals(conversion: HealthConnectConversion): Boolean {
    val previousObservations = observations.sortedBy(::outputIdentifierKey)
    val currentObservations = conversion.observations.sortedBy(::outputIdentifierKey)
    if (
        previousObservations.size != currentObservations.size ||
        previousObservations.zip(currentObservations).any { (previous, current) -> !previous.equalsDeep(current) }
    ) {
        return false
    }

    val previousContext = bundle.entry
        .filterNot { it.resource is Observation || it.resource is org.hl7.fhir.r4.model.Provenance }
        .associate { it.fullUrl to it.resource }
    val currentContext = conversion.bundle.entry
        .filterNot { it.resource is Observation || it.resource is org.hl7.fhir.r4.model.Provenance }
        .associate { it.fullUrl to it.resource }
    return previousContext.keys == currentContext.keys && previousContext.all { (fullUrl, resource) ->
        resource.equalsDeep(currentContext.getValue(fullUrl))
    }
}

private fun HealthConnectExportJournalEntry.isUnchangedActiveProjection(
    conversion: HealthConnectConversion,
    currentProjectionScopeKey: ScopeKey,
): Boolean {
    val zeroOutputTimestampMatches = conversion.observations.isNotEmpty() ||
        sourceLastModified == conversion.sourceLastModified
    return state == HealthConnectExportState.ACTIVE &&
        projectionScopeKey == currentProjectionScopeKey &&
        zeroOutputTimestampMatches &&
        semanticallyEquals(conversion)
}

private fun HealthConnectExportJournalEntry?.requiresExplicitContractMigration(
    conversion: HealthConnectConversion,
    currentProjectionScopeKey: ScopeKey,
): Boolean = this != null &&
    projectionScopeKey == currentProjectionScopeKey &&
    (
        conversionContractVersion != conversion.conversionContractVersion ||
            !sourceRecordIdentifier.sameCompleteIdentifier(conversion.sourceRecordIdentifier)
        )

private fun HealthConnectExportJournalEntry?.requiresSourceIdentityRetirement(
    conversion: HealthConnectConversion,
    currentProjectionScopeKey: ScopeKey,
): Boolean = this != null &&
    state == HealthConnectExportState.ACTIVE &&
    observations.isNotEmpty() &&
    projectionScopeKey != currentProjectionScopeKey &&
    !sourceRecordIdentifier.sameCompleteIdentifier(conversion.sourceRecordIdentifier)

private fun org.hl7.fhir.r4.model.Identifier.sameCompleteIdentifier(
    other: org.hl7.fhir.r4.model.Identifier,
): Boolean = system == other.system && value == other.value

private fun outputIdentifierKey(observation: Observation): String =
    observationIdentity(observation).let { "${it.system}|${it.value}" }
