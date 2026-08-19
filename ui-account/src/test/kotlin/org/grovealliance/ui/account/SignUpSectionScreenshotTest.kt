//
// This source file is part of the My Heart Counts Android open-source project
//
// SPDX-FileCopyrightText: 2026 Stanford University and the project authors (see CONTRIBUTORS.md)
//
// SPDX-License-Identifier: MIT

package org.grovealliance.ui.account

import androidx.compose.foundation.layout.padding
import androidx.compose.ui.Modifier
import org.grovealliance.testing.screenshot.ScreenshotTest
import org.grovealliance.ui.StringResource
import org.grovealliance.ui.theme.Spacings
import org.junit.Test

class SignUpSectionScreenshotTest : ScreenshotTest() {

    @Test
    fun `SignUpSection screenshot`() {
        val section = SignUpSection(
            title = StringResource("PERSONAL DETAILS"),
            entries = listOf(
                SignUpFormEntry(
                    title = StringResource("First name"),
                    entry = StringDataEntry(placeholder = StringResource("Enter your first name")),
                    value = "John",
                    onValueChange = {}
                ),
                SignUpFormEntry(
                    title = StringResource("Last name"),
                    entry = StringDataEntry(
                        placeholder = StringResource("Enter your last name"),
                        hideContent = false,
                    ),
                    value = "",
                    onValueChange = {}
                ),
                SignUpFormEntry(
                    title = StringResource("Data collection"),
                    entry = BooleanDataEntry(description = StringResource("Allow data collection")),
                    value = true,
                    onValueChange = {}
                ),
            )
        )

        screenshot {
            section.Content(modifier = Modifier.padding(Spacings.small))
        }
    }
}
