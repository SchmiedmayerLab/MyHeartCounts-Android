//
// This source file is part of the My Heart Counts Android open-source project
//
// SPDX-FileCopyrightText: 2026 Stanford University and the project authors (see CONTRIBUTORS.md)
//
// SPDX-License-Identifier: MIT

package org.grovealliance.studydefinition.internal

import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.jsonObject

/**
 * Helpers for the encoding used by study definition documents, in which a value with alternatives
 * is written as a single-entry object keyed by the chosen alternative.
 *
 * ```json
 * { "informational": { "_0": { … } } }
 * { "activation": {} }
 * ```
 *
 * An alternative's unlabelled value is carried under [PAYLOAD]; labelled values are carried under
 * their own names alongside it.
 */
internal object SwiftCodable {

    /**
     * The key carrying an alternative's single unlabelled value.
     */
    const val PAYLOAD = "_0"

    /**
     * Splits [element] into the chosen alternative's name and its body.
     *
     * @throws IllegalArgumentException when [element] does not hold exactly one alternative.
     */
    fun decodeCase(element: JsonElement): Pair<String, JsonObject> {
        val entries = element.jsonObject
        require(entries.size == 1) {
            "Expected exactly one alternative, found ${entries.keys.sorted()}"
        }
        val (name, body) = entries.entries.single()
        return name to body.jsonObject
    }

    /**
     * Builds the encoded form of the alternative [name] carrying [body].
     */
    fun encodeCase(name: String, body: JsonObject = JsonObject(emptyMap())): JsonObject =
        JsonObject(mapOf(name to body))

    /**
     * Builds the encoded form of the alternative [name] carrying [payload] as its unlabelled value.
     */
    fun encodePayloadCase(name: String, payload: JsonElement): JsonObject =
        encodeCase(name = name, body = JsonObject(mapOf(PAYLOAD to payload)))

    /**
     * The unlabelled value of an alternative's [body].
     *
     * @throws IllegalArgumentException when the body carries no unlabelled value.
     */
    fun payload(name: String, body: JsonObject): JsonElement =
        requireNotNull(body[PAYLOAD]) { "Alternative '$name' is missing its '$PAYLOAD' value" }
}
