//
// This source file is part of the My Heart Counts Android open-source project
//
// SPDX-FileCopyrightText: 2026 Stanford University and the project authors (see CONTRIBUTORS.md)
//
// SPDX-License-Identifier: MIT

package org.grovealliance.health.fhir

import androidx.health.connect.client.feature.ExperimentalMindfulnessSessionApi
import androidx.health.connect.client.records.ExerciseSessionRecord
import androidx.health.connect.client.records.HeartRateRecord
import androidx.health.connect.client.records.StepsRecord
import androidx.health.connect.client.records.WeightRecord
import androidx.health.connect.client.records.metadata.DataOrigin
import androidx.health.connect.client.records.metadata.Metadata
import androidx.health.connect.client.testing.populatedWithTestValues
import androidx.health.connect.client.units.Mass
import com.google.common.truth.Truth.assertThat
import org.hl7.fhir.r4.model.Observation
import org.hl7.fhir.r4.model.Patient
import org.hl7.fhir.r4.model.Provenance
import org.hl7.fhir.r4.model.Reference
import org.junit.Assert.assertThrows
import org.junit.Test
import java.time.Instant
import java.time.ZoneOffset
import java.util.UUID
import org.hl7.fhir.r4.model.Device as FhirDevice

@OptIn(ExperimentalMindfulnessSessionApi::class)
class HealthConnectR4ConverterGraphIdentityTest : HealthConnectR4ConverterTestSupport() {
    @Test
    fun `does not impose an arbitrary transport cap on heart-rate samples`() {
        val start = Instant.parse("2026-08-19T16:00:00Z")
        val samples = List(201) { index ->
            HeartRateRecord.Sample(
                time = start.plusSeconds(index.toLong()),
                beatsPerMinute = 60L + index % 20,
            )
        }
        val record = HeartRateRecord(
            startTime = start,
            startZoneOffset = null,
            endTime = start.plusSeconds(201),
            endZoneOffset = null,
            samples = samples,
            metadata = metadata(Metadata.autoRecorded(device), id = "large-heart-rate-series"),
        )

        val conversion = converter.convert(record, convertedAt)

        assertThat(conversion.observations).hasSize(201)
        assertThat(conversion.observationIdentifiers.map { it.value }.distinct()).hasSize(201)
    }

    @Test
    fun `heart-rate output identity is stable across replay and sample order`() {
        val start = Instant.parse("2026-08-19T17:30:00Z")
        val first = heartRateRecord(
            start = start,
            samples = listOf(
                HeartRateRecord.Sample(start.plusSeconds(15), 72),
                HeartRateRecord.Sample(start.plusSeconds(45), 75),
            ),
        )
        val reordered = heartRateRecord(
            start = start,
            samples = first.samples.reversed(),
        )

        val firstIdentifiers = converter.convert(first, convertedAt).observationIdentifiers.map { it.value }
        val replayedIdentifiers = converter.convert(reordered, convertedAt).observationIdentifiers.map { it.value }

        assertThat(replayedIdentifiers).containsExactlyElementsIn(firstIdentifiers).inOrder()
    }

    @Test
    fun `business identities drive RFC UUIDv5 references without producer resource ids`() {
        val record = heartRateRecord(
            start = Instant.parse("2026-08-19T17:30:00Z"),
            samples = listOf(HeartRateRecord.Sample(Instant.parse("2026-08-19T17:30:15Z"), 72)),
        )

        val first = converter.convert(record, convertedAt, EventSequence("41"))
        val replay = converter.convert(record, convertedAt, EventSequence("41"))
        val nextEvent = converter.convert(record, convertedAt, EventSequence("42"))
        val firstConversion = entryIdentifier(first.bundle, "Provenance")
        val replayConversion = entryIdentifier(replay.bundle, "Provenance")
        val nextConversion = entryIdentifier(nextEvent.bundle, "Provenance")
        val referenceUuids = first.bundle.entry.map { entry ->
            UUID.fromString(requireNotNull(entry.fullUrl).removePrefix("urn:uuid:"))
        }

        assertThat(referenceUuids.map(UUID::version).distinct()).containsExactly(5)
        assertThat(referenceUuids.map(UUID::variant).distinct()).containsExactly(2)
        assertThat(first.bundle.id).isNull()
        assertThat(first.provenance!!.id).isNull()
        assertThat(first.observations.single().id).isNull()
        assertThat(replay.bundle.identifier.value).isEqualTo(first.bundle.identifier.value)
        assertThat(replayConversion.value).isEqualTo(firstConversion.value)
        assertThat(nextEvent.bundle.identifier.value).isNotEqualTo(first.bundle.identifier.value)
        assertThat(nextConversion.value).isNotEqualTo(firstConversion.value)
        assertThat(nextEvent.observationIdentifiers.single().value)
            .isEqualTo(first.observationIdentifiers.single().value)
        assertThat(first.bundle.identifier.value)
            .isEqualTo("e0:$TEST_PRODUCER_INSTANCE:41")
        assertThat(firstConversion.value)
            .matches("n0:conversion-provenance:0:[A-Za-z0-9_-]{43}")
    }

    @Test
    fun `supporting resources cannot inject an Observation`() {
        assertThrows(IllegalArgumentException::class.java) {
            fhirContext.copy(
                supportingResources = listOf(
                    HealthConnectBundleResource(
                        identifier(TEST_CONTEXT_IDENTIFIER_SYSTEM, "forbidden-observation"),
                        Observation(),
                    ),
                ),
            )
        }
    }

    @Test
    fun `supporting resources cannot inject conversion Provenance`() {
        assertThrows(IllegalArgumentException::class.java) {
            fhirContext.copy(
                supportingResources = listOf(
                    HealthConnectBundleResource(
                        identifier(TEST_CONTEXT_IDENTIFIER_SYSTEM, "forbidden-provenance"),
                        Provenance(),
                    ),
                ),
            )
        }
    }

    @Test
    fun `conversion rejects a Bundle whose Provenance differs from its exposed result`() {
        val valid = converter.convert(stepRecord(), convertedAt)
        val mismatchedBundle = valid.bundle.apply {
            (entry.single { it.resource is Provenance }.resource as Provenance).target.clear()
        }

        assertThrows(IllegalArgumentException::class.java) {
            HealthConnectConversion(
                conversionContractMarker = valid.conversionContractMarker,
                sourceRecordIdentifier = valid.sourceRecordIdentifier,
                sourceRecordType = valid.sourceRecordType,
                sourceLastModified = valid.sourceLastModified,
                bundle = mismatchedBundle,
            )
        }
    }

    @Test
    fun `active boundary requires the Health Connect envelope for record-type lineage`() {
        val valid = converter.convert(stepRecord(), convertedAt)
        assertThat(activeBatch(valid, valid.bundle).operation)
            .isEqualTo(HealthConnectExportOperation.ACTIVE)

        val mutated = valid.bundle.apply {
            val observation = entry.single { it.resource is Observation }.resource as Observation
            observation.meta.profile.removeIf {
                it.value == HealthConnectContract.HEALTH_CONNECT_OBSERVATION_PROFILE
            }
        }

        assertThrows(IllegalArgumentException::class.java) {
            activeBatch(valid, mutated)
        }
    }

    @Test
    fun `conversion rejects an unresolved internal UUID reference even when snapshots agree`() {
        val valid = converter.convert(stepRecord(), convertedAt)
        val unresolved = "urn:uuid:00000000-0000-5000-8000-000000000000"
        val bundle = valid.bundle.apply {
            (entry.single { it.resource is Observation }.resource as Observation).subject.reference = unresolved
        }

        assertThrows(IllegalArgumentException::class.java) {
            HealthConnectConversion(
                conversionContractMarker = valid.conversionContractMarker,
                sourceRecordIdentifier = valid.sourceRecordIdentifier,
                sourceRecordType = valid.sourceRecordType,
                sourceLastModified = valid.sourceLastModified,
                bundle = bundle,
            )
        }
    }

    @Test
    fun `all contained resources are prohibited even when their local id is valid`() {
        val bundle = converter.convert(stepRecord(), convertedAt).bundle
        val observation = bundle.entry.single { it.resource is Observation }.resource as Observation
        observation.addContained(Patient().apply { id = "valid-local-id" })

        assertThrows(IllegalArgumentException::class.java) {
            bundle.requireGroveReferencePolicy()
        }
    }

    @Test
    fun `Health Connect DataOrigin cannot become a literal event Device node`() {
        val valid = converter.convert(stepRecord(), convertedAt)
        val assemblerUrl = valid.bundle.entry.single { entry ->
            entry.resource is FhirDevice &&
                (entry.resource as FhirDevice).meta.profile.map { it.value } ==
                listOf(HealthConnectContract.MOBILE_APPLICATION_DEVICE_PROFILE)
        }.fullUrl
        val mutated = valid.bundle.apply {
            val provenance = entry.single { it.resource is Provenance }.resource as Provenance
            provenance.entity.single().agent.single().who.reference = assemblerUrl
        }

        assertThrows(IllegalArgumentException::class.java) {
            activeBatch(valid, mutated)
        }
    }

    @Test
    fun `Health Connect DataOrigin requires the exact Android package NamingSystem`() {
        val valid = converter.convert(stepRecord(), convertedAt)
        val mutated = valid.bundle.apply {
            val provenance = entry.single { it.resource is Provenance }.resource as Provenance
            provenance.entity.single().agent.single().who.identifier.system =
                "https://example.invalid/fhir/NamingSystem/application"
        }

        assertThrows(IllegalArgumentException::class.java) {
            activeBatch(valid, mutated)
        }
    }

    @Test
    fun `Health Connect DataOrigin requires exactly one enterer participation coding`() {
        val valid = converter.convert(stepRecord(), convertedAt)
        val mutated = valid.bundle.apply {
            val provenance = entry.single { it.resource is Provenance }.resource as Provenance
            provenance.entity.single().agent.single().type.coding.single().code = "assembler"
        }

        assertThrows(IllegalArgumentException::class.java) {
            activeBatch(valid, mutated)
        }
    }

    @Test
    fun `conversion rejects unresolved external and malformed governed Patient references`() {
        val invalidators = listOf<(Reference) -> Unit>(
            { subject -> subject.reference = "https://external.example/Patient/42" },
            { subject -> subject.identifier = identifier(TEST_CONTEXT_IDENTIFIER_SYSTEM, "mixed-subject") },
            { subject ->
                subject.reference = null
                subject.type = null
                subject.identifier = identifier(TEST_CONTEXT_IDENTIFIER_SYSTEM, "untyped-subject")
            },
        )

        invalidators.forEach { invalidate ->
            val valid = converter.convert(stepRecord(), convertedAt)
            val bundle = valid.bundle.apply {
                invalidate((entry.single { it.resource is Observation }.resource as Observation).subject)
            }
            assertThrows(IllegalArgumentException::class.java) {
                HealthConnectConversion(
                    conversionContractMarker = valid.conversionContractMarker,
                    sourceRecordIdentifier = valid.sourceRecordIdentifier,
                    sourceRecordType = valid.sourceRecordType,
                    sourceLastModified = valid.sourceLastModified,
                    bundle = bundle,
                )
            }
        }
    }

    @Test
    fun `a non-ASCII record id is accepted but never disclosed on the wire`() {
        val result = converter.convert(
            StepsRecord(
                startTime = Instant.parse("2026-08-19T16:00:00Z"),
                startZoneOffset = ZoneOffset.UTC,
                endTime = Instant.parse("2026-08-19T17:00:00Z"),
                endZoneOffset = ZoneOffset.UTC,
                count = 1,
                metadata = metadata(Metadata.autoRecorded(device), id = "héal记录"),
            ),
            convertedAt,
        )

        assertThat(result.observationIdentifiers.single().value)
            .matches("v0:test-key:1:[A-Za-z0-9_-]{43}")
        assertThat(HealthConnectWireFormat.bundleJson(result.bundle)).doesNotContain("héal记录")
    }

    @Test
    fun `malformed source UTF-16 is a typed rejection instead of an uncaught identity failure`() {
        val sourceTime = Instant.parse("2026-08-19T15:15:00Z")
        val malformedMetadata = listOf(
            metadata(Metadata.manualEntry(), id = "\uD800"),
            Metadata.manualEntry().populatedWithTestValues(
                id = "invalid-package",
                dataOrigin = DataOrigin("\uDC00"),
                lastModifiedTime = Instant.parse("2026-08-19T17:30:01Z"),
            ),
            Metadata.activelyRecorded(
                device = device,
                clientRecordId = "\uD800",
                clientRecordVersion = 1,
            ).populatedWithTestValues(
                id = "invalid-client-record",
                dataOrigin = DataOrigin("com.example.source"),
                lastModifiedTime = Instant.parse("2026-08-19T17:30:01Z"),
            ),
        )

        malformedMetadata.forEachIndexed { index, metadata ->
            val outcome = converter.convertOutcome(
                WeightRecord(
                    time = sourceTime,
                    zoneOffset = ZoneOffset.UTC,
                    weight = Mass.kilograms(68.4),
                    metadata = metadata,
                ),
                convertedAt,
                EventSequence((index + 1).toString()),
            )

            assertThat(outcome).isInstanceOf(HealthConnectConversionOutcome.Rejected::class.java)
            assertThat((outcome as HealthConnectConversionOutcome.Rejected).reason).contains("unpaired")
        }
    }

    @Test
    fun `malformed retained source text is a typed rejection`() {
        val outcome = converter.convertOutcome(
            exerciseSession(
                exerciseType = ExerciseSessionRecord.EXERCISE_TYPE_RUNNING,
                id = "invalid-title",
                title = "\uD800",
            ),
            convertedAt,
            EventSequence("1"),
        )

        assertThat(outcome).isInstanceOf(HealthConnectConversionOutcome.Rejected::class.java)
        assertThat((outcome as HealthConnectConversionOutcome.Rejected).reason)
            .contains("ExerciseSessionRecord.title contains an unpaired high surrogate")
    }

    @Test
    fun `gives same-instant heart-rate samples distinct occurrence-keyed identities`() {
        val start = Instant.parse("2026-08-19T17:30:00Z")
        val record = heartRateRecord(
            start = start,
            samples = listOf(
                HeartRateRecord.Sample(start.plusSeconds(15), 72),
                HeartRateRecord.Sample(start.plusSeconds(15), 75),
                HeartRateRecord.Sample(start.plusSeconds(15), 72),
            ),
        )

        val observations = converter.convert(record, convertedAt).observations

        val identities = observations.map { observationIdentity(it).value }
        assertThat(identities).containsNoDuplicates()
        assertThat(identities).containsExactlyElementsIn(
            converter.convert(record, convertedAt).observations.map { observationIdentity(it).value },
        ).inOrder()
        assertThat(identities.all { it.matches(Regex("v0:test-key:1:[A-Za-z0-9_-]{43}")) }).isTrue()
        assertThat(observations.map { it.effectiveDateTimeType.valueAsString }.distinct())
            .containsExactly("2026-08-19T17:30:15Z")
    }

    @Test
    fun `source-list occurrence allocation preserves exact platform order`() {
        val sourceOrder = listOf(
            "same:first",
            "other:only",
            "same:corrected",
            "same:last",
        )

        val identified = assignSourceListOccurrences(sourceOrder) { it.substringBefore(':') }

        assertThat(identified.map { it.first }).containsExactlyElementsIn(sourceOrder).inOrder()
        assertThat(identified.map { it.second }).containsExactly(0, 0, 1, 2).inOrder()
    }

    @Test
    fun `same-instant sample correction retains each source-list occurrence identity`() {
        val start = Instant.parse("2026-08-19T17:30:00Z")
        val sampleTime = start.plusSeconds(15)
        val original = heartRateRecord(
            start = start,
            samples = listOf(
                HeartRateRecord.Sample(sampleTime, 80),
                HeartRateRecord.Sample(sampleTime, 70),
            ),
        )
        val corrected = heartRateRecord(
            start = start,
            samples = listOf(
                HeartRateRecord.Sample(sampleTime, 60),
                HeartRateRecord.Sample(sampleTime, 70),
            ),
        )

        val originalOutputs = converter.convert(original, convertedAt).observations
        val correctedOutputs = converter.convert(corrected, convertedAt).observations

        assertThat(correctedOutputs.map { observationIdentity(it).value })
            .containsExactlyElementsIn(originalOutputs.map { observationIdentity(it).value })
            .inOrder()
        assertThat(originalOutputs.map { it.valueQuantity.value.toLong() }).containsExactly(80L, 70L).inOrder()
        assertThat(correctedOutputs.map { it.valueQuantity.value.toLong() }).containsExactly(60L, 70L).inOrder()
    }
}
