//
// This source file is part of the My Heart Counts Android open-source project
//
// SPDX-FileCopyrightText: 2026 Stanford University and the project authors (see CONTRIBUTORS.md)
//
// SPDX-License-Identifier: MIT

package edu.stanford.myheartcounts.dashboard.ui

import androidx.compose.foundation.layout.padding
import androidx.compose.ui.Modifier
import org.grovealliance.testing.screenshot.ScreenshotTest
import org.grovealliance.ui.StringResource
import org.grovealliance.ui.theme.ColorToken
import org.grovealliance.ui.theme.Spacings
import org.junit.Test

class GaugeScreenshotTest : ScreenshotTest() {

    private val base = Gauge(
        progress = 0.7f,
        colors = DashboardColorPalette.redToGreen,
        lineWidth = Gauge.defaultLineWidth * 1.5f,
        size = Gauge.Size.Regular,
        label = Gauge.Label(
            value = StringResource("70"),
            caption = Gauge.Caption.Description(text = StringResource("7:36")),
        ),
    )

    private val range = Gauge.Caption.Range(
        minimum = StringResource("0"),
        maximum = StringResource("100"),
    )

    private val large = base.copy(
        lineWidth = Gauge.defaultLineWidth * 2,
        size = Gauge.Size.Large,
    )

    @Test
    fun `Gauge screenshot`() {
        screenshot(gauge = base)
    }

    @Test
    fun `Gauge without value screenshot`() {
        screenshot(gauge = base.withoutValue())
    }

    @Test
    fun `Gauge with range screenshot`() {
        screenshot(gauge = base.withCaption(range))
    }

    @Test
    fun `Gauge with range without value screenshot`() {
        screenshot(gauge = base.withCaption(range).withoutValue())
    }

    @Test
    fun `Gauge without caption screenshot`() {
        screenshot(gauge = base.withCaption(null))
    }

    @Test
    fun `Gauge without caption without value screenshot`() {
        screenshot(gauge = base.withCaption(null).withoutValue())
    }

    @Test
    fun `Gauge without label screenshot`() {
        screenshot(gauge = base.copy(label = null))
    }

    @Test
    fun `Gauge without label without value screenshot`() {
        screenshot(gauge = base.copy(label = null).withoutValue())
    }

    @Test
    fun `Gauge large screenshot`() {
        screenshot(gauge = large)
    }

    @Test
    fun `Gauge large without value screenshot`() {
        screenshot(gauge = large.withoutValue())
    }

    @Test
    fun `Gauge large with range screenshot`() {
        screenshot(gauge = large.withCaption(range))
    }

    @Test
    fun `Gauge large with range without value screenshot`() {
        screenshot(gauge = large.withCaption(range).withoutValue())
    }

    @Test
    fun `Gauge large without caption screenshot`() {
        screenshot(gauge = large.withCaption(null))
    }

    @Test
    fun `Gauge large without caption without value screenshot`() {
        screenshot(gauge = large.withCaption(null).withoutValue())
    }

    @Test
    fun `Gauge large without label screenshot`() {
        screenshot(gauge = large.copy(label = null))
    }

    @Test
    fun `Gauge large without label without value screenshot`() {
        screenshot(gauge = large.copy(label = null).withoutValue())
    }

    @Test
    fun `Gauge at start screenshot`() {
        screenshot(gauge = base.copy(progress = 0f))
    }

    @Test
    fun `Gauge at end screenshot`() {
        screenshot(gauge = base.copy(progress = 1f))
    }

    @Test
    fun `Gauge single color screenshot`() {
        screenshot(gauge = base.copy(colors = listOf(ColorToken.Primary)))
    }

    @Test
    fun `Gauge default line width screenshot`() {
        screenshot(gauge = base.copy(lineWidth = Gauge.defaultLineWidth))
    }

    private fun screenshot(gauge: Gauge) {
        screenshot {
            gauge.Content(modifier = Modifier.padding(Spacings.medium))
        }
    }

    private fun Gauge.withCaption(caption: Gauge.Caption?): Gauge {
        return copy(label = label?.copy(caption = caption))
    }

    private fun Gauge.withoutValue(): Gauge {
        return copy(
            progress = null,
            colors = listOf(ColorToken.Outline),
            label = label?.copy(value = StringResource("—")),
        )
    }
}
