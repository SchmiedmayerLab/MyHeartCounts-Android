//
// This source file is part of the My Heart Counts Android open-source project
//
// SPDX-FileCopyrightText: 2026 Stanford University and the project authors (see CONTRIBUTORS.md)
//
// SPDX-License-Identifier: MIT

package org.grovealliance.health.fhir

import androidx.health.connect.client.feature.ExperimentalMindfulnessSessionApi
import androidx.health.connect.client.records.BloodGlucoseRecord
import androidx.health.connect.client.records.StepsRecord
import androidx.health.connect.client.records.WeightRecord
import androidx.health.connect.client.records.metadata.Metadata
import androidx.health.connect.client.units.Mass
import com.google.common.truth.Truth.assertThat
import org.hl7.fhir.r4.model.Coding
import org.hl7.fhir.r4.model.Patient
import org.hl7.fhir.r4.model.Specimen
import org.junit.Assert.assertThrows
import org.junit.Test
import java.time.Instant
import java.time.ZoneOffset
import org.hl7.fhir.r4.model.Device as FhirDevice

@OptIn(ExperimentalMindfulnessSessionApi::class)
class HealthConnectR4ConverterIdentityContextTest : HealthConnectR4ConverterTestSupport() {
    @Test
    fun `repository scope generator returns a complete deployment scope pair`() {
        val generated = HealthConnectSynchronizationScope.generateRepositoryScope()

        assertThat(generated.system)
            .matches("urn:uuid:[0-9a-f]{8}-[0-9a-f]{4}-4[0-9a-f]{3}-[89ab][0-9a-f]{3}-[0-9a-f]{12}")
        assertThat(generated.value).isEqualTo("default")
        assertThrows(IllegalArgumentException::class.java) {
            FhirIdentifierKey("relative", "default")
        }
    }

    @Test
    fun `recording-device stable and event identities are opaque and the snapshot is the entry key`() {
        val admitted = HealthConnectRecordingDeviceResource(
            stablePerUnitToken = "watch-unit-token-42",
            resource = FhirDevice().apply {
                meta.addProfile(HealthConnectContract.MOBILE_RECORDING_DEVICE_PROFILE)
            },
        )
        val governedConverter = HealthConnectConverter(
            fhirContext.copy(recordingDevice = { admitted }),
            synchronizationScope,
        )
        val sourceRecord = StepsRecord(
            startTime = Instant.parse("2026-08-19T16:00:00Z"),
            startZoneOffset = ZoneOffset.UTC,
            endTime = Instant.parse("2026-08-19T17:00:00Z"),
            endZoneOffset = ZoneOffset.UTC,
            count = 1,
            metadata = metadata(Metadata.autoRecorded(device)),
        )
        val result = governedConverter.convert(sourceRecord, convertedAt)
        val wire = HealthConnectWireFormat.bundleJson(result.bundle)
        assertThat(wire).doesNotContain("watch-unit-token-42")
        val recorderEntry = result.bundle.entry.single { entry ->
            entry.resource.meta.profile.any {
                it.value == HealthConnectContract.MOBILE_RECORDING_DEVICE_PROFILE
            }
        }
        val recorder = recorderEntry.resource as FhirDevice
        assertThat(recorder.identifier).hasSize(2)
        assertThat(recorder.identifier.single { it.hasGroveRole(GroveIdentifierRole.RECORDING_DEVICE) }.value)
            .startsWith("v0:test-key:1:")
        val snapshot = recorder.identifier.single { it.hasGroveRole(GroveIdentifierRole.DEVICE_SNAPSHOT) }
        assertThat(recorderEntry.fullUrl).isEqualTo(GroveExchangeIdentity.fullUrl(snapshot))

        admitted.resource.serialNumber = "late-mutation"
        assertThrows(IllegalArgumentException::class.java) {
            governedConverter.convert(sourceRecord, convertedAt)
        }
    }

    @Test
    fun `recording-device serial number and caller identifiers are rejected`() {
        val hardwareIdentifier = identifier("https://hardware.example.org/devices", "watch-42")

        assertThrows(IllegalArgumentException::class.java) {
            HealthConnectRecordingDeviceResource(
                stablePerUnitToken = "watch-unit-token-42",
                resource = FhirDevice().apply {
                    addIdentifier(hardwareIdentifier.copy())
                    serialNumber = "globally-linkable-serial"
                },
            )
        }
    }

    @Test
    fun `recording Device is omitted when deployment has no governed stable unit evidence`() {
        val noRecorder = HealthConnectConverter(
            fhirContext.copy(recordingDevice = { null }),
            synchronizationScope,
        ).convert(stepRecord(), convertedAt)

        val devices = noRecorder.bundle.entry.map { it.resource }.filterIsInstance<FhirDevice>()
        assertThat(devices.none { device ->
            device.meta.profile.any { it.value == HealthConnectContract.MOBILE_RECORDING_DEVICE_PROFILE }
        }).isTrue()
        assertThat(noRecorder.observations.single().hasDevice()).isFalse()
    }

    @Test
    fun `host and applications are separate event snapshots linked by Device parent`() {
        val hostToken = "governed-host-token"
        val hostIdentifier = HealthConnectIdentity.deviceSnapshot(
            testIdentityKey(),
            conversionEventIdentifier,
            "host",
            hostToken,
        )
        val hostTemplate = HealthConnectHostDeviceResource(
            sourceDeviceToken = hostToken,
            resource = FhirDevice().apply {
                meta.addProfile(HealthConnectContract.MOBILE_HOST_DEVICE_PROFILE)
                manufacturer = "Example Host Company"
                modelNumber = "Phone One"
                addVersion()
                    .setType(
                        org.hl7.fhir.r4.model.CodeableConcept(
                            Coding(
                                HealthConnectContract.GROVE_APPLICATION_VERSION_TYPE,
                                "os-version",
                                "Operating system version",
                            ),
                        ),
                    )
                    .setValue("20.1")
            },
        )

        val result = HealthConnectConverter(
            fhirContext.copy(assemblerHost = hostTemplate),
            synchronizationScope,
        ).convert(stepRecord(), convertedAt)
        val hostEntry = result.bundle.entry.single {
            it.resource.meta.profile.any { profile ->
                profile.value == HealthConnectContract.MOBILE_HOST_DEVICE_PROFILE
            }
        }
        assertThat(hostEntry.fullUrl).isEqualTo(GroveExchangeIdentity.fullUrl(hostIdentifier))
        assertThat((hostEntry.resource as FhirDevice).identifier).hasSize(1)
        val applications = result.bundle.entry.map { it.resource }.filterIsInstance<FhirDevice>()
            .filter { device ->
                device.meta.profile.any { it.value == HealthConnectContract.MOBILE_APPLICATION_DEVICE_PROFILE }
            }
        assertThat(applications).hasSize(1)
        assertThat(applications.map { it.parent.reference }.distinct())
            .containsExactly(hostEntry.fullUrl)
        assertThat(HealthConnectWireFormat.bundleJson(result.bundle)).doesNotContain(hostToken)
    }

    @Test
    fun `context entry identity mutation after construction fails closed`() {
        (fhirContext.subject as HealthConnectPatientSubject.Bundled)
            .patient.entryIdentifier.value = "mutated-patient-entry"

        assertThrows(IllegalArgumentException::class.java) {
            converter.convert(stepRecord(), convertedAt)
        }
    }

    @Test
    fun `identifier-only logical Patient stays typed and does not fabricate a Bundle node`() {
        val logicalIdentifier = identifier(
            "https://deployment.example/fhir/NamingSystem/patient-pseudonym",
            "participant-42",
        )
        val logicalContext = fhirContext.copy(
            subject = HealthConnectPatientSubject.Logical(logicalIdentifier),
        )
        val result = HealthConnectConverter(logicalContext, synchronizationScope).convert(
            bloodGlucoseRecord(BloodGlucoseRecord.SPECIMEN_SOURCE_CAPILLARY_BLOOD, "logical-subject"),
            convertedAt,
        )
        val observationSubject = result.observations.single().subject
        val specimenSubject = result.bundle.entry.map { it.resource }.filterIsInstance<Specimen>().single().subject

        assertThat(result.bundle.entry.none { it.resource is Patient }).isTrue()
        listOf(observationSubject, specimenSubject).forEach { subject ->
            assertThat(subject.hasReference()).isFalse()
            assertThat(subject.type).isEqualTo("Patient")
            assertThat(subject.identifier.system).isEqualTo(logicalIdentifier.system)
            assertThat(subject.identifier.value).isEqualTo(logicalIdentifier.value)
        }
        assertThat(HealthConnectWireFormat.bundleJson(result.bundle))
            .doesNotContain(GroveExchangeIdentity.fullUrl(logicalIdentifier))
    }

    @Test
    fun `logical Patient requires a complete absolute-system identifier and is revalidated`() {
        assertThrows(IllegalArgumentException::class.java) {
            HealthConnectPatientSubject.Logical(identifier("relative", "participant-42"))
        }
        assertThrows(IllegalArgumentException::class.java) {
            HealthConnectPatientSubject.Logical(
                identifier(HealthConnectContract.GROVE_IDENTIFIER_ROLE, "participant-42"),
            )
        }
        assertThrows(IllegalArgumentException::class.java) {
            HealthConnectPatientSubject.Logical(
                identifier("https://deployment.example/fhir/patient", "participant-42").apply {
                    type.addCoding(
                        Coding(
                            HealthConnectContract.GROVE_IDENTIFIER_ROLE,
                            GroveIdentifierRole.SOURCE_RECORD.code,
                            null,
                        ),
                    )
                },
            )
        }
        val logical = HealthConnectPatientSubject.Logical(
            identifier("https://deployment.example/fhir/patient", "participant-42"),
        )
        val logicalConverter = HealthConnectConverter(fhirContext.copy(subject = logical), synchronizationScope)
        logical.identifier.value = ""

        assertThrows(IllegalArgumentException::class.java) {
            logicalConverter.convert(stepRecord(), convertedAt)
        }
    }

    @Test
    fun `static context resource mutation after construction is revalidated`() {
        fhirContext.assembler.resource.meta.profile.clear()

        assertThrows(IllegalArgumentException::class.java) {
            converter.convert(stepRecord(), convertedAt)
        }
    }

    @Test
    fun `recording callback cannot invalidate a previously checked static resource`() {
        val mutatingContext = fhirContext.copy(
            recordingDevice = {
                fhirContext.assembler.resource.meta.profile.clear()
                null
            },
        )

        assertThrows(IllegalArgumentException::class.java) {
            HealthConnectConverter(mutatingContext, synchronizationScope)
                .convert(stepRecord(), convertedAt)
        }
    }

    @Test
    fun `conversion result protects its validated graph from caller mutation`() {
        val result = converter.convert(stepRecord(), convertedAt)
        val expectedSourceValue = result.sourceRecordIdentifier.value
        val expectedObservation = result.observations.single()
        val expectedProvenance = requireNotNull(result.provenance)
        val expectedBundleJson = HealthConnectWireFormat.bundleJson(result.bundle)

        result.sourceRecordIdentifier.value = "caller-mutated-source"
        result.observations.single().identifier.clear()
        requireNotNull(result.provenance).target.clear()
        result.bundle.entry.clear()

        assertThat(result.sourceRecordIdentifier.value).isEqualTo(expectedSourceValue)
        assertThat(result.observations.single().equalsDeep(expectedObservation)).isTrue()
        assertThat(requireNotNull(result.provenance).equalsDeep(expectedProvenance)).isTrue()
        assertThat(HealthConnectWireFormat.bundleJson(result.bundle)).isEqualTo(expectedBundleJson)
    }

    @Test
    fun `record class participates in repository-scoped source identity`() {
        val steps = converter.convert(
            StepsRecord(
                startTime = Instant.parse("2026-08-19T16:00:00Z"),
                startZoneOffset = ZoneOffset.UTC,
                endTime = Instant.parse("2026-08-19T17:00:00Z"),
                endZoneOffset = ZoneOffset.UTC,
                count = 1,
                metadata = metadata(Metadata.autoRecorded(device), id = "same-raw-id"),
            ),
            convertedAt,
        )
        val weight = converter.convert(
            WeightRecord(
                time = Instant.parse("2026-08-19T16:00:00Z"),
                zoneOffset = ZoneOffset.UTC,
                weight = Mass.kilograms(70.0),
                metadata = metadata(Metadata.manualEntry(), id = "same-raw-id"),
            ),
            convertedAt,
        )

        assertThat(steps.sourceRecordIdentifier.value).isNotEqualTo(weight.sourceRecordIdentifier.value)
    }
}
