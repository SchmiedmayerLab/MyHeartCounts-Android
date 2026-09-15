//
// This source file is part of the My Heart Counts Android open-source project
//
// SPDX-FileCopyrightText: 2026 Stanford University and the project authors (see CONTRIBUTORS.md)
//
// SPDX-License-Identifier: MIT

package edu.stanford.myheartcounts.standard.health

import android.content.Context
import androidx.room.Dao
import androidx.room.Database
import androidx.room.Entity
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.PrimaryKey
import androidx.room.Query
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.Transaction

/**
 * How many ids one statement binds at most, comfortably below the 999 host parameters SQLite
 * allows on Android 10 and 11.
 */
private const val MAX_IDS_PER_STATEMENT = 500

/**
 * [value] with the characters `LIKE` treats specially escaped by a backslash, so it only ever
 * matches itself.
 */
private fun escapeLike(value: String): String =
    value.replace("\\", "\\\\").replace("%", "\\%").replace("_", "\\_")

/**
 * One health observation waiting to be uploaded, already serialized as FHIR JSON.
 *
 * Serializing at staging time rather than at upload time is deliberate: it keeps the FHIR mapping
 * next to the record it came from, and it means an archive can be built without Health Connect being
 * reachable.
 *
 * @param id The observation's FHIR id, which is also what makes re-staging the same sample a no-op.
 * @param sampleTypeIdentifier The sample type, which decides the archive the row ends up in.
 * @param accountId The participant the row was staged for, and the only one it may be uploaded as.
 * @param json The serialized FHIR `Observation`.
 * @param effectiveAtMillis When the sample was measured, which the retention window is applied to.
 */
@Entity(tableName = "staged_observations")
data class StagedObservation(
    @PrimaryKey val id: String,
    val sampleTypeIdentifier: String,
    val accountId: String,
    val json: String,
    val effectiveAtMillis: Long,
)

/**
 * One health sample the participant deleted, waiting to be reported to the backend.
 *
 * @param sampleId The identifier the sample was uploaded under.
 * @param sampleTypeIdentifier The sample type it was uploaded as.
 * @param accountId The participant the deletion was staged for, and the only one it may be reported as.
 * @param deletedAtMillis When the deletion was observed.
 */
@Entity(tableName = "staged_deletions", primaryKeys = ["sampleId", "sampleTypeIdentifier"])
data class StagedDeletion(
    val sampleId: String,
    val sampleTypeIdentifier: String,
    val accountId: String,
    val deletedAtMillis: Long,
)

/**
 * The observation ids one Health Connect record carrying a series was delivered as.
 *
 * Health Connect reports a deleted record by the record's id alone, while each of its readings
 * reached the backend under an id of its own. This is what lets a deletion name every one of them.
 *
 * @param recordId The Health Connect record id.
 * @param sampleTypeIdentifier The sample type the readings were delivered as.
 * @param observationIds The readings' observation ids, comma-separated.
 * @param updatedAtMillis When the record was last delivered, which retention is applied to.
 */
@Entity(tableName = "series_record_readings", primaryKeys = ["recordId", "sampleTypeIdentifier"])
data class SeriesRecordReadings(
    val recordId: String,
    val sampleTypeIdentifier: String,
    val observationIds: String,
    val updatedAtMillis: Long,
)

/**
 * Reads and writes the readings tracked per series record.
 */
@Dao
interface SeriesRecordReadingsDao {

    /**
     * The readings tracked for [recordId], or `null` when none are.
     */
    @Query(
        "SELECT * FROM series_record_readings " +
            "WHERE recordId = :recordId AND sampleTypeIdentifier = :sampleTypeIdentifier",
    )
    suspend fun readings(recordId: String, sampleTypeIdentifier: String): SeriesRecordReadings?

    /**
     * Saves [readings], replacing what was tracked for the same records.
     */
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun save(readings: List<SeriesRecordReadings>)

    /**
     * Stops tracking the readings of [recordId].
     */
    @Query(
        "DELETE FROM series_record_readings " +
            "WHERE recordId = :recordId AND sampleTypeIdentifier = :sampleTypeIdentifier",
    )
    suspend fun remove(recordId: String, sampleTypeIdentifier: String)

    /**
     * Stops tracking records last delivered before [cutoffMillis].
     */
    @Query("DELETE FROM series_record_readings WHERE updatedAtMillis < :cutoffMillis")
    suspend fun removeOlderThan(cutoffMillis: Long)

    /**
     * Stops tracking every record, for when the participant signs out.
     */
    @Query("DELETE FROM series_record_readings")
    suspend fun clear()

    /**
     * Which of [ids] are still staged, and so never left the device. Chunked like
     * [StagedObservationDao.removeObservations].
     */
    @Transaction
    suspend fun stagedObservationIdsAmong(ids: List<String>): List<String> =
        ids.chunked(MAX_IDS_PER_STATEMENT).flatMap { stagedObservationIds(ids = it) }

    /**
     * Which of [ids] are still staged. Bounded by [MAX_IDS_PER_STATEMENT]; use
     * [stagedObservationIdsAmong].
     */
    @Query("SELECT id FROM staged_observations WHERE id IN (:ids)")
    suspend fun stagedObservationIds(ids: List<String>): List<String>
}

/**
 * Reads and writes the staged observations.
 */
@Dao
interface StagedObservationDao {

    /**
     * Stages [observations], replacing any already staged under the same id.
     */
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun stage(observations: List<StagedObservation>)

    /**
     * The sample types with observations of [accountId] old enough to upload.
     *
     * @param cutoffMillis The newest measurement time still held back by the retention window.
     */
    @Query(
        "SELECT DISTINCT sampleTypeIdentifier FROM staged_observations " +
            "WHERE accountId = :accountId AND effectiveAtMillis <= :cutoffMillis",
    )
    suspend fun sampleTypesReadyForUpload(accountId: String, cutoffMillis: Long): List<String>

    /**
     * The staged observations of [accountId] and one sample type that are old enough to upload.
     */
    @Query(
        "SELECT * FROM staged_observations " +
            "WHERE accountId = :accountId AND sampleTypeIdentifier = :sampleTypeIdentifier " +
            "AND effectiveAtMillis <= :cutoffMillis " +
            "ORDER BY effectiveAtMillis ASC LIMIT :limit",
    )
    suspend fun observationsReadyForUpload(
        accountId: String,
        sampleTypeIdentifier: String,
        cutoffMillis: Long,
        limit: Int,
    ): List<StagedObservation>

    /**
     * Removes the observations that were successfully uploaded.
     *
     * Chunked, because one statement binding every id overruns SQLite's host-parameter limit, which
     * is only 999 on the SQLite versions Android 10 and 11 ship.
     */
    @Transaction
    suspend fun removeObservations(ids: List<String>) {
        ids.chunked(MAX_IDS_PER_STATEMENT).forEach { deleteObservations(ids = it) }
    }

    /**
     * Removes the observations with [ids]. Bounded by [MAX_IDS_PER_STATEMENT]; use
     * [removeObservations].
     */
    @Query("DELETE FROM staged_observations WHERE id IN (:ids)")
    suspend fun deleteObservations(ids: List<String>)

    /**
     * Removes every staged observation the Health Connect record [recordId] produced.
     *
     * A record carrying a series was staged as one row per reading, keyed `recordId-suffix`, so
     * matching the bare record id alone would leave those rows to upload after all.
     *
     * @return How many rows were removed.
     */
    suspend fun removeObservationsOfRecord(recordId: String): Int =
        deleteObservationsOfRecord(recordId = recordId, readingPattern = "${escapeLike(recordId)}-%")

    /**
     * Removes the observation [recordId] produced, and those of its readings. See
     * [removeObservationsOfRecord].
     */
    @Query("DELETE FROM staged_observations WHERE id = :recordId OR id LIKE :readingPattern ESCAPE '\\'")
    suspend fun deleteObservationsOfRecord(recordId: String, readingPattern: String): Int

    /**
     * Removes every staged observation of [sampleTypeIdentifier], for when Health Connect asks for a
     * full re-sync and the staged rows may no longer reflect what it holds.
     */
    @Query("DELETE FROM staged_observations WHERE sampleTypeIdentifier = :sampleTypeIdentifier")
    suspend fun removeObservationsOfType(sampleTypeIdentifier: String)

    /**
     * Removes what any participant other than [accountId] left staged, for when the cleanup at their
     * sign-out did not complete.
     */
    @Query("DELETE FROM staged_observations WHERE accountId != :accountId")
    suspend fun removeObservationsNotOwnedBy(accountId: String)

    /**
     * Drops everything staged, for when the participant signs out and their data must not survive
     * into the next session.
     */
    @Query("DELETE FROM staged_observations")
    suspend fun clearObservations()
}

/**
 * Reads and writes the staged deletions.
 */
@Dao
interface StagedDeletionDao {

    /**
     * Stages [deletions], replacing any already staged for the same sample.
     */
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun stageDeletions(deletions: List<StagedDeletion>)

    /**
     * Every deletion staged for [accountId].
     */
    @Query("SELECT * FROM staged_deletions WHERE accountId = :accountId ORDER BY deletedAtMillis ASC")
    suspend fun allDeletions(accountId: String): List<StagedDeletion>

    /**
     * Removes the deletions that were successfully reported, chunked like
     * [StagedObservationDao.removeObservations].
     */
    @Transaction
    suspend fun removeDeletions(sampleIds: List<String>) {
        sampleIds.chunked(MAX_IDS_PER_STATEMENT).forEach { deleteDeletions(sampleIds = it) }
    }

    /**
     * Removes the deletions with [sampleIds]. Bounded by [MAX_IDS_PER_STATEMENT]; use
     * [removeDeletions].
     */
    @Query("DELETE FROM staged_deletions WHERE sampleId IN (:sampleIds)")
    suspend fun deleteDeletions(sampleIds: List<String>)

    /**
     * Removes what any participant other than [accountId] left staged. See
     * [StagedObservationDao.removeObservationsNotOwnedBy].
     */
    @Query("DELETE FROM staged_deletions WHERE accountId != :accountId")
    suspend fun removeDeletionsNotOwnedBy(accountId: String)

    /**
     * Drops every staged deletion. See [StagedObservationDao.clearObservations].
     */
    @Query("DELETE FROM staged_deletions")
    suspend fun clearDeletions()
}

/**
 * The on-device queue of health data waiting to reach the backend.
 *
 * The Kotlin counterpart of iOS's GRDB-backed `HealthUploadStaging`. It exists because health data
 * arrives continuously in small batches while uploading is worth doing rarely and in bulk, and
 * because the study deliberately holds samples back for
 * [HealthUploadSchedule.RETENTION_OFFSET_DAYS] before uploading them.
 */
@Database(
    entities = [StagedObservation::class, StagedDeletion::class, SeriesRecordReadings::class],
    version = 3,
    exportSchema = false,
)
abstract class HealthStagingDatabase : RoomDatabase() {

    /**
     * Access to the staged observations.
     */
    abstract fun observations(): StagedObservationDao

    /**
     * Access to the staged deletions.
     */
    abstract fun deletions(): StagedDeletionDao

    /**
     * Access to the readings tracked per series record.
     */
    abstract fun seriesReadings(): SeriesRecordReadingsDao

    companion object {

        private const val DATABASE_NAME = "mhc-health-staging"

        /**
         * Opens the staging database for [context].
         *
         * No released build shipped an earlier schema, so an outdated one is simply recreated.
         */
        fun create(context: Context): HealthStagingDatabase =
            Room.databaseBuilder(
                context.applicationContext,
                HealthStagingDatabase::class.java,
                DATABASE_NAME,
            ).fallbackToDestructiveMigration(dropAllTables = true).build()
    }
}
