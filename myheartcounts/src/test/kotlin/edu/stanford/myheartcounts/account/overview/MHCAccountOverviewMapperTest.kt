//
// This source file is part of the My Heart Counts Android open-source project
//
// SPDX-FileCopyrightText: 2026 Stanford University and the project authors (see CONTRIBUTORS.md)
//
// SPDX-License-Identifier: MIT

package edu.stanford.myheartcounts.account.overview

import com.google.common.truth.Truth.assertThat
import edu.stanford.myheartcounts.account.overview.WithdrawFromStudySection.WithdrawalState
import kotlinx.coroutines.flow.MutableStateFlow
import org.grovealliance.ui.ActionSource
import org.junit.Test

class MHCAccountOverviewMapperTest {

    private val mapper = MHCAccountOverviewMapperImpl()
    private val pushedActions = mutableListOf<MHCAccountOverviewAction>()
    private val actionSink = ActionSource<MHCAccountOverviewAction> { pushedActions.add(it) }
        .sink<MHCAccountOverviewAction>()
    private val withdrawalState = MutableStateFlow(WithdrawalState.Idle)

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
    fun `reports dismissal through the action sink`() {
        // given
        val overview = mapper.map(input = input())

        // when
        overview.onDismiss()

        // then
        assertThat(pushedActions).containsExactly(MHCAccountOverviewAction.Dismissed)
    }

    private fun input() = MHCAccountOverviewInput(
        withdrawalState = withdrawalState,
        actionSink = actionSink,
    )

    private fun MHCAccountOverview.withdrawSection() =
        sections.items.filterIsInstance<WithdrawFromStudySection>().single()
}
