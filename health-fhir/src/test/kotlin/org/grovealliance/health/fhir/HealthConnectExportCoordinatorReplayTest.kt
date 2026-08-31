//
// This source file is part of the My Heart Counts Android open-source project
//
// SPDX-FileCopyrightText: 2026 Stanford University and the project authors (see CONTRIBUTORS.md)
//
// SPDX-License-Identifier: MIT

package org.grovealliance.health.fhir

import androidx.health.connect.client.records.StepsRecord
import androidx.health.connect.client.records.WeightRecord
import androidx.health.connect.client.records.metadata.Metadata
import androidx.health.connect.client.units.Mass
import com.google.common.truth.Truth.assertThat
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import org.hl7.fhir.r4.model.InstantType
import org.hl7.fhir.r4.model.Observation
import org.junit.Test
import java.time.Instant
import java.time.ZoneOffset

class HealthConnectExportCoordinatorReplayTest : HealthConnectExportCoordinatorTestSupport() {
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

        coordinator.reconcile("HeartRateRecord", { conversionTime }) { listOf(newer) }
        coordinator.upsert(changedWithOlderTimestamp, conversionTime.plusSeconds(1))

        assertThat(sink.batches).hasSize(3)
        assertThat(sink.batches.map { it.operation }).containsExactly(
            HealthConnectExportOperation.ACTIVE,
            HealthConnectExportOperation.RETRACTION,
            HealthConnectExportOperation.ACTIVE,
        ).inOrder()
        assertThat(sink.batches[2].eventSequence).isGreaterThan(sink.batches[0].eventSequence)
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

        assertThat(sink.batches).hasSize(3)
        assertThat(sink.batches.map { it.wireSourceVersion }.distinct()).hasSize(1)
        assertThat(sink.batches.map { it.operation }).containsExactly(
            HealthConnectExportOperation.ACTIVE,
            HealthConnectExportOperation.RETRACTION,
            HealthConnectExportOperation.ACTIVE,
        ).inOrder()
        assertThat(sink.batches[2].eventSequence).isGreaterThan(sink.batches[0].eventSequence)
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
        assertThat(sink.batches[1].wireOperation).isEqualTo("active")
        assertThat(requireNotNull(journal.entry("StepsRecord", "step-record")).destinationReferences)
            .isNotEmpty()
        assertThat(requireNotNull(journal.entry("StepsRecord", "step-record")).lastEventSequence)
            .isEqualTo(sink.batches.last().eventSequence)
    }

    @Test
    fun `a second coordinator resumes the exact event after stale-fence loss before replacement`() = runTest {
        val journal = InMemoryJournal().apply { loseSourceLeaseBeforeNextComplete = true }
        val sink = RecordingSink()
        val firstCoordinator = HealthConnectExportCoordinator(converter, journal, sink)
        val secondCoordinator = HealthConnectExportCoordinator(converter, journal, sink)
        val initial = stepRecord("fenced-retry", count = 1042)
        val replacement = stepRecord("fenced-retry", count = 2042)

        val failure = runCatching { firstCoordinator.upsert(initial, conversionTime) }.exceptionOrNull()
        val staged = requireNotNull(journal.pending("StepsRecord", "fenced-retry"))
        secondCoordinator.upsert(replacement, conversionTime.plusSeconds(1))

        assertThat(failure).isInstanceOf(IllegalStateException::class.java)
        assertThat(sink.batches.map { it.eventSequence.value }).containsExactly("1", "1", "2", "3").inOrder()
        assertThat(sink.batches[1].bundleJson).isEqualTo(staged.bundleJson)
        assertThat(sink.batches[1].payloadSha256).isEqualTo(staged.payloadSha256)
        assertThat(sink.batches.map { it.operation }).containsExactly(
            HealthConnectExportOperation.ACTIVE,
            HealthConnectExportOperation.ACTIVE,
            HealthConnectExportOperation.RETRACTION,
            HealthConnectExportOperation.ACTIVE,
        ).inOrder()
        assertThat(journal.pending("StepsRecord", "fenced-retry")).isNull()
        assertThat(requireNotNull(journal.entry("StepsRecord", "fenced-retry")).lastEventSequence?.value)
            .isEqualTo("3")
    }

    @Test
    fun `two coordinators serialize a same-source race into one ordered lifecycle`() = runTest {
        val journal = InMemoryJournal()
        val sink = RecordingSink()
        val firstCoordinator = HealthConnectExportCoordinator(converter, journal, sink)
        val secondCoordinator = HealthConnectExportCoordinator(converter, journal, sink)
        val first = stepRecord("same-source-race", count = 1042)
        val second = StepsRecord(
            startTime = Instant.parse("2026-08-19T17:30:00Z"),
            startZoneOffset = ZoneOffset.ofHours(-7),
            endTime = Instant.parse("2026-08-19T17:31:00Z"),
            endZoneOffset = ZoneOffset.ofHours(-7),
            count = 2042,
            metadata = metadata(
                Metadata.autoRecorded(watch),
                "same-source-race",
                Instant.parse("2026-08-19T17:30:02Z"),
            ),
        )

        coroutineScope {
            listOf(
                async { firstCoordinator.upsert(first, conversionTime) },
                async { secondCoordinator.upsert(second, conversionTime.plusSeconds(1)) },
            ).awaitAll()
        }

        assertThat(sink.batches.map { it.eventSequence.value }).containsExactly("1", "2", "3").inOrder()
        assertThat(sink.batches.map { it.operation }).containsExactly(
            HealthConnectExportOperation.ACTIVE,
            HealthConnectExportOperation.RETRACTION,
            HealthConnectExportOperation.ACTIVE,
        ).inOrder()
        val finalEntry = requireNotNull(journal.entry("StepsRecord", "same-source-race"))
        assertThat(finalEntry.sourceLastModified).isEqualTo(sink.batches.last().sourceVersion)
        assertThat(finalEntry.lastEventSequence?.value).isEqualTo("3")
        assertThat(journal.pending("StepsRecord", "same-source-race")).isNull()
    }

    @Test
    fun `reconciliation lease fences an ordinary upsert until the absence snapshot completes`() = runTest {
        val journal = InMemoryJournal()
        val sink = RecordingSink()
        val reconciliationCoordinator = HealthConnectExportCoordinator(converter, journal, sink)
        val changesCoordinator = HealthConnectExportCoordinator(converter, journal, sink)
        reconciliationCoordinator.upsert(stepRecord("absent-during-reconcile"), conversionTime)
        val enteredSink = CompletableDeferred<Unit>()
        val releaseSink = CompletableDeferred<Unit>()
        sink.pauseBeforeNextApply = enteredSink to releaseSink

        val reconciliation = async {
            reconciliationCoordinator.reconcile("StepsRecord", { conversionTime.plusSeconds(1) }) { emptyList() }
        }
        enteredSink.await()
        val concurrentUpsert = async {
            changesCoordinator.upsert(stepRecord("arrived-during-reconcile"), conversionTime.plusSeconds(2))
        }
        runCurrent()

        assertThat(concurrentUpsert.isCompleted).isFalse()
        releaseSink.complete(Unit)
        reconciliation.await()
        concurrentUpsert.await()

        assertThat(sink.batches.map { it.eventSequence.value }).containsExactly("1", "2", "3").inOrder()
        assertThat(requireNotNull(journal.entry("StepsRecord", "absent-during-reconcile")).state)
            .isEqualTo(HealthConnectExportState.INVALIDATED)
        assertThat(requireNotNull(journal.entry("StepsRecord", "arrived-during-reconcile")).state)
            .isEqualTo(HealthConnectExportState.ACTIVE)
    }

    @Test
    fun `complete read executes inside the reconciliation fence`() = runTest {
        val journal = InMemoryJournal()
        val sink = RecordingSink()
        val reconciliationCoordinator = HealthConnectExportCoordinator(converter, journal, sink)
        val changesCoordinator = HealthConnectExportCoordinator(converter, journal, sink)
        val enteredRead = CompletableDeferred<Unit>()
        val releaseRead = CompletableDeferred<Unit>()

        val reconciliation = async {
            reconciliationCoordinator.reconcile("StepsRecord", { conversionTime }) {
                enteredRead.complete(Unit)
                releaseRead.await()
                emptyList()
            }
        }
        enteredRead.await()
        val concurrentUpsert = async {
            changesCoordinator.upsert(stepRecord("arrived-during-read"), conversionTime.plusSeconds(1))
        }
        runCurrent()

        assertThat(concurrentUpsert.isCompleted).isFalse()
        releaseRead.complete(Unit)
        reconciliation.await()
        concurrentUpsert.await()

        assertThat(sink.batches.map { it.eventSequence.value }).containsExactly("1")
        assertThat(requireNotNull(journal.entry("StepsRecord", "arrived-during-read")).state)
            .isEqualTo(HealthConnectExportState.ACTIVE)
    }

    @Test
    fun `pending and journal getters cannot mutate the durable exact-event snapshot`() = runTest {
        val journal = InMemoryJournal().apply { failCompleteNext = true }
        val sink = RecordingSink()
        val coordinator = HealthConnectExportCoordinator(converter, journal, sink)
        runCatching { coordinator.upsert(stepRecord("immutable-pending"), conversionTime) }
        val pending = requireNotNull(journal.pending("StepsRecord", "immutable-pending"))
        val exactJson = pending.bundleJson

        pending.bundle.timestampElement = InstantType(conversionTime.plusSeconds(90).toString())
        pending.sourceRecordIdentifier.value = "mutated-source"
        pending.nextEntry.bundle.id = "mutated-next-entry"
        coordinator.upsert(stepRecord("immutable-pending"), conversionTime.plusSeconds(1))

        assertThat(sink.batches.last().bundleJson).isEqualTo(exactJson)
        assertThat(requireNotNull(journal.entry("StepsRecord", "immutable-pending")).bundle.id)
            .isNotEqualTo("mutated-next-entry")
    }

    @Test
    fun `expired baseline drains an orphaned first upsert then retracts its absent source`() = runTest {
        val journal = InMemoryJournal().apply { failCompleteNext = true }
        val sink = RecordingSink()
        val coordinator = HealthConnectExportCoordinator(converter, journal, sink)

        runCatching { coordinator.upsert(stepRecord("removed-before-recovery"), conversionTime) }
        coordinator.reconcile("StepsRecord", { conversionTime.plusSeconds(30) }) { emptyList() }

        assertThat(sink.batches).hasSize(3)
        assertThat(sink.batches[1].eventSequence).isEqualTo(sink.batches[0].eventSequence)
        assertThat(sink.batches[1].bundleJson).isEqualTo(sink.batches[0].bundleJson)
        assertThat(sink.batches[2].operation).isEqualTo(HealthConnectExportOperation.RETRACTION)
        assertThat(sink.batches[2].bundle.observations()).isEmpty()
        assertThat(sink.batches[2].bundle.entry.map { it.resource.fhirType() })
            .containsExactly("Provenance")
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
            HealthConnectPendingExport(
                eventSequence = pending.eventSequence,
                baseRevision = pending.baseRevision,
                repositoryScopeKey = pending.repositoryScopeKey,
                projectionScopeKey = pending.projectionScopeKey,
                operation = pending.operation,
                recordType = pending.recordType,
                healthConnectId = pending.healthConnectId,
                sourceRecordIdentifier = pending.sourceRecordIdentifier,
                sourceVersion = pending.sourceVersion,
                bundle = pending.bundle.apply { id = "different-bundle" },
                bundleJson = pending.bundleJson,
                payloadSha256 = pending.payloadSha256,
                retractedTargets = pending.retractedTargets,
                nextEntry = pending.nextEntry,
            )
        }.exceptionOrNull()

        assertThat(failure).isInstanceOf(IllegalArgumentException::class.java)
    }

    @Test
    fun `the export boundary rejects a missing selected output identity`() {
        val record = stepRecord("corrupt-journal-entry")
        val conversion = converter.convert(record, conversionTime, EventSequence("1"))
        val corruptedBundle = conversion.bundle.copy().apply {
            entry.single { it.resource is Observation }.extension.removeAll {
                it.url == GroveExchangeIdentity.ENTRY_IDENTIFIER_EXTENSION
            }
        }
        val bundleJson = HealthConnectWireFormat.bundleJson(corruptedBundle)

        val failure = runCatching {
            HealthConnectExportBatch(
                eventSequence = EventSequence("1"),
                operation = HealthConnectExportOperation.ACTIVE,
                sourceRecordIdentifier = conversion.sourceRecordIdentifier,
                sourceVersion = conversion.sourceLastModified,
                bundle = corruptedBundle,
                bundleJson = bundleJson,
                payloadSha256 = HealthConnectWireFormat.sha256(bundleJson),
            )
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
}
