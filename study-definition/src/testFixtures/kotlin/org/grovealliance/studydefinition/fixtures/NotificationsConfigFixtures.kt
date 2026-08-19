//
// This source file is part of the My Heart Counts Android open-source project
//
// SPDX-FileCopyrightText: 2026 Stanford University and the project authors (see CONTRIBUTORS.md)
//
// SPDX-License-Identifier: MIT

package org.grovealliance.studydefinition.fixtures

import org.grovealliance.scheduler.NotificationThread
import org.grovealliance.studydefinition.NotificationsConfig
import org.grovealliance.studydefinition.Time

/**
 * Fixtures for [NotificationsConfig]. [create] returns [NotificationsConfig.Disabled].
 */
object NotificationsConfigFixtures {
    fun create(): NotificationsConfig = NotificationsConfig.Disabled

    fun createEnabled(
        thread: NotificationThread = NotificationThread.None,
        time: Time? = null,
    ): NotificationsConfig.Enabled = NotificationsConfig.Enabled(
        thread = thread,
        time = time,
    )
}
