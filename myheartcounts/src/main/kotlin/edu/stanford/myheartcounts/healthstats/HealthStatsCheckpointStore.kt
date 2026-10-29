//
// This source file is part of the My Heart Counts Android open-source project
//
// SPDX-FileCopyrightText: 2026 Stanford University and the project authors (see CONTRIBUTORS.md)
//
// SPDX-License-Identifier: MIT

package edu.stanford.myheartcounts.healthstats

import kotlinx.serialization.Serializable
import org.grovealliance.account.Account
import org.grovealliance.account.accountId
import org.grovealliance.storage.local.KeyValueStorage
import org.grovealliance.storage.local.getSerializable
import org.grovealliance.storage.local.putSerializable
import java.time.YearMonth

/**
 * Acknowledged change history and the records belonging to its monthly snapshot.
 */
@Serializable
data class HealthStatsCheckpoint(
    val tokens: Map<String, String>,
    val recordIds: Set<String>,
    val zoneId: String,
)

/**
 * Stores acknowledged monthly checkpoints separately for each participant and metric.
 */
interface HealthStatsCheckpointStore {
    fun get(metricId: String, month: YearMonth): HealthStatsCheckpoint?
    fun put(metricId: String, month: YearMonth, checkpoint: HealthStatsCheckpoint)
    fun clear()
}

class HealthStatsCheckpointStoreImpl(
    private val storage: KeyValueStorage,
    private val account: Account,
) : HealthStatsCheckpointStore {
    override fun get(metricId: String, month: YearMonth): HealthStatsCheckpoint? =
        storage.getSerializable(key = key(metricId = metricId, month = month))

    override fun put(metricId: String, month: YearMonth, checkpoint: HealthStatsCheckpoint) {
        storage.putSerializable(
            key = key(metricId = metricId, month = month),
            value = checkpoint,
        )
    }

    override fun clear() = storage.clear()

    private fun key(metricId: String, month: YearMonth): String =
        "${account.details.value?.accountId.orEmpty()}/$metricId/$month"
}
