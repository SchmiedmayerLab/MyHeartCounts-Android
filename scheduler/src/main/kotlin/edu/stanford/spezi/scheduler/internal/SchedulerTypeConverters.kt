//
// This source file is part of the My Heart Counts open-source project
//
// SPDX-FileCopyrightText: 2026 Stanford University and the project authors (see CONTRIBUTORS.md)
//
// SPDX-License-Identifier: MIT

@file:Suppress("detekt:TooManyFunctions")

package edu.stanford.spezi.scheduler.internal

import androidx.room.TypeConverter
import edu.stanford.spezi.foundation.JsonSerializer
import edu.stanford.spezi.scheduler.AllowedCompletionPolicy
import edu.stanford.spezi.scheduler.NotificationThread
import edu.stanford.spezi.scheduler.NotificationTime
import edu.stanford.spezi.scheduler.OutcomeContext
import edu.stanford.spezi.scheduler.Recurrence
import edu.stanford.spezi.scheduler.RecurrenceEnd
import edu.stanford.spezi.scheduler.RecurrenceFrequency
import edu.stanford.spezi.scheduler.Schedule
import edu.stanford.spezi.scheduler.ScheduleDuration
import edu.stanford.spezi.scheduler.TaskCategory
import edu.stanford.spezi.scheduler.TaskContext
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.builtins.ListSerializer
import kotlinx.serialization.builtins.serializer
import java.time.DayOfWeek
import java.time.Duration
import java.time.Instant

/**
 * Room converters that persist the scheduler's value types as primitive columns.
 */
internal class SchedulerTypeConverters {
    private val tagsSerializer = ListSerializer(String.serializer())

    @TypeConverter
    fun fromSchedule(schedule: Schedule): String =
        JsonSerializer.encode(mapDto(schedule), ScheduleDto.serializer())

    @TypeConverter
    fun toSchedule(value: String): Schedule =
        mapModel(JsonSerializer.decode(value, ScheduleDto.serializer()))

    @TypeConverter
    fun fromTags(tags: List<String>): String = JsonSerializer.encode(tags, tagsSerializer)

    @TypeConverter
    fun toTags(value: String): List<String> = JsonSerializer.decode(value, tagsSerializer)

    @TypeConverter
    fun fromCategory(category: TaskCategory?): String? = category?.rawValue

    @TypeConverter
    fun toCategory(value: String?): TaskCategory? = value?.let { TaskCategory(it) }

    @TypeConverter
    fun fromCompletionPolicy(policy: AllowedCompletionPolicy): String = policy.name

    @TypeConverter
    fun toCompletionPolicy(value: String): AllowedCompletionPolicy = AllowedCompletionPolicy.valueOf(value)

    @TypeConverter
    fun fromTaskContext(context: TaskContext): String = context.encoded()

    @TypeConverter
    fun toTaskContext(value: String): TaskContext = TaskContext.decode(value)

    @TypeConverter
    fun fromOutcomeContext(context: OutcomeContext): String = context.encoded()

    @TypeConverter
    fun toOutcomeContext(value: String): OutcomeContext = OutcomeContext.decode(value)

    @TypeConverter
    fun fromNotificationThread(thread: NotificationThread): String = when (thread) {
        NotificationThread.None -> THREAD_NONE
        NotificationThread.Global -> THREAD_GLOBAL
        NotificationThread.PerTask -> THREAD_PER_TASK
        is NotificationThread.Custom -> "$THREAD_CUSTOM_PREFIX${thread.id}"
    }

    @TypeConverter
    fun toNotificationThread(value: String): NotificationThread = when {
        value == THREAD_GLOBAL -> NotificationThread.Global
        value == THREAD_PER_TASK -> NotificationThread.PerTask
        value.startsWith(THREAD_CUSTOM_PREFIX) -> NotificationThread.Custom(id = value.removePrefix(THREAD_CUSTOM_PREFIX))
        else -> NotificationThread.None
    }

    @TypeConverter
    fun fromNotificationTime(time: NotificationTime?): String? =
        time?.let { "${it.hour}:${it.minute}:${it.second}" }

    @TypeConverter
    fun toNotificationTime(value: String?): NotificationTime? = value?.split(":")?.let { parts ->
        NotificationTime(
            hour = parts[0].toInt(),
            minute = parts[1].toInt(),
            second = parts[2].toInt(),
        )
    }

    private fun mapDto(schedule: Schedule): ScheduleDto = ScheduleDto(
        startEpochMilli = schedule.start.toEpochMilli(),
        duration = when (val duration = schedule.duration) {
            ScheduleDuration.AllDay -> DurationDto.AllDay
            ScheduleDuration.TillEndOfDay -> DurationDto.TillEndOfDay
            is ScheduleDuration.Fixed -> DurationDto.Fixed(lengthSeconds = duration.duration.seconds)
        },
        recurrence = schedule.recurrence?.let { recurrence ->
            RecurrenceDto(
                frequency = recurrence.frequency.name,
                interval = recurrence.interval,
                weekday = recurrence.weekday?.value,
                dayOfMonth = recurrence.dayOfMonth,
                month = recurrence.month,
                end = when (val end = recurrence.end) {
                    RecurrenceEnd.Never -> RecurrenceEndDto.Never
                    is RecurrenceEnd.AfterOccurrences -> RecurrenceEndDto.AfterOccurrences(count = end.count)
                    is RecurrenceEnd.UntilDate -> RecurrenceEndDto.UntilDate(epochSecond = end.date.epochSecond)
                },
            )
        },
    )

    private fun mapModel(dto: ScheduleDto): Schedule = Schedule(
        start = Instant.ofEpochMilli(dto.startEpochMilli),
        duration = when (val duration = dto.duration) {
            DurationDto.AllDay -> ScheduleDuration.AllDay
            DurationDto.TillEndOfDay -> ScheduleDuration.TillEndOfDay
            is DurationDto.Fixed -> ScheduleDuration.Fixed(duration = Duration.ofSeconds(duration.lengthSeconds))
        },
        recurrence = dto.recurrence?.let { recurrence ->
            Recurrence(
                frequency = RecurrenceFrequency.valueOf(recurrence.frequency),
                interval = recurrence.interval,
                weekday = recurrence.weekday?.let { DayOfWeek.of(it) },
                dayOfMonth = recurrence.dayOfMonth,
                month = recurrence.month,
                end = when (val end = recurrence.end) {
                    RecurrenceEndDto.Never -> RecurrenceEnd.Never
                    is RecurrenceEndDto.AfterOccurrences -> RecurrenceEnd.AfterOccurrences(count = end.count)
                    is RecurrenceEndDto.UntilDate -> RecurrenceEnd.UntilDate(date = Instant.ofEpochSecond(end.epochSecond))
                },
            )
        },
    )

    @Serializable
    private data class ScheduleDto(
        val startEpochMilli: Long,
        val duration: DurationDto,
        val recurrence: RecurrenceDto? = null,
    )

    @Serializable
    private sealed interface DurationDto {
        @Serializable @SerialName("allDay") data object AllDay : DurationDto

        @Serializable @SerialName("tillEndOfDay") data object TillEndOfDay : DurationDto

        @Serializable @SerialName("fixed") data class Fixed(val lengthSeconds: Long) : DurationDto
    }

    @Serializable
    private data class RecurrenceDto(
        val frequency: String,
        val interval: Int,
        val weekday: Int? = null,
        val dayOfMonth: Int? = null,
        val month: Int? = null,
        val end: RecurrenceEndDto,
    )

    @Serializable
    private sealed interface RecurrenceEndDto {
        @Serializable @SerialName("never") data object Never : RecurrenceEndDto

        @Serializable @SerialName("afterOccurrences") data class AfterOccurrences(val count: Int) : RecurrenceEndDto

        @Serializable @SerialName("untilDate") data class UntilDate(val epochSecond: Long) : RecurrenceEndDto
    }

    private companion object {
        const val THREAD_NONE = "none"
        const val THREAD_GLOBAL = "global"
        const val THREAD_PER_TASK = "perTask"
        const val THREAD_CUSTOM_PREFIX = "custom:"
    }
}
