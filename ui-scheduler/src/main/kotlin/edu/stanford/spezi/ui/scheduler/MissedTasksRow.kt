//
// This source file is part of the My Heart Counts open-source project
//
// SPDX-FileCopyrightText: 2026 Stanford University and the project authors (see CONTRIBUTORS.md)
//
// SPDX-License-Identifier: MIT

package edu.stanford.spezi.ui.scheduler

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowForwardIos
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import edu.stanford.spezi.ui.ComposableContent
import edu.stanford.spezi.ui.ImageResource
import edu.stanford.spezi.ui.SpeziCard
import edu.stanford.spezi.ui.StringResource
import edu.stanford.spezi.ui.noRippleClickable
import edu.stanford.spezi.ui.theme.Colors
import edu.stanford.spezi.ui.theme.Sizes
import edu.stanford.spezi.ui.theme.Spacings
import edu.stanford.spezi.ui.theme.SpeziTheme
import edu.stanford.spezi.ui.theme.TextStyles
import edu.stanford.spezi.ui.theme.ThemePreviews
import edu.stanford.spezi.ui.theme.medium

/**
 * The entry point leading from a task list to the tasks the participant has missed.
 *
 * @param icon a symbol representing missed work
 * @param title the name of the destination
 * @param subtitle how much was missed and over what period, already formatted for display
 * @param onClick opens the missed tasks
 */
data class MissedTasksRow(
    val icon: ImageResource,
    val title: StringResource,
    val subtitle: StringResource,
    val onClick: () -> Unit,
) : ComposableContent {

    @Composable
    override fun Content(modifier: Modifier) {
        SpeziCard(modifier = modifier) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .noRippleClickable(onClick = onClick)
                    .padding(Spacings.medium),
                horizontalArrangement = Arrangement.spacedBy(Spacings.small),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                icon.Content(modifier = Modifier.size(Sizes.Icon.small))

                Column(verticalArrangement = Arrangement.spacedBy(Spacings.tiny)) {
                    Text(
                        text = title.text(),
                        style = TextStyles.bodyLarge.medium(),
                        color = Colors.onSurface,
                    )
                    Text(
                        text = subtitle.text(),
                        style = TextStyles.bodySmall,
                        color = Colors.onSurfaceVariant,
                    )
                }

                Spacer(modifier = Modifier.weight(1f))

                Icon(
                    imageVector = Icons.AutoMirrored.Filled.ArrowForwardIos,
                    contentDescription = null,
                    tint = Colors.onSurfaceVariant,
                    modifier = Modifier.size(Sizes.Icon.extraSmall),
                )
            }
        }
    }
}

@ThemePreviews
@Composable
private fun Preview() {
    val row = MissedTasksRow(
        icon = ImageResource(image = Icons.Default.CalendarMonth),
        title = StringResource("Missed Tasks"),
        subtitle = StringResource("3 missed tasks in the past 2 weeks"),
        onClick = {},
    )

    SpeziTheme {
        row.Content(modifier = Modifier.fillMaxWidth())
    }
}
