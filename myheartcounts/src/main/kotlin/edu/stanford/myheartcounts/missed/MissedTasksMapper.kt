//
// This source file is part of the My Heart Counts Android open-source project
//
// SPDX-FileCopyrightText: 2026 Stanford University and the project authors (see CONTRIBUTORS.md)
//
// SPDX-License-Identifier: MIT

package edu.stanford.myheartcounts.missed

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
 * Builds the missed tasks screen's layout from the missed events.
 */
interface MissedTasksMapper {
    /**
     * Maps [input] into the layout to render.
     */
    fun map(input: MissedTasksInput): TaskListLayout
}

/**
 * Lists the missed events in one section per day, titled with the date, with each event's tile
 * from [tileMapper]. With nothing missed, it says so and offers the tasks that may be performed at
 * any time instead.
 */
class MissedTasksMapperImpl(
    private val tileMapper: EventTileMapper,
) : MissedTasksMapper {

    override fun map(input: MissedTasksInput): TaskListLayout = TaskListLayout(
        header = TaskListHeader(
            title = StringResource(MHCStrings.missed_tasks_title),
            subtitle = StringResource(MHCStrings.missed_tasks_subtitle),
        ),
        sections = daySections(input),
        emptyState = if (input.events.isEmpty()) {
            NoTasksContent(
                icon = ImageResource(image = Icons.Default.Celebration),
                title = StringResource(MHCStrings.missed_tasks_empty_title),
            )
        } else {
            null
        },
        additionalSections = if (input.events.isEmpty()) {
            listOf(
                tileMapper.alwaysAvailableSection(
                    onSelected = { input.actionSink.push(MissedTasksAction.AlwaysAvailableTaskSelected(task = it)) },
                )
            )
        } else {
            emptyList()
        },
    )

    private fun daySections(input: MissedTasksInput): List<TaskSection> = tileMapper.daySections(
        events = input.events,
        onPerform = { performed, completesEvent ->
            input.actionSink.push(
                MissedTasksAction.PerformTaskSelected(
                    event = performed,
                    completesEvent = completesEvent,
                )
            )
        },
    )
}

/**
 * The data the missed tasks screen is built from.
 *
 * @property events The missed events, sorted by occurrence.
 */
data class MissedTasksInput(
    val events: List<Event>,
    val actionSink: ActionSink<MissedTasksAction>,
)

/**
 * An intent raised by the missed tasks screen.
 */
sealed interface MissedTasksAction {
    /**
     * The participant chose to perform [event]; [completesEvent] states whether performing it
     * completes the event.
     */
    data class PerformTaskSelected(
        val event: Event,
        val completesEvent: Boolean,
    ) : MissedTasksAction

    /**
     * The participant chose one of the tasks that may be performed at any time.
     */
    data class AlwaysAvailableTaskSelected(val task: AlwaysAvailableTask) : MissedTasksAction
}
