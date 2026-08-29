//
// This source file belongs to the My Heart Counts Android project
//
// SPDX-FileCopyrightText: 2026 Stanford University and the project authors (see CONTRIBUTORS.md)
//
// SPDX-License-Identifier: MIT

package org.grovealliance.health.fhir

import androidx.health.connect.client.records.BloodGlucoseRecord
import androidx.health.connect.client.records.MealType
import androidx.health.connect.client.records.metadata.Device
import androidx.health.connect.client.records.metadata.Metadata
import androidx.health.connect.client.units.BloodGlucose
import com.google.common.truth.Truth.assertThat
import kotlinx.coroutines.test.runTest
import org.hl7.fhir.r4.model.Provenance
import org.junit.Test
import java.time.Instant
import java.time.ZoneOffset

class HealthConnectExportCoordinatorRetractionTest : HealthConnectExportCoordinatorTestSupport() {
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

        val retraction = sink.batches[sink.batches.lastIndex - 1]
        val update = sink.batches.last()
        assertThat(retraction.retractedTargets).hasSize(4)
        assertThat(retraction.retractedTargets.count {
            it.role == HealthConnectRetractionTargetRole.DEVICE_SNAPSHOT
        }).isEqualTo(2)
        assertThat(retraction.bundle.observations()).isEmpty()
        assertThat(update.retractedTargets).isEmpty()
        val current = requireNotNull(journal.entry("HeartRateRecord", "heart-record"))
        assertThat(current.observations).hasSize(1)
        assertThat(current.state).isEqualTo(HealthConnectExportState.ACTIVE)
    }

    @Test
    fun `heart-rate update retracts prior outputs with a current assembler snapshot`() = runTest {
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

        val retraction = sink.batches[sink.batches.lastIndex - 1].bundle
        val update = sink.batches.last().bundle
        assertThat(retraction.entry.map { it.resource.fhirType() }).containsExactly("Provenance")
        val retractionProvenance = retraction.entry.single().resource as Provenance
        assertThat(retractionProvenance.agent.single().who.identifier.value)
            .isEqualTo(
                HealthConnectIdentity.deviceSnapshot(
                    testIdentityKey(),
                    HealthConnectIdentity.exchange(TEST_EVENT_SYSTEM, TEST_PRODUCER_INSTANCE, EventSequence("2")),
                    "application",
                    "edu.stanford.myheartcounts.fhir",
                ).value,
            )
        assertThat(
            retractionProvenance.agent.single().who.identifier
                .hasGroveRole(GroveIdentifierRole.DEVICE_SNAPSHOT),
        ).isTrue()
        val newRecorderSnapshot = HealthConnectIdentity.deviceSnapshot(
            testIdentityKey(),
            HealthConnectIdentity.exchange(TEST_EVENT_SYSTEM, TEST_PRODUCER_INSTANCE, EventSequence("3")),
            "recording-device",
            "new-study-watch",
        )
        assertThat(update.entry.map { it.fullUrl })
            .contains(GroveExchangeIdentity.fullUrl(newRecorderSnapshot))
        assertThat(retraction.observations()).isEmpty()
    }

    @Test
    fun `deletion is acknowledged before a durable invalidation marker and replay is safe`() = runTest {
        val journal = InMemoryJournal()
        val sink = RecordingSink()
        val coordinator = HealthConnectExportCoordinator(converter, journal, sink)
        coordinator.upsert(stepRecord("step-record"), conversionTime)

        coordinator.delete("StepsRecord", "step-record", conversionTime.plusSeconds(1))

        val deletion = sink.batches.last()
        assertThat(deletion.retractedTargets).hasSize(3)
        assertThat(deletion.retractedTargets.count {
            it.role == HealthConnectRetractionTargetRole.DEVICE_SNAPSHOT
        }).isEqualTo(2)
        assertThat(deletion.operation).isEqualTo(HealthConnectExportOperation.RETRACTION)
        assertThat(deletion.wireOperation).isEqualTo("retraction")
        assertThat(deletion.sourceVersion).isEqualTo(Instant.parse("2026-08-19T17:30:01Z"))
        assertThat(deletion.bundle.observations()).isEmpty()
        val lifecycle = deletion.bundle.entry.mapNotNull { it.resource as? Provenance }.single()
        assertThat(lifecycle.meta.profile.map { it.value })
            .containsExactly(HealthConnectContract.MOBILE_RETRACTION_PROVENANCE_PROFILE)
        assertThat(lifecycle.activity.coding.single().code).isEqualTo("source-record-retracted")
        assertThat(requireNotNull(journal.entry("StepsRecord", "step-record")).state)
            .isEqualTo(HealthConnectExportState.INVALIDATED)

        val acknowledgedBatchCount = sink.batches.size
        coordinator.delete("StepsRecord", "step-record", conversionTime.plusSeconds(2))
        assertThat(sink.batches).hasSize(acknowledgedBatchCount)

        coordinator.upsert(stepRecord("step-record"), conversionTime.plusSeconds(3))
        assertThat(sink.batches).hasSize(acknowledgedBatchCount + 1)
        assertThat(sink.batches.last().operation).isEqualTo(HealthConnectExportOperation.ACTIVE)
        assertThat(requireNotNull(journal.entry("StepsRecord", "step-record")).state)
            .isEqualTo(HealthConnectExportState.ACTIVE)
    }

    @Test
    fun `glucose deletion retracts its specimen output with an exact typed target`() = runTest {
        val journal = InMemoryJournal()
        val sink = RecordingSink()
        val coordinator = HealthConnectExportCoordinator(converter, journal, sink)
        val record = BloodGlucoseRecord(
            time = Instant.parse("2026-08-19T16:00:00Z"),
            zoneOffset = ZoneOffset.UTC,
            metadata = metadata(Metadata.autoRecorded(watch), "glucose-to-delete"),
            level = BloodGlucose.milligramsPerDeciliter(95.5),
            specimenSource = BloodGlucoseRecord.SPECIMEN_SOURCE_CAPILLARY_BLOOD,
            mealType = MealType.MEAL_TYPE_UNKNOWN,
            relationToMeal = BloodGlucoseRecord.RELATION_TO_MEAL_UNKNOWN,
        )
        coordinator.upsert(record, conversionTime)
        assertThat(
            requireNotNull(journal.entry("BloodGlucoseRecord", "glucose-to-delete"))
                .destinationReferences,
        ).hasSize(2)

        coordinator.delete("BloodGlucoseRecord", "glucose-to-delete", conversionTime.plusSeconds(1))

        val retraction = sink.batches.last()
        val specimenTarget = retraction.retractedTargets.single {
            it.role == HealthConnectRetractionTargetRole.SPECIMEN
        }
        assertThat(specimenTarget.resourceType).isEqualTo("Specimen")
        assertThat(specimenTarget.identifierRole).isEqualTo(GroveIdentifierRole.SOURCE_OUTPUT)
        val provenance = retraction.bundle.entry.mapNotNull { it.resource as? Provenance }.single()
        val provenanceTarget = provenance.target.single {
            it.getExtensionByUrl(HealthConnectContract.GROVE_RETRACTION_TARGET_ROLE)
                .value.primitiveValue() == HealthConnectRetractionTargetRole.SPECIMEN.code
        }
        assertThat(provenanceTarget.type).isEqualTo("Specimen")
        assertThat(provenanceTarget.identifier.system).isEqualTo(specimenTarget.identifier.system)
        assertThat(provenanceTarget.identifier.value).isEqualTo(specimenTarget.identifier.value)
        assertThat(provenanceTarget.identifier.hasGroveRole(GroveIdentifierRole.SOURCE_OUTPUT)).isTrue()
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
    fun `zero-output update emits a separate retraction and retains local zero-output state`() = runTest {
        val journal = InMemoryJournal()
        val sink = RecordingSink()
        val coordinator = HealthConnectExportCoordinator(converter, journal, sink)
        coordinator.upsert(heartRateRecord(twoHeartRateSamples()), conversionTime)

        coordinator.upsert(heartRateRecord(emptyList()), conversionTime.plusSeconds(1))

        assertThat(sink.batches).hasSize(2)
        assertThat(sink.batches.last().operation).isEqualTo(HealthConnectExportOperation.RETRACTION)
        assertThat(sink.batches.last().bundle.observations()).isEmpty()
        assertThat(sink.batches.last().bundle.entry.map { it.resource.fhirType() })
            .containsExactly("Provenance")
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
    fun `a new projection baseline can reactivate an equal-version invalidation marker`() = runTest {
        val journal = InMemoryJournal()
        val sink = RecordingSink()
        val firstCoordinator = HealthConnectExportCoordinator(converter, journal, sink)
        val record = stepRecord("reactivated")
        firstCoordinator.upsert(record, conversionTime)
        firstCoordinator.delete("StepsRecord", "reactivated", conversionTime.plusSeconds(1))
        val nextScope = testSynchronizationScope(
            repositoryScope = EXAMPLE_REPOSITORY_SCOPE,
            configurationFingerprint = "expanded-filter-revision",
        )
        val nextCoordinator = HealthConnectExportCoordinator(
            HealthConnectConverter(fhirContext(), nextScope),
            journal,
            sink,
        )

        nextCoordinator.reconcile("StepsRecord", { conversionTime.plusSeconds(2) }) { listOf(record) }

        assertThat(sink.batches.last().operation).isEqualTo(HealthConnectExportOperation.ACTIVE)
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
        initialCoordinator.reconcile("StepsRecord", { conversionTime }) { listOf(record) }
        val upgradedScope = testSynchronizationScope(
            repositoryScope = EXAMPLE_REPOSITORY_SCOPE,
            configurationFingerprint = "all-supported-records-v1",
            conversionContractVersion = "health-connect-r4-revision-test",
        )
        val upgradedCoordinator = HealthConnectExportCoordinator(
            HealthConnectConverter(fhirContext(), upgradedScope),
            journal,
            sink,
        )

        upgradedCoordinator.reconcile("StepsRecord", { conversionTime.plusSeconds(1) }) { listOf(record) }

        assertThat(upgradedScope.repositoryScopeKey).isEqualTo(synchronizationScope.repositoryScopeKey)
        assertThat(upgradedScope.projectionScopeKey).isNotEqualTo(synchronizationScope.projectionScopeKey)
        assertThat(sink.batches).hasSize(3)
        assertThat(sink.batches.map { it.eventSequence.value }).containsExactly("1", "2", "3").inOrder()
        assertThat(sink.batches.map { it.operation }).containsExactly(
            HealthConnectExportOperation.ACTIVE,
            HealthConnectExportOperation.RETRACTION,
            HealthConnectExportOperation.ACTIVE,
        ).inOrder()
        val migrated = requireNotNull(journal.entry("StepsRecord", "contract-upgrade"))
        assertThat(migrated.projectionScopeKey).isEqualTo(upgradedScope.projectionScopeKey)
        assertThat(migrated.conversionContractVersion).isEqualTo("health-connect-r4-revision-test")
    }

    @Test
    @Suppress("LongMethod")
    fun `contract migration retires an old source identity before publishing the new identity`() = runTest {
        val record = stepRecord("identity-upgrade")
        val oldScope = testSynchronizationScope(
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
                    destinationReferences = oldConversion.outputIdentifiers.associate {
                        it.key() to "Resource/old-${it.value}"
                    },
                    lastEventSequence = EventSequence("99"),
                ),
            )
        }
        val sink = RecordingSink()
        val upgradedScope = testSynchronizationScope(
            repositoryScope = EXAMPLE_REPOSITORY_SCOPE,
            configurationFingerprint = "all-supported-records-v1",
            conversionContractVersion = "health-connect-r4-revision-test",
        )
        val coordinator = HealthConnectExportCoordinator(
            HealthConnectConverter(fhirContext(), upgradedScope),
            journal,
            sink,
        )

        coordinator.reconcile("StepsRecord", { conversionTime.plusSeconds(1) }) { listOf(record) }

        assertThat(sink.batches.map { it.operation }).containsExactly(
            HealthConnectExportOperation.RETRACTION,
            HealthConnectExportOperation.ACTIVE,
        ).inOrder()
        assertThat(sink.batches.map { it.eventSequence.value }).containsExactly("100", "101").inOrder()
        assertThat(sink.batches.first().sourceRecordIdentifier.value)
            .isEqualTo(oldConversion.sourceRecordIdentifier.value)
        assertThat(sink.batches.first().bundle.observations()).isEmpty()
        assertThat(
            (sink.batches.first().bundle.entry.single().resource as Provenance)
                .entity.single().what.identifier.value,
        ).isEqualTo(oldConversion.sourceRecordIdentifier.value)
        val migrated = requireNotNull(journal.entry("StepsRecord", "identity-upgrade"))
        assertThat(migrated.sourceRecordIdentifier.value)
            .isNotEqualTo(oldConversion.sourceRecordIdentifier.value)
        assertThat(migrated.state).isEqualTo(HealthConnectExportState.ACTIVE)
    }

    @Test
    fun `projection A to B to A retracts and reactivates an unchanged source`() = runTest {
        val journal = InMemoryJournal()
        val sink = RecordingSink()
        val record = stepRecord("projection-switch")
        val firstA = HealthConnectExportCoordinator(converter, journal, sink)
        val scopeB = testSynchronizationScope(EXAMPLE_REPOSITORY_SCOPE, "filter-b")
        val projectionB = HealthConnectExportCoordinator(HealthConnectConverter(fhirContext(), scopeB), journal, sink)
        val secondA = HealthConnectExportCoordinator(converter, journal, sink)

        firstA.reconcile("StepsRecord", { conversionTime }) { listOf(record) }
        projectionB.reconcile("StepsRecord", { conversionTime.plusSeconds(1) }) { emptyList() }
        secondA.reconcile("StepsRecord", { conversionTime.plusSeconds(2) }) { listOf(record) }

        assertThat(sink.batches.map { it.operation }).containsExactly(
            HealthConnectExportOperation.ACTIVE,
            HealthConnectExportOperation.RETRACTION,
            HealthConnectExportOperation.ACTIVE,
        ).inOrder()
        val entry = requireNotNull(journal.entry("StepsRecord", "projection-switch"))
        assertThat(entry.state).isEqualTo(HealthConnectExportState.ACTIVE)
        assertThat(entry.projectionScopeKey).isEqualTo(synchronizationScope.projectionScopeKey)
    }
}
