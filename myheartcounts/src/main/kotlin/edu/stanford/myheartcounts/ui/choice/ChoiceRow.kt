//
// This source file is part of the My Heart Counts Android open-source project
//
// SPDX-FileCopyrightText: 2026 Stanford University and the project authors (see CONTRIBUTORS.md)
//
// SPDX-License-Identifier: MIT

package edu.stanford.myheartcounts.ui.choice

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.DirectionsWalk
import androidx.compose.material.icons.filled.Check
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.PreviewParameter
import androidx.compose.ui.tooling.preview.PreviewParameterProvider
import org.grovealliance.ui.ComposableContent
import org.grovealliance.ui.GroveCard
import org.grovealliance.ui.ImageResource
import org.grovealliance.ui.StringResource
import org.grovealliance.ui.theme.Colors
import org.grovealliance.ui.theme.GroveTheme
import org.grovealliance.ui.theme.Sizes
import org.grovealliance.ui.theme.Spacings
import org.grovealliance.ui.theme.TextStyles
import org.grovealliance.ui.theme.ThemePreviews

/**
 * One option of a list to choose from, checked while [isSelected], with its [followUp] below it
 * while it is chosen.
 *
 * @param icon an image leading the option
 * @param title the option
 * @param subtitle further explanation below the title
 * @param detail a short trailing value, such as an abbreviation
 * @param isSelected whether the option is chosen
 * @param followUp a further detail of the option, shown below it while it is chosen
 * @param onClick called when the option is tapped
 */
data class ChoiceRow(
    val icon: ImageResource?,
    val title: StringResource,
    val subtitle: StringResource?,
    val detail: StringResource?,
    val isSelected: Boolean,
    val followUp: ChoiceFollowUpRow?,
    val onClick: () -> Unit,
) : ComposableContent {

    @Composable
    override fun Content(modifier: Modifier) {
        Column(modifier = modifier.fillMaxWidth()) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable(onClick = onClick)
                    .padding(Spacings.medium),
                horizontalArrangement = Arrangement.spacedBy(Spacings.small),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                icon?.Content(modifier = Modifier.size(Sizes.Icon.small))
                Column(
                    modifier = Modifier.weight(1f),
                    verticalArrangement = Arrangement.spacedBy(Spacings.extraSmall),
                ) {
                    Text(
                        text = title.text(),
                        style = TextStyles.bodyLarge,
                        color = Colors.onSurface,
                    )
                    subtitle?.let {
                        Text(
                            text = it.text(),
                            style = TextStyles.bodyMedium,
                            color = Colors.onSurfaceVariant,
                        )
                    }
                }
                detail?.let {
                    Text(
                        text = it.text(),
                        style = TextStyles.bodyLarge,
                        color = Colors.onSurfaceVariant,
                    )
                }
                if (isSelected) {
                    Icon(
                        imageVector = Icons.Default.Check,
                        modifier = Modifier.size(Sizes.Icon.small),
                        tint = Colors.primary,
                        contentDescription = null,
                    )
                }
            }
            followUp?.let {
                AnimatedVisibility(visible = isSelected) {
                    it.Content()
                }
            }
        }
    }
}

private class ChoiceRowPreviewProvider : PreviewParameterProvider<ChoiceRow> {
    private val base = ChoiceRow(
        icon = null,
        title = StringResource("California"),
        subtitle = null,
        detail = StringResource("CA"),
        isSelected = true,
        followUp = null,
        onClick = {},
    )

    override val values = sequenceOf(
        base,
        base.copy(
            title = StringResource("Chronic Kidney Disease"),
            subtitle = StringResource("Stage 3, 4 or 5"),
            detail = null,
            followUp = ChoiceFollowUpRow(
                label = StringResource("Since"),
                value = StringResource("Mar 2020"),
                isSet = true,
                onClick = {},
            ),
        ),
        base.copy(isSelected = false),
        base.copy(
            icon = ImageResource(image = Icons.AutoMirrored.Filled.DirectionsWalk),
            title = StringResource("Walking"),
            detail = null,
        ),
    )
}

@ThemePreviews
@Composable
private fun Preview(
    @PreviewParameter(ChoiceRowPreviewProvider::class) content: ChoiceRow,
) {
    GroveTheme {
        GroveCard(modifier = Modifier.padding(Spacings.medium)) {
            content.Content()
        }
    }
}
