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

// Light
private val PrimaryLight = Color(0xFF3C318F)
private val OnPrimaryLight = Color(0xFFFFFFFF)
private val PrimaryContainerLight = Color(0xFFE8DDFF)
private val OnPrimaryContainerLight = Color(0xFF000C61)
private val InversePrimaryLight = Color(0xFFCFBCFF)
private val SecondaryLight = Color(0xFF625A74)
private val OnSecondaryLight = Color(0xFFFFFFFF)
private val SecondaryContainerLight = Color(0xFFE7DEFC)
private val OnSecondaryContainerLight = Color(0xFF1E182E)
private val TertiaryLight = Color(0xFF006559)
private val OnTertiaryLight = Color(0xFFFFFFFF)
private val TertiaryContainerLight = Color(0xFF82F6E1)
private val OnTertiaryContainerLight = Color(0xFF00201B)
private val ErrorLight = Color(0xFFBA1A1A)
private val OnErrorLight = Color(0xFFFFFFFF)
private val ErrorContainerLight = Color(0xFFFFDAD6)
private val OnErrorContainerLight = Color(0xFF410002)
private val BackgroundLight = Color(0xFFF2F0F5)
private val OnBackgroundLight = Color(0xFF1C1B1F)
private val SurfaceLight = Color(0xFFF2F0F5)
private val OnSurfaceLight = Color(0xFF1C1B1F)
private val SurfaceVariantLight = Color(0xFFE5E0EE)
private val OnSurfaceVariantLight = Color(0xFF484550)
private val SurfaceTintLight = Color(0xFF3C318F)
private val InverseSurfaceLight = Color(0xFF313034)
private val InverseOnSurfaceLight = Color(0xFFF2F0F5)
private val OutlineLight = Color(0xFF797581)
private val OutlineVariantLight = Color(0xFFC9C4D1)
private val ScrimLight = Color(0xFF000000)
private val SurfaceBrightLight = Color(0xFFFFFFFF)
private val SurfaceDimLight = Color(0xFFDBD9DF)
private val SurfaceContainerLowestLight = Color(0xFFFFFFFF)
private val SurfaceContainerLowLight = Color(0xFFF7F6FB)
private val SurfaceContainerLight = Color(0xFFECEAF0)
private val SurfaceContainerHighLight = Color(0xFFE6E4EA)
private val SurfaceContainerHighestLight = Color(0xFFE1DFE4)

// Dark
private val PrimaryDark = Color(0xFFCFBCFF)
private val OnPrimaryDark = Color(0xFF28217C)
private val PrimaryContainerDark = Color(0xFF453796)
private val OnPrimaryContainerDark = Color(0xFFE8DDFF)
private val InversePrimaryDark = Color(0xFF453796)
private val SecondaryDark = Color(0xFFCBC2DF)
private val OnSecondaryDark = Color(0xFF332D44)
private val SecondaryContainerDark = Color(0xFF4A435B)
private val OnSecondaryContainerDark = Color(0xFFE7DEFC)
private val TertiaryDark = Color(0xFF64DAC5)
private val OnTertiaryDark = Color(0xFF003830)
private val TertiaryContainerDark = Color(0xFF005046)
private val OnTertiaryContainerDark = Color(0xFF82F6E1)
private val ErrorDark = Color(0xFFFFB4AB)
private val OnErrorDark = Color(0xFF690005)
private val ErrorContainerDark = Color(0xFF93000A)
private val OnErrorContainerDark = Color(0xFFFFDAD6)
private val BackgroundDark = Color(0xFF141317)
private val OnBackgroundDark = Color(0xFFE3E2E7)
private val SurfaceDark = Color(0xFF141317)
private val OnSurfaceDark = Color(0xFFE3E2E7)
private val SurfaceVariantDark = Color(0xFF484550)
private val OnSurfaceVariantDark = Color(0xFFC9C4D1)
private val SurfaceTintDark = Color(0xFFCFBCFF)
private val InverseSurfaceDark = Color(0xFFE3E2E7)
private val InverseOnSurfaceDark = Color(0xFF313034)
private val OutlineDark = Color(0xFF938F9B)
private val OutlineVariantDark = Color(0xFF484550)
private val ScrimDark = Color(0xFF000000)
private val SurfaceBrightDark = Color(0xFF3A383D)
private val SurfaceDimDark = Color(0xFF141317)
private val SurfaceContainerLowestDark = Color(0xFF0F0D13)
private val SurfaceContainerLowDark = Color(0xFF1C1B1F)
private val SurfaceContainerDark = Color(0xFF201F23)
private val SurfaceContainerHighDark = Color(0xFF2B292D)
private val SurfaceContainerHighestDark = Color(0xFF353438)

internal val AccentBlueLightColorScheme = lightColorScheme(
    primary = PrimaryLight,
    onPrimary = OnPrimaryLight,
    primaryContainer = PrimaryContainerLight,
    onPrimaryContainer = OnPrimaryContainerLight,
    inversePrimary = InversePrimaryLight,
    secondary = SecondaryLight,
    onSecondary = OnSecondaryLight,
    secondaryContainer = SecondaryContainerLight,
    onSecondaryContainer = OnSecondaryContainerLight,
    tertiary = TertiaryLight,
    onTertiary = OnTertiaryLight,
    tertiaryContainer = TertiaryContainerLight,
    onTertiaryContainer = OnTertiaryContainerLight,
    error = ErrorLight,
    onError = OnErrorLight,
    errorContainer = ErrorContainerLight,
    onErrorContainer = OnErrorContainerLight,
    background = BackgroundLight,
    onBackground = OnBackgroundLight,
    surface = SurfaceLight,
    onSurface = OnSurfaceLight,
    surfaceVariant = SurfaceVariantLight,
    onSurfaceVariant = OnSurfaceVariantLight,
    surfaceTint = SurfaceTintLight,
    inverseSurface = InverseSurfaceLight,
    inverseOnSurface = InverseOnSurfaceLight,
    outline = OutlineLight,
    outlineVariant = OutlineVariantLight,
    scrim = ScrimLight,
    surfaceBright = SurfaceBrightLight,
    surfaceDim = SurfaceDimLight,
    surfaceContainerLowest = SurfaceContainerLowestLight,
    surfaceContainerLow = SurfaceContainerLowLight,
    surfaceContainer = SurfaceContainerLight,
    surfaceContainerHigh = SurfaceContainerHighLight,
    surfaceContainerHighest = SurfaceContainerHighestLight,
)

internal val AccentBlueDarkColorScheme = darkColorScheme(
    primary = PrimaryDark,
    onPrimary = OnPrimaryDark,
    primaryContainer = PrimaryContainerDark,
    onPrimaryContainer = OnPrimaryContainerDark,
    inversePrimary = InversePrimaryDark,
    secondary = SecondaryDark,
    onSecondary = OnSecondaryDark,
    secondaryContainer = SecondaryContainerDark,
    onSecondaryContainer = OnSecondaryContainerDark,
    tertiary = TertiaryDark,
    onTertiary = OnTertiaryDark,
    tertiaryContainer = TertiaryContainerDark,
    onTertiaryContainer = OnTertiaryContainerDark,
    error = ErrorDark,
    onError = OnErrorDark,
    errorContainer = ErrorContainerDark,
    onErrorContainer = OnErrorContainerDark,
    background = BackgroundDark,
    onBackground = OnBackgroundDark,
    surface = SurfaceDark,
    onSurface = OnSurfaceDark,
    surfaceVariant = SurfaceVariantDark,
    onSurfaceVariant = OnSurfaceVariantDark,
    surfaceTint = SurfaceTintDark,
    inverseSurface = InverseSurfaceDark,
    inverseOnSurface = InverseOnSurfaceDark,
    outline = OutlineDark,
    outlineVariant = OutlineVariantDark,
    scrim = ScrimDark,
    surfaceBright = SurfaceBrightDark,
    surfaceDim = SurfaceDimDark,
    surfaceContainerLowest = SurfaceContainerLowestDark,
    surfaceContainerLow = SurfaceContainerLowDark,
    surfaceContainer = SurfaceContainerDark,
    surfaceContainerHigh = SurfaceContainerHighDark,
    surfaceContainerHighest = SurfaceContainerHighestDark,
)
