//
// This source file is part of the My Heart Counts Android open-source project
//
// SPDX-FileCopyrightText: 2026 Stanford University and the project authors (see CONTRIBUTORS.md)
//
// SPDX-License-Identifier: MIT

package edu.stanford.myheartcounts.healthstats

import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.ensureActive
import org.grovealliance.account.Account
import org.grovealliance.account.accountId
import java.time.YearMonth

/**
 * Advances monthly checkpoints only after the corresponding source contribution is acknowledged.
 */
interface HealthStatsPersistence {
    fun checkpoint(metric: HealthStatsSyncMetric, month: YearMonth): HealthStatsCheckpoint?
    suspend fun persist(
        contribution: HealthStatsContribution,
        checkpoint: HealthStatsCheckpoint,
        hasDeletions: Boolean,
    ): Result<Unit>
    fun clear()
}

class HealthStatsPersistenceImpl(
    private val writer: HealthStatsWriter,
    private val checkpoints: HealthStatsCheckpointStore,
    private val account: Account,
) : HealthStatsPersistence {
    override fun checkpoint(metric: HealthStatsSyncMetric, month: YearMonth): HealthStatsCheckpoint? =
        checkpoints.get(metricId = metric.id, month = month)

    override suspend fun persist(
        contribution: HealthStatsContribution,
        checkpoint: HealthStatsCheckpoint,
        hasDeletions: Boolean,
    ): Result<Unit> {
        currentCoroutineContext().ensureActive()
        val owner = account.details.value?.accountId.orEmpty()
        if (owner.isEmpty()) return Result.failure(HealthStatsStoreException.NotSignedIn())
        if (contribution.entries.isNotEmpty() || hasDeletions) {
            writer.write(contribution = contribution).getOrElse { return Result.failure(it) }
        }
        currentCoroutineContext().ensureActive()
        return if (account.details.value?.accountId == owner) {
            checkpoints.put(metricId = contribution.metricId, month = contribution.month, checkpoint = checkpoint)
            Result.success(Unit)
        } else {
            Result.failure(HealthStatsStoreException.SessionChanged())
        }
    }

    override fun clear() = checkpoints.clear()
}
