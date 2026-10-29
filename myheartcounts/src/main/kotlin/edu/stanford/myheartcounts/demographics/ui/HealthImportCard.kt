//
// This source file is part of the My Heart Counts Android open-source project
//
// SPDX-FileCopyrightText: 2026 Stanford University and the project authors (see CONTRIBUTORS.md)
//
// SPDX-License-Identifier: MIT

package edu.stanford.myheartcounts.demographics.ui

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.MonitorHeart
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.PreviewParameter
import androidx.compose.ui.tooling.preview.PreviewParameterProvider
import org.grovealliance.ui.ComposableContent
import org.grovealliance.ui.GroveCard
import org.grovealliance.ui.StringResource
import org.grovealliance.ui.theme.Colors
import org.grovealliance.ui.theme.GroveShapes
import org.grovealliance.ui.theme.GroveTheme
import org.grovealliance.ui.theme.Sizes
import org.grovealliance.ui.theme.Spacings
import org.grovealliance.ui.theme.TextStyles
import org.grovealliance.ui.theme.ThemePreviews

/**
 * A tappable card offering to fill in answers from health data, explained by [description].
 *
 * @param title the action, in the accent color
 * @param description what the action fills in, and how to answer manually instead
 * @param onClick called when the card is tapped
 */
data class HealthImportCard(
    val title: StringResource,
    val description: StringResource,
    val onClick: () -> Unit,
) : ComposableContent {

    @Composable
    override fun Content(modifier: Modifier) {
        GroveCard(
            modifier = modifier,
            shape = GroveShapes.large,
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable(onClick = onClick)
                    .padding(Spacings.medium),
                horizontalArrangement = Arrangement.spacedBy(Spacings.small),
            ) {
                Icon(
                    imageVector = Icons.Outlined.MonitorHeart,
                    modifier = Modifier.size(Sizes.Icon.small),
                    tint = Colors.primary,
                    contentDescription = null,
                )
                Column(verticalArrangement = Arrangement.spacedBy(Spacings.extraSmall)) {
                    Text(
                        text = title.text(),
                        style = TextStyles.bodyLarge,
                        color = Colors.primary,
                    )
                    Text(
                        text = description.text(),
                        style = TextStyles.bodyMedium,
                        color = Colors.onSurfaceVariant,
                    )
                }
            }
        }
    }
}

private class HealthImportCardPreviewProvider : PreviewParameterProvider<HealthImportCard> {
    private val base = HealthImportCard(
        title = StringResource("Read from Health Connect"),
        description = StringResource("Use this option to fill in your answers from Health Connect."),
        onClick = {},
    )
    override val values = sequenceOf(base)
}

@ThemePreviews
@Composable
private fun Preview(@PreviewParameter(HealthImportCardPreviewProvider::class) content: HealthImportCard) {
    GroveTheme { content.Content(modifier = Modifier.padding(Spacings.medium)) }
}
