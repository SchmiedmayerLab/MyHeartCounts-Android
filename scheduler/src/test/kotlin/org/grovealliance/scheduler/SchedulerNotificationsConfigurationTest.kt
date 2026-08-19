//
// This source file is part of the My Heart Counts Android open-source project
//
// SPDX-FileCopyrightText: 2026 Stanford University and the project authors (see CONTRIBUTORS.md)
//
// SPDX-License-Identifier: MIT

package org.grovealliance.scheduler

import com.google.common.truth.Truth.assertThat
import org.junit.Test
import java.time.Duration

class SchedulerNotificationsConfigurationTest {

    @Test
    fun `default schedules four weeks ahead at nine in the morning`() {
        // when
        val configuration = SchedulerNotificationsConfiguration.DEFAULT

        // then
        assertThat(configuration.limit).isEqualTo(30)
        assertThat(configuration.window).isEqualTo(Duration.ofDays(28))
        assertThat(configuration.allDayNotificationTime).isEqualTo(NotificationTime(hour = 9))
    }

    @Test
    fun `rejects a non-positive limit`() {
        // when / then
        assertThrows<IllegalArgumentException> {
            SchedulerNotificationsConfiguration(
                limit = 0,
                window = Duration.ofDays(1),
                allDayNotificationTime = NotificationTime(hour = 9),
            )
        }
    }

    @Test
    fun `rejects a non-positive window`() {
        // when / then
        assertThrows<IllegalArgumentException> {
            SchedulerNotificationsConfiguration(
                limit = 1,
                window = Duration.ZERO,
                allDayNotificationTime = NotificationTime(hour = 9),
            )
        }
    }

    private inline fun <reified T : Throwable> assertThrows(block: () -> Unit) {
        val thrown = runCatching(block).exceptionOrNull()
        assertThat(thrown).isInstanceOf(T::class.java)
    }
}
