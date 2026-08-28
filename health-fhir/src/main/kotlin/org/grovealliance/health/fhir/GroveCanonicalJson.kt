//
// This source file belongs to the My Heart Counts Android project
//
// SPDX-FileCopyrightText: 2026 Stanford University and the project authors (see CONTRIBUTORS.md)
//
// SPDX-License-Identifier: MIT

@file:Suppress("Filename")

package org.grovealliance.health.fhir

import java.nio.charset.StandardCharsets

/** The closed arrays-and-strings subset of RFC 8785 used by Grove identity contracts. */
internal sealed interface GroveCanonicalJsonValue {
    data class Text(val value: String) : GroveCanonicalJsonValue

    data class Array(val elements: List<GroveCanonicalJsonValue>) : GroveCanonicalJsonValue {
        constructor(vararg elements: GroveCanonicalJsonValue) : this(elements.toList())
    }
}

/** Serializes the contract's closed JSON subset without whitespace or lossy Unicode handling. */
internal fun GroveCanonicalJsonValue.canonicalJson(): String = when (this) {
    is GroveCanonicalJsonValue.Array -> elements.joinToString(prefix = "[", postfix = "]", separator = ",") {
        it.canonicalJson()
    }
    is GroveCanonicalJsonValue.Text -> value.canonicalJsonString()
}

internal fun GroveCanonicalJsonValue.canonicalUtf8(): ByteArray =
    canonicalJson().toByteArray(StandardCharsets.UTF_8)

private fun String.canonicalJsonString(): String = buildString(length + JSON_QUOTE_CAPACITY) {
    append('"')
    var index = 0
    while (index < this@canonicalJsonString.length) {
        val character = this@canonicalJsonString[index]
        when (character) {
            '"' -> append("\\\"")
            '\\' -> append("\\\\")
            '\b' -> append("\\b")
            '\t' -> append("\\t")
            '\n' -> append("\\n")
            '\u000c' -> append("\\f")
            '\r' -> append("\\r")
            in '\u0000'..'\u001f' -> {
                append("\\u00")
                append(LOWERCASE_HEX[character.code ushr HEX_NIBBLE_BITS])
                append(LOWERCASE_HEX[character.code and HEX_NIBBLE_MASK])
            }
            in '\ud800'..'\udbff' -> {
                require(
                    index + 1 < this@canonicalJsonString.length &&
                        this@canonicalJsonString[index + 1] in '\udc00'..'\udfff',
                ) { "Grove identity inputs must not contain isolated UTF-16 surrogates." }
                append(character)
                append(this@canonicalJsonString[++index])
            }
            in '\udc00'..'\udfff' -> require(false) {
                "Grove identity inputs must not contain isolated UTF-16 surrogates."
            }
            else -> append(character)
        }
        index += 1
    }
    append('"')
}

private const val JSON_QUOTE_CAPACITY = 2
private const val LOWERCASE_HEX = "0123456789abcdef"
private const val HEX_NIBBLE_BITS = 4
private const val HEX_NIBBLE_MASK = 0x0f
