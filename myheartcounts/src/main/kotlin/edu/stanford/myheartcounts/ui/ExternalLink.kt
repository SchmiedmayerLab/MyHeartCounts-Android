//
// This source file is part of the My Heart Counts Android open-source project
//
// SPDX-FileCopyrightText: 2026 Stanford University and the project authors (see CONTRIBUTORS.md)
//
// SPDX-License-Identifier: MIT

package edu.stanford.myheartcounts.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.OpenInNew
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import org.grovealliance.ui.ComposableContent
import org.grovealliance.ui.ComposeValue
import org.grovealliance.ui.ImageResource
import org.grovealliance.ui.StringResource
import org.grovealliance.ui.noRippleClickable
import org.grovealliance.ui.theme.Colors
import org.grovealliance.ui.theme.GroveTheme
import org.grovealliance.ui.theme.Sizes
import org.grovealliance.ui.theme.Spacings
import org.grovealliance.ui.theme.ThemePreviews

/**
 * A tappable row that opens an external destination, showing [text] after an optional leading
 * [icon] and before a trailing open-in-new icon, inset by [padding] on every side. Taps are
 * reported through [onClicked].
 */
data class ExternalLink(
    val text: StringResource,
    val icon: ImageResource? = null,
    val padding: ComposeValue<Dp> = { 0.dp },
    val onClicked: () -> Unit,
) : ComposableContent {

    @Composable
    override fun Content(modifier: Modifier) {
        Row(
            modifier = modifier
                .padding(padding())
                .fillMaxWidth()
                .noRippleClickable(onClick = onClicked),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(Spacings.small),
        ) {
            icon?.Content(modifier = Modifier.size(Sizes.Icon.small))
            val text = text.text()
            Text(
                modifier = Modifier.weight(1f),
                text = text,
                color = Colors.primary,
            )

            Icon(
                imageVector = Icons.AutoMirrored.Outlined.OpenInNew,
                contentDescription = text,
                tint = Colors.primary,
            )
        }
    }
}

@ThemePreviews
@Composable
private fun Preview() {
    GroveTheme {
        ExternalLink(
            text = StringResource("About the My Heart Counts Study"),
            onClicked = {},
        ).Content()
    }
}
