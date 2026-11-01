//
// This source file is part of the My Heart Counts Android open-source project
//
// SPDX-FileCopyrightText: 2026 Stanford University and the project authors (see CONTRIBUTORS.md)
//
// SPDX-License-Identifier: MIT

package edu.stanford.myheartcounts.healthstats

import com.google.firebase.firestore.FieldPath
import com.google.firebase.firestore.FirebaseFirestoreException
import com.google.firebase.firestore.SetOptions
import edu.stanford.myheartcounts.firebase.FirebaseSchema
import edu.stanford.myheartcounts.firebase.MHCFirestore
import kotlinx.coroutines.tasks.await
import org.grovealliance.account.Account
import org.grovealliance.account.accountId

/**
 * Writes one source's statistics without replacing other contributions.
 */
interface HealthStatsWriter {
    suspend fun awaitPendingWrites(): Result<Unit>
    suspend fun write(contribution: HealthStatsContribution): Result<Unit>
}

class HealthStatsWriterImpl(
    private val firestore: MHCFirestore,
    private val encoder: HealthStatsDocumentEncoder,
    private val account: Account,
) : HealthStatsWriter {
    override suspend fun awaitPendingWrites(): Result<Unit> {
        return try {
            firestore.usersCollection().firestore.waitForPendingWrites().await()
            Result.success(Unit)
        } catch (error: FirebaseFirestoreException) {
            Result.failure(error)
        }
    }

    override suspend fun write(contribution: HealthStatsContribution): Result<Unit> {
        val accountId = account.details.value?.accountId.orEmpty()
        if (accountId.isEmpty()) return Result.failure(HealthStatsStoreException.NotSignedIn())
        return encoder.encode(contribution = contribution).fold(
            onSuccess = { data -> writeEncoded(accountId = accountId, contribution = contribution, data = data) },
            onFailure = { Result.failure(it) },
        )
    }

    private suspend fun writeEncoded(
        accountId: String,
        contribution: HealthStatsContribution,
        data: Map<String, Any>,
    ): Result<Unit> {
        val users = firestore.usersCollection()
        if (account.details.value?.accountId != accountId) return Result.failure(HealthStatsStoreException.SessionChanged())
        val document = users.document(accountId)
            .collection(FirebaseSchema.Collection.STATS)
            .document(contribution.metricId)
            .collection(FirebaseSchema.Collection.STATS_MONTHS)
            .document(contribution.month.toString())
        val sourcePath = FieldPath.of(contribution.collection.field, contribution.sourceId)
        return try {
            if (contribution.entries.isEmpty()) {
                document.update(sourcePath, emptyList<Map<String, Any>>()).await()
            } else {
                document.set(
                    data,
                    SetOptions.mergeFieldPaths(
                        listOf(
                            FieldPath.of(FirebaseSchema.StatsDocument.VERSION),
                            FieldPath.of(FirebaseSchema.StatsDocument.METRIC),
                            sourcePath,
                        ),
                    ),
                ).await()
            }
            Result.success(Unit)
        } catch (error: FirebaseFirestoreException) {
            if (contribution.entries.isEmpty() && error.code == FirebaseFirestoreException.Code.NOT_FOUND) {
                Result.success(Unit)
            } else {
                Result.failure(error)
            }
        }
    }
}
