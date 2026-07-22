//
// This source file is part of the My Heart Counts open-source project
//
// SPDX-FileCopyrightText: 2026 Stanford University and the project authors (see CONTRIBUTORS.md)
//
// SPDX-License-Identifier: MIT

package edu.stanford.spezi.scheduler.internal

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import edu.stanford.spezi.core.optionalDependency

/**
 * Re-arms the pending task notifications after a reboot, which clears all alarms.
 *
 * The [NotificationScheduler] is resolved optionally: when the app has not enabled scheduler
 * notifications (`scheduler(notifications = …)`) there is nothing to re-arm and this is a no-op.
 */
internal class SchedulerBootReceiver : BroadcastReceiver() {

    private val notificationScheduler: NotificationScheduler? by optionalDependency()

    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action != Intent.ACTION_BOOT_COMPLETED) return
        notificationScheduler?.reArm()
    }
}
