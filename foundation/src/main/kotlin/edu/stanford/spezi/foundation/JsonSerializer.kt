//
// This source file is part of the My Heart Counts open-source project
//
// SPDX-FileCopyrightText: 2026 Stanford University and the project authors (see CONTRIBUTORS.md)
//
// SPDX-License-Identifier: MIT

package edu.stanford.spezi.foundation

import kotlinx.serialization.DeserializationStrategy
import kotlinx.serialization.SerializationStrategy
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonElement

/**
 * Central JSON codec for the project. Route serialization through these helpers instead of
 * constructing an ad-hoc [Json], so encoding and decoding behave uniformly across modules.
 *
 * Unknown keys are ignored on decoding and default values are always written on encoding.
 *
 * Operations come in three flavours: a direct form that throws on failure, a `Catching` form that
 * captures the outcome in a [Result], and an `OrNull` form that yields `null` on failure.
 */
object JsonSerializer {

    private val json: Json = Json {
        ignoreUnknownKeys = true
        encodeDefaults = true
    }

    /**
     * Encodes [value] to a JSON string using [strategy].
     */
    fun <T> encode(value: T, strategy: SerializationStrategy<T>): String =
        json.encodeToString(strategy, value)

    /**
     * Decodes [text] into a [T] using [deserializer].
     */
    fun <T> decode(text: String, deserializer: DeserializationStrategy<T>): T =
        json.decodeFromString(deserializer, text)

    /**
     * Encodes [value] into a [JsonElement] tree using [strategy].
     */
    fun <T> encodeToElement(value: T, strategy: SerializationStrategy<T>): JsonElement =
        json.encodeToJsonElement(strategy, value)

    /**
     * Decodes [element] into a [T] using [deserializer].
     */
    fun <T> decodeFromElement(element: JsonElement, deserializer: DeserializationStrategy<T>): T =
        json.decodeFromJsonElement(deserializer, element)

    /**
     * Parses [text] into a [JsonElement] tree without binding it to a type.
     */
    fun parseToElement(text: String): JsonElement = json.parseToJsonElement(text)

    /**
     * Encodes [value] using [strategy], capturing any failure in the [Result].
     */
    fun <T> encodeCatching(value: T, strategy: SerializationStrategy<T>): Result<String> =
        runCatching { encode(value, strategy) }

    /**
     * Decodes [text] using [deserializer], capturing any failure in the [Result].
     */
    fun <T> decodeCatching(text: String, deserializer: DeserializationStrategy<T>): Result<T> =
        runCatching { decode(text, deserializer) }

    /**
     * Decodes [text] using [deserializer], yielding `null` on failure.
     */
    fun <T> decodeOrNull(text: String, deserializer: DeserializationStrategy<T>): T? =
        decodeCatching(text, deserializer).getOrNull()

    /**
     * Decodes [element] using [deserializer], yielding `null` on failure.
     */
    fun <T> decodeFromElementOrNull(element: JsonElement, deserializer: DeserializationStrategy<T>): T? =
        runCatching { decodeFromElement(element, deserializer) }.getOrNull()
}
