//
// This source file is part of the My Heart Counts Android open-source project
//
// SPDX-FileCopyrightText: 2026 Stanford University and the project authors (see CONTRIBUTORS.md)
//
// SPDX-License-Identifier: MIT

package edu.stanford.myheartcounts.notification

import com.google.common.truth.Truth.assertThat
import org.junit.Test

class MHCNudgeNotificationsTest {

    @Test
    fun `opens with the message id and the message data`() {
        // when
        val extras = MHCNudgeNotifications.openExtras(
            messageId = "0:1234",
            data = mapOf("notificationId" to "backlog-item"),
        )

        // then
        assertThat(extras).containsExactly(
            MHCNudgeNotifications.FCM_MESSAGE_ID_EXTRA,
            "0:1234",
            "notificationId",
            "backlog-item",
        )
    }

    @Test
    fun `opens without tracking when the message has no id`() {
        // when
        val extras = MHCNudgeNotifications.openExtras(
            messageId = null,
            data = mapOf("notificationId" to "backlog-item"),
        )

        // then
        assertThat(extras).doesNotContainKey(MHCNudgeNotifications.FCM_MESSAGE_ID_EXTRA)
    }
}
