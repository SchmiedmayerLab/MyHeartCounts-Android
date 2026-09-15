//
// This source file is part of the My Heart Counts Android open-source project
//
// SPDX-FileCopyrightText: 2026 Stanford University and the project authors (see CONTRIBUTORS.md)
//
// SPDX-License-Identifier: MIT

package edu.stanford.myheartcounts.notification

import com.google.firebase.Timestamp
import edu.stanford.myheartcounts.firebase.MHCFirestore
import kotlinx.coroutines.tasks.await
import org.grovealliance.core.Module
import org.grovealliance.core.logging.groveLogger
import org.grovealliance.core.time.TimeProvider
import java.util.TimeZone
import java.util.UUID

/**
 * Records that the participant acted on a notification, so the study can tell which nudges landed.
 *
 * Ports iOS's `NotificationTracking`, writing the same document shape into the same collection.
 */
class MHCNotificationTracking(
    private val firestore: MHCFirestore,
    private val timeProvider: TimeProvider,
) : Module {

    private val logger by groveLogger(tag = "MHCFirebase")

    /**
     * Records that the participant opened the notification identified by [notificationId].
     *
     * @param notificationId The identifier the notification was posted with.
     * @param payload Anything else the notification carried, kept verbatim for later analysis.
     */
    suspend fun trackDidOpen(notificationId: String, payload: Map<String, String> = emptyMap()) {
        runCatching {
            firestore.awaitReady()
            firestore.notificationTracking
                .document(UUID.randomUUID().toString())
                .set(
                    mapOf(
                        "timestamp" to Timestamp(timeProvider.nowInstant().epochSecond, 0),
                        "timeZone" to TimeZone.getDefault().id,
                        "event" to EVENT_OPENED,
                        "notificationId" to notificationId,
                        "additionalStuff" to payload.toString(),
                    ),
                )
                .await()
        }.onFailure { throwable ->
            logger.e(throwable) { "Failed to record that notification '$notificationId' was opened." }
        }
    }

    private companion object {
        /**
         * The only event either platform records today. Matches iOS's `TrackedNotificationEvent`.
         */
        const val EVENT_OPENED = "opened"
    }
}
