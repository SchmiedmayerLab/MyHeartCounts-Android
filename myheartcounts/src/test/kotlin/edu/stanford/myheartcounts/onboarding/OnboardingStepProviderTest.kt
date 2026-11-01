//
// This source file is part of the My Heart Counts Android open-source project
//
// SPDX-FileCopyrightText: 2026 Stanford University and the project authors (see CONTRIBUTORS.md)
//
// SPDX-License-Identifier: MIT

package edu.stanford.myheartcounts.onboarding

import com.google.common.truth.Truth.assertThat
import edu.stanford.myheartcounts.account.HasWithdrawnFromStudyKey
import edu.stanford.myheartcounts.featureflag.FakeFeatureFlags
import edu.stanford.myheartcounts.featureflag.FeatureFlag
import edu.stanford.myheartcounts.model.Country
import edu.stanford.myheartcounts.notification.FakeNotificationPermissionHandler
import edu.stanford.myheartcounts.standard.consent.FakeConsentSigning
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.async
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import org.grovealliance.account.AccountDetails
import org.grovealliance.account.FakeAccount
import org.grovealliance.account.isAnonymousUser
import org.grovealliance.account.isIncomplete
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class OnboardingStepProviderTest {

    private val account = FakeAccount()
    private val consentSigning = FakeConsentSigning()
    private val featureFlags = FakeFeatureFlags()
    private val notificationPermission = FakeNotificationPermissionHandler()
    private val provider = OnboardingStepProviderImpl(
        account = account,
        notificationPermissionHandler = notificationPermission,
        consentSigning = consentSigning,
        featureFlags = featureFlags,
    )

    @Test
    fun `demographics comes before final enrollment with notifications requested or already granted`() = runTest {
        // when / then
        assertThat(provider.getNext(step = OnboardingStep.Notifications, answers = OnboardingAnswers.default))
            .isEqualTo(OnboardingStepResult.Step(step = OnboardingStep.Demographics))
        notificationPermission.setGranted(granted = true)
        assertThat(provider.getNext(step = OnboardingStep.HealthAccess, answers = OnboardingAnswers.default))
            .isEqualTo(OnboardingStepResult.Step(step = OnboardingStep.Demographics))
        assertThat(provider.getNext(step = OnboardingStep.Demographics, answers = OnboardingAnswers.default))
            .isEqualTo(OnboardingStepResult.Step(step = OnboardingStep.FinalEnrollment))
        assertThat(provider.getPrevious(step = OnboardingStep.FinalEnrollment))
            .isEqualTo(OnboardingStepResult.Step(step = OnboardingStep.Demographics))
        assertThat(provider.getPrevious(step = OnboardingStep.Demographics))
            .isEqualTo(OnboardingStepResult.Step(step = OnboardingStep.HealthAccess))
    }

    @Test
    fun `asks a withdrawn participant to reactivate their account after login`() = runTest {
        // given
        account.supplyUserDetails(details = details(hasWithdrawn = true))

        // when
        val result = provider.getNext(step = OnboardingStep.Login, answers = OnboardingAnswers.default)

        // then
        assertThat(result).isEqualTo(OnboardingStepResult.Step(step = OnboardingStep.ReactivateAccount))
    }

    @Test
    fun `continues with the disclaimers after login when the participant has not withdrawn`() = runTest {
        // given
        account.supplyUserDetails(details = details(hasWithdrawn = false))

        // when
        val result = provider.getNext(step = OnboardingStep.Login, answers = OnboardingAnswers.default)

        // then
        assertThat(result).isEqualTo(OnboardingStepResult.Step(step = OnboardingStep.TrialComponent))
    }

    @Test
    fun `waits for the account details before leaving the login step`() = runTest {
        // given
        val result = async { provider.getNext(step = OnboardingStep.Login, answers = OnboardingAnswers.default) }
        runCurrent()

        // when
        account.supplyUserDetails(details = details(hasWithdrawn = true))

        // then
        assertThat(result.await()).isEqualTo(OnboardingStepResult.Step(step = OnboardingStep.ReactivateAccount))
    }

    @Test
    fun `waits for incomplete account details to load before leaving the login step`() = runTest {
        // given
        account.supplyUserDetails(details = details(hasWithdrawn = false).apply { isIncomplete = true })
        val result = async { provider.getNext(step = OnboardingStep.Login, answers = OnboardingAnswers.default) }
        runCurrent()

        // when
        account.supplyUserDetails(details = details(hasWithdrawn = true))

        // then
        assertThat(result.await()).isEqualTo(OnboardingStepResult.Step(step = OnboardingStep.ReactivateAccount))
    }

    @Test
    fun `continues with the disclaimers when the account details do not load in time`() = runTest {
        // when
        val result = provider.getNext(step = OnboardingStep.Login, answers = OnboardingAnswers.default)

        // then
        assertThat(result).isEqualTo(OnboardingStepResult.Step(step = OnboardingStep.TrialComponent))
    }

    @Test
    fun `asks a signed-in withdrawn participant to reactivate their account after the study overview`() = runTest {
        // given
        account.supplyUserDetails(details = details(hasWithdrawn = true))

        // when
        val result = provider.getNext(step = OnboardingStep.StudyOverview, answers = OnboardingAnswers.default)

        // then
        assertThat(result).isEqualTo(OnboardingStepResult.Step(step = OnboardingStep.ReactivateAccount))
    }

    @Test
    fun `continues with the disclaimers after reactivation`() = runTest {
        // when
        val result = provider.getNext(step = OnboardingStep.ReactivateAccount, answers = OnboardingAnswers.default)

        // then
        assertThat(result).isEqualTo(OnboardingStepResult.Step(step = OnboardingStep.TrialComponent))
    }

    @Test
    fun `skips the consent steps after login when the current version is already signed`() = runTest {
        // given
        account.supplyUserDetails(details = details(hasWithdrawn = false))
        consentSigning.setSigningRequired(required = false)

        // when
        val result = provider.getNext(step = OnboardingStep.Login, answers = OnboardingAnswers.default)

        // then
        assertThat(result).isEqualTo(OnboardingStepResult.Step(step = OnboardingStep.HealthAccess))
    }

    @Test
    fun `skips the consent steps after the study overview when the current version is already signed`() = runTest {
        // given
        account.supplyUserDetails(details = details(hasWithdrawn = false))
        consentSigning.setSigningRequired(required = false)

        // when
        val result = provider.getNext(step = OnboardingStep.StudyOverview, answers = OnboardingAnswers.default)

        // then
        assertThat(result).isEqualTo(OnboardingStepResult.Step(step = OnboardingStep.HealthAccess))
    }

    @Test
    fun `skips the consent steps after reactivation when the current version is already signed`() = runTest {
        // given
        account.supplyUserDetails(details = details(hasWithdrawn = false))
        consentSigning.setSigningRequired(required = false)

        // when
        val result = provider.getNext(step = OnboardingStep.ReactivateAccount, answers = OnboardingAnswers.default)

        // then
        assertThat(result).isEqualTo(OnboardingStepResult.Step(step = OnboardingStep.HealthAccess))
    }

    @Test
    fun `asks to reactivate before skipping the consent steps`() = runTest {
        // given
        account.supplyUserDetails(details = details(hasWithdrawn = true))
        consentSigning.setSigningRequired(required = false)

        // when
        val result = provider.getNext(step = OnboardingStep.Login, answers = OnboardingAnswers.default)

        // then
        assertThat(result).isEqualTo(OnboardingStepResult.Step(step = OnboardingStep.ReactivateAccount))
    }

    @Test
    fun `returns past the skipped consent steps from health access`() = runTest {
        // given
        account.supplyUserDetails(details = details(hasWithdrawn = false))
        consentSigning.setSigningRequired(required = false)
        provider.getNext(step = OnboardingStep.Login, answers = OnboardingAnswers.default)

        // when
        val result = provider.getPrevious(step = OnboardingStep.HealthAccess)

        // then
        assertThat(result).isEqualTo(OnboardingStepResult.Step(step = OnboardingStep.StudyOverview))
    }

    @Test
    fun `returns to the consent from health access when it was signed in this flow`() = runTest {
        // given
        account.supplyUserDetails(details = details(hasWithdrawn = false))
        provider.getNext(step = OnboardingStep.Login, answers = OnboardingAnswers.default)

        // when
        val result = provider.getPrevious(step = OnboardingStep.HealthAccess)

        // then
        assertThat(result).isEqualTo(OnboardingStepResult.Step(step = OnboardingStep.Consent))
    }

    @Test
    fun `starts at the welcome step`() {
        // when
        val step = provider.getInitialStep()

        // then
        assertThat(step).isEqualTo(OnboardingStep.Welcome)
    }

    @Test
    fun `starts at the login step when onboarding is simplified`() {
        // given
        featureFlags.setEnabled(
            flag = FeatureFlag.SimplifyOnboarding,
            enabled = true,
        )

        // when
        val step = provider.getInitialStep()

        // then
        assertThat(step).isEqualTo(OnboardingStep.Login)
    }

    @Test
    fun `goes from login straight to health access when onboarding is simplified`() = runTest {
        // given
        featureFlags.setEnabled(
            flag = FeatureFlag.SimplifyOnboarding,
            enabled = true,
        )
        account.supplyUserDetails(details = details(hasWithdrawn = false))

        // when
        val result = provider.getNext(step = OnboardingStep.Login, answers = OnboardingAnswers.default)

        // then
        assertThat(result).isEqualTo(OnboardingStepResult.Step(step = OnboardingStep.HealthAccess))
    }

    @Test
    fun `offers the waitlist to an eligible participant in the United Kingdom`() = runTest {
        // when
        val result = provider.getNext(step = OnboardingStep.Eligibility, answers = eligibleAnswers(countryCode = "GB"))

        // then
        assertThat(result).isEqualTo(OnboardingStepResult.Step(step = OnboardingStep.CountryUnavailable))
    }

    @Test
    fun `admits an eligible participant in the United Kingdom while UK study testing is enabled`() = runTest {
        // given
        featureFlags.setEnabled(
            flag = FeatureFlag.UKStudyTesting,
            enabled = true,
        )

        // when
        val result = provider.getNext(step = OnboardingStep.Eligibility, answers = eligibleAnswers(countryCode = "GB"))

        // then
        assertThat(result).isEqualTo(OnboardingStepResult.Step(step = OnboardingStep.StudyOverview))
    }

    @Test
    fun `keeps other coming soon countries on the waitlist while UK study testing is enabled`() = runTest {
        // given
        featureFlags.setEnabled(
            flag = FeatureFlag.UKStudyTesting,
            enabled = true,
        )

        // when
        val result = provider.getNext(step = OnboardingStep.Eligibility, answers = eligibleAnswers(countryCode = "DE"))

        // then
        assertThat(result).isEqualTo(OnboardingStepResult.Step(step = OnboardingStep.CountryUnavailable))
    }

    @Test
    fun `returns to the study overview from the reactivation step`() {
        // when
        val result = provider.getPrevious(step = OnboardingStep.ReactivateAccount)

        // then
        assertThat(result).isEqualTo(OnboardingStepResult.Step(step = OnboardingStep.StudyOverview))
    }

    private fun eligibleAnswers(countryCode: String) = OnboardingAnswers.default.copy(
        isOfAge = true,
        country = Country(
            code = countryCode,
            name = countryCode,
        ),
        understandsEnglish = true,
        usesSharedAccount = false,
    )

    private fun details(hasWithdrawn: Boolean) = AccountDetails().apply {
        isAnonymousUser = false
        this[HasWithdrawnFromStudyKey::class] = hasWithdrawn
    }
}
