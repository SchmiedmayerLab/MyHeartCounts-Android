//
// This source file is part of the My Heart Counts Android open-source project
//
// SPDX-FileCopyrightText: 2026 Stanford University and the project authors (see CONTRIBUTORS.md)
//
// SPDX-License-Identifier: MIT

package edu.stanford.myheartcounts.ui.choice

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.DirectionsRun
import androidx.compose.material.icons.automirrored.filled.DirectionsWalk
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.PreviewParameter
import androidx.compose.ui.tooling.preview.PreviewParameterProvider
import org.grovealliance.ui.ComposableContent
import org.grovealliance.ui.GroveCard
import org.grovealliance.ui.ImageResource
import org.grovealliance.ui.StringResource
import org.grovealliance.ui.theme.Colors
import org.grovealliance.ui.theme.GroveShapes
import org.grovealliance.ui.theme.GroveTheme
import org.grovealliance.ui.theme.Spacings
import org.grovealliance.ui.theme.TextStyles
import org.grovealliance.ui.theme.ThemePreviews

/**
 * A group of [options] shown as one card, separated by hairline dividers, with an optional [header]
 * above and [footer] below.
 *
 * @param header the group's title
 * @param options the options, in display order
 * @param footer an explanation of the group
 */
data class ChoiceSection(
    val header: StringResource?,
    val options: List<ChoiceRow>,
    val footer: StringResource?,
) : ComposableContent {

    @Composable
    override fun Content(modifier: Modifier) {
        Column(
            modifier = modifier,
            verticalArrangement = Arrangement.spacedBy(Spacings.small),
        ) {
            header?.let {
                Text(
                    modifier = Modifier.padding(horizontal = Spacings.medium),
                    text = it.text(),
                    style = TextStyles.titleSmall,
                    color = Colors.onSurfaceVariant,
                )
            }
            GroveCard(
                modifier = Modifier.fillMaxWidth(),
                shape = GroveShapes.large,
            ) {
                options.forEachIndexed { position, option ->
                    if (position > 0) {
                        HorizontalDivider(modifier = Modifier.padding(horizontal = Spacings.medium))
                    }
                    option.Content()
                }
            }
            footer?.let {
                Text(
                    modifier = Modifier.padding(horizontal = Spacings.medium),
                    text = it.text(),
                    style = TextStyles.bodySmall,
                    color = Colors.onSurfaceVariant,
                )
            }
        }
    }
}

private class ChoiceSectionPreviewProvider : PreviewParameterProvider<ChoiceSection> {
    private val option = ChoiceRow(
        icon = null,
        title = StringResource("Diabetes"),
        subtitle = null,
        detail = null,
        isSelected = true,
        followUp = null,
        onClick = {},
    )
    private val base = ChoiceSection(
        header = StringResource("Conditions"),
        options = listOf(
            option,
            option.copy(
                title = StringResource("Heart Failure"),
                isSelected = false,
            ),
        ),
        footer = StringResource("Choose every condition that applies."),
    )
    override val values = sequenceOf(
        base,
        base.copy(
            header = StringResource("Preferred Workout Types"),
            options = listOf(
                option.copy(
                    icon = ImageResource(image = Icons.AutoMirrored.Filled.DirectionsWalk),
                    title = StringResource("Walking"),
                ),
                option.copy(
                    icon = ImageResource(image = Icons.AutoMirrored.Filled.DirectionsRun),
                    title = StringResource("Running"),
                    isSelected = false,
                ),
            ),
            footer = null,
        ),
        base.copy(
            header = null,
            footer = null,
        ),
    )
}

@ThemePreviews
@Composable
private fun Preview(@PreviewParameter(ChoiceSectionPreviewProvider::class) content: ChoiceSection) {
    GroveTheme {
        content.Content(
            modifier = Modifier
                .background(Colors.surfaceContainer)
                .padding(Spacings.medium),
        )
    }
}
