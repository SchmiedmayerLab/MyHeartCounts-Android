//
// This source file is part of the My Heart Counts Android open-source project
//
// SPDX-FileCopyrightText: 2026 Stanford University and the project authors (see CONTRIBUTORS.md)
//
// SPDX-License-Identifier: MIT

package edu.stanford.myheartcounts.standard.health.exchange

import androidx.health.connect.client.records.StepsRecord
import com.github.luben.zstd.Zstd
import com.google.common.truth.Truth.assertThat
import edu.stanford.myheartcounts.standard.MHCAppRevisionFixtures
import kotlinx.coroutines.test.runTest
import org.grovealliance.core.time.FakeTimeProvider
import org.grovealliance.fhir.contract.ExchangeRecord
import org.grovealliance.study.FakeStudyManager
import org.junit.Test
import java.time.Instant

class HealthExchangeStagingTest {

    private val records = FakeStagedExchangeRecordDao()
    private val timeProvider = FakeTimeProvider().apply {
        setNow(instant = Instant.parse("2026-01-02T00:00:00Z"))
    }
    private val staging = HealthExchangeStagingImpl(
        records = records,
        exporterProvider = FhirExchangeExporterProviderImpl(
            identityStore = FakeFhirExchangeIdentityStore(),
            appRevision = MHCAppRevisionFixtures.create(),
            studyManager = FakeStudyManager(),
        ),
        timeProvider = timeProvider,
    )

    @Test
    fun `stages each converted Record as an active record`() = runTest {
        // when
        staging.stageRecords(
            records = listOf(StepsRecordFixtures.create(id = "record-1"), StepsRecordFixtures.create(id = "record-2")),
            sampleTypeIdentifier = SAMPLE_TYPE,
            accountId = ACCOUNT_ID,
        ).getOrThrow()

        // then
        assertThat(records.staged.map { it.recordId }).containsExactly("record-1", "record-2")
        assertThat(records.staged.map { it.kind }.distinct()).containsExactly(StagedExchangeRecord.KIND_ACTIVE)
        assertThat(records.staged.map { it.accountId }.distinct()).containsExactly(ACCOUNT_ID)
        assertThat(records.staged.map { it.stagedAtMillis }.distinct()).containsExactly(timeProvider.currentTimeMillis())
        assertThat(records.staged.map { decoded(row = it).kind }.distinct()).containsExactly(ExchangeRecord.Kind.ACTIVE)
    }

    @Test
    fun `replaces the staged record of a Record delivered again`() = runTest {
        // given
        stage(record = StepsRecordFixtures.create(id = "record-1", count = 100))

        // when
        stage(record = StepsRecordFixtures.create(id = "record-1", count = 200))

        // then
        assertThat(records.staged).hasSize(1)
        assertThat(decoded(row = records.staged.single()).json.decodeToString()).contains("200")
    }

    @Test
    fun `replaces a staged Record by its retraction once deleted`() = runTest {
        // given
        stage(record = StepsRecordFixtures.create(id = "record-1"))

        // when
        staging.stageDeletions(
            recordIds = setOf("record-1"),
            recordType = StepsRecord::class,
            sampleTypeIdentifier = SAMPLE_TYPE,
            accountId = ACCOUNT_ID,
        ).getOrThrow()

        // then
        val row = records.staged.single()
        assertThat(row.kind).isEqualTo(StagedExchangeRecord.KIND_RETRACTION)
        assertThat(row.recordId).isEqualTo("record-1")
        assertThat(decoded(row = row).kind).isEqualTo(ExchangeRecord.Kind.RETRACTION)
    }

    @Test
    fun `stages nothing for no Records`() = runTest {
        // when
        staging.stageRecords(
            records = emptyList(),
            sampleTypeIdentifier = SAMPLE_TYPE,
            accountId = ACCOUNT_ID,
        ).getOrThrow()

        // then
        assertThat(records.staged).isEmpty()
    }

    private suspend fun stage(record: StepsRecord) {
        staging.stageRecords(
            records = listOf(record),
            sampleTypeIdentifier = SAMPLE_TYPE,
            accountId = ACCOUNT_ID,
        ).getOrThrow()
    }

    private fun decoded(row: StagedExchangeRecord): ExchangeRecord = ExchangeRecord.validating(
        json = Zstd.decompress(row.payload, Zstd.getFrameContentSize(row.payload).toInt()),
    ).getOrThrow()

    private companion object {
        const val ACCOUNT_ID = "participant-1"
        const val SAMPLE_TYPE = "steps"
    }
}
