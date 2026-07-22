//
// This source file is part of the My Heart Counts open-source project
//
// SPDX-FileCopyrightText: 2026 Stanford University and the project authors (see CONTRIBUTORS.md)
//
// SPDX-License-Identifier: MIT

package edu.stanford.spezi.studydefinition.internal

import com.google.common.truth.Truth.assertThat
import edu.stanford.spezi.scheduler.AllowedCompletionPolicy
import edu.stanford.spezi.studydefinition.EnrollmentConditions
import edu.stanford.spezi.studydefinition.Metadata
import edu.stanford.spezi.studydefinition.StudyLifecycleEvent
import kotlinx.serialization.json.Json
import org.junit.Test
import java.time.Duration
import java.util.UUID

/**
 * Verifies the serializers against the encodings taken verbatim from an exported study definition.
 */
class SwiftCodableSerializersTest {

    private val json = Json { ignoreUnknownKeys = true }

    @Test
    fun `decodes a duration from its attosecond halves`() {
        val duration = json.decodeFromString(
            AttosecondDurationSerializer,
            "[19, 9511862599518519296]",
        )

        assertThat(duration).isEqualTo(Duration.ofMinutes(6))
    }

    @Test
    fun `round trips a duration through its attosecond halves`() {
        val encoded = json.encodeToString(AttosecondDurationSerializer, Duration.ofMinutes(6))

        assertThat(encoded).isEqualTo("[19,9511862599518519296]")
    }

    @Test
    fun `decodes sparse calendar components treating absent fields as zero`() {
        val components = json.decodeFromString(SparseDateComponentsSerializer, """{"day":1}""")

        assertThat(components.day).isEqualTo(1)
        assertThat(components.year).isEqualTo(0)
        assertThat(components.hour).isEqualTo(0)
    }

    @Test
    fun `decodes empty calendar components`() {
        val components = json.decodeFromString(SparseDateComponentsSerializer, "{}")

        assertThat(components).isEqualTo(SparseDateComponentsZero)
    }

    @Test
    fun `decodes display text from a localizable value`() {
        val text = json.decodeFromString(
            DisplayTextSerializer,
            """{"defaultValue":{"arguments":[],"key":"ECG"},"bundleURL":"file:///","key":"ECG"}""",
        )

        assertThat(text).isEqualTo("ECG")
    }

    @Test
    fun `decodes display text from a plain string`() {
        val text = json.decodeFromString(DisplayTextSerializer, "\"ECG\"")

        assertThat(text).isEqualTo("ECG")
    }

    @Test
    fun `decodes sample types from their storage wrapper`() {
        val types = json.decodeFromString(
            SampleTypesSerializer,
            """{"storage":["HKQuantityType;HKQuantityTypeIdentifierStepCount"]}""",
        )

        assertThat(types).containsExactly("HKQuantityType;HKQuantityTypeIdentifierStepCount")
    }

    @Test
    fun `decodes an icon`() {
        val icon = json.decodeFromString(
            IconSerializer,
            """{"systemSymbol":{"_0":"cube.transparent"}}""",
        )

        assertThat(icon).isEqualTo(Metadata.Icon.SystemSymbol(name = "cube.transparent"))
    }

    @Test
    fun `decodes enrollment conditions without requirements`() {
        val conditions = json.decodeFromString(EnrollmentConditionsSerializer, """{"none":{}}""")

        assertThat(conditions).isEqualTo(EnrollmentConditions.None)
    }

    @Test
    fun `decodes a completion policy`() {
        val policy = json.decodeFromString(CompletionPolicySerializer, """{"anytime":{}}""")

        assertThat(policy).isEqualTo(AllowedCompletionPolicy.ANYTIME)
    }

    @Test
    fun `round trips a completion policy`() {
        val encoded = json.encodeToString(CompletionPolicySerializer, AllowedCompletionPolicy.SAME_DAY)

        assertThat(encoded).isEqualTo("""{"sameDay":{}}""")
    }

    @Test
    fun `decodes a lifecycle event without a payload`() {
        val event = json.decodeFromString(StudyLifecycleEventSerializer, """{"activation":{}}""")

        assertThat(event).isEqualTo(StudyLifecycleEvent.Activation)
    }

    @Test
    fun `decodes a lifecycle event carrying a labelled component`() {
        val event = json.decodeFromString(
            StudyLifecycleEventSerializer,
            """{"completedTask":{"componentId":"B0CEA4AC-B22A-4D15-8FDA-BCE32BBB88F9"}}""",
        )

        assertThat(event).isEqualTo(
            StudyLifecycleEvent.CompletedTask(
                componentId = UUID.fromString("B0CEA4AC-B22A-4D15-8FDA-BCE32BBB88F9")
            )
        )
    }

    @Test
    fun `rejects a value carrying more than one alternative`() {
        val failure = runCatching {
            json.decodeFromString(StudyLifecycleEventSerializer, """{"activation":{},"enrollment":{}}""")
        }.exceptionOrNull()

        assertThat(failure).isInstanceOf(IllegalArgumentException::class.java)
    }

    private companion object {
        val SparseDateComponentsZero = edu.stanford.spezi.studydefinition.DateComponents(
            year = 0,
            month = 0,
            day = 0,
            hour = 0,
            minute = 0,
            second = 0,
        )
    }
}
