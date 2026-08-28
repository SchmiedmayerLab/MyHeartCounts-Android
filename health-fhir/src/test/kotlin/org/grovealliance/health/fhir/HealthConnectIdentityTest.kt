//
// This source file belongs to the My Heart Counts Android project
//
// SPDX-FileCopyrightText: 2026 Stanford University and the project authors (see CONTRIBUTORS.md)
//
// SPDX-License-Identifier: MIT

package org.grovealliance.health.fhir

import com.google.common.truth.Truth.assertThat
import org.junit.Assert.assertThrows
import org.junit.Test
import java.time.Instant

class HealthConnectIdentityTest {
    private val key = testIdentityKey()
    private val source = HealthConnectIdentity.record(
        key,
        REPOSITORY_SCOPE_KEY,
        "HeartRateRecord",
        "record-heart-001",
    )

    @Test
    fun `source identity uses its deployment key epoch system and typed opaque value`() {
        val vector = HealthConnectIdentity.record(
            key,
            REPOSITORY_SCOPE_KEY,
            "RestingHeartRateRecord",
            "record|東京",
        )
        assertThat(vector.identifier.system).isEqualTo(
            "$TEST_OPAQUE_IDENTITY_SYSTEM_FAMILY/source-record/test-key/1",
        )
        assertThat(vector.identifier.value).startsWith("v2:test-key:1:")
        assertThat(vector.identifier.hasGroveRole(GroveIdentifierRole.SOURCE_RECORD)).isTrue()
    }

    @Test
    fun `source identity debug representation redacts native and derived identifiers`() {
        val debugRepresentation = source.toString()

        assertThat(debugRepresentation).doesNotContain("record-heart-001")
        assertThat(debugRepresentation).doesNotContain(source.identifier.value)
        assertThat(debugRepresentation).contains("nativeRecordId=<redacted>")
        assertThat(debugRepresentation).contains("identifier=<redacted>")
    }

    @Test
    fun `length framing accepts separators and supplementary Unicode without collisions`() {
        val value = HealthConnectIdentity.record(
            key,
            REPOSITORY_SCOPE_KEY,
            "HeartRateRecord",
            "record|heart-😀",
        )
        assertThat(value.identifier.value).isNotEqualTo(source.identifier.value)
    }

    @Test
    fun `rejects only unpaired Unicode surrogates`() {
        listOf("\ud800", "\udc00", "prefix\ud800suffix").forEach { invalid ->
            assertThrows(IllegalArgumentException::class.java) {
                HealthConnectIdentity.record(key, REPOSITORY_SCOPE_KEY, "HeartRateRecord", invalid)
            }
        }
    }

    @Test
    fun `recording Device identity requires a per-unit token`() {
        val subject = FhirIdentifierKey("https://example.org/patients", "participant-1")
        val mine = HealthConnectIdentity.recordingDevice(key, subject, "unit-42")
        val again = HealthConnectIdentity.recordingDevice(key, subject, "unit-42")
        val yours = HealthConnectIdentity.recordingDevice(key, subject, "unit-43")
        assertThat(mine.value).isEqualTo(again.value)
        assertThat(mine.value).isNotEqualTo(yours.value)
        assertThat(mine.hasGroveRole(GroveIdentifierRole.RECORDING_DEVICE)).isTrue()
        assertThrows(IllegalArgumentException::class.java) {
            HealthConnectIdentity.recordingDevice(key, subject, "")
        }
    }

    @Test
    fun `sample slot replays identically while a duplicate uses another occurrence`() {
        val time = Instant.parse("2026-08-20T17:30:15Z")
        val first = HealthConnectIdentity.heartRateSampleOutput(key, source, time, 0)
        val duplicate = HealthConnectIdentity.heartRateSampleOutput(key, source, time, 1)
        val replayedSlot = HealthConnectIdentity.heartRateSampleOutput(key, source, time, 0)
        assertThat(replayedSlot.value).isEqualTo(first.value)
        assertThat(duplicate.value).isNotEqualTo(first.value)
        assertThat(first.hasGroveRole(GroveIdentifierRole.SOURCE_OUTPUT)).isTrue()
    }

    @Test
    fun `event and entry-node identities use their typed v2 lexical forms`() {
        val event = HealthConnectIdentity.exchange(TEST_EVENT_SYSTEM, TEST_PRODUCER_INSTANCE, EventSequence("42"))
        assertThat(event.value).isEqualTo("e2:$TEST_PRODUCER_INSTANCE:42")
        assertThat(event.hasGroveRole(GroveIdentifierRole.EVENT)).isTrue()

        val node = HealthConnectIdentity.conversionNode(TEST_ENTRY_NODE_SYSTEM, event)
        assertThat(node.value).matches("n2:conversion-provenance:0:[A-Za-z0-9_-]{43}")
        assertThat(node.hasGroveRole(GroveIdentifierRole.ENTRY_NODE)).isTrue()
        assertThat(GroveExchangeIdentity.fullUrl(node)).startsWith("urn:uuid:")
    }

    @Test
    fun `fails closed on values outside the frozen grammar`() {
        assertThrows(IllegalArgumentException::class.java) {
            GroveHmacIdentityKey(
                TEST_OPAQUE_IDENTITY_SYSTEM_FAMILY,
                "test-key",
                "1",
                ByteArray(32) { index -> index.toByte() },
            )
        }
        assertThrows(IllegalArgumentException::class.java) {
            HealthConnectIdentity.exchange(TEST_EVENT_SYSTEM, TEST_PRODUCER_INSTANCE.uppercase(), EventSequence("1"))
        }
        assertThrows(IllegalArgumentException::class.java) {
            HealthConnectIdentity.record(key, REPOSITORY_SCOPE_KEY, "FutureRecord", "record")
        }
        assertThrows(IllegalArgumentException::class.java) {
            HealthConnectIdentity.specimenOutput(key, source, "SPECIMEN_SOURCE_TEARS")
        }
    }

    private companion object {
        val REPOSITORY_SCOPE_KEY = FhirIdentifierKey(
            "urn:uuid:1f5c58aa-6ec6-4e79-a682-829a9debd3f5",
            "default",
        )
    }
}
