//
// This source file is part of the My Heart Counts Android open-source project
//
// SPDX-FileCopyrightText: 2026 Stanford University and the project authors (see CONTRIBUTORS.md)
//
// SPDX-License-Identifier: MIT

package edu.stanford.myheartcounts.standard.health

import android.content.Context
import androidx.work.Constraints
import androidx.work.CoroutineWorker
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.NetworkType
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.WorkerParameters
import org.grovealliance.core.logging.groveLogger
import org.grovealliance.core.requireDependency
import java.util.concurrent.TimeUnit

/**
 * Uploads the staged health data in the background.
 *
 * The counterpart of iOS's `BGProcessingTask`: health data is staged as it arrives and shipped later
 * in bulk, so that a participant's battery and data plan are not spent on a stream of small uploads.
 */
class HealthUploadWorker(
    context: Context,
    parameters: WorkerParameters,
) : CoroutineWorker(context, parameters) {

    private val logger by groveLogger(tag = "MHCFirebase")

    override suspend fun doWork(): Result {
        val handler = runCatching { requireDependency<MHCHealthDataHandler>() }.getOrElse { throwable ->
            // Grove's graph is built from a content provider, so it exists by the time any worker
            // runs; if it somehow does not, retrying is right — the data stays staged either way.
            logger.e(throwable) { "Grove is not available yet; retrying the health upload later." }
            return Result.retry()
        }
        handler.recoverInterruptedUploads()
        return handler.uploadStagedData().fold(
            onSuccess = { Result.success() },
            onFailure = { throwable ->
                logger.e(throwable) { "The staged health upload failed; retrying later." }
                Result.retry()
            },
        )
    }

    companion object {

        private const val WORK_NAME = "mhc-health-upload"

        /**
         * Schedules the periodic upload, replacing an existing schedule so a changed interval takes
         * effect rather than being ignored.
         *
         * Requiring a network is what keeps this from burning retries offline; WorkManager holds the
         * run back until the device has one.
         */
        fun schedule(context: Context) {
            val request = PeriodicWorkRequestBuilder<HealthUploadWorker>(
                repeatInterval = HealthUploadSchedule.UPLOAD_INTERVAL_HOURS,
                repeatIntervalTimeUnit = TimeUnit.HOURS,
            ).setConstraints(
                Constraints.Builder()
                    .setRequiredNetworkType(NetworkType.CONNECTED)
                    .build(),
            ).build()

            WorkManager.getInstance(context.applicationContext).enqueueUniquePeriodicWork(
                WORK_NAME,
                ExistingPeriodicWorkPolicy.UPDATE,
                request,
            )
        }

        /**
         * Stops the periodic upload, for when the participant signs out and there is nothing left to
         * send.
         */
        fun cancel(context: Context) {
            WorkManager.getInstance(context.applicationContext).cancelUniqueWork(WORK_NAME)
        }
    }
}
