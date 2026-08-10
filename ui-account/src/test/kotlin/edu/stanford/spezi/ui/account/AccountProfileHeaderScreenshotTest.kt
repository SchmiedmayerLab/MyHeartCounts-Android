//
// This source file is part of the My Heart Counts Android open-source project
//
// SPDX-FileCopyrightText: 2026 Stanford University and the project authors (see CONTRIBUTORS.md)
//
// SPDX-License-Identifier: MIT

package edu.stanford.spezi.ui.account

import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.ui.Modifier
import edu.stanford.spezi.testing.screenshot.ScreenshotTest
import org.junit.Test

class AccountProfileHeaderScreenshotTest : ScreenshotTest() {

    @Test
    fun `AccountProfileHeader screenshot`() {
        val header = AccountProfileHeader(
            initials = "LS",
            name = "Leland Stanford",
            description = "lelandstanford@stanford.edu"
        )

        screenshot {
            header.Content(modifier = Modifier.fillMaxWidth())
        }
    }
}
