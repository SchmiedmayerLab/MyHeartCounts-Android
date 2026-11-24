//
// This source file is part of the My Heart Counts Android open-source project
//
// SPDX-FileCopyrightText: 2026 Stanford University and the project authors (see CONTRIBUTORS.md)
//
// SPDX-License-Identifier: MIT

package edu.stanford.myheartcounts.standard.health.exchange

import androidx.health.connect.client.records.StepsRecord
import com.google.common.truth.Truth.assertThat
import edu.stanford.myheartcounts.standard.MHCAppRevisionFixtures
import kotlinx.coroutines.test.runTest
import org.grovealliance.fhir.contract.ExchangeRecord
import org.grovealliance.health.fhir.HealthConnectDeletion
import org.grovealliance.study.FakeStudyManager
import org.junit.Test
import java.time.Instant

class ExchangeChunkPackerTest {

    private val provider = FhirExchangeExporterProviderImpl(
        identityStore = FakeFhirExchangeIdentityStore(),
        appRevision = MHCAppRevisionFixtures.create(),
        studyManager = FakeStudyManager(),
    )
    private val packer = ExchangeChunkPackerImpl()
    private val now = Instant.parse("2026-01-02T00:00:00Z")

    @Test
    fun `packs the records of one participant into one Bundle`() = runTest {
        // given
        val records = listOf(
            "a" to active(recordId = "record-1"),
            "b" to active(recordId = "record-2"),
        )

        // when
        val chunk = packer.pack(records = records, packedAt = now)

        // then
        assertThat(chunk.bundles).hasSize(1)
        assertThat(chunk.bundles.single().records().getOrThrow()).hasSize(2)
        assertThat(chunk.failedAlone).isEmpty()
        assertThat(chunk.otherSubject).isEmpty()
    }

    @Test
    fun `packs a record delivered twice once`() = runTest {
        // given
        val record = active(recordId = "record-1")

        // when
        val chunk = packer.pack(records = listOf("a" to record, "b" to record), packedAt = now)

        // then
        assertThat(chunk.bundles.single().records().getOrThrow()).hasSize(1)
    }

    @Test
    fun `starts a Bundle of its own kind for a retraction`() = runTest {
        // given
        val records = listOf(
            "a" to active(recordId = "record-1"),
            "b" to retraction(recordId = "record-2"),
        )

        // when
        val chunk = packer.pack(records = records, packedAt = now)

        // then
        assertThat(chunk.bundles.map { it.kind }).containsExactly(ExchangeRecord.Kind.ACTIVE, ExchangeRecord.Kind.RETRACTION).inOrder()
    }

    @Test
    fun `sets a record of another participant apart`() = runTest {
        // given
        val records = listOf(
            "a" to active(recordId = "record-1"),
            "b" to active(recordId = "record-2", accountId = "participant-2"),
        )

        // when
        val chunk = packer.pack(records = records, packedAt = now)

        // then
        assertThat(chunk.bundles.single().records().getOrThrow()).hasSize(1)
        assertThat(chunk.otherSubject).containsExactly("b")
    }

    @Test
    fun `packs nothing from no records`() {
        // when
        val chunk = packer.pack(records = emptyList<Pair<String, ExchangeRecord>>(), packedAt = now)

        // then
        assertThat(chunk.bundles).isEmpty()
    }

    private suspend fun active(recordId: String, accountId: String = ACCOUNT_ID): ExchangeRecord = provider
        .exporter(accountId = accountId)
        .getOrThrow()
        .export(records = listOf(StepsRecordFixtures.create(id = recordId)), instant = now)
        .single()
        .record!!

    private suspend fun retraction(recordId: String): ExchangeRecord = provider
        .exporter(accountId = ACCOUNT_ID)
        .getOrThrow()
        .retract(
            deletions = listOf(
                HealthConnectDeletion(
                    recordId = recordId,
                    recordType = StepsRecord::class,
                    deletedAfter = null,
                    detectedAt = now,
                ),
            ),
            instant = now,
        )
        .single()
        .record!!

    private companion object {
        const val ACCOUNT_ID = "participant-1"
    }
}
