//
// This source file is part of the My Heart Counts Android open-source project
//
// SPDX-FileCopyrightText: 2026 Stanford University and the project authors (see CONTRIBUTORS.md)
//
// SPDX-License-Identifier: MIT

package edu.stanford.myheartcounts.workoutpreference.ui

import edu.stanford.myheartcounts.MHCStrings
import org.grovealliance.testing.screenshot.ScreenshotTest
import org.grovealliance.ui.StringResource
import org.junit.Test

class NotificationTimePickerScreenshotTest : ScreenshotTest() {
    private val base = NotificationTimePicker(
        selection = NotificationTimePicker.State(
            hour = 9,
            minute = 0,
        ),
        confirmTitle = StringResource(MHCStrings.workout_preference_time_confirm),
        dismissTitle = StringResource(MHCStrings.workout_preference_time_cancel),
        onSelect = {},
        onDismiss = {},
    )

    @Test
    fun `NotificationTimePicker screenshot`() {
        // when / then
        screenshot {
            base.Content()
        }
    }
}
