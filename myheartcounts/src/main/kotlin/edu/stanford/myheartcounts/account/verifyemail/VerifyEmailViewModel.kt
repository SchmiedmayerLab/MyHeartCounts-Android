//
// This source file is part of the My Heart Counts Android open-source project
//
// SPDX-FileCopyrightText: 2026 Stanford University and the project authors (see CONTRIBUTORS.md)
//
// SPDX-License-Identifier: MIT

package edu.stanford.myheartcounts.account.verifyemail

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import edu.stanford.myheartcounts.MHCStrings
import edu.stanford.myheartcounts.account.verifyemail.ui.VerifyEmailScreenContent
import edu.stanford.myheartcounts.navigation.ExternalAppSource
import edu.stanford.myheartcounts.navigation.MHCRoute
import edu.stanford.myheartcounts.navigation.NavigationEvent
import edu.stanford.myheartcounts.navigation.Navigator
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch
import org.grovealliance.account.Account
import org.grovealliance.account.AccountService
import org.grovealliance.account.isVerified
import org.grovealliance.core.logging.groveLogger
import org.grovealliance.ui.ActionSource
import org.grovealliance.ui.ImageResource
import org.grovealliance.ui.StringResource
import org.grovealliance.ui.mutableScaffoldState
import org.grovealliance.ui.showErrorToast
import kotlin.time.Duration.Companion.seconds

/**
 * Backs the email verification screen: shows whether the signed-in participant's email address is
 * verified, sends a verification link on request, and reloads the account every few seconds while
 * the screen is shown and the address is not verified yet, since nothing reports the verification
 * when it happens.
 */
class VerifyEmailViewModel(
    private val account: Account,
    private val accountService: AccountService,
    private val navigator: Navigator,
    externalApps: ExternalAppSource,
    mapper: VerifyEmailMapper,
) : ViewModel() {

    private val logger by groveLogger()
    private val actionSink = ActionSource(::onAction).sink<VerifyEmailAction>()
    private val scaffoldState = mutableScaffoldState(
        appBar = mapper.mapAppBar(
            isVerified = account.details.value?.isVerified == true,
            actionSink = actionSink,
        ),
    )

    /**
     * The screen's content.
     */
    val screen = VerifyEmailScreenContent(
        scaffoldState = scaffoldState.asScaffoldState(),
        layout = mapper.map(
            input = VerifyEmailInput(
                details = account.details,
                scope = viewModelScope,
                actionSink = actionSink,
                canOpenMailApp = externalApps.canOpen(app = NavigationEvent.OpenMailApp),
                onRequestLink = ::requestLink,
            ),
        ),
    )

    init {
        viewModelScope.launch {
            account.details
                .map { it?.isVerified == true }
                .distinctUntilChanged()
                .collect { isVerified ->
                    scaffoldState.setAppBar(
                        appBar = mapper.mapAppBar(
                            isVerified = isVerified,
                            actionSink = actionSink,
                        ),
                    )
                }
        }
        viewModelScope.launch {
            while (account.details.value?.isVerified != true) {
                accountService.refresh().onFailure { logger.w(it) { "Failed to reload the account." } }
                delay(POLL_INTERVAL)
            }
        }
    }

    private fun onAction(action: VerifyEmailAction) {
        when (action) {
            VerifyEmailAction.Close -> navigator.push(event = NavigationEvent.Close(route = MHCRoute.VerifyEmail))
            VerifyEmailAction.OpenMailSelected -> navigator.push(event = NavigationEvent.OpenMailApp)
        }
    }

    private suspend fun requestLink() {
        accountService.sendEmailVerification()
            .onSuccess {
                scaffoldState.showToast(
                    imageResource = ImageResource(image = Icons.Default.CheckCircle),
                    message = StringResource(MHCStrings.verify_email_link_sent),
                )
            }
            .onFailure {
                logger.e(it) { "Failed to send the email verification link." }
                scaffoldState.showErrorToast(message = StringResource(MHCStrings.verify_email_link_failed))
            }
    }

    private companion object {
        val POLL_INTERVAL = 5.seconds
    }
}
