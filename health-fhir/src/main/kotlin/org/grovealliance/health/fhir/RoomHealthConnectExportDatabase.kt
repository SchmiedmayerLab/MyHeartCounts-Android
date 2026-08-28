//
// This source file belongs to the My Heart Counts Android project
//
// SPDX-FileCopyrightText: 2026 Stanford University and the project authors (see CONTRIBUTORS.md)
//
// SPDX-License-Identifier: MIT

package org.grovealliance.health.fhir

import androidx.room.Dao
import androidx.room.Database
import androidx.room.Entity
import androidx.room.Index
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.PrimaryKey
import androidx.room.Query
import androidx.room.RoomDatabase
import androidx.room.Upsert

@Entity(tableName = "health_connect_export_counter")
internal data class RoomHealthConnectCounter(
    @PrimaryKey val singletonId: Int = SINGLETON_ID,
    val nextEventSequence: String,
    val nextFence: String,
) {
    companion object {
        const val SINGLETON_ID = 1
    }
}

@Entity(
    tableName = "health_connect_export_entries",
    primaryKeys = ["repositoryScopeKey", "recordType", "healthConnectId"],
    indices = [Index(value = ["repositoryScopeKey", "recordType"])],
)
internal data class RoomHealthConnectEntry(
    val repositoryScopeKey: String,
    val recordType: String,
    val healthConnectId: String,
    val revision: String,
    val encodedEntry: String,
)

@Entity(
    tableName = "health_connect_pending_exports",
    primaryKeys = ["repositoryScopeKey", "recordType", "healthConnectId"],
    indices = [
        Index(value = ["eventSequence"], unique = true),
        Index(value = ["repositoryScopeKey", "recordType"]),
    ],
)
internal data class RoomHealthConnectPendingExport(
    val repositoryScopeKey: String,
    val recordType: String,
    val healthConnectId: String,
    val eventSequence: String,
    val encodedPending: String,
)

@Entity(
    tableName = "health_connect_source_leases",
    primaryKeys = ["repositoryScopeKey", "recordType", "healthConnectId"],
    indices = [Index(value = ["repositoryScopeKey", "recordType", "expiresAtEpochMillis"])],
)
internal data class RoomHealthConnectSourceLease(
    val repositoryScopeKey: String,
    val recordType: String,
    val healthConnectId: String,
    val owner: String,
    val fence: String,
    val reconciliationFence: String?,
    val expiresAtEpochMillis: Long,
)

@Entity(
    tableName = "health_connect_reconciliation_leases",
    primaryKeys = ["repositoryScopeKey", "recordType"],
)
internal data class RoomHealthConnectReconciliationLease(
    val repositoryScopeKey: String,
    val recordType: String,
    val owner: String,
    val fence: String,
    val expiresAtEpochMillis: Long,
)

@Entity(
    tableName = "health_connect_unmatched_deletions",
    primaryKeys = ["repositoryScopeKey", "recordType", "healthConnectId"],
)
internal data class RoomHealthConnectUnmatchedDeletion(
    val repositoryScopeKey: String,
    val projectionScopeKey: String,
    val recordType: String,
    val healthConnectId: String,
    val observedAt: String,
)

@Entity(
    tableName = "health_connect_rejected_records",
    primaryKeys = ["repositoryScopeKey", "recordType", "healthConnectId"],
)
internal data class RoomHealthConnectRejectedRecord(
    val repositoryScopeKey: String,
    val projectionScopeKey: String,
    val recordType: String,
    val healthConnectId: String,
    val sourceLastModified: String,
    val observedAt: String,
    val reason: String,
)

@Dao
@Suppress("TooManyFunctions")
internal interface RoomHealthConnectExportDao {
    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun initializeCounter(counter: RoomHealthConnectCounter): Long

    @Query("SELECT * FROM health_connect_export_counter WHERE singletonId = 1")
    suspend fun counter(): RoomHealthConnectCounter

    @Upsert
    suspend fun upsertCounter(counter: RoomHealthConnectCounter)

    @Query(
        "SELECT * FROM health_connect_export_entries " +
            "WHERE repositoryScopeKey = :scope AND recordType = :recordType AND healthConnectId = :sourceId",
    )
    suspend fun entry(scope: String, recordType: String, sourceId: String): RoomHealthConnectEntry?

    @Query(
        "SELECT * FROM health_connect_export_entries " +
            "WHERE repositoryScopeKey = :scope AND recordType = :recordType " +
            "ORDER BY healthConnectId",
    )
    suspend fun entries(scope: String, recordType: String): List<RoomHealthConnectEntry>

    @Upsert
    suspend fun upsertEntry(entry: RoomHealthConnectEntry)

    @Query(
        "SELECT * FROM health_connect_pending_exports " +
            "WHERE repositoryScopeKey = :scope AND recordType = :recordType AND healthConnectId = :sourceId",
    )
    suspend fun pending(scope: String, recordType: String, sourceId: String): RoomHealthConnectPendingExport?

    @Query(
        "SELECT * FROM health_connect_pending_exports " +
            "WHERE repositoryScopeKey = :scope AND recordType = :recordType " +
            "ORDER BY LENGTH(eventSequence), eventSequence",
    )
    suspend fun pendingForType(scope: String, recordType: String): List<RoomHealthConnectPendingExport>

    @Insert
    suspend fun insertPending(pending: RoomHealthConnectPendingExport)

    @Query(
        "DELETE FROM health_connect_pending_exports " +
            "WHERE repositoryScopeKey = :scope AND recordType = :recordType AND healthConnectId = :sourceId",
    )
    suspend fun deletePending(scope: String, recordType: String, sourceId: String): Int

    @Query(
        "SELECT * FROM health_connect_source_leases " +
            "WHERE repositoryScopeKey = :scope AND recordType = :recordType AND healthConnectId = :sourceId",
    )
    suspend fun sourceLease(scope: String, recordType: String, sourceId: String): RoomHealthConnectSourceLease?

    @Upsert
    suspend fun upsertSourceLease(lease: RoomHealthConnectSourceLease)

    @Query(
        "UPDATE health_connect_source_leases SET expiresAtEpochMillis = :newExpiry " +
            "WHERE repositoryScopeKey = :scope AND recordType = :recordType AND healthConnectId = :sourceId " +
            "AND owner = :owner AND fence = :fence AND expiresAtEpochMillis > :now",
    )
    @Suppress("LongParameterList") // Room binds each fenced compare-and-set column as one SQL parameter.
    suspend fun renewSourceLease(
        scope: String,
        recordType: String,
        sourceId: String,
        owner: String,
        fence: String,
        now: Long,
        newExpiry: Long,
    ): Int

    @Query(
        "DELETE FROM health_connect_source_leases " +
            "WHERE repositoryScopeKey = :scope AND recordType = :recordType AND healthConnectId = :sourceId " +
            "AND owner = :owner AND fence = :fence",
    )
    suspend fun releaseSourceLease(
        scope: String,
        recordType: String,
        sourceId: String,
        owner: String,
        fence: String,
    ): Int

    @Query(
        "SELECT COUNT(*) FROM health_connect_source_leases " +
            "WHERE repositoryScopeKey = :scope AND recordType = :recordType AND expiresAtEpochMillis > :now",
    )
    suspend fun activeSourceLeaseCount(scope: String, recordType: String, now: Long): Int

    @Query(
        "SELECT * FROM health_connect_reconciliation_leases " +
            "WHERE repositoryScopeKey = :scope AND recordType = :recordType",
    )
    suspend fun reconciliationLease(scope: String, recordType: String): RoomHealthConnectReconciliationLease?

    @Upsert
    suspend fun upsertReconciliationLease(lease: RoomHealthConnectReconciliationLease)

    @Query(
        "UPDATE health_connect_reconciliation_leases SET expiresAtEpochMillis = :newExpiry " +
            "WHERE repositoryScopeKey = :scope AND recordType = :recordType " +
            "AND owner = :owner AND fence = :fence AND expiresAtEpochMillis > :now",
    )
    suspend fun renewReconciliationLease(
        scope: String,
        recordType: String,
        owner: String,
        fence: String,
        now: Long,
        newExpiry: Long,
    ): Int

    @Query(
        "DELETE FROM health_connect_reconciliation_leases " +
            "WHERE repositoryScopeKey = :scope AND recordType = :recordType " +
            "AND owner = :owner AND fence = :fence",
    )
    suspend fun releaseReconciliationLease(
        scope: String,
        recordType: String,
        owner: String,
        fence: String,
    ): Int

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insertUnmatchedDeletion(deletion: RoomHealthConnectUnmatchedDeletion): Long

    @Upsert
    suspend fun upsertRejectedRecord(rejected: RoomHealthConnectRejectedRecord)
}

@Database(
    entities = [
        RoomHealthConnectCounter::class,
        RoomHealthConnectEntry::class,
        RoomHealthConnectPendingExport::class,
        RoomHealthConnectSourceLease::class,
        RoomHealthConnectReconciliationLease::class,
        RoomHealthConnectUnmatchedDeletion::class,
        RoomHealthConnectRejectedRecord::class,
    ],
    version = 1,
    exportSchema = true,
)
internal abstract class RoomHealthConnectExportDatabase : RoomDatabase() {
    abstract fun journalDao(): RoomHealthConnectExportDao
}
