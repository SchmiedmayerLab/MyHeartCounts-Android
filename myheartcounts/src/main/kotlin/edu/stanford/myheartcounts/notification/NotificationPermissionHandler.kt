//
// This source file is part of the My Heart Counts Android open-source project
//
// SPDX-FileCopyrightText: 2026 Stanford University and the project authors (see CONTRIBUTORS.md)
//
// SPDX-License-Identifier: MIT

package edu.stanford.myheartcounts.notification

import android.Manifest
import android.os.Build
import org.grovealliance.storage.local.KeyValueStorage

/**
 * Determines whether the user has granted permission to post notifications.
 * This permission is required for the :scheduler module to
 * deliver task-reminder notifications.
 */
interface NotificationPermissionHandler {
    /**
     * The permission string for posting notifications
     */
    val permission: String

    /**
     * Returns true if the user has granted permission to post notifications, false otherwise
     */
    fun isGranted(): Boolean

    /**
     * Indicates that the user has been asked to grant notification permissions.
     */
    fun onPermissionRequested()

    /**
     * Returns true if the user has been asked to grant notification permissions before, false otherwise.
     */
    fun hasRequestedPermissionBefore(): Boolean
}

/**
 * Default [NotificationPermissionHandler] implementation backed by the application context.
 */
class NotificationPermissionHandlerImpl(
    private val permissionChecker: PermissionChecker,
    private val storage: KeyValueStorage,
) : NotificationPermissionHandler {
    override val permission: String
        get() = if (Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU) "" else Manifest.permission.POST_NOTIFICATIONS

    override fun isGranted(): Boolean {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU) return true
        return permissionChecker.isPermissionGranted(permission)
    }

    override fun onPermissionRequested() {
        storage.putBoolean(KEY_NOTIFICATION_PERMISSION_REQUESTED, true)
    }

    override fun hasRequestedPermissionBefore(): Boolean {
        return storage.getBoolean(KEY_NOTIFICATION_PERMISSION_REQUESTED, false)
    }

    companion object {
        private const val KEY_NOTIFICATION_PERMISSION_REQUESTED = "notification_permission_requested"
    }
}
