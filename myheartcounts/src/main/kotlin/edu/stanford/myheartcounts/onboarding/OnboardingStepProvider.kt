//
// This source file is part of the My Heart Counts open-source project
//
// SPDX-FileCopyrightText: 2026 Stanford University and the project authors (see CONTRIBUTORS.md)
//
// SPDX-License-Identifier: MIT

@file:Suppress("detekt:CyclomaticComplexMethod")

package edu.stanford.myheartcounts.onboarding

import edu.stanford.myheartcounts.notification.NotificationPermissionHandler
import edu.stanford.myheartcounts.onboarding.eligibility.EligibilityVerdict
import edu.stanford.spezi.account.Account

/**
 * The onboarding step graph: which step comes first, and which step follows or precedes a given one.
 * Branching (e.g. eligibility outcomes, skipping login when already signed in) is resolved here so the
 * rest of the flow stays unaware of step ordering.
 */
interface OnboardingStepProvider {

    /**
     * The step shown when the flow starts.
     */
    fun getInitialStep(): OnboardingStep

    /**
     * The step that follows [step] given the current [answers], or a terminal [OnboardingStepResult].
     */
    fun getNext(step: OnboardingStep, answers: OnboardingAnswers): OnboardingStepResult

    /**
     * The step preceding [step], or a terminal [OnboardingStepResult] (e.g. [OnboardingStepResult.Dismissed]
     * when backing out of the first step).
     */
    fun getPrevious(step: OnboardingStep): OnboardingStepResult
}

/**
 * Default [OnboardingStepProvider] implementation.
 */
class OnboardingStepProviderImpl(
    private val account: Account,
    private val notificationPermissionHandler: NotificationPermissionHandler,
) : OnboardingStepProvider {
    private val isSignedIn: Boolean get() = account.isSignedIn
    override fun getInitialStep(): OnboardingStep = OnboardingStep.Welcome

    override fun getNext(step: OnboardingStep, answers: OnboardingAnswers): OnboardingStepResult = when (step) {
        OnboardingStep.Welcome -> OnboardingStepResult.Step(OnboardingStep.Eligibility)
        OnboardingStep.Eligibility -> OnboardingStepResult.Step(eligibilityNext(answers))
        OnboardingStep.Ineligible -> OnboardingStepResult.Step(OnboardingStep.Ineligible)
        OnboardingStep.CountryUnavailable -> OnboardingStepResult.Step(OnboardingStep.CountryUnavailable)
        OnboardingStep.StudyOverview -> OnboardingStepResult.Step(afterStudyOverview())
        OnboardingStep.Login -> OnboardingStepResult.Step(OnboardingStep.TrialComponent)
        OnboardingStep.TrialComponent -> OnboardingStepResult.Step(OnboardingStep.DataCollection)
        OnboardingStep.DataCollection -> OnboardingStepResult.Step(OnboardingStep.RisksAndBenefits)
        OnboardingStep.RisksAndBenefits -> OnboardingStepResult.Step(OnboardingStep.Comprehension)
        OnboardingStep.Comprehension -> OnboardingStepResult.Step(OnboardingStep.Consent)
        OnboardingStep.Consent -> OnboardingStepResult.Step(OnboardingStep.HealthAccess)
        OnboardingStep.HealthAccess -> OnboardingStepResult.Step(afterHealthAccess())
        OnboardingStep.Notifications -> OnboardingStepResult.Step(OnboardingStep.FinalEnrollment)
        OnboardingStep.FinalEnrollment -> OnboardingStepResult.Completed
    }

    private fun eligibilityNext(answers: OnboardingAnswers): OnboardingStep = when (evaluate(answers)) {
        EligibilityVerdict.Eligible -> OnboardingStep.StudyOverview
        is EligibilityVerdict.CountryComingSoon,
        is EligibilityVerdict.CountryUnsupported,
        -> OnboardingStep.CountryUnavailable
        EligibilityVerdict.Ineligible -> OnboardingStep.Ineligible
    }

    /**
     * Evaluates the eligibility answers into an [EligibilityVerdict]: the participant must be of age,
     * able to understand English, not using a shared account, and in a supported country. When only
     * the country is unavailable, the verdict reflects whether it is coming soon or unsupported.
     */
    private fun evaluate(answers: OnboardingAnswers): EligibilityVerdict {
        val criteriaPass = answers.isOfAge && answers.understandsEnglish == true && answers.usesSharedAccount == false
        val country = answers.country
        return when {
            !criteriaPass -> EligibilityVerdict.Ineligible
            country == null -> EligibilityVerdict.Ineligible
            country.isEnabled -> EligibilityVerdict.Eligible
            country.isComingSoon -> EligibilityVerdict.CountryComingSoon(country)
            else -> EligibilityVerdict.CountryUnsupported(country)
        }
    }

    private fun afterStudyOverview(): OnboardingStep =
        if (isSignedIn) OnboardingStep.TrialComponent else OnboardingStep.Login

    private fun afterHealthAccess(): OnboardingStep =
        if (notificationPermissionHandler.isGranted()) OnboardingStep.FinalEnrollment else OnboardingStep.Notifications

    override fun getPrevious(step: OnboardingStep): OnboardingStepResult = when (step) {
        OnboardingStep.Welcome -> OnboardingStepResult.Dismissed
        OnboardingStep.Eligibility -> OnboardingStepResult.Step(OnboardingStep.Welcome)
        OnboardingStep.Ineligible -> OnboardingStepResult.Step(OnboardingStep.Eligibility)
        OnboardingStep.CountryUnavailable -> OnboardingStepResult.Step(OnboardingStep.Eligibility)
        OnboardingStep.StudyOverview -> OnboardingStepResult.Step(OnboardingStep.Eligibility)
        OnboardingStep.Login -> OnboardingStepResult.Step(OnboardingStep.StudyOverview)
        OnboardingStep.TrialComponent -> OnboardingStepResult.Step(beforeTrialComponent())
        OnboardingStep.DataCollection -> OnboardingStepResult.Step(OnboardingStep.TrialComponent)
        OnboardingStep.RisksAndBenefits -> OnboardingStepResult.Step(OnboardingStep.DataCollection)
        OnboardingStep.Comprehension -> OnboardingStepResult.Step(OnboardingStep.RisksAndBenefits)
        OnboardingStep.Consent -> OnboardingStepResult.Step(OnboardingStep.Comprehension)
        OnboardingStep.HealthAccess -> OnboardingStepResult.Step(OnboardingStep.Consent)
        OnboardingStep.Notifications -> OnboardingStepResult.Step(OnboardingStep.HealthAccess)
        OnboardingStep.FinalEnrollment -> OnboardingStepResult.Step(beforeFinalEnrollment())
    }

    private fun beforeTrialComponent(): OnboardingStep =
        if (isSignedIn) OnboardingStep.StudyOverview else OnboardingStep.Login

    private fun beforeFinalEnrollment(): OnboardingStep =
        if (notificationPermissionHandler.isGranted()) OnboardingStep.HealthAccess else OnboardingStep.Notifications
}

/**
 * The result of a navigation query against the step graph.
 */
sealed interface OnboardingStepResult {

    /**
     * The flow was backed out of entirely.
     */
    data object Dismissed : OnboardingStepResult

    /**
     * Move to [step].
     */
    data class Step(val step: OnboardingStep) : OnboardingStepResult

    /**
     * Onboarding finished; the participant should be enrolled and shown the study.
     */
    object Completed : OnboardingStepResult
}

/**
 * The ordered steps of the onboarding flow. Not every step is reached on every run; branches are
 * decided by the [OnboardingStepProvider].
 */
enum class OnboardingStep {
    Welcome,
    Eligibility,
    Ineligible,
    CountryUnavailable,
    StudyOverview,
    Login,
    TrialComponent,
    DataCollection,
    RisksAndBenefits,
    Comprehension,
    Consent,
    HealthAccess,
    Notifications,
    FinalEnrollment,
}
