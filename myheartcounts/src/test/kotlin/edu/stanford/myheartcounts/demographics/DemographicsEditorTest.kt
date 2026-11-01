//
// This source file is part of the My Heart Counts Android open-source project
//
// SPDX-FileCopyrightText: 2026 Stanford University and the project authors (see CONTRIBUTORS.md)
//
// SPDX-License-Identifier: MIT

package edu.stanford.myheartcounts.demographics

import com.google.common.truth.Truth.assertThat
import edu.stanford.myheartcounts.account.DidOptInToTrialKey
import edu.stanford.myheartcounts.account.FakeAccountService
import edu.stanford.myheartcounts.account.WeightInKgKey
import edu.stanford.myheartcounts.model.demographics.BiologicalSex
import edu.stanford.myheartcounts.model.demographics.MHCGenderIdentity
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.advanceTimeBy
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import org.grovealliance.account.AccountDetails
import org.grovealliance.account.AccountKeys
import org.grovealliance.account.FakeAccount
import org.grovealliance.account.accountId
import org.grovealliance.core.time.FakeTimeProvider
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class DemographicsEditorTest {
    private val account = FakeAccount()
    private val accountService = FakeAccountService()
    private val timeProvider = FakeTimeProvider()
    private val regionSource = FakeDemographicsRegionSource()
    private val codec = DemographicsCodecImpl(timeProvider = timeProvider)

    @Test
    fun `reads the signed-in participant's answers without storing them`() = runTest {
        // given
        val editor = editor()

        // when
        advanceTimeBy(delayTimeMillis = 2_000)
        runCurrent()

        // then
        assertThat(editor.state.value.accountId).isEqualTo("participant")
        assertThat(editor.state.value.response).isEqualTo(DemographicsResponseFixtures.answered())
        assertThat(accountService.updates).isEmpty()
    }

    @Test
    fun `rapid edits are stored once, a second after the last`() = runTest {
        // given
        val editor = editor()
        editor.update { it.copy(weightInKg = 80.0) }
        advanceTimeBy(delayTimeMillis = 500)

        // when
        editor.update { it.copy(weightInKg = 81.0) }
        advanceTimeBy(delayTimeMillis = 999)
        runCurrent()
        val updatesBeforeDelay = accountService.updates.size
        advanceTimeBy(delayTimeMillis = 2)
        runCurrent()

        // then
        assertThat(updatesBeforeDelay).isEqualTo(0)
        assertThat(accountService.updates).hasSize(1)
        assertThat(accountService.updates.single().modifiedDetails.accountKeyTypes).containsExactly(WeightInKgKey::class)
        assertThat(accountService.updates.single().modifiedDetails[WeightInKgKey::class]).isEqualTo(81.0)
        assertThat(editor.state.value.pending).isEmpty()
    }

    @Test
    fun `a flush removes cleared answers right away`() = runTest {
        // given
        val editor = editor()
        editor.update { it.copy(weightInKg = null) }

        // when
        val result = editor.flush()

        // then
        assertThat(result.isSuccess).isTrue()
        assertThat(accountService.updates.single().removedAccountKeys).containsExactly(WeightInKgKey::class)
        assertThat(accountService.updates.single().modifiedDetails.isEmpty).isTrue()
    }

    @Test
    fun `a flush without changes succeeds without storing anything`() = runTest {
        // given
        val editor = editor()

        // when
        val result = editor.flush()

        // then
        assertThat(result.isSuccess).isTrue()
        assertThat(accountService.updates).isEmpty()
    }

    @Test
    fun `a failed save keeps the edit pending until a retry stores it`() = runTest {
        // given
        val editor = editor()
        accountService.setUpdateResult(result = Result.failure(IllegalStateException("offline")))
        editor.update { it.copy(weightInKg = 80.0) }
        val failed = editor.flush()
        val stateAfterFailure = editor.state.value
        accountService.setUpdateResult(result = Result.success(Unit))

        // when
        val retried = editor.flush()

        // then
        assertThat(failed.isFailure).isTrue()
        assertThat(stateAfterFailure.pending).containsExactly(DemographicsField.Weight)
        assertThat(retried.isSuccess).isTrue()
        assertThat(editor.state.value.pending).isEmpty()
    }

    @Test
    fun `every failed save is reported`() = runTest {
        // given
        val editor = editor()
        val failures = mutableListOf<Unit>()
        backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) { editor.saveFailures.collect { failures.add(it) } }
        accountService.setUpdateResult(result = Result.failure(IllegalStateException("offline")))
        editor.update { it.copy(weightInKg = 80.0) }

        // when
        editor.flush()
        editor.flush()

        // then
        assertThat(failures).hasSize(2)
    }

    @Test
    fun `edits imply the missing counterpart of gender identity and sex at birth`() = runTest {
        // given
        val editor = editor(
            response = DemographicsResponseFixtures.answered().copy(
                genderIdentity = null,
                biologicalSexAtBirth = null,
            ),
        )

        // when
        editor.update { it.copy(genderIdentity = MHCGenderIdentity.FEMALE) }

        // then
        assertThat(editor.state.value.response.biologicalSexAtBirth).isEqualTo(BiologicalSex.FEMALE)
        assertThat(editor.state.value.pending).containsExactly(DemographicsField.GenderIdentity, DemographicsField.SexAtBirth)
    }

    @Test
    fun `a refresh of the account keeps edits not stored yet`() = runTest {
        // given
        val editor = editor()
        editor.update { it.copy(weightInKg = 80.0) }

        // when
        account.supplyUserDetails(
            details = details(response = DemographicsResponseFixtures.answered().copy(heightInCm = 180.0)),
        )
        runCurrent()
        editor.flush()

        // then
        assertThat(editor.state.value.response.weightInKg).isEqualTo(80.0)
        assertThat(editor.state.value.response.heightInCm).isEqualTo(180.0)
        assertThat(accountService.updates.single().modifiedDetails.accountKeyTypes).containsExactly(WeightInKgKey::class)
    }

    @Test
    fun `another participant signing in discards the former participant's edits`() = runTest {
        // given
        val editor = editor()
        editor.update { it.copy(heightInCm = 190.0) }

        // when
        account.supplyUserDetails(
            details = details(
                response = DemographicsResponseFixtures.answered(),
                accountId = "another",
            ),
        )
        runCurrent()
        advanceTimeBy(delayTimeMillis = 2_000)
        runCurrent()

        // then
        assertThat(accountService.updates).isEmpty()
        assertThat(editor.state.value.accountId).isEqualTo("another")
        assertThat(editor.state.value.response.heightInCm).isEqualTo(172.5)
        assertThat(editor.state.value.pending).isEmpty()
    }

    @Test
    fun `edits are ignored while nobody is signed in`() = runTest {
        // given
        val editor = editor()
        account.removeUserDetails()
        runCurrent()

        // when
        editor.update { it.copy(heightInCm = 190.0) }
        val result = editor.flush()

        // then
        assertThat(editor.state.value.accountId).isNull()
        assertThat(editor.state.value.response).isEqualTo(DemographicsResponse.empty)
        assertThat(result.isFailure).isTrue()
        assertThat(accountService.updates).isEmpty()
    }

    @Test
    fun `completion follows the answers and the trial opt-in`() = runTest {
        // given
        val editor = editor(
            response = DemographicsResponseFixtures.answered().copy(comorbidities = null),
            optedIntoTrial = true,
        )

        // when
        val incomplete = editor.isComplete.value
        editor.update { it.copy(comorbidities = DemographicsResponseFixtures.answered().comorbidities) }
        runCurrent()

        // then
        assertThat(incomplete).isFalse()
        assertThat(editor.isComplete.value).isTrue()
        assertThat(editor.completion.value[DemographicsField.Comorbidities]?.isAnswered).isTrue()
    }

    @Test
    fun `the form switches when the region changes`() = runTest {
        // given
        val editor = editor()

        // when
        regionSource.setRegion(region = DemographicsRegion.UK)
        runCurrent()

        // then
        assertThat(editor.state.value.region).isEqualTo(DemographicsRegion.UK)
    }

    @Test
    fun `the form follows the region source`() = runTest {
        // given
        regionSource.setRegion(region = DemographicsRegion.UK)

        // when
        val editor = editor()

        // then
        assertThat(editor.state.value.region).isEqualTo(DemographicsRegion.UK)
    }

    private fun TestScope.editor(
        response: DemographicsResponse = DemographicsResponseFixtures.answered(),
        optedIntoTrial: Boolean = false,
    ): DemographicsEditor {
        account.supplyUserDetails(
            details = details(
                response = response,
                optedIntoTrial = optedIntoTrial,
            ),
        )
        val editor = DemographicsEditorImpl(
            account = account,
            accountService = accountService,
            codec = codec,
            rules = DemographicsRules(),
            regionSource = regionSource,
            scope = backgroundScope,
        )
        editor.configure()
        runCurrent()
        return editor
    }

    private fun details(
        response: DemographicsResponse,
        accountId: String = "participant",
        optedIntoTrial: Boolean = false,
    ): AccountDetails = codec.changes(
        baseline = DemographicsResponse.empty,
        response = response,
    ).getOrThrow().modifiedDetails.apply {
        this[AccountKeys.accountId::class] = accountId
        this[DidOptInToTrialKey::class] = optedIntoTrial
    }
}
