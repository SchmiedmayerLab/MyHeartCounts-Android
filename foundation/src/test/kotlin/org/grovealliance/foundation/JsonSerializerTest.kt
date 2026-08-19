//
// This source file is part of the My Heart Counts Android open-source project
//
// SPDX-FileCopyrightText: 2026 Stanford University and the project authors (see CONTRIBUTORS.md)
//
// SPDX-License-Identifier: MIT

package org.grovealliance.foundation

import com.google.common.truth.Truth.assertThat
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import org.junit.Test

class JsonSerializerTest {

    private val serializer = TestSerializable.serializer()

    @Test
    fun `encode produces valid json`() {
        // when
        val encoded = JsonSerializer.encode(TestSerializable("value"), serializer)

        // then
        assertThat(encoded).isEqualTo("{\"content\":\"value\"}")
    }

    @Test
    fun `decode round-trips a value`() {
        // given
        val input = TestSerializable("whoops")

        // when
        val decoded = JsonSerializer.decode(JsonSerializer.encode(input, serializer), serializer)

        // then
        assertThat(decoded).isEqualTo(input)
    }

    @Test
    fun `decode ignores unknown keys`() {
        // when
        val decoded = JsonSerializer.decode("{\"content\":\"value\",\"extra\":true}", serializer)

        // then
        assertThat(decoded).isEqualTo(TestSerializable("value"))
    }

    @Test
    fun `element round-trips a value`() {
        // given
        val input = TestSerializable("element")

        // when
        val element = JsonSerializer.encodeToElement(input, serializer)

        // then
        assertThat(element.jsonObject["content"]?.jsonPrimitive?.content).isEqualTo("element")
        assertThat(JsonSerializer.decodeFromElement(element, serializer)).isEqualTo(input)
    }

    @Test
    fun `parseToElement exposes the raw tree`() {
        // when
        val element = JsonSerializer.parseToElement("{\"content\":\"raw\"}")

        // then
        assertThat(element.jsonObject["content"]?.jsonPrimitive?.content).isEqualTo("raw")
    }

    @Test
    fun `catching captures failure in a result`() {
        // when
        val decode = JsonSerializer.decodeCatching("INVALID", serializer)
        val encode = JsonSerializer.encodeCatching(TestSerializable("ok"), serializer)

        // then
        assertThat(decode.isFailure).isTrue()
        assertThat(encode.isSuccess).isTrue()
    }

    @Test
    fun `orNull yields null on failure`() {
        // when
        val decoded = JsonSerializer.decodeOrNull("INVALID", serializer)
        val fromElement = JsonSerializer.decodeFromElementOrNull(JsonSerializer.parseToElement("[]"), serializer)

        // then
        assertThat(decoded).isNull()
        assertThat(fromElement).isNull()
    }

    @Serializable
    data class TestSerializable(val content: String)
}
