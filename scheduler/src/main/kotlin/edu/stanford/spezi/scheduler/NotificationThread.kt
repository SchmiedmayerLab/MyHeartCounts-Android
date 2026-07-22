//
// This source file is part of the My Heart Counts open-source project
//
// SPDX-FileCopyrightText: 2026 Stanford University and the project authors (see CONTRIBUTORS.md)
//
// SPDX-License-Identifier: MIT

package edu.stanford.spezi.scheduler

/**
 * How a task's event notifications are grouped in the notification shade.
 */
sealed interface NotificationThread {
    /**
     * Grouping is left to the platform; no explicit group is set.
     */
    data object None : NotificationThread

    /**
     * All scheduler notifications share a single group.
     */
    data object Global : NotificationThread

    /**
     * Notifications are grouped per task.
     */
    data object PerTask : NotificationThread

    /**
     * Notifications are grouped under the caller-specified [id].
     */
    data class Custom(val id: String) : NotificationThread
}
