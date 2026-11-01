//
// This source file is part of the My Heart Counts Android open-source project
//
// SPDX-FileCopyrightText: 2026 Stanford University and the project authors (see CONTRIBUTORS.md)
//
// SPDX-License-Identifier: MIT

package edu.stanford.myheartcounts.di

import edu.stanford.myheartcounts.notification.NotificationPermissionHandler
import edu.stanford.myheartcounts.notification.NotificationPermissionHandlerImpl
import edu.stanford.myheartcounts.notification.PermissionChecker
import edu.stanford.myheartcounts.notification.PermissionCheckerImpl
import org.grovealliance.core.ConfigurationBuilder

/**
 * Registers notification permission handling.
 */
fun ConfigurationBuilder.permissionConfigurations() {
    singleton<PermissionChecker> {
        PermissionCheckerImpl(
            context = appContext(),
        )
    }
    singleton<NotificationPermissionHandler> {
        NotificationPermissionHandlerImpl(
            permissionChecker = dependency(),
            storage = dependency(),
        )
    }
}
