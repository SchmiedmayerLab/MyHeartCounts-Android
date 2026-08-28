//
// This source file belongs to the My Heart Counts Android project
//
// SPDX-FileCopyrightText: 2026 Stanford University and the project authors (see CONTRIBUTORS.md)
//
// SPDX-License-Identifier: MIT

package org.grovealliance.health.fhir

import android.content.Context
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.withTransaction
import kotlinx.coroutines.NonCancellable
import kotlinx.coroutines.cancelAndJoin
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.math.BigInteger
import java.time.Duration
import java.util.UUID
import java.util.concurrent.ConcurrentHashMap

/** Lease timing for [RoomHealthConnectExportJournal]. */
data class RoomHealthConnectJournalOptions(
    val leaseDuration: Duration = Duration.ofSeconds(DEFAULT_LEASE_SECONDS),
    val unavailableLeaseRetryDelay: Duration = Duration.ofMillis(DEFAULT_RETRY_MILLIS),
) {
    init {
        require(!leaseDuration.isZero && !leaseDuration.isNegative) {
            "The journal lease duration must be positive."
        }
        require(!unavailableLeaseRetryDelay.isZero && !unavailableLeaseRetryDelay.isNegative) {
            "The unavailable-lease retry delay must be positive."
        }
        require(leaseDuration.toMillis() >= MINIMUM_LEASE_MILLIS) {
            "The journal lease duration must be at least $MINIMUM_LEASE_MILLIS milliseconds."
        }
    }

    internal val leaseMillis: Long = leaseDuration.toMillis()
    internal val retryMillis: Long = unavailableLeaseRetryDelay.toMillis().coerceAtLeast(1L)
    internal val renewalMillis: Long = (leaseMillis / RENEWAL_DIVISOR).coerceAtLeast(1L)

    private companion object {
        const val DEFAULT_LEASE_SECONDS = 30L
        const val DEFAULT_RETRY_MILLIS = 50L
        const val MINIMUM_LEASE_MILLIS = 30L
        const val RENEWAL_DIVISOR = 3L
    }
}

/** A Room-backed, process-safe durable outbox and fenced Health Connect export journal. */
@Suppress("TooManyFunctions")
class RoomHealthConnectExportJournal private constructor(
    private val database: RoomHealthConnectExportDatabase,
    private val options: RoomHealthConnectJournalOptions,
    private val epochMillis: () -> Long,
) : HealthConnectExportJournal, AutoCloseable {
    private val dao = database.journalDao()
    private val sourceOwners = ConcurrentHashMap<String, String>()
    private val reconciliationOwners = ConcurrentHashMap<String, String>()

    override suspend fun <T> withSourceTransition(
        repositoryScopeKey: ScopeKey,
        recordType: String,
        healthConnectId: String,
        reconciliationLease: HealthConnectReconciliationLease?,
        block: suspend (HealthConnectSourceTransitionLease) -> T,
    ): T {
        requireSourceCoordinates(recordType, healthConnectId)
        reconciliationLease?.let {
            require(it.repositoryScopeKey == repositoryScopeKey && it.recordType == recordType) {
                "A parent reconciliation lease must cover the source transition's repository and Record type."
            }
        }
        val ownership = awaitSourceLease(
            repositoryScopeKey,
            recordType,
            healthConnectId,
            reconciliationLease,
        )
        sourceOwners[ownership.lease.fence.value] = ownership.owner
        return runRenewableLease(
            lease = ownership.lease,
            renew = { renewSourceLease(ownership) },
            release = { releaseSourceLease(ownership) },
            block = block,
        )
    }

    override suspend fun <T> withReconciliationLease(
        repositoryScopeKey: ScopeKey,
        recordType: String,
        block: suspend (HealthConnectReconciliationLease) -> T,
    ): T {
        requireRecordType(recordType)
        val ownership = awaitReconciliationLease(repositoryScopeKey, recordType)
        reconciliationOwners[ownership.lease.fence.value] = ownership.owner
        return runRenewableLease(
            lease = ownership.lease,
            renew = { renewReconciliationLease(ownership) },
            release = { releaseReconciliationLease(ownership) },
            block = block,
        )
    }

    override suspend fun entry(lease: HealthConnectSourceTransitionLease): HealthConnectExportJournalEntry? =
        database.withTransaction {
            requireSourceLeaseLocked(lease)
            dao.entry(lease.repositoryScopeKey.value, lease.recordType, lease.healthConnectId)
                ?.let { RoomHealthConnectJournalCodec.decodeEntry(it.encodedEntry) }
        }
    override suspend fun entries(lease: HealthConnectReconciliationLease): List<HealthConnectExportJournalEntry> =
        database.withTransaction {
            requireReconciliationLeaseLocked(lease)
            dao.entries(lease.repositoryScopeKey.value, lease.recordType).map {
                RoomHealthConnectJournalCodec.decodeEntry(it.encodedEntry)
            }
        }

    override suspend fun pending(lease: HealthConnectSourceTransitionLease): HealthConnectPendingExport? =
        database.withTransaction {
            requireSourceLeaseLocked(lease)
            dao.pending(lease.repositoryScopeKey.value, lease.recordType, lease.healthConnectId)
                ?.let { RoomHealthConnectJournalCodec.decodePending(it.encodedPending) }
        }

    override suspend fun pendingForType(
        lease: HealthConnectReconciliationLease,
    ): List<HealthConnectPendingExport> = database.withTransaction {
        requireReconciliationLeaseLocked(lease)
        dao.pendingForType(lease.repositoryScopeKey.value, lease.recordType).map {
            RoomHealthConnectJournalCodec.decodePending(it.encodedPending)
        }
    }

    override suspend fun stage(
        lease: HealthConnectSourceTransitionLease,
        expectedRevision: HealthConnectJournalRevision?,
        buildDraft: (eventSequence: EventSequence) -> HealthConnectPendingExportDraft,
    ): HealthConnectPendingExport = database.withTransaction {
        requireSourceLeaseLocked(lease)
        dao.pending(lease.repositoryScopeKey.value, lease.recordType, lease.healthConnectId)?.let {
            return@withTransaction RoomHealthConnectJournalCodec.decodePending(it.encodedPending)
        }
        check(currentRevision(lease) == expectedRevision) {
            "The journal base revision changed before staging."
        }
        initializeCounterLocked()
        val counter = dao.counter()
        val eventSequence = EventSequence(counter.nextEventSequence)
        val draft = buildDraft(eventSequence)
        requireDraftMatchesLease(draft, lease)
        val pending = draft.toPending(eventSequence, expectedRevision)
        dao.insertPending(pending.toRoomEntity())
        dao.upsertCounter(
            counter.copy(nextEventSequence = counter.nextEventSequence.incrementDecimal()),
        )
        pending
    }

    override suspend fun complete(
        lease: HealthConnectSourceTransitionLease,
        pending: HealthConnectPendingExport,
        entry: HealthConnectExportJournalEntry,
    ) = database.withTransaction {
        requireSourceLeaseLocked(lease)
        requireSourceMatchesLease(pending.repositoryScopeKey, pending.recordType, pending.healthConnectId, lease)
        requireSourceMatchesLease(entry.repositoryScopeKey, entry.recordType, entry.healthConnectId, lease)
        val expectedEntry = pending.acknowledgedEntry(entry.destinationReferences)
        check(expectedEntry.revision == entry.revision) {
            "Completion must be derived from the exact validated pending payload."
        }
        val storedPendingRow = dao.pending(
            lease.repositoryScopeKey.value,
            lease.recordType,
            lease.healthConnectId,
        )
        if (storedPendingRow == null) {
            check(currentRevision(lease) == entry.revision) {
                "Only an exact-event, exact-revision repeated completion is idempotent."
            }
            return@withTransaction
        }
        val storedPending = RoomHealthConnectJournalCodec.decodePending(storedPendingRow.encodedPending)
        check(storedPending.sameExactEvent(pending)) {
            "Completion did not name the exact staged event."
        }
        check(currentRevision(lease) == storedPending.baseRevision) {
            "The journal base revision changed before completion."
        }
        dao.upsertEntry(entry.toRoomEntity())
        check(
            dao.deletePending(lease.repositoryScopeKey.value, lease.recordType, lease.healthConnectId) == 1,
        ) { "The exact pending event disappeared before completion." }
    }

    override suspend fun storeLocal(
        lease: HealthConnectSourceTransitionLease,
        expectedRevision: HealthConnectJournalRevision?,
        entry: HealthConnectExportJournalEntry,
    ) = database.withTransaction {
        requireSourceLeaseLocked(lease)
        requireSourceMatchesLease(entry.repositoryScopeKey, entry.recordType, entry.healthConnectId, lease)
        check(entry.outputIdentifiers.isEmpty()) {
            "A local-only transition cannot bypass delivery of active or retraction outputs."
        }
        check(dao.pending(lease.repositoryScopeKey.value, lease.recordType, lease.healthConnectId) == null) {
            "A local-only transition cannot replace a staged outbox event."
        }
        check(currentRevision(lease) == expectedRevision) {
            "The journal base revision changed before local storage."
        }
        dao.upsertEntry(entry.toRoomEntity())
    }

    override suspend fun recordUnmatchedDeletion(
        lease: HealthConnectSourceTransitionLease,
        deletion: HealthConnectUnmatchedDeletion,
    ) = database.withTransaction {
        requireSourceLeaseLocked(lease)
        requireSourceMatchesLease(
            deletion.repositoryScopeKey,
            deletion.recordType,
            deletion.healthConnectId,
            lease,
        )
        dao.insertUnmatchedDeletion(
            RoomHealthConnectUnmatchedDeletion(
                repositoryScopeKey = deletion.repositoryScopeKey.value,
                projectionScopeKey = deletion.projectionScopeKey.value,
                recordType = deletion.recordType,
                healthConnectId = deletion.healthConnectId,
                observedAt = deletion.observedAt.toString(),
            ),
        )
        Unit
    }

    override suspend fun recordRejectedRecord(
        lease: HealthConnectSourceTransitionLease,
        rejected: HealthConnectRejectedRecord,
    ) = database.withTransaction {
        requireSourceLeaseLocked(lease)
        requireSourceMatchesLease(
            rejected.repositoryScopeKey,
            rejected.recordType,
            rejected.healthConnectId,
            lease,
        )
        dao.upsertRejectedRecord(
            RoomHealthConnectRejectedRecord(
                repositoryScopeKey = rejected.repositoryScopeKey.value,
                projectionScopeKey = rejected.projectionScopeKey.value,
                recordType = rejected.recordType,
                healthConnectId = rejected.healthConnectId,
                sourceLastModified = rejected.sourceLastModified.toString(),
                observedAt = rejected.observedAt.toString(),
                reason = rejected.reason,
            ),
        )
    }

    override fun close() = database.close()

    private suspend fun awaitSourceLease(
        scope: ScopeKey,
        recordType: String,
        sourceId: String,
        reconciliationLease: HealthConnectReconciliationLease?,
    ): SourceLeaseOwnership {
        val owner = UUID.randomUUID().toString()
        while (true) {
            tryAcquireSourceLease(scope, recordType, sourceId, reconciliationLease, owner)?.let { return it }
            delay(options.retryMillis)
        }
    }

    private suspend fun tryAcquireSourceLease(
        scope: ScopeKey,
        recordType: String,
        sourceId: String,
        reconciliationLease: HealthConnectReconciliationLease?,
        owner: String,
    ): SourceLeaseOwnership? = database.withTransaction {
        val now = epochMillis()
        val reconciliation = dao.reconciliationLease(scope.value, recordType)
        val parentFence = reconciliationLease?.fence?.value
        if (reconciliationLease == null) {
            if (reconciliation != null && reconciliation.expiresAtEpochMillis > now) return@withTransaction null
        } else {
            val currentParentFence = reconciliationLease.fence.value
            val parentOwner = reconciliationOwners[currentParentFence] ?: return@withTransaction null
            if (!reconciliation.isCurrentOwner(currentParentFence, parentOwner, now)) {
                throw HealthConnectJournalLeaseLostException("The parent reconciliation lease is no longer owned.")
            }
        }
        val current = dao.sourceLease(scope.value, recordType, sourceId)
        if (current != null && current.expiresAtEpochMillis > now) return@withTransaction null
        val fence = allocateFenceLocked()
        dao.upsertSourceLease(
            RoomHealthConnectSourceLease(
                repositoryScopeKey = scope.value,
                recordType = recordType,
                healthConnectId = sourceId,
                owner = owner,
                fence = fence.value,
                reconciliationFence = parentFence,
                expiresAtEpochMillis = leaseExpiry(now),
            ),
        )
        SourceLeaseOwnership(
            HealthConnectSourceTransitionLease(scope, recordType, sourceId, fence, reconciliationLease?.fence),
            owner,
        )
    }

    private suspend fun awaitReconciliationLease(
        scope: ScopeKey,
        recordType: String,
    ): ReconciliationLeaseOwnership {
        val owner = UUID.randomUUID().toString()
        while (true) {
            tryAcquireReconciliationLease(scope, recordType, owner)?.let { return it }
            delay(options.retryMillis)
        }
    }

    private suspend fun tryAcquireReconciliationLease(
        scope: ScopeKey,
        recordType: String,
        owner: String,
    ): ReconciliationLeaseOwnership? = database.withTransaction {
        val now = epochMillis()
        val current = dao.reconciliationLease(scope.value, recordType)
        if (current != null && current.expiresAtEpochMillis > now) return@withTransaction null
        if (dao.activeSourceLeaseCount(scope.value, recordType, now) != 0) return@withTransaction null
        val fence = allocateFenceLocked()
        dao.upsertReconciliationLease(
            RoomHealthConnectReconciliationLease(
                repositoryScopeKey = scope.value,
                recordType = recordType,
                owner = owner,
                fence = fence.value,
                expiresAtEpochMillis = leaseExpiry(now),
            ),
        )
        ReconciliationLeaseOwnership(
            HealthConnectReconciliationLease(scope, recordType, fence),
            owner,
        )
    }

    private suspend fun renewSourceLease(ownership: SourceLeaseOwnership): Boolean = database.withTransaction {
        val lease = ownership.lease
        val now = epochMillis()
        lease.reconciliationFence?.let { requireReconciliationLeaseLocked(
            HealthConnectReconciliationLease(lease.repositoryScopeKey, lease.recordType, it),
        ) }
        dao.renewSourceLease(
            lease.repositoryScopeKey.value,
            lease.recordType,
            lease.healthConnectId,
            ownership.owner,
            lease.fence.value,
            now,
            leaseExpiry(now),
        ) == 1
    }

    private suspend fun renewReconciliationLease(ownership: ReconciliationLeaseOwnership): Boolean {
        val lease = ownership.lease
        val now = epochMillis()
        return dao.renewReconciliationLease(
            lease.repositoryScopeKey.value,
            lease.recordType,
            ownership.owner,
            lease.fence.value,
            now,
            leaseExpiry(now),
        ) == 1
    }

    private suspend fun releaseSourceLease(ownership: SourceLeaseOwnership) {
        val lease = ownership.lease
        try {
            dao.releaseSourceLease(
                lease.repositoryScopeKey.value,
                lease.recordType,
                lease.healthConnectId,
                ownership.owner,
                lease.fence.value,
            )
        } finally {
            sourceOwners.remove(lease.fence.value, ownership.owner)
        }
    }

    private suspend fun releaseReconciliationLease(ownership: ReconciliationLeaseOwnership) {
        val lease = ownership.lease
        try {
            dao.releaseReconciliationLease(
                lease.repositoryScopeKey.value,
                lease.recordType,
                ownership.owner,
                lease.fence.value,
            )
        } finally {
            reconciliationOwners.remove(lease.fence.value, ownership.owner)
        }
    }

    private suspend fun requireSourceLeaseLocked(lease: HealthConnectSourceTransitionLease) {
        val owner = sourceOwners[lease.fence.value]
            ?: throw HealthConnectJournalLeaseLostException("The source-transition lease is not owned by this journal.")
        val stored = dao.sourceLease(lease.repositoryScopeKey.value, lease.recordType, lease.healthConnectId)
        val now = epochMillis()
        if (!stored.isCurrentOwner(lease, owner, now)) {
            throw HealthConnectJournalLeaseLostException("The source-transition fence is stale or expired.")
        }
        lease.reconciliationFence?.let { fence ->
            requireReconciliationLeaseLocked(
                HealthConnectReconciliationLease(lease.repositoryScopeKey, lease.recordType, fence),
            )
        }
    }

    private suspend fun requireReconciliationLeaseLocked(lease: HealthConnectReconciliationLease) {
        val owner = reconciliationOwners[lease.fence.value]
            ?: throw HealthConnectJournalLeaseLostException("The reconciliation lease is not owned by this journal.")
        val stored = dao.reconciliationLease(lease.repositoryScopeKey.value, lease.recordType)
        if (!stored.isCurrentOwner(lease, owner, epochMillis())) {
            throw HealthConnectJournalLeaseLostException("The reconciliation fence is stale or expired.")
        }
    }

    private suspend fun initializeCounterLocked() {
        dao.initializeCounter(
            RoomHealthConnectCounter(nextEventSequence = FIRST_COUNTER_VALUE, nextFence = FIRST_COUNTER_VALUE),
        )
    }

    private suspend fun allocateFenceLocked(): HealthConnectJournalFence {
        initializeCounterLocked()
        val counter = dao.counter()
        val fence = HealthConnectJournalFence(counter.nextFence)
        dao.upsertCounter(counter.copy(nextFence = counter.nextFence.incrementDecimal()))
        return fence
    }

    private suspend fun currentRevision(lease: HealthConnectSourceTransitionLease): HealthConnectJournalRevision? =
        dao.entry(lease.repositoryScopeKey.value, lease.recordType, lease.healthConnectId)
            ?.revision
            ?.let(::HealthConnectJournalRevision)

    private fun leaseExpiry(now: Long): Long =
        runCatching { Math.addExact(now, options.leaseMillis) }.getOrElse { Long.MAX_VALUE }

    private suspend fun <L, T> runRenewableLease(
        lease: L,
        renew: suspend () -> Boolean,
        release: suspend () -> Unit,
        block: suspend (L) -> T,
    ): T = coroutineScope {
        val heartbeat = launch {
            while (true) {
                delay(options.renewalMillis)
                if (!renew()) {
                    throw HealthConnectJournalLeaseLostException("The Room journal lease could not be renewed.")
                }
            }
        }
        try {
            block(lease)
        } finally {
            heartbeat.cancelAndJoin()
            withContext(NonCancellable) { runCatching { release() } }
        }
    }

    private fun HealthConnectPendingExportDraft.toPending(
        sequence: EventSequence,
        revision: HealthConnectJournalRevision?,
    ): HealthConnectPendingExport = HealthConnectPendingExport(
        eventSequence = sequence,
        baseRevision = revision,
        repositoryScopeKey = repositoryScopeKey,
        projectionScopeKey = projectionScopeKey,
        operation = operation,
        recordType = recordType,
        healthConnectId = healthConnectId,
        sourceRecordIdentifier = sourceRecordIdentifier,
        sourceVersion = sourceVersion,
        bundle = bundle,
        bundleJson = bundleJson,
        payloadSha256 = payloadSha256,
        retractedTargets = retractedTargets,
        nextEntry = nextEntry,
    )

    private fun HealthConnectPendingExport.toRoomEntity() = RoomHealthConnectPendingExport(
        repositoryScopeKey.value,
        recordType,
        healthConnectId,
        eventSequence.value,
        RoomHealthConnectJournalCodec.encodePending(this),
    )

    private fun HealthConnectExportJournalEntry.toRoomEntity() = RoomHealthConnectEntry(
        repositoryScopeKey.value,
        recordType,
        healthConnectId,
        revision.value,
        RoomHealthConnectJournalCodec.encodeEntry(this),
    )

    private data class SourceLeaseOwnership(
        val lease: HealthConnectSourceTransitionLease,
        val owner: String,
    )

    private data class ReconciliationLeaseOwnership(
        val lease: HealthConnectReconciliationLease,
        val owner: String,
    )

    companion object {
        const val DEFAULT_DATABASE_NAME = "grove_health_connect_export_journal.db"

        /** Opens the production journal without destructive migration fallback. */
        fun open(
            context: Context,
            databaseName: String = DEFAULT_DATABASE_NAME,
            options: RoomHealthConnectJournalOptions = RoomHealthConnectJournalOptions(),
        ): RoomHealthConnectExportJournal {
            require(databaseName.isNotBlank()) { "The Room journal database name must not be blank." }
            val database = Room.databaseBuilder(
                context.applicationContext,
                RoomHealthConnectExportDatabase::class.java,
                databaseName,
            ).setJournalMode(RoomDatabase.JournalMode.WRITE_AHEAD_LOGGING).build()
            return RoomHealthConnectExportJournal(database, options, System::currentTimeMillis)
        }

        internal fun createForTest(
            database: RoomHealthConnectExportDatabase,
            options: RoomHealthConnectJournalOptions,
            epochMillis: () -> Long = System::currentTimeMillis,
        ): RoomHealthConnectExportJournal = RoomHealthConnectExportJournal(database, options, epochMillis)

        private const val FIRST_COUNTER_VALUE = "1"
    }
}

/** Raised when a process attempts to use a stale or expired journal fencing token. */
class HealthConnectJournalLeaseLostException(message: String) : IllegalStateException(message)

private fun String.incrementDecimal(): String = (BigInteger(this) + BigInteger.ONE).toString()

private fun requireRecordType(recordType: String) {
    require(recordType.isNotBlank()) { "A journal lease must identify one source Record type." }
    GroveUnicode.requireScalarText(recordType, "Journal Record type")
}

private fun requireSourceCoordinates(recordType: String, sourceId: String) {
    requireRecordType(recordType)
    require(sourceId.isNotBlank()) { "A journal lease must identify one source Record id." }
    GroveUnicode.requireScalarText(sourceId, "Journal source Record id")
}

private fun requireDraftMatchesLease(
    draft: HealthConnectPendingExportDraft,
    lease: HealthConnectSourceTransitionLease,
) = requireSourceMatchesLease(draft.repositoryScopeKey, draft.recordType, draft.healthConnectId, lease)

private fun requireSourceMatchesLease(
    scope: ScopeKey,
    recordType: String,
    sourceId: String,
    lease: HealthConnectSourceTransitionLease,
) {
    require(
        scope == lease.repositoryScopeKey && recordType == lease.recordType && sourceId == lease.healthConnectId,
    ) { "Journal state must identify the exact source-transition lease." }
}

private fun HealthConnectPendingExport.sameExactEvent(other: HealthConnectPendingExport): Boolean =
    eventSequence == other.eventSequence &&
        baseRevision == other.baseRevision &&
        repositoryScopeKey == other.repositoryScopeKey &&
        projectionScopeKey == other.projectionScopeKey &&
        operation == other.operation &&
        recordType == other.recordType &&
        healthConnectId == other.healthConnectId &&
        sourceVersion == other.sourceVersion &&
        sourceRecordIdentifier.equalsDeep(other.sourceRecordIdentifier) &&
        bundleJson == other.bundleJson &&
        payloadSha256 == other.payloadSha256 &&
        retractedTargets == other.retractedTargets &&
        nextEntry.revision == other.nextEntry.revision

private fun RoomHealthConnectReconciliationLease?.isCurrentOwner(
    expectedFence: String,
    expectedOwner: String,
    now: Long,
): Boolean {
    if (this == null) return false
    return listOf(
        fence == expectedFence,
        owner == expectedOwner,
        expiresAtEpochMillis > now,
    ).all { it }
}

private fun RoomHealthConnectSourceLease?.isCurrentOwner(
    lease: HealthConnectSourceTransitionLease,
    expectedOwner: String,
    now: Long,
): Boolean {
    if (this == null) return false
    return listOf(
        owner == expectedOwner,
        fence == lease.fence.value,
        reconciliationFence == lease.reconciliationFence?.value,
        expiresAtEpochMillis > now,
    ).all { it }
}

private fun RoomHealthConnectReconciliationLease?.isCurrentOwner(
    lease: HealthConnectReconciliationLease,
    expectedOwner: String,
    now: Long,
): Boolean {
    if (this == null) return false
    return listOf(
        owner == expectedOwner,
        fence == lease.fence.value,
        expiresAtEpochMillis > now,
    ).all { it }
}
