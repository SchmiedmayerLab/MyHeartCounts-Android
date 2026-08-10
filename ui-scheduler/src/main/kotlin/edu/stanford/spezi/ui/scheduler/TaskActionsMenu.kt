//
// This source file is part of the My Heart Counts Android open-source project
//
// SPDX-FileCopyrightText: 2026 Stanford University and the project authors (see CONTRIBUTORS.md)
//
// SPDX-License-Identifier: MIT

package edu.stanford.spezi.ui.scheduler

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.DirectionsRun
import androidx.compose.material.icons.automirrored.filled.DirectionsWalk
import androidx.compose.material.icons.filled.MonitorHeart
import androidx.compose.material.icons.filled.MoreHoriz
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import edu.stanford.spezi.ui.ComposableContent
import edu.stanford.spezi.ui.ImageResource
import edu.stanford.spezi.ui.SpeziIconButtonComposable
import edu.stanford.spezi.ui.StringResource
import edu.stanford.spezi.ui.theme.Sizes
import edu.stanford.spezi.ui.theme.SpeziTheme
import edu.stanford.spezi.ui.theme.TextStyles
import edu.stanford.spezi.ui.theme.ThemePreviews

/**
 * One entry of a [TaskActionsMenu].
 *
 * @param icon a symbol representing the task
 * @param title the task's name
 * @param onClick starts the task
 */
data class TaskActionMenuItem(
    val icon: ImageResource,
    val title: StringResource,
    val onClick: () -> Unit,
) : ComposableContent {

    @Composable
    override fun Content(modifier: Modifier) {
        DropdownMenuItem(
            modifier = modifier,
            leadingIcon = { icon.Content(modifier = Modifier.size(Sizes.Icon.small)) },
            text = {
                Text(
                    text = title.text(),
                    style = TextStyles.bodyMedium,
                )
            },
            onClick = onClick,
        )
    }
}

/**
 * The tasks a participant may start at any time, reached from a single button.
 *
 * @param icon the symbol of the button opening the menu
 * @param items the tasks offered, in display order
 */
data class TaskActionsMenu(
    val icon: ImageResource = ImageResource(image = Icons.Default.MoreHoriz),
    val items: List<TaskActionMenuItem>,
) : ComposableContent {

    @Composable
    override fun Content(modifier: Modifier) {
        var isExpanded by remember { mutableStateOf(false) }

        Box(modifier = modifier) {
            SpeziIconButtonComposable(
                image = icon,
                onClick = { isExpanded = true },
            )

            DropdownMenu(
                expanded = isExpanded,
                onDismissRequest = { isExpanded = false },
            ) {
                items.forEach { item ->
                    item.Content()
                }
            }
        }
    }
}

@ThemePreviews
@Composable
private fun Preview() {
    val menu = TaskActionsMenu(
        items = listOf(
            TaskActionMenuItem(
                icon = ImageResource(image = Icons.Default.MonitorHeart),
                title = StringResource("Electrocardiogram"),
                onClick = {},
            ),
            TaskActionMenuItem(
                icon = ImageResource(image = Icons.AutoMirrored.Filled.DirectionsWalk),
                title = StringResource("6-Minute Walk Test"),
                onClick = {},
            ),
            TaskActionMenuItem(
                icon = ImageResource(image = Icons.AutoMirrored.Filled.DirectionsRun),
                title = StringResource("12-Minute Run Test"),
                onClick = {},
            ),
        ),
    )

    SpeziTheme {
        menu.Content()
    }
}
