//
// This source file is part of the My Heart Counts Android open-source project
//
// SPDX-FileCopyrightText: 2026 Stanford University and the project authors (see CONTRIBUTORS.md)
//
// SPDX-License-Identifier: MIT

package edu.stanford.myheartcounts.standard.health.exchange

/**
 * Staged exchange records for tests.
 */
object StagedExchangeRecordFixtures {

    /**
     * A [StagedExchangeRecord] with the given properties.
     */
    fun create(
        kind: String = StagedExchangeRecord.KIND_ACTIVE,
        sampleTypeIdentifier: String = "",
        recordId: String = "",
        accountId: String = "",
        payload: ByteArray = byteArrayOf(),
        stagedAtMillis: Long = 0,
    ): StagedExchangeRecord = StagedExchangeRecord(
        kind = kind,
        sampleTypeIdentifier = sampleTypeIdentifier,
        recordId = recordId,
        accountId = accountId,
        payload = payload,
        stagedAtMillis = stagedAtMillis,
    )
}
