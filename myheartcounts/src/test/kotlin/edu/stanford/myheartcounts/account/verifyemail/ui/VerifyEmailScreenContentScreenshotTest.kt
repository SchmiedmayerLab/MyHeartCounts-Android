//
// This source file is part of the My Heart Counts Android open-source project
//
// SPDX-FileCopyrightText: 2026 Stanford University and the project authors (see CONTRIBUTORS.md)
//
// SPDX-License-Identifier: MIT

package edu.stanford.myheartcounts.account.verifyemail.ui

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.GppMaybe
import androidx.compose.material.icons.filled.VerifiedUser
import androidx.compose.material.icons.outlined.Email
import androidx.compose.material.icons.outlined.VerifiedUser
import edu.stanford.myheartcounts.MHCStrings
import edu.stanford.myheartcounts.ui.AppBarTextAction
import edu.stanford.myheartcounts.ui.ExternalLink
import edu.stanford.myheartcounts.ui.MHCColors
import kotlinx.coroutines.flow.MutableStateFlow
import org.grovealliance.testing.screenshot.ScreenshotTest
import org.grovealliance.ui.AsyncTextButton
import org.grovealliance.ui.GroveScaffoldState
import org.grovealliance.ui.ImageResource
import org.grovealliance.ui.StringResource
import org.grovealliance.ui.groveAppBar
import org.grovealliance.ui.theme.resolve
import org.junit.Test

class VerifyEmailScreenContentScreenshotTest : ScreenshotTest() {
    private val unverified = VerifyEmailLayout.Body.Available(
        status = VerificationStatusCard(
            icon = ImageResource(
                image = Icons.Default.GppMaybe,
                tint = { MHCColors.orange.resolve() },
            ),
            text = StringResource(MHCStrings.verify_email_not_verified),
        ),
        stepsTitle = StringResource(MHCStrings.verify_email_how_to),
        steps = listOf(
            VerificationStepRow(
                title = StringResource(MHCStrings.verify_email_request_step),
                explanation = StringResource(MHCStrings.verify_email_request_explanation, EMAIL),
                emphasis = EMAIL,
                action = AsyncTextButton(
                    title = StringResource(MHCStrings.verify_email_send_link),
                    action = {},
                ),
            ),
            VerificationStepRow(
                title = StringResource(MHCStrings.verify_email_open_step),
                explanation = StringResource(MHCStrings.verify_email_open_explanation),
                emphasis = null,
                action = ExternalLink(
                    text = StringResource(MHCStrings.verify_email_open_mail),
                    icon = ImageResource(image = Icons.Outlined.Email),
                    onClicked = {},
                ),
            ),
        ),
    )
    private val base = VerifyEmailScreenContent(
        scaffoldState = GroveScaffoldState(appBar = groveAppBar { close {} }),
        layout = VerifyEmailLayout(
            icon = ImageResource(image = Icons.Outlined.VerifiedUser),
            title = StringResource(MHCStrings.verify_email_title),
            explanation = StringResource(MHCStrings.verify_email_explanation),
            body = MutableStateFlow(unverified),
        ),
    )

    @Test
    fun `VerifyEmailScreenContent not verified`() {
        // when / then
        screenshot { base.Content() }
    }

    @Test
    fun `VerifyEmailScreenContent verified`() {
        // given
        val content = base.copy(
            scaffoldState = GroveScaffoldState(
                appBar = groveAppBar {
                    action(
                        content = AppBarTextAction(
                            title = StringResource(MHCStrings.verify_email_done),
                            enabled = MutableStateFlow(true),
                            action = {},
                        ),
                    )
                },
            ),
            layout = base.layout.copy(
                body = MutableStateFlow(
                    VerifyEmailLayout.Body.Available(
                        status = VerificationStatusCard(
                            icon = ImageResource(
                                image = Icons.Default.VerifiedUser,
                                tint = { MHCColors.green.resolve() },
                            ),
                            text = StringResource(MHCStrings.verify_email_verified),
                        ),
                        stepsTitle = null,
                        steps = emptyList(),
                    ),
                ),
            ),
        )

        // when / then
        screenshot { content.Content() }
    }

    private companion object {
        const val EMAIL = "participant@example.com"
    }
}
