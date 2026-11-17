//
// This source file is part of the My Heart Counts Android open-source project
//
// SPDX-FileCopyrightText: 2026 Stanford University and the project authors (see CONTRIBUTORS.md)
//
// SPDX-License-Identifier: MIT

package edu.stanford.myheartcounts.account.verifyemail

import androidx.lifecycle.viewModelScope
import com.google.common.truth.Truth.assertThat
import edu.stanford.myheartcounts.MHCStrings
import edu.stanford.myheartcounts.account.verifyemail.ui.VerifyEmailLayout
import edu.stanford.myheartcounts.navigation.FakeExternalAppSource
import edu.stanford.myheartcounts.navigation.FakeNavigator
import edu.stanford.myheartcounts.navigation.MHCRoute
import edu.stanford.myheartcounts.navigation.NavigationEvent
import edu.stanford.myheartcounts.ui.ExternalLink
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.cancel
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.advanceTimeBy
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.grovealliance.account.AccountDetails
import org.grovealliance.account.AccountKeys
import org.grovealliance.account.FakeAccount
import org.grovealliance.account.FakeAccountService
import org.grovealliance.account.isVerified
import org.grovealliance.ui.ActionSink
import org.grovealliance.ui.AsyncTextButton
import org.grovealliance.ui.GroveAppBar
import org.grovealliance.ui.StringResource
import org.junit.After
import org.junit.Before
import org.junit.Test
import kotlin.time.Duration.Companion.seconds

@OptIn(ExperimentalCoroutinesApi::class)
class VerifyEmailViewModelTest {

    private val dispatcher = StandardTestDispatcher()
    private val account = FakeAccount()
    private val accountService = FakeAccountService()
    private val navigator = FakeNavigator()
    private val externalApps = FakeExternalAppSource()
    private val mapper = RecordingVerifyEmailMapper()

    @Before
    fun setup() {
        Dispatchers.setMain(dispatcher)
        account.supplyUserDetails(details = details(isVerified = false))
    }

    @After
    fun teardown() {
        Dispatchers.resetMain()
    }

    @Test
    fun `reloads the account every five seconds while the email is not verified`() = runTest(dispatcher) {
        // given
        val viewModel = viewModel()
        runCurrent()

        // when
        advanceTimeBy(delayTime = 10.seconds)
        runCurrent()

        // then
        assertThat(accountService.refreshes).isEqualTo(3)
        viewModel.viewModelScope.cancel()
    }

    @Test
    fun `stops reloading once the email is verified`() = runTest(dispatcher) {
        // given
        accountService.setOnRefresh { account.supplyUserDetails(details = details(isVerified = true)) }
        val viewModel = viewModel()
        runCurrent()

        // when
        advanceTimeBy(delayTime = 30.seconds)
        runCurrent()

        // then
        assertThat(accountService.refreshes).isEqualTo(1)
        val body = viewModel.screen.layout.body.value as VerifyEmailLayout.Body.Available
        assertThat(body.steps).isEmpty()
    }

    @Test
    fun `never reloads an already verified account`() = runTest(dispatcher) {
        // given
        account.supplyUserDetails(details = details(isVerified = true))

        // when
        viewModel()
        runCurrent()

        // then
        assertThat(accountService.refreshes).isEqualTo(0)
    }

    @Test
    fun `sends a verification link when asked`() = runTest(dispatcher) {
        // given
        val viewModel = viewModel()
        runCurrent()
        val body = viewModel.screen.layout.body.value as VerifyEmailLayout.Body.Available

        // when
        (body.steps.first().action as AsyncTextButton).action()

        // then
        assertThat(accountService.verificationEmails).isEqualTo(1)
        assertThat(viewModel.screen.scaffoldState.widgets.value.toast?.message)
            .isEqualTo(StringResource(MHCStrings.verify_email_link_sent))
        viewModel.viewModelScope.cancel()
    }

    @Test
    fun `tells when the verification link could not be sent`() = runTest(dispatcher) {
        // given
        accountService.setSendEmailVerificationResult(Result.failure(IllegalStateException("offline")))
        val viewModel = viewModel()
        runCurrent()
        val body = viewModel.screen.layout.body.value as VerifyEmailLayout.Body.Available

        // when
        (body.steps.first().action as AsyncTextButton).action()

        // then
        assertThat(viewModel.screen.scaffoldState.widgets.value.toast?.message)
            .isEqualTo(StringResource(MHCStrings.verify_email_link_failed))
        viewModel.viewModelScope.cancel()
    }

    @Test
    fun `opens the email app`() = runTest(dispatcher) {
        // given
        val viewModel = viewModel()
        runCurrent()
        val body = viewModel.screen.layout.body.value as VerifyEmailLayout.Body.Available

        // when
        (body.steps.last().action as ExternalLink).onClicked()

        // then
        assertThat(navigator.pushedEvents).containsExactly(NavigationEvent.OpenMailApp)
        viewModel.viewModelScope.cancel()
    }

    @Test
    fun `offers the email app only when one is installed`() = runTest(dispatcher) {
        // given
        externalApps.setCanOpen { it != NavigationEvent.OpenMailApp }

        // when
        val viewModel = viewModel()
        runCurrent()

        // then
        val body = viewModel.screen.layout.body.value as VerifyEmailLayout.Body.Available
        assertThat(body.steps.last().action).isNull()
        viewModel.viewModelScope.cancel()
    }

    @Test
    fun `closes the screen`() = runTest(dispatcher) {
        // given
        val viewModel = viewModel()
        runCurrent()

        // when
        mapper.actionSink.push(VerifyEmailAction.Close)

        // then
        assertThat(navigator.pushedEvents).containsExactly(NavigationEvent.Close(route = MHCRoute.VerifyEmail))
        viewModel.viewModelScope.cancel()
    }

    private fun viewModel() = VerifyEmailViewModel(
        account = account,
        accountService = accountService,
        navigator = navigator,
        externalApps = externalApps,
        mapper = mapper,
    )

    private fun details(isVerified: Boolean) = AccountDetails().apply {
        this[AccountKeys.accountId::class] = "account-id"
        this[AccountKeys.userId::class] = "participant@example.com"
        this.isVerified = isVerified
    }

    private class RecordingVerifyEmailMapper : VerifyEmailMapper {
        private val mapper = VerifyEmailMapperImpl()
        private var currentActionSink: ActionSink<VerifyEmailAction>? = null
        val actionSink: ActionSink<VerifyEmailAction>
            get() = requireNotNull(currentActionSink)

        override fun map(input: VerifyEmailInput) = mapper.map(input = input)

        override fun mapAppBar(
            isVerified: Boolean,
            actionSink: ActionSink<VerifyEmailAction>,
        ): GroveAppBar {
            currentActionSink = actionSink
            return mapper.mapAppBar(
                isVerified = isVerified,
                actionSink = actionSink,
            )
        }
    }
}
