//
// This source file is part of the My Heart Counts Android open-source project
//
// SPDX-FileCopyrightText: 2026 Stanford University and the project authors (see CONTRIBUTORS.md)
//
// SPDX-License-Identifier: MIT

package edu.stanford.spezi.ui.theme

import androidx.compose.material3.ColorScheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.ReadOnlyComposable
import androidx.compose.ui.graphics.Color
import edu.stanford.spezi.ui.theme.Colors.background
import edu.stanford.spezi.ui.theme.Colors.error
import edu.stanford.spezi.ui.theme.Colors.errorContainer
import edu.stanford.spezi.ui.theme.Colors.inverseSurface
import edu.stanford.spezi.ui.theme.Colors.outline
import edu.stanford.spezi.ui.theme.Colors.primary
import edu.stanford.spezi.ui.theme.Colors.primaryContainer
import edu.stanford.spezi.ui.theme.Colors.secondary
import edu.stanford.spezi.ui.theme.Colors.secondaryContainer
import edu.stanford.spezi.ui.theme.Colors.surface
import edu.stanford.spezi.ui.theme.Colors.surfaceContainer
import edu.stanford.spezi.ui.theme.Colors.surfaceVariant
import edu.stanford.spezi.ui.theme.Colors.tertiary
import edu.stanford.spezi.ui.theme.Colors.tertiaryContainer
import edu.stanford.spezi.ui.theme.Colors.transparent

/**
 * A facade over [MaterialTheme.colorScheme] that exposes every [androidx.compose.material3.ColorScheme]
 * color as a composable, read-only property, plus a convenience [transparent] constant.
 *
 * All properties are annotated with [@ReadOnlyComposable][ReadOnlyComposable] so they can be read
 * in snapshot-safe contexts without triggering unnecessary recompositions.
 */
object Colors {
    private val scheme
        @Composable
        @ReadOnlyComposable
        get() = MaterialTheme.colorScheme

    /**
     * The primary color of the theme. Used for prominent UI elements such as FABs and filled buttons.
     */
    val primary
        @Composable
        @ReadOnlyComposable
        get() = scheme.primary

    /**
     * Color used for content (icons, text) drawn on top of [primary].
     */
    val onPrimary
        @Composable
        @ReadOnlyComposable
        get() = scheme.onPrimary

    /**
     * A tonal variant of [primary], typically used for container backgrounds.
     */
    val primaryContainer
        @Composable
        @ReadOnlyComposable
        get() = scheme.primaryContainer

    /**
     * Color used for content drawn on top of [primaryContainer].
     */
    val onPrimaryContainer
        @Composable
        @ReadOnlyComposable
        get() = scheme.onPrimaryContainer

    /**
     * An accent color used on dark surfaces to provide contrast against [primary].
     */
    val inversePrimary
        @Composable
        @ReadOnlyComposable
        get() = scheme.inversePrimary

    /**
     * The secondary color of the theme. Used for less prominent UI components.
     */
    val secondary
        @Composable
        @ReadOnlyComposable
        get() = scheme.secondary

    /**
     * Color used for content drawn on top of [secondary].
     */
    val onSecondary
        @Composable
        @ReadOnlyComposable
        get() = scheme.onSecondary

    /**
     * A tonal variant of [secondary], typically used for container backgrounds.
     */
    val secondaryContainer
        @Composable
        @ReadOnlyComposable
        get() = scheme.secondaryContainer

    /**
     * Color used for content drawn on top of [secondaryContainer].
     */
    val onSecondaryContainer
        @Composable
        @ReadOnlyComposable
        get() = scheme.onSecondaryContainer

    /**
     * The tertiary color of the theme. Used for contrasting accents to balance primary and secondary.
     */
    val tertiary
        @Composable
        @ReadOnlyComposable
        get() = scheme.tertiary

    /**
     * Color used for content drawn on top of [tertiary].
     */
    val onTertiary
        @Composable
        @ReadOnlyComposable
        get() = scheme.onTertiary

    /**
     * A tonal variant of [tertiary], typically used for container backgrounds.
     */
    val tertiaryContainer
        @Composable
        @ReadOnlyComposable
        get() = scheme.tertiaryContainer

    /**
     * Color used for content drawn on top of [tertiaryContainer].
     */
    val onTertiaryContainer
        @Composable
        @ReadOnlyComposable
        get() = scheme.onTertiaryContainer

    /**
     * The background color, shown behind scrollable content.
     */
    val background
        @Composable
        @ReadOnlyComposable
        get() = scheme.background

    /**
     * Color used for content drawn on top of [background].
     */
    val onBackground
        @Composable
        @ReadOnlyComposable
        get() = scheme.onBackground

    /**
     * The surface color, used for component backgrounds such as cards and sheets.
     */
    val surface
        @Composable
        @ReadOnlyComposable
        get() = scheme.surface

    /**
     * Color used for content drawn on top of [surface].
     */
    val onSurface
        @Composable
        @ReadOnlyComposable
        get() = scheme.onSurface

    /**
     * A tonal variant of [surface], used for differentiated surface regions.
     */
    val surfaceVariant
        @Composable
        @ReadOnlyComposable
        get() = scheme.surfaceVariant

    /**
     * Color used for content drawn on top of [surfaceVariant].
     */
    val onSurfaceVariant
        @Composable
        @ReadOnlyComposable
        get() = scheme.onSurfaceVariant

    /**
     * Color used to tint surface containers and other surfaces. Typically matches [primary].
     */
    val surfaceTint
        @Composable
        @ReadOnlyComposable
        get() = scheme.surfaceTint

    /**
     * A surface color used on dark surfaces to provide contrast, opposite of [surface].
     */
    val inverseSurface
        @Composable
        @ReadOnlyComposable
        get() = scheme.inverseSurface

    /**
     * Color used for content drawn on top of [inverseSurface].
     */
    val inverseOnSurface
        @Composable
        @ReadOnlyComposable
        get() = scheme.inverseOnSurface

    /**
     * The error color, used to indicate errors or destructive actions.
     */
    val error
        @Composable
        @ReadOnlyComposable
        get() = scheme.error

    /**
     * Color used for content drawn on top of [error].
     */
    val onError
        @Composable
        @ReadOnlyComposable
        get() = scheme.onError

    /**
     * A tonal variant of [error], used for error container backgrounds.
     */
    val errorContainer
        @Composable
        @ReadOnlyComposable
        get() = scheme.errorContainer

    /**
     * Color used for content drawn on top of [errorContainer].
     */
    val onErrorContainer
        @Composable
        @ReadOnlyComposable
        get() = scheme.onErrorContainer

    /**
     * The outline color, used for borders and decorative separators.
     */
    val outline
        @Composable
        @ReadOnlyComposable
        get() = scheme.outline

    /**
     * A lower-emphasis variant of [outline], used for decorative elements that require less contrast.
     */
    val outlineVariant
        @Composable
        @ReadOnlyComposable
        get() = scheme.outlineVariant

    /**
     * The scrim color, used to obscure content behind modal surfaces such as bottom sheets.
     */
    val scrim
        @Composable
        @ReadOnlyComposable
        get() = scheme.scrim

    /**
     * A bright variant of [surface], used to highlight elevated or focused surface areas.
     */
    val surfaceBright
        @Composable
        @ReadOnlyComposable
        get() = scheme.surfaceBright

    /**
     * A dimmed variant of [surface], used for recessed or inactive surface areas.
     */
    val surfaceDim
        @Composable
        @ReadOnlyComposable
        get() = scheme.surfaceDim

    /**
     * A surface container color at the default elevation level.
     */
    val surfaceContainer
        @Composable
        @ReadOnlyComposable
        get() = scheme.surfaceContainer

    /**
     * A surface container color at a higher elevation level than [surfaceContainer].
     */
    val surfaceContainerHigh
        @Composable
        @ReadOnlyComposable
        get() = scheme.surfaceContainerHigh

    /**
     * A surface container color at the highest elevation level.
     */
    val surfaceContainerHighest
        @Composable
        @ReadOnlyComposable
        get() = scheme.surfaceContainerHighest

    /**
     * A surface container color at a lower elevation level than [surfaceContainer].
     */
    val surfaceContainerLow
        @Composable
        @ReadOnlyComposable
        get() = scheme.surfaceContainerLow

    /**
     * A surface container color at the lowest elevation level.
     */
    val surfaceContainerLowest
        @Composable
        @ReadOnlyComposable
        get() = scheme.surfaceContainerLowest

    /**
     * Fully transparent color. Convenience constant for cases where no color fill is desired.
     */
    val transparent
        @Composable
        @ReadOnlyComposable
        get() = Color.Transparent
}

/**
 * A color palette containing both light and dark [ColorScheme]s. Used to provide a complete set of
 * colors for both themes when dynamic color is not enabled.
 */
data class ColorPalette(
    val light: ColorScheme,
    val dark: ColorScheme,
) {
    companion object {
        val CardinalRed = ColorPalette(
            light = CardinalRedLightColors,
            dark = CardinalRedDarkColors,
        )

        val AccentBlue = ColorPalette(
            light = AccentBlueLightColorScheme,
            dark = AccentBlueDarkColorScheme,
        )
    }
}
