//
// This source file is part of the My Heart Counts Android open-source project
//
// SPDX-FileCopyrightText: 2026 Stanford University and the project authors (see CONTRIBUTORS.md)
//
// SPDX-License-Identifier: MIT

package edu.stanford.myheartcounts.dashboard.ui

import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.DirectionsWalk
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.Hotel
import androidx.compose.material.icons.filled.LocalFireDepartment
import androidx.compose.ui.Modifier
import org.grovealliance.testing.screenshot.ScreenshotTest
import org.grovealliance.ui.ImageResource
import org.grovealliance.ui.StringResource
import org.grovealliance.ui.theme.Spacings
import org.grovealliance.ui.theme.resolve
import org.junit.Test

class IconTextCardScreenshotTest : ScreenshotTest() {

    private val base = IconTextCard(
        rows = listOf(
            IconTextCardRow(
                icon = ImageResource.Vector(
                    image = Icons.AutoMirrored.Filled.DirectionsWalk,
                    tint = { DashboardColorPalette.green.resolve() },
                ),
                text = StringResource("Your 26,635 steps cover about 26 km — that's 9.6 lengths of a long bridge."),
            ),
            IconTextCardRow(
                icon = ImageResource.Vector(
                    image = Icons.Default.Favorite,
                    tint = { DashboardColorPalette.red.resolve() },
                ),
                text = StringResource("Your heart has beaten about 681K times since you started."),
            ),
            IconTextCardRow(
                icon = ImageResource.Vector(
                    image = Icons.Default.LocalFireDepartment,
                    tint = { DashboardColorPalette.orange.resolve() },
                ),
                text = StringResource("You've burned 2,053 active calories — the equivalent of 7 slices of pizza."),
            ),
            IconTextCardRow(
                icon = ImageResource.Vector(
                    image = Icons.Default.Hotel,
                    tint = { DashboardColorPalette.indigo.resolve() },
                ),
                text = StringResource("A short text"),
            ),
        ),
    )

    @Test
    fun `IconTextCard screenshot`() {
        screenshot(card = base)
    }

    @Test
    fun `IconTextCard single row screenshot`() {
        screenshot(card = base.copy(rows = base.rows.take(1)))
    }

    private fun screenshot(card: IconTextCard) {
        screenshot {
            card.Content(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(Spacings.medium),
            )
        }
    }
}
