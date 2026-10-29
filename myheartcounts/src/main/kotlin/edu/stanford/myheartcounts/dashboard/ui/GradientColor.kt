//
// This source file is part of the My Heart Counts Android open-source project
//
// SPDX-FileCopyrightText: 2026 Stanford University and the project authors (see CONTRIBUTORS.md)
//
// SPDX-License-Identifier: MIT

package edu.stanford.myheartcounts.dashboard.ui

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.colorspace.ColorSpaces
import org.grovealliance.ui.theme.ColorToken

/**
 * The color at [fraction] of the way along these colors, spread evenly from the first at `0` to the
 * last at `1`, as a [Gauge] or a [ColorBandList.Band] spreads them.
 *
 * Neighboring colors are blended component by component in sRGB.
 *
 * @param fraction the position along the colors; must lie within `0` and `1`
 */
fun List<Color>.colorAt(fraction: Float): Color {
    require(isNotEmpty()) { "colors must not be empty" }
    require(fraction in 0f..1f) {
        "fraction must lie within 0 and 1, but was $fraction"
    }
    if (size == 1) return single()
    val position = fraction * lastIndex
    val index = position.toInt().coerceAtMost(lastIndex - 1)
    val start = this[index].convert(ColorSpaces.Srgb)
    val end = this[index + 1].convert(ColorSpaces.Srgb)
    val amount = position - index
    return Color(
        red = start.red + (end.red - start.red) * amount,
        green = start.green + (end.green - start.green) * amount,
        blue = start.blue + (end.blue - start.blue) * amount,
        alpha = start.alpha + (end.alpha - start.alpha) * amount,
        colorSpace = ColorSpaces.Srgb,
    )
}

/**
 * The color at [fraction] of the way along these colors, as [List.colorAt] picks it, in both light
 * and dark mode.
 *
 * @param fraction the position along the colors; must lie within `0` and `1`
 */
fun List<ColorToken.Custom>.colorAt(fraction: Float): ColorToken.Custom {
    return ColorToken.Custom(
        light = map { it.light }.colorAt(fraction = fraction),
        dark = map { it.dark }.colorAt(fraction = fraction),
    )
}
