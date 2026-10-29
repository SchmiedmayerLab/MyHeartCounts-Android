//
// This source file is part of the My Heart Counts Android open-source project
//
// SPDX-FileCopyrightText: 2026 Stanford University and the project authors (see CONTRIBUTORS.md)
//
// SPDX-License-Identifier: MIT

package edu.stanford.myheartcounts.dashboard.ui

import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import org.grovealliance.testing.screenshot.ScreenshotTest
import org.grovealliance.ui.StringResource
import org.grovealliance.ui.theme.Spacings
import org.junit.Test

class ColorBandListScreenshotTest : ScreenshotTest() {

    private val base = ColorBandList(
        bands = listOf(
            ColorBandList.Band(
                leadingText = StringResource("7 to 9 hours"),
                trailingText = StringResource("100"),
                fillColors = listOf(DashboardColorPalette.green),
                isHighlighted = true,
            ),
            ColorBandList.Band(
                leadingText = StringResource("9 to 10 hours"),
                trailingText = StringResource("90"),
                fillColors = listOf(DashboardColorPalette.redToGreen.colorAt(fraction = 0.9f)),
                isHighlighted = false,
            ),
            ColorBandList.Band(
                leadingText = StringResource("6 to 7 hours"),
                trailingText = StringResource("70"),
                fillColors = listOf(DashboardColorPalette.redToGreen.colorAt(fraction = 0.7f)),
                isHighlighted = false,
            ),
            ColorBandList.Band(
                leadingText = StringResource("4 to 5 hours"),
                trailingText = StringResource("20"),
                fillColors = listOf(DashboardColorPalette.redToGreen.colorAt(fraction = 0.2f)),
                isHighlighted = false,
            ),
        ),
    )

    @Test
    fun `ColorBandList screenshot`() {
        screenshot(list = base)
    }

    @Test
    fun `ColorBandList highlighting a middle band screenshot`() {
        screenshot(list = base.copy(bands = base.bands.mapIndexed { index, band -> band.copy(isHighlighted = index == 2) }))
    }

    @Test
    fun `ColorBandList without highlight screenshot`() {
        screenshot(list = base.copy(bands = base.bands.map { it.copy(isHighlighted = false) }))
    }

    @Test
    fun `ColorBandList without trailing text screenshot`() {
        screenshot(list = base.copy(bands = base.bands.map { it.copy(trailingText = null) }))
    }

    @Test
    fun `ColorBandList without leading text screenshot`() {
        screenshot(list = base.copy(bands = base.bands.map { it.copy(leadingText = null) }))
    }

    @Test
    fun `ColorBandList long leading text screenshot`() {
        screenshot(
            list = base.copy(
                bands = base.bands.mapIndexed { index, band ->
                    if (index == 1) {
                        band.copy(leadingText = StringResource("A label long enough to wrap onto a second line of the band"))
                    } else {
                        band
                    }
                },
            ),
        )
    }

    @Test
    fun `ColorBandList single band screenshot`() {
        screenshot(list = base.copy(bands = base.bands.take(1)))
    }

    @Test
    fun `ColorBandList gradient band screenshot`() {
        screenshot(
            list = base.copy(
                bands = listOf(
                    base.bands.first().copy(
                        leadingText = StringResource("0"),
                        fillColors = DashboardColorPalette.redToGreen,
                        isHighlighted = false,
                    ),
                ),
            ),
        )
    }

    private fun screenshot(list: ColorBandList) {
        screenshot {
            list.Content(
                modifier = Modifier
                    .padding(Spacings.medium)
                    .width(LIST_WIDTH),
            )
        }
    }

    private companion object {
        val LIST_WIDTH = 360.dp
    }
}
