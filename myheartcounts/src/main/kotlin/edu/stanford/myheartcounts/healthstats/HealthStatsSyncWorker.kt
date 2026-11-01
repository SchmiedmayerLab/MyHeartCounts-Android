//
// This source file is part of the My Heart Counts Android open-source project
//
// SPDX-FileCopyrightText: 2026 Stanford University and the project authors (see CONTRIBUTORS.md)
//
// SPDX-License-Identifier: MIT

package edu.stanford.myheartcounts.healthstats

import android.content.Context
import androidx.work.Constraints
import androidx.work.CoroutineWorker
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.NetworkType
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.WorkerParameters
import org.grovealliance.core.coroutines.runCatchingAsync
import org.grovealliance.core.requireDependency
import java.util.concurrent.TimeUnit

/**
 * Refreshes monthly health statistics in scheduled background work.
 */
class HealthStatsSyncWorker(
    context: Context,
    parameters: WorkerParameters,
) : CoroutineWorker(
    context,
    parameters,
) {
    override suspend fun doWork(): Result {
        val refreshed = runCatchingAsync { requireDependency<HealthStatsSync>().refresh(force = true) }
        return if (refreshed.getOrNull()?.isSuccess == true) Result.success() else Result.retry()
    }
}

/**
 * Schedules and cancels background statistics refreshes.
 */
interface HealthStatsSyncScheduler {
    fun schedule()
    fun cancel()
}

class HealthStatsSyncSchedulerImpl(
    private val context: Context,
) : HealthStatsSyncScheduler {
    override fun schedule() {
        val request = PeriodicWorkRequestBuilder<HealthStatsSyncWorker>(
            repeatInterval = REFRESH_HOURS,
            repeatIntervalTimeUnit = TimeUnit.HOURS,
        ).setConstraints(Constraints.Builder().setRequiredNetworkType(NetworkType.CONNECTED).build()).build()
        workManager().enqueueUniquePeriodicWork(WORK_NAME, ExistingPeriodicWorkPolicy.UPDATE, request)
    }

    override fun cancel() {
        workManager().cancelUniqueWork(WORK_NAME)
    }

    private fun workManager(): WorkManager = WorkManager.getInstance(context)

    private companion object {
        const val REFRESH_HOURS = 12L
        const val WORK_NAME = "mhc-health-stats-sync"
    }
}
