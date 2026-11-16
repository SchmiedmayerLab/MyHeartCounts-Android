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
import org.grovealliance.scheduler.OpenedTaskNotification
import java.math.BigDecimal
import java.util.TimeZone
import java.util.UUID

/**
 * Records that the participant acted on a notification, so the study can tell which nudges landed.
 *
 * Each interaction is one document in the participant's notification tracking collection.
 *
 * @param bundleIdentifier The app's identifier, which prefixes the identifiers of task reminders.
 */
class MHCNotificationTracking(
    private val firestore: MHCFirestore,
    private val timeProvider: TimeProvider,
    private val bundleIdentifier: String,
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

    /**
     * Records that the participant opened the reminder for the task event [reminder]. The reminder is
     * identified as `<bundleIdentifier>.scheduler.notification.event.<task>.<occurrence start>`, the
     * start in seconds since 2001-01-01 UTC, and carries the task's identifier as its payload.
     */
    suspend fun trackDidOpen(reminder: OpenedTaskNotification) {
        val occurrenceStart = BigDecimal.valueOf(reminder.occurrenceStart.toEpochMilli() - REFERENCE_DATE_MILLIS)
            .movePointLeft(MILLIS_DECIMALS)
            .stripTrailingZeros()
            .let { if (it.scale() <= 0) it.setScale(1) else it }
            .toPlainString()
        trackDidOpen(
            notificationId = "$bundleIdentifier.$REMINDER_EVENT_ID_INFIX.${reminder.taskId}.$occurrenceStart",
            payload = mapOf("$bundleIdentifier.$REMINDER_TASK_ID_KEY_SUFFIX" to reminder.taskId),
        )
    }

    private companion object {
        /**
         * What follows the app's identifier in a task reminder's identifier.
         */
        const val REMINDER_EVENT_ID_INFIX = "scheduler.notification.event"

        /**
         * What follows the app's identifier in the payload key carrying a task reminder's task.
         */
        const val REMINDER_TASK_ID_KEY_SUFFIX = "scheduler.notification.taskId"

        /**
         * 2001-01-01T00:00:00Z, the moment occurrence starts in reminder identifiers count seconds from.
         */
        const val REFERENCE_DATE_MILLIS = 978_307_200_000L

        const val MILLIS_DECIMALS = 3
    }
}
