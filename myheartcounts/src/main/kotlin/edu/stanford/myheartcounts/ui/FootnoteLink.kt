//
// This source file is part of the My Heart Counts Android open-source project
//
// SPDX-FileCopyrightText: 2026 Stanford University and the project authors (see CONTRIBUTORS.md)
//
// SPDX-License-Identifier: MIT

package edu.stanford.myheartcounts.ui

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import org.grovealliance.ui.ComposableContent
import org.grovealliance.ui.StringResource
import org.grovealliance.ui.theme.Colors
import org.grovealliance.ui.theme.GroveTheme
import org.grovealliance.ui.theme.Spacings
import org.grovealliance.ui.theme.TextStyles
import org.grovealliance.ui.theme.ThemePreviews

/**
 * A small, subdued [text] that reports taps through [onClicked], for secondary links such as a
 * privacy policy below a form.
 */
data class FootnoteLink(
    val text: StringResource,
    val onClicked: () -> Unit,
) : ComposableContent {

    @Composable
    override fun Content(modifier: Modifier) {
        Text(
            modifier = modifier.clickable(onClick = onClicked),
            text = text.text(),
            style = TextStyles.bodySmall,
            color = Colors.onSurfaceVariant,
        )
    }
}

@ThemePreviews
@Composable
private fun Preview() {
    GroveTheme {
        FootnoteLink(
            text = StringResource("Privacy Policy"),
            onClicked = {},
        ).Content(modifier = Modifier.padding(Spacings.medium))
    }
}
