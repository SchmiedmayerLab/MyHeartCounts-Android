//
// This source file is part of the My Heart Counts Android open-source project
//
// SPDX-FileCopyrightText: 2026 Stanford University and the project authors (see CONTRIBUTORS.md)
//
// SPDX-License-Identifier: MIT

package edu.stanford.myheartcounts.model.workout

import com.google.common.truth.Truth.assertThat
import kotlinx.serialization.json.Json
import org.junit.Test

class WorkoutPreferenceSerializationTest {

    @Test
    fun `workout types are stored as their comma separated ids`() {
        // given
        val types = WorkoutTypes(elements = linkedSetOf(WorkoutType.WALK, WorkoutType.RUN, WorkoutType.YOGA_PILATES))

        // when
        val encoded = Json.encodeToString(WorkoutTypes.serializer(), types)

        // then
        assertThat(encoded).isEqualTo("\"walk,run,yoga/pilates\"")
    }

    @Test
    fun `stored workout types read back, ignoring unknown ids`() {
        // when
        val decoded = Json.decodeFromString(WorkoutTypes.serializer(), "\"walk,HIIT,unknown,other\"")

        // then
        assertThat(decoded.elements).containsExactly(WorkoutType.WALK, WorkoutType.HIIT, WorkoutType.OTHER)
    }

    @Test
    fun `no preferred workout type is stored as an empty string`() {
        // when
        val encoded = Json.encodeToString(WorkoutTypes.serializer(), WorkoutTypes(elements = emptySet()))
        val decoded = Json.decodeFromString(WorkoutTypes.serializer(), "\"\"")

        // then
        assertThat(encoded).isEqualTo("\"\"")
        assertThat(decoded.elements).isEmpty()
    }

    @Test
    fun `every workout type survives a round trip`() {
        // given
        val types = WorkoutTypes(elements = WorkoutType.entries.toSet())

        // when
        val decoded = Json.decodeFromString(WorkoutTypes.serializer(), Json.encodeToString(WorkoutTypes.serializer(), types))

        // then
        assertThat(decoded).isEqualTo(types)
    }

    @Test
    fun `notification times are stored as zero padded hours and minutes`() {
        // when
        val morning = Json.encodeToString(NotificationTime.serializer(), NotificationTime(hour = 9, minute = 0))
        val evening = Json.encodeToString(NotificationTime.serializer(), NotificationTime(hour = 18, minute = 45))

        // then
        assertThat(morning).isEqualTo("\"09:00\"")
        assertThat(evening).isEqualTo("\"18:45\"")
    }

    @Test
    fun `stored notification times read back`() {
        // when
        val decoded = Json.decodeFromString(NotificationTime.serializer(), "\"07:15\"")

        // then
        assertThat(decoded).isEqualTo(
            NotificationTime(
                hour = 7,
                minute = 15,
            ),
        )
    }

    @Test
    fun `invalid notification times are rejected when parsed`() {
        // when / then
        assertThat(NotificationTime.parse("24:00")).isNull()
        assertThat(NotificationTime.parse("09:60")).isNull()
        assertThat(NotificationTime.parse("nine")).isNull()
    }
}
