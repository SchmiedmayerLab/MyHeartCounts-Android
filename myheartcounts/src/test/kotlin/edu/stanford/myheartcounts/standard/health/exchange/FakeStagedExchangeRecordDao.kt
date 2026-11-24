//
// This source file is part of the My Heart Counts Android open-source project
//
// SPDX-FileCopyrightText: 2026 Stanford University and the project authors (see CONTRIBUTORS.md)
//
// SPDX-License-Identifier: MIT

package edu.stanford.myheartcounts.standard.health.exchange

/**
 * An in-memory [StagedExchangeRecordDao] answering every query as the staging database does.
 */
class FakeStagedExchangeRecordDao : StagedExchangeRecordDao {
    private val rows = linkedMapOf<Triple<String, String, String>, StagedExchangeRecord>()

    /**
     * Every staged record, in staging order.
     */
    val staged: List<StagedExchangeRecord> get() = rows.values.toList()

    override suspend fun stage(records: List<StagedExchangeRecord>) {
        records.forEach { rows[it.rowKey] = it }
    }

    override suspend fun sampleTypesReady(kind: String, accountId: String, cutoffMillis: Long): List<String> =
        rows.values
            .filter { it.kind == kind && it.accountId == accountId && it.stagedAtMillis <= cutoffMillis }
            .map { it.sampleTypeIdentifier }
            .distinct()

    override suspend fun ready(
        kind: String,
        accountId: String,
        sampleTypeIdentifier: String,
        cutoffMillis: Long,
        limit: Int,
    ): List<StagedExchangeRecord> = rows.values
        .filter {
            it.kind == kind &&
                it.accountId == accountId &&
                it.sampleTypeIdentifier == sampleTypeIdentifier &&
                it.stagedAtMillis <= cutoffMillis
        }
        .sortedWith(compareBy({ it.stagedAtMillis }, { it.recordId }))
        .take(limit)

    override suspend fun remove(kind: String, sampleTypeIdentifier: String, recordId: String) {
        rows.remove(Triple(kind, sampleTypeIdentifier, recordId))
    }

    override suspend fun removeOfType(kind: String, sampleTypeIdentifier: String) {
        rows.values.removeAll { it.kind == kind && it.sampleTypeIdentifier == sampleTypeIdentifier }
    }

    override suspend fun removeNotOwnedBy(accountId: String) {
        rows.values.removeAll { it.accountId != accountId }
    }

    override suspend fun clear() {
        rows.clear()
    }

    private val StagedExchangeRecord.rowKey get() = Triple(kind, sampleTypeIdentifier, recordId)
}
