//
// This source file is part of the My Heart Counts Android open-source project
//
// SPDX-FileCopyrightText: 2026 Stanford University and the project authors (see CONTRIBUTORS.md)
//
// SPDX-License-Identifier: MIT

package edu.stanford.myheartcounts.healthstats

import com.google.firebase.firestore.FieldPath
import com.google.firebase.firestore.FirebaseFirestoreException
import com.google.firebase.firestore.MetadataChanges
import com.google.firebase.firestore.Query
import com.google.firebase.firestore.QuerySnapshot
import com.google.firebase.firestore.Source
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
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withContext
import org.grovealliance.account.Account
import org.grovealliance.account.accountId

/**
 * Reads the signed-in participant's health statistics, bound to that participant: a change of
 * account never lets one participant's read deliver another's data.
 */
interface HealthStatsStore {

    /**
     * Reads [request] once from where [readPolicy] allows, or fails with a
     * [HealthStatsStoreException], a [HealthStatsProcessingException] or the read's own error.
     */
    suspend fun <T> fetch(
        request: HealthStatsRequest<T>,
        readPolicy: HealthStatsReadPolicy = HealthStatsReadPolicy.ServerOrCache,
    ): Result<HealthStatsSnapshot<T>>

    /**
     * The newest result of [request] each time the participant's documents change.
     *
     * Follows the signed-in account, starting over when it changes and staying silent while nobody
     * is signed in. A failure is emitted as such; a failed read of the documents ends the flow,
     * while a result that cannot be processed is followed by the next change's result.
     */
    fun <T> updates(request: HealthStatsRequest<T>): Flow<Result<HealthStatsSnapshot<T>>>
}

class HealthStatsStoreImpl(
    private val account: Account,
    private val firestore: MHCFirestore,
    private val months: HealthStatsMonths,
    private val decoder: HealthStatsDocumentDecoder,
    private val processor: HealthStatsProcessor,
    private val processingDispatcher: CoroutineDispatcher,
) : HealthStatsStore {

    @Suppress("detekt:ReturnCount")
    override suspend fun <T> fetch(
        request: HealthStatsRequest<T>,
        readPolicy: HealthStatsReadPolicy,
    ): Result<HealthStatsSnapshot<T>> {
        val accountId = signedInAccountId() ?: return Result.failure(HealthStatsStoreException.NotSignedIn())

        val source = when (readPolicy) {
            HealthStatsReadPolicy.Server -> Source.SERVER
            HealthStatsReadPolicy.ServerOrCache -> Source.DEFAULT
            HealthStatsReadPolicy.Cache -> Source.CACHE
        }
        val snapshot = try {
            query(request = request).get(source).await()
        } catch (exception: FirebaseFirestoreException) {
            return Result.failure(exception)
        }
        if (signedInAccountId() != accountId) return Result.failure(HealthStatsStoreException.SessionChanged())
        return withContext(processingDispatcher) {
            process(
                snapshot = snapshot,
                request = request,
            )
        }
    }

    @OptIn(ExperimentalCoroutinesApi::class)
    override fun <T> updates(request: HealthStatsRequest<T>): Flow<Result<HealthStatsSnapshot<T>>> =
        account.details
            .map { it?.accountId }
            .distinctUntilChanged()
            .flatMapLatest { accountId ->
                if (accountId == null) {
                    emptyFlow()
                } else {
                    processed(
                        snapshots = snapshots(request = request),
                        request = request,
                    )
                }
            }

    /**
     * The query's snapshots as they change, including changes of their metadata alone. A failed
     * listener emits its error and ends the flow.
     */
    private fun snapshots(request: HealthStatsRequest<*>): Flow<Result<QuerySnapshot>> =
        callbackFlow {
            val registration = query(request = request)
                .addSnapshotListener(MetadataChanges.INCLUDE) { snapshot, error ->
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

    /**
     * Processes each snapshot, reusing the previous result with fresh metadata when only the
     * metadata changed.
     */
    private fun <T> processed(
        snapshots: Flow<Result<QuerySnapshot>>,
        request: HealthStatsRequest<T>,
    ): Flow<Result<HealthStatsSnapshot<T>>> = flow {
        var latest: HealthStatsSnapshot<T>? = null
        snapshots.collect { result ->
            val snapshot = result.getOrElse {
                emit(Result.failure(it))
                return@collect
            }
            val previous = latest
            val next = if (previous != null && snapshot.documentChanges.isEmpty()) {
                Result.success(
                    previous.copy(
                        isFromCache = snapshot.metadata.isFromCache,
                        hasPendingWrites = snapshot.metadata.hasPendingWrites(),
                    )
                )
            } else {
                withContext(processingDispatcher) {
                    process(
                        snapshot = snapshot,
                        request = request,
                    )
                }
            }
            next.onSuccess { latest = it }
            emit(next)
        }
    }

    private suspend fun query(request: HealthStatsRequest<*>): Query {
        val documentIds = months.documentIds(range = request.timeRange) ?: (NO_MONTH..NO_MONTH)
        return firestore.healthStatsMonths(metricId = request.metricId)
            .whereGreaterThanOrEqualTo(FieldPath.documentId(), documentIds.start)
            .whereLessThanOrEqualTo(FieldPath.documentId(), documentIds.endInclusive)
    }

    /**
     * Decodes the snapshot's documents, leaving out and counting those that cannot be read or belong
     * to another metric or format version, and processes the rest.
     */
    private fun <T> process(snapshot: QuerySnapshot, request: HealthStatsRequest<T>): Result<HealthStatsSnapshot<T>> {
        var invalidDocuments = 0
        val documents = snapshot.documents.mapNotNull { document ->
            decoder.decode(data = document.data.orEmpty())
                .getOrNull()
                ?.takeIf { it.version == 0 && it.metric == request.metricId }
                .also { if (it == null) invalidDocuments++ }
        }
        return processor.process(request = request, documents = documents).map { output ->
            val invalidDocumentDiagnostics = if (invalidDocuments > 0) {
                listOf(HealthStatsDiagnostic.InvalidDocumentCount(count = invalidDocuments))
            } else {
                emptyList()
            }
            HealthStatsSnapshot(
                elements = output.elements,
                diagnostics = output.diagnostics + invalidDocumentDiagnostics,
                contributingSourceIds = output.contributingSourceIds,
                isFromCache = snapshot.metadata.isFromCache,
                hasPendingWrites = snapshot.metadata.hasPendingWrites(),
            )
        }
    }

    private fun signedInAccountId(): String? = account.details.value?.accountId

    private companion object {
        /**
         * A month identifier no document carries, selecting nothing.
         */
        const val NO_MONTH = "0000-00"
    }
}

/**
 * Describes why the participant's statistics could not be read.
 */
sealed class HealthStatsStoreException(message: String) : Exception(message) {

    /**
     * Nobody is signed in.
     */
    class NotSignedIn : HealthStatsStoreException("Not signed in")

    /**
     * The signed-in account changed while the read was underway, so its result was discarded.
     */
    class SessionChanged : HealthStatsStoreException("The signed-in account changed")
}
