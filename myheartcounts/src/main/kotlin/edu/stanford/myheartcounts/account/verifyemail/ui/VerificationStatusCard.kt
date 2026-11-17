//
// This source file is part of the My Heart Counts Android open-source project
//
// SPDX-FileCopyrightText: 2026 Stanford University and the project authors (see CONTRIBUTORS.md)
//
// SPDX-License-Identifier: MIT

package edu.stanford.myheartcounts.account.verifyemail.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.GppMaybe
import androidx.compose.material.icons.filled.VerifiedUser
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.tooling.preview.PreviewParameter
import androidx.compose.ui.tooling.preview.PreviewParameterProvider
import org.grovealliance.ui.ComposableContent
import org.grovealliance.ui.ImageResource
import org.grovealliance.ui.StringResource
import org.grovealliance.ui.theme.Colors
import org.grovealliance.ui.theme.GroveShapes
import org.grovealliance.ui.theme.GroveTheme
import org.grovealliance.ui.theme.Sizes
import org.grovealliance.ui.theme.Spacings
import org.grovealliance.ui.theme.TextStyles
import org.grovealliance.ui.theme.ThemePreviews

/**
 * A rounded strip stating whether the participant's email address is verified: an [icon] beside
 * the status [text].
 */
data class VerificationStatusCard(
    val icon: ImageResource,
    val text: StringResource,
) : ComposableContent {

    @Composable
    override fun Content(modifier: Modifier) {
        Row(
            modifier = modifier
                .clip(GroveShapes.medium)
                .background(color = Colors.surfaceContainerHigh)
                .padding(
                    horizontal = Spacings.medium,
                    vertical = Spacings.small,
                ),
            horizontalArrangement = Arrangement.spacedBy(Spacings.small),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            icon.Content(modifier = Modifier.size(Sizes.Icon.small))
            Text(
                text = text.text(),
                style = TextStyles.bodyLarge,
                color = Colors.onSurfaceVariant,
            )
        }
    }
}

private class VerificationStatusCardPreviewProvider : PreviewParameterProvider<VerificationStatusCard> {
    private val base = VerificationStatusCard(
        icon = ImageResource(image = Icons.Default.GppMaybe),
        text = StringResource("Your email currently is not verified"),
    )
    override val values = sequenceOf(
        base,
        base.copy(
            icon = ImageResource(image = Icons.Default.VerifiedUser),
            text = StringResource("Your email address is verified"),
        ),
    )
}

@ThemePreviews
@Composable
private fun Preview(@PreviewParameter(VerificationStatusCardPreviewProvider::class) content: VerificationStatusCard) {
    GroveTheme { content.Content(modifier = Modifier.padding(Spacings.medium)) }
}
