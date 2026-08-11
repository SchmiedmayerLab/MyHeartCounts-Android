//
// This source file is part of the My Heart Counts Android open-source project
//
// SPDX-FileCopyrightText: 2026 Stanford University and the project authors (see CONTRIBUTORS.md)
//
// SPDX-License-Identifier: MIT

package edu.stanford.myheartcounts.notification

import android.content.Context
import android.content.pm.PackageManager
import androidx.core.content.ContextCompat

/**
 * Reports whether runtime permissions are currently granted to the app.
 */
interface PermissionChecker {
    /**
     * Returns `true` if the app currently holds the given [permission].
     */
    fun isPermissionGranted(permission: String): Boolean
}

/**
 * Default [PermissionChecker] implementation.
 */
class PermissionCheckerImpl(
    private val context: Context,
) : PermissionChecker {
    override fun isPermissionGranted(permission: String): Boolean =
        ContextCompat.checkSelfPermission(
            context,
            permission,
        ) == PackageManager.PERMISSION_GRANTED
}
