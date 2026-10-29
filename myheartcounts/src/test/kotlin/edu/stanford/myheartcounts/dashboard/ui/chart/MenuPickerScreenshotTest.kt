//
// This source file is part of the My Heart Counts Android open-source project
//
// SPDX-FileCopyrightText: 2026 Stanford University and the project authors (see CONTRIBUTORS.md)
//
// SPDX-License-Identifier: MIT

package edu.stanford.myheartcounts.dashboard.ui.chart

import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import org.grovealliance.testing.screenshot.ScreenshotTest
import org.grovealliance.ui.StringResource
import org.grovealliance.ui.theme.Spacings
import org.junit.Test

class MenuPickerScreenshotTest : ScreenshotTest() {
    private val base = MenuPicker(
        label = StringResource("Time range"),
        options = listOf(
            StringResource("Last 7 days"), StringResource("Last 14 days"), StringResource("Last month"),
            StringResource("Last 3 months"), StringResource("Last 6 months"), StringResource("Last 12 months"),
        ),
        selectedIndex = 1,
        onSelected = {},
    )

    @Test
    fun `MenuPicker screenshot`() {
        screenshot { base.Content(modifier = Modifier.padding(Spacings.medium)) }
    }

    @Test
    fun `MenuPicker narrow screenshot`() {
        screenshot {
            base.copy(selectedIndex = 5).Content(modifier = Modifier.padding(Spacings.medium).width(100.dp))
        }
    }
}
