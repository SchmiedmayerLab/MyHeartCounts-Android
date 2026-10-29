//
// This source file is part of the My Heart Counts Android open-source project
//
// SPDX-FileCopyrightText: 2026 Stanford University and the project authors (see CONTRIBUTORS.md)
//
// SPDX-License-Identifier: MIT

package edu.stanford.myheartcounts.customsamples

import com.google.firebase.firestore.QuerySnapshot
import edu.stanford.myheartcounts.firebase.MHCFirestore
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.flow.conflate
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.emptyFlow
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.map
import org.grovealliance.account.Account
import org.grovealliance.account.accountId
import org.grovealliance.scheduler.InstantRange

/**
 * The signed-in participant's [CustomQuantitySample]s, as they change.
 */
interface CustomSampleSource {

    /**
     * The participant's samples of [type] within [timeRange] (all of them when `null`), newest
     * first, each time they change.
     *
     * Follows the signed-in account, starting over when it changes and staying silent while nobody
     * is signed in. Observations that cannot be read are left out. A failed read is emitted as such
     * and ends the flow.
     */
    fun samples(type: CustomQuantitySampleType, timeRange: InstantRange?): Flow<Result<List<CustomQuantitySample>>>
}

class CustomSampleSourceImpl(
    private val account: Account,
    private val firestore: MHCFirestore,
    private val decoder: CustomSampleDecoder,
    private val processingDispatcher: CoroutineDispatcher,
) : CustomSampleSource {

    @OptIn(ExperimentalCoroutinesApi::class)
    override fun samples(
        type: CustomQuantitySampleType,
        timeRange: InstantRange?,
    ): Flow<Result<List<CustomQuantitySample>>> = account.details
        .map { it?.accountId }
        .distinctUntilChanged()
        .flatMapLatest { accountId ->
            if (accountId == null) {
                emptyFlow()
            } else {
                snapshots(type = type).map { result ->
                    result.map { snapshot ->
                        samples(
                            snapshot = snapshot,
                            type = type,
                            timeRange = timeRange,
                        )
                    }
                }.flowOn(processingDispatcher)
            }
        }

    /**
     * The collection's snapshots as its documents change. A failed listener emits its error and
     * ends the flow.
     */
    private fun snapshots(type: CustomQuantitySampleType): Flow<Result<QuerySnapshot>> = callbackFlow {
        val registration = firestore.healthObservations(sampleTypeIdentifier = type.id)
            .addSnapshotListener { snapshot, error ->
                when {
                    error != null -> {
                        trySend(Result.failure(error))
                        close()
                    }
                    snapshot != null -> trySend(Result.success(snapshot))
                }
            }
        awaitClose { registration.remove() }
    }.conflate()

    private fun samples(
        snapshot: QuerySnapshot,
        type: CustomQuantitySampleType,
        timeRange: InstantRange?,
    ): List<CustomQuantitySample> = snapshot.documents
        .mapNotNull { document ->
            decoder.decode(
                data = document.data.orEmpty(),
                type = type,
                timeRange = timeRange,
            )
        }
        .sortedByDescending { it.timeRange.start }
}
