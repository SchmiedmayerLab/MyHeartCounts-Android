//
// This source file is part of the My Heart Counts Android open-source project
//
// SPDX-FileCopyrightText: 2026 Stanford University and the project authors (see CONTRIBUTORS.md)
//
// SPDX-License-Identifier: MIT

package org.grovealliance.ui.account

import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Lock
import androidx.compose.ui.Modifier
import org.grovealliance.testing.screenshot.ScreenshotTest
import org.grovealliance.ui.AsyncTextButton
import org.grovealliance.ui.ImageResource
import org.grovealliance.ui.StringResource
import org.junit.Test

class AccountChangePasswordLayoutScreenshotTest : ScreenshotTest() {

    @Test
    fun `AccountChangePasswordLayout no error screenshot`() {
        val layout = AccountChangePasswordLayout(
            newPassword = "hunter2",
            confirmPassword = "hunter2",
            onNewPasswordChange = {},
            onConfirmPasswordChange = {},
            validationMessage = null,
            saveButton = AsyncTextButton(title = StringResource("Save"), action = {}),
            icon = ImageResource(Icons.Default.Lock),
        )

        screenshot {
            layout.Content(modifier = Modifier.fillMaxSize())
        }
    }

    @Test
    fun `AccountChangePasswordLayout with validation error screenshot`() {
        val layout = AccountChangePasswordLayout(
            newPassword = "abc",
            confirmPassword = "xyz",
            onNewPasswordChange = {},
            onConfirmPasswordChange = {},
            validationMessage = StringResource("Passwords do not match."),
            saveButton = AsyncTextButton(title = StringResource("Save"), action = {}),
            icon = ImageResource(Icons.Default.Lock),
        )

        screenshot {
            layout.Content(modifier = Modifier.fillMaxSize())
        }
    }
}
