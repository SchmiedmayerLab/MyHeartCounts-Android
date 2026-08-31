//
// This source file is part of the My Heart Counts Android open-source project
//
// SPDX-FileCopyrightText: 2026 Stanford University and the project authors (see CONTRIBUTORS.md)
//
// SPDX-License-Identifier: MIT

package org.grovealliance.health.fhir

import androidx.health.connect.client.records.Record

/** Builds immutable active outbox state without owning journal or sink side effects. */
internal class HealthConnectActiveExportBuilder(
    private val synchronizationScope: HealthConnectSynchronizationScope,
) {
    fun draft(
        record: Record,
        conversion: HealthConnectConversion,
        prior: HealthConnectExportJournalEntry?,
    ): HealthConnectPendingExportDraft = HealthConnectPendingExportDraft(
        repositoryScopeKey = synchronizationScope.repositoryScopeKey,
        projectionScopeKey = synchronizationScope.projectionScopeKey,
        operation = HealthConnectExportOperation.ACTIVE,
        recordType = conversion.sourceRecordType,
        healthConnectId = record.metadata.id,
        sourceRecordIdentifier = conversion.sourceRecordIdentifier.copy(),
        sourceVersion = conversion.sourceLastModified,
        bundle = conversion.bundle,
        retractedTargets = emptySet(),
        nextEntry = entry(record, conversion, prior),
    )

    fun entry(
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
        conversionContractMarker = conversion.conversionContractMarker,
        sourceRecordIdentifier = conversion.sourceRecordIdentifier.copy(),
        bundle = conversion.bundle.copy(),
        destinationReferences = emptyMap(),
        lastEventSequence = prior?.lastEventSequence,
    )
}
