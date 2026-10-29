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

class GaugeHeaderScreenshotTest : ScreenshotTest() {

    private val base = GaugeHeader(
        label = StringResource("Label"),
        value = GaugeHeader.Value.Measured(
            parts = listOf(
                GaugeHeader.Part(
                    text = StringResource("8"),
                    unit = StringResource("hr"),
                ),
                GaugeHeader.Part(
                    text = StringResource("45"),
                    unit = StringResource("min"),
                ),
            ),
        ),
        footnote = StringResource("7:36"),
        gauge = Gauge(
            progress = 1f,
            colors = DashboardColorPalette.redToGreen,
            lineWidth = Gauge.defaultLineWidth * 1.5f,
            size = Gauge.Size.Regular,
            label = Gauge.Label(
                value = StringResource("100"),
                caption = null,
            ),
        ),
    )

    @Test
    fun `GaugeHeader screenshot`() {
        screenshot(header = base)
    }

    @Test
    fun `GaugeHeader single part screenshot`() {
        screenshot(
            header = base.copy(
                value = GaugeHeader.Value.Measured(
                    parts = listOf(
                        GaugeHeader.Part(
                            text = StringResource("120/80"),
                            unit = StringResource("mmHg"),
                        ),
                    ),
                ),
            ),
        )
    }

    @Test
    fun `GaugeHeader part without unit screenshot`() {
        screenshot(
            header = base.copy(
                value = GaugeHeader.Value.Measured(
                    parts = listOf(
                        GaugeHeader.Part(
                            text = StringResource("8,412"),
                            unit = null,
                        ),
                    ),
                ),
            ),
        )
    }

    @Test
    fun `GaugeHeader without footnote screenshot`() {
        screenshot(header = base.copy(footnote = null))
    }

    @Test
    fun `GaugeHeader missing value screenshot`() {
        screenshot(
            header = base.copy(
                value = GaugeHeader.Value.Missing(text = StringResource("No Data")),
                footnote = null,
                gauge = base.gauge.copy(
                    progress = null,
                    colors = listOf(ColorToken.Outline),
                    label = base.gauge.label?.copy(value = StringResource("—")),
                ),
            ),
        )
    }

    @Test
    fun `GaugeHeader long label screenshot`() {
        screenshot(header = base.copy(label = StringResource("A label long enough to wrap onto a second line")))
    }

    private fun screenshot(header: GaugeHeader) {
        screenshot {
            header.Content(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(Spacings.medium),
            )
        }
    }
}
