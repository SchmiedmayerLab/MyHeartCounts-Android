//
// This source file is part of the My Heart Counts Android open-source project
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
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Assignment
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import edu.stanford.spezi.ui.ComposableContent
import edu.stanford.spezi.ui.ImageResource
import edu.stanford.spezi.ui.StringResource
import edu.stanford.spezi.ui.theme.Colors
import edu.stanford.spezi.ui.theme.Sizes
import edu.stanford.spezi.ui.theme.Spacings
import edu.stanford.spezi.ui.theme.SpeziTheme
import edu.stanford.spezi.ui.theme.TextStyles
import edu.stanford.spezi.ui.theme.ThemePreviews
import edu.stanford.spezi.ui.theme.bold

/**
 * The heading of a [TaskTile]: what the task is, and when it is due.
 *
 * @param icon a symbol representing the kind of task
 * @param title the task's name
 * @param categoryLabel a short description of the kind of task, shown beneath the title
 * @param timing when the task is due, already formatted for display; omitted for tasks without a
 * meaningful due time
 * @param horizontalAlignment how the heading arranges itself; [Alignment.CenterHorizontally] stacks
 * the icon above the text, any other alignment places it alongside
 */
data class TaskTileHeader(
    val icon: ImageResource?,
    val title: StringResource,
    val categoryLabel: StringResource? = null,
    val timing: StringResource? = null,
    val horizontalAlignment: Alignment.Horizontal = Alignment.Start,
) : ComposableContent {

    @Composable
    override fun Content(modifier: Modifier) {
        if (horizontalAlignment == Alignment.CenterHorizontally) {
            Column(
                modifier = modifier,
                verticalArrangement = Arrangement.spacedBy(Spacings.extraSmall),
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                icon?.Content(modifier = Modifier.size(Sizes.Icon.medium))
                Title()
                StackedSubheadline()
            }
        } else {
            Row(
                modifier = modifier,
                horizontalArrangement = Arrangement.spacedBy(Spacings.small),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                icon?.Content(modifier = Modifier.size(Sizes.Icon.small))
                Column(
                    verticalArrangement = Arrangement.spacedBy(Spacings.tiny),
                    horizontalAlignment = horizontalAlignment,
                ) {
                    Title()
                    SpreadSubheadline()
                }
            }
        }
    }

    @Composable
    private fun Title() {
        Text(
            text = title.text(),
            style = TextStyles.titleMedium.bold(),
            color = Colors.onSurface,
            textAlign = if (horizontalAlignment == Alignment.CenterHorizontally) TextAlign.Center else TextAlign.Start,
        )
    }

    /**
     * The category and timing spread to opposite ends of the heading's width.
     */
    @Composable
    private fun SpreadSubheadline() {
        if (categoryLabel == null && timing == null) return
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            categoryLabel?.let { SubheadlineText(resource = it) }
            Spacer(modifier = Modifier.weight(1f))
            timing?.let { SubheadlineText(resource = it) }
        }
    }

    /**
     * The category and timing stacked beneath one another.
     */
    @Composable
    private fun StackedSubheadline() {
        categoryLabel?.let { SubheadlineText(resource = it) }
        timing?.let { SubheadlineText(resource = it) }
    }

    @Composable
    private fun SubheadlineText(resource: StringResource) {
        Text(
            text = resource.text(),
            style = TextStyles.bodySmall,
            color = Colors.onSurfaceVariant,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
    }
}

@ThemePreviews
@Composable
private fun Preview() {
    val leading = TaskTileHeader(
        icon = ImageResource(image = Icons.AutoMirrored.Filled.Assignment),
        title = StringResource("Daily Check-In"),
        categoryLabel = StringResource("Questionnaire"),
        timing = StringResource("9:00 AM"),
    )

    val centered = leading.copy(horizontalAlignment = Alignment.CenterHorizontally)

    SpeziTheme {
        Column(verticalArrangement = Arrangement.spacedBy(Spacings.large)) {
            leading.Content(modifier = Modifier.fillMaxWidth())
            centered.Content(modifier = Modifier.fillMaxWidth())
        }
    }
}
