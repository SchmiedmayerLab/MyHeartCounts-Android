//
// This source file is part of the My Heart Counts Android open-source project
//
// SPDX-FileCopyrightText: 2026 Stanford University and the project authors (see CONTRIBUTORS.md)
//
// SPDX-License-Identifier: MIT

package org.grovealliance.scheduler.internal

import android.Manifest
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat

/**
 * Posts a task notification when its scheduled alarm fires. The full payload travels in the alarm's
 * intent, so no dependency graph access is needed here.
 */
internal class SchedulerNotificationReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action != ACTION_DELIVER) return
        val id = intent.getStringExtra(EXTRA_ID) ?: return
        val granted = ContextCompat.checkSelfPermission(
            context,
            Manifest.permission.POST_NOTIFICATIONS,
        ) == PackageManager.PERMISSION_GRANTED
        if (!granted) return

        val builder = NotificationCompat.Builder(context, NotificationSchedulerImpl.CHANNEL_ID)
            .setSmallIcon(android.R.drawable.ic_popup_reminder)
            .setContentTitle(intent.getStringExtra(EXTRA_TITLE).orEmpty())
            .setContentText(intent.getStringExtra(EXTRA_BODY).orEmpty())
            .setAutoCancel(true)
            .setCategory(NotificationCompat.CATEGORY_REMINDER)
        if (intent.getBooleanExtra(EXTRA_TIME_SENSITIVE, false)) {
            builder.priority = NotificationCompat.PRIORITY_HIGH
        }
        intent.getStringExtra(EXTRA_THREAD)?.let { builder.setGroup(it) }

        NotificationManagerCompat.from(context).notify(id.hashCode(), builder.build())
    }

    companion object {
        const val ACTION_DELIVER = "org.grovealliance.scheduler.action.DELIVER_NOTIFICATION"
        const val EXTRA_ID = "id"
        const val EXTRA_TITLE = "title"
        const val EXTRA_BODY = "body"
        const val EXTRA_THREAD = "thread"
        const val EXTRA_TIME_SENSITIVE = "timeSensitive"
    }
}
