//
// This source file is part of the My Heart Counts Android open-source project
//
// SPDX-FileCopyrightText: 2026 Stanford University and the project authors (see CONTRIBUTORS.md)
//
// SPDX-License-Identifier: MIT

package edu.stanford.myheartcounts.home

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.CheckCircle
import edu.stanford.myheartcounts.MHCPlurals
import edu.stanford.myheartcounts.MHCStrings
import edu.stanford.myheartcounts.promptedactions.PromptedActionsMapper
import edu.stanford.myheartcounts.promptedactions.PromptedActionsMode
import edu.stanford.myheartcounts.task.EventTileMapper
import edu.stanford.myheartcounts.task.ui.TaskListLayout
import edu.stanford.myheartcounts.ui.ExternalLink
import org.grovealliance.ui.GroveSection
import org.grovealliance.ui.ImageResource
import org.grovealliance.ui.StringResource
import org.grovealliance.ui.scheduler.MissedTasksRow
import org.grovealliance.ui.scheduler.NoTasksContent
import org.grovealliance.ui.scheduler.NudgeBanner
import org.grovealliance.ui.scheduler.PromptedActionsCard
import org.grovealliance.ui.scheduler.TaskListHeader

/**
 * Builds the Home tab's layout from the participant's current tasks.
 */
interface HomeContentMapper {
    /**
     * Maps [input] into the layout to render.
     */
    fun map(input: HomeContentInput): HomeContentLayout
}

/**
 * Builds Home's sections, with each event's tile from [tileMapper].
 */
class HomeContentMapperImpl(
    private val tileMapper: EventTileMapper,
    private val promptedActionsMapper: PromptedActionsMapper,
) : HomeContentMapper {

    override fun map(input: HomeContentInput): HomeContentLayout = HomeContentLayout(
        nudge = input.nudge?.let { NudgeBanner(title = it.title, message = it.message) },
        promptedActions = promptedActions(input),
        tasks = tasks(input),
        missedTasks = missedTasks(input),
        learnMore = ExternalLink(
            text = StringResource(MHCStrings.home_learn_more),
            onClicked = { input.actionSink.push(HomeAction.LearnMoreSelected) },
        ),
    )

    private fun promptedActions(input: HomeContentInput): PromptedActionsCard? = promptedActionsMapper.mapCard(
        actions = input.promptedActions,
        mode = PromptedActionsMode.Pending,
        onClick = { input.actionSink.push(HomeAction.PromptedActionsSelected) },
    )

    /**
     * The tasks a participant may perform at any time, offered when nothing is scheduled for today so
     * that a clear day still leads somewhere.
     */
    private fun alwaysAvailableSections(input: HomeContentInput): List<GroveSection> {
        if (input.events.isNotEmpty()) return emptyList()
        return listOf(
            tileMapper.alwaysAvailableSection(
                onSelected = { input.actionSink.push(HomeAction.AlwaysAvailableTaskSelected(task = it)) },
            )
        )
    }

    private fun tasks(input: HomeContentInput) = TaskListLayout(
        header = TaskListHeader(title = StringResource(MHCStrings.home_todays_tasks)),
        sections = if (input.events.isEmpty()) {
            emptyList()
        } else {
            listOf(
                tileMapper.section(
                    key = TODAY_KEY,
                    title = null,
                    events = input.events,
                    onPerform = { performed, completesEvent ->
                        input.actionSink.push(
                            HomeAction.PerformTaskSelected(
                                event = performed,
                                completesEvent = completesEvent,
                            )
                        )
                    },
                )
            )
        },
        emptyState = if (input.events.isEmpty()) {
            NoTasksContent(
                icon = ImageResource(image = Icons.Default.CheckCircle),
                title = StringResource(MHCStrings.home_no_tasks_title),
            )
        } else {
            null
        },
        additionalSections = alwaysAvailableSections(input),
    )

    private fun missedTasks(input: HomeContentInput): MissedTasksRow? {
        if (input.missedEventCount == 0) return null
        return MissedTasksRow(
            icon = ImageResource(image = Icons.Default.CalendarMonth),
            title = StringResource(MHCStrings.home_missed_tasks_title),
            subtitle = StringResource.quantity(
                id = MHCPlurals.home_missed_tasks_subtitle,
                quantity = input.missedEventCount,
            ),
            onClick = { input.actionSink.push(HomeAction.MissedTasksSelected) },
        )
    }

    private companion object {
        const val TODAY_KEY = "today"
    }
}
