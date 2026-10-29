//
// This source file is part of the My Heart Counts Android open-source project
//
// SPDX-FileCopyrightText: 2026 Stanford University and the project authors (see CONTRIBUTORS.md)
//
// SPDX-License-Identifier: MIT

package edu.stanford.myheartcounts.dashboard.ui

import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Assignment
import androidx.compose.material.icons.automirrored.filled.DirectionsWalk
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.MilitaryTech
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.dp
import org.grovealliance.testing.screenshot.ScreenshotTest
import org.grovealliance.ui.ImageResource
import org.grovealliance.ui.StringResource
import org.grovealliance.ui.theme.ColorToken
import org.grovealliance.ui.theme.Colors
import org.grovealliance.ui.theme.Spacings
import org.junit.Test

class CounterCardScreenshotTest : ScreenshotTest() {

    private val base = CounterCard(
        value = StringResource("6"),
        unit = StringResource("days"),
        badge = ProgressBadge(
            icon = ImageResource.Vector(
                image = Icons.Default.MilitaryTech,
                tint = { Colors.onSurface },
            ),
            label = StringResource("5 / 20"),
            progress = 0.25f,
            progressColor = ColorToken.Primary,
        ),
        detail = LabeledValue(
            label = StringResource("Label"),
            value = StringResource("20 October 2026"),
        ),
        trailingRow = ProgressRingRow(
            ring = ProgressRing(
                progress = 0f,
                progressColor = DashboardColorPalette.green,
                icon = ImageResource.Vector(
                    image = Icons.Default.CalendarMonth,
                    tint = { Colors.onSurface },
                ),
            ),
            title = StringResource("Next title"),
            subtitle = StringResource("Next subtitle"),
            trailingBadge = null,
        ),
        list = ProgressRingRowList(
            label = StringResource("List label"),
            rows = listOf(
                ProgressRingRow(
                    ring = ProgressRing(
                        progress = 0.7f,
                        progressColor = DashboardColorPalette.green,
                        icon = ImageResource.Vector(
                            image = Icons.AutoMirrored.Filled.DirectionsWalk,
                            tint = { Colors.onSurface },
                        ),
                    ),
                    title = StringResource("Title"),
                    subtitle = StringResource("A subtitle describing the goal"),
                    trailingBadge = null,
                ),
                ProgressRingRow(
                    ring = ProgressRing(
                        progress = 0f,
                        progressColor = DashboardColorPalette.green,
                        icon = ImageResource.Vector(
                            image = Icons.AutoMirrored.Filled.Assignment,
                            tint = { Colors.onSurface },
                        ),
                    ),
                    title = StringResource("Another title"),
                    subtitle = StringResource("Another subtitle"),
                    trailingBadge = null,
                ),
            ),
        ),
        onClick = {},
    )

    @Test
    fun `CounterCard screenshot`() {
        screenshot(card = base)
    }

    @Test
    fun `CounterCard without unit screenshot`() {
        screenshot(card = base.copy(unit = null))
    }

    @Test
    fun `CounterCard without badge screenshot`() {
        screenshot(card = base.copy(badge = null))
    }

    @Test
    fun `CounterCard without detail screenshot`() {
        screenshot(card = base.copy(detail = null))
    }

    @Test
    fun `CounterCard without trailing row screenshot`() {
        screenshot(card = base.copy(trailingRow = null))
    }

    @Test
    fun `CounterCard without list screenshot`() {
        screenshot(card = base.copy(list = null))
    }

    @Test
    fun `CounterCard large counter screenshot`() {
        screenshot(card = base.copy(value = StringResource("1,234")))
    }

    @Test
    fun `CounterCard long achievement with badge screenshot`() {
        screenshot(card = participation())
    }

    @Test
    fun `CounterCard achievement without badge and large font screenshot`() {
        val sample = participation()
        val card = sample.copy(trailingRow = sample.trailingRow?.copy(trailingBadge = null))
        screenshot {
            val density = LocalDensity.current
            CompositionLocalProvider(LocalDensity provides Density(density = density.density, fontScale = 1.3f)) {
                card.Content(modifier = Modifier.width(320.dp).padding(Spacings.medium))
            }
        }
    }

    private fun participation(): CounterCard = base.copy(
        value = StringResource("14"),
        detail = base.detail?.copy(label = StringResource("Enrolled since"), value = StringResource("12 October 2026")),
        trailingRow = base.trailingRow?.copy(
            title = StringResource("A week of consistency"),
            subtitle = StringResource("Completed all seven daily check-ins"),
            trailingBadge = TextBadge(text = StringResource("Complete"), color = DashboardColorPalette.green),
        ),
    )

    private fun screenshot(card: CounterCard) {
        screenshot {
            card.Content(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(Spacings.medium),
            )
        }
    }
}
