//
// This source file is part of the My Heart Counts Android open-source project
//
// SPDX-FileCopyrightText: 2026 Stanford University and the project authors (see CONTRIBUTORS.md)
//
// SPDX-License-Identifier: MIT

package edu.stanford.myheartcounts.notification

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import com.google.firebase.messaging.RemoteMessage
import edu.stanford.myheartcounts.MHCStrings
import org.grovealliance.core.Module
import org.grovealliance.core.logging.groveLogger

/**
 * Shows the study's remote nudges and owns the notification channel they are posted on.
 *
 * Firebase Cloud Messaging displays a nudge itself while the app is in the background, on the channel
 * the manifest names as its default — the one created here. A nudge arriving while the app is in the
 * foreground is handed to the app instead, and [show] posts it the same way, so a nudge is always
 * shown and opening it is tracked identically either way.
 */
class MHCNudgeNotifications(
    private val context: Context,
    private val notificationPermissionHandler: NotificationPermissionHandler,
) : Module {

    private val logger by groveLogger(tag = "MHCFirebase")

    override fun configure() {
        val manager = context.getSystemService(NotificationManager::class.java)
        val channelId = context.getString(MHCStrings.nudge_notification_channel_id)
        if (manager.getNotificationChannel(channelId) != null) return
        manager.createNotificationChannel(
            NotificationChannel(
                channelId,
                context.getString(MHCStrings.nudge_notification_channel_name),
                NotificationManager.IMPORTANCE_HIGH,
            ).apply {
                description = context.getString(MHCStrings.nudge_notification_channel_description)
            },
        )
    }

    /**
     * Posts [message] as a notification, when it carries one and notifications are allowed.
     */
    fun show(message: RemoteMessage) {
        val content = message.notification ?: return
        val manager = NotificationManagerCompat.from(context)
        if (!notificationPermissionHandler.isGranted() || !manager.areNotificationsEnabled()) {
            logger.i { "Not showing nudge ${message.messageId}: notifications are not allowed." }
            return
        }
        val notificationId = (message.messageId ?: message.sentTime.toString()).hashCode()
        val notification = NotificationCompat.Builder(context, context.getString(MHCStrings.nudge_notification_channel_id))
            .setSmallIcon(context.applicationInfo.icon)
            .setContentTitle(content.title)
            .setContentText(content.body)
            .setStyle(NotificationCompat.BigTextStyle().bigText(content.body))
            .setAutoCancel(true)
            .setContentIntent(
                openIntent(
                    requestCode = notificationId,
                    extras = openExtras(messageId = message.messageId, data = message.data),
                ),
            )
            .build()
        @Suppress("MissingPermission") // Checked through notificationPermissionHandler above.
        manager.notify(notificationId, notification)
    }

    /**
     * Opens the app the way a nudge Firebase displayed would, carrying [extras] for open tracking.
     */
    private fun openIntent(requestCode: Int, extras: Map<String, String>): PendingIntent? {
        val intent = context.packageManager.getLaunchIntentForPackage(context.packageName) ?: return null
        extras.forEach { (key, value) -> intent.putExtra(key, value) }
        return PendingIntent.getActivity(
            context,
            requestCode,
            intent,
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT,
        )
    }

    companion object {

        /**
         * The launch-intent extra Firebase Cloud Messaging identifies a displayed message by; its
         * presence is what marks a launch as the opening of a nudge.
         */
        const val FCM_MESSAGE_ID_EXTRA = "google.message_id"

        /**
         * The extras that opening a nudge posted in the foreground launches the app with: the
         * message's data, plus its id under [FCM_MESSAGE_ID_EXTRA] when it has one — the same extras
         * the nudge carries when Firebase displays it in the background.
         */
        fun openExtras(messageId: String?, data: Map<String, String>): Map<String, String> =
            data + listOfNotNull(messageId?.let { FCM_MESSAGE_ID_EXTRA to it })
    }
}
