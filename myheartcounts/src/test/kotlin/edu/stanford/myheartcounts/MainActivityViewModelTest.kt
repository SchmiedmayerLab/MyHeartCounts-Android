//
// This source file is part of the My Heart Counts Android open-source project
//
// SPDX-FileCopyrightText: 2026 Stanford University and the project authors (see CONTRIBUTORS.md)
//
// SPDX-License-Identifier: MIT

package edu.stanford.myheartcounts

import com.google.common.truth.Truth.assertThat
import edu.stanford.myheartcounts.account.FakeAccount
import edu.stanford.myheartcounts.navigation.FakeNavigator
import edu.stanford.myheartcounts.navigation.MHCRoute
import edu.stanford.myheartcounts.navigation.NavigationEvent
import edu.stanford.myheartcounts.onboarding.OnboardingCompletionImpl
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.setMain
import org.grovealliance.account.AccountDetails
import org.grovealliance.storage.local.InMemoryKeyValueStorage
import org.junit.After
import org.junit.Before
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class MainActivityViewModelTest {

    private val navigator = FakeNavigator()
    private val account = FakeAccount()
    private val onboardingCompletion = OnboardingCompletionImpl(storage = InMemoryKeyValueStorage())

    @Before
    fun setup() {
        Dispatchers.setMain(dispatcher = UnconfinedTestDispatcher())
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun `launches into onboarding when onboarding was never completed`() {
        // when
        val initialRoot = viewModel().state.value.initialRoot

        // then
        assertThat(initialRoot).isEqualTo(MHCRoute.Root.Onboarding)
    }

    @Test
    fun `launches into the study once onboarding was completed`() {
        // given
        onboardingCompletion.markComplete()

        // when
        val initialRoot = viewModel().state.value.initialRoot

        // then
        assertThat(initialRoot).isEqualTo(MHCRoute.Root.Study)
    }

    @Test
    fun `launches into onboarding after a completed onboarding was reset`() {
        // given
        onboardingCompletion.markComplete()
        onboardingCompletion.reset()

        // when
        val initialRoot = viewModel().state.value.initialRoot

        // then
        assertThat(initialRoot).isEqualTo(MHCRoute.Root.Onboarding)
    }

    @Test
    fun `switches to onboarding when the account signs out`() {
        // given
        onboardingCompletion.markComplete()
        account.supplyUserDetails(details = AccountDetails())
        viewModel()

        // when
        account.removeUserDetails()

        // then
        assertThat(navigator.pushedEvents).containsExactly(NavigationEvent.SwitchRoot(root = MHCRoute.Root.Onboarding))
    }

    private fun viewModel() = MainActivityViewModel(
        navigator = navigator,
        account = account,
        onboardingCompletion = onboardingCompletion,
    )
}
