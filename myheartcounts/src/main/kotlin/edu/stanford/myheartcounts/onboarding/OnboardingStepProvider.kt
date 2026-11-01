//
// This source file is part of the My Heart Counts Android open-source project
//
// SPDX-FileCopyrightText: 2026 Stanford University and the project authors (see CONTRIBUTORS.md)
//
// SPDX-License-Identifier: MIT

@file:Suppress("detekt:CyclomaticComplexMethod")

package edu.stanford.myheartcounts.onboarding

import edu.stanford.myheartcounts.account.hasWithdrawnFromStudy
import edu.stanford.myheartcounts.featureflag.FeatureFlag
import edu.stanford.myheartcounts.featureflag.FeatureFlags
import edu.stanford.myheartcounts.model.StudyVariant
import edu.stanford.myheartcounts.notification.NotificationPermissionHandler
import edu.stanford.myheartcounts.onboarding.eligibility.EligibilityVerdict
import edu.stanford.myheartcounts.standard.consent.MHCConsentSigning
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.withTimeoutOrNull
import org.grovealliance.account.Account
import org.grovealliance.account.AccountDetails
import org.grovealliance.account.isAnonymousUser
import org.grovealliance.account.isIncomplete
import kotlin.time.Duration.Companion.seconds

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
     *
     * Leaving the account steps waits until the signed-in account's details have loaded, since
     * whether the participant withdrew earlier, and which consent version they signed, decide the
     * next step.
     */
    suspend fun getNext(step: OnboardingStep, answers: OnboardingAnswers): OnboardingStepResult

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
    private val consentSigning: MHCConsentSigning,
    private val featureFlags: FeatureFlags,
) : OnboardingStepProvider {

    // Whether leaving the account steps last skipped the consent steps, because the participant had
    // already signed the current version; going back from HealthAccess then skips them too.
    private var skippedConsent = false

    private val isSimplified: Boolean get() = featureFlags.isEnabled(FeatureFlag.SimplifyOnboarding)

    // An anonymous account exists only to join a launch waitlist, so it must not skip the login step.
    private val isSignedIn: Boolean get() = account.details.value?.isAnonymousUser == false
    override fun getInitialStep(): OnboardingStep =
        if (isSimplified) OnboardingStep.Login else OnboardingStep.Welcome

    override suspend fun getNext(step: OnboardingStep, answers: OnboardingAnswers): OnboardingStepResult = when {
        isSimplified -> when (step) {
            OnboardingStep.FinalEnrollment -> OnboardingStepResult.Completed
            OnboardingStep.HealthAccess -> OnboardingStepResult.Step(step = OnboardingStep.FinalEnrollment)
            OnboardingStep.Login -> OnboardingStepResult.Step(step = afterAccount(next = OnboardingStep.HealthAccess))
            else -> OnboardingStepResult.Step(step = OnboardingStep.HealthAccess)
        }
        else -> nextStep(step = step, answers = answers)
    }

    private suspend fun nextStep(step: OnboardingStep, answers: OnboardingAnswers): OnboardingStepResult = when (step) {
        OnboardingStep.Welcome -> OnboardingStepResult.Step(OnboardingStep.Eligibility)
        OnboardingStep.Eligibility -> OnboardingStepResult.Step(eligibilityNext(answers))
        OnboardingStep.Ineligible -> OnboardingStepResult.Step(OnboardingStep.Ineligible)
        OnboardingStep.CountryUnavailable -> OnboardingStepResult.Step(OnboardingStep.CountryUnavailable)
        OnboardingStep.StudyOverview -> OnboardingStepResult.Step(afterStudyOverview())
        OnboardingStep.Login -> OnboardingStepResult.Step(afterAccount(next = consentOrHealthAccess()))
        OnboardingStep.ReactivateAccount -> OnboardingStepResult.Step(consentOrHealthAccess())
        OnboardingStep.TrialComponent -> OnboardingStepResult.Step(OnboardingStep.DataCollection)
        OnboardingStep.DataCollection -> OnboardingStepResult.Step(OnboardingStep.RisksAndBenefits)
        OnboardingStep.RisksAndBenefits -> OnboardingStepResult.Step(OnboardingStep.Comprehension)
        OnboardingStep.Comprehension -> OnboardingStepResult.Step(OnboardingStep.Consent)
        OnboardingStep.Consent -> OnboardingStepResult.Step(OnboardingStep.HealthAccess)
        OnboardingStep.HealthAccess -> OnboardingStepResult.Step(afterHealthAccess())
        OnboardingStep.Notifications -> OnboardingStepResult.Step(OnboardingStep.Demographics)
        OnboardingStep.Demographics -> OnboardingStepResult.Step(OnboardingStep.FinalEnrollment)
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
     * With [FeatureFlag.UKStudyTesting] enabled, the United Kingdom counts as supported.
     */
    private fun evaluate(answers: OnboardingAnswers): EligibilityVerdict {
        val criteriaPass = answers.isOfAge && answers.understandsEnglish == true && answers.usesSharedAccount == false
        val country = answers.country
        val isUKStudyTesting = country?.code == StudyVariant.Imperial.regionCode &&
            featureFlags.isEnabled(FeatureFlag.UKStudyTesting)
        return when {
            !criteriaPass -> EligibilityVerdict.Ineligible
            country == null -> EligibilityVerdict.Ineligible
            country.isEnabled || isUKStudyTesting -> EligibilityVerdict.Eligible
            country.isComingSoon -> EligibilityVerdict.CountryComingSoon(country)
            else -> EligibilityVerdict.CountryUnsupported(country)
        }
    }

    private suspend fun afterStudyOverview(): OnboardingStep =
        if (isSignedIn) afterAccount(next = consentOrHealthAccess()) else OnboardingStep.Login

    /**
     * The first consent step, or HealthAccess when the participant already signed the current
     * consent version.
     */
    private suspend fun consentOrHealthAccess(): OnboardingStep {
        awaitAccountDetails()
        skippedConsent = !consentSigning.isSigningRequired()
        return if (skippedConsent) OnboardingStep.HealthAccess else OnboardingStep.TrialComponent
    }

    /**
     * [next], unless the signed-in participant withdrew from the study earlier and has to decide
     * whether to reactivate their account first.
     */
    private suspend fun afterAccount(next: OnboardingStep): OnboardingStep =
        if (awaitAccountDetails()?.hasWithdrawnFromStudy == true) OnboardingStep.ReactivateAccount else next

    /**
     * The signed-in account's details once they have loaded, or whatever is known after
     * [ACCOUNT_DETAILS_TIMEOUT].
     */
    private suspend fun awaitAccountDetails(): AccountDetails? =
        withTimeoutOrNull(ACCOUNT_DETAILS_TIMEOUT) {
            account.details.first { it != null && !it.isAnonymousUser && !it.isIncomplete }
        } ?: account.details.value

    private fun afterHealthAccess(): OnboardingStep =
        if (notificationPermissionHandler.isGranted()) OnboardingStep.Demographics else OnboardingStep.Notifications

    override fun getPrevious(step: OnboardingStep): OnboardingStepResult = when {
        isSimplified -> when (step) {
            OnboardingStep.Login -> OnboardingStepResult.Dismissed
            OnboardingStep.FinalEnrollment -> OnboardingStepResult.Step(step = OnboardingStep.HealthAccess)
            else -> OnboardingStepResult.Step(step = OnboardingStep.Login)
        }
        else -> previousStep(step = step)
    }

    private fun previousStep(step: OnboardingStep): OnboardingStepResult = when (step) {
        OnboardingStep.Welcome -> OnboardingStepResult.Dismissed
        OnboardingStep.Eligibility -> OnboardingStepResult.Step(OnboardingStep.Welcome)
        OnboardingStep.Ineligible -> OnboardingStepResult.Step(OnboardingStep.Eligibility)
        OnboardingStep.CountryUnavailable -> OnboardingStepResult.Step(OnboardingStep.Eligibility)
        OnboardingStep.StudyOverview -> OnboardingStepResult.Step(OnboardingStep.Eligibility)
        OnboardingStep.Login -> OnboardingStepResult.Step(OnboardingStep.StudyOverview)
        OnboardingStep.ReactivateAccount -> OnboardingStepResult.Step(OnboardingStep.StudyOverview)
        OnboardingStep.TrialComponent -> OnboardingStepResult.Step(beforeTrialComponent())
        OnboardingStep.DataCollection -> OnboardingStepResult.Step(OnboardingStep.TrialComponent)
        OnboardingStep.RisksAndBenefits -> OnboardingStepResult.Step(OnboardingStep.DataCollection)
        OnboardingStep.Comprehension -> OnboardingStepResult.Step(OnboardingStep.RisksAndBenefits)
        OnboardingStep.Consent -> OnboardingStepResult.Step(OnboardingStep.Comprehension)
        OnboardingStep.HealthAccess -> OnboardingStepResult.Step(
            if (skippedConsent) OnboardingStep.StudyOverview else OnboardingStep.Consent,
        )
        OnboardingStep.Notifications -> OnboardingStepResult.Step(OnboardingStep.HealthAccess)
        OnboardingStep.Demographics -> OnboardingStepResult.Step(beforeFinalEnrollment())
        OnboardingStep.FinalEnrollment -> OnboardingStepResult.Step(OnboardingStep.Demographics)
    }

    private fun beforeTrialComponent(): OnboardingStep =
        if (isSignedIn) OnboardingStep.StudyOverview else OnboardingStep.Login

    private fun beforeFinalEnrollment(): OnboardingStep =
        if (notificationPermissionHandler.isGranted()) OnboardingStep.HealthAccess else OnboardingStep.Notifications

    private companion object {
        val ACCOUNT_DETAILS_TIMEOUT = 10.seconds
    }
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
    ReactivateAccount,
    TrialComponent,
    DataCollection,
    RisksAndBenefits,
    Comprehension,
    Consent,
    HealthAccess,
    Notifications,
    Demographics,
    FinalEnrollment,
}
