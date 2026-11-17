//
// This source file is part of the My Heart Counts Android open-source project
//
// SPDX-FileCopyrightText: 2026 Stanford University and the project authors (see CONTRIBUTORS.md)
//
// SPDX-License-Identifier: MIT

package edu.stanford.myheartcounts.account.overview

import android.os.Build
import com.google.common.truth.Truth.assertThat
import edu.stanford.myheartcounts.BuildInfoFixtures
import edu.stanford.myheartcounts.MHCStrings
import edu.stanford.myheartcounts.account.overview.WithdrawFromStudyButton.WithdrawalState
import edu.stanford.myheartcounts.promptedactions.PromptedAction
import edu.stanford.myheartcounts.promptedactions.PromptedActionFixtures
import edu.stanford.myheartcounts.promptedactions.PromptedActionsMapperImpl
import edu.stanford.myheartcounts.standard.MHCAppRevisionFixtures
import edu.stanford.myheartcounts.study.EnrolledStudy
import edu.stanford.myheartcounts.study.EnrolledStudyFixtures
import edu.stanford.myheartcounts.ui.ExternalLink
import edu.stanford.myheartcounts.ui.ToggleRow
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import org.grovealliance.core.time.FakeTimeProvider
import org.grovealliance.ui.ActionSource
import org.grovealliance.ui.GroveAlertDialog
import org.grovealliance.ui.GroveScaffoldState
import org.grovealliance.ui.ItemDisplayRow
import org.grovealliance.ui.StringResource
import org.grovealliance.ui.scheduler.PromptedActionsCard
import org.junit.After
import org.junit.Before
import org.junit.Test
import java.time.Instant
import java.time.ZoneOffset
import java.util.Locale

@OptIn(ExperimentalCoroutinesApi::class)
class MHCAccountOverviewMapperTest {

    private val defaultLocale: Locale = Locale.getDefault()
    private val timeProvider = FakeTimeProvider().apply {
        setZone(zone = ZoneOffset.UTC)
    }
    private var buildInfo = BuildInfoFixtures.create()
    private val mapper get() = MHCAccountOverviewMapperImpl(
        appRevision = MHCAppRevisionFixtures.create(version = APP_VERSION),
        timeProvider = timeProvider,
        buildInfo = buildInfo,
        promptedActionsMapper = PromptedActionsMapperImpl(),
    )
    private val promptedActions = MutableStateFlow<List<PromptedAction>>(emptyList())
    private val pushedActions = mutableListOf<MHCAccountOverviewAction>()
    private val actionSink = ActionSource<MHCAccountOverviewAction> { pushedActions.add(it) }
        .sink<MHCAccountOverviewAction>()
    private val scaffoldState = GroveScaffoldState()
    private val withdrawalState = MutableStateFlow(WithdrawalState.Idle)
    private val postTrialNudgesOptIn = MutableStateFlow(false)
    private val enrolledStudy = MutableStateFlow<EnrolledStudy?>(value = null)
    private val isProcessingHealthData = MutableStateFlow(false)

    @Before
    fun setup() {
        Locale.setDefault(Locale.US)
    }

    @After
    fun tearDown() {
        Locale.setDefault(defaultLocale)
    }

    @Test
    fun `offers the prompted actions, study participation, data processing, support and the app info above withdrawing from the study`() {
        // when
        val overview = mapper.map(input = input())

        // then
        assertThat(overview.customSections.sectionKeys())
            .containsExactly(
                "prompted-actions",
                "study-participation",
                "data-processing",
                "support",
                "app-info",
                "withdraw-from-study",
            )
            .inOrder()
    }

    @Test
    fun `shows the given post-trial nudges opt-in`() {
        // given
        val section = mapper.map(input = input()).postTrialNudgesSection()

        // when
        postTrialNudgesOptIn.value = true

        // then
        assertThat(section.isOn.value).isTrue()
    }

    @Test
    fun `reports post-trial nudges changes through the action sink`() {
        // given
        val section = mapper.map(input = input()).postTrialNudgesSection()

        // when
        section.onToggle(true)

        // then
        assertThat(pushedActions).containsExactly(MHCAccountOverviewAction.PostTrialNudgesChanged(isOptedIn = true))
    }

    @Test
    fun `shows the given app version`() {
        // when
        val section = mapper.map(input = input()).appInfoSection()

        // then
        assertThat(section.row<ItemDisplayRow>(key = "version").value).isEqualTo(StringResource(APP_VERSION))
    }

    @Test
    fun `reports opening license information through the action sink`() {
        // given
        val section = mapper.map(input = input()).appInfoSection()

        // when
        section.row<ItemDisplayRow>(key = "license-information").onClick?.invoke()

        // then
        assertThat(pushedActions).containsExactly(MHCAccountOverviewAction.LicenseInformationSelected)
    }

    @Test
    fun `summarises every prompted action and opens them`() = runTest {
        // given
        promptedActions.value = listOf(PromptedActionFixtures.create())
        val section = mapper.map(input = input(scope = backgroundScope)).customSections.dynamicSection(key = "prompted-actions")
        backgroundScope.launch { section.collect {} }
        runCurrent()
        val card = section.value.content<PromptedActionsCard>(key = "prompted-actions")

        // when
        card.onClick?.invoke()

        // then
        assertThat(card.icons).hasSize(1)
        assertThat(pushedActions).containsExactly(MHCAccountOverviewAction.PromptedActionsSelected)
    }

    @Test
    fun `reports opening the privacy policy through the action sink`() {
        // given
        val section = mapper.map(input = input()).appInfoSection()

        // when
        section.row<ExternalLink>(key = "privacy-policy").onClicked()

        // then
        assertThat(pushedActions).containsExactly(MHCAccountOverviewAction.PrivacyPolicySelected)
    }

    @Test
    fun `reports opening the participation stats through the action sink`() {
        // given
        val section = mapper.map(input = input()).customSections.section(key = "study-participation")

        // when
        section.row<ItemDisplayRow>(key = "participation-stats").onClick?.invoke()

        // then
        assertThat(pushedActions).containsExactly(MHCAccountOverviewAction.ParticipationStatsSelected)
    }

    @Test
    fun `describes the enrolled study with its enrollment date and time`() = runTest(UnconfinedTestDispatcher()) {
        // given
        enrolledStudy.value = EnrolledStudyFixtures.create(
            title = "My Heart Counts",
            shortExplanation = "A study",
            enrollmentDate = Instant.parse("2026-01-01T09:30:00Z"),
        )

        // when
        val row = enrolledStudyRow(scope = backgroundScope)

        // then
        assertThat(row?.title).isEqualTo("My Heart Counts")
        assertThat(row?.shortExplanation).isEqualTo("A study")
        assertThat(row?.enrolledSince).isEqualTo(StringResource(MHCStrings.account_enrolled_since, "Jan 1, 2026, 9:30\u202FAM"))
    }

    @Test
    fun `reports asking to visit the study homepage through the action sink`() = runTest(UnconfinedTestDispatcher()) {
        // given
        enrolledStudy.value = EnrolledStudyFixtures.create(title = "My Heart Counts")
        val row = enrolledStudyRow(scope = backgroundScope)

        // when
        row?.website?.onClicked?.invoke()

        // then
        assertThat(pushedActions).containsExactly(MHCAccountOverviewAction.StudyHomepageSelected)
    }

    @Test
    fun `hides an enrolled study without a title`() = runTest(UnconfinedTestDispatcher()) {
        // given
        enrolledStudy.value = EnrolledStudyFixtures.create()

        // when
        val row = enrolledStudyRow(scope = backgroundScope)

        // then
        assertThat(row).isNull()
    }

    @Test
    fun `hides the enrolled study while there is none`() = runTest(UnconfinedTestDispatcher()) {
        // when
        val row = enrolledStudyRow(scope = backgroundScope)

        // then
        assertThat(row).isNull()
    }

    @Test
    fun `says that health data is processing while an enrolled participant's data uploads`() = runTest(UnconfinedTestDispatcher()) {
        // given
        enrolledStudy.value = EnrolledStudyFixtures.create(title = "My Heart Counts")
        isProcessingHealthData.value = true

        // when
        val row = dataProcessingRow(scope = backgroundScope)

        // then
        assertThat(row?.text).isEqualTo(StringResource(MHCStrings.account_processing_health_data))
    }

    @Test
    fun `says nothing about processing while no health data uploads`() = runTest(UnconfinedTestDispatcher()) {
        // given
        enrolledStudy.value = EnrolledStudyFixtures.create(title = "My Heart Counts")

        // when
        val row = dataProcessingRow(scope = backgroundScope)

        // then
        assertThat(row).isNull()
    }

    @Test
    fun `says nothing about processing without an enrollment`() = runTest(UnconfinedTestDispatcher()) {
        // given
        isProcessingHealthData.value = true

        // when
        val row = dataProcessingRow(scope = backgroundScope)

        // then
        assertThat(row).isNull()
    }

    @Test
    fun `reports asking to update the comorbidities through the action sink`() {
        // given
        val section = mapper.map(input = input()).customSections.section(key = "study-participation")

        // when
        section.row<ItemDisplayRow>(key = "comorbidities").onClick?.invoke()

        // then
        assertThat(pushedActions).containsExactly(MHCAccountOverviewAction.UpdateComorbiditiesSelected)
    }

    @Test
    fun `reports asking to review the signed consent forms through the action sink`() {
        // given
        val section = mapper.map(input = input()).customSections.section(key = "study-participation")

        // when
        section.row<ItemDisplayRow>(key = "signed-consent-forms").onClick?.invoke()

        // then
        assertThat(pushedActions).containsExactly(MHCAccountOverviewAction.SignedConsentFormsSelected)
    }

    @Test
    fun `reports asking to change the workout preference through the action sink`() {
        // given
        val section = mapper.map(input = input()).customSections.section(key = "study-participation")

        // when
        section.row<ItemDisplayRow>(key = "workout-preference").onClick?.invoke()

        // then
        assertThat(pushedActions).containsExactly(MHCAccountOverviewAction.WorkoutPreferenceSelected)
    }

    @Test
    fun `reports asking to send feedback through the action sink`() {
        // given
        val section = mapper.map(input = input()).customSections.section(key = "support")

        // when
        section.row<ItemDisplayRow>(key = "send-feedback").onClick?.invoke()

        // then
        assertThat(pushedActions).containsExactly(MHCAccountOverviewAction.SendFeedbackSelected)
    }

    @Test
    fun `reports asking to change the language through the action sink`() {
        // given
        val section = mapper.map(input = input()).customSections.section(key = "support")

        // when
        section.rowOrNull<ItemDisplayRow>(key = "change-language")?.onClick?.invoke()

        // then
        assertThat(pushedActions).containsExactly(MHCAccountOverviewAction.ChangeLanguageSelected)
    }

    @Test
    fun `offers no language change where the system has no app language setting`() {
        // given
        buildInfo = BuildInfoFixtures.create(sdkInt = Build.VERSION_CODES.S_V2)

        // when
        val section = mapper.map(input = input()).customSections.section(key = "support")

        // then
        assertThat(section.rowOrNull<ItemDisplayRow>(key = "change-language")).isNull()
    }

    @Test
    fun `offers withdrawing from the study`() {
        // when
        val overview = mapper.map(input = input())

        // then
        val section = overview.customSections.section(key = "withdraw-from-study")
        assertThat(section.content<WithdrawFromStudyButton>(key = "withdraw-from-study")).isNotNull()
    }

    @Test
    fun `shows the given withdrawal state`() {
        // given
        val section = mapper.map(input = input()).withdrawSection()

        // when
        withdrawalState.value = WithdrawalState.Withdrawing

        // then
        assertThat(section.state.value).isEqualTo(WithdrawalState.Withdrawing)
    }

    @Test
    fun `reports withdrawal interactions through the action sink`() {
        // given
        val section = mapper.map(input = input()).withdrawSection()

        // when
        section.onWithdrawSelected()
        section.confirmation.confirmButton.action()
        section.confirmation.onDismiss()
        section.failure.onDismiss()

        // then
        assertThat(pushedActions)
            .containsExactly(
                MHCAccountOverviewAction.WithdrawSelected,
                MHCAccountOverviewAction.WithdrawConfirmed,
                MHCAccountOverviewAction.WithdrawDialogDismissed,
                MHCAccountOverviewAction.WithdrawDialogDismissed,
            )
            .inOrder()
    }

    @Test
    fun `marks confirming the withdrawal as destructive`() {
        // when
        val section = mapper.map(input = input()).withdrawSection()

        // then
        assertThat(section.confirmation.confirmButton.style).isEqualTo(GroveAlertDialog.Button.Style.Destructive)
        assertThat(section.confirmation.dismissButton).isNotNull()
        assertThat(section.failure.dismissButton).isNull()
    }

    @Test
    fun `uses the given scaffold state`() {
        // when
        val overview = mapper.map(input = input())

        // then
        assertThat(overview.scaffoldState).isSameInstanceAs(scaffoldState)
    }

    @Test
    fun `reports dismissal through the action sink`() {
        // given
        val overview = mapper.map(input = input())

        // when
        overview.onDismiss()

        // then
        assertThat(pushedActions).containsExactly(MHCAccountOverviewAction.Dismissed)
    }

    private fun input(scope: CoroutineScope = TestScope()) = MHCAccountOverviewInput(
        scaffoldState = scaffoldState,
        withdrawalState = withdrawalState,
        postTrialNudgesOptIn = postTrialNudgesOptIn,
        enrolledStudy = enrolledStudy,
        promptedActions = promptedActions,
        isProcessingHealthData = isProcessingHealthData,
        scope = scope,
        actionSink = actionSink,
    )

    private fun enrolledStudyRow(scope: CoroutineScope): EnrolledStudyRow? {
        val section = mapper.map(input = input(scope = scope)).customSections.dynamicSection(key = "study-participation")
        scope.launch { section.collect {} }
        return section.value.rowOrNull<EnrolledStudyRow>(key = "enrolled-study")
    }

    private fun dataProcessingRow(scope: CoroutineScope): DataProcessingRow? {
        val section = mapper.map(input = input(scope = scope)).customSections.dynamicSection(key = "data-processing")
        scope.launch { section.collect {} }
        return section.value.rowOrNull<DataProcessingRow>(key = "data-processing")
    }

    private fun MHCAccountOverview.withdrawSection() =
        customSections.section(key = "withdraw-from-study").content<WithdrawFromStudyButton>(key = "withdraw-from-study")

    private fun MHCAccountOverview.postTrialNudgesSection() =
        customSections.section(key = "study-participation").row<ToggleRow>(key = "post-trial-nudges")

    private fun MHCAccountOverview.appInfoSection() =
        customSections.section(key = "app-info")

    private companion object {
        const val APP_VERSION = "1.2.0"
    }
}
