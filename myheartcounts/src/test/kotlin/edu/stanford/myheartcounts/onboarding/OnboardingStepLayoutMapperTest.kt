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
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.runTest
import org.grovealliance.health.HealthConnectAvailability
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

    private fun healthAccessLayout(availability: HealthConnectAvailability): OnboardingLayout =
        mapper.map(input(availability = availability)) as OnboardingLayout

    private fun input(availability: HealthConnectAvailability) = OnboardingStepInput(
        step = OnboardingStep.HealthAccess,
        scope = TestScope(),
        advance = { advancedSteps.add(it) },
        answers = MutableStateFlow(OnboardingAnswers.default),
        waitlistEmail = MutableStateFlow(""),
        countries = emptyList(),
        healthConnectAvailability = availability,
        actionSink = actionSink,
    )
}
