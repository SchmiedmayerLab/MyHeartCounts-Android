//
// This source file is part of the My Heart Counts Android open-source project
//
// SPDX-FileCopyrightText: 2026 Stanford University and the project authors (see CONTRIBUTORS.md)
//
// SPDX-License-Identifier: MIT

package edu.stanford.myheartcounts.account.overview

import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.ui.Modifier
import org.grovealliance.testing.screenshot.ScreenshotTest
import org.grovealliance.ui.theme.Spacings
import org.junit.Test

class AppInfoSectionScreenshotTest : ScreenshotTest() {

    @Test
    fun `AppInfoSection screenshot`() {
        val section = AppInfoSection(
            version = "1.2.0",
            onPrivacyPolicySelected = {},
        )

        screenshot {
            section.Content(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(Spacings.medium),
            )
        }
    }
}
