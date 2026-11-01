//
// This source file is part of the My Heart Counts Android open-source project
//
// SPDX-FileCopyrightText: 2026 Stanford University and the project authors (see CONTRIBUTORS.md)
//
// SPDX-License-Identifier: MIT

package edu.stanford.myheartcounts.onboarding

import edu.stanford.myheartcounts.account.MostRecentOnboardingStepKey
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.distinctUntilChanged
import org.grovealliance.account.Account
import org.grovealliance.account.AccountDetails
import org.grovealliance.account.AccountModifications
import org.grovealliance.account.AccountService
import org.grovealliance.account.observeIsSignedIn
import org.grovealliance.core.logging.groveLogger
import edu.stanford.myheartcounts.model.OnboardingStep as RecordedOnboardingStep

/**
 * Records on the participant's account how far they have come through onboarding.
 */
interface OnboardingProgressTracker {

    /**
     * Records the latest of [steps] as the most recent onboarding step the participant reached, until
     * cancelled.
     *
     * A step is recorded once it is current while somebody is signed in, so a participant who signs
     * in on a step has that step recorded then. Steps without a recorded identifier of their own,
     * such as the pages shown after a failed eligibility check or the reactivation of a withdrawn
     * account, are not recorded. A failed update is logged and not retried.
     */
    suspend fun track(steps: Flow<OnboardingStep>)
}

/**
 * [OnboardingProgressTracker] that stores the step in the account's `mostRecentOnboardingStep`.
 */
class OnboardingProgressTrackerImpl(
    private val account: Account,
    private val accountService: AccountService,
) : OnboardingProgressTracker {

    private val logger by groveLogger()

    override suspend fun track(steps: Flow<OnboardingStep>) {
        combine(
            steps.distinctUntilChanged(),
            account.observeIsSignedIn(),
        ) { step, isSignedIn -> step to isSignedIn }
            .distinctUntilChanged()
            .collect { (step, isSignedIn) ->
                val recordedStep = step.recordedStep()
                if (isSignedIn && recordedStep != null) {
                    record(step = recordedStep)
                        .onFailure { logger.e(it) { "Recording onboarding step $step failed." } }
                }
            }
    }

    private suspend fun record(step: RecordedOnboardingStep): Result<Unit> {
        val modifiedDetails = AccountDetails()
        modifiedDetails[MostRecentOnboardingStepKey::class] = step
        return AccountModifications(modifiedDetails = modifiedDetails)
            .mapCatching { accountService.updateAccountDetails(it).getOrThrow() }
    }

    @Suppress("detekt:CyclomaticComplexMethod")
    private fun OnboardingStep.recordedStep(): RecordedOnboardingStep? = when (this) {
        OnboardingStep.Welcome -> RecordedOnboardingStep.WELCOME
        OnboardingStep.Eligibility -> RecordedOnboardingStep.ELIGIBILITY
        OnboardingStep.Ineligible, OnboardingStep.CountryUnavailable, OnboardingStep.ReactivateAccount -> null
        OnboardingStep.StudyOverview -> RecordedOnboardingStep.DISCLAIMER_1
        OnboardingStep.Login -> RecordedOnboardingStep.LOGIN
        OnboardingStep.TrialComponent -> RecordedOnboardingStep.DISCLAIMER_2
        OnboardingStep.DataCollection -> RecordedOnboardingStep.DISCLAIMER_3
        OnboardingStep.RisksAndBenefits -> RecordedOnboardingStep.DISCLAIMER_4
        OnboardingStep.Comprehension -> RecordedOnboardingStep.COMPREHENSION
        OnboardingStep.Consent -> RecordedOnboardingStep.CONSENT
        OnboardingStep.HealthAccess -> RecordedOnboardingStep.HEALTH_ACCESS
        OnboardingStep.Notifications -> RecordedOnboardingStep.NOTIFICATIONS
        OnboardingStep.Demographics -> RecordedOnboardingStep.DEMOGRAPHICS
        OnboardingStep.FinalEnrollment -> RecordedOnboardingStep.FINAL_STEP
    }
}
