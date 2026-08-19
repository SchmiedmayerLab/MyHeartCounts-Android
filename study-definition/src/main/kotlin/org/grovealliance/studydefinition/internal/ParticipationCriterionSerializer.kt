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
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.int
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonPrimitive
import org.grovealliance.studydefinition.CustomCriterionKey
import org.grovealliance.studydefinition.ParticipationCriterion

/**
 * Reads a participation criterion, whose combining alternatives nest further criteria.
 */
internal object ParticipationCriterionSerializer : KSerializer<ParticipationCriterion> {
    private const val AGE_AT_LEAST = "ageAtLeast"
    private const val IS_FROM_REGION = "isFromRegion"
    private const val SPEAKS_LANGUAGE = "speaksLanguage"
    private const val CUSTOM = "custom"
    private const val NOT = "not"
    private const val ALL = "all"
    private const val ANY = "any"

    override val descriptor: SerialDescriptor =
        buildClassSerialDescriptor("org.grovealliance.studydefinition.ParticipationCriterion")

    override fun deserialize(decoder: Decoder): ParticipationCriterion {
        val json = decoder.asJson()
        val (name, body) = SwiftCodable.decodeCase(json.decodeJsonElement())
        val payload = SwiftCodable.payload(name = name, body = body)
        return when (name) {
            AGE_AT_LEAST -> ParticipationCriterion.AgeAtLeast(years = payload.jsonPrimitive.int)
            IS_FROM_REGION -> ParticipationCriterion.IsFromRegion(region = payload.jsonPrimitive.content)
            SPEAKS_LANGUAGE -> ParticipationCriterion.SpeaksLanguage(language = payload.jsonPrimitive.content)
            CUSTOM -> ParticipationCriterion.Custom(
                key = json.json.decodeFromJsonElement(CustomCriterionKey.serializer(), payload)
            )
            NOT -> ParticipationCriterion.Not(inner = json.json.decodeFromJsonElement(this, payload))
            ALL -> ParticipationCriterion.All(
                criteria = payload.jsonArray.map { json.json.decodeFromJsonElement(this, it) }
            )
            ANY -> ParticipationCriterion.Any(
                criteria = payload.jsonArray.map { json.json.decodeFromJsonElement(this, it) }
            )
            else -> error("Unknown participation criterion '$name'")
        }
    }

    override fun serialize(encoder: Encoder, value: ParticipationCriterion) {
        val json = encoder.asJson()
        val (name, payload) = when (value) {
            is ParticipationCriterion.AgeAtLeast -> AGE_AT_LEAST to JsonPrimitive(value.years)
            is ParticipationCriterion.IsFromRegion -> IS_FROM_REGION to JsonPrimitive(value.region)
            is ParticipationCriterion.SpeaksLanguage -> SPEAKS_LANGUAGE to JsonPrimitive(value.language)
            is ParticipationCriterion.Custom ->
                CUSTOM to json.json.encodeToJsonElement(CustomCriterionKey.serializer(), value.key)
            is ParticipationCriterion.Not -> NOT to json.json.encodeToJsonElement(this, value.inner)
            is ParticipationCriterion.All ->
                ALL to JsonArray(value.criteria.map { json.json.encodeToJsonElement(this, it) })
            is ParticipationCriterion.Any ->
                ANY to JsonArray(value.criteria.map { json.json.encodeToJsonElement(this, it) })
        }
        json.encodeJsonElement(SwiftCodable.encodePayloadCase(name = name, payload = payload))
    }
}
