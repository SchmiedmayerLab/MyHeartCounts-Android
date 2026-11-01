//
// This source file is part of the My Heart Counts Android open-source project
//
// SPDX-FileCopyrightText: 2026 Stanford University and the project authors (see CONTRIBUTORS.md)
//
// SPDX-License-Identifier: MIT

package edu.stanford.myheartcounts.account.overview

import com.google.common.truth.Truth.assertThat
import edu.stanford.myheartcounts.account.overview.WithdrawFromStudySection.WithdrawalState
import edu.stanford.myheartcounts.standard.MHCAppRevisionFixtures
import edu.stanford.myheartcounts.ui.ToggleSection
import kotlinx.coroutines.flow.MutableStateFlow
import org.grovealliance.ui.ActionSource
import org.grovealliance.ui.GroveScaffoldState
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
    fun `offers study participation, the post-trial nudges opt-in, support and the app info above withdrawing from the study`() {
        // when
        val overview = mapper.map(input = input())

        // then
        assertThat(overview.sections.items.map { it::class })
            .containsExactly(
                StudyParticipationSection::class,
                ToggleSection::class,
                SupportSection::class,
                AppInfoSection::class,
                WithdrawFromStudySection::class,
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
        assertThat(section.version).isEqualTo(APP_VERSION)
    }

    @Test
    fun `reports opening the privacy policy through the action sink`() {
        // given
        val section = mapper.map(input = input()).appInfoSection()

        // when
        section.onPrivacyPolicySelected()

        // then
        assertThat(pushedActions).containsExactly(MHCAccountOverviewAction.PrivacyPolicySelected)
    }

    @Test
    fun `reports opening the participation stats through the action sink`() {
        // given
        val section = mapper.map(input = input()).sections.items.filterIsInstance<StudyParticipationSection>().single()

        // when
        section.onParticipationStatsSelected()

        // then
        assertThat(pushedActions).containsExactly(MHCAccountOverviewAction.ParticipationStatsSelected)
    }

    @Test
    fun `reports asking to send feedback through the action sink`() {
        // given
        val section = mapper.map(input = input()).sections.items.filterIsInstance<SupportSection>().single()

        // when
        section.onSendFeedbackSelected()

        // then
        assertThat(pushedActions).containsExactly(MHCAccountOverviewAction.SendFeedbackSelected)
    }

    @Test
    fun `offers withdrawing from the study`() {
        // when
        val overview = mapper.map(input = input())

        // then
        assertThat(overview.sections.items.filterIsInstance<WithdrawFromStudySection>()).hasSize(1)
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
        section.onWithdrawConfirmed()
        section.onDismissed()

        // then
        assertThat(pushedActions)
            .containsExactly(
                MHCAccountOverviewAction.WithdrawSelected,
                MHCAccountOverviewAction.WithdrawConfirmed,
                MHCAccountOverviewAction.WithdrawDialogDismissed,
            )
            .inOrder()
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
        sections.items.filterIsInstance<WithdrawFromStudySection>().single()

    private fun MHCAccountOverview.postTrialNudgesSection() =
        sections.items.filterIsInstance<ToggleSection>().single()

    private fun MHCAccountOverview.appInfoSection() =
        sections.items.filterIsInstance<AppInfoSection>().single()

    private companion object {
        const val APP_VERSION = "1.2.0"
    }
}
