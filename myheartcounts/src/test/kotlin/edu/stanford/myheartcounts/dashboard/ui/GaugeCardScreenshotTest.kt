//
// This source file is part of the My Heart Counts Android open-source project
//
// SPDX-FileCopyrightText: 2026 Stanford University and the project authors (see CONTRIBUTORS.md)
//
// SPDX-License-Identifier: MIT

package edu.stanford.myheartcounts.dashboard.ui

import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.ui.Modifier
import org.grovealliance.testing.screenshot.ScreenshotTest
import org.grovealliance.ui.StringResource
import org.grovealliance.ui.theme.ColorToken
import org.grovealliance.ui.theme.Spacings
import org.junit.Test

class GaugeCardScreenshotTest : ScreenshotTest() {

    private val base = GaugeCard(
        gauge = Gauge(
            progress = 0.7f,
            colors = DashboardColorPalette.redToGreen,
            lineWidth = Gauge.defaultLineWidth * 2,
            size = Gauge.Size.Large,
            label = Gauge.Label(
                value = StringResource("70"),
                caption = Gauge.Caption.Range(
                    minimum = StringResource("0"),
                    maximum = StringResource("100"),
                ),
            ),
        ),
    )

    @Test
    fun `GaugeCard screenshot`() {
        screenshot(card = base)
    }

    @Test
    fun `GaugeCard without value screenshot`() {
        screenshot(
            card = base.copy(
                gauge = base.gauge.copy(
                    progress = null,
                    colors = listOf(ColorToken.Outline),
                    label = base.gauge.label?.copy(value = StringResource("—")),
                ),
            ),
        )
    }

    @Test
    fun `GaugeCard regular gauge screenshot`() {
        screenshot(
            card = base.copy(
                gauge = base.gauge.copy(
                    lineWidth = Gauge.defaultLineWidth * 1.5f,
                    size = Gauge.Size.Regular,
                    label = base.gauge.label?.copy(
                        caption = Gauge.Caption.Description(text = StringResource("7:36")),
                    ),
                ),
            ),
        )
    }

    private fun screenshot(card: GaugeCard) {
        screenshot {
            card.Content(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(Spacings.medium),
            )
        }
    }
}
