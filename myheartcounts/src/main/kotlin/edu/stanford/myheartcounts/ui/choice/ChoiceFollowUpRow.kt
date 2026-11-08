//
// This source file is part of the My Heart Counts Android open-source project
//
// SPDX-FileCopyrightText: 2026 Stanford University and the project authors (see CONTRIBUTORS.md)
//
// SPDX-License-Identifier: MIT

package edu.stanford.myheartcounts.ui.choice

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.UnfoldMore
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.PreviewParameter
import androidx.compose.ui.tooling.preview.PreviewParameterProvider
import org.grovealliance.ui.ComposableContent
import org.grovealliance.ui.GroveCard
import org.grovealliance.ui.StringResource
import org.grovealliance.ui.theme.Colors
import org.grovealliance.ui.theme.GroveTheme
import org.grovealliance.ui.theme.Sizes
import org.grovealliance.ui.theme.Spacings
import org.grovealliance.ui.theme.TextStyles
import org.grovealliance.ui.theme.ThemePreviews

/**
 * A further detail of a chosen [ChoiceRow], such as since when it applies, tapped to change it.
 *
 * @param label names the detail
 * @param value the detail, or what stands in for an unknown one
 * @param isSet whether the detail is known
 * @param onClick called when the detail is tapped
 */
data class ChoiceFollowUpRow(
    val label: StringResource,
    val value: StringResource,
    val isSet: Boolean,
    val onClick: () -> Unit,
) : ComposableContent {

    @Composable
    override fun Content(modifier: Modifier) {
        Row(
            modifier = modifier
                .fillMaxWidth()
                .clickable(onClick = onClick)
                .padding(Spacings.medium)
                .heightIn(min = Sizes.Icon.small),
            horizontalArrangement = Arrangement.spacedBy(Spacings.small),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                modifier = Modifier.weight(1f),
                text = label.text(),
                style = TextStyles.bodyMedium,
                color = Colors.onSurfaceVariant,
            )
            Text(
                text = value.text(),
                style = TextStyles.bodyMedium,
                color = if (isSet) Colors.onSurface else Colors.onSurfaceVariant,
            )
            Icon(
                imageVector = Icons.Default.UnfoldMore,
                modifier = Modifier.size(Sizes.Icon.extraSmall),
                tint = Colors.primary,
                contentDescription = null,
            )
        }
    }
}

private class ChoiceFollowUpRowPreviewProvider : PreviewParameterProvider<ChoiceFollowUpRow> {
    private val base = ChoiceFollowUpRow(
        label = StringResource("Since"),
        value = StringResource("Mar 2020"),
        isSet = true,
        onClick = {},
    )

    override val values = sequenceOf(
        base,
        base.copy(
            value = StringResource("Unknown"),
            isSet = false,
        ),
    )
}

@ThemePreviews
@Composable
private fun Preview(
    @PreviewParameter(ChoiceFollowUpRowPreviewProvider::class) content: ChoiceFollowUpRow,
) {
    GroveTheme {
        GroveCard(modifier = Modifier.padding(Spacings.medium)) {
            content.Content()
        }
    }
}
