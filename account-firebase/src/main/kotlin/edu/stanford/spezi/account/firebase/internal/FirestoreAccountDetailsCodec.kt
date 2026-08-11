//
// This source file is part of the My Heart Counts Android open-source project
//
// SPDX-FileCopyrightText: 2026 Stanford University and the project authors (see CONTRIBUTORS.md)
//
// SPDX-License-Identifier: MIT

package edu.stanford.spezi.account.firebase.internal

import com.google.firebase.Timestamp
import edu.stanford.spezi.account.AccountDetails
import edu.stanford.spezi.account.AccountDetailsCodecConfig
import edu.stanford.spezi.account.AccountKey
import edu.stanford.spezi.account.AccountKeys
import edu.stanford.spezi.account.AnyAccountKey
import edu.stanford.spezi.account.keys
import edu.stanford.spezi.core.Module
import edu.stanford.spezi.core.dependency
import edu.stanford.spezi.foundation.JsonSerializer
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonNull
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.booleanOrNull
import kotlinx.serialization.json.doubleOrNull
import kotlinx.serialization.json.longOrNull
import java.time.Instant

@Suppress("UNCHECKED_CAST")
internal class FirestoreAccountDetailsCodec : Module {
    private val codecConfig by dependency<AccountDetailsCodecConfig>()

    fun encode(details: AccountDetails): Map<String, Any?> {
        return buildMap {
            details.accountKeyTypes.keys().forEach { key ->
                if (key.identifier == AccountKeys.accountId.identifier) return@forEach
                val typedKey = key as AccountKey<Any>
                val value = details.getAnyOrNull(typedKey::class) ?: return@forEach

                put(
                    key = codecConfig.encodingIdentifier(typedKey),
                    value = encodeValue(typedKey, value),
                )
            }
        }
    }

    fun decode(firestoreData: Map<String, Any?>, requestedKeys: Set<AnyAccountKey>): AccountDetails {
        val details = AccountDetails()
        firestoreData.forEach { (identifier, rawValue) ->
            val key = codecConfig.resolveDecodingKey(identifier, requestedKeys) ?: return@forEach
            val typedKey = key as AccountKey<Any>
            val decoded = runCatching { decodeValue(typedKey, rawValue) }.getOrNull() ?: return@forEach
            details.setAny(typedKey::class, decoded)
        }
        return details
    }

    /**
     * Encodes [value] for storage. [Instant] values are stored as a native Firestore [Timestamp];
     * everything else is serialized through the key's serializer onto native Firestore types.
     */
    private fun encodeValue(key: AccountKey<Any>, value: Any): Any? = when (value) {
        is Instant -> Timestamp(value.epochSecond, value.nano)
        else -> JsonSerializer.encodeToElement(value, key.serializer).toFirestoreValue()
    }

    /**
     * Decodes a value read back from Firestore. [Instant]-typed keys accept a native [Timestamp] or
     * a numeric epoch-milliseconds fallback; everything else goes through the key's serializer.
     */
    private fun decodeValue(key: AccountKey<Any>, rawValue: Any?): Any? =
        if (key.valueType == Instant::class) {
            when (rawValue) {
                is Timestamp -> Instant.ofEpochSecond(rawValue.seconds, rawValue.nanoseconds.toLong())
                is Number -> Instant.ofEpochMilli(rawValue.toLong())
                else -> null
            }
        } else {
            JsonSerializer.decodeFromElement(rawValue.toJsonElement(), key.serializer)
        }

    /**
     * Converts a serialized [JsonElement] into a value Firestore stores natively (primitives,
     * [Map], [List]) instead of a JSON-encoded string.
     */
    private fun JsonElement.toFirestoreValue(): Any? = when (this) {
        is JsonNull -> null
        is JsonPrimitive -> if (isString) {
            content
        } else {
            booleanOrNull ?: longOrNull ?: doubleOrNull ?: content
        }
        is JsonObject -> mapValues { it.value.toFirestoreValue() }
        is JsonArray -> map { it.toFirestoreValue() }
    }

    /**
     * Converts a value read back from Firestore into the [JsonElement] tree expected by the key's
     * serializer.
     */
    private fun Any?.toJsonElement(): JsonElement = when (this) {
        null -> JsonNull
        is JsonElement -> this
        is Boolean -> JsonPrimitive(this)
        is Number -> JsonPrimitive(this)
        is String -> JsonPrimitive(this)
        is Map<*, *> -> JsonObject(entries.associate { (k, v) -> k.toString() to v.toJsonElement() })
        is List<*> -> JsonArray(map { it.toJsonElement() })
        else -> JsonPrimitive(toString())
    }
}
