//
// This source file is part of the My Heart Counts Android open-source project
//
// SPDX-FileCopyrightText: 2026 Stanford University and the project authors (see CONTRIBUTORS.md)
//
// SPDX-License-Identifier: MIT

package edu.stanford.myheartcounts.account.overview

import com.google.common.truth.Truth.assertThat
import edu.stanford.myheartcounts.account.FakeStudyWithdrawal
import edu.stanford.myheartcounts.account.overview.WithdrawFromStudySection.WithdrawalState
import edu.stanford.myheartcounts.navigation.FakeNavigator
import edu.stanford.myheartcounts.navigation.MHCRoute
import edu.stanford.myheartcounts.navigation.NavigationEvent
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Before
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class MHCAccountOverviewViewModelTest {

    private val navigator = FakeNavigator()
    private val studyWithdrawal = FakeStudyWithdrawal()

    @Before
    fun setup() {
        Dispatchers.setMain(dispatcher = UnconfinedTestDispatcher())
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
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
        viewModel.withdrawSection().onDismissed()

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
        viewModel.withdrawSection().onWithdrawConfirmed()

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
        viewModel.withdrawSection().onWithdrawConfirmed()

        // then
        assertThat(viewModel.withdrawSection().state.value).isEqualTo(WithdrawalState.Failed)
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

    private fun viewModel() = MHCAccountOverviewViewModel(
        navigator = navigator,
        studyWithdrawal = studyWithdrawal,
        mapper = MHCAccountOverviewMapperImpl(),
    )

    private fun MHCAccountOverviewViewModel.withdrawSection() =
        screen.sections.items.filterIsInstance<WithdrawFromStudySection>().single()
}
