//
// This source file is part of the My Heart Counts Android open-source project
//
// SPDX-FileCopyrightText: 2026 Stanford University and the project authors (see CONTRIBUTORS.md)
//
// SPDX-License-Identifier: MIT

package edu.stanford.myheartcounts.task

import androidx.annotation.PluralsRes
import androidx.annotation.StringRes
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Assignment
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Description
import androidx.compose.material.icons.filled.DirectionsRun
import androidx.compose.material.icons.filled.DirectionsWalk
import androidx.compose.material.icons.filled.MonitorHeart
import edu.stanford.myheartcounts.MHCPlurals
import edu.stanford.myheartcounts.MHCStrings
import org.grovealliance.core.time.TimeProvider
import org.grovealliance.scheduler.Event
import org.grovealliance.scheduler.ScheduleCalculator
import org.grovealliance.scheduler.ScheduleDuration
import org.grovealliance.scheduler.TaskCategory
import org.grovealliance.study.StudyTaskCategories
import org.grovealliance.ui.AsyncTextButton
import org.grovealliance.ui.ImageResource
import org.grovealliance.ui.StringResource
import org.grovealliance.ui.scheduler.TaskActionMenuItem
import org.grovealliance.ui.scheduler.TaskActionsMenu
import org.grovealliance.ui.scheduler.TaskSection
import org.grovealliance.ui.scheduler.TaskTile
import org.grovealliance.ui.scheduler.TaskTileHeader
import java.time.Duration
import java.time.ZonedDateTime
import java.time.format.DateTimeFormatter
import java.time.format.FormatStyle
import java.time.temporal.ChronoUnit
import java.time.temporal.WeekFields
import java.util.Locale
import kotlin.math.abs
import kotlin.math.max
import kotlin.math.min
import kotlin.math.roundToInt

/**
 * Builds the tiles of the participant's tasks, deciding per event whether its action may be
 * performed and whether performing it completes the event.
 */
interface EventTileMapper {
    /**
     * The tile of [event] as of now. Its action, when one may be performed, calls [onPerform] with
     * the event and whether performing it completes the event.
     */
    fun tile(
        event: Event,
        onPerform: (event: Event, completesEvent: Boolean) -> Unit,
    ): TaskTile

    /**
     * The tiles of [events] in one section per day they occur on, titled with the date and in the
     * order given; see [tile].
     */
    fun daySections(
        events: List<Event>,
        onPerform: (event: Event, completesEvent: Boolean) -> Unit,
    ): List<TaskSection>

    /**
     * The section offering [tasks], which may be performed at any time, each calling [onSelected]
     * when chosen.
     */
    fun alwaysAvailableSection(
        tasks: List<AlwaysAvailableTask> = AlwaysAvailableTask.entries,
        onSelected: (AlwaysAvailableTask) -> Unit,
    ): TaskSection

    /**
     * A menu offering every task that may be performed at any time, each calling [onSelected] when
     * chosen.
     */
    fun alwaysAvailableMenu(onSelected: (AlwaysAvailableTask) -> Unit): TaskActionsMenu
}

/**
 * Lets an event be performed when its category has a performer: today's once its completion policy
 * allows, any other day's at any time. Performing an event that is not yet completed completes it.
 * Active tasks may be repeated once completed, but repeating them never completes them again.
 */
class EventTileMapperImpl(
    private val scheduleCalculator: ScheduleCalculator,
    private val timeProvider: TimeProvider,
) : EventTileMapper {

    override fun tile(
        event: Event,
        onPerform: (event: Event, completesEvent: Boolean) -> Unit,
    ): TaskTile {
        val category = event.task.category
        return TaskTile(
            header = TaskTileHeader(
                icon = categoryIcon(category),
                title = StringResource(event.task.title),
                categoryLabel = categoryLabel(category),
                timing = timing(event = event),
            ),
            instructions = event.task.instructions.takeIf { it.isNotBlank() }?.let { StringResource(it) },
            action = actionButton(
                event = event,
                interaction = interaction(event = event),
                onPerform = onPerform,
            ),
            isCompleted = event.isCompleted,
        )
    }

    override fun daySections(
        events: List<Event>,
        onPerform: (event: Event, completesEvent: Boolean) -> Unit,
    ): List<TaskSection> = events
        .groupBy { it.occurrence.start.atZone(timeProvider.currentZone()).toLocalDate() }
        .map { (day, dayEvents) ->
            TaskSection(
                title = StringResource(DAY_FORMAT.format(day)),
                tiles = dayEvents.map { event ->
                    tile(
                        event = event,
                        onPerform = onPerform,
                    )
                },
            )
        }

    override fun alwaysAvailableSection(
        tasks: List<AlwaysAvailableTask>,
        onSelected: (AlwaysAvailableTask) -> Unit,
    ): TaskSection {
        val tiles = tasks.map { task ->
            TaskTile(
                header = TaskTileHeader(
                    icon = alwaysAvailableIcon(task),
                    title = StringResource(task.titleId),
                    categoryLabel = task.subtitleId?.let { StringResource(it) },
                ),
                action = AsyncTextButton(
                    title = StringResource(task.actionTitleId),
                    action = { onSelected(task) },
                ),
            )
        }
        return TaskSection(
            title = StringResource(MHCStrings.home_other_tasks_title),
            tiles = tiles,
            subtitle = StringResource(MHCStrings.home_other_tasks_subtitle),
        )
    }

    override fun alwaysAvailableMenu(onSelected: (AlwaysAvailableTask) -> Unit): TaskActionsMenu = TaskActionsMenu(
        icon = ImageResource(image = Icons.Default.Add),
        items = AlwaysAvailableTask.entries.map { task ->
            TaskActionMenuItem(
                icon = alwaysAvailableIcon(task),
                title = StringResource(task.titleId),
                onClick = { onSelected(task) },
            )
        },
    )

    private fun alwaysAvailableIcon(task: AlwaysAvailableTask) = when (task) {
        AlwaysAvailableTask.ELECTROCARDIOGRAM -> ImageResource(image = Icons.Default.MonitorHeart)
        AlwaysAvailableTask.SIX_MINUTE_WALK -> ImageResource(image = Icons.Default.DirectionsWalk)
        AlwaysAvailableTask.TWELVE_MINUTE_RUN -> ImageResource(image = Icons.Default.DirectionsRun)
    }

    /**
     * The action offered on the tile, or `null` when none is.
     *
     * A completed event keeps its action only while it may still be performed, which is the case for
     * active tasks; anything else that is done offers no action at all rather than a dormant one.
     * Performing an event that is not yet completed always completes it.
     */
    private fun actionButton(
        event: Event,
        interaction: EventInteraction,
        onPerform: (event: Event, completesEvent: Boolean) -> Unit,
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
            action = { canPerform?.let { onPerform(event, !event.isCompleted || it.completesEvent) } },
        )
    }

    /**
     * When the occurrence takes place: its start time, or its start and end time when it has a fixed
     * duration, preceded by how far away its day is when that is not today. All-day occurrences, and
     * those lasting the whole of today, carry none.
     */
    private fun timing(event: Event): StringResource? {
        val zone = timeProvider.currentZone()
        val now = timeProvider.nowInstant().atZone(zone)
        val start = event.occurrence.start.atZone(zone)
        val time = timeOfDay(
            event = event,
            start = start,
            now = now,
        ) ?: return null
        if (start.toLocalDate() == now.toLocalDate()) return time
        return StringResource(MHCStrings.task_timing_with_offset, distance(start = start, now = now), time)
    }

    /**
     * How far [start] is from [now], in the largest unit that applies: hours while less than a day
     * away, then calendar days, calendar weeks and calendar months, and the year once it differs.
     * Weeks give way to months from four calendar weeks away.
     */
    private fun distance(
        start: ZonedDateTime,
        now: ZonedDateTime,
    ): StringResource {
        val hours = (Duration.between(now, start).toMinutes() / MINUTES_PER_HOUR).roundToInt()
        val days = ChronoUnit.DAYS.between(now.toLocalDate(), start.toLocalDate()).toInt()
        val firstDayOfWeek = WeekFields.of(Locale.getDefault()).dayOfWeek()
        val weeks = ChronoUnit.DAYS.between(
            now.toLocalDate().with(firstDayOfWeek, 1),
            start.toLocalDate().with(firstDayOfWeek, 1),
        ).toInt() / DAYS_PER_WEEK
        val months = (start.year - now.year) * MONTHS_PER_YEAR + start.monthValue - now.monthValue
        return when {
            abs(hours) < HOURS_PER_DAY -> distance(
                amount = if (hours < 0) min(hours, -1) else max(hours, 1),
                ago = MHCPlurals.task_timing_hours_ago,
                ahead = MHCPlurals.task_timing_hours_ahead,
            )
            abs(days) < DAYS_PER_WEEK -> distance(
                amount = days,
                ago = MHCPlurals.task_timing_days_ago,
                ahead = MHCPlurals.task_timing_days_ahead,
                previous = MHCStrings.task_timing_yesterday,
                next = MHCStrings.task_timing_tomorrow,
            )
            abs(weeks) < WEEKS_BEFORE_MONTHS || months == 0 -> distance(
                amount = weeks,
                ago = MHCPlurals.task_timing_weeks_ago,
                ahead = MHCPlurals.task_timing_weeks_ahead,
                previous = MHCStrings.task_timing_last_week,
                next = MHCStrings.task_timing_next_week,
            )
            start.year != now.year -> StringResource(start.year.toString())
            else -> distance(
                amount = months,
                ago = MHCPlurals.task_timing_months_ago,
                ahead = MHCPlurals.task_timing_months_ahead,
                previous = MHCStrings.task_timing_last_month,
                next = MHCStrings.task_timing_next_month,
            )
        }
    }

    /**
     * [amount] units away, negative in the past; one unit either way reads as [previous] or [next]
     * when given.
     */
    private fun distance(
        amount: Int,
        @PluralsRes ago: Int,
        @PluralsRes ahead: Int,
        @StringRes previous: Int? = null,
        @StringRes next: Int? = null,
    ): StringResource {
        val count = abs(amount)
        val adjacent = if (amount < 0) previous else next
        return when {
            count == 1 && adjacent != null -> StringResource(adjacent)
            amount < 0 -> StringResource.quantity(ago, count, count)
            else -> StringResource.quantity(ahead, count, count)
        }
    }

    private fun timeOfDay(
        event: Event,
        start: ZonedDateTime,
        now: ZonedDateTime,
    ): StringResource? = when (event.occurrence.schedule.duration) {
        ScheduleDuration.AllDay -> null
        ScheduleDuration.TillEndOfDay -> if (start == now.toLocalDate().atStartOfDay(start.zone)) {
            null
        } else {
            StringResource(TIME_FORMAT.format(start))
        }
        is ScheduleDuration.Fixed -> StringResource(
            MHCStrings.task_timing_time_range,
            TIME_FORMAT.format(start),
            TIME_FORMAT.format(event.occurrence.end.atZone(start.zone)),
        )
    }

    /**
     * Whether the event's action may be performed, and whether performing it completes the event.
     */
    private fun interaction(event: Event): EventInteraction {
        val zone = timeProvider.currentZone()
        val isActiveTask = event.task.category in ACTIVE_TASK_CATEGORIES
        val occurrenceDay = event.occurrence.start.atZone(zone).toLocalDate()
        val today = timeProvider.nowInstant().atZone(zone).toLocalDate()
        return when {
            // Only articles and questionnaires can be performed. Completing any other kind from here
            // would record an activity the participant never performed.
            event.task.category !in PERFORMABLE_CATEGORIES -> EventInteraction.Disabled
            // Active tasks may always be repeated, but repeating never re-completes them.
            event.isCompleted -> if (isActiveTask) {
                EventInteraction.CanPerform(completesEvent = false)
            } else {
                EventInteraction.Disabled
            }
            occurrenceDay == today -> if (scheduleCalculator.isAllowedToComplete(event)) {
                EventInteraction.CanPerform(completesEvent = true)
            } else {
                EventInteraction.Disabled
            }
            else -> EventInteraction.CanPerform(
                completesEvent = !isActiveTask &&
                    kotlin.math.abs(ChronoUnit.DAYS.between(occurrenceDay, today)) <= MAX_DAYS_TO_COMPLETE_LATE,
            )
        }
    }

    /**
     * The symbol representing the kind of task, or `null` when the category has no defined symbol.
     */
    private fun categoryIcon(category: TaskCategory?): ImageResource? = when (category) {
        StudyTaskCategories.informational -> ImageResource(image = Icons.Default.Description)
        StudyTaskCategories.questionnaire -> ImageResource(image = Icons.AutoMirrored.Filled.Assignment)
        StudyTaskCategories.timedWalkingTest -> ImageResource(image = Icons.Default.DirectionsWalk)
        StudyTaskCategories.timedRunningTest -> ImageResource(image = Icons.Default.DirectionsRun)
        StudyTaskCategories.customActiveTask(AlwaysAvailableTask.ECG_IDENTIFIER) ->
            ImageResource(image = Icons.Default.MonitorHeart)
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
        StudyTaskCategories.customActiveTask(AlwaysAvailableTask.ECG_IDENTIFIER),
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
            StudyTaskCategories.customActiveTask(AlwaysAvailableTask.ECG_IDENTIFIER) ->
                StringResource(MHCStrings.task_action_take_ecg)
            else -> null
        }
    }

    private companion object {
        const val MAX_DAYS_TO_COMPLETE_LATE = 14L
        const val MINUTES_PER_HOUR = 60.0
        const val HOURS_PER_DAY = 24
        const val DAYS_PER_WEEK = 7
        const val MONTHS_PER_YEAR = 12
        const val WEEKS_BEFORE_MONTHS = 4
        val TIME_FORMAT: DateTimeFormatter = DateTimeFormatter.ofLocalizedTime(FormatStyle.SHORT)
        val DAY_FORMAT: DateTimeFormatter = DateTimeFormatter.ofLocalizedDate(FormatStyle.LONG)
        val PERFORMABLE_CATEGORIES = setOf(
            StudyTaskCategories.informational,
            StudyTaskCategories.questionnaire,
        )
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
