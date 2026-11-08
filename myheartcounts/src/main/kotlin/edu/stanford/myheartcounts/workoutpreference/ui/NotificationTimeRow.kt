//
// This source file is part of the My Heart Counts Android open-source project
//
// SPDX-FileCopyrightText: 2026 Stanford University and the project authors (see CONTRIBUTORS.md)
//
// SPDX-License-Identifier: MIT

package edu.stanford.myheartcounts.workoutpreference.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.PreviewParameter
import androidx.compose.ui.tooling.preview.PreviewParameterProvider
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import edu.stanford.myheartcounts.ui.ValueBadge
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import org.grovealliance.ui.ComposableContent
import org.grovealliance.ui.GroveCard
import org.grovealliance.ui.StringResource
import org.grovealliance.ui.theme.Colors
import org.grovealliance.ui.theme.GroveShapes
import org.grovealliance.ui.theme.GroveTheme
import org.grovealliance.ui.theme.Spacings
import org.grovealliance.ui.theme.TextStyles
import org.grovealliance.ui.theme.ThemePreviews

/**
 * A card naming the time of day notifications are sent at, with the current time as a trailing
 * [badge].
 *
 * @param title what the time is for
 * @param subtitle further explanation below the title
 * @param badge the time as currently chosen, tapped to change it
 */
data class NotificationTimeRow(
    val title: StringResource,
    val subtitle: StringResource,
    val badge: StateFlow<ValueBadge>,
) : ComposableContent {

    @Composable
    override fun Content(modifier: Modifier) {
        val badge by badge.collectAsStateWithLifecycle()

        GroveCard(
            modifier = modifier,
            shape = GroveShapes.large,
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(Spacings.medium),
                horizontalArrangement = Arrangement.spacedBy(Spacings.medium),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Column(
                    modifier = Modifier.weight(1f),
                    verticalArrangement = Arrangement.spacedBy(Spacings.tiny),
                ) {
                    Text(
                        text = title.text(),
                        style = TextStyles.bodyLarge,
                        color = Colors.onSurface,
                    )
                    Text(
                        text = subtitle.text(),
                        style = TextStyles.bodySmall,
                        color = Colors.onSurfaceVariant,
                    )
                }
                badge.Content()
            }
        }
    }
}

private class NotificationTimeRowPreviewProvider : PreviewParameterProvider<NotificationTimeRow> {
    private val base = NotificationTimeRow(
        title = StringResource("Notification Time"),
        subtitle = StringResource("The preferred time for activity-related notifications."),
        badge = MutableStateFlow(
            ValueBadge(
                text = StringResource("09:00"),
                onClick = {},
            ),
        ),
    )
    override val values = sequenceOf(
        base,
        base.copy(
            badge = MutableStateFlow(
                ValueBadge(
                    text = StringResource("6:45 PM"),
                    onClick = {},
                ),
            ),
        ),
    )
}

@ThemePreviews
@Composable
private fun Preview(@PreviewParameter(NotificationTimeRowPreviewProvider::class) content: NotificationTimeRow) {
    GroveTheme {
        content.Content(
            modifier = Modifier
                .background(Colors.surfaceContainer)
                .padding(Spacings.medium),
        )
    }
}
