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
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.jsonPrimitive
import org.grovealliance.scheduler.AllowedCompletionPolicy
import org.grovealliance.studydefinition.EnrollmentConditions
import org.grovealliance.studydefinition.Metadata
import org.grovealliance.studydefinition.StudyLifecycleEvent
import java.util.UUID

/**
 * Reads a study's icon.
 */
internal object IconSerializer : KSerializer<Metadata.Icon> {
    private const val SYSTEM_SYMBOL = "systemSymbol"
    private const val CUSTOM = "custom"

    override val descriptor: SerialDescriptor =
        buildClassSerialDescriptor("org.grovealliance.studydefinition.Icon")

    override fun deserialize(decoder: Decoder): Metadata.Icon {
        val (name, body) = SwiftCodable.decodeCase(decoder.asJson().decodeJsonElement())
        val value = SwiftCodable.payload(name = name, body = body).jsonPrimitive.content
        return when (name) {
            SYSTEM_SYMBOL -> Metadata.Icon.SystemSymbol(name = value)
            CUSTOM -> Metadata.Icon.Custom(url = value)
            else -> error("Unknown icon '$name'")
        }
    }

    override fun serialize(encoder: Encoder, value: Metadata.Icon) {
        val (name, content) = when (value) {
            is Metadata.Icon.SystemSymbol -> SYSTEM_SYMBOL to value.name
            is Metadata.Icon.Custom -> CUSTOM to value.url
        }
        encoder.asJson().encodeJsonElement(
            SwiftCodable.encodePayloadCase(name = name, payload = JsonPrimitive(content))
        )
    }
}

/**
 * Reads the conditions a participant must satisfy to enroll.
 */
internal object EnrollmentConditionsSerializer : KSerializer<EnrollmentConditions> {
    private const val NONE = "none"
    private const val REQUIRES_INVITATION = "requiresInvitation"

    override val descriptor: SerialDescriptor =
        buildClassSerialDescriptor("org.grovealliance.studydefinition.EnrollmentConditions")

    override fun deserialize(decoder: Decoder): EnrollmentConditions {
        val (name, body) = SwiftCodable.decodeCase(decoder.asJson().decodeJsonElement())
        return when (name) {
            NONE -> EnrollmentConditions.None
            REQUIRES_INVITATION -> EnrollmentConditions.RequiresInvitation(
                verificationEndpoint = SwiftCodable.payload(name = name, body = body).jsonPrimitive.content
            )
            else -> error("Unknown enrollment condition '$name'")
        }
    }

    override fun serialize(encoder: Encoder, value: EnrollmentConditions) {
        val encoded = when (value) {
            is EnrollmentConditions.None -> SwiftCodable.encodeCase(name = NONE)
            is EnrollmentConditions.RequiresInvitation -> SwiftCodable.encodePayloadCase(
                name = REQUIRES_INVITATION,
                payload = JsonPrimitive(value.verificationEndpoint),
            )
        }
        encoder.asJson().encodeJsonElement(encoded)
    }
}

/**
 * Reads when a scheduled occurrence may be completed.
 */
internal object CompletionPolicySerializer : KSerializer<AllowedCompletionPolicy> {
    private val NAMES = mapOf(
        "sameDay" to AllowedCompletionPolicy.SAME_DAY,
        "afterStart" to AllowedCompletionPolicy.AFTER_START,
        "sameDayAfterStart" to AllowedCompletionPolicy.SAME_DAY_AFTER_START,
        "duringEvent" to AllowedCompletionPolicy.DURING_EVENT,
        "anytime" to AllowedCompletionPolicy.ANYTIME,
    )

    override val descriptor: SerialDescriptor =
        buildClassSerialDescriptor("org.grovealliance.studydefinition.AllowedCompletionPolicy")

    override fun deserialize(decoder: Decoder): AllowedCompletionPolicy {
        val (name, _) = SwiftCodable.decodeCase(decoder.asJson().decodeJsonElement())
        return NAMES[name] ?: error("Unknown completion policy '$name'")
    }

    override fun serialize(encoder: Encoder, value: AllowedCompletionPolicy) {
        val name = NAMES.entries.first { it.value == value }.key
        encoder.asJson().encodeJsonElement(SwiftCodable.encodeCase(name = name))
    }
}

/**
 * Reads an event anchoring an event-based schedule.
 */
internal object StudyLifecycleEventSerializer : KSerializer<StudyLifecycleEvent> {
    private const val ENROLLMENT = "enrollment"
    private const val ACTIVATION = "activation"
    private const val UNENROLLMENT = "unenrollment"
    private const val STUDY_END = "studyEnd"
    private const val COMPLETED_TASK = "completedTask"
    private const val COMPONENT_ID = "componentId"

    override val descriptor: SerialDescriptor =
        buildClassSerialDescriptor("org.grovealliance.studydefinition.StudyLifecycleEvent")

    override fun deserialize(decoder: Decoder): StudyLifecycleEvent {
        val (name, body) = SwiftCodable.decodeCase(decoder.asJson().decodeJsonElement())
        return when (name) {
            ENROLLMENT -> StudyLifecycleEvent.Enrollment
            ACTIVATION -> StudyLifecycleEvent.Activation
            UNENROLLMENT -> StudyLifecycleEvent.Unenrollment
            STUDY_END -> StudyLifecycleEvent.StudyEnd
            COMPLETED_TASK -> StudyLifecycleEvent.CompletedTask(
                componentId = UUID.fromString(
                    requireNotNull(body[COMPONENT_ID]) { "Missing '$COMPONENT_ID'" }.jsonPrimitive.content
                )
            )
            else -> error("Unknown study lifecycle event '$name'")
        }
    }

    override fun serialize(encoder: Encoder, value: StudyLifecycleEvent) {
        val encoded = when (value) {
            is StudyLifecycleEvent.Enrollment -> SwiftCodable.encodeCase(name = ENROLLMENT)
            is StudyLifecycleEvent.Activation -> SwiftCodable.encodeCase(name = ACTIVATION)
            is StudyLifecycleEvent.Unenrollment -> SwiftCodable.encodeCase(name = UNENROLLMENT)
            is StudyLifecycleEvent.StudyEnd -> SwiftCodable.encodeCase(name = STUDY_END)
            is StudyLifecycleEvent.CompletedTask -> SwiftCodable.encodeCase(
                name = COMPLETED_TASK,
                body = JsonObject(mapOf(COMPONENT_ID to JsonPrimitive(value.componentId.toString()))),
            )
        }
        encoder.asJson().encodeJsonElement(encoded)
    }
}
