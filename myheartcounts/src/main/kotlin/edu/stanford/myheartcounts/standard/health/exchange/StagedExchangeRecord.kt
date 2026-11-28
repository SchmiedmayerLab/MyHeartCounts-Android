//
// This source file is part of the My Heart Counts Android open-source project
//
// SPDX-FileCopyrightText: 2026 Stanford University and the project authors (see CONTRIBUTORS.md)
//
// SPDX-License-Identifier: MIT

package edu.stanford.myheartcounts.standard.health.exchange

import androidx.room.Dao
import androidx.room.Entity
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query

/**
 * One exchange record waiting to be uploaded: a converted Record or the retraction of a deleted one,
 * stored as its compressed one-record Bundle.
 *
 * @property kind [KIND_ACTIVE] for a converted Record, [KIND_RETRACTION] for a deleted one
 * @property sampleTypeIdentifier the sample type, which names the file the record travels in
 * @property recordId the Health Connect record id; with the kind and the type it keys the row, so a
 *   newer version of the same Record replaces the staged one
 * @property accountId the participant the record was converted for, and the only one it may be uploaded as
 * @property payload the record's one-record Bundle, zstd-compressed
 * @property stagedAtMillis when the record was staged, which the retention window is applied to
 */
@Entity(tableName = "staged_exchange_records", primaryKeys = ["kind", "sampleTypeIdentifier", "recordId"])
data class StagedExchangeRecord(
    val kind: String,
    val sampleTypeIdentifier: String,
    val recordId: String,
    val accountId: String,
    val payload: ByteArray,
    val stagedAtMillis: Long,
) {

    override fun equals(other: Any?): Boolean = other is StagedExchangeRecord && key == other.key && payload.contentEquals(other.payload)

    override fun hashCode(): Int = key.hashCode()

    private val key get() = listOf(kind, sampleTypeIdentifier, recordId, accountId, stagedAtMillis)

    companion object {
        const val KIND_ACTIVE = "active"
        const val KIND_RETRACTION = "retraction"
    }
}

/**
 * Reads and writes the staged exchange records.
 */
@Dao
interface StagedExchangeRecordDao {

    /**
     * Stages [records], replacing any staged under the same kind, type and record id.
     */
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun stage(records: List<StagedExchangeRecord>)

    /**
     * The sample types with records of [kind] of [accountId] staged no later than [cutoffMillis].
     */
    @Query(
        "SELECT DISTINCT sampleTypeIdentifier FROM staged_exchange_records " +
            "WHERE kind = :kind AND accountId = :accountId AND stagedAtMillis <= :cutoffMillis",
    )
    suspend fun sampleTypesReady(kind: String, accountId: String, cutoffMillis: Long): List<String>

    /**
     * Up to [limit] records of [kind] and one sample type of [accountId] staged no later than
     * [cutoffMillis], oldest first.
     */
    @Query(
        "SELECT * FROM staged_exchange_records " +
            "WHERE kind = :kind AND accountId = :accountId AND sampleTypeIdentifier = :sampleTypeIdentifier " +
            "AND stagedAtMillis <= :cutoffMillis ORDER BY stagedAtMillis ASC, recordId ASC LIMIT :limit",
    )
    suspend fun ready(
        kind: String,
        accountId: String,
        sampleTypeIdentifier: String,
        cutoffMillis: Long,
        limit: Int,
    ): List<StagedExchangeRecord>

    /**
     * Removes the record of [kind] and [sampleTypeIdentifier] staged for [recordId].
     */
    @Query(
        "DELETE FROM staged_exchange_records " +
            "WHERE kind = :kind AND sampleTypeIdentifier = :sampleTypeIdentifier AND recordId = :recordId",
    )
    suspend fun remove(kind: String, sampleTypeIdentifier: String, recordId: String)

    /**
     * Removes every record of [sampleTypeIdentifier] staged as [kind].
     */
    @Query("DELETE FROM staged_exchange_records WHERE kind = :kind AND sampleTypeIdentifier = :sampleTypeIdentifier")
    suspend fun removeOfType(kind: String, sampleTypeIdentifier: String)

    /**
     * Removes every record staged for anyone but [accountId].
     */
    @Query("DELETE FROM staged_exchange_records WHERE accountId != :accountId")
    suspend fun removeNotOwnedBy(accountId: String)

    /**
     * Removes every staged record.
     */
    @Query("DELETE FROM staged_exchange_records")
    suspend fun clear()
}
