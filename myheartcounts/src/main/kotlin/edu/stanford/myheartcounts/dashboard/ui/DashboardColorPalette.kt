//
// This source file is part of the My Heart Counts Android open-source project
//
// SPDX-FileCopyrightText: 2026 Stanford University and the project authors (see CONTRIBUTORS.md)
//
// SPDX-License-Identifier: MIT

package edu.stanford.myheartcounts.dashboard.ui

import androidx.compose.ui.graphics.Color
import org.grovealliance.ui.theme.ColorToken

/**
 * The colors of dashboards: [red] through [green] rate values from the worst to the best, and the
 * remaining colors tell apart the kinds of value shown side by side, such as the icons of tiles.
 *
 * Each color is a [ColorToken] with a lighter variant used in dark mode. The rating colors are
 * bright in both modes, so text on them is drawn in [onRating] in both modes.
 */
object DashboardColorPalette {

    /**
     * Red, marking the worst values.
     */
    val red: ColorToken.Custom = ColorToken.Custom(
        light = Color(color = 0xFFFF3B30),
        dark = Color(color = 0xFFFF453A),
    )

    /**
     * Orange, marking poor values.
     */
    val orange: ColorToken.Custom = ColorToken.Custom(
        light = Color(color = 0xFFFF9500),
        dark = Color(color = 0xFFFF9F0A),
    )

    /**
     * Yellow, marking fair values.
     */
    val yellow: ColorToken.Custom = ColorToken.Custom(
        light = Color(color = 0xFFFFCC00),
        dark = Color(color = 0xFFFFD60A),
    )

    /**
     * Green, marking the best values.
     */
    val green: ColorToken.Custom = ColorToken.Custom(
        light = Color(color = 0xFF34C759),
        dark = Color(color = 0xFF30D158),
    )

    /**
     * Blue, an accent color.
     */
    val blue: ColorToken.Custom = ColorToken.Custom(
        light = Color(color = 0xFF007AFF),
        dark = Color(color = 0xFF0A84FF),
    )

    /**
     * Pink, an accent color.
     */
    val pink: ColorToken.Custom = ColorToken.Custom(
        light = Color(color = 0xFFFF2D55),
        dark = Color(color = 0xFFFF375F),
    )

    /**
     * Indigo, an accent color.
     */
    val indigo: ColorToken.Custom = ColorToken.Custom(
        light = Color(color = 0xFF5856D6),
        dark = Color(color = 0xFF5E5CE6),
    )

    /**
     * Teal, an accent color.
     */
    val teal: ColorToken.Custom = ColorToken.Custom(
        light = Color(color = 0xFF30B0C7),
        dark = Color(color = 0xFF40C8E0),
    )

    /**
     * Cyan, an accent color.
     */
    val cyan: ColorToken.Custom = ColorToken.Custom(
        light = Color(color = 0xFF32ADE6),
        dark = Color(color = 0xFF64D2FF),
    )

    /**
     * Purple, an accent color.
     */
    val purple: ColorToken.Custom = ColorToken.Custom(
        light = Color(color = 0xFFAF52DE),
        dark = Color(color = 0xFFBF5AF2),
    )

    /**
     * Brown, an accent color.
     */
    val brown: ColorToken.Custom = ColorToken.Custom(
        light = Color(color = 0xFFA2845E),
        dark = Color(color = 0xFFAC8E68),
    )

    /**
     * [red], [orange], [yellow] and [green] in that order, for a fill whose start is the worst and
     * whose end is the best value.
     */
    val redToGreen: List<ColorToken.Custom> = listOf(
        red,
        orange,
        yellow,
        green,
    )

    /**
     * The color of text drawn on [red], [orange], [yellow], [green] or any color between them, dark
     * in both modes.
     */
    val onRating: ColorToken = ColorToken.Custom(light = Color.Black)
}
