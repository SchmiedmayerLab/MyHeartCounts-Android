//
// This source file is part of the My Heart Counts Android open-source project
//
// SPDX-FileCopyrightText: 2026 Stanford University and the project authors (see CONTRIBUTORS.md)
//
// SPDX-License-Identifier: MIT

package edu.stanford.myheartcounts.upcoming

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Celebration
import edu.stanford.myheartcounts.MHCStrings
import edu.stanford.myheartcounts.task.AlwaysAvailableTask
import edu.stanford.myheartcounts.task.EventTileMapper
import org.grovealliance.scheduler.Event
import org.grovealliance.ui.ActionSink
import org.grovealliance.ui.ImageResource
import org.grovealliance.ui.StringResource
import org.grovealliance.ui.scheduler.NoTasksContent
import org.grovealliance.ui.scheduler.TaskListHeader
import org.grovealliance.ui.scheduler.TaskListLayout
import org.grovealliance.ui.scheduler.TaskSection

/**
 * Builds the upcoming tasks tab's layout from the upcoming events.
 */
interface UpcomingTasksMapper {
    /**
     * Maps [input] into the layout to render.
     */
    fun map(input: UpcomingTasksInput): TaskListLayout
}

/**
 * Lists the upcoming events in one section per day, with each event's tile from [tileMapper],
 * followed by the tasks that may be performed at any time and are not already listed.
 */
class UpcomingTasksMapperImpl(
    private val tileMapper: EventTileMapper,
) : UpcomingTasksMapper {

    override fun map(input: UpcomingTasksInput): TaskListLayout = TaskListLayout(
        header = TaskListHeader(title = StringResource(MHCStrings.upcoming_tasks_title)),
        sections = tileMapper.daySections(
            events = input.events,
            onPerform = { performed, completesEvent ->
                input.actionSink.push(
                    UpcomingTasksAction.PerformTaskSelected(
                        event = performed,
                        completesEvent = completesEvent,
                    )
                )
            },
        ),
        emptyState = if (input.events.isEmpty()) {
            NoTasksContent(
                icon = ImageResource(image = Icons.Default.Celebration),
                title = StringResource(MHCStrings.upcoming_tasks_empty_title),
            )
        } else {
            null
        },
        additionalSections = alwaysAvailableSections(input),
    )

    private fun alwaysAvailableSections(input: UpcomingTasksInput): List<TaskSection> {
        val tasks = AlwaysAvailableTask.entries
            .filterNot { it.isScheduledIn(events = input.events) }
            .ifEmpty { return emptyList() }
        return listOf(
            tileMapper.alwaysAvailableSection(
                tasks = tasks,
                onSelected = { input.actionSink.push(UpcomingTasksAction.AlwaysAvailableTaskSelected(task = it)) },
            )
        )
    }
}

/**
 * The data the upcoming tasks tab is built from.
 *
 * @property events The upcoming events, sorted by occurrence.
 */
data class UpcomingTasksInput(
    val events: List<Event>,
    val actionSink: ActionSink<UpcomingTasksAction>,
)

/**
 * An intent raised by the upcoming tasks tab.
 */
sealed interface UpcomingTasksAction {
    /**
     * The participant chose to perform [event]; [completesEvent] states whether performing it
     * completes the event.
     */
    data class PerformTaskSelected(
        val event: Event,
        val completesEvent: Boolean,
    ) : UpcomingTasksAction

    /**
     * The participant chose one of the tasks that may be performed at any time.
     */
    data class AlwaysAvailableTaskSelected(val task: AlwaysAvailableTask) : UpcomingTasksAction
}
