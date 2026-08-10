//
// This source file is part of the My Heart Counts Android open-source project
//
// SPDX-FileCopyrightText: 2026 Stanford University and the project authors (see CONTRIBUTORS.md)
//
// SPDX-License-Identifier: MIT

package edu.stanford.spezi.studydefinition.internal

import edu.stanford.spezi.studydefinition.Component
import edu.stanford.spezi.studydefinition.TimedWalkingTestConfiguration
import kotlinx.serialization.KSerializer
import kotlinx.serialization.descriptors.PrimitiveKind
import kotlinx.serialization.descriptors.PrimitiveSerialDescriptor
import kotlinx.serialization.descriptors.SerialDescriptor
import kotlinx.serialization.descriptors.buildClassSerialDescriptor
import kotlinx.serialization.encoding.Decoder
import kotlinx.serialization.encoding.Encoder
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.double
import kotlinx.serialization.json.jsonPrimitive
import java.time.Instant

/**
 * Reads a study component, whose alternative determines the kind of unit it describes.
 */
internal object ComponentSerializer : KSerializer<Component> {
    private const val INFORMATIONAL = "informational"
    private const val QUESTIONNAIRE = "questionnaire"
    private const val HEALTH_DATA_COLLECTION = "healthDataCollection"
    private const val TIMED_WALKING_TEST = "timedWalkingTest"
    private const val CUSTOM_ACTIVE_TASK = "customActiveTask"

    override val descriptor: SerialDescriptor =
        buildClassSerialDescriptor("edu.stanford.spezi.studydefinition.Component")

    override fun deserialize(decoder: Decoder): Component {
        val json = decoder.asJson()
        val (name, body) = SwiftCodable.decodeCase(json.decodeJsonElement())
        val payload = SwiftCodable.payload(name = name, body = body)
        val serializer = when (name) {
            INFORMATIONAL -> Component.Informational.serializer()
            QUESTIONNAIRE -> Component.Questionnaire.serializer()
            HEALTH_DATA_COLLECTION -> Component.HealthDataCollection.serializer()
            TIMED_WALKING_TEST -> Component.TimedWalkingTest.serializer()
            CUSTOM_ACTIVE_TASK -> Component.CustomActiveTask.serializer()
            else -> error("Unknown component '$name'")
        }
        return json.json.decodeFromJsonElement(serializer, payload)
    }

    override fun serialize(encoder: Encoder, value: Component) {
        val json = encoder.asJson()
        val (name, payload) = when (value) {
            is Component.Informational ->
                INFORMATIONAL to json.json.encodeToJsonElement(Component.Informational.serializer(), value)
            is Component.Questionnaire ->
                QUESTIONNAIRE to json.json.encodeToJsonElement(Component.Questionnaire.serializer(), value)
            is Component.HealthDataCollection ->
                HEALTH_DATA_COLLECTION to
                    json.json.encodeToJsonElement(Component.HealthDataCollection.serializer(), value)
            is Component.TimedWalkingTest ->
                TIMED_WALKING_TEST to
                    json.json.encodeToJsonElement(Component.TimedWalkingTest.serializer(), value)
            is Component.CustomActiveTask ->
                CUSTOM_ACTIVE_TASK to
                    json.json.encodeToJsonElement(Component.CustomActiveTask.serializer(), value)
        }
        json.encodeJsonElement(SwiftCodable.encodePayloadCase(name = name, payload = payload))
    }
}

/**
 * Reads whether, and from when, historical health data is collected.
 */
internal object HistoricalDataCollectionSerializer :
    KSerializer<Component.HealthDataCollection.HistoricalDataCollection> {
    private const val DISABLED = "disabled"
    private const val ENABLED = "enabled"

    override val descriptor: SerialDescriptor =
        buildClassSerialDescriptor("edu.stanford.spezi.studydefinition.HistoricalDataCollection")

    override fun deserialize(decoder: Decoder): Component.HealthDataCollection.HistoricalDataCollection {
        val json = decoder.asJson()
        val (name, body) = SwiftCodable.decodeCase(json.decodeJsonElement())
        return when (name) {
            DISABLED -> Component.HealthDataCollection.HistoricalDataCollection.Disabled
            ENABLED -> Component.HealthDataCollection.HistoricalDataCollection.Enabled(
                startDate = json.json.decodeFromJsonElement(
                    HistoricalStartDateSerializer,
                    SwiftCodable.payload(name = name, body = body),
                )
            )
            else -> error("Unknown historical data collection '$name'")
        }
    }

    override fun serialize(
        encoder: Encoder,
        value: Component.HealthDataCollection.HistoricalDataCollection,
    ) {
        val json = encoder.asJson()
        val encoded = when (value) {
            is Component.HealthDataCollection.HistoricalDataCollection.Disabled ->
                SwiftCodable.encodeCase(name = DISABLED)
            is Component.HealthDataCollection.HistoricalDataCollection.Enabled ->
                SwiftCodable.encodePayloadCase(
                    name = ENABLED,
                    payload = json.json.encodeToJsonElement(
                        HistoricalStartDateSerializer,
                        value.startDate,
                    ),
                )
        }
        json.encodeJsonElement(encoded)
    }
}

/**
 * Reads the point from which historical health data is collected.
 *
 * An absolute start is written as a number of seconds relative to the start of 2001.
 */
internal object HistoricalStartDateSerializer :
    KSerializer<Component.HealthDataCollection.HistoricalDataCollection.StartDate> {
    private const val LAST = "last"
    private const val ABSOLUTE = "absolute"
    private const val REFERENCE_EPOCH_SECOND = 978_307_200L

    override val descriptor: SerialDescriptor =
        buildClassSerialDescriptor("edu.stanford.spezi.studydefinition.HistoricalStartDate")

    override fun deserialize(
        decoder: Decoder,
    ): Component.HealthDataCollection.HistoricalDataCollection.StartDate {
        val json = decoder.asJson()
        val (name, body) = SwiftCodable.decodeCase(json.decodeJsonElement())
        val payload = SwiftCodable.payload(name = name, body = body)
        return when (name) {
            LAST -> Component.HealthDataCollection.HistoricalDataCollection.StartDate.Last(
                duration = json.json.decodeFromJsonElement(SparseDateComponentsSerializer, payload)
            )
            ABSOLUTE -> Component.HealthDataCollection.HistoricalDataCollection.StartDate.Absolute(
                instant = Instant.ofEpochSecond(
                    REFERENCE_EPOCH_SECOND + payload.jsonPrimitive.double.toLong()
                )
            )
            else -> error("Unknown historical start date '$name'")
        }
    }

    override fun serialize(
        encoder: Encoder,
        value: Component.HealthDataCollection.HistoricalDataCollection.StartDate,
    ) {
        val json = encoder.asJson()
        val (name, payload) = when (value) {
            is Component.HealthDataCollection.HistoricalDataCollection.StartDate.Last ->
                LAST to json.json.encodeToJsonElement(SparseDateComponentsSerializer, value.duration)
            is Component.HealthDataCollection.HistoricalDataCollection.StartDate.Absolute ->
                ABSOLUTE to JsonPrimitive(value.instant.epochSecond - REFERENCE_EPOCH_SECOND)
        }
        json.encodeJsonElement(SwiftCodable.encodePayloadCase(name = name, payload = payload))
    }
}

/**
 * Reads whether a timed test observes walking or running, written as the alternative's position.
 */
internal object TimedWalkingTestKindSerializer : KSerializer<TimedWalkingTestConfiguration.Kind> {
    override val descriptor: SerialDescriptor =
        PrimitiveSerialDescriptor(
            "edu.stanford.spezi.studydefinition.TimedWalkingTestKind",
            PrimitiveKind.INT,
        )

    override fun deserialize(decoder: Decoder): TimedWalkingTestConfiguration.Kind {
        val position = decoder.decodeInt()
        return TimedWalkingTestConfiguration.Kind.entries.getOrNull(position)
            ?: error("Unknown timed walking test kind '$position'")
    }

    override fun serialize(encoder: Encoder, value: TimedWalkingTestConfiguration.Kind) {
        encoder.encodeInt(value.ordinal)
    }
}
