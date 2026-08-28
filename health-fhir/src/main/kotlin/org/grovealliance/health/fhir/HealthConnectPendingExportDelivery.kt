//
// This source file belongs to the My Heart Counts Android project
//
// SPDX-FileCopyrightText: 2026 Stanford University and the project authors (see CONTRIBUTORS.md)
//
// SPDX-License-Identifier: MIT

package org.grovealliance.health.fhir

import java.time.Instant

/** Owns exact pending delivery and construction of prior-output retraction events. */
internal class HealthConnectPendingExportDelivery(
    converter: HealthConnectConverter,
    private val journal: HealthConnectExportJournal,
    private val sink: HealthConnectExportSink,
) {
    private val synchronizationScope = converter.synchronizationScope
    private val retractionBuilder = HealthConnectRetractionBuilder(converter)

    suspend fun retract(
        prior: HealthConnectExportJournalEntry,
        retractedAt: Instant,
        lease: HealthConnectSourceTransitionLease,
    ) {
        HealthConnectWireFormat.requireFhirInstant(retractedAt, "Retraction event time")
        val targets = retractionBuilder.targets(prior)
        require(targets.isNotEmpty()) { "An exchange retraction must name at least one prior output." }
        val pending = journal.stage(
            lease,
            prior.revision,
        ) { eventSequence ->
            val retractionBundle = retractionBuilder.bundle(prior, targets, retractedAt, eventSequence)
            HealthConnectPendingExportDraft(
                repositoryScopeKey = synchronizationScope.repositoryScopeKey,
                projectionScopeKey = synchronizationScope.projectionScopeKey,
                operation = HealthConnectExportOperation.RETRACTION,
                recordType = prior.recordType,
                healthConnectId = prior.healthConnectId,
                sourceRecordIdentifier = prior.sourceRecordIdentifier.copy(),
                sourceVersion = prior.sourceLastModified,
                bundle = retractionBundle,
                retractedTargets = targets,
                nextEntry = prior.copy(
                    projectionScopeKey = synchronizationScope.projectionScopeKey,
                    observations = emptyList(),
                    bundle = retractionBundle.copy(),
                    destinationReferences = emptyMap(),
                    state = HealthConnectExportState.INVALIDATED,
                    invalidatedAt = retractedAt,
                ),
            )
        }
        check(pending.operation == HealthConnectExportOperation.RETRACTION) {
            "The journal returned an unrelated pending event while holding the source-transition lease."
        }
        deliver(pending, lease)
    }

    suspend fun deliver(
        pending: HealthConnectPendingExport,
        lease: HealthConnectSourceTransitionLease,
    ) {
        val acknowledgement = sink.apply(pending.batch())
        val completedEntry = pending.acknowledgedEntry(acknowledgement.destinationReferences)
        journal.complete(lease, pending, completedEntry)
    }
}
