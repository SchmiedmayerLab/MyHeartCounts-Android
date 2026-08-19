//
// This source file is part of the My Heart Counts Android open-source project
//
// SPDX-FileCopyrightText: 2026 Stanford University and the project authors (see CONTRIBUTORS.md)
//
// SPDX-License-Identifier: MIT

@file:Suppress("detekt:MagicNumber")
package org.grovealliance.ui

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

private const val DARK_FACTOR = 0.1f
private const val LIGHT_FACTOR = 0.9f

@Composable
fun Color.lighten(): Color {
    val factor = if (isSystemInDarkTheme()) DARK_FACTOR else LIGHT_FACTOR
    val red = (this.red + factor).coerceIn(0f, 1f)
    val green = (this.green + factor).coerceIn(0f, 1f)
    val blue = (this.blue + factor).coerceIn(0f, 1f)
    return Color(red, green, blue, this.alpha)
}
