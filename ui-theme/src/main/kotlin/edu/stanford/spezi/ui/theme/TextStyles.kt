//
// This source file is part of the My Heart Counts open-source project
//
// SPDX-FileCopyrightText: 2026 Stanford University and the project authors (see CONTRIBUTORS.md)
//
// SPDX-License-Identifier: MIT

package edu.stanford.spezi.ui.theme

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.Typography
import androidx.compose.runtime.Composable
import androidx.compose.runtime.ReadOnlyComposable
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.sp

object TextStyles {
    private val typography
        @Composable
        @ReadOnlyComposable
        get() = MaterialTheme.typography

    val bodyMedium: TextStyle
        @Composable
        @ReadOnlyComposable
        get() = typography.bodyMedium

    val bodyLarge: TextStyle
        @Composable
        @ReadOnlyComposable
        get() = typography.bodyLarge

    val bodySmall: TextStyle
        @Composable
        @ReadOnlyComposable
        get() = typography.bodySmall

    val titleLarge: TextStyle
        @Composable
        @ReadOnlyComposable
        get() = typography.titleLarge

    val titleMedium: TextStyle
        @Composable
        @ReadOnlyComposable
        get() = typography.titleMedium

    val titleSmall: TextStyle
        @Composable
        @ReadOnlyComposable
        get() = typography.titleSmall

    val labelSmall: TextStyle
        @Composable
        @ReadOnlyComposable
        get() = typography.labelSmall

    val labelMedium: TextStyle
        @Composable
        @ReadOnlyComposable
        get() = typography.labelMedium

    val labelLarge: TextStyle
        @Composable
        @ReadOnlyComposable
        get() = typography.labelLarge

    val headlineSmall
        @Composable
        @ReadOnlyComposable
        get() = typography.headlineSmall

    val headlineMedium
        @Composable
        @ReadOnlyComposable
        get() = typography.headlineMedium

    val headlineLarge
        @Composable
        @ReadOnlyComposable
        get() = typography.headlineLarge

    val displaySmall
        @Composable
        @ReadOnlyComposable
        get() = typography.displaySmall

    val displayMedium
        @Composable
        @ReadOnlyComposable
        get() = typography.displayMedium

    val displayLarge
        @Composable
        @ReadOnlyComposable
        get() = typography.displayLarge
}

/**
 * Returns a [TextStyle] with the same properties as the original, but with a [FontWeight.Light] font weight.
 */
fun TextStyle.light(): TextStyle = copy(fontWeight = FontWeight.Light)

/**
 * Returns a [TextStyle] with the same properties as the original, but with a [FontWeight.Medium] font weight.
 */
fun TextStyle.medium(): TextStyle = copy(fontWeight = FontWeight.Medium)

/**
 * Returns a [TextStyle] with the same properties as the original, but with a [FontWeight.SemiBold] font weight.
 */
fun TextStyle.semiBold(): TextStyle = copy(fontWeight = FontWeight.SemiBold)

/**
 * Returns a [TextStyle] with the same properties as the original, but with a [FontWeight.Bold] font weight.
 */
fun TextStyle.bold(): TextStyle = copy(fontWeight = FontWeight.Bold)

/**
 * Returns a [TextStyle] with the same properties as the original, but with a [FontWeight.ExtraBold] font weight.
 */
fun TextStyle.extraBold(): TextStyle = copy(fontWeight = FontWeight.ExtraBold)

/**
 * Returns a [TextStyle] with the same properties as the original, but with a [FontWeight.Black] font weight.
 */
fun TextStyle.black(): TextStyle = copy(fontWeight = FontWeight.Black)

/**
 * Returns a [TextStyle] with the same properties as the original, but with an [FontStyle.Italic] font style.
 */
fun TextStyle.italic(): TextStyle = copy(fontStyle = FontStyle.Italic)

/**
 * Creates a [Typography] with the given [fontFamily] and default values for the other properties.
 */
@Suppress("LongMethod")
fun speziTypography(fontFamily: FontFamily) = Typography(
    displayLarge = TextStyle(
        fontFamily = fontFamily,
        fontSize = 57.sp,
        lineHeight = 64.sp,
        letterSpacing = (-0.25).sp
    ),
    displayMedium = TextStyle(
        fontFamily = fontFamily,
        fontSize = 45.sp,
        lineHeight = 52.sp,
        letterSpacing = 0.sp
    ),
    displaySmall = TextStyle(
        fontFamily = fontFamily,
        fontSize = 36.sp,
        lineHeight = 44.sp,
        letterSpacing = 0.sp
    ),

    headlineLarge = TextStyle(
        fontFamily = fontFamily,
        fontSize = 32.sp,
        lineHeight = 40.sp,
        letterSpacing = 0.sp
    ),
    headlineMedium = TextStyle(
        fontFamily = fontFamily,
        fontSize = 28.sp,
        lineHeight = 36.sp,
        letterSpacing = 0.sp
    ),
    headlineSmall = TextStyle(
        fontFamily = fontFamily,
        fontSize = 24.sp,
        lineHeight = 32.sp,
        letterSpacing = 0.sp
    ),

    titleLarge = TextStyle(
        fontFamily = fontFamily,
        fontSize = 20.sp,
        lineHeight = 28.sp,
        letterSpacing = 0.sp
    ),
    titleMedium = TextStyle(
        fontFamily = fontFamily,
        fontSize = 18.sp,
        lineHeight = 24.sp,
        letterSpacing = 0.sp
    ),
    titleSmall = TextStyle(
        fontFamily = fontFamily,
        fontSize = 16.sp,
        lineHeight = 22.sp,
        letterSpacing = 0.15.sp
    ),
    bodyLarge = TextStyle(
        fontFamily = fontFamily,
        fontSize = 16.sp,
        lineHeight = 24.sp,
        letterSpacing = 0.5.sp
    ),
    bodyMedium = TextStyle(
        fontFamily = fontFamily,
        fontSize = 14.sp,
        lineHeight = 20.sp,
        letterSpacing = 0.25.sp
    ),
    bodySmall = TextStyle(
        fontFamily = fontFamily,
        fontSize = 12.sp,
        lineHeight = 16.sp,
        letterSpacing = 0.4.sp
    ),

    labelLarge = TextStyle(
        fontFamily = fontFamily,
        fontSize = 14.sp,
        lineHeight = 20.sp,
        letterSpacing = 0.5.sp
    ),
    labelMedium = TextStyle(
        fontFamily = fontFamily,
        fontSize = 12.sp,
        lineHeight = 16.sp,
        letterSpacing = 0.5.sp
    ),
    labelSmall = TextStyle(
        fontFamily = fontFamily,
        fontSize = 11.sp,
        lineHeight = 16.sp,
        letterSpacing = 0.5.sp
    )
)

@Preview(showBackground = true)
@Composable
private fun TypographyPreview() {
    SpeziTheme {
        Column(verticalArrangement = Arrangement.spacedBy(Spacings.small)) {
            Text(
                text = "headlineLarge",
                style = TextStyles.headlineLarge
            )
            Text(
                text = "headlineMedium",
                style = TextStyles.headlineMedium
            )
            Text(
                text = "headlineSmall",
                style = TextStyles.headlineSmall
            )
            Text(
                text = "titleLarge",
                style = TextStyles.titleLarge
            )
            Text(
                text = "titleMedium",
                style = TextStyles.titleMedium
            )
            Text(
                text = "titleSmall",
                style = TextStyles.titleSmall
            )
            Text(
                text = "bodyLarge",
                style = TextStyles.bodyLarge
            )
            Text(
                text = "bodyMedium",
                style = TextStyles.bodyMedium
            )
            Text(
                text = "bodySmall",
                style = TextStyles.bodySmall
            )
            Text(
                text = "labelLarge",
                style = TextStyles.labelLarge
            )
            Text(
                text = "labelSmall",
                style = TextStyles.labelSmall
            )
        }
    }
}
