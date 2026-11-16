//
// This source file is part of the My Heart Counts Android open-source project
//
// SPDX-FileCopyrightText: 2026 Stanford University and the project authors (see CONTRIBUTORS.md)
//
// SPDX-License-Identifier: MIT

package edu.stanford.myheartcounts.account.overview

import androidx.lifecycle.viewModelScope
import com.google.common.truth.Truth.assertThat
import edu.stanford.myheartcounts.BuildInfoFixtures
import edu.stanford.myheartcounts.MHCStrings
import edu.stanford.myheartcounts.account.FakePostTrialNudges
import edu.stanford.myheartcounts.account.FakeStudyWithdrawal
import edu.stanford.myheartcounts.account.feedback.FakeFeedback
import edu.stanford.myheartcounts.account.overview.WithdrawFromStudyButton.WithdrawalState
import edu.stanford.myheartcounts.navigation.FakeNavigator
import edu.stanford.myheartcounts.navigation.MHCRoute
import edu.stanford.myheartcounts.navigation.NavigationEvent
import edu.stanford.myheartcounts.standard.MHCAppRevisionFixtures
import edu.stanford.myheartcounts.study.EnrolledStudyFixtures
import edu.stanford.myheartcounts.study.FakeEnrolledStudySource
import edu.stanford.myheartcounts.ui.ExternalLink
import edu.stanford.myheartcounts.ui.ToggleRow
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.advanceTimeBy
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.grovealliance.core.time.FakeTimeProvider
import org.grovealliance.ui.GroveAppBar
import org.grovealliance.ui.ItemDisplayRow
import org.grovealliance.ui.StringResource
import org.junit.After
import org.junit.Before
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class MHCAccountOverviewViewModelTest {

    private val dispatcher = UnconfinedTestDispatcher()
    private val navigator = FakeNavigator()
    private val studyWithdrawal = FakeStudyWithdrawal()
    private val postTrialNudges = FakePostTrialNudges()
    private val feedback = FakeFeedback()
    private val enrolledStudySource = FakeEnrolledStudySource()
    private val mapper = RecordingMapper()

    @Before
    fun setup() {
        Dispatchers.setMain(dispatcher = dispatcher)
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun `opens the demographics editor from the account overview`() {
        // given
        val viewModel = viewModel()

        // when
        viewModel.studyParticipationSection().row<ItemDisplayRow>(key = "demographics").onClick?.invoke()

        // then
        assertThat(navigator.pushedEvents).containsExactly(NavigationEvent.NavigateTo(route = MHCRoute.Demographics))
    }

    @Test
    fun `opens the participation stats from the account overview`() {
        // given
        val viewModel = viewModel()

        // when
        viewModel.studyParticipationSection().row<ItemDisplayRow>(key = "participation-stats").onClick?.invoke()

        // then
        assertThat(navigator.pushedEvents).containsExactly(NavigationEvent.NavigateTo(route = MHCRoute.ParticipationStats))
    }

    @Test
    fun `opens the study homepage from the enrolled study`() {
        // given
        val viewModel = viewModel()
        enrolledStudySource.setEnrolledStudy(enrolledStudy = EnrolledStudyFixtures.create(title = "My Heart Counts"))

        // when
        viewModel.enrolledStudyRow()?.website?.onClicked?.invoke()

        // then
        assertThat(navigator.pushedEvents).containsExactly(NavigationEvent.OpenStudyHomepage)
    }

    @Test
    fun `hides the enrolled study until it is loaded`() {
        // given
        val viewModel = viewModel()

        // when
        val row = viewModel.enrolledStudyRow()

        // then
        assertThat(row).isNull()
    }

    @Test
    fun `shows the enrolled study once enrolled`() {
        // given
        val viewModel = viewModel()

        // when
        enrolledStudySource.setEnrolledStudy(enrolledStudy = EnrolledStudyFixtures.create(title = "My Heart Counts"))

        // then
        assertThat(viewModel.enrolledStudyRow()?.title).isEqualTo("My Heart Counts")
    }

    @Test
    fun `opens the comorbidities from the account overview`() {
        // given
        val viewModel = viewModel()

        // when
        viewModel.studyParticipationSection().row<ItemDisplayRow>(key = "comorbidities").onClick?.invoke()

        // then
        assertThat(navigator.pushedEvents).containsExactly(NavigationEvent.NavigateTo(route = MHCRoute.Comorbidities))
    }

    @Test
    fun `opens the signed consent forms from the account overview`() {
        // given
        val viewModel = viewModel()

        // when
        viewModel.studyParticipationSection().row<ItemDisplayRow>(key = "signed-consent-forms").onClick?.invoke()

        // then
        assertThat(navigator.pushedEvents).containsExactly(NavigationEvent.NavigateTo(route = MHCRoute.SignedConsentForms))
    }

    @Test
    fun `opens the workout preference from the account overview`() {
        // given
        val viewModel = viewModel()

        // when
        viewModel.studyParticipationSection().row<ItemDisplayRow>(key = "workout-preference").onClick?.invoke()

        // then
        assertThat(navigator.pushedEvents).containsExactly(NavigationEvent.NavigateTo(route = MHCRoute.WorkoutPreference))
    }

    @Test
    fun `asks for confirmation before withdrawing`() {
        // given
        val viewModel = viewModel()

        // when
        viewModel.withdrawSection().onWithdrawSelected()

        // then
        assertThat(viewModel.withdrawSection().state.value).isEqualTo(WithdrawalState.Confirming)
        assertThat(studyWithdrawal.withdrawCount).isEqualTo(0)
    }

    @Test
    fun `does not withdraw when the confirmation is dismissed`() {
        // given
        val viewModel = viewModel()
        viewModel.withdrawSection().onWithdrawSelected()

        // when
        viewModel.withdrawSection().confirmation.onDismiss()

        // then
        assertThat(viewModel.withdrawSection().state.value).isEqualTo(WithdrawalState.Idle)
        assertThat(studyWithdrawal.withdrawCount).isEqualTo(0)
    }

    @Test
    fun `withdraws once confirmed`() {
        // given
        val viewModel = viewModel()
        viewModel.withdrawSection().onWithdrawSelected()

        // when
        viewModel.withdrawSection().confirm()

        // then
        assertThat(studyWithdrawal.withdrawCount).isEqualTo(1)
        assertThat(viewModel.withdrawSection().state.value).isEqualTo(WithdrawalState.Idle)
    }

    @Test
    fun `reports a failed withdrawal`() {
        // given
        studyWithdrawal.setResult(result = Result.failure(IllegalStateException("offline")))
        val viewModel = viewModel()
        viewModel.withdrawSection().onWithdrawSelected()

        // when
        viewModel.withdrawSection().confirm()

        // then
        assertThat(viewModel.withdrawSection().state.value).isEqualTo(WithdrawalState.Failed)
    }

    @Test
    fun `shows the stored post-trial nudges opt-in`() {
        // given
        val viewModel = viewModel()

        // when
        postTrialNudges.setStored(optedIn = true)

        // then
        assertThat(viewModel.postTrialNudgesSection().isOn.value).isTrue()
    }

    @Test
    fun `switches the post-trial nudges opt-in before storing it`() = runTest(context = dispatcher) {
        // given
        val viewModel = viewModel()

        // when
        viewModel.postTrialNudgesSection().onToggle(true)

        // then
        assertThat(viewModel.postTrialNudgesSection().isOn.value).isTrue()
        assertThat(postTrialNudges.writes).isEmpty()
    }

    @Test
    fun `stores the post-trial nudges opt-in after a short delay`() = runTest(context = dispatcher) {
        // given
        val viewModel = viewModel()

        // when
        viewModel.postTrialNudgesSection().onToggle(true)
        advanceUntilIdle()

        // then
        assertThat(postTrialNudges.writes).containsExactly(true)
    }

    @Test
    fun `stores only the last of quick post-trial nudges changes`() = runTest(context = dispatcher) {
        // given
        val viewModel = viewModel()

        // when
        viewModel.postTrialNudgesSection().onToggle(true)
        advanceTimeBy(delayTimeMillis = 100)
        viewModel.postTrialNudgesSection().onToggle(false)
        advanceUntilIdle()

        // then
        assertThat(postTrialNudges.writes).containsExactly(false)
    }

    @Test
    fun `reverts to the stored opt-in when storing it fails`() = runTest(context = dispatcher) {
        // given
        postTrialNudges.setStored(optedIn = true)
        postTrialNudges.setResult(result = Result.failure(IllegalStateException("offline")))
        val viewModel = viewModel()

        // when
        viewModel.postTrialNudgesSection().onToggle(false)
        advanceTimeBy(delayTimeMillis = DEBOUNCE_ELAPSED_MILLIS)

        // then
        assertThat(viewModel.postTrialNudgesSection().isOn.value).isTrue()
    }

    @Test
    fun `shows an error toast when storing the opt-in fails`() = runTest(context = dispatcher) {
        // given
        postTrialNudges.setResult(result = Result.failure(IllegalStateException("offline")))
        val viewModel = viewModel()

        // when
        viewModel.postTrialNudgesSection().onToggle(true)
        advanceTimeBy(delayTimeMillis = DEBOUNCE_ELAPSED_MILLIS)

        // then
        assertThat(viewModel.screen.scaffoldState.widgets.value.toast?.message)
            .isEqualTo(StringResource(MHCStrings.account_post_trial_nudges_error))
    }

    @Test
    fun `shows no toast when storing the opt-in succeeds`() = runTest(context = dispatcher) {
        // given
        val viewModel = viewModel()

        // when
        viewModel.postTrialNudgesSection().onToggle(true)
        advanceTimeBy(delayTimeMillis = DEBOUNCE_ELAPSED_MILLIS)

        // then
        assertThat(viewModel.screen.scaffoldState.widgets.value.toast).isNull()
    }

    @Test
    fun `shows the app version`() {
        // when
        val viewModel = viewModel()

        // then
        assertThat(viewModel.appInfoSection().row<ItemDisplayRow>(key = "version").value).isEqualTo(StringResource(APP_VERSION))
    }

    @Test
    fun `opens the privacy policy`() {
        // given
        val viewModel = viewModel()

        // when
        viewModel.appInfoSection().row<ExternalLink>(key = "privacy-policy").onClicked()

        // then
        assertThat(navigator.pushedEvents)
            .containsExactly(NavigationEvent.OpenPrivacyPolicy)
    }

    @Test
    fun `opens the app language settings when asked to change the language`() {
        // given
        val viewModel = viewModel()

        // when
        viewModel.supportSection().rowOrNull<ItemDisplayRow>(key = "change-language")?.onClick?.invoke()

        // then
        assertThat(navigator.pushedEvents).containsExactly(NavigationEvent.OpenAppLanguageSettings)
    }

    @Test
    fun `shows the feedback sheet when asked to send feedback`() {
        // given
        val viewModel = viewModel()

        // when
        viewModel.supportSection().row<ItemDisplayRow>(key = "send-feedback").onClick?.invoke()

        // then
        assertThat(viewModel.screen.scaffoldState.widgets.value.bottomSheet).isNotNull()
        assertThat(mapper.feedbackSheet?.message?.value).isEmpty()
    }

    @Test
    fun `offers sending feedback only once a message is written`() {
        // given
        val viewModel = viewModel()
        viewModel.supportSection().row<ItemDisplayRow>(key = "send-feedback").onClick?.invoke()
        val canSendInitially = mapper.canSendFeedback

        // when
        mapper.feedbackSheet?.onMessageChanged?.invoke("The dashboard is great")

        // then
        assertThat(canSendInitially).isFalse()
        assertThat(mapper.canSendFeedback).isTrue()
    }

    @Test
    fun `stops offering to send feedback when the message is cleared`() {
        // given
        val viewModel = viewModel()
        viewModel.supportSection().row<ItemDisplayRow>(key = "send-feedback").onClick?.invoke()
        mapper.feedbackSheet?.onMessageChanged?.invoke("The dashboard is great")

        // when
        mapper.feedbackSheet?.onMessageChanged?.invoke("   ")

        // then
        assertThat(mapper.canSendFeedback).isFalse()
    }

    @Test
    fun `sends the trimmed feedback message`() = runTest(context = dispatcher) {
        // given
        val viewModel = viewModel()
        viewModel.supportSection().row<ItemDisplayRow>(key = "send-feedback").onClick?.invoke()
        mapper.feedbackSheet?.onMessageChanged?.invoke("  The dashboard is great  ")

        // when
        mapper.sendFeedback?.invoke()

        // then
        assertThat(feedback.messages).containsExactly("The dashboard is great")
    }

    @Test
    fun `shows an error toast when sending feedback fails`() = runTest(context = dispatcher) {
        // given
        feedback.setResult(result = Result.failure(IllegalStateException("offline")))
        val viewModel = viewModel()
        viewModel.supportSection().row<ItemDisplayRow>(key = "send-feedback").onClick?.invoke()
        mapper.feedbackSheet?.onMessageChanged?.invoke("The dashboard is great")

        // when
        mapper.sendFeedback?.invoke()

        // then
        assertThat(viewModel.screen.scaffoldState.widgets.value.toast?.message)
            .isEqualTo(StringResource(MHCStrings.account_feedback_error))
        assertThat(mapper.feedbackSheet?.isDismissible?.invoke()).isFalse()
    }

    @Test
    fun `keeps the feedback sheet open while a message is unsent`() {
        // given
        val viewModel = viewModel()
        viewModel.supportSection().row<ItemDisplayRow>(key = "send-feedback").onClick?.invoke()
        val isDismissibleWhileEmpty = mapper.feedbackSheet?.isDismissible?.invoke()

        // when
        mapper.feedbackSheet?.onMessageChanged?.invoke("The dashboard is great")

        // then
        assertThat(isDismissibleWhileEmpty).isTrue()
        assertThat(mapper.feedbackSheet?.isDismissible?.invoke()).isFalse()
    }

    @Test
    fun `lets the feedback sheet close once its close button is tapped`() {
        // given
        val viewModel = viewModel()
        viewModel.supportSection().row<ItemDisplayRow>(key = "send-feedback").onClick?.invoke()
        mapper.feedbackSheet?.onMessageChanged?.invoke("The dashboard is great")

        // when
        mapper.closeFeedback?.invoke()

        // then
        assertThat(mapper.feedbackSheet?.isDismissible?.invoke()).isTrue()
    }

    @Test
    fun `closes the account overview when dismissed`() {
        // given
        val viewModel = viewModel()

        // when
        viewModel.screen.onDismiss()

        // then
        assertThat(navigator.pushedEvents).containsExactly(NavigationEvent.Close(route = MHCRoute.AccountOverview))
    }

    @Test
    fun `opens license information from the account overview`() {
        // given
        val viewModel = viewModel()

        // when
        viewModel.appInfoSection().row<ItemDisplayRow>(key = "license-information").onClick?.invoke()

        // then
        assertThat(navigator.pushedEvents).containsExactly(NavigationEvent.NavigateTo(route = MHCRoute.LicenseInformation))
    }

    private fun viewModel() = MHCAccountOverviewViewModel(
        navigator = navigator,
        studyWithdrawal = studyWithdrawal,
        postTrialNudges = postTrialNudges,
        feedback = feedback,
        enrolledStudySource = enrolledStudySource,
        mapper = mapper,
    )

    private fun MHCAccountOverviewViewModel.withdrawSection() =
        screen.customSections.section(key = "withdraw-from-study").content<WithdrawFromStudyButton>(key = "withdraw-from-study")

    private fun MHCAccountOverviewViewModel.postTrialNudgesSection() =
        studyParticipationSection().row<ToggleRow>(key = "post-trial-nudges")

    private fun MHCAccountOverviewViewModel.supportSection() =
        screen.customSections.section(key = "support")

    private fun MHCAccountOverviewViewModel.studyParticipationSection() =
        screen.customSections.section(key = "study-participation")

    private fun MHCAccountOverviewViewModel.enrolledStudyRow(): EnrolledStudyRow? {
        val section = screen.customSections.dynamicSection(key = "study-participation")
        viewModelScope.launch { section.collect {} }
        return section.value.rowOrNull<EnrolledStudyRow>(key = "enrolled-study")
    }

    private fun MHCAccountOverviewViewModel.appInfoSection() =
        screen.customSections.section(key = "app-info")

    /**
     * The real mapper, recording the latest feedback sheet and the actions of its app bar.
     */
    private class RecordingMapper(
        private val delegate: MHCAccountOverviewMapper = MHCAccountOverviewMapperImpl(
            appRevision = MHCAppRevisionFixtures.create(version = APP_VERSION),
            timeProvider = FakeTimeProvider(),
            buildInfo = BuildInfoFixtures.create(),
        ),
    ) : MHCAccountOverviewMapper by delegate {
        var feedbackSheet: FeedbackSheet? = null
        var canSendFeedback: Boolean? = null
        var sendFeedback: (suspend () -> Unit)? = null
        var closeFeedback: (() -> Unit)? = null

        override fun mapFeedback(
            appBar: StateFlow<GroveAppBar>,
            message: StateFlow<String>,
            onMessageChanged: (String) -> Unit,
            isDismissible: () -> Boolean,
        ) = delegate.mapFeedback(
            appBar = appBar,
            message = message,
            onMessageChanged = onMessageChanged,
            isDismissible = isDismissible,
        ).also { feedbackSheet = it }

        override fun mapFeedbackAppBar(
            canSend: Boolean,
            onClose: () -> Unit,
            onSend: suspend () -> Unit,
        ): GroveAppBar {
            canSendFeedback = canSend
            sendFeedback = onSend
            closeFeedback = onClose
            return delegate.mapFeedbackAppBar(
                canSend = canSend,
                onClose = onClose,
                onSend = onSend,
            )
        }
    }

    private companion object {
        const val DEBOUNCE_ELAPSED_MILLIS = 300L
        const val APP_VERSION = "1.2.0"
    }

    private fun WithdrawFromStudyButton.confirm() {
        confirmation.onDismiss()
        confirmation.confirmButton.action()
    }
}
