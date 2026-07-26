//
// This source file is part of the My Heart Counts open-source project
//
// SPDX-FileCopyrightText: 2026 Stanford University and the project authors (see CONTRIBUTORS.md)
//
// SPDX-License-Identifier: MIT

package edu.stanford.spezi.ui.theme

import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.ui.graphics.Color

private val primaryLight = Color(0xFFBF0036)
private val onPrimaryLight = Color(0xFFFFFFFF)
private val primaryContainerLight = Color(0xFFFFDAD9)
private val onPrimaryContainerLight = Color(0xFF420007)
private val inversePrimaryLight = Color(0xFFFFB3B2)
private val secondaryLight = Color(0xFF7F5353)
private val onSecondaryLight = Color(0xFFFFFFFF)
private val secondaryContainerLight = Color(0xFFFFDAD9)
private val onSecondaryContainerLight = Color(0xFF341011)
private val tertiaryLight = Color(0xFF875213)
private val onTertiaryLight = Color(0xFFFFFFFF)
private val tertiaryContainerLight = Color(0xFFFFDCBE)
private val onTertiaryContainerLight = Color(0xFF2A1800)
private val errorLight = Color(0xFFBA1A1A)
private val onErrorLight = Color(0xFFFFFFFF)
private val errorContainerLight = Color(0xFFFFDAD6)
private val onErrorContainerLight = Color(0xFF410002)
private val backgroundLight = Color(0xFFF8F6F2)
private val onBackgroundLight = Color(0xFF211F1D)
private val surfaceLight = Color(0xFFF8F6F2)
private val onSurfaceLight = Color(0xFF211F1D)
private val surfaceVariantLight = Color(0xFFE7E2D8)
private val onSurfaceVariantLight = Color(0xFF6D6962)
private val surfaceTintLight = Color(0xFFBF0036)
private val inverseSurfaceLight = Color(0xFF363532)
private val inverseOnSurfaceLight = Color(0xFFF2F0ED)
private val outlineLight = Color(0xFF7B776E)
private val outlineVariantLight = Color(0xFFE5E3DE)
private val scrimLight = Color(0xFF000000)
private val surfaceBrightLight = Color(0xFFFEFCF8)
private val surfaceDimLight = Color(0xFFDBDAD6)
private val surfaceContainerLowestLight = Color(0xFFFFFFFF)
private val surfaceContainerLowLight = Color(0xFFF5F3F0)
private val surfaceContainerLight = Color(0xFFEFEEEA)
private val surfaceContainerHighLight = Color(0xFFEAE8E4)
private val surfaceContainerHighestLight = Color(0xFFE4E2DF)

private val primaryDark = Color(0xFFFFB3B2)
private val onPrimaryDark = Color(0xFF680019)
private val primaryContainerDark = Color(0xFF920027)
private val onPrimaryContainerDark = Color(0xFFFFDAD9)
private val inversePrimaryDark = Color(0xFFBF0036)
private val secondaryDark = Color(0xFFEEBAB9)
private val onSecondaryDark = Color(0xFF4C2526)
private val secondaryContainerDark = Color(0xFF653B3C)
private val onSecondaryContainerDark = Color(0xFFFFDAD9)
private val tertiaryDark = Color(0xFFFCB977)
private val onTertiaryDark = Color(0xFF492900)
private val tertiaryContainerDark = Color(0xFF6A3C00)
private val onTertiaryContainerDark = Color(0xFFFFDCBE)
private val errorDark = Color(0xFFFFB4AB)
private val onErrorDark = Color(0xFF690005)
private val errorContainerDark = Color(0xFF93000A)
private val onErrorContainerDark = Color(0xFFFFDAD6)
private val backgroundDark = Color(0xFF171512)
private val onBackgroundDark = Color(0xFFE4E2DF)
private val surfaceDark = Color(0xFF171512)
private val onSurfaceDark = Color(0xFFE4E2DF)
private val surfaceVariantDark = Color(0xFF4A463E)
private val onSurfaceVariantDark = Color(0xFFA7A39A)
private val surfaceTintDark = Color(0xFFFFB3B2)
private val inverseSurfaceDark = Color(0xFFE4E2DF)
private val inverseOnSurfaceDark = Color(0xFF363532)
private val outlineDark = Color(0xFF959087)
private val outlineVariantDark = Color(0xFF4A463E)
private val scrimDark = Color(0xFF000000)
private val surfaceBrightDark = Color(0xFF3A3936)
private val surfaceDimDark = Color(0xFF171512)
private val surfaceContainerLowestDark = Color(0xFF100E0A)
private val surfaceContainerLowDark = Color(0xFF1D1B19)
private val surfaceContainerDark = Color(0xFF211F1D)
private val surfaceContainerHighDark = Color(0xFF2B2A27)
private val surfaceContainerHighestDark = Color(0xFF363532)

internal val CardinalRedLightColors = lightColorScheme(
    primary = primaryLight,
    onPrimary = onPrimaryLight,
    primaryContainer = primaryContainerLight,
    onPrimaryContainer = onPrimaryContainerLight,
    inversePrimary = inversePrimaryLight,
    secondary = secondaryLight,
    onSecondary = onSecondaryLight,
    secondaryContainer = secondaryContainerLight,
    onSecondaryContainer = onSecondaryContainerLight,
    tertiary = tertiaryLight,
    onTertiary = onTertiaryLight,
    tertiaryContainer = tertiaryContainerLight,
    onTertiaryContainer = onTertiaryContainerLight,
    error = errorLight,
    onError = onErrorLight,
    errorContainer = errorContainerLight,
    onErrorContainer = onErrorContainerLight,
    background = backgroundLight,
    onBackground = onBackgroundLight,
    surface = surfaceLight,
    onSurface = onSurfaceLight,
    surfaceVariant = surfaceVariantLight,
    onSurfaceVariant = onSurfaceVariantLight,
    surfaceTint = surfaceTintLight,
    inverseSurface = inverseSurfaceLight,
    inverseOnSurface = inverseOnSurfaceLight,
    outline = outlineLight,
    outlineVariant = outlineVariantLight,
    scrim = scrimLight,
    surfaceBright = surfaceBrightLight,
    surfaceDim = surfaceDimLight,
    surfaceContainerLowest = surfaceContainerLowestLight,
    surfaceContainerLow = surfaceContainerLowLight,
    surfaceContainer = surfaceContainerLight,
    surfaceContainerHigh = surfaceContainerHighLight,
    surfaceContainerHighest = surfaceContainerHighestLight,
)

internal val CardinalRedDarkColors = darkColorScheme(
    primary = primaryDark,
    onPrimary = onPrimaryDark,
    primaryContainer = primaryContainerDark,
    onPrimaryContainer = onPrimaryContainerDark,
    inversePrimary = inversePrimaryDark,
    secondary = secondaryDark,
    onSecondary = onSecondaryDark,
    secondaryContainer = secondaryContainerDark,
    onSecondaryContainer = onSecondaryContainerDark,
    tertiary = tertiaryDark,
    onTertiary = onTertiaryDark,
    tertiaryContainer = tertiaryContainerDark,
    onTertiaryContainer = onTertiaryContainerDark,
    error = errorDark,
    onError = onErrorDark,
    errorContainer = errorContainerDark,
    onErrorContainer = onErrorContainerDark,
    background = backgroundDark,
    onBackground = onBackgroundDark,
    surface = surfaceDark,
    onSurface = onSurfaceDark,
    surfaceVariant = surfaceVariantDark,
    onSurfaceVariant = onSurfaceVariantDark,
    surfaceTint = surfaceTintDark,
    inverseSurface = inverseSurfaceDark,
    inverseOnSurface = inverseOnSurfaceDark,
    outline = outlineDark,
    outlineVariant = outlineVariantDark,
    scrim = scrimDark,
    surfaceBright = surfaceBrightDark,
    surfaceDim = surfaceDimDark,
    surfaceContainerLowest = surfaceContainerLowestDark,
    surfaceContainerLow = surfaceContainerLowDark,
    surfaceContainer = surfaceContainerDark,
    surfaceContainerHigh = surfaceContainerHighDark,
    surfaceContainerHighest = surfaceContainerHighestDark,
)
