//
// This source file is part of the My Heart Counts Android open-source project
//
// SPDX-FileCopyrightText: 2026 Stanford University and the project authors (see CONTRIBUTORS.md)
//
// SPDX-License-Identifier: MIT

package edu.stanford.myheartcounts.account.overview

import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.ui.Modifier
import edu.stanford.myheartcounts.MHCStrings
import edu.stanford.myheartcounts.account.overview.WithdrawFromStudyButton.WithdrawalState
import kotlinx.coroutines.flow.MutableStateFlow
import org.grovealliance.testing.screenshot.ScreenshotTest
import org.grovealliance.ui.AsyncTextButton
import org.grovealliance.ui.GroveAlertDialog
import org.grovealliance.ui.StaticGroveScaffold
import org.grovealliance.ui.StringResource
import org.grovealliance.ui.account.AccountOverviewItem
import org.grovealliance.ui.account.AccountOverviewLayout
import org.grovealliance.ui.account.AccountProfileHeader
import org.grovealliance.ui.account.CustomAccountSections
import org.grovealliance.ui.account.StringDataDisplay
import org.grovealliance.ui.groveAppBar
import org.grovealliance.ui.groveSection
import org.grovealliance.ui.groveSections
import org.grovealliance.ui.theme.Colors
import org.junit.Test

class MHCAccountOverviewScreenshotTest : ScreenshotTest() {

    private val studyParticipation = StudyParticipationSectionFixtures.create()

    @Test
    fun `MHCAccountOverview screenshot`() {
        screenshot {
            StaticGroveScaffold(appBar = groveAppBar { title("Account Overview") }) {
                overview(state = WithdrawalState.Idle).Content(modifier = Modifier.fillMaxSize())
            }
        }
    }

    @Test
    fun `MHCAccountOverview while withdrawing screenshot`() {
        screenshot {
            StaticGroveScaffold(appBar = groveAppBar { title("Account Overview") }) {
                overview(state = WithdrawalState.Withdrawing).Content(modifier = Modifier.fillMaxSize())
            }
        }
    }

    private fun overview(state: WithdrawalState) = AccountOverviewLayout(
        title = StringResource("Account Overview"),
        header = AccountProfileHeader(
            initials = "LS",
            name = "Leland Stanford",
            description = "lelandstanford@stanford.edu",
        ),
        sections = groveSections {
            section(key = "name") {
                title("Name")
                card(key = "items") {
                    row(
                        key = "0",
                        content = AccountOverviewItem(
                            title = StringResource("Name"),
                            valueDisplay = StringDataDisplay(),
                            value = "Leland Stanford",
                            leadingImage = null,
                            showArrow = true,
                            onClick = {},
                        ),
                    )
                }
            }
        },
        customSections = CustomAccountSections(
            sections = listOf(
                studyParticipation,
                StudyParticipationSectionFixtures.createPostTrialNudges(),
                SupportSectionFixtures.create(),
                AppInfoSectionFixtures.create(),
                withdrawFromStudy(state = state),
            ),
        ),
        logout = AsyncTextButton(
            title = StringResource("Logout"),
            containerColor = { Colors.error },
            action = {},
        ),
    )

    private fun withdrawFromStudy(state: WithdrawalState) = groveSection(key = "withdraw-from-study") {
        content(
            key = "withdraw-from-study",
            content = WithdrawFromStudyButton(
                state = MutableStateFlow(value = state),
                onWithdrawSelected = {},
                confirmation = GroveAlertDialog(
                    title = StringResource(MHCStrings.account_withdraw_confirm_title),
                    message = StringResource(MHCStrings.account_withdraw_confirm_message),
                    confirmButton = GroveAlertDialog.Button(
                        title = StringResource(MHCStrings.account_withdraw_confirm_action),
                        action = {},
                    ),
                ),
                failure = GroveAlertDialog(
                    title = StringResource(MHCStrings.account_withdraw_failed_title),
                    message = StringResource(MHCStrings.account_withdraw_failed_message),
                    confirmButton = GroveAlertDialog.Button(
                        title = StringResource(MHCStrings.account_withdraw_failed_dismiss),
                        action = {},
                    ),
                ),
            ),
        )
    }
}
