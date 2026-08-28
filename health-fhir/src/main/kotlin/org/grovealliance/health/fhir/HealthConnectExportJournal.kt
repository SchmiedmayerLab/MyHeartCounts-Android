//
// This source file belongs to the My Heart Counts Android project
//
// SPDX-FileCopyrightText: 2026 Stanford University and the project authors (see CONTRIBUTORS.md)
//
// SPDX-License-Identifier: MIT

package org.grovealliance.health.fhir

import org.hl7.fhir.r4.formats.JsonParser
import org.hl7.fhir.r4.model.Bundle
import org.hl7.fhir.r4.model.Identifier
import org.hl7.fhir.r4.model.Observation
import java.time.Instant

/**
 * Durable state required to resolve Health Connect deletions and one-to-many conversions.
 *
 * Health Connect deletion changes contain only the Health Connect id. Implementations persist an
 * active entry only after every upsert has succeeded. An acknowledged deletion becomes a durable
 * tombstone so a replay can be distinguished from a genuinely unknown deletion. Retention and
 * compaction of acknowledged tombstones is an explicit application policy. Event sequences are
 * allocated from one durable, monotonically increasing counter for the entire producer outbox,
 * across all record types and source ids.
 */
interface HealthConnectExportJournal {
    suspend fun entry(
        repositoryScopeKey: ScopeKey,
        recordType: String,
        healthConnectId: String,
    ): HealthConnectExportJournalEntry?

    suspend fun entries(repositoryScopeKey: ScopeKey, recordType: String): List<HealthConnectExportJournalEntry>

    /** Returns the durable outbox event currently blocking this source Record, if one exists. */
    suspend fun pending(
        repositoryScopeKey: ScopeKey,
        recordType: String,
        healthConnectId: String,
    ): HealthConnectPendingExport?

    /** Enumerates every durable outbox event for one repository and record type. */
    suspend fun pendingForType(repositoryScopeKey: ScopeKey, recordType: String): List<HealthConnectPendingExport>

    /**
     * Atomically allocates a monotonically increasing decimal event sequence and stores a draft.
     *
     * The implementation passes the candidate sequence to [buildDraft] before committing either
     * the counter or outbox row. The pure callback uses that sequence in Bundle/Provenance technical
     * identities. If it throws, neither value is committed. If an event for this source Record is
     * already pending, return it unchanged without invoking [buildDraft]. Implementations must
     * serialize concurrent calls so no two committed events receive the same sequence.
     */
    suspend fun stage(
        repositoryScopeKey: ScopeKey,
        recordType: String,
        healthConnectId: String,
        buildDraft: (eventSequence: EventSequence) -> HealthConnectPendingExportDraft,
    ): HealthConnectPendingExport

    /** Atomically stores [entry] and removes the exact [pending] outbox event. */
    suspend fun complete(pending: HealthConnectPendingExport, entry: HealthConnectExportJournalEntry)

    /** Atomically stores a source transition that has neither active outputs nor tombstones. */
    suspend fun storeLocal(entry: HealthConnectExportJournalEntry)

    /**
     * Durably records a deletion that has no export journal row, then allows token progress.
     *
     * Such a deletion can be legitimate when a Record was inserted and deleted before polling or
     * was excluded by collection filters. Retaining it makes possible journal loss visible rather
     * than silently discarding an ambiguous event.
     */
    /**
     * Upserts by `(recordType, healthConnectId)` and preserves the first observation time.
     *
     * Token-store failure can replay the same deletion with a later local clock value; that replay
     * must not create another quarantine row.
     */
    suspend fun recordUnmatchedDeletion(deletion: HealthConnectUnmatchedDeletion)

    /** Durably upserts one rejected source version before the changes token may advance. */
    suspend fun recordRejectedRecord(rejected: HealthConnectRejectedRecord)
}

/**
 * What the journal remembers about one source record after its conversion was acknowledged.
 *
 * The entry is keyed by the Health Connect id and the scope it was exported under, so a later
 * read of the same record can tell an unchanged row from one that has to be re-sent.
 */
data class HealthConnectExportJournalEntry(
    val repositoryScopeKey: ScopeKey,
    val projectionScopeKey: ScopeKey,
    val recordType: String,
    val healthConnectId: String,
    val dataOriginPackage: String,
    val sourceLastModified: Instant,
    val conversionContractVersion: String,
    val sourceRecordIdentifier: Identifier,
    val observations: List<Observation>,
    val bundle: Bundle,
    val destinationReferences: Map<String, String>,
    val lastEventSequence: EventSequence? = null,
    val state: HealthConnectExportState = HealthConnectExportState.ACTIVE,
    val invalidatedAt: Instant? = null,
) {
    init {
        require(recordType.isNotBlank()) { "The source record type must not be blank." }
        require(healthConnectId.isNotBlank()) { "The Health Connect id must not be blank." }
        require(dataOriginPackage.isNotBlank()) { "The data-origin package must not be blank." }
        require(conversionContractVersion.isNotBlank()) { "The conversion-contract version must not be blank." }
        require(sourceRecordIdentifier.hasSystem() && sourceRecordIdentifier.hasValue()) {
            "The source record identifier must contain a system and value."
        }
        require(
            observations.all { observation ->
                observation.identifier.count {
                    it.system == HealthConnectContract.HEALTH_CONNECT_RECORD_IDENTIFIER &&
                        it.hasValue()
                } == 1 &&
                    // At most one: a one-to-one conversion emits no output identifier, so the
                    // record identifier is the Observation's identity.
                    observation.identifier.count {
                        it.system == HealthConnectContract.HEALTH_CONNECT_OUTPUT_IDENTIFIER &&
                            it.hasValue()
                    } <= 1
            },
        ) { "The journal must retain a complete snapshot of every derived Observation." }
        require(bundle.type == Bundle.BundleType.COLLECTION) {
            "The journal must retain the complete collection Bundle for replay and invalidation."
        }
        require(
            destinationReferences.isEmpty() ||
                destinationReferences.keys == observationIdentifiers.map { it.value }.toSet(),
        ) {
            "A completed journal entry must retain one destination reference for every output identifier."
        }
        require(destinationReferences.values.all { it.isNotBlank() }) {
            "Destination references must not be blank."
        }
        require((state == HealthConnectExportState.INVALIDATED) == (invalidatedAt != null)) {
            "Only invalidated journal entries carry an invalidation time."
        }
    }

    val observationIdentifiers: List<Identifier>
        get() = observations.map { observation -> observationIdentity(observation).copy() }
}

/** Whether a journal entry still describes the record, or was superseded by a later export. */
enum class HealthConnectExportState {
    ACTIVE,
    INVALIDATED,
}

/** Whether an export carries a converted record or withdraws one the source has deleted. */
enum class HealthConnectExportOperation(val wireValue: String) {
    UPSERT("upsert"),
    DELETE("delete"),
}

/**
 * A conversion that is ready to be handed to the outbox but has not yet been given its sequence.
 *
 * The draft computes and holds the exact serialized payload, so the checksum the sink acknowledges
 * is the one that was built here rather than a re-serialization that could differ.
 */
data class HealthConnectPendingExportDraft(
    val repositoryScopeKey: ScopeKey,
    val projectionScopeKey: ScopeKey,
    val operation: HealthConnectExportOperation,
    val recordType: String,
    val healthConnectId: String,
    val sourceRecordIdentifier: Identifier,
    val sourceVersion: Instant,
    val bundle: Bundle,
    val invalidatedOutputIdentifiers: Set<String>,
    val nextEntry: HealthConnectExportJournalEntry,
) {
    val bundleJson: String = HealthConnectWireFormat.bundleJson(bundle)
    val payloadSha256: String = HealthConnectWireFormat.sha256(bundleJson)

    init {
        require(recordType.isNotBlank() && healthConnectId.isNotBlank()) {
            "A pending export must identify its source Record."
        }
        require(sourceRecordIdentifier.hasSystem() && sourceRecordIdentifier.hasValue()) {
            "A pending export must carry the exact source identifier."
        }
        require(bundle.type == Bundle.BundleType.COLLECTION && bundle.entry.isNotEmpty()) {
            "A pending export must retain a complete non-empty collection Bundle."
        }
    }
}

/**
 * A sequenced export waiting for its sink acknowledgement.
 *
 * The payload, its checksum, and the Bundle are all retained and cross-checked on construction: a
 * stored row whose JSON no longer matches its Bundle is a corrupted outbox, not a resendable event.
 */
data class HealthConnectPendingExport(
    val eventSequence: EventSequence,
    val repositoryScopeKey: ScopeKey,
    val projectionScopeKey: ScopeKey,
    val operation: HealthConnectExportOperation,
    val recordType: String,
    val healthConnectId: String,
    val sourceRecordIdentifier: Identifier,
    val sourceVersion: Instant,
    val bundle: Bundle,
    val bundleJson: String,
    val payloadSha256: String,
    val invalidatedOutputIdentifiers: Set<String>,
    val nextEntry: HealthConnectExportJournalEntry,
) {
    init {
        require(payloadSha256 == HealthConnectWireFormat.sha256(bundleJson)) {
            "The stored outbox checksum must match its exact UTF-8 Bundle JSON."
        }
        val parsedBundle = runCatching { JsonParser().parse(bundleJson) as? Bundle }.getOrNull()
        require(parsedBundle != null && parsedBundle.equalsDeep(bundle)) {
            "The stored outbox Bundle must exactly match its authoritative JSON payload."
        }
    }

    internal fun batch(): HealthConnectExportBatch = HealthConnectExportBatch(
        eventSequence = eventSequence,
        operation = operation,
        sourceRecordIdentifier = sourceRecordIdentifier.copy(),
        sourceVersion = sourceVersion,
        bundle = bundle.copy(),
        bundleJson = bundleJson,
        payloadSha256 = payloadSha256,
        invalidatedOutputIdentifiers = invalidatedOutputIdentifiers.toSet(),
    )
}

/** A Health Connect deletion for a record this journal never exported, retained for audit. */
data class HealthConnectUnmatchedDeletion(
    val repositoryScopeKey: ScopeKey,
    val projectionScopeKey: ScopeKey,
    val recordType: String,
    val healthConnectId: String,
    val observedAt: Instant,
) {
    init {
        require(recordType.isNotBlank() && healthConnectId.isNotBlank()) {
            "An unmatched deletion must identify its source Record."
        }
    }
}

/** A record the converter refused, kept so a deployment can see what its source data contains. */
data class HealthConnectRejectedRecord(
    val repositoryScopeKey: ScopeKey,
    val projectionScopeKey: ScopeKey,
    val recordType: String,
    val healthConnectId: String,
    val sourceLastModified: Instant,
    val observedAt: Instant,
    val reason: String,
) {
    init {
        require(recordType.isNotBlank() && reason.isNotBlank()) {
            "A rejected Record requires a type and nonempty reason."
        }
    }
}

/**
 * Raised when the journal holds entries written under a different conversion contract version.
 *
 * Re-exporting under a new contract is a deployment decision, so the producer stops rather than
 * silently mixing two contract versions in one destination.
 */
class HealthConnectConversionContractMigrationRequired(recordType: String) :
    IllegalStateException(
        "A conversion-contract migration for $recordType requires an explicit scoped baseline.",
    )
