//
// This source file is part of the My Heart Counts open-source project
//
// SPDX-FileCopyrightText: 2026 Stanford University and the project authors (see CONTRIBUTORS.md)
//
// SPDX-License-Identifier: MIT

package edu.stanford.spezi.studydefinition

import edu.stanford.spezi.foundation.UUIDSerializer
import edu.stanford.spezi.scheduler.AllowedCompletionPolicy
import edu.stanford.spezi.scheduler.NotificationThread
import edu.stanford.spezi.studydefinition.internal.CompletionPolicySerializer
import edu.stanford.spezi.studydefinition.internal.NotificationThreadSerializer
import edu.stanford.spezi.studydefinition.internal.NotificationsConfigSerializer
import edu.stanford.spezi.studydefinition.internal.OneTimeScheduleSerializer
import edu.stanford.spezi.studydefinition.internal.RepetitionPatternSerializer
import edu.stanford.spezi.studydefinition.internal.ScheduleDefinitionSerializer
import edu.stanford.spezi.studydefinition.internal.SparseDateComponentsSerializer
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import java.time.DayOfWeek
import java.util.UUID

/**
 * Associates a [Component] with a [ScheduleDefinition], defining when the component activates.
 *
 * Schedules apply only to user-interactive components; health data collection is always implicitly
 * active for the whole enrollment.
 */
@Serializable
data class ComponentSchedule(
    @Serializable(with = UUIDSerializer::class)
    val id: UUID,
    @Serializable(with = UUIDSerializer::class)
    val componentId: UUID,
    val scheduleDefinition: ScheduleDefinition,
    @Serializable(with = CompletionPolicySerializer::class)
    val completionPolicy: AllowedCompletionPolicy,
    val notifications: NotificationsConfig,
)

/**
 * When, and how often, a component activates.
 */
@Serializable(with = ScheduleDefinitionSerializer::class)
sealed interface ScheduleDefinition {
    /**
     * A schedule that is not inherently repetitive; it activates in response to a single point in
     * time or a single lifecycle event.
     */
    @Serializable
    @SerialName("once")
    data class Once(val schedule: OneTimeSchedule) : ScheduleDefinition

    /**
     * A schedule that repeats according to [pattern], first taking effect [offset] after enrollment.
     */
    @Serializable
    @SerialName("repeated")
    data class Repeated(
        val pattern: RepetitionPattern,
        val offset: DateComponents,
    ) : ScheduleDefinition
}

/**
 * A one-time activation, anchored either to a calendar date or a lifecycle event.
 */
@Serializable(with = OneTimeScheduleSerializer::class)
sealed interface OneTimeSchedule {
    /**
     * Activates once, on the given time-zone-independent [date].
     */
    @Serializable
    @SerialName("date")
    data class Date(val date: DateComponents) : OneTimeSchedule

    /**
     * Activates once, [offsetInDays] after [event] occurs, optionally at [time].
     */
    @Serializable
    @SerialName("event")
    data class Event(
        val event: StudyLifecycleEvent,
        val offsetInDays: Int,
        val time: Time?,
    ) : OneTimeSchedule
}

/**
 * How a [ScheduleDefinition.Repeated] repeats.
 */
@Serializable(with = RepetitionPatternSerializer::class)
sealed interface RepetitionPattern {
    /**
     * How many periods elapse between two consecutive occurrences.
     */
    val interval: Int

    /**
     * The hour of day at which an occurrence starts.
     */
    val hour: Int

    /**
     * The minute of the hour at which an occurrence starts.
     */
    val minute: Int

    /**
     * The second of the minute at which an occurrence starts.
     */
    val second: Int

    /**
     * Repeats every [interval] days at the given time of day.
     */
    @Serializable
    @SerialName("daily")
    data class Daily(
        override val interval: Int,
        override val hour: Int,
        override val minute: Int,
        override val second: Int,
    ) : RepetitionPattern

    /**
     * Repeats every [interval] weeks on [weekday] (or the enrollment weekday when `null`).
     */
    @Serializable
    @SerialName("weekly")
    data class Weekly(
        override val interval: Int,
        val weekday: Weekday?,
        override val hour: Int,
        override val minute: Int,
        override val second: Int,
    ) : RepetitionPattern

    /**
     * Repeats every [interval] months on [day] of the month (or the enrollment day when `null`).
     */
    @Serializable
    @SerialName("monthly")
    data class Monthly(
        override val interval: Int,
        val day: Int?,
        override val hour: Int,
        override val minute: Int,
        override val second: Int,
    ) : RepetitionPattern
}

/**
 * A day of the week.
 */
@Serializable
enum class Weekday {
    @SerialName("monday")
    MONDAY,

    @SerialName("tuesday")
    TUESDAY,

    @SerialName("wednesday")
    WEDNESDAY,

    @SerialName("thursday")
    THURSDAY,

    @SerialName("friday")
    FRIDAY,

    @SerialName("saturday")
    SATURDAY,

    @SerialName("sunday")
    SUNDAY,
    ;

    /**
     * The corresponding [DayOfWeek].
     */
    fun toDayOfWeek(): DayOfWeek = when (this) {
        MONDAY -> DayOfWeek.MONDAY
        TUESDAY -> DayOfWeek.TUESDAY
        WEDNESDAY -> DayOfWeek.WEDNESDAY
        THURSDAY -> DayOfWeek.THURSDAY
        FRIDAY -> DayOfWeek.FRIDAY
        SATURDAY -> DayOfWeek.SATURDAY
        SUNDAY -> DayOfWeek.SUNDAY
    }
}

/**
 * A time of day.
 */
@Serializable
data class Time(
    val hour: Int,
    val minute: Int,
    val second: Int,
)

/**
 * A time-zone-independent calendar date, optionally with a time of day.
 *
 * Fields that carry no value are zero.
 */
@Serializable(with = SparseDateComponentsSerializer::class)
data class DateComponents(
    val year: Int,
    val month: Int,
    val day: Int,
    val hour: Int,
    val minute: Int,
    val second: Int,
) {
    companion object {
        /**
         * Components that all carry no value.
         */
        val EMPTY = DateComponents(
            year = 0,
            month = 0,
            day = 0,
            hour = 0,
            minute = 0,
            second = 0,
        )
    }
}

/**
 * Whether occurrences of a schedule produce notifications.
 */
@Serializable(with = NotificationsConfigSerializer::class)
sealed interface NotificationsConfig {
    /**
     * No notifications are produced.
     */
    @Serializable
    @SerialName("disabled")
    data object Disabled : NotificationsConfig

    /**
     * Notifications are produced, grouped by [thread] and optionally overridden to [time].
     */
    @Serializable
    @SerialName("enabled")
    data class Enabled(
        @Serializable(with = NotificationThreadSerializer::class)
        val thread: NotificationThread,
        val time: Time?,
    ) : NotificationsConfig
}
