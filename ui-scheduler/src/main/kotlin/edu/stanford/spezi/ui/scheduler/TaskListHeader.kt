//
// This source file is part of the My Heart Counts Android open-source project
//
// SPDX-FileCopyrightText: 2026 Stanford University and the project authors (see CONTRIBUTORS.md)
//
// SPDX-License-Identifier: MIT

package edu.stanford.spezi.ui.scheduler

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import edu.stanford.spezi.ui.ComposableContent
import edu.stanford.spezi.ui.StringResource
import edu.stanford.spezi.ui.theme.Colors
import edu.stanford.spezi.ui.theme.Spacings
import edu.stanford.spezi.ui.theme.SpeziTheme
import edu.stanford.spezi.ui.theme.TextStyles
import edu.stanford.spezi.ui.theme.ThemePreviews
import edu.stanford.spezi.ui.theme.bold

/**
 * The heading introducing a list of tasks.
 *
 * @param title what the list covers
 * @param subtitle a qualifier such as the period the list spans, already formatted for display
 */
data class TaskListHeader(
    val title: StringResource,
    val subtitle: StringResource? = null,
) : ComposableContent {

    @Composable
    override fun Content(modifier: Modifier) {
        Column(
            modifier = modifier,
            verticalArrangement = Arrangement.spacedBy(Spacings.tiny),
        ) {
            Text(
                text = title.text(),
                style = TextStyles.titleLarge.bold(),
                color = Colors.onSurface,
            )
            subtitle?.let {
                Text(
                    text = it.text(),
                    style = TextStyles.bodySmall,
                    color = Colors.onSurfaceVariant,
                )
            }
        }
    }
}

@ThemePreviews
@Composable
private fun Preview() {
    val header = TaskListHeader(
        title = StringResource("Next 2 Weeks"),
        subtitle = StringResource("01/08/2026 – 15/08/2026"),
    )

    SpeziTheme {
        header.Content(modifier = Modifier.fillMaxWidth())
    }
}
