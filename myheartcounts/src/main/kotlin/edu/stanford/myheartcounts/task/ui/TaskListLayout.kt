//
// This source file is part of the My Heart Counts Android open-source project
//
// SPDX-FileCopyrightText: 2026 Stanford University and the project authors (see CONTRIBUTORS.md)
//
// SPDX-License-Identifier: MIT

package edu.stanford.myheartcounts.task.ui

import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Assignment
import androidx.compose.material.icons.filled.Celebration
import androidx.compose.material.icons.filled.MonitorHeart
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.PreviewParameter
import androidx.compose.ui.tooling.preview.PreviewParameterProvider
import org.grovealliance.ui.AsyncTextButton
import org.grovealliance.ui.ComposableContent
import org.grovealliance.ui.GroveSection
import org.grovealliance.ui.ImageResource
import org.grovealliance.ui.LazyListContent
import org.grovealliance.ui.StringResource
import org.grovealliance.ui.VerticalSpacer
import org.grovealliance.ui.groveSections
import org.grovealliance.ui.scheduler.NoTasksContent
import org.grovealliance.ui.scheduler.TaskListHeader
import org.grovealliance.ui.scheduler.TaskTile
import org.grovealliance.ui.scheduler.TaskTileHeader
import org.grovealliance.ui.theme.GroveTheme
import org.grovealliance.ui.theme.Spacings
import org.grovealliance.ui.theme.ThemePreviews

/**
 * A list of the participant's tasks, grouped into [GroveSection]s.
 *
 * [Content] shows the list as a screen of its own, in a lazy list it owns; [items] adds it to a lazy
 * list owned by the surrounding screen, alongside whatever else that screen shows.
 *
 * Task tiles bring their own surface, so they belong in a section as content elements rather than
 * as rows of a card. Sections adding no items take no space.
 *
 * Item keys combine [key] with the keys of the sections, so [key] must be unique within the hosting
 * list and section keys unique across [sections] and [additionalSections].
 *
 * @param key identifies the list's items within the hosting list
 * @param header what the list covers
 * @param sections the task groups, in display order; when empty the [emptyState] takes their place
 * @param emptyState what to show instead of an empty list of [sections]
 * @param additionalSections groups always shown below the list, such as tasks available at any time
 */
data class TaskListLayout(
    val key: String = "tasks",
    val header: TaskListHeader? = null,
    val sections: List<GroveSection> = emptyList(),
    val emptyState: NoTasksContent? = null,
    val additionalSections: List<GroveSection> = emptyList(),
) : ComposableContent, LazyListContent {

    @Composable
    override fun Content(modifier: Modifier) {
        val taskItems = items()
        LazyColumn(
            modifier = modifier,
            contentPadding = PaddingValues(Spacings.medium),
        ) {
            taskItems()
        }
    }

    @Composable
    override fun items(): LazyListScope.() -> Unit {
        val listed = sections.filterNot { it.isEmpty }
        val additional = additionalSections.filterNot { it.isEmpty }
        val sectionItems = (listed + additional).map { it.key to it.items() }
        val showsEmptyState = listed.isEmpty() && emptyState != null
        return {
            var hasItems = false
            header?.let { header ->
                item(key = "$key-header") { header.Content(modifier = Modifier.fillMaxWidth()) }
                hasItems = true
            }
            if (showsEmptyState) {
                gap(key = "$key-empty", isShown = hasItems)
                item(key = "$key-empty") { emptyState?.Content(modifier = Modifier.fillMaxWidth()) }
                hasItems = true
            }
            sectionItems.forEach { (sectionKey, items) ->
                gap(key = "$key-$sectionKey", isShown = hasItems)
                items()
                hasItems = true
            }
        }
    }

    private fun LazyListScope.gap(key: String, isShown: Boolean) {
        if (isShown) item(key = "$key-gap") { VerticalSpacer(height = Spacings.large) }
    }
}

private class TaskListLayoutPreviewProvider : PreviewParameterProvider<TaskListLayout> {
    private val checkIn = TaskTile(
        header = TaskTileHeader(
            icon = ImageResource(image = Icons.AutoMirrored.Filled.Assignment),
            title = StringResource("Daily Check-In"),
            categoryLabel = StringResource("Questionnaire"),
            timing = StringResource("9:00 AM"),
        ),
        instructions = StringResource("Tell us how you have been feeling over the past 24 hours."),
        action = AsyncTextButton(
            title = StringResource("Answer Survey"),
            action = {},
        ),
    )
    private val ecg = TaskTile(
        header = TaskTileHeader(
            icon = ImageResource(image = Icons.Default.MonitorHeart),
            title = StringResource("Electrocardiogram"),
            categoryLabel = StringResource("Active Task"),
        ),
        instructions = StringResource("Record a short ECG with a connected wearable device."),
        action = AsyncTextButton(
            title = StringResource("Take ECG"),
            action = {},
        ),
    )
    private val base = TaskListLayout(
        header = TaskListHeader(title = StringResource("Today's Tasks")),
        sections = groveSections {
            section(key = "today") { content(key = "check-in", content = checkIn) }
        },
        emptyState = NoTasksContent(
            icon = ImageResource(image = Icons.Default.Celebration),
            title = StringResource("You're All Set"),
            description = StringResource("All tasks have been completed!"),
        ),
        additionalSections = groveSections {
            section(key = "always-available") {
                title("Always Available")
                subtitle("Take these whenever it suits you.")
                content(key = "ecg", content = ecg)
            }
        },
    )
    override val values = sequenceOf(
        base,
        base.copy(sections = emptyList()),
    )
}

@ThemePreviews
@Composable
private fun Preview(@PreviewParameter(TaskListLayoutPreviewProvider::class) layout: TaskListLayout) {
    GroveTheme { layout.Content() }
}
