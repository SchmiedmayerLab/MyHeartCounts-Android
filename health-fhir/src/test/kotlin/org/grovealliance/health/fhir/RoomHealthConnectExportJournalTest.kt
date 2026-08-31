//
// This source file is part of the My Heart Counts Android open-source project
//
// SPDX-FileCopyrightText: 2026 Stanford University and the project authors (see CONTRIBUTORS.md)
//
// SPDX-License-Identifier: MIT

package org.grovealliance.health.fhir

import android.content.Context
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.withTransaction
import androidx.test.core.app.ApplicationProvider
import com.google.common.truth.Truth.assertThat
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.delay
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.supervisorScope
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import java.time.Duration
import java.util.UUID
import java.util.concurrent.ConcurrentLinkedQueue
import java.util.concurrent.atomic.AtomicLong

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [35])
class RoomHealthConnectExportJournalTest : HealthConnectExportCoordinatorTestSupport() {
    private val context = ApplicationProvider.getApplicationContext<Context>()
    private val databaseNames = mutableListOf<String>()

    @After
    fun deleteDatabases() {
        databaseNames.forEach(context::deleteDatabase)
    }

    @Test
    fun `exact pending payload and sequence survive database reopen`() = runTest {
        val databaseName = databaseName()
        val firstJournal = open(databaseName)
        val firstSink = CaptureThenFailSink()
        val firstCoordinator = HealthConnectExportCoordinator(converter, firstJournal, firstSink)

        val failure = runCatching {
            firstCoordinator.upsert(stepRecord("room-replay"), conversionTime)
        }.exceptionOrNull()
        val failedBatch = requireNotNull(firstSink.batch)
        firstJournal.close()

        assertThat(failure).isInstanceOf(IllegalStateException::class.java)

        val reopened = open(databaseName)
        val retrySink = RecordingSink()
        val retryCoordinator = HealthConnectExportCoordinator(converter, reopened, retrySink)
        retryCoordinator.upsert(stepRecord("room-replay"), conversionTime.plusSeconds(30))
        retryCoordinator.upsert(stepRecord("room-next"), conversionTime.plusSeconds(31))

        assertThat(retrySink.batches.map { it.eventSequence.value }).containsExactly("1", "2").inOrder()
        assertThat(retrySink.batches.first().bundleJson).isEqualTo(failedBatch.bundleJson)
        assertThat(retrySink.batches.first().payloadSha256).isEqualTo(failedBatch.payloadSha256)
        assertThat(retrySink.batches.first().bundle.equalsDeep(failedBatch.bundle)).isTrue()
        reopened.withSourceTransition(
            synchronizationScope.repositoryScopeKey,
            "StepsRecord",
            "room-replay",
        ) { lease ->
            assertThat(reopened.pending(lease)).isNull()
            assertThat(requireNotNull(reopened.entry(lease)).lastEventSequence?.value).isEqualTo("1")
        }
        reopened.close()
    }

    @Test
    fun `base revision compare and swap rejects stale local transition`() = runTest {
        val journal = open(databaseName())
        val coordinator = HealthConnectExportCoordinator(converter, journal, RecordingSink())
        coordinator.upsert(heartRateRecord(emptyList()), conversionTime)

        journal.withSourceTransition(
            synchronizationScope.repositoryScopeKey,
            "HeartRateRecord",
            "heart-record",
        ) { lease ->
            val stored = requireNotNull(journal.entry(lease))
            val failure = runCatching { journal.storeLocal(lease, null, stored) }.exceptionOrNull()

            assertThat(failure).isInstanceOf(IllegalStateException::class.java)
            journal.storeLocal(lease, stored.revision, stored)
            assertThat(requireNotNull(journal.entry(lease)).revision).isEqualTo(stored.revision)
        }
        journal.close()
    }

    @Test
    fun `local transition cannot bypass delivery for active outputs`() = runTest {
        val journal = open(databaseName())
        val record = stepRecord("local-output-bypass")
        val conversion = converter.convert(record, conversionTime, EventSequence("1"))
        val entry = HealthConnectActiveExportBuilder(synchronizationScope).entry(record, conversion, null)

        journal.withSourceTransition(
            synchronizationScope.repositoryScopeKey,
            "StepsRecord",
            "local-output-bypass",
        ) { lease ->
            val failure = runCatching { journal.storeLocal(lease, null, entry) }.exceptionOrNull()

            assertThat(failure).isInstanceOf(IllegalStateException::class.java)
            assertThat(journal.entry(lease)).isNull()
        }
        journal.close()
    }

    @Test
    fun `two database instances serialize one source with increasing fences`() = runBlocking {
        val databaseName = databaseName()
        // A frozen clock leaves the lock protocol, not wall-clock expiry, deciding who blocks.
        val clock = AtomicLong(PROCESS_DEATH_EPOCH_MILLIS)
        val firstJournal = openFenced(databaseName, clock)
        val secondJournal = openFenced(databaseName, clock)
        val firstEntered = CompletableDeferred<HealthConnectJournalFence>()
        val releaseFirst = CompletableDeferred<Unit>()

        supervisorScope {
            val first = async(Dispatchers.Default) {
                firstJournal.withSourceTransition(
                    synchronizationScope.repositoryScopeKey,
                    "StepsRecord",
                    "fenced-source",
                ) { lease ->
                    firstEntered.complete(lease.fence)
                    releaseFirst.await()
                }
            }
            val firstFence = firstEntered.await()
            val secondEntered = CompletableDeferred<HealthConnectJournalFence>()
            val second = async(Dispatchers.Default) {
                secondJournal.withSourceTransition(
                    synchronizationScope.repositoryScopeKey,
                    "StepsRecord",
                    "fenced-source",
                ) { lease -> secondEntered.complete(lease.fence) }
            }
            delay(BLOCKED_ASSERTION_MILLIS)
            assertThat(secondEntered.isCompleted).isFalse()

            releaseFirst.complete(Unit)
            first.await()
            second.await()

            assertThat(secondEntered.await()).isGreaterThan(firstFence)
        }
        firstJournal.close()
        secondJournal.close()
    }

    @Test
    fun `stale owner cannot stage or complete after a larger fence takes over`() = runBlocking {
        val database = Room.inMemoryDatabaseBuilder(
            context,
            RoomHealthConnectExportDatabase::class.java,
        ).build()
        val clock = AtomicLong(1_800_000_000_000L)
        val options = RoomHealthConnectJournalOptions(
            leaseDuration = Duration.ofMillis(STALE_WRITER_LEASE_MILLIS),
            unavailableLeaseRetryDelay = Duration.ofMillis(LEASE_RETRY_MILLIS),
        )
        val staleOwner = RoomHealthConnectExportJournal.createForTest(database, options, clock::get)
        val nextOwner = RoomHealthConnectExportJournal.createForTest(database, options, clock::get)
        val record = stepRecord("stale-room-writer")
        val staleEntered = CompletableDeferred<Pair<HealthConnectSourceTransitionLease, HealthConnectPendingExport>>()
        val releaseStale = CompletableDeferred<Unit>()

        supervisorScope {
            val staleTransition = async(Dispatchers.Default) {
                staleOwner.withSourceTransition(
                    synchronizationScope.repositoryScopeKey,
                    "StepsRecord",
                    "stale-room-writer",
                ) { lease ->
                    val pending = staleOwner.stage(lease, null) { sequence ->
                        val conversion = converter.convert(record, conversionTime, sequence)
                        HealthConnectActiveExportBuilder(synchronizationScope).draft(record, conversion, null)
                    }
                    staleEntered.complete(lease to pending)
                    releaseStale.await()
                }
            }
            val (staleLease, pending) = staleEntered.await()
            clock.addAndGet(STALE_WRITER_LEASE_MILLIS + 1L)

            nextOwner.withSourceTransition(
                synchronizationScope.repositoryScopeKey,
                "StepsRecord",
                "stale-room-writer",
            ) { takeoverLease ->
                assertThat(takeoverLease.fence).isGreaterThan(staleLease.fence)
                assertStaleWriterRejected(staleOwner, staleLease, nextOwner, takeoverLease, pending)
            }
            releaseStale.complete(Unit)
            staleTransition.await()
        }
        database.close()
    }

    @Test
    fun `reconciliation lease admits child fence and excludes ordinary source in another instance`() = runBlocking {
        val databaseName = databaseName()
        val clock = AtomicLong(PROCESS_DEATH_EPOCH_MILLIS)
        val reconciliationJournal = openFenced(databaseName, clock)
        val ordinaryJournal = openFenced(databaseName, clock)
        val childEntered = CompletableDeferred<Pair<HealthConnectReconciliationLease, HealthConnectSourceTransitionLease>>()
        val releaseChild = CompletableDeferred<Unit>()
        val childExited = CompletableDeferred<Unit>()
        val releaseParent = CompletableDeferred<Unit>()

        supervisorScope {
            val reconciliation = async(Dispatchers.Default) {
                reconciliationJournal.withReconciliationLease(
                    synchronizationScope.repositoryScopeKey,
                    "StepsRecord",
                ) { parent ->
                    reconciliationJournal.withSourceTransition(
                        synchronizationScope.repositoryScopeKey,
                        "StepsRecord",
                        "reconciliation-child",
                        parent,
                    ) { child ->
                        childEntered.complete(parent to child)
                        releaseChild.await()
                    }
                    childExited.complete(Unit)
                    releaseParent.await()
                }
            }
            val (parent, child) = childEntered.await()
            assertThat(child.reconciliationFence).isEqualTo(parent.fence)
            val ordinaryEntered = CompletableDeferred<HealthConnectJournalFence>()
            val ordinary = async(Dispatchers.Default) {
                ordinaryJournal.withSourceTransition(
                    synchronizationScope.repositoryScopeKey,
                    "StepsRecord",
                    "ordinary-during-reconciliation",
                ) { lease -> ordinaryEntered.complete(lease.fence) }
            }

            delay(BLOCKED_ASSERTION_MILLIS)
            assertThat(ordinaryEntered.isCompleted).isFalse()
            releaseChild.complete(Unit)
            childExited.await()
            delay(BLOCKED_ASSERTION_MILLIS)
            assertThat(ordinaryEntered.isCompleted).isFalse()
            releaseParent.complete(Unit)
            reconciliation.await()
            ordinary.await()
            assertThat(ordinaryEntered.await()).isGreaterThan(parent.fence)
        }
        reconciliationJournal.close()
        ordinaryJournal.close()
    }

    @Test
    fun `expired orphaned lease is recovered with a larger fence after database reopen`() = runBlocking {
        val databaseName = databaseName()
        val options = RoomHealthConnectJournalOptions(
            leaseDuration = Duration.ofMillis(PROCESS_DEATH_LEASE_MILLIS),
            unavailableLeaseRetryDelay = Duration.ofMillis(LEASE_RETRY_MILLIS),
        )
        val clock = AtomicLong(PROCESS_DEATH_EPOCH_MILLIS)
        val orphanedFence = HealthConnectJournalFence("1")
        val failedProcessDatabase = database(databaseName)
        failedProcessDatabase.withTransaction {
            val dao = failedProcessDatabase.journalDao()
            dao.initializeCounter(RoomHealthConnectCounter(nextEventSequence = "1", nextFence = "2"))
            dao.upsertSourceLease(
                RoomHealthConnectSourceLease(
                    repositoryScopeKey = synchronizationScope.repositoryScopeKey.value,
                    recordType = "StepsRecord",
                    healthConnectId = "process-death",
                    owner = "terminated-process",
                    fence = orphanedFence.value,
                    reconciliationFence = null,
                    expiresAtEpochMillis = clock.get() + PROCESS_DEATH_LEASE_MILLIS,
                ),
            )
        }
        failedProcessDatabase.close()

        clock.addAndGet(PROCESS_DEATH_LEASE_MILLIS + 1L)
        val survivor = RoomHealthConnectExportJournal.createForTest(
            database(databaseName),
            options,
            clock::get,
        )
        val recoveredFence = survivor.withSourceTransition(
            synchronizationScope.repositoryScopeKey,
            "StepsRecord",
            "process-death",
        ) { it.fence }

        assertThat(recoveredFence).isGreaterThan(orphanedFence)
        survivor.close()
    }

    @Test
    fun `an unknown deletion durably quarantines its source id across reopen`() = runTest {
        val databaseName = databaseName()
        val journal = open(databaseName)
        HealthConnectExportCoordinator(converter, journal, RecordingSink())
            .delete("StepsRecord", "never-exported", conversionTime)
        journal.close()

        val reopened = database(databaseName)
        val quarantined = reopened.journalDao()
            .unmatchedDeletions(synchronizationScope.repositoryScopeKey.value, "StepsRecord")

        assertThat(quarantined.map { it.healthConnectId }).containsExactly("never-exported")
        assertThat(quarantined.single().observedAt).isEqualTo(conversionTime.toString())
        assertThat(quarantined.single().projectionScopeKey)
            .isEqualTo(synchronizationScope.projectionScopeKey.value)
        reopened.close()
    }

    @Test
    fun `reconciled absence keeps its invalidation marker and drains the outbox across reopen`() = runTest {
        val databaseName = databaseName()
        val journal = open(databaseName)
        val coordinator = HealthConnectExportCoordinator(converter, journal, RecordingSink())
        coordinator.upsert(stepRecord("reconcile-retained"), conversionTime)
        coordinator.upsert(stepRecord("reconcile-removed"), conversionTime)

        coordinator.reconcile(
            recordType = "StepsRecord",
            observedAt = { conversionTime.plusSeconds(30) },
            readAll = { listOf(stepRecord("reconcile-retained")) },
        )
        journal.close()

        val reopened = open(databaseName)
        reopened.withReconciliationLease(synchronizationScope.repositoryScopeKey, "StepsRecord") { lease ->
            val states = reopened.entries(lease).associate { it.healthConnectId to it.state }

            assertThat(states).containsExactly(
                "reconcile-retained",
                HealthConnectExportState.ACTIVE,
                "reconcile-removed",
                HealthConnectExportState.INVALIDATED,
            )
            assertThat(reopened.pendingForType(lease)).isEmpty()
        }
        reopened.close()
    }

    @Test
    fun `a refused record round-trips its durable rejection row through the codec`() = runTest {
        val databaseName = databaseName()
        val journal = open(databaseName)

        HealthConnectExportCoordinator(converter, journal, RecordingSink())
            .upsert(tearGlucoseRecord("refused-glucose"), conversionTime)
        journal.close()

        val reopened = database(databaseName)
        val rejected = reopened.journalDao()
            .rejectedRecords(synchronizationScope.repositoryScopeKey.value, "BloodGlucoseRecord")
            .single()

        assertThat(rejected.healthConnectId).isEqualTo("refused-glucose")
        assertThat(rejected.observedAt).isEqualTo(conversionTime.toString())
        assertThat(rejected.reason).isNotEmpty()
        assertThat(
            RoomHealthConnectExportJournal.open(context, databaseName).use { journalAfterReopen ->
                journalAfterReopen.withSourceTransition(
                    synchronizationScope.repositoryScopeKey,
                    "BloodGlucoseRecord",
                    "refused-glucose",
                ) { lease -> journalAfterReopen.entry(lease) }
            },
        ).isNull()
        reopened.close()
    }

    @Test
    fun `concurrent sources across database instances receive unique global sequences`() = runTest {
        val databaseName = databaseName()
        val firstJournal = open(databaseName)
        val secondJournal = open(databaseName)
        val sink = ConcurrentRecordingSink()
        val firstCoordinator = HealthConnectExportCoordinator(converter, firstJournal, sink)
        val secondCoordinator = HealthConnectExportCoordinator(converter, secondJournal, sink)

        listOf(
            async(Dispatchers.Default) {
                firstCoordinator.upsert(stepRecord("parallel-a"), conversionTime)
            },
            async(Dispatchers.Default) {
                secondCoordinator.upsert(stepRecord("parallel-b"), conversionTime.plusSeconds(1))
            },
        ).awaitAll()

        assertThat(sink.batches.map { it.eventSequence.value }).containsExactly("1", "2")
        assertThat(sink.batches.map { it.eventSequence }.toSet()).hasSize(2)
        firstJournal.close()
        secondJournal.close()
    }

    private fun databaseName(): String = "room-journal-${UUID.randomUUID()}.db".also(databaseNames::add)

    private fun open(
        databaseName: String,
        options: RoomHealthConnectJournalOptions = RoomHealthConnectJournalOptions(),
    ): RoomHealthConnectExportJournal = RoomHealthConnectExportJournal.open(context, databaseName, options)

    private fun database(databaseName: String): RoomHealthConnectExportDatabase = Room.databaseBuilder(
        context,
        RoomHealthConnectExportDatabase::class.java,
        databaseName,
    ).setJournalMode(RoomDatabase.JournalMode.WRITE_AHEAD_LOGGING).build()

    private fun openFenced(databaseName: String, clock: AtomicLong): RoomHealthConnectExportJournal =
        RoomHealthConnectExportJournal.createForTest(
            database(databaseName),
            RoomHealthConnectJournalOptions(
                leaseDuration = Duration.ofMillis(STALE_WRITER_LEASE_MILLIS),
                unavailableLeaseRetryDelay = Duration.ofMillis(LEASE_RETRY_MILLIS),
            ),
            clock::get,
        )

    private suspend fun assertStaleWriterRejected(
        staleOwner: RoomHealthConnectExportJournal,
        staleLease: HealthConnectSourceTransitionLease,
        nextOwner: RoomHealthConnectExportJournal,
        takeoverLease: HealthConnectSourceTransitionLease,
        pending: HealthConnectPendingExport,
    ) {
        val stageFailure = runCatching {
            staleOwner.stage(staleLease, null) {
                error("A stale writer must fail before constructing another draft.")
            }
        }.exceptionOrNull()
        val destinationReferences = pending.nextEntry.outputIdentifiers.associate { identifier ->
            identifier.key() to "Observation/takeover"
        }
        val completeFailure = runCatching {
            staleOwner.complete(staleLease, pending, destinationReferences)
        }.exceptionOrNull()

        assertThat(stageFailure).isInstanceOf(HealthConnectJournalLeaseLostException::class.java)
        assertThat(completeFailure).isInstanceOf(HealthConnectJournalLeaseLostException::class.java)
        assertThat(requireNotNull(nextOwner.pending(takeoverLease)).eventSequence)
            .isEqualTo(pending.eventSequence)
        nextOwner.complete(takeoverLease, pending, destinationReferences)
        assertThat(requireNotNull(nextOwner.entry(takeoverLease)).revision)
            .isEqualTo(pending.acknowledgedEntry(destinationReferences).revision)
    }

    private class CaptureThenFailSink : HealthConnectExportSink {
        var batch: HealthConnectExportBatch? = null

        override suspend fun apply(batch: HealthConnectExportBatch): HealthConnectExportAcknowledgement {
            this.batch = batch
            error("Simulated sink outage after receiving the exact payload.")
        }
    }

    private class ConcurrentRecordingSink : HealthConnectExportSink {
        val batches = ConcurrentLinkedQueue<HealthConnectExportBatch>()

        override suspend fun apply(batch: HealthConnectExportBatch): HealthConnectExportAcknowledgement {
            batches += batch
            val destinations = batch.bundle.groveOutputIdentifiers().associate { identifier ->
                identifier.key() to "Resource/destination-${identifier.value.takeLast(12)}"
            }
            return HealthConnectExportAcknowledgement(destinations)
        }
    }

    private companion object {
        const val BLOCKED_ASSERTION_MILLIS = 75L
        const val LEASE_RETRY_MILLIS = 10L
        const val PROCESS_DEATH_EPOCH_MILLIS = 1_800_000_000_000L
        const val PROCESS_DEATH_LEASE_MILLIS = 120L
        const val STALE_WRITER_LEASE_MILLIS = 5_000L
    }
}
