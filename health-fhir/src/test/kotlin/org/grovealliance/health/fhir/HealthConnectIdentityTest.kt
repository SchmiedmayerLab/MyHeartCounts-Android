//
// This source file belongs to the My Heart Counts Android project
//
// SPDX-FileCopyrightText: 2026 Stanford University and the project authors (see CONTRIBUTORS.md)
//
// SPDX-License-Identifier: MIT

package org.grovealliance.health.fhir

import com.google.common.truth.Truth.assertThat
import org.hl7.fhir.r4.model.Identifier
import org.junit.Assert.assertThrows
import org.junit.Test
import java.time.Instant

class HealthConnectIdentityTest {
    private val source = identifier(
        HealthConnectContract.HEALTH_CONNECT_RECORD_IDENTIFIER,
        RECORD_VALUE,
    )

    @Test
    fun `matches the normative recording-device identity vector`() {
        // The same preimage is published in grove-fhir catalog/exchange-identity.json and is
        // reproduced by the Swift producer, so the three implementations agree byte for byte.
        assertThat(
            HealthConnectIdentity.recordingDeviceValue(
                "Patient/1a2b3c",
                "Google",
                "Pixel Watch",
            ),
        ).isEqualTo("v1:Patient/1a2b3c|health-connect|Google|Pixel Watch|")
    }

    @Test
    fun `one participant's recorder is one device and two participants' are not`() {
        val mine = HealthConnectIdentity.recordingDeviceValue("Patient/1a2b3c", "Google", "Pixel Watch")
        val again = HealthConnectIdentity.recordingDeviceValue("Patient/1a2b3c", "Google", "Pixel Watch")
        val yours = HealthConnectIdentity.recordingDeviceValue("Patient/9z8y7x", "Google", "Pixel Watch")
        assertThat(mine).isEqualTo(again)
        assertThat(mine).isNotEqualTo(yours)
    }

    @Test
    fun `a record naming too little has no admitted device identity`() {
        assertThat(HealthConnectIdentity.recordingDeviceValue("Patient/1a2b3c", "Google", null)).isNull()
        assertThat(HealthConnectIdentity.recordingDeviceValue("Patient/1a2b3c", null, "Pixel Watch")).isNull()
        assertThat(HealthConnectIdentity.recordingDeviceValue("", "Google", "Pixel Watch")).isNull()
    }

    @Test
    fun `matches every normative Health Connect identity vector`() {
        assertThat(
            HealthConnectIdentity.recordValue(
                REPOSITORY_SCOPE,
                "HeartRateRecord",
                "record-heart-001",
            ),
        ).isEqualTo(RECORD_VALUE)
        assertThat(
            HealthConnectIdentity.heartRateSampleOutput(
                source,
                Instant.parse("2026-08-20T17:30:15Z"),
                0,
            ).value,
        ).isEqualTo("v1:1f5c58aa-6ec6-4e79-a682-829a9debd3f5|HeartRateRecord|record-heart-001|sample|2026-08-20T17:30:15.000000000Z|0")
        assertThat(
            HealthConnectIdentity.sleepStageOutput(
                source,
                Instant.parse("2026-08-20T17:30:00Z"),
                Instant.parse("2026-08-20T18:00:00Z"),
                "STAGE_TYPE_LIGHT",
                0,
            ).value,
        ).isEqualTo(
            RECORD_VALUE + "|sleep-stage|2026-08-20T17:30:00.000000000Z" +
                "|2026-08-20T18:00:00.000000000Z|STAGE_TYPE_LIGHT|0"
        )
        assertThat(
            HealthConnectIdentity.specimen(source, "SPECIMEN_SOURCE_WHOLE_BLOOD").value,
        ).isEqualTo("v1:1f5c58aa-6ec6-4e79-a682-829a9debd3f5|HeartRateRecord|record-heart-001|specimen|SPECIMEN_SOURCE_WHOLE_BLOOD")
        // Export-created nodes live in the deployment's namespace, so they carry no scheme prefix.
        assertThat(HealthConnectIdentity.conversion(GRAPH_SYSTEM, REPOSITORY_SCOPE, EventSequence("1")).value)
            .isEqualTo("1f5c58aa-6ec6-4e79-a682-829a9debd3f5|1|conversion-provenance")
        assertThat(HealthConnectIdentity.exchange(GRAPH_SYSTEM, REPOSITORY_SCOPE, EventSequence("1")).value)
            .isEqualTo("1f5c58aa-6ec6-4e79-a682-829a9debd3f5|1|exchange-bundle")
    }

    @Test
    fun `an export event identity is keyed by the event, not by any one record`() {
        // One export covers many Records, so the identity names the event and its repository.
        assertThat(HealthConnectIdentity.exchange(GRAPH_SYSTEM, REPOSITORY_SCOPE, EventSequence("42")).value)
            .isEqualTo(HealthConnectIdentity.exchange(GRAPH_SYSTEM, REPOSITORY_SCOPE, EventSequence("42")).value)
        assertThat(HealthConnectIdentity.exchange(GRAPH_SYSTEM, REPOSITORY_SCOPE, EventSequence("43")).value)
            .isNotEqualTo(HealthConnectIdentity.exchange(GRAPH_SYSTEM, REPOSITORY_SCOPE, EventSequence("42")).value)
    }

    @Test
    fun `a separator in a source value is rejected, never escaped`() {
        assertThrows(IllegalArgumentException::class.java) {
            HealthConnectIdentity.recordValue(REPOSITORY_SCOPE, "HeartRateRecord", "record|heart")
        }
    }

    @Test
    fun `fails closed on values outside the frozen lexical grammar`() {
        assertThrows(IllegalArgumentException::class.java) {
            HealthConnectIdentity.recordValue(REPOSITORY_SCOPE.uppercase(), "HeartRateRecord", "record")
        }
        assertThrows(IllegalArgumentException::class.java) {
            HealthConnectIdentity.recordValue(REPOSITORY_SCOPE, "FutureRecord", "record")
        }
        assertThrows(IllegalArgumentException::class.java) {
            HealthConnectIdentity.recordValue(REPOSITORY_SCOPE, "HeartRateRecord", "prefix\ud800suffix")
        }
        assertThrows(IllegalArgumentException::class.java) {
            HealthConnectIdentity.conversion(GRAPH_SYSTEM, REPOSITORY_SCOPE, EventSequence("01"))
        }
        assertThrows(IllegalArgumentException::class.java) {
            HealthConnectIdentity.sleepStageOutput(
                source,
                Instant.parse("2026-08-20T17:30:00Z"),
                Instant.parse("2026-08-20T18:00:00Z"),
                "LIGHT",
                0,
            )
        }
        assertThrows(IllegalArgumentException::class.java) {
            HealthConnectIdentity.specimen(source, "SPECIMEN_SOURCE_TEARS")
        }
    }

    private fun identifier(system: String, value: String): Identifier =
        Identifier().setSystem(system).setValue(value)

    private companion object {
        const val REPOSITORY_SCOPE = "1f5c58aa-6ec6-4e79-a682-829a9debd3f5"
        const val GRAPH_SYSTEM = "urn:grove:health-connect-graph:org.grovealliance.example"
        const val RECORD_VALUE = "v1:1f5c58aa-6ec6-4e79-a682-829a9debd3f5|HeartRateRecord|record-heart-001"
    }
}
