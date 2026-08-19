//
// This source file is part of the My Heart Counts Android open-source project
//
// SPDX-FileCopyrightText: 2026 Stanford University and the project authors (see CONTRIBUTORS.md)
//
// SPDX-License-Identifier: MIT

package org.grovealliance.ui

import androidx.activity.compose.LocalActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.core.app.ActivityCompat

interface PermissionRequester {
    fun request(permission: String, onResult: (PermissionResult) -> Unit)
}

/**
 * Represents the result of a permission request.
 */
sealed interface PermissionResult {
    /**
     * The permission that was requested.
     */
    val permission: String

    /**
     * Indicates that the permission was granted.
     */
    data class Granted(override val permission: String) : PermissionResult

    /**
     * Indicates that the permission was denied.
     *
     * @param shouldShowRationale Indicates whether the user should be shown a rationale for why the permission is needed.
     */
    data class Denied(override val permission: String, val shouldShowRationale: Boolean) : PermissionResult
}

@Composable
fun rememberPermissionRequester(): PermissionRequester {
    val activity = LocalActivity.current

    var currentPermission by remember { mutableStateOf<String?>(null) }
    var currentOnResult by remember {
        mutableStateOf<(PermissionResult) -> Unit>({})
    }

    val launcher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { granted ->
        val permission = currentPermission ?: return@rememberLauncherForActivityResult
        val result = if (granted) {
            PermissionResult.Granted(permission)
        } else {
            val shouldShowRationale = activity?.let {
                ActivityCompat.shouldShowRequestPermissionRationale(it, permission)
            } ?: false
            PermissionResult.Denied(
                permission = permission,
                shouldShowRationale = shouldShowRationale,
            )
        }
        currentOnResult(result)
    }

    return remember {
        object : PermissionRequester {
            override fun request(
                permission: String,
                onResult: (PermissionResult) -> Unit,
            ) {
                currentPermission = permission
                currentOnResult = onResult
                launcher.launch(permission)
            }
        }
    }
}
