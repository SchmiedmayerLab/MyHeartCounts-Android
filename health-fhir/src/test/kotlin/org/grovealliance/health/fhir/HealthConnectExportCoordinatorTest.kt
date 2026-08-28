//
// This source file belongs to the My Heart Counts Android project
//
// SPDX-FileCopyrightText: 2026 Stanford University and the project authors (see CONTRIBUTORS.md)
//
// SPDX-License-Identifier: MIT

package org.grovealliance.health.fhir

import androidx.health.connect.client.records.ActiveCaloriesBurnedRecord
import androidx.health.connect.client.records.BasalBodyTemperatureRecord
import androidx.health.connect.client.records.BloodGlucoseRecord
import androidx.health.connect.client.records.BloodPressureRecord
import androidx.health.connect.client.records.BodyTemperatureMeasurementLocation
import androidx.health.connect.client.records.BodyTemperatureRecord
import androidx.health.connect.client.records.DistanceRecord
import androidx.health.connect.client.records.HeartRateRecord
import androidx.health.connect.client.records.HeightRecord
import androidx.health.connect.client.records.MealType
import androidx.health.connect.client.records.OxygenSaturationRecord
import androidx.health.connect.client.records.Record
import androidx.health.connect.client.records.RespiratoryRateRecord
import androidx.health.connect.client.records.SleepSessionRecord
import androidx.health.connect.client.records.StepsRecord
import androidx.health.connect.client.records.WeightRecord
import androidx.health.connect.client.records.metadata.DataOrigin
import androidx.health.connect.client.records.metadata.Device
import androidx.health.connect.client.records.metadata.Metadata
import androidx.health.connect.client.testing.populatedWithTestValues
import androidx.health.connect.client.units.BloodGlucose
import androidx.health.connect.client.units.Energy
import androidx.health.connect.client.units.Length
import androidx.health.connect.client.units.Mass
import androidx.health.connect.client.units.Percentage
import androidx.health.connect.client.units.Pressure
import androidx.health.connect.client.units.Temperature
import com.google.common.truth.Truth.assertThat
import kotlinx.coroutines.test.runTest
import org.grovealliance.health.RecordType
import org.hl7.fhir.r4.formats.IParser
import org.hl7.fhir.r4.formats.JsonParser
import org.hl7.fhir.r4.model.Bundle
import org.hl7.fhir.r4.model.CodeableConcept
import org.hl7.fhir.r4.model.Coding
import org.hl7.fhir.r4.model.Identifier
import org.hl7.fhir.r4.model.Observation
import org.hl7.fhir.r4.model.Patient
import org.hl7.fhir.r4.model.Provenance
import org.hl7.fhir.r4.model.ResearchStudy
import org.junit.Test
import java.io.File
import java.time.Instant
import java.time.ZoneOffset
import org.hl7.fhir.r4.model.Device as FhirDevice

@Suppress("LargeClass")
class HealthConnectExportCoordinatorTest {
    private val conversionTime = Instant.parse("2026-08-19T18:00:00Z")
    private val semanticConversionTime = Instant.parse("2026-08-21T18:00:00Z")
    private val watch = Device(Device.TYPE_WATCH, "Example Device Company", "Study Watch")
    private val synchronizationScope = HealthConnectSynchronizationScope.create(
        repositoryScope = EXAMPLE_REPOSITORY_SCOPE,
        configurationFingerprint = "all-supported-records-v1",
    )
    private val converter = HealthConnectConverter(fhirContext(), synchronizationScope)

    @Test
    fun `equal-version replay is an acknowledged no-op`() = runTest {
        val journal = InMemoryJournal()
        val sink = RecordingSink()
        val coordinator = HealthConnectExportCoordinator(converter, journal, sink)
        val record = heartRateRecord(twoHeartRateSamples())

        coordinator.upsert(record, conversionTime)
        coordinator.upsert(record, conversionTime.plusSeconds(1))

        assertThat(sink.batches).hasSize(1)
        assertThat(journal.entries("HeartRateRecord")).hasSize(1)
    }

    @Test
    fun `event sequence accepts a changed payload even when the source timestamp decreases`() = runTest {
        val journal = InMemoryJournal()
        val sink = RecordingSink()
        val coordinator = HealthConnectExportCoordinator(converter, journal, sink)
        val newer = heartRateRecord(
            twoHeartRateSamples(),
            lastModified = Instant.parse("2026-08-19T17:30:02Z"),
        )
        val changedWithOlderTimestamp = heartRateRecord(
            twoHeartRateSamples().take(1),
            lastModified = Instant.parse("2026-08-19T17:30:01Z"),
        )

        coordinator.reconcile("HeartRateRecord", listOf(newer), conversionTime)
        coordinator.upsert(changedWithOlderTimestamp, conversionTime.plusSeconds(1))

        assertThat(sink.batches).hasSize(2)
        assertThat(sink.batches[1].eventSequence).isGreaterThan(sink.batches[0].eventSequence)
        assertThat(requireNotNull(journal.entry("HeartRateRecord", "heart-record")).sourceLastModified)
            .isEqualTo(Instant.parse("2026-08-19T17:30:01Z"))
    }

    @Test
    fun `event sequence accepts a changed payload at the same source timestamp`() = runTest {
        val journal = InMemoryJournal()
        val sink = RecordingSink()
        val coordinator = HealthConnectExportCoordinator(converter, journal, sink)

        coordinator.upsert(stepRecord("same-timestamp", count = 1042), conversionTime)
        coordinator.upsert(stepRecord("same-timestamp", count = 2042), conversionTime.plusSeconds(1))

        assertThat(sink.batches).hasSize(2)
        assertThat(sink.batches.map { it.wireSourceVersion }.distinct()).hasSize(1)
        assertThat(sink.batches[1].eventSequence).isGreaterThan(sink.batches[0].eventSequence)
    }

    @Test
    fun `pending outbox replays the exact acknowledged payload after journal failure`() = runTest {
        val journal = InMemoryJournal().apply { failCompleteNext = true }
        val sink = RecordingSink()
        val coordinator = HealthConnectExportCoordinator(converter, journal, sink)
        val record = stepRecord("step-record")

        val failure = runCatching { coordinator.upsert(record, conversionTime) }.exceptionOrNull()
        assertThat(failure).isInstanceOf(IllegalStateException::class.java)

        coordinator.upsert(record, conversionTime.plusSeconds(30))

        assertThat(sink.batches).hasSize(2)
        assertThat(sink.batches[1].eventSequence).isEqualTo(sink.batches[0].eventSequence)
        assertThat(sink.batches[1].bundleJson).isEqualTo(sink.batches[0].bundleJson)
        assertThat(sink.batches[1].payloadSha256).isEqualTo(sink.batches[0].payloadSha256)
        assertThat(sink.batches[1].bundle.equalsDeep(sink.batches[0].bundle)).isTrue()
        assertThat(sink.batches[1].sourceVersion).isEqualTo(sink.batches[0].sourceVersion)
        assertThat(sink.batches[1].wireSourceVersion).isEqualTo("1787160601000000000")
        assertThat(sink.batches[1].wireOperation).isEqualTo("upsert")
        assertThat(requireNotNull(journal.entry("StepsRecord", "step-record")).destinationReferences)
            .isNotEmpty()
        assertThat(requireNotNull(journal.entry("StepsRecord", "step-record")).lastEventSequence)
            .isEqualTo(sink.batches.last().eventSequence)
    }

    @Test
    fun `expired baseline drains an orphaned first upsert then tombstones its absent source`() = runTest {
        val journal = InMemoryJournal().apply { failCompleteNext = true }
        val sink = RecordingSink()
        val coordinator = HealthConnectExportCoordinator(converter, journal, sink)

        runCatching { coordinator.upsert(stepRecord("removed-before-recovery"), conversionTime) }
        coordinator.reconcile("StepsRecord", emptyList(), conversionTime.plusSeconds(30))

        assertThat(sink.batches).hasSize(3)
        assertThat(sink.batches[1].eventSequence).isEqualTo(sink.batches[0].eventSequence)
        assertThat(sink.batches[1].bundleJson).isEqualTo(sink.batches[0].bundleJson)
        assertThat(sink.batches[2].operation).isEqualTo(HealthConnectExportOperation.DELETE)
        assertThat(sink.batches[2].bundle.observations().map { it.status }.distinct())
            .containsExactly(Observation.ObservationStatus.ENTEREDINERROR)
        assertThat(requireNotNull(journal.entry("StepsRecord", "removed-before-recovery")).state)
            .isEqualTo(HealthConnectExportState.INVALIDATED)
    }

    @Test
    fun `hydrated outbox rejects a Bundle that differs from its exact JSON`() = runTest {
        val journal = InMemoryJournal().apply { failCompleteNext = true }
        val coordinator = HealthConnectExportCoordinator(converter, journal, RecordingSink())
        runCatching { coordinator.upsert(stepRecord("corrupt-pending"), conversionTime) }
        val pending = requireNotNull(journal.pending("StepsRecord", "corrupt-pending"))

        val failure = runCatching {
            pending.copy(bundle = pending.bundle.copy().apply { id = "different-bundle" })
        }.exceptionOrNull()

        assertThat(failure).isInstanceOf(IllegalArgumentException::class.java)
    }

    @Test
    fun `wire source version retains Health Connect nanosecond precision`() {
        assertThat(
            HealthConnectWireFormat.sourceVersion(Instant.parse("2026-08-19T17:30:01.123456789Z")),
        ).isEqualTo("1787160601123456789")
    }

    @Test
    fun `FHIR-incompatible source time is durably rejected without a wire event`() = runTest {
        val journal = InMemoryJournal()
        val sink = RecordingSink()
        val coordinator = HealthConnectExportCoordinator(converter, journal, sink)
        val record = WeightRecord(
            time = Instant.parse("2026-08-19T15:15:00Z"),
            zoneOffset = ZoneOffset.ofHours(18),
            weight = Mass.kilograms(68.4),
            metadata = metadata(Metadata.manualEntry(), "invalid-fhir-offset"),
        )

        coordinator.upsert(record, conversionTime)

        assertThat(sink.batches).isEmpty()
        assertThat(journal.pending("WeightRecord", "invalid-fhir-offset")).isNull()
        assertThat(journal.rejectedRecords).hasSize(1)
    }

    @Test
    fun `heart-rate update invalidates samples removed from the output set`() = runTest {
        val journal = InMemoryJournal()
        val sink = RecordingSink()
        val coordinator = HealthConnectExportCoordinator(converter, journal, sink)
        coordinator.upsert(heartRateRecord(twoHeartRateSamples()), conversionTime)

        coordinator.upsert(
            heartRateRecord(
                samples = twoHeartRateSamples().take(1),
                lastModified = Instant.parse("2026-08-19T17:30:02Z"),
            ),
            conversionTime.plusSeconds(1),
        )

        val update = sink.batches.last()
        assertThat(update.invalidatedOutputIdentifiers).hasSize(1)
        assertThat(
            update.bundle.observations().filter { it.status == Observation.ObservationStatus.ENTEREDINERROR },
        ).hasSize(1)
        val current = requireNotNull(journal.entry("HeartRateRecord", "heart-record"))
        assertThat(current.observations).hasSize(1)
        assertThat(current.state).isEqualTo(HealthConnectExportState.ACTIVE)
    }

    @Test
    fun `heart-rate update carries prior context needed by removed samples`() = runTest {
        val oldWatch = Device(Device.TYPE_WATCH, "Example Device Company", "Old Study Watch")
        val newWatch = Device(Device.TYPE_WATCH, "Example Device Company", "New Study Watch")
        val contextAwareConverter = HealthConnectConverter(
            fhirContext { source ->
                if (source.model == oldWatch.model) {
                    "old-study-watch"
                } else {
                    "new-study-watch"
                }
            },
            synchronizationScope,
        )
        val journal = InMemoryJournal()
        val sink = RecordingSink()
        val coordinator = HealthConnectExportCoordinator(contextAwareConverter, journal, sink)
        coordinator.upsert(heartRateRecord(twoHeartRateSamples(), device = oldWatch), conversionTime)

        coordinator.upsert(
            heartRateRecord(
                samples = twoHeartRateSamples().take(1),
                lastModified = Instant.parse("2026-08-19T17:30:02Z"),
                device = newWatch,
            ),
            conversionTime.plusSeconds(1),
        )

        val update = sink.batches.last().bundle
        assertThat(update.entry.map { it.fullUrl }).containsAtLeast(
            contextFullUrl("old-study-watch"),
            contextFullUrl("new-study-watch"),
        )
        assertThat(
            update.observations().single { it.status == Observation.ObservationStatus.ENTEREDINERROR }
                .device.reference,
        ).isEqualTo(contextFullUrl("old-study-watch"))
    }

    @Test
    fun `deletion is acknowledged before a durable tombstone and replay is safe`() = runTest {
        val journal = InMemoryJournal()
        val sink = RecordingSink()
        val coordinator = HealthConnectExportCoordinator(converter, journal, sink)
        coordinator.upsert(stepRecord("step-record"), conversionTime)

        coordinator.delete("StepsRecord", "step-record", conversionTime.plusSeconds(1))

        val deletion = sink.batches.last()
        assertThat(deletion.invalidatedOutputIdentifiers).hasSize(1)
        assertThat(deletion.operation).isEqualTo(HealthConnectExportOperation.DELETE)
        assertThat(deletion.wireOperation).isEqualTo("delete")
        assertThat(deletion.sourceVersion).isEqualTo(Instant.parse("2026-08-19T17:30:01Z"))
        assertThat(deletion.bundle.observations().single().status)
            .isEqualTo(Observation.ObservationStatus.ENTEREDINERROR)
        assertThat(deletion.bundle.entry.none { it.resource is Provenance }).isTrue()
        assertThat(requireNotNull(journal.entry("StepsRecord", "step-record")).state)
            .isEqualTo(HealthConnectExportState.INVALIDATED)

        val acknowledgedBatchCount = sink.batches.size
        coordinator.delete("StepsRecord", "step-record", conversionTime.plusSeconds(2))
        assertThat(sink.batches).hasSize(acknowledgedBatchCount)

        coordinator.upsert(stepRecord("step-record"), conversionTime.plusSeconds(3))
        assertThat(sink.batches).hasSize(acknowledgedBatchCount + 1)
        assertThat(sink.batches.last().operation).isEqualTo(HealthConnectExportOperation.UPSERT)
        assertThat(requireNotNull(journal.entry("StepsRecord", "step-record")).state)
            .isEqualTo(HealthConnectExportState.ACTIVE)
    }

    @Test
    fun `missing deletion journal state is durably quarantined`() = runTest {
        val journal = InMemoryJournal()
        val coordinator = HealthConnectExportCoordinator(converter, journal, RecordingSink())

        coordinator.delete("StepsRecord", "unknown", conversionTime)
        coordinator.delete("StepsRecord", "unknown", conversionTime.plusSeconds(1))

        assertThat(journal.unmatchedDeletions).containsExactly(
            HealthConnectUnmatchedDeletion(
                synchronizationScope.repositoryScopeKey,
                synchronizationScope.projectionScopeKey,
                "StepsRecord",
                "unknown",
                conversionTime,
            ),
        )
    }

    @Test
    fun `first-sight zero-output record is durably active without a wire event`() = runTest {
        val journal = InMemoryJournal()
        val sink = RecordingSink()
        val coordinator = HealthConnectExportCoordinator(converter, journal, sink)
        val empty = heartRateRecord(emptyList())

        coordinator.upsert(empty, conversionTime)

        assertThat(sink.batches).isEmpty()
        assertThat(journal.rejectedRecords).isEmpty()
        val entry = requireNotNull(journal.entry("HeartRateRecord", "heart-record"))
        assertThat(entry.state).isEqualTo(HealthConnectExportState.ACTIVE)
        assertThat(entry.observations).isEmpty()
    }

    @Test
    fun `zero-output update sends upsert tombstones and remains active`() = runTest {
        val journal = InMemoryJournal()
        val sink = RecordingSink()
        val coordinator = HealthConnectExportCoordinator(converter, journal, sink)
        coordinator.upsert(heartRateRecord(twoHeartRateSamples()), conversionTime)

        coordinator.upsert(heartRateRecord(emptyList()), conversionTime.plusSeconds(1))

        assertThat(sink.batches).hasSize(2)
        assertThat(sink.batches.last().operation).isEqualTo(HealthConnectExportOperation.UPSERT)
        assertThat(sink.batches.last().bundle.observations().map { it.status }.distinct())
            .containsExactly(Observation.ObservationStatus.ENTEREDINERROR)
        assertThat(sink.batches.last().bundle.entry.none { it.resource is Provenance }).isTrue()
        assertThat(requireNotNull(journal.entry("HeartRateRecord", "heart-record")).state)
            .isEqualTo(HealthConnectExportState.ACTIVE)
        assertThat(requireNotNull(journal.entry("HeartRateRecord", "heart-record")).observations).isEmpty()
        assertThat(journal.rejectedRecords).isEmpty()
    }

    @Test
    fun `zero-output update refreshes local source timestamp without a wire event`() = runTest {
        val journal = InMemoryJournal()
        val sink = RecordingSink()
        val coordinator = HealthConnectExportCoordinator(converter, journal, sink)
        coordinator.upsert(heartRateRecord(emptyList()), conversionTime)

        coordinator.upsert(
            heartRateRecord(emptyList(), lastModified = Instant.parse("2026-08-19T17:30:02Z")),
            conversionTime.plusSeconds(1),
        )

        assertThat(sink.batches).isEmpty()
        assertThat(requireNotNull(journal.entry("HeartRateRecord", "heart-record")).sourceLastModified)
            .isEqualTo(Instant.parse("2026-08-19T17:30:02Z"))
        assertThat(requireNotNull(journal.entry("HeartRateRecord", "heart-record")).lastEventSequence).isNull()
    }

    @Test
    fun `a new projection baseline can reactivate an equal-version tombstone`() = runTest {
        val journal = InMemoryJournal()
        val sink = RecordingSink()
        val firstCoordinator = HealthConnectExportCoordinator(converter, journal, sink)
        val record = stepRecord("reactivated")
        firstCoordinator.upsert(record, conversionTime)
        firstCoordinator.delete("StepsRecord", "reactivated", conversionTime.plusSeconds(1))
        val nextScope = HealthConnectSynchronizationScope.create(
            repositoryScope = EXAMPLE_REPOSITORY_SCOPE,
            configurationFingerprint = "expanded-filter-v2",
        )
        val nextCoordinator = HealthConnectExportCoordinator(
            HealthConnectConverter(fhirContext(), nextScope),
            journal,
            sink,
        )

        nextCoordinator.reconcile("StepsRecord", listOf(record), conversionTime.plusSeconds(2))

        assertThat(sink.batches.last().operation).isEqualTo(HealthConnectExportOperation.UPSERT)
        assertThat(requireNotNull(journal.entry("StepsRecord", "reactivated")).state)
            .isEqualTo(HealthConnectExportState.ACTIVE)
        assertThat(requireNotNull(journal.entry("StepsRecord", "reactivated")).projectionScopeKey)
            .isEqualTo(nextScope.projectionScopeKey)
    }

    @Test
    fun `adapter contract version changes projection scope and baseline re-encodes unchanged source`() = runTest {
        val journal = InMemoryJournal()
        val sink = RecordingSink()
        val record = stepRecord("contract-upgrade")
        val initialCoordinator = HealthConnectExportCoordinator(converter, journal, sink)
        initialCoordinator.reconcile("StepsRecord", listOf(record), conversionTime)
        val upgradedScope = HealthConnectSynchronizationScope.createForContractVersion(
            repositoryScope = EXAMPLE_REPOSITORY_SCOPE,
            configurationFingerprint = "all-supported-records-v1",
            conversionContractVersion = "health-connect-r4-v2-test",
        )
        val upgradedCoordinator = HealthConnectExportCoordinator(
            HealthConnectConverter(fhirContext(), upgradedScope),
            journal,
            sink,
        )

        upgradedCoordinator.reconcile("StepsRecord", listOf(record), conversionTime.plusSeconds(1))

        assertThat(upgradedScope.repositoryScopeKey).isEqualTo(synchronizationScope.repositoryScopeKey)
        assertThat(upgradedScope.projectionScopeKey).isNotEqualTo(synchronizationScope.projectionScopeKey)
        assertThat(sink.batches).hasSize(2)
        assertThat(sink.batches.map { it.eventSequence.value }).containsExactly("1", "2").inOrder()
        val migrated = requireNotNull(journal.entry("StepsRecord", "contract-upgrade"))
        assertThat(migrated.projectionScopeKey).isEqualTo(upgradedScope.projectionScopeKey)
        assertThat(migrated.conversionContractVersion).isEqualTo("health-connect-r4-v2-test")
    }

    @Test
    @Suppress("LongMethod")
    fun `contract migration retires an old source identity before publishing the new identity`() = runTest {
        val record = stepRecord("identity-upgrade")
        val oldScope = HealthConnectSynchronizationScope.create(
            repositoryScope = "86f286c0-ec67-40d9-901d-264f2e1c627e",
            configurationFingerprint = "all-supported-records-v1",
        )
        val oldConversion = HealthConnectConverter(fhirContext(), oldScope)
            .convert(record, conversionTime, EventSequence("99"))
        val journal = InMemoryJournal(startingSequence = 100).apply {
            storeLocal(
                HealthConnectExportJournalEntry(
                    repositoryScopeKey = synchronizationScope.repositoryScopeKey,
                    projectionScopeKey = synchronizationScope.projectionScopeKey,
                    recordType = oldConversion.sourceRecordType,
                    healthConnectId = record.metadata.id,
                    dataOriginPackage = record.metadata.dataOrigin.packageName,
                    sourceLastModified = oldConversion.sourceLastModified,
                    conversionContractVersion = oldConversion.conversionContractVersion,
                    sourceRecordIdentifier = oldConversion.sourceRecordIdentifier.copy(),
                    observations = oldConversion.observations.map { it.copy() },
                    bundle = oldConversion.bundle.copy(),
                    destinationReferences = oldConversion.observationIdentifiers.associate {
                        it.value to "Observation/old-${it.value}"
                    },
                    lastEventSequence = EventSequence("99"),
                ),
            )
        }
        val sink = RecordingSink()
        val upgradedScope = HealthConnectSynchronizationScope.createForContractVersion(
            repositoryScope = EXAMPLE_REPOSITORY_SCOPE,
            configurationFingerprint = "all-supported-records-v1",
            conversionContractVersion = "health-connect-r4-v2-test",
        )
        val coordinator = HealthConnectExportCoordinator(
            HealthConnectConverter(fhirContext(), upgradedScope),
            journal,
            sink,
        )

        coordinator.reconcile("StepsRecord", listOf(record), conversionTime.plusSeconds(1))

        assertThat(sink.batches.map { it.operation }).containsExactly(
            HealthConnectExportOperation.DELETE,
            HealthConnectExportOperation.UPSERT,
        ).inOrder()
        assertThat(sink.batches.map { it.eventSequence.value }).containsExactly("100", "101").inOrder()
        assertThat(sink.batches.first().sourceRecordIdentifier.value)
            .isEqualTo(oldConversion.sourceRecordIdentifier.value)
        assertThat(sink.batches.first().bundle.observations().map(::sourceIdentifierValue).distinct())
            .containsExactly(oldConversion.sourceRecordIdentifier.value)
        val migrated = requireNotNull(journal.entry("StepsRecord", "identity-upgrade"))
        assertThat(migrated.sourceRecordIdentifier.value)
            .isNotEqualTo(oldConversion.sourceRecordIdentifier.value)
        assertThat(migrated.state).isEqualTo(HealthConnectExportState.ACTIVE)
    }

    @Test
    fun `projection A to B to A baselines tombstone and reactivate an unchanged source`() = runTest {
        val journal = InMemoryJournal()
        val sink = RecordingSink()
        val record = stepRecord("projection-switch")
        val firstA = HealthConnectExportCoordinator(converter, journal, sink)
        val scopeB = HealthConnectSynchronizationScope.create(EXAMPLE_REPOSITORY_SCOPE, "filter-b")
        val projectionB = HealthConnectExportCoordinator(HealthConnectConverter(fhirContext(), scopeB), journal, sink)
        val secondA = HealthConnectExportCoordinator(converter, journal, sink)

        firstA.reconcile("StepsRecord", listOf(record), conversionTime)
        projectionB.reconcile("StepsRecord", emptyList(), conversionTime.plusSeconds(1))
        secondA.reconcile("StepsRecord", listOf(record), conversionTime.plusSeconds(2))

        assertThat(sink.batches.map { it.operation }).containsExactly(
            HealthConnectExportOperation.UPSERT,
            HealthConnectExportOperation.DELETE,
            HealthConnectExportOperation.UPSERT,
        ).inOrder()
        val entry = requireNotNull(journal.entry("StepsRecord", "projection-switch"))
        assertThat(entry.state).isEqualTo(HealthConnectExportState.ACTIVE)
        assertThat(entry.projectionScopeKey).isEqualTo(synchronizationScope.projectionScopeKey)
    }

    @Test
    fun `negative acknowledgement retains the prior journal state for retry`() = runTest {
        val journal = InMemoryJournal()
        val sink = RecordingSink()
        val coordinator = HealthConnectExportCoordinator(converter, journal, sink)
        coordinator.upsert(stepRecord("step-record"), conversionTime)
        sink.failNext = true

        val failure = runCatching {
            coordinator.delete("StepsRecord", "step-record", conversionTime.plusSeconds(1))
        }.exceptionOrNull()

        assertThat(failure).isInstanceOf(IllegalStateException::class.java)
        assertThat(requireNotNull(journal.entry("StepsRecord", "step-record")).state)
            .isEqualTo(HealthConnectExportState.ACTIVE)

        coordinator.delete("StepsRecord", "step-record", conversionTime.plusSeconds(1))
        assertThat(requireNotNull(journal.entry("StepsRecord", "step-record")).state)
            .isEqualTo(HealthConnectExportState.INVALIDATED)
    }

    @Test
    fun `incomplete acknowledgement retains the exact pending upsert`() = runTest {
        val journal = InMemoryJournal()
        val sink = RecordingSink().apply { omitNextAcknowledgement = true }
        val coordinator = HealthConnectExportCoordinator(converter, journal, sink)
        val record = stepRecord("step-record")

        val failure = runCatching { coordinator.upsert(record, conversionTime) }.exceptionOrNull()

        assertThat(failure).isInstanceOf(IllegalArgumentException::class.java)
        assertThat(journal.entry("StepsRecord", "step-record")).isNull()
        val pending = requireNotNull(journal.pending("StepsRecord", "step-record"))

        coordinator.upsert(record, conversionTime.plusSeconds(20))

        assertThat(sink.batches.last().eventSequence).isEqualTo(pending.eventSequence)
        assertThat(requireNotNull(journal.entry("StepsRecord", "step-record")).destinationReferences)
            .isNotEmpty()
    }

    @Test
    fun `journal snapshots destination references returned by a mutable sink`() = runTest {
        val journal = InMemoryJournal()
        val mutableReferences = mutableMapOf<String, String>()
        val coordinator = HealthConnectExportCoordinator(
            converter,
            journal,
            HealthConnectExportSink { batch ->
                batch.bundle.entry.mapNotNull { it.resource as? Observation }
                    .forEach { observation ->
                        val output = observationIdentity(observation)
                        mutableReferences[output.value] = "Observation/server-id"
                    }
                HealthConnectExportAcknowledgement(mutableReferences)
            },
        )

        coordinator.upsert(stepRecord("step-record"), conversionTime)
        mutableReferences.clear()

        assertThat(requireNotNull(journal.entry("StepsRecord", "step-record")).destinationReferences)
            .isNotEmpty()
    }

    @Test
    fun `export batch rejects a Bundle that differs from its authoritative JSON`() {
        val conversion = converter.convert(stepRecord("step-record"), conversionTime)
        val bundleJson = HealthConnectWireFormat.bundleJson(conversion.bundle)
        val changedBundle = conversion.bundle.apply {
            timestampElement = org.hl7.fhir.r4.model.InstantType(conversionTime.plusSeconds(1).toString())
        }

        val failure = runCatching {
            HealthConnectExportBatch(
                eventSequence = EventSequence("1"),
                operation = HealthConnectExportOperation.UPSERT,
                sourceRecordIdentifier = conversion.sourceRecordIdentifier,
                sourceVersion = conversion.sourceLastModified,
                bundle = changedBundle,
                bundleJson = bundleJson,
                payloadSha256 = HealthConnectWireFormat.sha256(bundleJson),
            )
        }.exceptionOrNull()

        assertThat(failure).isInstanceOf(IllegalArgumentException::class.java)
    }

    @Test
    fun `expired-token full read reconciles absence and tolerates deletion replay`() = runTest {
        val journal = InMemoryJournal()
        val sink = RecordingSink()
        val coordinator = HealthConnectExportCoordinator(converter, journal, sink)
        coordinator.upsert(stepRecord("retained"), conversionTime)
        coordinator.upsert(stepRecord("removed"), conversionTime)
        val constraint = HealthConnectSynchronizationConstraint(
            coordinator = coordinator,
            fullReader = HealthConnectFullReader { listOf(stepRecord("retained")) },
            now = { conversionTime.plusSeconds(30) },
        )

        constraint.onFullyResyncRequired(RecordType.steps)

        assertThat(requireNotNull(journal.entry("StepsRecord", "retained")).state)
            .isEqualTo(HealthConnectExportState.ACTIVE)
        assertThat(requireNotNull(journal.entry("StepsRecord", "removed")).state)
            .isEqualTo(HealthConnectExportState.INVALIDATED)
        val batchCount = sink.batches.size
        constraint.handleDeletedRecords(setOf("removed"), RecordType.steps)
        assertThat(sink.batches).hasSize(batchCount)
    }

    @Test
    fun `an upsert leaving the collection filter invalidates its prior output`() = runTest {
        val journal = InMemoryJournal()
        val sink = RecordingSink()
        val coordinator = HealthConnectExportCoordinator(converter, journal, sink)
        coordinator.upsert(stepRecord("filtered-record"), conversionTime)
        val constraint = HealthConnectSynchronizationConstraint(
            coordinator = coordinator,
            fullReader = HealthConnectFullReader { emptyList() },
            now = { conversionTime.plusSeconds(1) },
        )

        constraint.handleExcludedRecords(setOf("filtered-record"), RecordType.steps)

        assertThat(requireNotNull(journal.entry("StepsRecord", "filtered-record")).state)
            .isEqualTo(HealthConnectExportState.INVALIDATED)
        assertThat(sink.batches.last().operation).isEqualTo(HealthConnectExportOperation.DELETE)
    }

    @Test
    @Suppress("LongMethod")
    fun `emits deterministic complete conformance fixtures`() = runTest {
        val exportDirectory = File(requireNotNull(System.getProperty("grove.conformance.export")))
        exportDirectory.mkdirs()
        exportDirectory.listFiles()?.forEach { it.delete() }
        val wireExportDirectory = File(requireNotNull(System.getProperty("grove.wire.export")))
        wireExportDirectory.mkdirs()
        wireExportDirectory.listFiles()?.forEach { it.delete() }
        val conversions = completeConformanceRecords().map { (name, record) ->
            name to converter.convert(record, conversionTime)
        }
        val parser = JsonParser().setOutputStyle(IParser.OutputStyle.PRETTY)

        conversions.forEach { (name, conversion) ->
            File(exportDirectory, "health-connect-$name-bundle.json")
                .writeText(parser.composeString(conversion.bundle) + "\n")
            conversion.observations.forEachIndexed { index, observation ->
                File(exportDirectory, "health-connect-$name-observation-${index + 1}.json")
                    .writeText(parser.composeString(observation) + "\n")
            }
            File(exportDirectory, "health-connect-$name-provenance.json")
                .writeText(parser.composeString(requireNotNull(conversion.provenance)) + "\n")
        }

        semanticVectorRecords().forEach { vector ->
            val conversion = converter.convert(vector.record, semanticConversionTime)
            val observation = conversion.observations.single { candidate ->
                candidate.meta.profile.any { it.value == vector.profile }
            }
            File(exportDirectory, "health-connect-semantic-${vector.id}-observation.json")
                .writeText(parser.composeString(observation) + "\n")
        }

        val journal = InMemoryJournal()
        val sink = RecordingSink()
        val coordinator = HealthConnectExportCoordinator(converter, journal, sink)
        coordinator.upsert(heartRateRecord(twoHeartRateSamples()), conversionTime)
        File(wireExportDirectory, "health-connect-heart-rate-upsert-bundle.json")
            .writeText(sink.batches.last().bundleJson)
        assertThat(sink.batches.last().payloadSha256)
            .isEqualTo("f6b01db680679057b0ebc94e4f95bc25ecf5b4fa4b2a9d47e4ed0de598d0d900")
        coordinator.upsert(
            heartRateRecord(
                samples = twoHeartRateSamples().take(1),
                lastModified = Instant.parse("2026-08-19T17:30:02Z"),
            ),
            conversionTime.plusSeconds(1),
        )
        File(exportDirectory, "health-connect-heart-rate-update-bundle.json")
            .writeText(parser.composeString(sink.batches.last().bundle) + "\n")
        File(wireExportDirectory, "health-connect-heart-rate-update-bundle.json")
            .writeText(sink.batches.last().bundleJson)
        assertThat(sink.batches.last().payloadSha256)
            .isEqualTo("915f84bfac551527c4dc7fd7a083b0159facde776f7a0360637b7731be8b152c")

        coordinator.upsert(
            heartRateRecord(
                samples = emptyList(),
                lastModified = Instant.parse("2026-08-19T17:30:03Z"),
            ),
            conversionTime.plusSeconds(2),
        )
        val zeroOutputUpsert = sink.batches.last()
        File(wireExportDirectory, "health-connect-heart-rate-zero-output-upsert-bundle.json")
            .writeText(zeroOutputUpsert.bundleJson)
        assertThat(zeroOutputUpsert.operation).isEqualTo(HealthConnectExportOperation.UPSERT)
        assertThat(zeroOutputUpsert.eventSequence.value).isEqualTo("3")
        assertThat(zeroOutputUpsert.wireSourceVersion).isEqualTo("1787160603000000000")
        assertThat(zeroOutputUpsert.bundle.observations()).isNotEmpty()
        assertThat(zeroOutputUpsert.bundle.observations().map { it.status }.distinct())
            .containsExactly(Observation.ObservationStatus.ENTEREDINERROR)
        assertThat(zeroOutputUpsert.payloadSha256)
            .isEqualTo("fa1f442289c435d7dd15d28d9fa76e01861644674bbbb6ffd9a27e7f7f1ca40e")

        coordinator.upsert(stepRecord("fixture-deletion"), conversionTime)
        coordinator.delete("StepsRecord", "fixture-deletion", conversionTime.plusSeconds(2))
        File(exportDirectory, "health-connect-step-deletion-bundle.json")
            .writeText(parser.composeString(sink.batches.last().bundle) + "\n")
        File(wireExportDirectory, "health-connect-step-deletion-bundle.json")
            .writeText(sink.batches.last().bundleJson)
        assertThat(sink.batches.last().payloadSha256)
            .isEqualTo("1ed2ad9d13cd5c5f09cb098e0c6f16c26f0a012dcd6b6d1d676cc38f4dccea78")

        assertThat(
            exportDirectory.listFiles()?.map { it.name }
                ?.filter { it.startsWith("health-connect-semantic-") }
                ?.sorted()
                .orEmpty(),
        ).containsExactlyElementsIn(
            semanticVectorRecords().map { "health-connect-semantic-${it.id}-observation.json" }.sorted(),
        ).inOrder()
        assertThat(exportDirectory.listFiles()?.map { it.name }.orEmpty()).hasSize(75)
        assertThat(wireExportDirectory.listFiles()?.map { it.name }.orEmpty()).hasSize(4)
    }

    private data class SemanticVectorRecord(
        val id: String,
        val profile: String,
        val record: Record,
    )

    @Suppress("LongMethod")
    private fun semanticVectorRecords(): List<SemanticVectorRecord> {
        val offset = ZoneOffset.ofHours(-7)
        val metadata = { id: String ->
            metadata(
                Metadata.autoRecorded(watch),
                "semantic-$id",
                Instant.parse("2026-08-20T17:30:01Z"),
            )
        }
        val sleep = SleepSessionRecord(
            startTime = Instant.parse("2026-08-20T06:00:00Z"),
            startZoneOffset = offset,
            endTime = Instant.parse("2026-08-20T13:30:00Z"),
            endZoneOffset = offset,
            title = null,
            notes = null,
            stages = listOf(
                SleepSessionRecord.Stage(
                    Instant.parse("2026-08-20T07:10:00Z"),
                    Instant.parse("2026-08-20T07:42:00Z"),
                    SleepSessionRecord.STAGE_TYPE_LIGHT,
                ),
            ),
            metadata = metadata("sleep"),
        )
        return listOf(
            SemanticVectorRecord(
                "active-energy",
                HealthConnectContract.MOBILE_ACTIVE_ENERGY_PROFILE,
                ActiveCaloriesBurnedRecord(
                    Instant.parse("2026-08-20T15:00:00Z"),
                    offset,
                    Instant.parse("2026-08-20T16:00:00Z"),
                    offset,
                    Energy.kilocalories(312.5),
                    metadata("active-energy"),
                ),
            ),
            SemanticVectorRecord(
                "basal-body-temperature",
                HealthConnectContract.MOBILE_BASAL_BODY_TEMPERATURE_PROFILE,
                BasalBodyTemperatureRecord(
                    Instant.parse("2026-08-20T13:45:00Z"),
                    offset,
                    metadata("basal-body-temperature"),
                    Temperature.celsius(36.52),
                    BodyTemperatureMeasurementLocation.MEASUREMENT_LOCATION_MOUTH,
                ),
            ),
            SemanticVectorRecord(
                "blood-pressure",
                HealthConnectContract.MOBILE_BLOOD_PRESSURE_PROFILE,
                BloodPressureRecord(
                    Instant.parse("2026-08-20T15:10:00Z"),
                    offset,
                    metadata("blood-pressure"),
                    Pressure.millimetersOfMercury(118.0),
                    Pressure.millimetersOfMercury(76.0),
                    BloodPressureRecord.BODY_POSITION_SITTING_DOWN,
                    BloodPressureRecord.MEASUREMENT_LOCATION_LEFT_UPPER_ARM,
                ),
            ),
            SemanticVectorRecord(
                "body-height",
                HealthConnectContract.MOBILE_BODY_HEIGHT_PROFILE,
                HeightRecord(
                    Instant.parse("2026-08-20T15:15:00Z"),
                    offset,
                    Length.meters(1.712),
                    metadata("body-height"),
                ),
            ),
            SemanticVectorRecord(
                "body-temperature",
                HealthConnectContract.MOBILE_BODY_TEMPERATURE_PROFILE,
                BodyTemperatureRecord(
                    Instant.parse("2026-08-20T15:20:00Z"),
                    offset,
                    metadata("body-temperature"),
                    Temperature.celsius(37.1),
                    BodyTemperatureMeasurementLocation.MEASUREMENT_LOCATION_EAR,
                ),
            ),
            SemanticVectorRecord(
                "body-weight",
                HealthConnectContract.MOBILE_BODY_WEIGHT_PROFILE,
                WeightRecord(
                    Instant.parse("2026-08-20T15:25:00Z"),
                    offset,
                    Mass.kilograms(68.4),
                    metadata("body-weight"),
                ),
            ),
            SemanticVectorRecord(
                "distance",
                HealthConnectContract.MOBILE_DISTANCE_PROFILE,
                DistanceRecord(
                    Instant.parse("2026-08-20T14:00:00Z"),
                    offset,
                    Instant.parse("2026-08-20T14:30:00Z"),
                    offset,
                    Length.meters(4820.5),
                    metadata("distance"),
                ),
            ),
            SemanticVectorRecord(
                "heart-rate",
                HealthConnectContract.MOBILE_HEART_RATE_PROFILE,
                HeartRateRecord(
                    startTime = Instant.parse("2026-08-20T15:29:00Z"),
                    startZoneOffset = offset,
                    endTime = Instant.parse("2026-08-20T15:31:00Z"),
                    endZoneOffset = offset,
                    samples = listOf(
                        HeartRateRecord.Sample(Instant.parse("2026-08-20T15:30:00.251Z"), 72),
                    ),
                    metadata = metadata("heart-rate"),
                ),
            ),
            SemanticVectorRecord(
                "oxygen-saturation",
                HealthConnectContract.MOBILE_OXYGEN_SATURATION_PROFILE,
                OxygenSaturationRecord(
                    Instant.parse("2026-08-20T15:35:00Z"),
                    offset,
                    Percentage(98.0),
                    metadata("oxygen-saturation"),
                ),
            ),
            SemanticVectorRecord(
                "respiratory-rate",
                HealthConnectContract.MOBILE_RESPIRATORY_RATE_PROFILE,
                RespiratoryRateRecord(
                    Instant.parse("2026-08-20T15:40:00Z"),
                    offset,
                    15.0,
                    metadata("respiratory-rate"),
                ),
            ),
            SemanticVectorRecord(
                "sleep-duration",
                HealthConnectContract.MOBILE_SLEEP_DURATION_PROFILE,
                sleep,
            ),
            SemanticVectorRecord(
                "sleep-stage",
                HealthConnectContract.MOBILE_SLEEP_STAGE_PROFILE,
                sleep,
            ),
            SemanticVectorRecord(
                "step-count",
                HealthConnectContract.MOBILE_STEP_COUNT_PROFILE,
                StepsRecord(
                    Instant.parse("2026-08-20T15:00:00Z"),
                    offset,
                    Instant.parse("2026-08-20T16:00:00Z"),
                    offset,
                    1042,
                    metadata("step-count"),
                ),
            ),
        )
    }

    @Suppress("LongMethod")
    private fun completeConformanceRecords(): List<Pair<String, Record>> {
        val start = Instant.parse("2026-08-19T08:00:00Z")
        val instant = Instant.parse("2026-08-19T16:00:00Z")
        val end = instant.plusSeconds(3_600)
        val auto = { id: String -> metadata(Metadata.autoRecorded(watch), id) }
        val glucose = { name: String, specimen: Int ->
            name to BloodGlucoseRecord(
                time = instant,
                zoneOffset = ZoneOffset.UTC,
                metadata = auto("fixture-$name"),
                level = BloodGlucose.milligramsPerDeciliter(95.5),
                specimenSource = specimen,
                mealType = MealType.MEAL_TYPE_BREAKFAST,
                relationToMeal = BloodGlucoseRecord.RELATION_TO_MEAL_BEFORE_MEAL,
            )
        }
        val stageTypes = listOf(
            SleepSessionRecord.STAGE_TYPE_UNKNOWN,
            SleepSessionRecord.STAGE_TYPE_AWAKE,
            SleepSessionRecord.STAGE_TYPE_SLEEPING,
            SleepSessionRecord.STAGE_TYPE_OUT_OF_BED,
            SleepSessionRecord.STAGE_TYPE_LIGHT,
            SleepSessionRecord.STAGE_TYPE_DEEP,
            SleepSessionRecord.STAGE_TYPE_REM,
            SleepSessionRecord.STAGE_TYPE_AWAKE_IN_BED,
        )
        val sleepStages = stageTypes.mapIndexed { index, stage ->
            SleepSessionRecord.Stage(
                start.plusSeconds(index * 3_600L),
                start.plusSeconds((index + 1) * 3_600L),
                stage,
            )
        }
        return listOf(
            "active-energy" to ActiveCaloriesBurnedRecord(
                instant,
                ZoneOffset.ofHours(-7),
                end,
                ZoneOffset.ofHours(-7),
                Energy.kilocalories(412.5),
                auto("fixture-active-energy"),
            ),
            "basal-body-temperature" to BasalBodyTemperatureRecord(
                instant,
                ZoneOffset.UTC,
                auto("fixture-basal-body-temperature"),
                Temperature.celsius(36.4),
                BodyTemperatureMeasurementLocation.MEASUREMENT_LOCATION_MOUTH,
            ),
            glucose("glucose-whole-blood", BloodGlucoseRecord.SPECIMEN_SOURCE_WHOLE_BLOOD),
            glucose("glucose-capillary", BloodGlucoseRecord.SPECIMEN_SOURCE_CAPILLARY_BLOOD),
            glucose("glucose-plasma", BloodGlucoseRecord.SPECIMEN_SOURCE_PLASMA),
            glucose("glucose-serum", BloodGlucoseRecord.SPECIMEN_SOURCE_SERUM),
            glucose("glucose-interstitial", BloodGlucoseRecord.SPECIMEN_SOURCE_INTERSTITIAL_FLUID),
            "blood-pressure" to BloodPressureRecord(
                instant,
                ZoneOffset.UTC,
                auto("fixture-blood-pressure"),
                Pressure.millimetersOfMercury(120.0),
                Pressure.millimetersOfMercury(80.0),
                BloodPressureRecord.BODY_POSITION_SITTING_DOWN,
                BloodPressureRecord.MEASUREMENT_LOCATION_LEFT_UPPER_ARM,
            ),
            "body-temperature" to BodyTemperatureRecord(
                instant,
                ZoneOffset.UTC,
                auto("fixture-body-temperature"),
                Temperature.celsius(37.1),
                BodyTemperatureMeasurementLocation.MEASUREMENT_LOCATION_EAR,
            ),
            "distance" to DistanceRecord(
                instant,
                ZoneOffset.UTC,
                end,
                ZoneOffset.UTC,
                Length.kilometers(3.25),
                auto("fixture-distance"),
            ),
            "heart-rate" to heartRateRecord(twoHeartRateSamples()),
            "height" to HeightRecord(
                instant,
                ZoneOffset.UTC,
                Length.meters(1.82),
                auto("fixture-height"),
            ),
            "oxygen-saturation" to OxygenSaturationRecord(
                instant,
                ZoneOffset.UTC,
                Percentage(98.2),
                auto("fixture-oxygen-saturation"),
            ),
            "respiratory-rate" to RespiratoryRateRecord(
                instant,
                ZoneOffset.UTC,
                14.5,
                auto("fixture-respiratory-rate"),
            ),
            "sleep" to SleepSessionRecord(
                startTime = start,
                startZoneOffset = ZoneOffset.ofHours(-7),
                endTime = start.plusSeconds(8 * 3_600L),
                endZoneOffset = ZoneOffset.ofHours(-7),
                title = "Night sleep",
                notes = "Participant-reported note",
                stages = sleepStages,
                metadata = auto("fixture-sleep"),
            ),
            "steps" to stepRecord("fixture-step"),
            "weight" to weightRecord("fixture-weight"),
        )
    }

    private fun stepRecord(id: String, count: Long = 1042) = StepsRecord(
        startTime = Instant.parse("2026-08-19T16:00:00Z"),
        startZoneOffset = ZoneOffset.ofHours(-7),
        endTime = Instant.parse("2026-08-19T17:00:00Z"),
        endZoneOffset = ZoneOffset.ofHours(-7),
        count = count,
        metadata = metadata(Metadata.autoRecorded(watch), id),
    )

    private fun weightRecord(id: String) = WeightRecord(
        time = Instant.parse("2026-08-19T15:15:00Z"),
        zoneOffset = ZoneOffset.ofHours(-7),
        weight = Mass.kilograms(68.4),
        metadata = metadata(Metadata.manualEntry(), id),
    )

    private fun heartRateRecord(
        samples: List<HeartRateRecord.Sample>,
        lastModified: Instant = Instant.parse("2026-08-19T17:30:01Z"),
        device: Device = watch,
    ) = HeartRateRecord(
        startTime = Instant.parse("2026-08-19T17:30:00Z"),
        startZoneOffset = ZoneOffset.ofHours(-7),
        endTime = Instant.parse("2026-08-19T17:31:00Z"),
        endZoneOffset = ZoneOffset.ofHours(-7),
        samples = samples,
        metadata = metadata(Metadata.autoRecorded(device), "heart-record", lastModified),
    )

    private fun twoHeartRateSamples() = listOf(
        HeartRateRecord.Sample(Instant.parse("2026-08-19T17:30:15Z"), 72),
        HeartRateRecord.Sample(Instant.parse("2026-08-19T17:30:45Z"), 75),
    )

    private fun metadata(
        metadata: Metadata,
        id: String,
        lastModified: Instant = Instant.parse("2026-08-19T17:30:01Z"),
    ): Metadata = metadata.populatedWithTestValues(
        id = id,
        dataOrigin = DataOrigin("com.example.source"),
        lastModifiedTime = lastModified,
    )

    private class InMemoryJournal(startingSequence: Long = 1L) : HealthConnectExportJournal {
        private val values = mutableMapOf<Triple<ScopeKey, String, String>, HealthConnectExportJournalEntry>()
        private val pending = mutableMapOf<Triple<ScopeKey, String, String>, HealthConnectPendingExport>()
        private var nextSequence = startingSequence
        private val unmatchedDeletionValues = mutableMapOf<Triple<ScopeKey, String, String>, HealthConnectUnmatchedDeletion>()
        private val rejectedRecordValues = mutableMapOf<Triple<ScopeKey, String, String>, HealthConnectRejectedRecord>()
        val unmatchedDeletions: List<HealthConnectUnmatchedDeletion>
            get() = unmatchedDeletionValues.values.toList()
        val rejectedRecords: List<HealthConnectRejectedRecord>
            get() = rejectedRecordValues.values.toList()
        var failCompleteNext = false

        suspend fun entry(recordType: String, healthConnectId: String) =
            values.values.singleOrNull { it.recordType == recordType && it.healthConnectId == healthConnectId }

        suspend fun entries(recordType: String) = values.values.filter { it.recordType == recordType }

        suspend fun pending(recordType: String, healthConnectId: String) =
            pending.values.singleOrNull { it.recordType == recordType && it.healthConnectId == healthConnectId }

        override suspend fun entry(repositoryScopeKey: ScopeKey, recordType: String, healthConnectId: String) =
            values[Triple(repositoryScopeKey, recordType, healthConnectId)]

        override suspend fun entries(repositoryScopeKey: ScopeKey, recordType: String) = values.values.filter {
            it.repositoryScopeKey == repositoryScopeKey && it.recordType == recordType
        }

        override suspend fun pending(repositoryScopeKey: ScopeKey, recordType: String, healthConnectId: String) =
            pending[Triple(repositoryScopeKey, recordType, healthConnectId)]

        override suspend fun pendingForType(repositoryScopeKey: ScopeKey, recordType: String) = pending.values.filter {
            it.repositoryScopeKey == repositoryScopeKey && it.recordType == recordType
        }

        override suspend fun stage(
            repositoryScopeKey: ScopeKey,
            recordType: String,
            healthConnectId: String,
            buildDraft: (eventSequence: EventSequence) -> HealthConnectPendingExportDraft,
        ): HealthConnectPendingExport {
            val key = Triple(repositoryScopeKey, recordType, healthConnectId)
            pending[key]?.let { return it }
            val eventSequence = EventSequence(nextSequence.toString())
            val draft = buildDraft(eventSequence)
            check(draft.repositoryScopeKey == repositoryScopeKey)
            check(draft.recordType == recordType)
            check(draft.healthConnectId == healthConnectId)
            nextSequence++
            return HealthConnectPendingExport(
                eventSequence = eventSequence,
                repositoryScopeKey = draft.repositoryScopeKey,
                projectionScopeKey = draft.projectionScopeKey,
                operation = draft.operation,
                recordType = draft.recordType,
                healthConnectId = draft.healthConnectId,
                sourceRecordIdentifier = draft.sourceRecordIdentifier.copy(),
                sourceVersion = draft.sourceVersion,
                bundle = draft.bundle.copy(),
                bundleJson = draft.bundleJson,
                payloadSha256 = draft.payloadSha256,
                invalidatedOutputIdentifiers = draft.invalidatedOutputIdentifiers,
                nextEntry = draft.nextEntry,
            ).also { pending[key] = it }
        }

        override suspend fun complete(
            pending: HealthConnectPendingExport,
            entry: HealthConnectExportJournalEntry,
        ) {
            if (failCompleteNext) {
                failCompleteNext = false
                error("Journal transaction did not commit.")
            }
            val key = Triple(pending.repositoryScopeKey, pending.recordType, pending.healthConnectId)
            check(this.pending[key]?.eventSequence == pending.eventSequence)
            values[key] = entry
            this.pending.remove(key)
        }

        override suspend fun storeLocal(entry: HealthConnectExportJournalEntry) {
            val key = Triple(entry.repositoryScopeKey, entry.recordType, entry.healthConnectId)
            check(pending[key] == null)
            values[key] = entry
        }

        override suspend fun recordUnmatchedDeletion(deletion: HealthConnectUnmatchedDeletion) {
            unmatchedDeletionValues.putIfAbsent(
                Triple(deletion.repositoryScopeKey, deletion.recordType, deletion.healthConnectId),
                deletion,
            )
        }

        override suspend fun recordRejectedRecord(rejected: HealthConnectRejectedRecord) {
            rejectedRecordValues[Triple(rejected.repositoryScopeKey, rejected.recordType, rejected.healthConnectId)] = rejected
        }
    }

    private class RecordingSink : HealthConnectExportSink {
        val batches = mutableListOf<HealthConnectExportBatch>()
        private val destinationReferences = mutableMapOf<String, String>()
        private var nextDestinationId = 1
        var failNext = false
        var failOnAttempt: Int? = null
        var omitNextAcknowledgement = false
        private var attempts = 0

        override suspend fun apply(batch: HealthConnectExportBatch): HealthConnectExportAcknowledgement {
            attempts++
            if (failNext || failOnAttempt == attempts) {
                failNext = false
                failOnAttempt = null
                error("Sink did not durably apply the batch.")
            }
            batches += batch
            if (omitNextAcknowledgement) {
                omitNextAcknowledgement = false
                return HealthConnectExportAcknowledgement(emptyMap())
            }
            return HealthConnectExportAcknowledgement(
                batch.bundle.entry.mapNotNull { it.resource as? Observation }
                    .filter { it.status != Observation.ObservationStatus.ENTEREDINERROR }
                    .associate { observation ->
                        val output = observationIdentity(observation)
                        output.value to destinationReferences.getOrPut(output.value) {
                            "Observation/${nextDestinationId++}"
                        }
                    },
            )
        }

        fun observationKeys(index: Int) = batches[index].bundle.entry
            .mapNotNull { it.resource as? Observation }
            .map { observation ->
                val identifier = observationIdentity(observation)
                "${identifier.system}|${identifier.value}"
            }
    }

    private fun fhirContext(
        recordingIdentifierValue: (Device) -> String = { "study-watch" },
    ): HealthConnectConversionContext {
        val subjectIdentifier = contextIdentifier("participant-001")
        val researchStudyIdentifier = contextIdentifier("my-heart-counts")
        return HealthConnectConversionContext(
            graphIdentifierSystem = "urn:grove:health-connect-graph:org.grovealliance.example",
            subject = HealthConnectBundleResource(
                subjectIdentifier,
                Patient().apply { addIdentifier(subjectIdentifier.copy()) },
            ),
            assembler = application(
                "My Heart Counts Android FHIR Converter",
                "edu.stanford.myheartcounts.fhir",
                "1.0.0",
            ),
            researchStudies = listOf(
                HealthConnectBundleResource(
                    researchStudyIdentifier,
                    ResearchStudy().apply {
                        addIdentifier(researchStudyIdentifier.copy())
                        status = ResearchStudy.ResearchStudyStatus.ACTIVE
                    },
                ),
            ),
            sourceApplication = { packageName ->
                val sourceIdentifier = identifier(HealthConnectContract.ANDROID_PACKAGE_IDENTIFIER, packageName)
                HealthConnectBundleResource(
                    sourceIdentifier,
                    FhirDevice().apply {
                        addIdentifier(sourceIdentifier.copy())
                    },
                )
            },
            recordingDevice = { source ->
                val recordingIdentifier = contextIdentifier(recordingIdentifierValue(source))
                HealthConnectRecordingDeviceResource(
                    bundleResource = HealthConnectBundleResource(
                        recordingIdentifier,
                        FhirDevice().apply {
                            meta.addProfile(HealthConnectContract.MOBILE_RECORDING_DEVICE_PROFILE)
                            addIdentifier(recordingIdentifier.copy())
                            manufacturer = source.manufacturer
                            modelNumber = source.model
                        },
                    ),
                    identityAdmission = HealthConnectRecordingDeviceIdentityAdmission.DEPLOYMENT_SCOPED,
                )
            },
        )
    }

    private fun application(
        name: String,
        packageName: String,
        version: String? = null,
    ): HealthConnectBundleResource<FhirDevice> {
        val entryIdentifier = identifier(HealthConnectContract.ANDROID_PACKAGE_IDENTIFIER, packageName)
        return HealthConnectBundleResource(
            entryIdentifier,
            FhirDevice().apply {
                meta.addProfile(HealthConnectContract.MOBILE_APPLICATION_DEVICE_PROFILE)
                addIdentifier(entryIdentifier.copy())
                addDeviceName().setName(name).setType(FhirDevice.DeviceNameType.USERFRIENDLYNAME)
                version?.let {
                    addVersion()
                        .setType(
                            CodeableConcept(
                                Coding(
                                    HealthConnectContract.MDC,
                                    HealthConnectContract.APPLICATION_SOFTWARE_VERSION,
                                    "MDC_ID_PROD_SPEC_SW",
                                ),
                            ),
                        )
                        .setValue(it)
                }
            },
        )
    }

    private fun identifier(system: String, value: String): Identifier =
        Identifier().setSystem(system).setValue(value)

    private fun contextIdentifier(value: String): Identifier = identifier(TEST_CONTEXT_IDENTIFIER_SYSTEM, value)

    private fun contextFullUrl(value: String): String = GroveExchangeIdentity.fullUrl(contextIdentifier(value))

    private fun Bundle.observations(): List<Observation> = entry.mapNotNull { it.resource as? Observation }

    private fun sourceIdentifierValue(observation: Observation): String = observation.identifier.single {
        it.system == HealthConnectContract.HEALTH_CONNECT_RECORD_IDENTIFIER
    }.value

    private fun HealthConnectConverter.convert(record: androidx.health.connect.client.records.Record, at: Instant) =
        convert(record, at, EventSequence("1"))

    private companion object {
        const val EXAMPLE_REPOSITORY_SCOPE = "1f5c58aa-6ec6-4e79-a682-829a9debd3f5"
        const val TEST_CONTEXT_IDENTIFIER_SYSTEM = "urn:uuid:8d3fd52b-efda-5f3d-b83d-50f0a70b44aa"
    }
}
