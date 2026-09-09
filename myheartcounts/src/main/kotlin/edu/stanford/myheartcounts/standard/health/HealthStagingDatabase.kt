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

/**
 * One health observation waiting to be uploaded, already serialized as FHIR JSON.
 *
 * Serializing at staging time rather than at upload time is deliberate: it keeps the FHIR mapping
 * next to the record it came from, and it means an archive can be built without Health Connect being
 * reachable.
 *
 * @param id The observation's FHIR id, which is also what makes re-staging the same sample a no-op.
 * @param sampleTypeIdentifier The sample type, which decides the archive the row ends up in.
 * @param json The serialized FHIR `Observation`.
 * @param effectiveAtMillis When the sample was measured, which the retention window is applied to.
 */
@Entity(tableName = "staged_observations")
data class StagedObservation(
    @PrimaryKey val id: String,
    val sampleTypeIdentifier: String,
    val json: String,
    val effectiveAtMillis: Long,
)

/**
 * One health sample the participant deleted, waiting to be reported to the backend.
 *
 * @param sampleId The identifier the sample was uploaded under.
 * @param sampleTypeIdentifier The sample type it was uploaded as.
 * @param deletedAtMillis When the deletion was observed.
 */
@Entity(tableName = "staged_deletions", primaryKeys = ["sampleId", "sampleTypeIdentifier"])
data class StagedDeletion(
    val sampleId: String,
    val sampleTypeIdentifier: String,
    val deletedAtMillis: Long,
)

/**
 * Reads and writes the staged health data.
 */
@Dao
interface HealthStagingDao {

    /**
     * Stages [observations], replacing any already staged under the same id.
     */
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun stage(observations: List<StagedObservation>)

    /**
     * Stages [deletions], replacing any already staged for the same sample.
     */
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun stageDeletions(deletions: List<StagedDeletion>)

    /**
     * The sample types with observations old enough to upload.
     *
     * @param cutoffMillis The newest measurement time still held back by the retention window.
     */
    @Query("SELECT DISTINCT sampleTypeIdentifier FROM staged_observations WHERE effectiveAtMillis <= :cutoffMillis")
    suspend fun sampleTypesReadyForUpload(cutoffMillis: Long): List<String>

    /**
     * The staged observations of one sample type that are old enough to upload.
     */
    @Query(
        "SELECT * FROM staged_observations " +
            "WHERE sampleTypeIdentifier = :sampleTypeIdentifier AND effectiveAtMillis <= :cutoffMillis " +
            "ORDER BY effectiveAtMillis ASC LIMIT :limit",
    )
    suspend fun observationsReadyForUpload(
        sampleTypeIdentifier: String,
        cutoffMillis: Long,
        limit: Int,
    ): List<StagedObservation>

    /**
     * Removes the observations that were successfully uploaded.
     */
    @Query("DELETE FROM staged_observations WHERE id IN (:ids)")
    suspend fun removeObservations(ids: List<String>)

    /**
     * Removes every staged observation of [sampleTypeIdentifier], for when Health Connect asks for a
     * full re-sync and the staged rows may no longer reflect what it holds.
     */
    @Query("DELETE FROM staged_observations WHERE sampleTypeIdentifier = :sampleTypeIdentifier")
    suspend fun removeObservationsOfType(sampleTypeIdentifier: String)

    /**
     * Every staged deletion.
     */
    @Query("SELECT * FROM staged_deletions ORDER BY deletedAtMillis ASC")
    suspend fun allDeletions(): List<StagedDeletion>

    /**
     * Removes the deletions that were successfully reported.
     */
    @Query("DELETE FROM staged_deletions WHERE sampleId IN (:sampleIds)")
    suspend fun removeDeletions(sampleIds: List<String>)

    /**
     * Drops everything staged, for when the participant signs out and their data must not survive
     * into the next session.
     */
    @Query("DELETE FROM staged_observations")
    suspend fun clearObservations()

    /**
     * Drops every staged deletion. See [clearObservations].
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
    entities = [StagedObservation::class, StagedDeletion::class],
    version = 1,
    exportSchema = false,
)
abstract class HealthStagingDatabase : RoomDatabase() {

    /**
     * Access to the staged rows.
     */
    abstract fun dao(): HealthStagingDao

    companion object {

        private const val DATABASE_NAME = "mhc-health-staging"

        /**
         * Opens the staging database for [context].
         */
        fun create(context: Context): HealthStagingDatabase =
            Room.databaseBuilder(
                context.applicationContext,
                HealthStagingDatabase::class.java,
                DATABASE_NAME,
            ).build()
    }
}
