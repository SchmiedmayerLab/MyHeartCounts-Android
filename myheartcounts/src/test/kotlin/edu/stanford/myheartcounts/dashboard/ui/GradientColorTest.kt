//
// This source file is part of the My Heart Counts Android open-source project
//
// SPDX-FileCopyrightText: 2026 Stanford University and the project authors (see CONTRIBUTORS.md)
//
// SPDX-License-Identifier: MIT

package edu.stanford.myheartcounts.dashboard.ui

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import com.google.common.truth.Truth.assertThat
import org.grovealliance.ui.theme.ColorToken
import org.junit.Assert.assertThrows
import org.junit.Test

class GradientColorTest {

    private val colors = listOf(
        Color(color = 0xFFFF3B30),
        Color(color = 0xFFFFCC00),
        Color(color = 0xFF34C759),
    )

    @Test
    fun `the start is the first color`() {
        // when
        val color = colors.colorAt(fraction = 0f)

        // then
        assertThat(color.toArgb()).isEqualTo(colors.first().toArgb())
    }

    @Test
    fun `the end is the last color`() {
        // when
        val color = colors.colorAt(fraction = 1f)

        // then
        assertThat(color.toArgb()).isEqualTo(colors.last().toArgb())
    }

    @Test
    fun `a position on a color is that color`() {
        // when
        val color = colors.colorAt(fraction = 0.5f)

        // then
        assertThat(color.toArgb()).isEqualTo(colors[1].toArgb())
    }

    @Test
    fun `a single color is returned at any position`() {
        // when
        val color = listOf(Color.Red).colorAt(fraction = 0.3f)

        // then
        assertThat(color).isEqualTo(Color.Red)
    }

    @Test
    fun `colors are blended component by component in sRGB`() {
        // when
        val color = listOf(
            Color(color = 0xFFFF3B30),
            Color(color = 0xFFFF9500),
        ).colorAt(fraction = 0.6f)

        // then
        assertThat(color.toArgb()).isEqualTo(0xFFFF7113.toInt())
    }

    @Test
    fun `a fraction outside the colors is rejected`() {
        // when / then
        assertThrows(IllegalArgumentException::class.java) {
            colors.colorAt(fraction = 1.5f)
        }
    }

    @Test
    fun `tokens are picked in light and dark mode alike`() {
        // given
        val tokens = listOf(
            ColorToken.Custom(
                light = Color(color = 0xFFFF3B30),
                dark = Color(color = 0xFF000000),
            ),
            ColorToken.Custom(
                light = Color(color = 0xFFFF9500),
                dark = Color(color = 0xFFFFFFFF),
            ),
        )

        // when
        val token = tokens.colorAt(fraction = 0.6f)

        // then
        assertThat(token.light.toArgb()).isEqualTo(0xFFFF7113.toInt())
        assertThat(token.dark.toArgb()).isEqualTo(0xFF999999.toInt())
    }

    @Test
    fun `a fraction outside tokens is rejected`() {
        // when / then
        assertThrows(IllegalArgumentException::class.java) {
            DashboardColorPalette.redToGreen.colorAt(fraction = -0.1f)
        }
    }

    @Test
    fun `no colors are rejected`() {
        // when / then
        assertThrows(IllegalArgumentException::class.java) {
            emptyList<Color>().colorAt(fraction = 0.5f)
        }
    }
}
