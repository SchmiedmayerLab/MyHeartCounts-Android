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
import androidx.compose.ui.Modifier
import org.grovealliance.testing.screenshot.ScreenshotTest
import org.grovealliance.ui.ImageResource
import org.grovealliance.ui.StringResource
import org.grovealliance.ui.theme.Colors
import org.grovealliance.ui.theme.Spacings
import org.junit.Test

class ProgressRingCardScreenshotTest : ScreenshotTest() {

    private val row = ProgressRingRow(
        ring = ProgressRing(
            progress = 1f,
            progressColor = DashboardColorPalette.green,
            icon = ImageResource.Vector(
                image = Icons.AutoMirrored.Filled.DirectionsWalk,
                tint = { Colors.onSurface },
            ),
        ),
        title = StringResource("Title"),
        subtitle = StringResource("A subtitle describing the goal"),
        trailingBadge = null,
    )

    private val base = ProgressRingCard(
        rows = listOf(
            row,
            row.copy(
                ring = row.ring.copy(progress = 0.45f),
                title = StringResource("Another title"),
                trailingBadge = TextBadge(
                    text = StringResource("45%"),
                    color = DashboardColorPalette.green,
                ),
            ),
            row.copy(
                ring = row.ring.copy(
                    progress = 0f,
                    icon = null,
                ),
                title = StringResource("A third title"),
                subtitle = null,
            ),
        ),
    )

    @Test
    fun `ProgressRingCard screenshot`() {
        screenshot(card = base)
    }

    @Test
    fun `ProgressRingCard single row screenshot`() {
        screenshot(card = base.copy(rows = base.rows.take(1)))
    }

    @Test
    fun `ProgressRingCard in a section screenshot`() {
        screenshot(
            card = base,
            title = StringResource("Section title"),
        )
    }

    private fun screenshot(
        card: ProgressRingCard,
        title: StringResource? = null,
    ) {
        screenshot {
            DashboardSection(
                title = title,
                content = card,
                footnote = null,
            ).Content(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(Spacings.medium),
            )
        }
    }
}
