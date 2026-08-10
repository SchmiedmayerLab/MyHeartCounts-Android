//
// This source file is part of the My Heart Counts Android open-source project
//
// SPDX-FileCopyrightText: 2026 Stanford University and the project authors (see CONTRIBUTORS.md)
//
// SPDX-License-Identifier: MIT

package edu.stanford.spezi.studydefinition.internal

import edu.stanford.spezi.studydefinition.DateComponents
import kotlinx.serialization.ExperimentalSerializationApi
import kotlinx.serialization.KSerializer
import kotlinx.serialization.descriptors.SerialDescriptor
import kotlinx.serialization.descriptors.buildClassSerialDescriptor
import kotlinx.serialization.encoding.Decoder
import kotlinx.serialization.encoding.Encoder
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonDecoder
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonEncoder
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.JsonUnquotedLiteral
import kotlinx.serialization.json.contentOrNull
import kotlinx.serialization.json.int
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import java.math.BigInteger
import java.time.Duration

/**
 * Requires the decoder to be reading JSON, which every study definition document is.
 */
internal fun Decoder.asJson(): JsonDecoder =
    this as? JsonDecoder ?: error("Study definitions can only be decoded from JSON")

/**
 * Requires the encoder to be writing JSON, which every study definition document is.
 */
internal fun Encoder.asJson(): JsonEncoder =
    this as? JsonEncoder ?: error("Study definitions can only be encoded to JSON")

/**
 * Reads a duration written as the pair of 64-bit halves of its length in attoseconds.
 */
@OptIn(ExperimentalSerializationApi::class)
internal object AttosecondDurationSerializer : KSerializer<Duration> {
    private const val ATTOS_PER_SECOND_EXPONENT = 18
    private const val ATTOS_PER_NANO_EXPONENT = 9

    private val ATTOS_PER_SECOND: BigInteger = BigInteger.TEN.pow(ATTOS_PER_SECOND_EXPONENT)
    private val ATTOS_PER_NANO: BigInteger = BigInteger.TEN.pow(ATTOS_PER_NANO_EXPONENT)
    private val TWO_POW_64: BigInteger = BigInteger.ONE.shiftLeft(Long.SIZE_BITS)

    override val descriptor: SerialDescriptor =
        buildClassSerialDescriptor("edu.stanford.spezi.studydefinition.AttosecondDuration")

    override fun deserialize(decoder: Decoder): Duration {
        val halves = decoder.asJson().decodeJsonElement().jsonArray
        require(halves.size == 2) { "Expected a duration as two 64-bit halves, found ${halves.size}" }
        // The low half spans the full unsigned 64-bit range, beyond what a signed Long holds.
        val high = BigInteger(halves[0].jsonPrimitive.content)
        val low = BigInteger(halves[1].jsonPrimitive.content)
        val attoseconds = high.multiply(TWO_POW_64).add(low)
        val (seconds, remainder) = attoseconds.divideAndRemainder(ATTOS_PER_SECOND)
        return Duration.ofSeconds(
            seconds.toLong(),
            remainder.divide(ATTOS_PER_NANO).toLong(),
        )
    }

    override fun serialize(encoder: Encoder, value: Duration) {
        val attoseconds = BigInteger.valueOf(value.seconds).multiply(ATTOS_PER_SECOND)
            .add(BigInteger.valueOf(value.nano.toLong()).multiply(ATTOS_PER_NANO))
        val high = attoseconds.shiftRight(Long.SIZE_BITS)
        val low = attoseconds.subtract(high.multiply(TWO_POW_64))
        encoder.asJson().encodeJsonElement(
            JsonArray(
                listOf(
                    JsonUnquotedLiteral(high.toString()),
                    JsonUnquotedLiteral(low.toString()),
                )
            )
        )
    }
}

/**
 * Reads calendar components written with only the fields that carry a value, treating the rest as
 * zero.
 */
internal object SparseDateComponentsSerializer : KSerializer<DateComponents> {
    override val descriptor: SerialDescriptor =
        buildClassSerialDescriptor("edu.stanford.spezi.studydefinition.SparseDateComponents")

    override fun deserialize(decoder: Decoder): DateComponents {
        val fields = decoder.asJson().decodeJsonElement().jsonObject
        fun field(name: String) = fields[name]?.jsonPrimitive?.int ?: 0
        return DateComponents(
            year = field("year"),
            month = field("month"),
            day = field("day"),
            hour = field("hour"),
            minute = field("minute"),
            second = field("second"),
        )
    }

    override fun serialize(encoder: Encoder, value: DateComponents) {
        val fields = buildMap {
            if (value.year != 0) put("year", JsonPrimitive(value.year))
            if (value.month != 0) put("month", JsonPrimitive(value.month))
            if (value.day != 0) put("day", JsonPrimitive(value.day))
            if (value.hour != 0) put("hour", JsonPrimitive(value.hour))
            if (value.minute != 0) put("minute", JsonPrimitive(value.minute))
            if (value.second != 0) put("second", JsonPrimitive(value.second))
        }
        encoder.asJson().encodeJsonElement(JsonObject(fields))
    }
}

/**
 * Reads display text that may be written either as a plain string or as a localizable value, of
 * which only the authored text is kept.
 */
internal object DisplayTextSerializer : KSerializer<String> {
    private const val DEFAULT_VALUE = "defaultValue"
    private const val KEY = "key"

    override val descriptor: SerialDescriptor =
        buildClassSerialDescriptor("edu.stanford.spezi.studydefinition.DisplayText")

    override fun deserialize(decoder: Decoder): String =
        decoder.asJson().decodeJsonElement().authoredText()

    override fun serialize(encoder: Encoder, value: String) {
        encoder.asJson().encodeJsonElement(JsonPrimitive(value))
    }

    /**
     * The authored text of [element], whether written plainly or as a localizable value.
     */
    private fun JsonElement.authoredText(): String {
        (this as? JsonPrimitive)?.contentOrNull?.let { return it }
        val fields = jsonObject
        val text = fields[DEFAULT_VALUE]?.jsonObject?.get(KEY)?.jsonPrimitive?.contentOrNull
            ?: fields[KEY]?.jsonPrimitive?.contentOrNull
        return requireNotNull(text) { "Missing display text" }
    }
}

/**
 * Reads a set of health sample types, written either as a plain list or wrapped in its storage.
 */
internal object SampleTypesSerializer : KSerializer<List<String>> {
    private const val STORAGE = "storage"

    override val descriptor: SerialDescriptor =
        buildClassSerialDescriptor("edu.stanford.spezi.studydefinition.SampleTypes")

    override fun deserialize(decoder: Decoder): List<String> {
        val element = decoder.asJson().decodeJsonElement()
        val entries = (element as? JsonArray) ?: element.jsonObject[STORAGE]?.jsonArray
        return entries.orEmpty().map { it.jsonPrimitive.content }
    }

    override fun serialize(encoder: Encoder, value: List<String>) {
        encoder.asJson().encodeJsonElement(
            JsonObject(mapOf(STORAGE to JsonArray(value.map { JsonPrimitive(it) })))
        )
    }
}
