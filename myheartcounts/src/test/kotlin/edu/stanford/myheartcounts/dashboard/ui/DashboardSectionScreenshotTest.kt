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
import org.grovealliance.ui.theme.Spacings
import org.junit.Test

class DashboardSectionScreenshotTest : ScreenshotTest() {

    private val base = DashboardSection(
        title = StringResource("Title"),
        content = ColorBandList(
            bands = listOf(
                ColorBandList.Band(
                    leadingText = StringResource("7 to 9 hours"),
                    trailingText = StringResource("100"),
                    fillColors = listOf(DashboardColorPalette.green),
                    isHighlighted = true,
                ),
                ColorBandList.Band(
                    leadingText = StringResource("6 to 7 hours"),
                    trailingText = StringResource("70"),
                    fillColors = listOf(DashboardColorPalette.redToGreen.colorAt(fraction = 0.7f)),
                    isHighlighted = false,
                ),
            ),
        ),
        footnote = StringResource("A footnote explaining the section, long enough to wrap onto a second line."),
    )

    @Test
    fun `DashboardSection screenshot`() {
        screenshot(section = base)
    }

    @Test
    fun `DashboardSection without title screenshot`() {
        screenshot(section = base.copy(title = null))
    }

    @Test
    fun `DashboardSection without footnote screenshot`() {
        screenshot(section = base.copy(footnote = null))
    }

    @Test
    fun `DashboardSection content only screenshot`() {
        screenshot(
            section = base.copy(
                title = null,
                footnote = null,
            ),
        )
    }

    @Test
    fun `DashboardSection with gauge card screenshot`() {
        screenshot(
            section = base.copy(
                title = null,
                content = GaugeCard(
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
                ),
            ),
        )
    }

    private fun screenshot(section: DashboardSection) {
        screenshot {
            section.Content(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(Spacings.medium),
            )
        }
    }
}
