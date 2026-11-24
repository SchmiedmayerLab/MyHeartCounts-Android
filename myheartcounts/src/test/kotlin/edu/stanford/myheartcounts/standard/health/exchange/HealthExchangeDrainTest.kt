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
import edu.stanford.myheartcounts.standard.health.FakeManagedFileUpload
import edu.stanford.myheartcounts.standard.health.HealthUploadCategory
import edu.stanford.myheartcounts.standard.health.RecordedUpload
import kotlinx.coroutines.test.runTest
import org.grovealliance.core.time.FakeTimeProvider
import org.grovealliance.fhir.contract.ExchangeGraph
import org.grovealliance.fhir.contract.ExchangeRecord
import org.grovealliance.study.FakeStudyManager
import org.junit.Test
import java.time.Duration
import java.time.Instant

class HealthExchangeDrainTest {

    private val records = FakeStagedExchangeRecordDao()
    private val managedUpload = FakeManagedFileUpload()
    private val timeProvider = FakeTimeProvider().apply {
        setNow(instant = Instant.parse("2026-01-02T00:00:00Z"))
    }
    private val stagedAt = timeProvider.currentTimeMillis()
    private val staging = HealthExchangeStagingImpl(
        records = records,
        exporterProvider = FhirExchangeExporterProviderImpl(
            identityStore = FakeFhirExchangeIdentityStore(),
            appRevision = MHCAppRevisionFixtures.create(),
            studyManager = FakeStudyManager(),
        ),
        timeProvider = timeProvider,
    )
    private val drain = HealthExchangeDrainImpl(
        records = records,
        packer = ExchangeChunkPackerImpl(),
        managedUpload = managedUpload,
        timeProvider = timeProvider,
    )

    @Test
    fun `uploads the due records of a sample type as one Bundle`() = runTest {
        // given
        stage(recordIds = listOf("record-1", "record-2"))

        // when
        val isComplete = drain.drain(accountId = ACCOUNT_ID, cutoffMillis = stagedAt, maximumArchives = null).getOrThrow()

        // then
        assertThat(isComplete).isTrue()
        val upload = managedUpload.uploads.single()
        assertThat(upload.accountId).isEqualTo(ACCOUNT_ID)
        assertThat(upload.category).isEqualTo(HealthUploadCategory.LIVE)
        assertThat(upload.fileName).matches("${SAMPLE_TYPE}_[0-9a-f]{24}\\.json\\.zstd")
        assertThat(bundle(upload = upload, kind = ExchangeRecord.Kind.ACTIVE).records().getOrThrow()).hasSize(2)
        assertThat(records.staged).isEmpty()
    }

    @Test
    fun `names the Bundle of the same staged records the same`() = runTest {
        // given
        stage(recordIds = listOf("record-1", "record-2"))
        val rows = records.staged
        drain.drain(accountId = ACCOUNT_ID, cutoffMillis = stagedAt, maximumArchives = null).getOrThrow()
        timeProvider.advanceBy(duration = Duration.ofHours(1))
        records.stage(records = rows.reversed())

        // when
        drain.drain(accountId = ACCOUNT_ID, cutoffMillis = stagedAt, maximumArchives = null).getOrThrow()

        // then
        val (first, second) = managedUpload.uploads
        assertThat(second.fileName).isEqualTo(first.fileName)
    }

    @Test
    fun `holds back records staged after the cutoff`() = runTest {
        // given
        stage(recordIds = listOf("record-1"))

        // when
        val isComplete = drain.drain(accountId = ACCOUNT_ID, cutoffMillis = stagedAt - 1, maximumArchives = null).getOrThrow()

        // then
        assertThat(isComplete).isTrue()
        assertThat(managedUpload.uploads).isEmpty()
        assertThat(records.staged).hasSize(1)
    }

    @Test
    fun `uploads retractions as deletions after the converted Records`() = runTest {
        // given
        stage(recordIds = listOf("record-1"))
        staging.stageDeletions(
            recordIds = setOf("record-2"),
            recordType = StepsRecord::class,
            sampleTypeIdentifier = SAMPLE_TYPE,
            accountId = ACCOUNT_ID,
        ).getOrThrow()

        // when
        drain.drain(accountId = ACCOUNT_ID, cutoffMillis = stagedAt, maximumArchives = null).getOrThrow()

        // then
        assertThat(managedUpload.uploads.map { it.category })
            .containsExactly(HealthUploadCategory.LIVE, HealthUploadCategory.DELETIONS)
            .inOrder()
        assertThat(bundle(upload = managedUpload.uploads.last(), kind = ExchangeRecord.Kind.RETRACTION).records().getOrThrow()).hasSize(1)
    }

    @Test
    fun `uploads an unreadable row alone as its stored bytes`() = runTest {
        // given
        val payload = Zstd.compress("not a record".toByteArray())
        records.stage(
            records = listOf(
                StagedExchangeRecordFixtures.create(
                    sampleTypeIdentifier = SAMPLE_TYPE,
                    recordId = "record-1",
                    accountId = ACCOUNT_ID,
                    payload = payload,
                    stagedAtMillis = stagedAt,
                ),
            ),
        )

        // when
        drain.drain(accountId = ACCOUNT_ID, cutoffMillis = stagedAt, maximumArchives = null).getOrThrow()

        // then
        val upload = managedUpload.uploads.single()
        assertThat(upload.fileName).matches("unreadable_${SAMPLE_TYPE}_[0-9a-f]{24}\\.zstd")
        assertThat(upload.bytes).isEqualTo(payload)
        assertThat(records.staged).isEmpty()
    }

    @Test
    fun `stops once the archive budget is spent`() = runTest {
        // given
        stage(recordIds = listOf("record-1"))
        stage(recordIds = listOf("record-2"), sampleTypeIdentifier = OTHER_SAMPLE_TYPE)

        // when
        val isComplete = drain.drain(accountId = ACCOUNT_ID, cutoffMillis = stagedAt, maximumArchives = 1).getOrThrow()

        // then
        assertThat(isComplete).isFalse()
        assertThat(managedUpload.uploads).hasSize(1)
        assertThat(records.staged).hasSize(1)
    }

    @Test
    fun `keeps the rows of a chunk whose upload failed`() = runTest {
        // given
        stage(recordIds = listOf("record-1"))
        managedUpload.setFailure(error = IllegalStateException("offline"))

        // when
        val result = drain.drain(accountId = ACCOUNT_ID, cutoffMillis = stagedAt, maximumArchives = null)

        // then
        assertThat(result.isFailure).isTrue()
        assertThat(records.staged).hasSize(1)
    }

    @Test
    fun `leaves another participant's rows staged`() = runTest {
        // given
        stage(recordIds = listOf("record-1"), accountId = "participant-2")

        // when
        drain.drain(accountId = ACCOUNT_ID, cutoffMillis = stagedAt, maximumArchives = null).getOrThrow()

        // then
        assertThat(managedUpload.uploads).isEmpty()
        assertThat(records.staged).hasSize(1)
    }

    private suspend fun stage(
        recordIds: List<String>,
        sampleTypeIdentifier: String = SAMPLE_TYPE,
        accountId: String = ACCOUNT_ID,
    ) {
        staging.stageRecords(
            records = recordIds.map { StepsRecordFixtures.create(id = it) },
            sampleTypeIdentifier = sampleTypeIdentifier,
            accountId = accountId,
        ).getOrThrow()
    }

    private fun bundle(upload: RecordedUpload, kind: ExchangeRecord.Kind): ExchangeGraph = ExchangeGraph.validating(
        json = Zstd.decompress(upload.bytes, Zstd.getFrameContentSize(upload.bytes).toInt()),
        kind = kind,
    ).getOrThrow()

    private companion object {
        const val ACCOUNT_ID = "participant-1"
        const val SAMPLE_TYPE = "steps"
        const val OTHER_SAMPLE_TYPE = "heartRate"
    }
}
