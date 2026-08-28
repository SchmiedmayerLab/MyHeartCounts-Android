//
// This source file belongs to the My Heart Counts Android project
//
// SPDX-FileCopyrightText: 2026 Stanford University and the project authors (see CONTRIBUTORS.md)
//
// SPDX-License-Identifier: MIT

package org.grovealliance.health.fhir

import androidx.health.connect.client.records.Record
import org.grovealliance.health.AnyRecordType
import org.grovealliance.health.HealthConstraint
import org.grovealliance.health.RecordType
import java.time.Instant

/** Supplies a complete paged read for one Health Connect record type after token expiry. */
fun interface HealthConnectFullReader {
    suspend fun readAll(type: AnyRecordType): List<Record>
}

/**
 * Connects the reusable Health collector to the acknowledged FHIR export coordinator.
 *
 * Production wiring must provide a durable journal, an idempotent sink, and a full reader that
 * drains every Health Connect page for the requested type.
 */
class HealthConnectSynchronizationConstraint(
    private val coordinator: HealthConnectExportCoordinator,
    private val fullReader: HealthConnectFullReader,
    private val now: () -> Instant,
) : HealthConstraint {
    override fun <T : Record> collectionScopeId(type: RecordType<out T>): String {
        requireSupported(type.identifier)
        // HealthConstraint is an external interface keyed by opaque strings.
        return coordinator.collectionScopeId.value
    }

    override fun <T : Record> collectionRepositoryId(type: RecordType<out T>): String {
        requireSupported(type.identifier)
        return coordinator.repositoryScopeId.value
    }

    override suspend fun <T : Record> handleNewRecords(
        addedRecords: Set<T>,
        type: RecordType<out T>,
    ) {
        requireSupported(type.identifier)
        addedRecords.sortedBy { it.metadata.id }.forEach { coordinator.upsert(it, now()) }
    }

    override suspend fun <T : Record> handleDeletedRecords(
        deletedRecordIds: Set<String>,
        type: RecordType<out T>,
    ) {
        requireSupported(type.identifier)
        deletedRecordIds.sorted().forEach { coordinator.delete(type.identifier, it, now()) }
    }

    override suspend fun <T : Record> handleExcludedRecords(
        excludedRecordIds: Set<String>,
        type: RecordType<out T>,
    ) {
        requireSupported(type.identifier)
        excludedRecordIds.sorted().forEach { coordinator.delete(type.identifier, it, now()) }
    }

    override suspend fun <T : Record> onFullyResyncRequired(type: RecordType<out T>) {
        requireSupported(type.identifier)
        coordinator.reconcile(type.identifier, now) {
            fullReader.readAll(type)
        }
    }

    private fun requireSupported(recordType: String) {
        if (recordType !in HealthConnectCatalog.supportedRecordTypeIdentifiers) {
            throw UnsupportedHealthConnectRecord(recordType)
        }
    }
}
