//
// This source file is part of the My Heart Counts Android open-source project
//
// SPDX-FileCopyrightText: 2026 Stanford University and the project authors (see CONTRIBUTORS.md)
//
// SPDX-License-Identifier: MIT

package org.grovealliance.health.internal

import androidx.health.connect.client.HealthConnectClient
import androidx.health.connect.client.changes.DeletionChange
import androidx.health.connect.client.changes.UpsertionChange
import androidx.health.connect.client.records.Record
import androidx.health.connect.client.request.ChangesTokenRequest
import androidx.health.connect.client.response.ChangesResponse
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.cancelAndJoin
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import org.grovealliance.health.AnyRecordType
import org.grovealliance.health.CollectionMode
import org.grovealliance.health.CollectionTimeRange
import org.grovealliance.health.HealthConstraint
import org.grovealliance.health.healthLogger
import kotlin.time.Duration

/**
 * Component responsible for collecting Health data of a specific [recordType] based on the provided
 * [deliverySetting], [timeRange], and optional [predicate] filter.
 *
 * It utilizes a [ChangesTokenStore] to manage changes tokens for efficient data retrieval
 * and interacts with a [HealthConstraint] to deliver collected data.
 */
@Suppress("LongParameterList")
internal class HealthDataCollector(
    val recordType: AnyRecordType,
    val deliverySetting: HealthDataCollectorDeliverySetting,
    private val timeRange: CollectionTimeRange,
    private val predicate: ((Record) -> Boolean)?,
    private val tokenStore: ChangesTokenStore,
    private val scope: CoroutineScope,
    private val healthConstraint: HealthConstraint,
    private val client: HealthConnectClient,
) {
    private val logger by healthLogger()
    private var collectionJob: Job? = null
    private var projectionLease: CollectionProjectionLease? = null
    val collectionScopeId: String = healthConstraint.collectionScopeId(recordType).also {
        require(it.isNotBlank()) { "A HealthConstraint collection scope id must not be blank." }
    }
    val collectionRepositoryId: String = healthConstraint.collectionRepositoryId(recordType).also {
        require(it.isNotBlank()) { "A HealthConstraint repository scope id must not be blank." }
    }

    val isActive: Boolean
        get() = collectionJob?.isActive == true

    fun startDataCollection() {
        if (isActive) return

        collectionJob = scope.launch {
            when (val mode = deliverySetting.collectionMode) {
                is CollectionMode.Manual -> {
                    collectUntilDrained()
                }

                is CollectionMode.Automatic -> {
                    runPollingLoop(mode.pollingInterval)
                }
            }
        }
    }

    suspend fun stopDataCollectionAndJoin() {
        collectionJob?.cancelAndJoin()
        collectionJob = null
    }

    private suspend fun ensureProjectionLease(): CollectionProjectionLease {
        projectionLease?.let {
            tokenStore.requireProjectionOwner(recordType, it)
            return it
        }
        return tokenStore.claimProjection(recordType, collectionRepositoryId, collectionScopeId).also {
            projectionLease = it
        }
    }

    private suspend fun acquireDurableBoundary(lease: CollectionProjectionLease): String {
        val boundary = client.getChangesToken(ChangesTokenRequest(setOf(recordType.type)))
        tokenStore.storePendingBoundary(recordType, lease, boundary)
        return boundary
    }

    /**
     * Establishes an exact baseline and drains every page after its durable boundary.
     *
     * A pending boundary survives callback, process, and token-store failures. Retrying repeats the
     * scoped baseline and the same page sequence. If Health Connect expires that boundary, a new
     * boundary is durably installed before the baseline is repeated.
     */
    private suspend fun reconcileFromBoundary(
        initialBoundary: String?,
        lease: CollectionProjectionLease,
    ): ChangesResponse {
        var boundary = initialBoundary ?: acquireDurableBoundary(lease)
        while (true) {
            tokenStore.requireProjectionOwner(recordType, lease)
            healthConstraint.onFullyResyncRequired(recordType)
            var pageToken = boundary
            while (true) {
                val response = client.getChanges(pageToken)
                if (response.changesTokenExpired) {
                    logger.w { "Recovery boundary expired for $recordType. Re-establishing baseline." }
                    boundary = acquireDurableBoundary(lease)
                    break
                }
                processResult(response, lease)
                pageToken = response.nextChangesToken
                if (!response.hasMore) {
                    tokenStore.commitToken(recordType, lease, pageToken)
                    return response
                }
            }
        }
    }

    private suspend fun runPollingLoop(interval: Duration) {
        while (isActive) {
            runCatching {
                val result = collectOnce()
                if (!result.hasMore) delay(interval)
            }.onFailure {
                logger.e(it) { "Error collecting Health data for $recordType" }
                delay(interval)
            }
        }
    }

    /**
     * Delivers one change page and advances its token only after every callback succeeds.
     *
     * The token is the commit point for a page. Advancing it before the constraint has durably
     * accepted upserts and deletions loses that page when a callback fails.
     */
    internal suspend fun collectOnce(): ChangesResponse {
        val lease = ensureProjectionLease()
        var state = tokenStore.getState(recordType, lease)
        if (tokenStore.baselineRequired(recordType, lease)) {
            val boundary = state
                ?.takeIf { it.phase == ChangesTokenPhase.PENDING_BASELINE }
                ?.token
                ?: client.getChangesToken(ChangesTokenRequest(setOf(recordType.type)))
            tokenStore.storePendingBoundary(recordType, lease, boundary)
            state = ChangesTokenState(boundary, ChangesTokenPhase.PENDING_BASELINE)
        }
        if (state == null || state.phase == ChangesTokenPhase.PENDING_BASELINE) {
            return reconcileFromBoundary(state?.token, lease)
        }

        val result = client.getChanges(state.token)
        if (result.changesTokenExpired) {
            logger.w { "Token expired for $recordType. Performing full resync." }
            return reconcileFromBoundary(initialBoundary = null, lease = lease)
        }
        processResult(result, lease)
        tokenStore.commitToken(recordType, lease, result.nextChangesToken)
        return result
    }

    internal suspend fun collectUntilDrained() {
        var result: ChangesResponse
        do {
            result = collectOnce()
        } while (result.hasMore)
    }

    private suspend fun processResult(result: ChangesResponse, lease: CollectionProjectionLease) {
        // Preserve Health Connect's change order. Collapsing a page into independent upsert and
        // deletion sets can resurrect a record or delete its replacement when both changes for an
        // id occur in the same page.
        for (change in result.changes) {
            tokenStore.requireProjectionOwner(recordType, lease)
            when (change) {
                is UpsertionChange -> {
                    val record = change.record
                    if (!recordType.type.java.isAssignableFrom(record::class.java)) continue
                    if (matchesFilter(record)) {
                        healthConstraint.handleNewRecords(setOf(record), recordType)
                    } else {
                        healthConstraint.handleExcludedRecords(setOf(record.metadata.id), recordType)
                    }
                }

                is DeletionChange -> {
                    healthConstraint.handleDeletedRecords(setOf(change.recordId), recordType)
                }
            }
            tokenStore.requireProjectionOwner(recordType, lease)
        }
    }

    private fun matchesFilter(record: Record): Boolean {
        val predicateFilterMatched = predicate?.invoke(record) ?: true
        if (!predicateFilterMatched) return false

        return when (timeRange) {
            CollectionTimeRange.NewRecords -> true
            is CollectionTimeRange.StartingAt -> {
                val startInstant = record.startTime()
                val minDate = timeRange.date
                startInstant == null || startInstant >= minDate
            }
        }
    }
}
