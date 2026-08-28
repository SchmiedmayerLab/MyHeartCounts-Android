//
// This source file belongs to the My Heart Counts Android project
//
// SPDX-FileCopyrightText: 2026 Stanford University and the project authors (see CONTRIBUTORS.md)
//
// SPDX-License-Identifier: MIT

package org.grovealliance.health.fhir

import org.hl7.fhir.r4.formats.IParser
import org.hl7.fhir.r4.formats.JsonParser
import org.hl7.fhir.r4.model.Bundle
import org.hl7.fhir.r4.model.Identifier
import org.hl7.fhir.r4.model.Observation
import org.hl7.fhir.r4.model.Parameters
import org.hl7.fhir.r4.model.Provenance
import java.time.Instant

/** Fenced ownership of one source-record transition. The journal, not the coordinator, issues it. */
data class HealthConnectSourceTransitionLease(
    val repositoryScopeKey: ScopeKey,
    val recordType: String,
    val healthConnectId: String,
    val fence: HealthConnectJournalFence,
    val reconciliationFence: HealthConnectJournalFence? = null,
) {
    init {
        require(recordType.isNotBlank() && healthConnectId.isNotBlank()) {
            "A source-transition lease must identify one source Record."
        }
    }

    override fun toString(): String =
        "HealthConnectSourceTransitionLease(" +
            "repositoryScopeKey=$repositoryScopeKey, recordType=$recordType, " +
            "healthConnectId=<redacted>, fence=$fence, reconciliationFence=$reconciliationFence)"
}

/** Fenced exclusive ownership of one complete-read reconciliation boundary. */
data class HealthConnectReconciliationLease(
    val repositoryScopeKey: ScopeKey,
    val recordType: String,
    val fence: HealthConnectJournalFence,
) {
    init {
        require(recordType.isNotBlank()) { "A reconciliation lease must identify one source Record type." }
    }
}

/**
 * Durable state required to resolve Health Connect deletions and one-to-many conversions.
 *
 * Health Connect deletion changes contain only the Health Connect id. Implementations persist an
 * active entry only after every upsert has succeeded. An acknowledged retraction becomes a durable
 * local invalidation marker so a replay can be distinguished from a genuinely unknown deletion.
 * This marker is journal state, not a copied FHIR clinical resource. Its retention and compaction
 * are explicit application policy. Event sequences are
 * allocated from one durable, monotonically increasing counter for the entire producer outbox,
 * across all record types and source ids.
 */
@Suppress("TooManyFunctions")
interface HealthConnectExportJournal {
    /**
     * Runs one source transition under a renewable, monotonically fenced cross-instance lease.
     *
     * The implementation must exclude another transition for the same source Record across threads,
     * coordinator instances, and processes. It must renew ownership without holding a database
     * transaction across sink I/O. Every mutating method below verifies [HealthConnectSourceTransitionLease.fence]
     * and fails closed after lease loss. A staged exact event remains durable for the next owner.
     * When [reconciliationLease] is supplied, it must be the still-current parent lease for this
     * repository and Record type; this permits a reconciliation owner to take child source leases.
     */
    suspend fun <T> withSourceTransition(
        repositoryScopeKey: ScopeKey,
        recordType: String,
        healthConnectId: String,
        reconciliationLease: HealthConnectReconciliationLease? = null,
        block: suspend (HealthConnectSourceTransitionLease) -> T,
    ): T

    /**
     * Runs a complete-read reconciliation under a renewable, exclusive, monotonically fenced lease.
     * Ordinary source transitions for this repository/type must wait, while child leases carrying
     * this reconciliation fence remain admitted. No database transaction is held across [block].
     */
    suspend fun <T> withReconciliationLease(
        repositoryScopeKey: ScopeKey,
        recordType: String,
        block: suspend (HealthConnectReconciliationLease) -> T,
    ): T

    suspend fun entry(lease: HealthConnectSourceTransitionLease): HealthConnectExportJournalEntry?

    suspend fun entries(lease: HealthConnectReconciliationLease): List<HealthConnectExportJournalEntry>

    /** Returns the durable outbox event currently blocking this source Record, if one exists. */
    suspend fun pending(lease: HealthConnectSourceTransitionLease): HealthConnectPendingExport?

    /** Enumerates every durable outbox event for one repository and record type. */
    suspend fun pendingForType(lease: HealthConnectReconciliationLease): List<HealthConnectPendingExport>

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
        lease: HealthConnectSourceTransitionLease,
        expectedRevision: HealthConnectJournalRevision?,
        buildDraft: (eventSequence: EventSequence) -> HealthConnectPendingExportDraft,
    ): HealthConnectPendingExport

    /**
     * Atomically stores [entry] and removes the exact [pending] outbox event with fence and base-state CAS.
     * Repeating completion for the same event and exact resulting revision is a successful no-op.
     */
    suspend fun complete(
        lease: HealthConnectSourceTransitionLease,
        pending: HealthConnectPendingExport,
        entry: HealthConnectExportJournalEntry,
    )

    /**
     * CAS-stores a source transition that has neither active outputs nor a retraction event.
     * Implementations must reject an [entry] whose retained Bundle contains an output Identifier;
     * local storage cannot be used to bypass durable sink acknowledgement.
     */
    suspend fun storeLocal(
        lease: HealthConnectSourceTransitionLease,
        expectedRevision: HealthConnectJournalRevision?,
        entry: HealthConnectExportJournalEntry,
    )

    /**
     * Durably records a deletion that has no export journal row, then allows token progress.
     *
     * Such a deletion can be legitimate when a Record was inserted and deleted before polling or
     * was excluded by collection filters. The row is upserted by `(recordType, healthConnectId)`
     * and preserves its first observation time. Retaining it makes possible journal loss visible.
     * Token-store failure can replay the same deletion with a later local clock value; that replay
     * must not create another quarantine row.
     */
    suspend fun recordUnmatchedDeletion(
        lease: HealthConnectSourceTransitionLease,
        deletion: HealthConnectUnmatchedDeletion,
    )

    /** Durably upserts one rejected source version before the changes token may advance. */
    suspend fun recordRejectedRecord(
        lease: HealthConnectSourceTransitionLease,
        rejected: HealthConnectRejectedRecord,
    )
}

/**
 * What the journal remembers about one source record after its conversion was acknowledged.
 *
 * The entry is keyed by the Health Connect id and the scope it was exported under, so a later
 * read of the same record can tell an unchanged row from one that has to be re-sent.
 */
@Suppress("LongParameterList")
class HealthConnectExportJournalEntry(
    val repositoryScopeKey: ScopeKey,
    val projectionScopeKey: ScopeKey,
    val recordType: String,
    val healthConnectId: String,
    val dataOriginPackage: String,
    val sourceLastModified: Instant,
    val conversionContractVersion: String,
    sourceRecordIdentifier: Identifier,
    observations: List<Observation>,
    bundle: Bundle,
    destinationReferences: Map<FhirIdentifierKey, String>,
    val lastEventSequence: EventSequence? = null,
    val state: HealthConnectExportState = HealthConnectExportState.ACTIVE,
    val invalidatedAt: Instant? = null,
) {
    private val sourceRecordIdentifierSnapshot = sourceRecordIdentifier.copy()
    private val observationSnapshots = observations.map(Observation::copy)
    private val bundleSnapshot = bundle.copy()
    private val destinationReferenceSnapshot = destinationReferences.toMap()

    init {
        require(recordType.isNotBlank()) { "The source record type must not be blank." }
        require(healthConnectId.isNotBlank()) { "The Health Connect id must not be blank." }
        require(dataOriginPackage.isNotBlank()) { "The data-origin package must not be blank." }
        GroveUnicode.requireScalarText(dataOriginPackage, "Journal data-origin package")
        require(conversionContractVersion.isNotBlank()) { "The conversion-contract version must not be blank." }
        require(
            sourceRecordIdentifierSnapshot.hasSystem() && sourceRecordIdentifierSnapshot.hasValue() &&
                sourceRecordIdentifierSnapshot.hasGroveRole(GroveIdentifierRole.SOURCE_RECORD),
        ) {
            "The source record identifier must contain a complete typed source-record pair."
        }
        sourceRecordIdentifierSnapshot.key()
        require(
            observationSnapshots.all { observation ->
                observation.identifier.count {
                    it.hasGroveRole(GroveIdentifierRole.SOURCE_RECORD) && it.hasSystem() && it.hasValue()
                } == 1 &&
                    observation.identifier.count {
                        it.hasGroveRole(GroveIdentifierRole.SOURCE_OUTPUT) && it.hasSystem() && it.hasValue()
                    } == 1
            },
        ) { "The journal must retain a complete snapshot of every derived Observation." }
        require(
            observationSnapshots.all { observation ->
                observation.identifier.single { it.hasGroveRole(GroveIdentifierRole.SOURCE_RECORD) }
                    .equalsDeep(sourceRecordIdentifierSnapshot)
            },
        ) { "Every journal Observation must identify the entry's exact source Record." }
        require(bundleSnapshot.type == Bundle.BundleType.COLLECTION) {
            "The journal must retain the complete collection Bundle for replay and invalidation."
        }
        bundleSnapshot.requireGroveEntryIdentitySelection()
        bundleSnapshot.requireGroveReferencePolicy()
        val retainedOutputIdentifiers = bundleSnapshot.groveOutputIdentifiers()
        if (state == HealthConnectExportState.ACTIVE && retainedOutputIdentifiers.isNotEmpty()) {
            bundleSnapshot.requireGroveActiveExchangeContract(sourceRecordIdentifierSnapshot)
        }
        val bundledObservations = bundleSnapshot.entry.mapNotNull { it.resource as? Observation }
        require(
            bundledObservations.size == observationSnapshots.size &&
                bundledObservations.sortedBy(::journalObservationKey)
                    .zip(observationSnapshots.sortedBy(::journalObservationKey))
                    .all { (bundled, retained) -> bundled.equalsDeep(retained) },
        ) { "The journal Observation snapshot must exactly match its retained Bundle." }
        val lifecycleSources = bundleSnapshot.entry
            .mapNotNull { it.resource as? Provenance }
            .flatMap(Provenance::getEntity)
            .filter { it.role == Provenance.ProvenanceEntityRole.SOURCE }
            .mapNotNull { entity ->
                entity.what.identifier.takeIf { identifier -> identifier.hasSystem() && identifier.hasValue() }
            }
        val hasExactLifecycleSource = lifecycleSources.size == 1 &&
            lifecycleSources.single().equalsDeep(sourceRecordIdentifierSnapshot)
        require(
            (lifecycleSources.isEmpty() && bundleSnapshot.groveOutputIdentifiers().isEmpty()) ||
                hasExactLifecycleSource,
        ) {
            "A journal lifecycle source Identifier must exactly match its source state; only local zero-output state omits it."
        }
        require(
            destinationReferenceSnapshot.isEmpty() ||
                destinationReferenceSnapshot.keys == retainedOutputIdentifiers.map(Identifier::key).toSet(),
        ) {
            "A completed journal entry must retain one destination reference for every output identifier."
        }
        require(destinationReferenceSnapshot.values.all { it.isNotBlank() }) {
            "Destination references must not be blank."
        }
        require((state == HealthConnectExportState.INVALIDATED) == (invalidatedAt != null)) {
            "Only invalidated journal entries carry an invalidation time."
        }
    }

    val sourceRecordIdentifier: Identifier
        get() = sourceRecordIdentifierSnapshot.copy()

    val observations: List<Observation>
        get() = observationSnapshots.map(Observation::copy)

    val bundle: Bundle
        get() = bundleSnapshot.copy()

    val destinationReferences: Map<FhirIdentifierKey, String>
        get() = destinationReferenceSnapshot.toMap()

    val observationIdentifiers: List<Identifier>
        get() = observationSnapshots.map { observation -> observationIdentity(observation).copy() }

    val outputIdentifiers: List<Identifier>
        get() = bundleSnapshot.groveOutputIdentifiers().map(Identifier::copy)

    val revision: HealthConnectJournalRevision = HealthConnectJournalRevision(
        "v1:${HealthConnectWireFormat.sha256(GroveExchangeProtocol.frameFields(revisionFields()))}",
    )

    @Suppress("LongParameterList")
    fun copy(
        repositoryScopeKey: ScopeKey = this.repositoryScopeKey,
        projectionScopeKey: ScopeKey = this.projectionScopeKey,
        recordType: String = this.recordType,
        healthConnectId: String = this.healthConnectId,
        dataOriginPackage: String = this.dataOriginPackage,
        sourceLastModified: Instant = this.sourceLastModified,
        conversionContractVersion: String = this.conversionContractVersion,
        sourceRecordIdentifier: Identifier = this.sourceRecordIdentifier,
        observations: List<Observation> = this.observations,
        bundle: Bundle = this.bundle,
        destinationReferences: Map<FhirIdentifierKey, String> = this.destinationReferences,
        lastEventSequence: EventSequence? = this.lastEventSequence,
        state: HealthConnectExportState = this.state,
        invalidatedAt: Instant? = this.invalidatedAt,
    ): HealthConnectExportJournalEntry = HealthConnectExportJournalEntry(
        repositoryScopeKey,
        projectionScopeKey,
        recordType,
        healthConnectId,
        dataOriginPackage,
        sourceLastModified,
        conversionContractVersion,
        sourceRecordIdentifier,
        observations,
        bundle,
        destinationReferences,
        lastEventSequence,
        state,
        invalidatedAt,
    )

    private fun revisionFields(): List<String> = buildList {
        add(repositoryScopeKey.value)
        add(projectionScopeKey.value)
        add(recordType)
        add(healthConnectId)
        add(dataOriginPackage)
        add(HealthConnectWireFormat.sourceVersion(sourceLastModified))
        add(conversionContractVersion)
        add(journalIdentifierJson(sourceRecordIdentifierSnapshot))
        add(HealthConnectWireFormat.bundleJson(bundleSnapshot))
        destinationReferenceSnapshot.toSortedMap().forEach { (identifier, reference) ->
            add(identifier.system)
            add(identifier.value)
            add(reference)
        }
        add(lastEventSequence?.value.orEmpty())
        add(state.name)
        add(invalidatedAt?.let(HealthConnectWireFormat::sourceVersion).orEmpty())
    }
}

/** Whether a journal entry still describes the record, or was superseded by a later export. */
enum class HealthConnectExportState {
    ACTIVE,
    INVALIDATED,
}

/** Whether an export carries a converted record or withdraws one the source has deleted. */
enum class HealthConnectExportOperation(val wireValue: String) {
    ACTIVE("active"),
    RETRACTION("retraction"),
}

/**
 * A conversion that is ready to be handed to the outbox but has not yet been given its sequence.
 *
 * The draft computes and holds the exact serialized payload, so the checksum the sink acknowledges
 * is the one that was built here rather than a re-serialization that could differ.
 */
@Suppress("LongParameterList")
class HealthConnectPendingExportDraft(
    val repositoryScopeKey: ScopeKey,
    val projectionScopeKey: ScopeKey,
    val operation: HealthConnectExportOperation,
    val recordType: String,
    val healthConnectId: String,
    sourceRecordIdentifier: Identifier,
    val sourceVersion: Instant,
    bundle: Bundle,
    retractedTargets: Set<HealthConnectRetractionTarget>,
    nextEntry: HealthConnectExportJournalEntry,
) {
    private val sourceRecordIdentifierSnapshot = sourceRecordIdentifier.copy()
    private val bundleSnapshot = bundle.copy()
    private val retractedTargetSnapshot = retractedTargets.toSet()
    private val nextEntrySnapshot = nextEntry.copy()

    val sourceRecordIdentifier: Identifier
        get() = sourceRecordIdentifierSnapshot.copy()

    val bundle: Bundle
        get() = bundleSnapshot.copy()

    val retractedTargets: Set<HealthConnectRetractionTarget>
        get() = retractedTargetSnapshot.toSet()

    val nextEntry: HealthConnectExportJournalEntry
        get() = nextEntrySnapshot.copy()

    val bundleJson: String = HealthConnectWireFormat.bundleJson(bundleSnapshot)
    val payloadSha256: String = HealthConnectWireFormat.sha256(bundleJson)

    init {
        require(recordType.isNotBlank() && healthConnectId.isNotBlank()) {
            "A pending export must identify its source Record."
        }
        require(
            sourceRecordIdentifierSnapshot.hasSystem() && sourceRecordIdentifierSnapshot.hasValue() &&
                sourceRecordIdentifierSnapshot.hasGroveRole(GroveIdentifierRole.SOURCE_RECORD),
        ) {
            "A pending export must carry the exact typed source-record identifier."
        }
        require(bundleSnapshot.type == Bundle.BundleType.COLLECTION && bundleSnapshot.entry.isNotEmpty()) {
            "A pending export must retain a complete non-empty collection Bundle."
        }
        requirePendingTransition(
            repositoryScopeKey,
            projectionScopeKey,
            operation,
            recordType,
            healthConnectId,
            sourceRecordIdentifierSnapshot,
            sourceVersion,
            bundleSnapshot,
            retractedTargetSnapshot,
            nextEntrySnapshot,
        )
    }
}

/**
 * A sequenced export waiting for its sink acknowledgement.
 *
 * The payload, its checksum, and the Bundle are all retained and cross-checked on construction: a
 * stored row whose JSON no longer matches its Bundle is a corrupted outbox, not a resendable event.
 */
@Suppress("LongParameterList")
class HealthConnectPendingExport(
    val eventSequence: EventSequence,
    val baseRevision: HealthConnectJournalRevision?,
    val repositoryScopeKey: ScopeKey,
    val projectionScopeKey: ScopeKey,
    val operation: HealthConnectExportOperation,
    val recordType: String,
    val healthConnectId: String,
    sourceRecordIdentifier: Identifier,
    val sourceVersion: Instant,
    bundle: Bundle,
    val bundleJson: String,
    val payloadSha256: String,
    retractedTargets: Set<HealthConnectRetractionTarget>,
    nextEntry: HealthConnectExportJournalEntry,
) {
    private val sourceRecordIdentifierSnapshot = sourceRecordIdentifier.copy()
    private val bundleSnapshot = bundle.copy()
    private val retractedTargetSnapshot = retractedTargets.toSet()
    private val nextEntrySnapshot = nextEntry.copy()

    val sourceRecordIdentifier: Identifier
        get() = sourceRecordIdentifierSnapshot.copy()

    val bundle: Bundle
        get() = bundleSnapshot.copy()

    val retractedTargets: Set<HealthConnectRetractionTarget>
        get() = retractedTargetSnapshot.toSet()

    val nextEntry: HealthConnectExportJournalEntry
        get() = nextEntrySnapshot.copy()

    init {
        require(payloadSha256 == HealthConnectWireFormat.sha256(bundleJson)) {
            "The stored outbox checksum must match its exact UTF-8 Bundle JSON."
        }
        val parsedBundle = runCatching { JsonParser().parse(bundleJson) as? Bundle }.getOrNull()
        require(parsedBundle != null && parsedBundle.equalsDeep(bundleSnapshot)) {
            "The stored outbox Bundle must exactly match its authoritative JSON payload."
        }
        requirePendingTransition(
            repositoryScopeKey,
            projectionScopeKey,
            operation,
            recordType,
            healthConnectId,
            sourceRecordIdentifierSnapshot,
            sourceVersion,
            bundleSnapshot,
            retractedTargetSnapshot,
            nextEntrySnapshot,
        )
    }

    internal fun batch(): HealthConnectExportBatch = HealthConnectExportBatch(
        eventSequence = eventSequence,
        operation = operation,
        sourceRecordIdentifier = sourceRecordIdentifierSnapshot.copy(),
        sourceVersion = sourceVersion,
        bundle = bundleSnapshot.copy(),
        bundleJson = bundleJson,
        payloadSha256 = payloadSha256,
        retractedTargets = retractedTargetSnapshot.toSet(),
    )

    /** Derives the only journal state that may complete this exact, validated outbox payload. */
    internal fun acknowledgedEntry(
        destinationReferences: Map<FhirIdentifierKey, String>,
    ): HealthConnectExportJournalEntry {
        val next = nextEntrySnapshot
        val acknowledged = when (next.state) {
            HealthConnectExportState.ACTIVE -> {
                val expected = next.outputIdentifiers.map(Identifier::key).toSet()
                require(destinationReferences.keys == expected) {
                    "The sink must acknowledge one destination reference for every active output."
                }
                next.copy(destinationReferences = destinationReferences)
            }
            HealthConnectExportState.INVALIDATED,
            -> {
                require(destinationReferences.isEmpty()) {
                    "A retraction assertion cannot acknowledge active destination references."
                }
                next
            }
        }
        return acknowledged.copy(lastEventSequence = eventSequence)
    }

    @Suppress("LongParameterList")
    fun copy(
        eventSequence: EventSequence = this.eventSequence,
        baseRevision: HealthConnectJournalRevision? = this.baseRevision,
        repositoryScopeKey: ScopeKey = this.repositoryScopeKey,
        projectionScopeKey: ScopeKey = this.projectionScopeKey,
        operation: HealthConnectExportOperation = this.operation,
        recordType: String = this.recordType,
        healthConnectId: String = this.healthConnectId,
        sourceRecordIdentifier: Identifier = this.sourceRecordIdentifier,
        sourceVersion: Instant = this.sourceVersion,
        bundle: Bundle = this.bundle,
        bundleJson: String = this.bundleJson,
        payloadSha256: String = this.payloadSha256,
        retractedTargets: Set<HealthConnectRetractionTarget> = this.retractedTargets,
        nextEntry: HealthConnectExportJournalEntry = this.nextEntry,
    ): HealthConnectPendingExport = HealthConnectPendingExport(
        eventSequence,
        baseRevision,
        repositoryScopeKey,
        projectionScopeKey,
        operation,
        recordType,
        healthConnectId,
        sourceRecordIdentifier,
        sourceVersion,
        bundle,
        bundleJson,
        payloadSha256,
        retractedTargets,
        nextEntry,
    )
}

@Suppress("LongParameterList")
private fun requirePendingTransition(
    repositoryScopeKey: ScopeKey,
    projectionScopeKey: ScopeKey,
    operation: HealthConnectExportOperation,
    recordType: String,
    healthConnectId: String,
    sourceRecordIdentifier: Identifier,
    sourceVersion: Instant,
    bundle: Bundle,
    retractedTargets: Set<HealthConnectRetractionTarget>,
    nextEntry: HealthConnectExportJournalEntry,
) {
    require(
        nextEntry.repositoryScopeKey == repositoryScopeKey &&
            nextEntry.projectionScopeKey == projectionScopeKey &&
            nextEntry.recordType == recordType &&
            nextEntry.healthConnectId == healthConnectId &&
            nextEntry.sourceLastModified == sourceVersion &&
            nextEntry.sourceRecordIdentifier.equalsDeep(sourceRecordIdentifier),
    ) { "The pending transition and its next journal entry must identify the exact same source state." }
    require(nextEntry.bundle.equalsDeep(bundle)) {
        "The pending transition's next journal entry must retain its exact acknowledged Bundle."
    }
    require(nextEntry.destinationReferences.isEmpty()) {
        "A staged next journal entry cannot contain destination references before acknowledgement."
    }
    when (operation) {
        HealthConnectExportOperation.ACTIVE -> require(
            nextEntry.state == HealthConnectExportState.ACTIVE &&
                nextEntry.invalidatedAt == null && retractedTargets.isEmpty(),
        ) { "An active pending transition must produce active state without retraction targets." }
        HealthConnectExportOperation.RETRACTION -> require(
            nextEntry.state == HealthConnectExportState.INVALIDATED &&
                nextEntry.invalidatedAt != null && nextEntry.observations.isEmpty() &&
                retractedTargets.isNotEmpty(),
        ) { "A retraction pending transition must produce invalidated state and exact targets." }
    }
}

private fun journalObservationKey(observation: Observation): FhirIdentifierKey =
    observationIdentity(observation).key()

private fun journalIdentifierJson(identifier: Identifier): String =
    JsonParser().setOutputStyle(IParser.OutputStyle.NORMAL).composeString(
        Parameters().apply {
            addParameter().apply {
                name = "identifier"
                value = identifier.copy()
            }
        },
    )

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

    override fun toString(): String =
        "HealthConnectUnmatchedDeletion(" +
            "repositoryScopeKey=$repositoryScopeKey, projectionScopeKey=$projectionScopeKey, " +
            "recordType=$recordType, healthConnectId=<redacted>, observedAt=$observedAt)"
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
        require(recordType.isNotBlank() && healthConnectId.isNotBlank() && reason.isNotBlank()) {
            "A rejected Record requires a type, source id, and nonempty reason."
        }
    }

    override fun toString(): String =
        "HealthConnectRejectedRecord(" +
            "repositoryScopeKey=$repositoryScopeKey, projectionScopeKey=$projectionScopeKey, " +
            "recordType=$recordType, healthConnectId=<redacted>, " +
            "sourceLastModified=$sourceLastModified, observedAt=$observedAt, reason=$reason)"
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
