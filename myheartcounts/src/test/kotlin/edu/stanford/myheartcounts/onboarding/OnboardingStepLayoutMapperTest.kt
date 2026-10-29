//
// This source file is part of the My Heart Counts Android open-source project
//
// SPDX-FileCopyrightText: 2026 Stanford University and the project authors (see CONTRIBUTORS.md)
//
// SPDX-License-Identifier: MIT
//

package edu.stanford.myheartcounts.onboarding

import com.google.common.truth.Truth.assertThat
import edu.stanford.myheartcounts.MHCStrings
import edu.stanford.myheartcounts.onboarding.comprehension.ConsentSurveyLayoutMapperImpl
import edu.stanford.myheartcounts.onboarding.eligibility.EligibilityLayoutMapperImpl
import edu.stanford.myheartcounts.standard.consent.MHCConsentDocumentProvider
import edu.stanford.myheartcounts.ui.FootnoteLink
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.runTest
import org.grovealliance.consent.ConsentFormState
import org.grovealliance.consent.ConsentResponses
import org.grovealliance.health.HealthConnectAvailability
import org.grovealliance.onboarding.OnboardingInformation
import org.grovealliance.onboarding.OnboardingLayout
import org.grovealliance.ui.ActionSource
import org.grovealliance.ui.StringResource
import org.junit.Test

class OnboardingStepLayoutMapperTest {

    private val mapper = OnboardingStepLayoutMapperImpl(
        eligibilityLayoutMapper = EligibilityLayoutMapperImpl(),
        consentSurveyLayoutMapper = ConsentSurveyLayoutMapperImpl(),
    )
    private val pushedActions = mutableListOf<OnboardingAction>()
    private val advancedSteps = mutableListOf<OnboardingStep>()
    private val actionSink = ActionSource<OnboardingAction> { pushedActions.add(it) }.sink<OnboardingAction>()

    @Test
    fun `it should offer installing Health Connect while it needs an update`() {
        // when
        val layout = healthAccessLayout(availability = HealthConnectAvailability.UpdateRequired)

        // then
        assertThat(layout.header?.subtitle).isEqualTo(StringResource(MHCStrings.onboarding_health_access_install_subtitle))
        assertThat(layout.footer?.primaryButton?.title).isEqualTo(StringResource(MHCStrings.onboarding_health_access_install))
        assertThat(layout.footer?.secondaryButton?.title).isEqualTo(StringResource(MHCStrings.onboarding_skip))
    }

    @Test
    fun `it should request installing Health Connect from the install button`() = runTest {
        // given
        val layout = healthAccessLayout(availability = HealthConnectAvailability.UpdateRequired)

        // when
        layout.footer?.primaryButton?.action?.invoke()

        // then
        assertThat(pushedActions).containsExactly(OnboardingAction.HealthConnectInstallRequested)
        assertThat(advancedSteps).isEmpty()
    }

    @Test
    fun `it should skip the step from the skip button while Health Connect needs an update`() = runTest {
        // given
        val layout = healthAccessLayout(availability = HealthConnectAvailability.UpdateRequired)

        // when
        layout.footer?.secondaryButton?.action?.invoke()

        // then
        assertThat(pushedActions).containsExactly(OnboardingAction.Skipped)
    }

    @Test
    fun `it should continue with the step while Health Connect is available`() = runTest {
        // given
        val layout = healthAccessLayout(availability = HealthConnectAvailability.Available)

        // when
        layout.footer?.primaryButton?.action?.invoke()

        // then
        assertThat(layout.header?.subtitle).isEqualTo(StringResource(MHCStrings.onboarding_health_access_subtitle))
        assertThat(layout.footer?.primaryButton?.title).isEqualTo(StringResource(MHCStrings.onboarding_continue))
        assertThat(advancedSteps).containsExactly(OnboardingStep.HealthAccess)
    }

    @Test
    fun `it should continue with the step when Health Connect is unavailable`() = runTest {
        // given
        val layout = healthAccessLayout(availability = HealthConnectAvailability.Unavailable)

        // when
        layout.footer?.primaryButton?.action?.invoke()

        // then
        assertThat(advancedSteps).containsExactly(OnboardingStep.HealthAccess)
        assertThat(pushedActions).isEmpty()
    }

    @Test
    fun `it should offer learning more while Health Connect is available`() = runTest {
        // given
        val layout = healthAccessLayout(availability = HealthConnectAvailability.Available)

        // when
        layout.footer?.secondaryButton?.action?.invoke()

        // then
        assertThat(pushedActions).containsExactly(
            OnboardingAction.ShowLearnMore(
                title = StringResource(MHCStrings.onboarding_health_access_title),
                content = StringResource(MHCStrings.onboarding_health_access_learn_more),
            ),
        )
    }

    @Test
    fun `it should offer reactivating or declining on the reactivation step`() {
        // when
        val layout = layout(step = OnboardingStep.ReactivateAccount)

        // then
        assertThat(layout.header?.title).isEqualTo(StringResource(MHCStrings.onboarding_reactivate_title))
        assertThat(layout.footer?.primaryButton?.title).isEqualTo(StringResource(MHCStrings.onboarding_reactivate_confirm))
        assertThat(layout.footer?.secondaryButton?.title).isEqualTo(StringResource(MHCStrings.onboarding_reactivate_decline))
    }

    @Test
    fun `it should continue with the step when reactivating`() = runTest {
        // given
        val layout = layout(step = OnboardingStep.ReactivateAccount)

        // when
        layout.footer?.primaryButton?.action?.invoke()

        // then
        assertThat(advancedSteps).containsExactly(OnboardingStep.ReactivateAccount)
        assertThat(pushedActions).isEmpty()
    }

    @Test
    fun `it should report declining the reactivation`() = runTest {
        // given
        val layout = layout(step = OnboardingStep.ReactivateAccount)

        // when
        layout.footer?.secondaryButton?.action?.invoke()

        // then
        assertThat(pushedActions).containsExactly(OnboardingAction.ReactivationDeclined)
        assertThat(advancedSteps).isEmpty()
    }

    @Test
    fun `it should mention the trial on the final step when the participant opted in`() {
        // when
        val layout = layout(
            step = OnboardingStep.FinalEnrollment,
            answers = answers(trialOption = MHCConsentDocumentProvider.ID_TRIAL_YES),
        )

        // then
        assertThat(layout.areaDescriptions()).contains(StringResource(MHCStrings.onboarding_final_enrollment_area_trial))
    }

    @Test
    fun `it should not mention the trial on the final step when the participant declined it`() {
        // when
        val layout = layout(
            step = OnboardingStep.FinalEnrollment,
            answers = answers(trialOption = "short-term-physical-activity-trial-no"),
        )

        // then
        assertThat(layout.areaDescriptions()).doesNotContain(StringResource(MHCStrings.onboarding_final_enrollment_area_trial))
        assertThat(layout.areaDescriptions()).hasSize(4)
    }

    @Test
    fun `it should not mention the trial on the final step without a signed consent`() {
        // when
        val layout = layout(step = OnboardingStep.FinalEnrollment)

        // then
        assertThat(layout.areaDescriptions()).doesNotContain(StringResource(MHCStrings.onboarding_final_enrollment_area_trial))
    }

    @Test
    fun `it should report opening the privacy policy from the login step`() {
        // given
        val step = mapper.map(input(step = OnboardingStep.Login)) as LoginStep

        // when
        (step.footer as FootnoteLink).onClicked()

        // then
        assertThat(pushedActions).containsExactly(OnboardingAction.OpenPrivacyPolicy)
    }

    @Test
    fun `it should report consent form changes from the consent step`() {
        // given
        val step = mapper.map(input(step = OnboardingStep.Consent)) as ConsentStep
        val state = ConsentFormState(
            responses = ConsentResponses(
                toggles = emptyMap(),
                selects = emptyMap(),
                signatures = emptyMap(),
            ),
            isComplete = true,
        )

        // when
        step.onFormChanged(state)

        // then
        assertThat(pushedActions).containsExactly(OnboardingAction.ConsentFormChanged(state = state))
    }

    private fun healthAccessLayout(availability: HealthConnectAvailability): OnboardingLayout =
        mapper.map(input(availability = availability)) as OnboardingLayout

    private fun layout(
        step: OnboardingStep,
        answers: OnboardingAnswers = OnboardingAnswers.default,
    ): OnboardingLayout =
        mapper.map(
            input(
                step = step,
                answers = answers,
            ),
        ) as OnboardingLayout

    @Test
    fun `it should keep the demographics continue button disabled until the answers are complete`() {
        // when
        val step = mapper.map(
            input = input(
                step = OnboardingStep.Demographics,
                isDemographicsComplete = false,
            ),
        ) as DemographicsStep

        // then
        assertThat(step.continueButton.value.enabled).isFalse()
    }

    @Test
    fun `it should continue from the demographics once the answers are complete`() = runTest {
        // given
        val step = mapper.map(
            input = input(
                step = OnboardingStep.Demographics,
                isDemographicsComplete = true,
            ),
        ) as DemographicsStep

        // when
        step.continueButton.value.action()

        // then
        assertThat(step.continueButton.value.enabled).isTrue()
        assertThat(advancedSteps).containsExactly(OnboardingStep.Demographics)
    }

    private fun OnboardingLayout.areaDescriptions() =
        (content as OnboardingInformation).areas.map { it.description }

    private fun answers(trialOption: String) = OnboardingAnswers.default.copy(
        consentResponses = ConsentResponses(
            toggles = emptyMap(),
            selects = mapOf(MHCConsentDocumentProvider.ID_TRIAL_OPT_IN to trialOption),
            signatures = emptyMap(),
        ),
    )

    private fun input(
        availability: HealthConnectAvailability = HealthConnectAvailability.Available,
        step: OnboardingStep = OnboardingStep.HealthAccess,
        answers: OnboardingAnswers = OnboardingAnswers.default,
        isDemographicsComplete: Boolean = false,
    ) = OnboardingStepInput(
        step = step,
        scope = TestScope(),
        advance = { advancedSteps.add(it) },
        answers = MutableStateFlow(answers),
        waitlistEmail = MutableStateFlow(""),
        countries = emptyList(),
        healthConnectAvailability = availability,
        isDemographicsComplete = MutableStateFlow(isDemographicsComplete),
        actionSink = actionSink,
    )
}
