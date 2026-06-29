package edu.stanford.myheartcounts.ui

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.text.font.FontFamily
import edu.stanford.spezi.ui.theme.ColorPalette
import edu.stanford.spezi.ui.theme.Figtree
import edu.stanford.spezi.ui.theme.SpeziTheme
import edu.stanford.spezi.ui.theme.speziTypography

/**
 * Applies the app's theme — the Cardinal Red palette and Figtree typography — to [content].
 */
@Composable
fun MHCAppTheme(
    content: @Composable () -> Unit,
) {
    SpeziTheme(
        darkTheme = isSystemInDarkTheme(),
        colorPalette = ColorPalette.CardinalRed,
        typography = speziTypography(fontFamily = FontFamily.Figtree),
        dynamicColor = false,
        content = content
    )
}
