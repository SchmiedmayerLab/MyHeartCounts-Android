//
// This source file is part of the My Heart Counts Android open-source project
//
// SPDX-FileCopyrightText: 2026 Stanford University and the project authors (see CONTRIBUTORS.md)
//
// SPDX-License-Identifier: MIT

package edu.stanford.myheartcounts.dashboard.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.MilitaryTech
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.semantics.ProgressBarRangeInfo
import androidx.compose.ui.semantics.progressBarRangeInfo
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.tooling.preview.PreviewParameter
import androidx.compose.ui.tooling.preview.PreviewParameterProvider
import org.grovealliance.ui.ComposableContent
import org.grovealliance.ui.ImageResource
import org.grovealliance.ui.StringResource
import org.grovealliance.ui.theme.ColorToken
import org.grovealliance.ui.theme.Colors
import org.grovealliance.ui.theme.GroveShapes
import org.grovealliance.ui.theme.GroveTheme
import org.grovealliance.ui.theme.Sizes
import org.grovealliance.ui.theme.Spacings
import org.grovealliance.ui.theme.TextStyles
import org.grovealliance.ui.theme.ThemePreviews
import org.grovealliance.ui.theme.medium
import org.grovealliance.ui.theme.resolve

/**
 * A short label over a small bar filling up from the start as progress is made, optionally led by
 * an icon.
 *
 * @param icon the symbol leading the label and the bar, tinted as given; omitted when there is none
 * @param label the text above the bar, already formatted, such as a count reached out of a total
 * @param progress how far the bar is filled, from `0` for empty to `1` for full; must lie within
 * that range
 * @param progressColor the color of the filled part of the bar
 */
@Immutable
data class ProgressBadge(
    val icon: ImageResource?,
    val label: StringResource,
    val progress: Float,
    val progressColor: ColorToken,
) : ComposableContent {

    init {
        require(progress in 0f..1f) {
            "progress must lie within 0 and 1, but was $progress"
        }
    }

    @Composable
    override fun Content(modifier: Modifier) {
        Row(
            modifier = modifier.semantics(mergeDescendants = true) {
                progressBarRangeInfo = ProgressBarRangeInfo(
                    current = progress,
                    range = 0f..1f,
                )
            },
            horizontalArrangement = Arrangement.spacedBy(Spacings.small),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            icon?.Content(modifier = Modifier.size(Sizes.Icon.extraSmall))
            Column(
                verticalArrangement = Arrangement.spacedBy(Spacings.extraSmall),
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                Text(
                    text = label.text(),
                    style = TextStyles.labelMedium.medium(),
                    color = Colors.onSurfaceVariant,
                    maxLines = 1,
                )
                Box(
                    modifier = Modifier
                        .width(Sizes.Icon.medium)
                        .height(Spacings.extraSmall)
                        .clip(GroveShapes.circle)
                        .background(color = Colors.outlineVariant),
                ) {
                    Box(
                        modifier = Modifier
                            .fillMaxHeight()
                            .fillMaxWidth(fraction = progress)
                            .clip(GroveShapes.circle)
                            .background(color = progressColor.resolve()),
                    )
                }
            }
        }
    }
}

private class ProgressBadgePreviewProvider : PreviewParameterProvider<ProgressBadge> {

    private val base = ProgressBadge(
        icon = ImageResource.Vector(
            image = Icons.Default.MilitaryTech,
            tint = { Colors.onSurface },
        ),
        label = StringResource("5 / 20"),
        progress = 0.25f,
        progressColor = ColorToken.Primary,
    )

    override val values: Sequence<ProgressBadge> = sequenceOf(
        base,
        base.copy(icon = null),
        base.copy(progress = 0f),
        base.copy(progress = 1f),
    )
}

@ThemePreviews
@Composable
private fun Preview(
    @PreviewParameter(ProgressBadgePreviewProvider::class) content: ProgressBadge,
) {
    GroveTheme {
        content.Content(modifier = Modifier.padding(Spacings.medium))
    }
}
