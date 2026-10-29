//
// This source file is part of the My Heart Counts Android open-source project
//
// SPDX-FileCopyrightText: 2026 Stanford University and the project authors (see CONTRIBUTORS.md)
//
// SPDX-License-Identifier: MIT

package edu.stanford.myheartcounts.healthstats

import edu.stanford.myheartcounts.account.dateOfEnrollment
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import org.grovealliance.account.Account
import org.grovealliance.account.accountId
import org.grovealliance.core.coroutines.runCatchingAsync
import org.grovealliance.health.Health
import org.grovealliance.health.RecordType

/**
 * Serializes refreshes of the enrolled participant's complete statistics history.
 */
interface HealthStatsRefresher {
    suspend fun refresh(force: Boolean): Result<Unit>
    suspend fun awaitIdle()
}

class HealthStatsRefresherImpl(
    private val account: Account,
    private val history: HealthStatsHistory,
    private val health: Health,
    private val monthlySync: HealthStatsMonthlySync,
    private val configuration: HealthStatsSyncConfiguration,
    private val writer: HealthStatsWriter,
) : HealthStatsRefresher {
    private val lock = Mutex()

    override suspend fun refresh(force: Boolean): Result<Unit> = lock.withLock {
        val details = account.details.value
        val owner = details?.accountId ?: return@withLock Result.success(Unit)
        val enrolledAt = details.dateOfEnrollment ?: return@withLock Result.success(Unit)
        writer.awaitPendingWrites().getOrElse { return@withLock Result.failure(it) }
        val months = history.months(enrolledAt = enrolledAt)
        var failure: Throwable? = null
        val heightMonth = runCatchingAsync { health.mostRecent(type = RecordType.height) }
            .getOrNull()?.let { history.month(containing = it.time) }
        for (metric in enabledMetrics()) {
            val metricMonths = if (metric == HealthStatsSyncMetric.Height && heightMonth != null && heightMonth !in months) {
                listOf(heightMonth) + months
            } else {
                months
            }
            for (month in metricMonths) {
                currentCoroutineContext().ensureActive()
                if (account.details.value?.accountId != owner) return@withLock Result.failure(HealthStatsStoreException.SessionChanged())
                monthlySync.refresh(metric = metric, month = month, force = force).onFailure { if (failure == null) failure = it }
            }
        }
        failure?.let { Result.failure(it) } ?: Result.success(Unit)
    }

    private fun enabledMetrics(): List<HealthStatsSyncMetric> = HealthStatsSyncMetric.entries.filter {
        (it != HealthStatsSyncMetric.ExerciseTime || configuration.deriveExerciseTime) &&
            (it != HealthStatsSyncMetric.BodyMassIndex || configuration.deriveBodyMassIndex)
    }

    override suspend fun awaitIdle() = lock.withLock { }
}
