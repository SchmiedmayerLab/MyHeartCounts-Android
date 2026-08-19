//
// This source file is part of the My Heart Counts Android open-source project
//
// SPDX-FileCopyrightText: 2026 Stanford University and the project authors (see CONTRIBUTORS.md)
//
// SPDX-License-Identifier: MIT

package org.grovealliance.studydefinition.internal

import kotlinx.serialization.KSerializer
import kotlinx.serialization.descriptors.SerialDescriptor
import kotlinx.serialization.descriptors.buildClassSerialDescriptor
import kotlinx.serialization.encoding.Decoder
import kotlinx.serialization.encoding.Encoder
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.int
import kotlinx.serialization.json.jsonPrimitive
import org.grovealliance.scheduler.NotificationThread
import org.grovealliance.studydefinition.DateComponents
import org.grovealliance.studydefinition.NotificationsConfig
import org.grovealliance.studydefinition.OneTimeSchedule
import org.grovealliance.studydefinition.RepetitionPattern
import org.grovealliance.studydefinition.ScheduleDefinition
import org.grovealliance.studydefinition.Time
import org.grovealliance.studydefinition.Weekday

private const val HOUR = "hour"
private const val MINUTE = "minute"
private const val SECOND = "second"
private const val INTERVAL = "interval"
private const val TIME = "time"

/**
 * The time of day carried by [body], or `null` when it carries none.
 */
private fun JsonObject.timeOfDay(): Time? =
    (this[TIME] as? JsonObject)?.let { fields ->
        Time(
            hour = fields.field(HOUR),
            minute = fields.field(MINUTE),
            second = fields.field(SECOND),
        )
    }

/**
 * The integer value of [name], or zero when absent.
 */
private fun JsonObject.field(name: String): Int = this[name]?.jsonPrimitive?.int ?: 0

/**
 * The encoded form of [time], or an empty map when there is none.
 */
private fun timeFields(time: Time?): Map<String, JsonElement> = time?.let {
    mapOf(
        TIME to JsonObject(
            mapOf(
                HOUR to JsonPrimitive(it.hour),
                MINUTE to JsonPrimitive(it.minute),
                SECOND to JsonPrimitive(it.second),
            )
        )
    )
}.orEmpty()

/**
 * Reads when, and how often, a component activates.
 */
internal object ScheduleDefinitionSerializer : KSerializer<ScheduleDefinition> {
    private const val ONCE = "once"
    private const val REPEATED = "repeated"
    private const val OFFSET = "offset"

    override val descriptor: SerialDescriptor =
        buildClassSerialDescriptor("org.grovealliance.studydefinition.ScheduleDefinition")

    override fun deserialize(decoder: Decoder): ScheduleDefinition {
        val json = decoder.asJson()
        val (name, body) = SwiftCodable.decodeCase(json.decodeJsonElement())
        val payload = SwiftCodable.payload(name = name, body = body)
        return when (name) {
            ONCE -> ScheduleDefinition.Once(
                schedule = json.json.decodeFromJsonElement(OneTimeScheduleSerializer, payload)
            )
            REPEATED -> ScheduleDefinition.Repeated(
                pattern = json.json.decodeFromJsonElement(RepetitionPatternSerializer, payload),
                offset = body[OFFSET]
                    ?.let { json.json.decodeFromJsonElement(SparseDateComponentsSerializer, it) }
                    ?: DateComponents.EMPTY,
            )
            else -> error("Unknown schedule definition '$name'")
        }
    }

    override fun serialize(encoder: Encoder, value: ScheduleDefinition) {
        val json = encoder.asJson()
        val encoded = when (value) {
            is ScheduleDefinition.Once -> SwiftCodable.encodePayloadCase(
                name = ONCE,
                payload = json.json.encodeToJsonElement(OneTimeScheduleSerializer, value.schedule),
            )
            is ScheduleDefinition.Repeated -> SwiftCodable.encodeCase(
                name = REPEATED,
                body = JsonObject(
                    mapOf(
                        SwiftCodable.PAYLOAD to json.json.encodeToJsonElement(
                            RepetitionPatternSerializer,
                            value.pattern,
                        ),
                        OFFSET to json.json.encodeToJsonElement(
                            SparseDateComponentsSerializer,
                            value.offset,
                        ),
                    )
                ),
            )
        }
        json.encodeJsonElement(encoded)
    }
}

/**
 * Reads a one-time activation. The event alternative carries its anchoring event as an unlabelled
 * value alongside labelled ones.
 */
internal object OneTimeScheduleSerializer : KSerializer<OneTimeSchedule> {
    private const val DATE = "date"
    private const val EVENT = "event"
    private const val OFFSET_IN_DAYS = "offsetInDays"

    override val descriptor: SerialDescriptor =
        buildClassSerialDescriptor("org.grovealliance.studydefinition.OneTimeSchedule")

    override fun deserialize(decoder: Decoder): OneTimeSchedule {
        val json = decoder.asJson()
        val (name, body) = SwiftCodable.decodeCase(json.decodeJsonElement())
        val payload = SwiftCodable.payload(name = name, body = body)
        return when (name) {
            DATE -> OneTimeSchedule.Date(
                date = json.json.decodeFromJsonElement(SparseDateComponentsSerializer, payload)
            )
            EVENT -> OneTimeSchedule.Event(
                event = json.json.decodeFromJsonElement(StudyLifecycleEventSerializer, payload),
                offsetInDays = body.field(OFFSET_IN_DAYS),
                time = body.timeOfDay(),
            )
            else -> error("Unknown one-time schedule '$name'")
        }
    }

    override fun serialize(encoder: Encoder, value: OneTimeSchedule) {
        val json = encoder.asJson()
        val encoded = when (value) {
            is OneTimeSchedule.Date -> SwiftCodable.encodePayloadCase(
                name = DATE,
                payload = json.json.encodeToJsonElement(SparseDateComponentsSerializer, value.date),
            )
            is OneTimeSchedule.Event -> SwiftCodable.encodeCase(
                name = EVENT,
                body = JsonObject(
                    mapOf(
                        SwiftCodable.PAYLOAD to json.json.encodeToJsonElement(
                            StudyLifecycleEventSerializer,
                            value.event,
                        ),
                        OFFSET_IN_DAYS to JsonPrimitive(value.offsetInDays),
                    ) + timeFields(value.time)
                ),
            )
        }
        json.encodeJsonElement(encoded)
    }
}

/**
 * Reads how a repeated schedule recurs. Every alternative carries only labelled values.
 */
internal object RepetitionPatternSerializer : KSerializer<RepetitionPattern> {
    private const val DAILY = "daily"
    private const val WEEKLY = "weekly"
    private const val MONTHLY = "monthly"
    private const val WEEKDAY = "weekday"
    private const val DAY = "day"

    override val descriptor: SerialDescriptor =
        buildClassSerialDescriptor("org.grovealliance.studydefinition.RepetitionPattern")

    override fun deserialize(decoder: Decoder): RepetitionPattern {
        val json = decoder.asJson()
        val (name, body) = SwiftCodable.decodeCase(json.decodeJsonElement())
        val interval = body.field(INTERVAL)
        val hour = body.field(HOUR)
        val minute = body.field(MINUTE)
        val second = body.field(SECOND)
        return when (name) {
            DAILY -> RepetitionPattern.Daily(
                interval = interval,
                hour = hour,
                minute = minute,
                second = second,
            )
            WEEKLY -> RepetitionPattern.Weekly(
                interval = interval,
                weekday = body[WEEKDAY]?.let {
                    json.json.decodeFromJsonElement(Weekday.serializer(), it)
                },
                hour = hour,
                minute = minute,
                second = second,
            )
            MONTHLY -> RepetitionPattern.Monthly(
                interval = interval,
                day = body[DAY]?.jsonPrimitive?.int,
                hour = hour,
                minute = minute,
                second = second,
            )
            else -> error("Unknown repetition pattern '$name'")
        }
    }

    override fun serialize(encoder: Encoder, value: RepetitionPattern) {
        val json = encoder.asJson()
        val (name, extra) = when (value) {
            is RepetitionPattern.Daily -> DAILY to emptyMap()
            is RepetitionPattern.Weekly -> WEEKLY to value.weekday?.let {
                mapOf(WEEKDAY to json.json.encodeToJsonElement(Weekday.serializer(), it))
            }.orEmpty()
            is RepetitionPattern.Monthly -> MONTHLY to value.day?.let {
                mapOf(DAY to JsonPrimitive(it))
            }.orEmpty()
        }
        val body = mapOf(
            INTERVAL to JsonPrimitive(value.interval),
            HOUR to JsonPrimitive(value.hour),
            MINUTE to JsonPrimitive(value.minute),
            SECOND to JsonPrimitive(value.second),
        ) + extra
        json.encodeJsonElement(SwiftCodable.encodeCase(name = name, body = JsonObject(body)))
    }
}

/**
 * Reads whether occurrences of a schedule produce notifications.
 */
internal object NotificationsConfigSerializer : KSerializer<NotificationsConfig> {
    private const val DISABLED = "disabled"
    private const val ENABLED = "enabled"
    private const val THREAD = "thread"

    override val descriptor: SerialDescriptor =
        buildClassSerialDescriptor("org.grovealliance.studydefinition.NotificationsConfig")

    override fun deserialize(decoder: Decoder): NotificationsConfig {
        val json = decoder.asJson()
        val (name, body) = SwiftCodable.decodeCase(json.decodeJsonElement())
        return when (name) {
            DISABLED -> NotificationsConfig.Disabled
            ENABLED -> NotificationsConfig.Enabled(
                thread = body[THREAD]
                    ?.let { json.json.decodeFromJsonElement(NotificationThreadSerializer, it) }
                    ?: NotificationThread.None,
                time = body.timeOfDay(),
            )
            else -> error("Unknown notifications configuration '$name'")
        }
    }

    override fun serialize(encoder: Encoder, value: NotificationsConfig) {
        val json = encoder.asJson()
        val encoded = when (value) {
            is NotificationsConfig.Disabled -> SwiftCodable.encodeCase(name = DISABLED)
            is NotificationsConfig.Enabled -> SwiftCodable.encodeCase(
                name = ENABLED,
                body = JsonObject(
                    mapOf(
                        THREAD to json.json.encodeToJsonElement(
                            NotificationThreadSerializer,
                            value.thread,
                        )
                    ) + timeFields(value.time)
                ),
            )
        }
        json.encodeJsonElement(encoded)
    }
}

/**
 * Reads the grouping applied to a schedule's notifications.
 */
internal object NotificationThreadSerializer : KSerializer<NotificationThread> {
    private const val NONE = "none"
    private const val GLOBAL = "global"
    private const val TASK = "task"
    private const val CUSTOM = "custom"

    override val descriptor: SerialDescriptor =
        buildClassSerialDescriptor("org.grovealliance.studydefinition.NotificationThread")

    override fun deserialize(decoder: Decoder): NotificationThread {
        val json = decoder.asJson()
        val (name, body) = SwiftCodable.decodeCase(json.decodeJsonElement())
        return when (name) {
            NONE -> NotificationThread.None
            GLOBAL -> NotificationThread.Global
            TASK -> NotificationThread.PerTask
            CUSTOM -> NotificationThread.Custom(
                id = SwiftCodable.payload(name = name, body = body).jsonPrimitive.content
            )
            else -> error("Unknown notification thread '$name'")
        }
    }

    override fun serialize(encoder: Encoder, value: NotificationThread) {
        val encoded = when (value) {
            is NotificationThread.None -> SwiftCodable.encodeCase(name = NONE)
            is NotificationThread.Global -> SwiftCodable.encodeCase(name = GLOBAL)
            is NotificationThread.PerTask -> SwiftCodable.encodeCase(name = TASK)
            is NotificationThread.Custom -> SwiftCodable.encodePayloadCase(
                name = CUSTOM,
                payload = JsonPrimitive(value.id),
            )
        }
        encoder.asJson().encodeJsonElement(encoded)
    }
}
