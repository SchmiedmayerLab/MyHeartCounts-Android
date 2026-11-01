//
// This source file is part of the My Heart Counts Android open-source project
//
// SPDX-FileCopyrightText: 2026 Stanford University and the project authors (see CONTRIBUTORS.md)
//
// SPDX-License-Identifier: MIT

package edu.stanford.myheartcounts.notification

import com.google.firebase.Timestamp
import edu.stanford.myheartcounts.firebase.FirebaseSchema
import edu.stanford.myheartcounts.firebase.MHCFirestore
import edu.stanford.myheartcounts.firebase.mhcFirebaseLogger
import kotlinx.coroutines.tasks.await
import org.grovealliance.core.Module
import org.grovealliance.core.coroutines.runCatchingAsync
import org.grovealliance.core.time.TimeProvider
import java.util.TimeZone
import java.util.UUID

/**
 * Records that the participant acted on a notification, so the study can tell which nudges landed.
 *
 * Each interaction is one document in the participant's notification tracking collection.
 */
class MHCNotificationTracking(
    private val firestore: MHCFirestore,
    private val timeProvider: TimeProvider,
) : Module {

    private val logger by mhcFirebaseLogger()

    /**
     * Records that the participant opened the notification identified by [notificationId].
     *
     * @param notificationId The identifier the notification was posted with.
     * @param payload Anything else the notification carried, kept verbatim for later analysis.
     */
    suspend fun trackDidOpen(notificationId: String, payload: Map<String, String> = emptyMap()) {
        runCatchingAsync {
            firestore.notificationTracking()
                .document(UUID.randomUUID().toString())
                .set(
                    mapOf(
                        FirebaseSchema.NotificationTracking.TIMESTAMP to Timestamp(timeProvider.nowInstant().epochSecond, 0),
                        FirebaseSchema.NotificationTracking.TIME_ZONE to TimeZone.getDefault().id,
                        FirebaseSchema.NotificationTracking.EVENT to FirebaseSchema.NotificationTracking.EVENT_OPENED,
                        FirebaseSchema.NotificationTracking.NOTIFICATION_ID to notificationId,
                        FirebaseSchema.NotificationTracking.ADDITIONAL_STUFF to payload.toString(),
                    ),
                )
                .await()
        }.onFailure { throwable ->
            logger.e(throwable) { "Failed to record that notification '$notificationId' was opened." }
        }
    }
}
