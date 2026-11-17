//
// This source file is part of the My Heart Counts Android open-source project
//
// SPDX-FileCopyrightText: 2026 Stanford University and the project authors (see CONTRIBUTORS.md)
//
// SPDX-License-Identifier: MIT

package edu.stanford.myheartcounts.account.verifyemail

import com.google.common.truth.Truth.assertThat
import edu.stanford.myheartcounts.MHCStrings
import edu.stanford.myheartcounts.account.verifyemail.ui.VerifyEmailLayout
import edu.stanford.myheartcounts.ui.ExternalLink
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.runTest
import org.grovealliance.account.AccountDetails
import org.grovealliance.account.AccountKeys
import org.grovealliance.account.isVerified
import org.grovealliance.ui.ActionSource
import org.grovealliance.ui.AsyncTextButton
import org.grovealliance.ui.StringResource
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class VerifyEmailMapperTest {

    private val pushedActions = mutableListOf<VerifyEmailAction>()
    private val actionSink = ActionSource<VerifyEmailAction> { pushedActions.add(it) }.sink<VerifyEmailAction>()
    private val details = MutableStateFlow<AccountDetails?>(null)
    private val scope = CoroutineScope(UnconfinedTestDispatcher())
    private var requestedLinks = 0
    private val mapper = VerifyEmailMapperImpl()

    @Test
    fun `says the account can't be read without account details`() {
        // when
        val body = mapper.map(input = input()).body.value

        // then
        assertThat(body).isEqualTo(
            VerifyEmailLayout.Body.Unavailable(message = StringResource(MHCStrings.verify_email_no_account)),
        )
    }

    @Test
    fun `shows an unverified email with the steps to verify it`() {
        // given
        details.value = details(isVerified = false)

        // when
        val body = mapper.map(input = input()).body.value as VerifyEmailLayout.Body.Available

        // then
        assertThat(body.status.text).isEqualTo(StringResource(MHCStrings.verify_email_not_verified))
        assertThat(body.stepsTitle).isEqualTo(StringResource(MHCStrings.verify_email_how_to))
        assertThat(body.steps.map { it.title }).containsExactly(
            StringResource(MHCStrings.verify_email_request_step),
            StringResource(MHCStrings.verify_email_open_step),
        ).inOrder()
        assertThat(body.steps.first().explanation)
            .isEqualTo(StringResource(MHCStrings.verify_email_request_explanation, EMAIL))
        assertThat(body.steps.first().emphasis).isEqualTo(EMAIL)
    }

    @Test
    fun `shows a verified email without steps`() {
        // given
        details.value = details(isVerified = true)

        // when
        val body = mapper.map(input = input()).body.value as VerifyEmailLayout.Body.Available

        // then
        assertThat(body.status.text).isEqualTo(StringResource(MHCStrings.verify_email_verified))
        assertThat(body.stepsTitle).isNull()
        assertThat(body.steps).isEmpty()
    }

    @Test
    fun `follows the email becoming verified`() {
        // given
        details.value = details(isVerified = false)
        val layout = mapper.map(input = input())

        // when
        details.value = details(isVerified = true)

        // then
        val body = layout.body.value as VerifyEmailLayout.Body.Available
        assertThat(body.status.text).isEqualTo(StringResource(MHCStrings.verify_email_verified))
    }

    @Test
    fun `requests a verification link from the first step`() = runTest {
        // given
        details.value = details(isVerified = false)
        val body = mapper.map(input = input()).body.value as VerifyEmailLayout.Body.Available

        // when
        (body.steps.first().action as AsyncTextButton).action()

        // then
        assertThat(requestedLinks).isEqualTo(1)
    }

    @Test
    fun `opens the email app from the second step`() = runTest {
        // given
        details.value = details(isVerified = false)
        val body = mapper.map(input = input()).body.value as VerifyEmailLayout.Body.Available

        // when
        (body.steps.last().action as ExternalLink).onClicked()

        // then
        assertThat(pushedActions).containsExactly(VerifyEmailAction.OpenMailSelected)
    }

    @Test
    fun `offers no email app without one installed`() {
        // given
        details.value = details(isVerified = false)

        // when
        val body = mapper.map(input = input(canOpenMailApp = false)).body.value as VerifyEmailLayout.Body.Available

        // then
        assertThat(body.steps.last().title).isEqualTo(StringResource(MHCStrings.verify_email_open_step))
        assertThat(body.steps.last().action).isNull()
    }

    @Test
    fun `closes from the app bar until the email is verified`() {
        // when
        val appBar = mapper.mapAppBar(
            isVerified = false,
            actionSink = actionSink,
        )

        // then
        assertThat(appBar.navigation).isNotNull()
        assertThat(appBar.actions.orEmpty()).isEmpty()
    }

    @Test
    fun `offers Done in the app bar once the email is verified`() {
        // when
        val appBar = mapper.mapAppBar(
            isVerified = true,
            actionSink = actionSink,
        )

        // then
        assertThat(appBar.navigation).isNull()
        assertThat(appBar.actions).hasSize(1)
    }

    private fun input(canOpenMailApp: Boolean = true) = VerifyEmailInput(
        details = details,
        scope = scope,
        actionSink = actionSink,
        canOpenMailApp = canOpenMailApp,
        onRequestLink = { requestedLinks++ },
    )

    private fun details(isVerified: Boolean) = AccountDetails().apply {
        this[AccountKeys.accountId::class] = "account-id"
        this[AccountKeys.userId::class] = EMAIL
        this.isVerified = isVerified
    }

    private companion object {
        const val EMAIL = "participant@example.com"
    }
}
