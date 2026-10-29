//
// This source file is part of the My Heart Counts Android open-source project
//
// SPDX-FileCopyrightText: 2026 Stanford University and the project authors (see CONTRIBUTORS.md)
//
// SPDX-License-Identifier: MIT

package edu.stanford.myheartcounts.dashboard.ui

import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.LocalFireDepartment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import org.grovealliance.testing.screenshot.ScreenshotTest
import org.grovealliance.ui.ImageResource
import org.grovealliance.ui.StringResource
import org.grovealliance.ui.theme.Spacings
import org.grovealliance.ui.theme.resolve
import org.junit.Test

class IconTextCardRowScreenshotTest : ScreenshotTest() {

    private val base = IconTextCardRow(
        icon = ImageResource.Vector(
            image = Icons.Default.LocalFireDepartment,
            tint = { DashboardColorPalette.orange.resolve() },
        ),
        text = StringResource("You've burned 2,053 active calories — the equivalent of 7 slices of pizza."),
    )

    @Test
    fun `IconTextCardRow screenshot`() {
        screenshot(row = base)
    }

    @Test
    fun `IconTextCardRow single line screenshot`() {
        screenshot(row = base.copy(text = StringResource("A short text")))
    }

    private fun screenshot(row: IconTextCardRow) {
        screenshot {
            row.Content(
                modifier = Modifier
                    .padding(Spacings.medium)
                    .width(ROW_WIDTH),
            )
        }
    }

    private companion object {
        val ROW_WIDTH = 360.dp
    }
}
