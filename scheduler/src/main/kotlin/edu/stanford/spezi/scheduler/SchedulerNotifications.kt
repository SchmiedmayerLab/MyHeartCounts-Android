//
// This source file is part of the My Heart Counts Android open-source project
//
// SPDX-FileCopyrightText: 2026 Stanford University and the project authors (see CONTRIBUTORS.md)
//
// SPDX-License-Identifier: MIT

package edu.stanford.spezi.scheduler

import edu.stanford.spezi.core.Module

/**
 * Automatically delivers local notifications for the upcoming events of tasks that opt in via
 * [Task.scheduleNotifications].
 *
 * The module keeps the pending notifications in sync with the task store on its own. Call
 * [scheduleNotificationsRefresh] to force a refresh — for example after the app returns to the
 * foreground or the user grants the notification permission.
 *
 * Registered by passing a [SchedulerNotificationsConfiguration] to `scheduler(notifications = …)`.
 */
interface SchedulerNotifications : Module {
    /**
     * Recomputes and re-arms the pending notifications from the current task store.
     */
    fun scheduleNotificationsRefresh()
}
