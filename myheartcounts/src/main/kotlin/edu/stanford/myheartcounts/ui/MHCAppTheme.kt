//
// This source file is part of the My Heart Counts Android open-source project
//
// SPDX-FileCopyrightText: 2026 Stanford University and the project authors (see CONTRIBUTORS.md)
//
// SPDX-License-Identifier: MIT

package edu.stanford.myheartcounts.ui

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.text.font.FontFamily
import org.grovealliance.ui.theme.ColorPalette
import org.grovealliance.ui.theme.Figtree
import org.grovealliance.ui.theme.GroveTheme
import org.grovealliance.ui.theme.groveTypography

/**
 * Applies the app's theme — the Cardinal Red palette and Figtree typography — to [content].
 */
@Composable
fun MHCAppTheme(
    content: @Composable () -> Unit,
) {
    GroveTheme(
        darkTheme = isSystemInDarkTheme(),
        colorPalette = ColorPalette.CardinalRed,
        typography = groveTypography(fontFamily = FontFamily.Figtree),
        dynamicColor = false,
        content = content
    )
}
