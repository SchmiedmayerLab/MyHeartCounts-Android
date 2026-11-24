//
// This source file is part of the My Heart Counts Android open-source project
//
// SPDX-FileCopyrightText: 2026 Stanford University and the project authors (see CONTRIBUTORS.md)
//
// SPDX-License-Identifier: MIT

package edu.stanford.myheartcounts.standard.health.exchange

import com.google.common.truth.Truth.assertThat
import edu.stanford.myheartcounts.standard.MHCAppRevisionFixtures
import kotlinx.coroutines.test.runTest
import org.grovealliance.study.FakeStudyManager
import org.grovealliance.study.fixtures.StudyEnrollmentFixtures
import org.junit.Test
import java.time.Instant
import java.util.UUID

class FhirExchangeExporterProviderTest {

    private val identityStore = FakeFhirExchangeIdentityStore()
    private val studyManager = FakeStudyManager()
    private val provider = FhirExchangeExporterProviderImpl(
        identityStore = identityStore,
        appRevision = MHCAppRevisionFixtures.create(),
        studyManager = studyManager,
    )
    private val now = Instant.parse("2026-01-02T00:00:00Z")

    @Test
    fun `converts a Record to the same record every time`() = runTest {
        // when
        val first = export(accountId = ACCOUNT_ID, provider = provider)
        val second = export(
            accountId = ACCOUNT_ID,
            provider = FhirExchangeExporterProviderImpl(
                identityStore = identityStore,
                appRevision = MHCAppRevisionFixtures.create(),
                studyManager = studyManager,
            ),
        )

        // then
        assertThat(second.json).isEqualTo(first.json)
    }

    @Test
    fun `names the participant as the subject`() = runTest {
        // when
        val record = export(accountId = ACCOUNT_ID, provider = provider)

        // then
        assertThat(record.subject?.value).isEqualTo(ACCOUNT_ID)
        assertThat(record.subject?.system?.rawValue).isEqualTo("https://myheartcounts.stanford.edu/fhir/identifiers/participant")
    }

    @Test
    fun `gives another install's copy of a Record other identities`() = runTest {
        // given
        val first = export(accountId = ACCOUNT_ID, provider = provider)
        identityStore.setSecret(secret = FhirExchangeIdentitySecretFixtures.create(keyByte = 2))

        // when
        val other = export(accountId = ACCOUNT_ID, provider = provider)

        // then
        assertThat(other.sourceRecord).isNotEqualTo(first.sourceRecord)
    }

    @Test
    fun `states the enrolled study`() = runTest {
        // given
        studyManager.setEnrollments(
            enrollments = listOf(StudyEnrollmentFixtures.create(studyId = STUDY_ID, studyRevision = 3u)),
        )

        // when
        val record = export(accountId = ACCOUNT_ID, provider = provider)

        // then
        val json = record.json.decodeToString()
        assertThat(json).contains("https://myheartcounts.stanford.edu/fhir/PlanDefinition/$STUDY_ID")
    }

    private suspend fun export(accountId: String, provider: FhirExchangeExporterProvider) = provider
        .exporter(accountId = accountId)
        .getOrThrow()
        .export(records = listOf(StepsRecordFixtures.create()), instant = now)
        .single()
        .record!!

    private companion object {
        const val ACCOUNT_ID = "participant-1"
        val STUDY_ID: UUID = UUID.fromString("00000000-0000-0000-0000-00000000000a")
    }
}
