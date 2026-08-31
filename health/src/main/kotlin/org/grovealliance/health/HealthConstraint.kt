//
// This source file is part of the My Heart Counts Android open-source project
//
// SPDX-FileCopyrightText: 2026 Stanford University and the project authors (see CONTRIBUTORS.md)
//
// SPDX-License-Identifier: MIT

package org.grovealliance.health

import androidx.health.connect.client.records.Record
import org.grovealliance.core.Standard

/**
 * A [Standard] extension for handling Health Connect data changes.
 */
interface HealthConstraint : Standard {
    /**
     * Stable identifier for the repository and filter/configuration projection being collected.
     *
     * Change this value whenever the source repository, time range, predicate, or other full-read
     * membership rule changes. It namespaces durable change-token state. Implementations that do
     * not maintain a synchronized projection may keep the default.
     */
    fun <T : Record> collectionScopeId(type: RecordType<out T>): String = DEFAULT_COLLECTION_SCOPE_ID

    /** Stable repository identity shared by every filter projection over the same source store. */
    fun <T : Record> collectionRepositoryId(type: RecordType<out T>): String = DEFAULT_REPOSITORY_SCOPE_ID

    /**
     * Called when new Health Connect records of a given type are detected.
     */
    suspend fun <T : Record> handleNewRecords(addedRecords: Set<T>, type: RecordType<out T>)

    /**
     * Called when records are detected as deleted or invalidated.
     */
    suspend fun <T : Record> handleDeletedRecords(deletedRecordIds: Set<String>, type: RecordType<out T>)

    /**
     * Called when an upserted Record does not match this collector's time range or predicate.
     *
     * Constraints that maintain a synchronized projection use this callback to invalidate a
     * previously included Record. The default preserves source compatibility for constraints that
     * treat collection filters as delivery-only.
     */
    suspend fun <T : Record> handleExcludedRecords(excludedRecordIds: Set<String>, type: RecordType<out T>) = Unit

    /**
     * Called when Health Connect reports that the stored changes token for this
     * record type has expired. Incremental updates are no longer reliable, so the app must
     * perform a complete scoped read and reconcile it with durable exported state before returning.
     *
     * Do not discard identity or deletion-journal entries before reconciliation. They are needed
     * to invalidate records that no longer exist in Health Connect. The collector establishes a
     * durable change-token boundary before this callback and replays every page from that boundary.
     * The same callback is used to establish the first baseline for a new collection scope.
     */
    suspend fun <T : Record> onFullyResyncRequired(type: RecordType<out T>)

    companion object {
        /** One [org.grovealliance.health.internal.DefaultHealthClient] fronts one repository. */
        const val DEFAULT_REPOSITORY_SCOPE_ID = "health-connect-default-repository"

        /** Its digest is an ingredient of every persisted token key, so it has exactly one home. */
        const val DEFAULT_COLLECTION_SCOPE_ID = "default"
    }
}
