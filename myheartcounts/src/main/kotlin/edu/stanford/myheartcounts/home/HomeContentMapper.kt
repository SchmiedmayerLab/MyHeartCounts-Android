//
// This source file is part of the My Heart Counts open-source project
//
// SPDX-FileCopyrightText: 2026 Stanford University and the project authors (see CONTRIBUTORS.md)
//
// SPDX-License-Identifier: MIT

package edu.stanford.myheartcounts.home

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Assignment
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Description
import androidx.compose.material.icons.filled.DirectionsRun
import androidx.compose.material.icons.filled.DirectionsWalk
import androidx.compose.material.icons.filled.MonitorHeart
import edu.stanford.myheartcounts.MHCPlurals
import edu.stanford.myheartcounts.MHCStrings
import edu.stanford.myheartcounts.ui.ExternalLink
import edu.stanford.spezi.scheduler.Event
import edu.stanford.spezi.scheduler.ScheduleCalculator
import edu.stanford.spezi.scheduler.ScheduleDuration
import edu.stanford.spezi.scheduler.TaskCategory
import edu.stanford.spezi.study.StudyTaskCategories
import edu.stanford.spezi.ui.AsyncTextButton
import edu.stanford.spezi.ui.ImageResource
import edu.stanford.spezi.ui.StringResource
import edu.stanford.spezi.ui.scheduler.MissedTasksRow
import edu.stanford.spezi.ui.scheduler.NoTasksContent
import edu.stanford.spezi.ui.scheduler.NudgeBanner
import edu.stanford.spezi.ui.scheduler.PromptedActionsCard
import edu.stanford.spezi.ui.scheduler.TaskListHeader
import edu.stanford.spezi.ui.scheduler.TaskListLayout
import edu.stanford.spezi.ui.scheduler.TaskSection
import edu.stanford.spezi.ui.scheduler.TaskTile
import edu.stanford.spezi.ui.scheduler.TaskTileHeader
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.time.format.FormatStyle
import java.time.temporal.ChronoUnit

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
 * Builds Home's sections, deciding per event whether its action may be performed and whether
 * performing it completes the event.
 */
class HomeContentMapperImpl(
    private val scheduleCalculator: ScheduleCalculator,
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

    private fun promptedActions(input: HomeContentInput): PromptedActionsCard? {
        val actions = input.promptedActions.ifEmpty { return null }
        return PromptedActionsCard(
            icons = actions.map { ImageResource(image = Icons.Default.CheckCircle) },
            title = StringResource(MHCStrings.home_prompted_actions_title),
            subtitle = StringResource.quantity(MHCPlurals.home_prompted_actions_subtitle, actions.size),
            onClick = { input.actionSink.push(HomeAction.PromptedActionsSelected) },
        )
    }

    /**
     * The tasks a participant may perform at any time, offered when nothing is scheduled for today so
     * that a clear day still leads somewhere.
     */
    private fun alwaysAvailableSections(input: HomeContentInput): List<TaskSection> {
        if (input.events.isNotEmpty()) return emptyList()
        val tiles = AlwaysAvailableTask.entries.map { task ->
            TaskTile(
                header = TaskTileHeader(
                    icon = alwaysAvailableIcon(task),
                    title = StringResource(task.titleId),
                    categoryLabel = task.subtitleId?.let { StringResource(it) },
                ),
                action = AsyncTextButton(
                    title = StringResource(task.actionTitleId),
                    action = { input.actionSink.push(HomeAction.AlwaysAvailableTaskSelected(task)) },
                ),
            )
        }
        return listOf(
            TaskSection(
                title = StringResource(MHCStrings.home_other_tasks_title),
                tiles = tiles,
                subtitle = StringResource(MHCStrings.home_other_tasks_subtitle),
            )
        )
    }

    private fun alwaysAvailableIcon(task: AlwaysAvailableTask) = when (task) {
        AlwaysAvailableTask.ELECTROCARDIOGRAM -> ImageResource(image = Icons.Default.MonitorHeart)
        AlwaysAvailableTask.SIX_MINUTE_WALK -> ImageResource(image = Icons.Default.DirectionsWalk)
        AlwaysAvailableTask.TWELVE_MINUTE_RUN -> ImageResource(image = Icons.Default.DirectionsRun)
    }

    private fun tasks(input: HomeContentInput) = TaskListLayout(
        header = TaskListHeader(title = StringResource(MHCStrings.home_todays_tasks)),
        sections = if (input.events.isEmpty()) {
            emptyList()
        } else {
            listOf(TaskSection(title = null, tiles = input.events.map { tile(it, input) }))
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

    private fun tile(event: Event, input: HomeContentInput): TaskTile {
        val category = event.task.category
        val interaction = interaction(event, input)
        return TaskTile(
            header = TaskTileHeader(
                icon = categoryIcon(category),
                title = StringResource(event.task.title),
                categoryLabel = categoryLabel(category),
                timing = timing(event, input.zone),
            ),
            instructions = event.task.instructions.takeIf { it.isNotBlank() }?.let { StringResource(it) },
            action = actionButton(event, interaction, input),
            isCompleted = event.isCompleted,
        )
    }

    /**
     * The action offered on the tile, or `null` when none is.
     *
     * A completed event keeps its action only while it may still be performed, which is the case for
     * active tasks; anything else that is done offers no action at all rather than a dormant one.
     */
    private fun actionButton(
        event: Event,
        interaction: EventInteraction,
        input: HomeContentInput,
    ): AsyncTextButton? {
        val canPerform = interaction as? EventInteraction.CanPerform
        if (event.isCompleted && canPerform == null) return null
        val title = actionTitle(
            category = event.task.category,
            isEnabled = canPerform != null,
            wouldBeFirstCompletion = !event.isCompleted,
        ) ?: return null
        return AsyncTextButton(
            title = title,
            enabled = canPerform != null,
            action = { perform(event = event, interaction = canPerform, input = input) },
        )
    }

    /**
     * Performs an event's action.
     *
     * An article is presented rather than completed here, since it is the presentation that
     * completes it; every other kind of task is completed directly.
     */
    private suspend fun perform(event: Event, interaction: EventInteraction.CanPerform?, input: HomeContentInput) {
        val completesEvent = interaction?.completesEvent == true
        if (event.task.category == StudyTaskCategories.informational) {
            input.actionSink.push(
                HomeAction.ReadArticleSelected(
                    event = event,
                    completesEvent = completesEvent,
                )
            )
        } else if (completesEvent) {
            input.onCompleteEvent(event)
        }
    }

    /**
     * The start time of a timed occurrence; all-day occurrences carry no time.
     */
    private fun timing(event: Event, zone: ZoneId): StringResource? {
        if (event.occurrence.schedule.duration is ScheduleDuration.AllDay) return null
        val formatted = TIME_FORMAT.withZone(zone).format(event.occurrence.start)
        return StringResource(formatted)
    }

    /**
     * Whether the event's action may be performed, and whether performing it completes the event.
     */
    private fun interaction(event: Event, input: HomeContentInput): EventInteraction {
        val isActiveTask = event.task.category in ACTIVE_TASK_CATEGORIES
        if (event.isCompleted) {
            // Active tasks may always be repeated, but repeating never re-completes them.
            return if (isActiveTask) EventInteraction.CanPerform(completesEvent = false) else EventInteraction.Disabled
        }
        val occursToday = event.occurrence.start.atZone(input.zone).toLocalDate() ==
            input.now.atZone(input.zone).toLocalDate()
        if (occursToday) {
            return if (scheduleCalculator.isAllowedToComplete(event)) {
                EventInteraction.CanPerform(completesEvent = true)
            } else {
                EventInteraction.Disabled
            }
        }
        val daysAway = ChronoUnit.DAYS.between(
            event.occurrence.start.atZone(input.zone).toLocalDate(),
            input.now.atZone(input.zone).toLocalDate(),
        ).let { kotlin.math.abs(it) }
        return EventInteraction.CanPerform(
            completesEvent = !isActiveTask && daysAway <= MAX_DAYS_TO_COMPLETE_LATE,
        )
    }

    /**
     * The symbol representing the kind of task, or `null` when the category has no defined symbol.
     */
    private fun categoryIcon(category: TaskCategory?): ImageResource? = when (category) {
        StudyTaskCategories.informational -> ImageResource(image = Icons.Default.Description)
        StudyTaskCategories.questionnaire -> ImageResource(image = Icons.AutoMirrored.Filled.Assignment)
        StudyTaskCategories.timedWalkingTest -> ImageResource(image = Icons.Default.DirectionsWalk)
        StudyTaskCategories.timedRunningTest -> ImageResource(image = Icons.Default.DirectionsRun)
        StudyTaskCategories.customActiveTask(ECG_IDENTIFIER) -> ImageResource(image = Icons.Default.MonitorHeart)
        else -> null
    }

    /**
     * The label naming the kind of task, or `null` when the category has no defined label.
     */
    private fun categoryLabel(category: TaskCategory?): StringResource? = when (category) {
        StudyTaskCategories.informational -> StringResource(MHCStrings.task_category_informational)
        StudyTaskCategories.questionnaire -> StringResource(MHCStrings.task_category_questionnaire)
        StudyTaskCategories.timedWalkingTest,
        StudyTaskCategories.timedRunningTest,
        StudyTaskCategories.customActiveTask(ECG_IDENTIFIER),
        -> StringResource(MHCStrings.task_category_active_task)
        else -> null
    }

    private fun actionTitle(
        category: TaskCategory?,
        isEnabled: Boolean,
        wouldBeFirstCompletion: Boolean,
    ): StringResource? {
        val repeat = isEnabled && !wouldBeFirstCompletion
        return when (category) {
            StudyTaskCategories.informational -> StringResource(
                if (repeat) MHCStrings.task_action_read_article_again else MHCStrings.task_action_read_article
            )
            StudyTaskCategories.questionnaire -> StringResource(
                if (repeat) MHCStrings.task_action_answer_survey_again else MHCStrings.task_action_answer_survey
            )
            StudyTaskCategories.timedWalkingTest, StudyTaskCategories.timedRunningTest -> StringResource(
                if (repeat) MHCStrings.task_action_take_test_again else MHCStrings.task_action_take_test
            )
            StudyTaskCategories.customActiveTask(ECG_IDENTIFIER) -> StringResource(MHCStrings.task_action_take_ecg)
            else -> null
        }
    }

    private companion object {
        const val MAX_DAYS_TO_COMPLETE_LATE = 14L
        const val ECG_IDENTIFIER = "edu.stanford.MyHeartCounts.activeTask.ecg"
        val TIME_FORMAT: DateTimeFormatter = DateTimeFormatter.ofLocalizedTime(FormatStyle.SHORT)
        val ACTIVE_TASK_CATEGORIES = setOf(
            StudyTaskCategories.timedWalkingTest,
            StudyTaskCategories.timedRunningTest,
        )
    }
}

/**
 * Whether an event's action may be performed, and what performing it does.
 */
private sealed interface EventInteraction {
    /**
     * The action may not be performed.
     */
    data object Disabled : EventInteraction

    /**
     * The action may be performed; [completesEvent] states whether doing so completes the event.
     */
    data class CanPerform(val completesEvent: Boolean) : EventInteraction
}
