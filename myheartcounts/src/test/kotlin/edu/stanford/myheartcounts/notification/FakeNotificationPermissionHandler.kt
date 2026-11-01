//
// This source file is part of the My Heart Counts Android open-source project
//
// SPDX-FileCopyrightText: 2026 Stanford University and the project authors (see CONTRIBUTORS.md)
//
// SPDX-License-Identifier: MIT

package edu.stanford.myheartcounts.notification

/**
 * A [NotificationPermissionHandler] whose granted state is set directly.
 */
class FakeNotificationPermissionHandler : NotificationPermissionHandler {
    private var isGranted = false
    private var hasRequested = false

    override val permission: String = "android.permission.POST_NOTIFICATIONS"

    /**
     * Sets what [isGranted] returns.
     */
    fun setGranted(granted: Boolean) {
        isGranted = granted
    }

    override fun isGranted(): Boolean = isGranted

    override fun onPermissionRequested() {
        hasRequested = true
    }

    override fun hasRequestedPermissionBefore(): Boolean = hasRequested
}
