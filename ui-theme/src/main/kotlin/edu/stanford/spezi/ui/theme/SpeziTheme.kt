//
// This source file is part of the My Heart Counts open-source project
//
// SPDX-FileCopyrightText: 2026 Stanford University and the project authors (see CONTRIBUTORS.md)
//
// SPDX-License-Identifier: MIT

package edu.stanford.spezi.ui.theme

import android.app.Activity
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Typography
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalInspectionMode
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.core.view.WindowCompat

@Composable
fun SpeziTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    dynamicColor: Boolean = false,
    content: @Composable () -> Unit,
) {
    SpeziTheme(
        darkTheme = darkTheme,
        colorPalette = ColorPalette.CardinalRed,
        typography = SpeziTypography,
        dynamicColor = dynamicColor,
        content = content
    )
}

@Composable
fun SpeziTheme(
    darkTheme: Boolean,
    colorPalette: ColorPalette,
    typography: Typography,
    dynamicColor: Boolean,
    content: @Composable () -> Unit,
) {
    val context = LocalContext.current
    val palette = remember(context, darkTheme) {
        when {
            dynamicColor -> {
                ColorPalette(
                    dark = dynamicDarkColorScheme(context),
                    light = dynamicLightColorScheme(context)
                )
            }
            else -> colorPalette
        }
    }
    val colorScheme = if (darkTheme) palette.dark else palette.light
    val view = LocalView.current
    if (!view.isInEditMode) {
        SideEffect {
            val window = (view.context as? Activity)?.window
            if (window != null) {
                window.statusBarColor = colorScheme.onBackground.toArgb()
                WindowCompat.getInsetsController(window, view).isAppearanceLightStatusBars = !darkTheme
            }
        }
    }
    MaterialTheme(
        colorScheme = colorScheme,
        typography = typography,
        content = {
            Surface(
                modifier = if (LocalInspectionMode.current) Modifier else Modifier.fillMaxSize(),
                color = colorScheme.background,
                content = content
            )
        }
    )
}

/**
 * Returns a default [TextStyle] used by Spezi Theme.
 */
private val SpeziTypography by lazy {
    speziTypography(fontFamily = figTree)
}

private val figTree = FontFamily(
    Font(R.font.figtree_light, FontWeight.Light),
    Font(R.font.figtree_light_italic, FontWeight.Light, FontStyle.Italic),
    Font(R.font.figtree_regular, FontWeight.Normal),
    Font(R.font.figtree_italic, FontWeight.Normal, FontStyle.Italic),
    Font(R.font.figtree_medium, FontWeight.Medium),
    Font(R.font.figtree_medium_italic, FontWeight.Medium, FontStyle.Italic),
    Font(R.font.figtree_semibold, FontWeight.SemiBold),
    Font(R.font.figtree_semibold_italic, FontWeight.SemiBold, FontStyle.Italic),
    Font(R.font.figtree_bold, FontWeight.Bold),
    Font(R.font.figtree_bold_italic, FontWeight.Bold, FontStyle.Italic),
    Font(R.font.figtree_extra_bold, FontWeight.ExtraBold),
    Font(R.font.figtree_extra_bold_italic, FontWeight.ExtraBold, FontStyle.Italic),
    Font(R.font.figtree_black, FontWeight.Black),
    Font(R.font.figtree_black_italic, FontWeight.Black, FontStyle.Italic),
)

val FontFamily.Companion.Figtree: FontFamily
    get() = figTree
