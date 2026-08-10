//
// This source file is part of the My Heart Counts Android open-source project
//
// SPDX-FileCopyrightText: 2026 Stanford University and the project authors (see CONTRIBUTORS.md)
//
// SPDX-License-Identifier: MIT

package edu.stanford.spezi.scheduler.internal

import edu.stanford.spezi.foundation.JsonSerializer
import kotlinx.serialization.KSerializer
import kotlinx.serialization.builtins.MapSerializer
import kotlinx.serialization.builtins.serializer
import kotlinx.serialization.json.JsonElement

/**
 * Backing store for a typed context, holding entries as JSON keyed by their stable identifier.
 *
 * Keeping the serialized form as the source of truth lets a context round-trip through persistence
 * while preserving entries it has no key for at read time.
 */
internal class JsonContextStore private constructor(
    private val entries: MutableMap<String, JsonElement>,
) {
    constructor() : this(mutableMapOf())

    fun <Value : Any> get(identifier: String, serializer: KSerializer<Value>): Value? {
        val element = entries[identifier] ?: return null
        return JsonSerializer.decodeFromElement(element, serializer)
    }

    fun <Value : Any> set(identifier: String, serializer: KSerializer<Value>, value: Value?) {
        if (value == null) {
            entries.remove(identifier)
        } else {
            entries[identifier] = JsonSerializer.encodeToElement(value, serializer)
        }
    }

    fun encoded(): String = JsonSerializer.encode(entries, mapSerializer)

    override fun equals(other: Any?): Boolean = other is JsonContextStore && other.entries == entries
    override fun hashCode(): Int = entries.hashCode()

    companion object {
        private val mapSerializer = MapSerializer(String.serializer(), JsonElement.serializer())

        fun decode(encoded: String?): JsonContextStore {
            if (encoded.isNullOrEmpty()) return JsonContextStore()
            val map = JsonSerializer.decode(encoded, mapSerializer).toMutableMap()
            return JsonContextStore(map)
        }
    }
}
