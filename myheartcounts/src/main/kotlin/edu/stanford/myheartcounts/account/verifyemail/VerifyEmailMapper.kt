//
// This source file is part of the My Heart Counts Android open-source project
//
// SPDX-FileCopyrightText: 2026 Stanford University and the project authors (see CONTRIBUTORS.md)
//
// SPDX-License-Identifier: MIT

package edu.stanford.myheartcounts.account.verifyemail

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.GppMaybe
import androidx.compose.material.icons.filled.VerifiedUser
import androidx.compose.material.icons.outlined.Email
import androidx.compose.material.icons.outlined.VerifiedUser
import edu.stanford.myheartcounts.MHCStrings
import edu.stanford.myheartcounts.account.verifyemail.ui.VerificationStatusCard
import edu.stanford.myheartcounts.account.verifyemail.ui.VerificationStepRow
import edu.stanford.myheartcounts.account.verifyemail.ui.VerifyEmailLayout
import edu.stanford.myheartcounts.ui.AppBarTextAction
import edu.stanford.myheartcounts.ui.ExternalLink
import edu.stanford.myheartcounts.ui.MHCColors
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import org.grovealliance.account.AccountDetails
import org.grovealliance.account.isVerified
import org.grovealliance.account.userId
import org.grovealliance.ui.ActionSink
import org.grovealliance.ui.AsyncTextButton
import org.grovealliance.ui.GroveAppBar
import org.grovealliance.ui.ImageResource
import org.grovealliance.ui.StringResource
import org.grovealliance.ui.groveAppBar
import org.grovealliance.ui.theme.resolve

/**
 * Builds the email verification screen and its app bar. Interactions are dispatched as
 * [VerifyEmailAction]s.
 */
interface VerifyEmailMapper {

    /**
     * The screen, showing whether the email of [VerifyEmailInput.details] is verified and, until it
     * is, how to verify it.
     */
    fun map(input: VerifyEmailInput): VerifyEmailLayout

    /**
     * The app bar: a close button until the email [isVerified], then a Done action instead.
     */
    fun mapAppBar(
        isVerified: Boolean,
        actionSink: ActionSink<VerifyEmailAction>,
    ): GroveAppBar
}

/**
 * What the email verification screen is built from.
 *
 * @param details the signed-in participant's account details, `null` while unavailable
 * @param scope the scope the details are followed in
 * @param actionSink receives the screen's interactions
 * @param canOpenMailApp whether an email app is installed, which the screen then offers to open
 * @param onRequestLink sends a verification link to the participant's email address
 */
data class VerifyEmailInput(
    val details: StateFlow<AccountDetails?>,
    val scope: CoroutineScope,
    val actionSink: ActionSink<VerifyEmailAction>,
    val canOpenMailApp: Boolean,
    val onRequestLink: suspend () -> Unit,
)

/**
 * Default [VerifyEmailMapper] implementation.
 */
class VerifyEmailMapperImpl : VerifyEmailMapper {

    override fun map(input: VerifyEmailInput) = VerifyEmailLayout(
        icon = ImageResource(image = Icons.Outlined.VerifiedUser),
        title = StringResource(MHCStrings.verify_email_title),
        explanation = StringResource(MHCStrings.verify_email_explanation),
        body = input.details
            .map { verification(details = it) }
            .distinctUntilChanged()
            .map { body(verification = it, input = input) }
            .stateIn(
                scope = input.scope,
                started = SharingStarted.Eagerly,
                initialValue = body(verification = verification(details = input.details.value), input = input),
            ),
    )

    override fun mapAppBar(
        isVerified: Boolean,
        actionSink: ActionSink<VerifyEmailAction>,
    ) = groveAppBar {
        if (isVerified) {
            action(
                content = AppBarTextAction(
                    title = StringResource(MHCStrings.verify_email_done),
                    enabled = MutableStateFlow(true),
                    action = { actionSink.push(VerifyEmailAction.Close) },
                ),
            )
        } else {
            close { actionSink.push(VerifyEmailAction.Close) }
        }
    }

    private fun verification(details: AccountDetails?) = details?.let {
        Verification(
            email = it.userId,
            isVerified = it.isVerified,
        )
    }

    private fun body(
        verification: Verification?,
        input: VerifyEmailInput,
    ): VerifyEmailLayout.Body {
        verification ?: return VerifyEmailLayout.Body.Unavailable(message = StringResource(MHCStrings.verify_email_no_account))
        val isVerified = verification.isVerified
        return VerifyEmailLayout.Body.Available(
            status = VerificationStatusCard(
                icon = if (isVerified) {
                    ImageResource(
                        image = Icons.Default.VerifiedUser,
                        tint = { MHCColors.green.resolve() },
                    )
                } else {
                    ImageResource(
                        image = Icons.Default.GppMaybe,
                        tint = { MHCColors.orange.resolve() },
                    )
                },
                text = StringResource(if (isVerified) MHCStrings.verify_email_verified else MHCStrings.verify_email_not_verified),
            ),
            stepsTitle = if (isVerified) null else StringResource(MHCStrings.verify_email_how_to),
            steps = if (isVerified) emptyList() else steps(email = verification.email, input = input),
        )
    }

    private fun steps(
        email: String,
        input: VerifyEmailInput,
    ) = listOf(
        VerificationStepRow(
            title = StringResource(MHCStrings.verify_email_request_step),
            explanation = StringResource(MHCStrings.verify_email_request_explanation, email),
            emphasis = email,
            action = AsyncTextButton(
                title = StringResource(MHCStrings.verify_email_send_link),
                action = { input.onRequestLink() },
            ),
        ),
        VerificationStepRow(
            title = StringResource(MHCStrings.verify_email_open_step),
            explanation = StringResource(MHCStrings.verify_email_open_explanation),
            emphasis = null,
            action = ExternalLink(
                text = StringResource(MHCStrings.verify_email_open_mail),
                icon = ImageResource(image = Icons.Outlined.Email),
                onClicked = { input.actionSink.push(VerifyEmailAction.OpenMailSelected) },
            ).takeIf { input.canOpenMailApp },
        ),
    )

    /**
     * The parts of the account details the screen shows.
     */
    private data class Verification(
        val email: String,
        val isVerified: Boolean,
    )
}
