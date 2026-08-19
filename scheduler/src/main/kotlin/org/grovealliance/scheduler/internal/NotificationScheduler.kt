//
// This source file is part of the My Heart Counts Android open-source project
//
// SPDX-FileCopyrightText: 2026 Stanford University and the project authors (see CONTRIBUTORS.md)
//
// SPDX-License-Identifier: MIT

package org.grovealliance.scheduler.internal

import android.app.AlarmManager
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import androidx.core.net.toUri
import org.grovealliance.core.logging.groveLogger
import org.grovealliance.core.time.TimeProvider
import org.grovealliance.storage.local.KeyValueStorageFactory
import org.grovealliance.storage.local.KeyValueStorageType
import org.grovealliance.storage.local.getSerializableList
import org.grovealliance.storage.local.putSerializable

/**
 * Arms and cancels the platform alarms that deliver task notifications.
 */
internal interface NotificationScheduler {
    /**
     * Replaces all pending scheduler notifications with [notifications].
     */
    fun schedule(notifications: List<PlannedNotification>)

    /**
     * Cancels every pending scheduler notification.
     */
    fun cancelAll()

    /**
     * Re-arms the persisted pending notifications whose delivery time is still in the future. Used to
     * restore alarms after a device reboot, which clears them.
     */
    fun reArm()
}

/**
 * [NotificationScheduler] backed by [AlarmManager]: each request is a one-shot exact alarm that wakes
 * [SchedulerNotificationReceiver] to post the notification. The pending set is persisted so it can be
 * re-armed after a reboot.
 */
internal class NotificationSchedulerImpl(
    private val context: Context,
    private val timeProvider: TimeProvider,
    storageFactory: KeyValueStorageFactory,
) : NotificationScheduler {

    private val storage = storageFactory.create(
        fileName = STORAGE_FILE,
        type = KeyValueStorageType.UNENCRYPTED,
    )
    private val logger by groveLogger()
    private val alarmManager = context.getSystemService(AlarmManager::class.java)

    override fun schedule(notifications: List<PlannedNotification>) {
        cancelAll()
        ensureChannel()
        notifications.forEach(::arm)
        storage.putSerializable(KEY_SNAPSHOT, notifications)
    }

    override fun cancelAll() {
        loadSnapshot().forEach(::cancel)
        storage.delete(KEY_SNAPSHOT)
    }

    override fun reArm() {
        ensureChannel()
        val now = timeProvider.nowInstant().toEpochMilli()
        loadSnapshot().filter { it.fireTimeMillis > now }.forEach(::arm)
    }

    private fun arm(notification: PlannedNotification) {
        val pendingIntent = deliverPendingIntent(notification, PendingIntent.FLAG_UPDATE_CURRENT) ?: return
        val exact = Build.VERSION.SDK_INT < Build.VERSION_CODES.S || alarmManager?.canScheduleExactAlarms() == true
        try {
            if (exact) {
                alarmManager?.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, notification.fireTimeMillis, pendingIntent)
            } else {
                alarmManager?.setAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, notification.fireTimeMillis, pendingIntent)
            }
        } catch (error: SecurityException) {
            logger.e(error) { "Missing exact-alarm permission; falling back to an inexact alarm." }
            alarmManager?.setAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, notification.fireTimeMillis, pendingIntent)
        }
    }

    private fun cancel(notification: PlannedNotification) {
        deliverPendingIntent(notification, PendingIntent.FLAG_NO_CREATE)?.let {
            alarmManager?.cancel(it)
            it.cancel()
        }
    }

    private fun deliverPendingIntent(notification: PlannedNotification, extraFlags: Int): PendingIntent? {
        val intent = Intent(context, SchedulerNotificationReceiver::class.java).apply {
            action = SchedulerNotificationReceiver.ACTION_DELIVER
            data = "$URI_SCHEME://notification/${notification.id}".toUri()
            putExtra(SchedulerNotificationReceiver.EXTRA_ID, notification.id)
            putExtra(SchedulerNotificationReceiver.EXTRA_TITLE, notification.title)
            putExtra(SchedulerNotificationReceiver.EXTRA_BODY, notification.body)
            putExtra(SchedulerNotificationReceiver.EXTRA_THREAD, notification.threadId)
            putExtra(SchedulerNotificationReceiver.EXTRA_TIME_SENSITIVE, notification.timeSensitive)
        }
        return PendingIntent.getBroadcast(
            context,
            notification.id.hashCode(),
            intent,
            PendingIntent.FLAG_IMMUTABLE or extraFlags,
        )
    }

    private fun loadSnapshot(): List<PlannedNotification> = storage.getSerializableList(KEY_SNAPSHOT)

    private fun ensureChannel() {
        val manager = context.getSystemService(NotificationManager::class.java) ?: return
        if (manager.getNotificationChannel(CHANNEL_ID) != null) return
        manager.createNotificationChannel(
            NotificationChannel(CHANNEL_ID, CHANNEL_NAME, NotificationManager.IMPORTANCE_HIGH),
        )
    }

    companion object {
        const val CHANNEL_ID = "org.grovealliance.scheduler.tasks"
        const val STORAGE_FILE = "org.grovealliance.scheduler.notifications"
        private const val CHANNEL_NAME = "Task reminders"
        private const val KEY_SNAPSHOT = "pending"
        private const val URI_SCHEME = "grove-scheduler"
    }
}
