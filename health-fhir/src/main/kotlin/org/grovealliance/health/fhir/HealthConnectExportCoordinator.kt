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
import org.hl7.fhir.r4.model.CodeType
import org.hl7.fhir.r4.model.Identifier
import org.hl7.fhir.r4.model.Observation
import org.hl7.fhir.r4.model.Provenance
import java.time.Instant

/** One prior logical output named by a retraction assertion. */
data class HealthConnectRetractionTarget(
    val identifier: FhirIdentifierKey,
    val identifierRole: GroveIdentifierRole,
    val resourceType: String,
    val role: HealthConnectRetractionTargetRole,
) {
    init {
        require(resourceType.matches(Regex("[A-Z][A-Za-z0-9]+"))) {
            "A retraction target requires its exact FHIR resource type."
        }
        val claim = HealthConnectContract.retractionTargetClaims.getValue(role)
        require(identifierRole == claim.identifierRole && resourceType in claim.resourceTypes) {
            "${role.code} requires Identifier role ${claim.identifierRole.code} and one of " +
                "${claim.resourceTypes.sorted().joinToString()}; received ${identifierRole.code}/$resourceType."
        }
    }
}

/** One closed target-role row projected from the normative exchange-protocol catalog. */
internal data class GroveRetractionTargetClaim(
    val identifierRole: GroveIdentifierRole,
    val resourceTypes: Set<String>,
)

enum class HealthConnectRetractionTargetRole(val code: String) {
    PRIMARY_OUTPUT("primary-output"),
    SOURCE_ARTIFACT("source-artifact"),
    CHILD_OUTPUT("child-output"),
    SPECIMEN("specimen"),
    DEVICE_SNAPSHOT("device-snapshot"),
}

/** One sink-acknowledged unit of FHIR changes for a Health Connect source record. */
@Suppress("LongParameterList")
class HealthConnectExportBatch(
    val eventSequence: EventSequence,
    val operation: HealthConnectExportOperation,
    sourceRecordIdentifier: org.hl7.fhir.r4.model.Identifier,
    val sourceVersion: Instant,
    bundle: Bundle,
    /** Exact compact FHIR JSON persisted before delivery and reused byte-for-byte on retry. */
    val bundleJson: String,
    /** Lowercase SHA-256 of the exact UTF-8 [bundleJson] string. */
    val payloadSha256: String,
    retractedTargets: Set<HealthConnectRetractionTarget> = emptySet(),
) {
    private val sourceRecordIdentifierSnapshot = sourceRecordIdentifier.copy()
    private val bundleSnapshot = bundle.copy()
    private val retractedTargetSnapshot = retractedTargets.toSet()

    /** Defensive copy; mutating it cannot change this batch or a later retry. */
    val sourceRecordIdentifier: Identifier
        get() = sourceRecordIdentifierSnapshot.copy()

    /** Diagnostic object view only; [bundleJson] remains the authoritative wire payload. */
    val bundle: Bundle
        get() = bundleSnapshot.copy()

    val retractedTargets: Set<HealthConnectRetractionTarget>
        get() = retractedTargetSnapshot.toSet()

    init {
        require(
            sourceRecordIdentifierSnapshot.hasSystem() && sourceRecordIdentifierSnapshot.hasValue() &&
                sourceRecordIdentifierSnapshot.hasGroveRole(GroveIdentifierRole.SOURCE_RECORD),
        ) {
            "An export batch requires the exact typed source-record identifier."
        }
        require(bundleSnapshot.type == Bundle.BundleType.COLLECTION && bundleSnapshot.entry.isNotEmpty()) {
            "An export batch must contain a non-empty collection Bundle."
        }
        require(
            bundleSnapshot.identifier.hasSystem() && bundleSnapshot.identifier.hasValue() &&
                bundleSnapshot.identifier.hasGroveRole(GroveIdentifierRole.EVENT) &&
                EVENT_IDENTIFIER_VALUE.matches(bundleSnapshot.identifier.value) &&
                bundleSnapshot.identifier.value.substringAfterLast(':') == eventSequence.value,
        ) { "An export batch event Identifier must contain its exact allocated sequence." }
        require(bundleSnapshot.hasTimestampElement()) { "An export batch requires its immutable assembly timestamp." }
        require(payloadSha256 == HealthConnectWireFormat.sha256(bundleJson)) {
            "The export batch payload checksum must match its exact UTF-8 Bundle JSON."
        }
        val parsedBundle = runCatching { JsonParser().parse(bundleJson) as? Bundle }.getOrNull()
        require(parsedBundle != null && parsedBundle.equalsDeep(bundleSnapshot)) {
            "The export batch Bundle must exactly match its authoritative JSON payload."
        }
        require(bundleSnapshot.entry.none { it.hasRequest() || it.hasResponse() }) {
            "A collection event cannot contain transaction request or response elements."
        }
        when (operation) {
            HealthConnectExportOperation.ACTIVE ->
                bundleSnapshot.requireGroveActiveExchangeContract(sourceRecordIdentifierSnapshot)
            HealthConnectExportOperation.RETRACTION -> bundleSnapshot.requireGroveEntryIdentitySelection()
        }
        bundleSnapshot.requireGroveReferencePolicy()
        when (operation) {
            HealthConnectExportOperation.ACTIVE -> {
                require(retractedTargetSnapshot.isEmpty()) { "An active event cannot carry retraction targets." }
                require(bundleSnapshot.meta.profile.map { it.value } == listOf(
                    HealthConnectContract.MOBILE_EXCHANGE_BUNDLE_PROFILE,
                )) { "An active export must claim exactly the Grove Mobile exchange Bundle profile." }
            }
            HealthConnectExportOperation.RETRACTION -> validateRetractionShape()
        }
    }

    private fun validateRetractionShape() {
        require(retractedTargetSnapshot.isNotEmpty()) { "A retraction event must name at least one prior graph node." }
        require(bundleSnapshot.meta.profile.map { it.value } == listOf(
            HealthConnectContract.MOBILE_RETRACTION_BUNDLE_PROFILE,
        )) { "A retraction export must claim exactly the Grove Mobile retraction Bundle profile." }
        require(bundleSnapshot.entry.size == 1 && bundleSnapshot.entry.single().resource is Provenance) {
            "A retraction Bundle contains only its lifecycle Provenance and no copied active resource."
        }
        val provenance = bundleSnapshot.entry.single().resource as Provenance
        validateRetractionAssertion(provenance)
        validateRetractionAgent(provenance)
        validateRetractionSource(provenance)
        validateRetractionTargets(provenance)
    }

    private fun validateRetractionAssertion(provenance: Provenance) {
        require(
            provenance.meta.profile.map { it.value } == listOf(
                HealthConnectContract.MOBILE_RETRACTION_PROVENANCE_PROFILE,
            ) &&
                provenance.activity.coding.singleOrNull()?.let {
                    it.system == HealthConnectContract.GROVE_LIFECYCLE_EVENT &&
                        it.code == "source-record-retracted"
                } == true,
        ) { "A retraction requires the exact Grove lifecycle assertion and profile." }
    }

    private fun validateRetractionAgent(provenance: Provenance) {
        require(
            provenance.agent.size == 1 &&
                provenance.agent.single().type.coding.singleOrNull()?.let {
                    it.system == HealthConnectContract.PROVENANCE_PARTICIPANT && it.code == "assembler"
                } == true &&
                provenance.agent.single().who.let {
                    !it.hasReference() && it.type == "Device" &&
                        it.identifier.hasGroveRole(GroveIdentifierRole.DEVICE_SNAPSHOT)
                },
        ) { "A retraction requires one current identifier-only assembler Device snapshot." }
    }

    private fun validateRetractionSource(provenance: Provenance) {
        require(
            provenance.entity.size == 1 &&
                provenance.entity.single().role == Provenance.ProvenanceEntityRole.SOURCE &&
                provenance.entity.single().what.let { source ->
                    !source.hasReference() && source.hasIdentifier() &&
                        source.identifier.hasGroveRole(GroveIdentifierRole.SOURCE_RECORD) &&
                        source.identifier.system == sourceRecordIdentifierSnapshot.system &&
                        source.identifier.value == sourceRecordIdentifierSnapshot.value
                },
        ) { "A retraction must identify the exact typed source Record as its sole source entity." }
    }

    private fun validateRetractionTargets(provenance: Provenance) {
        val actualTargets = provenance.target.map { target ->
            require(!target.hasReference() && target.hasType() && target.hasIdentifier()) {
                "A retraction target must be an identifier-only typed logical Reference."
            }
            val identifierRole = target.identifier.type.coding.singleOrNull {
                it.system == HealthConnectContract.GROVE_IDENTIFIER_ROLE
            }?.code
            val role = target.extension.singleOrNull {
                it.url == HealthConnectContract.GROVE_RETRACTION_TARGET_ROLE
            }?.value as? CodeType
            RetractionTargetShape(
                identifier = target.identifier.key(),
                identifierRole = identifierRole,
                resourceType = target.type,
                targetRole = role?.value,
            )
        }.toSet()
        val expectedTargets = retractedTargetSnapshot.map { target ->
            RetractionTargetShape(
                target.identifier,
                target.identifierRole.code,
                target.resourceType,
                target.role.code,
            )
        }.toSet()
        require(actualTargets == expectedTargets && actualTargets.size == provenance.target.size) {
            "The retraction Provenance must name every exact typed target and role once."
        }
    }

    val wireOperation: String
        get() = operation.wireValue

    val wireSourceVersion: String
        get() = HealthConnectWireFormat.sourceVersion(sourceVersion)

    private data class RetractionTargetShape(
        val identifier: FhirIdentifierKey,
        val identifierRole: String?,
        val resourceType: String,
        val targetRole: String?,
    )

    private companion object {
        val EVENT_IDENTIFIER_VALUE =
            Regex("""e0:[0-9a-f]{8}-[0-9a-f]{4}-[1-5][0-9a-f]{3}-[89ab][0-9a-f]{3}-[0-9a-f]{12}:[1-9][0-9]*""")
    }
}

/** A sink's confirmation that it durably stored the exact serialized event it was given. */
class HealthConnectExportAcknowledgement(
    destinationReferences: Map<FhirIdentifierKey, String>,
) {
    private val destinationReferenceSnapshot = destinationReferences.toMap()

    /** Defensive snapshot of the sink-owned response. */
    val destinationReferences: Map<FhirIdentifierKey, String>
        get() = destinationReferenceSnapshot.toMap()

    init {
        require(destinationReferenceSnapshot.values.all { it.isNotBlank() }) {
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
 * addressed by the exact `(identifier.system, identifier.value)` pair. The producer identity and
 * [HealthConnectExportBatch.eventSequence] form the idempotency key: an exact-event replay must be
 * a no-op returning the same acknowledgement, while the same key with different operation, source,
 * version, or checksum must fail closed. A thrown exception is a negative acknowledgement: the
 * coordinator does not commit journal state and the Health Connect changes token must not advance.
 */
fun interface HealthConnectExportSink {
    suspend fun apply(batch: HealthConnectExportBatch): HealthConnectExportAcknowledgement
}

/** Coordinates conversion, one-to-many update invalidation, deletion, replay, and full resync. */
class HealthConnectExportCoordinator(
    private val converter: HealthConnectConverter,
    private val journal: HealthConnectExportJournal,
    private val sink: HealthConnectExportSink,
) {
    private val synchronizationScope = converter.synchronizationScope
    private val activeExportBuilder = HealthConnectActiveExportBuilder(synchronizationScope)
    private val pendingDelivery = HealthConnectPendingExportDelivery(converter, journal, sink)

    val collectionScopeId: ScopeKey
        get() = synchronizationScope.projectionScopeKey

    val repositoryScopeId: ScopeKey
        get() = synchronizationScope.repositoryScopeKey

    suspend fun upsert(record: Record, convertedAt: Instant) {
        val recordType = sourceRecordType(record)
        val healthConnectId = record.metadata.id
        journal.withSourceTransition(
            synchronizationScope.repositoryScopeKey,
            recordType,
            healthConnectId,
        ) { lease ->
            upsert(record, convertedAt, lease)
        }
    }

    private suspend fun upsert(
        record: Record,
        convertedAt: Instant,
        lease: HealthConnectSourceTransitionLease,
    ) {
        journal.pending(lease)?.let { pending -> pendingDelivery.deliver(pending, lease) }
        val prior = journal.entry(lease)

        val preview = try {
            converter.preview(record, convertedAt, prior?.lastEventSequence)
        } catch (error: HealthConnectRecordRejected) {
            reject(record, lease, convertedAt, error)
            return
        }
        if (prior.requiresExplicitContractMigration(preview, synchronizationScope.projectionScopeKey)) {
            throw HealthConnectConversionContractMigrationRequired(preview.sourceRecordType)
        }
        when {
            migrateSourceIdentityIfRequired(record, preview, prior, convertedAt, lease) -> Unit
            prior?.isUnchangedActiveProjection(preview, synchronizationScope.projectionScopeKey) == true -> Unit
            prior?.state == HealthConnectExportState.ACTIVE && prior.outputIdentifiers.isNotEmpty() -> {
                // A changed immutable source version is a new event. Retract its complete prior
                // graph before publishing the replacement; active and lifecycle assertions never
                // share one collection Bundle.
                pendingDelivery.retract(prior, convertedAt, lease)
                upsert(record, convertedAt, lease)
            }
            storeZeroOutputLocally(record, preview, prior, lease) -> Unit
            else -> publishActive(record, convertedAt, preview, prior, lease)
        }
    }

    private suspend fun publishActive(
        record: Record,
        convertedAt: Instant,
        preview: HealthConnectConversion,
        prior: HealthConnectExportJournalEntry?,
        lease: HealthConnectSourceTransitionLease,
    ) {
        activeExportBuilder.draft(record, preview, prior)
        val pending = journal.stage(lease, prior?.revision) { eventSequence ->
            val conversion = converter.convert(record, convertedAt, eventSequence)
            activeExportBuilder.draft(record, conversion, prior)
        }
        if (
            pending.operation != HealthConnectExportOperation.ACTIVE ||
            pending.sourceVersion != preview.sourceLastModified
        ) {
            error("The journal returned an unrelated pending event while holding the source-transition lease.")
        }
        pendingDelivery.deliver(pending, lease)
    }

    private suspend fun migrateSourceIdentityIfRequired(
        record: Record,
        conversion: HealthConnectConversion,
        prior: HealthConnectExportJournalEntry?,
        convertedAt: Instant,
        lease: HealthConnectSourceTransitionLease,
    ): Boolean {
        if (!prior.requiresSourceIdentityRetirement(conversion, synchronizationScope.projectionScopeKey)) return false
        val priorEntry = requireNotNull(prior)
        if (priorEntry.state == HealthConnectExportState.ACTIVE && priorEntry.outputIdentifiers.isNotEmpty()) {
            pendingDelivery.retract(priorEntry, convertedAt, lease)
        }
        upsert(record, convertedAt, lease)
        return true
    }

    private suspend fun storeZeroOutputLocally(
        record: Record,
        conversion: HealthConnectConversion,
        prior: HealthConnectExportJournalEntry?,
        lease: HealthConnectSourceTransitionLease,
    ): Boolean {
        val hasPriorActiveOutputs = prior?.state == HealthConnectExportState.ACTIVE &&
            prior.outputIdentifiers.isNotEmpty()
        if (conversion.outputIdentifiers.isNotEmpty() || hasPriorActiveOutputs) return false
        journal.storeLocal(lease, prior?.revision, activeExportBuilder.entry(record, conversion, prior))
        return true
    }

    suspend fun delete(recordType: String, healthConnectId: String, invalidatedAt: Instant) {
        journal.withSourceTransition(
            synchronizationScope.repositoryScopeKey,
            recordType,
            healthConnectId,
        ) { lease ->
            delete(recordType, healthConnectId, invalidatedAt, lease)
        }
    }

    private suspend fun delete(
        recordType: String,
        healthConnectId: String,
        invalidatedAt: Instant,
        lease: HealthConnectSourceTransitionLease,
    ) {
        journal.pending(lease)?.let { pending -> pendingDelivery.deliver(pending, lease) }
        val prior = journal.entry(lease)
        if (prior == null) {
            journal.recordUnmatchedDeletion(
                lease,
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

        if (prior.outputIdentifiers.isEmpty()) {
            invalidateLocal(prior, invalidatedAt, lease)
            return
        }

        pendingDelivery.retract(prior, invalidatedAt, lease)
    }

    private suspend fun invalidateLocal(
        prior: HealthConnectExportJournalEntry,
        invalidatedAt: Instant,
        lease: HealthConnectSourceTransitionLease,
    ) {
        HealthConnectWireFormat.requireFhirInstant(invalidatedAt, "Local invalidation time")
        journal.storeLocal(
            lease,
            prior.revision,
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
     * is recognized by the retained local invalidation marker.
     */
    suspend fun reconcile(
        recordType: String,
        observedAt: () -> Instant,
        readAll: suspend () -> List<Record>,
    ) {
        journal.withReconciliationLease(
            synchronizationScope.repositoryScopeKey,
            recordType,
        ) { reconciliationLease ->
            // The complete source read is part of the fenced interval. Accepting an already-read
            // List here would leave a read-to-lease race in which another coordinator could publish
            // a newly visible source and then have this reconciliation retract it as absent.
            val ordered = readAll().sortedBy { it.metadata.id }
            val convertedAt = observedAt()
            require(ordered.all { sourceRecordType(it) == recordType }) {
                "A full reconciliation may contain only $recordType records."
            }
            require(ordered.map { it.metadata.id }.distinct().size == ordered.size) {
                "A full reconciliation cannot contain the same Health Connect id twice."
            }

            // Validate every candidate graph before replaying or publishing any sink event. Typed
            // record rejections remain source-local and are durably recorded during their later
            // transition.
            ordered.forEach { record ->
                try {
                    converter.preview(record, convertedAt, null)
                } catch (_: HealthConnectRecordRejected) {
                    // The source-local transition below records this exact rejection after preflight.
                }
            }

            journal.pendingForType(reconciliationLease)
                .sortedWith(compareBy(HealthConnectPendingExport::eventSequence))
                .forEach { snapshot ->
                    journal.withSourceTransition(
                        synchronizationScope.repositoryScopeKey,
                        recordType,
                        snapshot.healthConnectId,
                        reconciliationLease,
                    ) { sourceLease ->
                        journal.pending(sourceLease)?.let { pendingDelivery.deliver(it, sourceLease) }
                    }
                }

            ordered.forEach { record ->
                journal.withSourceTransition(
                    synchronizationScope.repositoryScopeKey,
                    recordType,
                    record.metadata.id,
                    reconciliationLease,
                ) { sourceLease ->
                    upsert(record, convertedAt, sourceLease)
                }
            }

            val presentIds = ordered.map { it.metadata.id }.toSet()
            journal.entries(reconciliationLease)
                .filter { it.state != HealthConnectExportState.INVALIDATED && it.healthConnectId !in presentIds }
                .sortedBy { it.healthConnectId }
                .forEach { entry ->
                    journal.withSourceTransition(
                        synchronizationScope.repositoryScopeKey,
                        recordType,
                        entry.healthConnectId,
                        reconciliationLease,
                    ) { sourceLease ->
                        delete(recordType, entry.healthConnectId, convertedAt, sourceLease)
                    }
                }
        }
    }

    private suspend fun reject(
        record: Record,
        lease: HealthConnectSourceTransitionLease,
        observedAt: Instant,
        error: HealthConnectRecordRejected,
    ) {
        val healthConnectId = record.metadata.id
        val prior = journal.entry(lease)
        if (prior?.state == HealthConnectExportState.ACTIVE) {
            delete(lease.recordType, healthConnectId, observedAt, lease)
        }
        journal.recordRejectedRecord(
            lease,
            HealthConnectRejectedRecord(
                repositoryScopeKey = synchronizationScope.repositoryScopeKey,
                projectionScopeKey = synchronizationScope.projectionScopeKey,
                recordType = lease.recordType,
                healthConnectId = healthConnectId,
                sourceLastModified = record.metadata.lastModifiedTime,
                observedAt = observedAt,
                reason = error.message ?: "Health Connect Record conversion failed.",
            ),
        )
    }

    private fun sourceRecordType(record: Record): String = RecordType.from(record).identifier
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
    val zeroOutputTimestampMatches = conversion.outputIdentifiers.isNotEmpty() ||
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
    state == HealthConnectExportState.ACTIVE &&
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
    outputIdentifiers.isNotEmpty() &&
    projectionScopeKey != currentProjectionScopeKey &&
    !sourceRecordIdentifier.sameCompleteIdentifier(conversion.sourceRecordIdentifier)

private fun org.hl7.fhir.r4.model.Identifier.sameCompleteIdentifier(
    other: org.hl7.fhir.r4.model.Identifier,
): Boolean = system == other.system && value == other.value

private fun outputIdentifierKey(observation: Observation): FhirIdentifierKey =
    observationIdentity(observation).key()
