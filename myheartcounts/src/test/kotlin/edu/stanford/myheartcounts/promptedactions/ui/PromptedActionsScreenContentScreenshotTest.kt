//
// This source file is part of the My Heart Counts Android open-source project
//
// SPDX-FileCopyrightText: 2026 Stanford University and the project authors (see CONTRIBUTORS.md)
//
// SPDX-License-Identifier: MIT

package edu.stanford.myheartcounts.promptedactions.ui

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Badge
import androidx.compose.material.icons.filled.Email
import edu.stanford.myheartcounts.MHCStrings
import kotlinx.coroutines.flow.MutableStateFlow
import org.grovealliance.testing.screenshot.ScreenshotTest
import org.grovealliance.ui.AsyncTextButton
import org.grovealliance.ui.CardRow
import org.grovealliance.ui.GroveAlertDialog
import org.grovealliance.ui.GroveScaffoldState
import org.grovealliance.ui.ImageResource
import org.grovealliance.ui.StringResource
import org.grovealliance.ui.groveAppBar
import org.grovealliance.ui.groveSectionList
import org.grovealliance.ui.scheduler.PromptedActionRow
import org.junit.Test

class PromptedActionsScreenContentScreenshotTest : ScreenshotTest() {
    private val demographics = CardRow(
        key = "demographics",
        content = PromptedActionRow(
            icon = ImageResource(image = Icons.Default.Badge),
            title = StringResource(MHCStrings.prompted_action_demographics_title),
            message = StringResource(MHCStrings.prompted_action_demographics_message),
            action = AsyncTextButton(
                title = StringResource(MHCStrings.prompted_action_demographics_button),
                action = {},
            ),
            isCompleted = false,
            onStopSuggesting = {},
        ),
    )
    private val verifyEmail = CardRow(
        key = "verify-email",
        content = PromptedActionRow(
            icon = ImageResource(image = Icons.Default.Email),
            title = StringResource(MHCStrings.prompted_action_verify_email_title),
            message = StringResource(MHCStrings.prompted_action_verify_email_message),
            action = AsyncTextButton(
                title = StringResource(MHCStrings.prompted_action_verify_email_button),
                action = {},
            ),
            isCompleted = false,
            onStopSuggesting = {},
        ),
    )
    private val base = PromptedActionsScreenContent(
        scaffoldState = GroveScaffoldState(
            appBar = groveAppBar {
                close {}
                title(MHCStrings.prompted_actions_title)
            },
        ),
        sections = MutableStateFlow(
            groveSectionList {
                section(key = "pending") {
                    card(key = demographics.key) { row(row = demographics) }
                    card(key = verifyEmail.key) { row(row = verifyEmail) }
                    footer(MHCStrings.prompted_actions_footer_pending)
                }
            },
        ),
    )

    @Test
    fun `PromptedActionsScreenContent pending`() {
        // when / then
        screenshot { base.Content() }
    }

    @Test
    fun `PromptedActionsScreenContent stop suggesting`() {
        // given
        val content = base.copy(
            scaffoldState = GroveScaffoldState(
                appBar = groveAppBar {
                    close {}
                    title(MHCStrings.prompted_actions_title)
                },
                dialog = GroveAlertDialog(
                    title = StringResource(MHCStrings.prompted_actions_stop_suggesting_title),
                    message = StringResource(MHCStrings.prompted_actions_stop_suggesting_message),
                    confirmButton = GroveAlertDialog.Button(
                        title = StringResource(MHCStrings.prompted_actions_stop_suggesting_confirm),
                        style = GroveAlertDialog.Button.Style.Destructive,
                        action = {},
                    ),
                    dismissButton = GroveAlertDialog.Button(
                        title = StringResource(MHCStrings.prompted_actions_stop_suggesting_cancel),
                        action = {},
                    ),
                ),
            ),
        )

        // when / then
        screenshot { content.Content() }
    }

    @Test
    fun `PromptedActionsScreenContent all`() {
        // given
        val content = base.copy(
            sections = MutableStateFlow(
                groveSectionList {
                    section(key = "pending") {
                        card(key = demographics.key) {
                            row(row = demographics.copy(content = demographics.content.copy(onStopSuggesting = null)))
                        }
                        footer(MHCStrings.prompted_actions_footer_all)
                    }
                    section(key = "completed") {
                        title(MHCStrings.prompted_actions_completed)
                        card(key = verifyEmail.key) {
                            row(
                                row = verifyEmail.copy(
                                    content = verifyEmail.content.copy(
                                        isCompleted = true,
                                        onStopSuggesting = null,
                                    ),
                                ),
                            )
                        }
                    }
                },
            ),
        )

        // when / then
        screenshot { content.Content() }
    }
}
