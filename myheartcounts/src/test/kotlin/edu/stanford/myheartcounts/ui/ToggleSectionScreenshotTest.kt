//
// This source file is part of the My Heart Counts Android open-source project
//
// SPDX-FileCopyrightText: 2026 Stanford University and the project authors (see CONTRIBUTORS.md)
//
// SPDX-License-Identifier: MIT

package edu.stanford.myheartcounts.ui

import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.ui.Modifier
import kotlinx.coroutines.flow.MutableStateFlow
import org.grovealliance.testing.screenshot.ScreenshotTest
import org.grovealliance.ui.StringResource
import org.grovealliance.ui.theme.Spacings
import org.junit.Test

class ToggleSectionScreenshotTest : ScreenshotTest() {

    @Test
    fun `ToggleSection on screenshot`() {
        screenshot {
            section(isOn = true).Content(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(Spacings.medium),
            )
        }
    }

    @Test
    fun `ToggleSection off screenshot`() {
        screenshot {
            section(isOn = false).Content(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(Spacings.medium),
            )
        }
    }

    @Test
    fun `ToggleSection without subtitle screenshot`() {
        screenshot {
            section(
                isOn = true,
                subtitle = null,
            ).Content(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(Spacings.medium),
            )
        }
    }

    private fun section(
        isOn: Boolean,
        subtitle: StringResource? = StringResource("Receive a summary of your activity every Monday."),
    ) = ToggleSection(
        title = StringResource("Weekly Summary"),
        subtitle = subtitle,
        isOn = MutableStateFlow(value = isOn),
        onToggle = {},
    )
}
