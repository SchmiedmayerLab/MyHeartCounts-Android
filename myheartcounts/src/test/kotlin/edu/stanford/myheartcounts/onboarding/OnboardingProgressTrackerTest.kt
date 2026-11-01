//
// This source file is part of the My Heart Counts Android open-source project
//
// SPDX-FileCopyrightText: 2026 Stanford University and the project authors (see CONTRIBUTORS.md)
//
// SPDX-License-Identifier: MIT

package edu.stanford.myheartcounts.onboarding

import com.google.common.truth.Truth.assertThat
import edu.stanford.myheartcounts.account.FakeAccountService
import edu.stanford.myheartcounts.account.MostRecentOnboardingStepKey
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.runTest
import org.grovealliance.account.AccountDetails
import org.grovealliance.account.FakeAccount
import org.junit.Test
import edu.stanford.myheartcounts.model.OnboardingStep as RecordedOnboardingStep

@OptIn(ExperimentalCoroutinesApi::class)
class OnboardingProgressTrackerTest {

    private val account = FakeAccount()
    private val accountService = FakeAccountService()
    private val steps = MutableStateFlow(OnboardingStep.Welcome)
    private val tracker = OnboardingProgressTrackerImpl(
        account = account,
        accountService = accountService,
    )

    @Test
    fun `records each step under its account identifier`() = runTest {
        // given
        account.supplyUserDetails(details = AccountDetails())
        startTracking()

        // when
        val recorded = RECORDED_STEPS.keys.associateWith { step ->
            steps.value = step
            recordedSteps().last()
        }

        // then
        assertThat(recorded).containsExactlyEntriesIn(RECORDED_STEPS)
    }

    @Test
    fun `records the current step once somebody signs in`() = runTest {
        // given
        steps.value = OnboardingStep.HealthAccess
        startTracking()

        // when
        account.supplyUserDetails(details = AccountDetails())

        // then
        assertThat(recordedSteps()).containsExactly(RecordedOnboardingStep.HEALTH_ACCESS)
    }

    @Test
    fun `records nothing while nobody is signed in`() = runTest {
        // given
        startTracking()

        // when
        steps.value = OnboardingStep.Eligibility

        // then
        assertThat(accountService.updates).isEmpty()
    }

    @Test
    fun `records nothing for the eligibility failure and reactivation pages`() = runTest {
        // given
        steps.value = OnboardingStep.Ineligible
        account.supplyUserDetails(details = AccountDetails())
        startTracking()

        // when
        steps.value = OnboardingStep.CountryUnavailable
        steps.value = OnboardingStep.ReactivateAccount

        // then
        assertThat(accountService.updates).isEmpty()
    }

    @Test
    fun `keeps recording after a failed account update`() = runTest {
        // given
        account.supplyUserDetails(details = AccountDetails())
        accountService.setUpdateResult(result = Result.failure(IllegalStateException("offline")))
        startTracking()
        accountService.setUpdateResult(result = Result.success(Unit))

        // when
        steps.value = OnboardingStep.Consent

        // then
        assertThat(recordedSteps())
            .containsExactly(
                RecordedOnboardingStep.WELCOME,
                RecordedOnboardingStep.CONSENT,
            )
            .inOrder()
    }

    private fun TestScope.startTracking() {
        backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) {
            tracker.track(steps = steps)
        }
    }

    private fun recordedSteps() = accountService.updates.map {
        it.modifiedDetails[MostRecentOnboardingStepKey::class]
    }

    private companion object {
        val RECORDED_STEPS = mapOf(
            OnboardingStep.Welcome to RecordedOnboardingStep.WELCOME,
            OnboardingStep.Eligibility to RecordedOnboardingStep.ELIGIBILITY,
            OnboardingStep.StudyOverview to RecordedOnboardingStep.DISCLAIMER_1,
            OnboardingStep.Login to RecordedOnboardingStep.LOGIN,
            OnboardingStep.TrialComponent to RecordedOnboardingStep.DISCLAIMER_2,
            OnboardingStep.DataCollection to RecordedOnboardingStep.DISCLAIMER_3,
            OnboardingStep.RisksAndBenefits to RecordedOnboardingStep.DISCLAIMER_4,
            OnboardingStep.Comprehension to RecordedOnboardingStep.COMPREHENSION,
            OnboardingStep.Consent to RecordedOnboardingStep.CONSENT,
            OnboardingStep.HealthAccess to RecordedOnboardingStep.HEALTH_ACCESS,
            OnboardingStep.Notifications to RecordedOnboardingStep.NOTIFICATIONS,
            OnboardingStep.Demographics to RecordedOnboardingStep.DEMOGRAPHICS,
            OnboardingStep.FinalEnrollment to RecordedOnboardingStep.FINAL_STEP,
        )
    }
}
