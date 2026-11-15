//
// This source file is part of the My Heart Counts Android open-source project
//
// SPDX-FileCopyrightText: 2026 Stanford University and the project authors (see CONTRIBUTORS.md)
//
// SPDX-License-Identifier: MIT

package edu.stanford.myheartcounts.account.overview

import com.google.common.truth.Truth.assertThat
import edu.stanford.myheartcounts.account.overview.WithdrawFromStudyButton.WithdrawalState
import edu.stanford.myheartcounts.standard.MHCAppRevisionFixtures
import edu.stanford.myheartcounts.ui.ToggleRow
import kotlinx.coroutines.flow.MutableStateFlow
import org.grovealliance.ui.ActionSource
import org.grovealliance.ui.GroveAlertDialog
import org.grovealliance.ui.GroveScaffoldState
import org.grovealliance.ui.ItemDisplayRow
import org.grovealliance.ui.StringResource
import org.junit.Test

class MHCAccountOverviewMapperTest {

    private val mapper = MHCAccountOverviewMapperImpl(
        appRevision = MHCAppRevisionFixtures.create(version = APP_VERSION),
    )
    private val pushedActions = mutableListOf<MHCAccountOverviewAction>()
    private val actionSink = ActionSource<MHCAccountOverviewAction> { pushedActions.add(it) }
        .sink<MHCAccountOverviewAction>()
    private val scaffoldState = GroveScaffoldState()
    private val withdrawalState = MutableStateFlow(WithdrawalState.Idle)
    private val postTrialNudgesOptIn = MutableStateFlow(false)

    @Test
    fun `offers study participation, post-trial nudges, support and the app info above withdrawing from the study`() {
        // when
        val overview = mapper.map(input = input())

        // then
        assertThat(overview.customSections.sectionKeys())
            .containsExactly(
                "study-participation",
                "post-trial-nudges",
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
    fun `reports opening the privacy policy through the action sink`() {
        // given
        val section = mapper.map(input = input()).appInfoSection()

        // when
        section.row<ItemDisplayRow>(key = "privacy-policy").onClick?.invoke()

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
    fun `reports asking to update the comorbidities through the action sink`() {
        // given
        val section = mapper.map(input = input()).customSections.section(key = "study-participation")

        // when
        section.row<ItemDisplayRow>(key = "comorbidities").onClick?.invoke()

        // then
        assertThat(pushedActions).containsExactly(MHCAccountOverviewAction.UpdateComorbiditiesSelected)
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
    fun `reports asking to update the demographics through the action sink`() {
        // given
        val section = mapper.map(input = input()).customSections.section(key = "support")

        // when
        section.row<ItemDisplayRow>(key = "demographics").onClick?.invoke()

        // then
        assertThat(pushedActions).containsExactly(MHCAccountOverviewAction.DemographicsSelected)
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

    private fun input() = MHCAccountOverviewInput(
        scaffoldState = scaffoldState,
        withdrawalState = withdrawalState,
        postTrialNudgesOptIn = postTrialNudgesOptIn,
        actionSink = actionSink,
    )

    private fun MHCAccountOverview.withdrawSection() =
        customSections.section(key = "withdraw-from-study").content<WithdrawFromStudyButton>(key = "withdraw-from-study")

    private fun MHCAccountOverview.postTrialNudgesSection() =
        customSections.section(key = "post-trial-nudges").row<ToggleRow>(key = "post-trial-nudges")

    private fun MHCAccountOverview.appInfoSection() =
        customSections.section(key = "app-info")

    private companion object {
        const val APP_VERSION = "1.2.0"
    }
}
