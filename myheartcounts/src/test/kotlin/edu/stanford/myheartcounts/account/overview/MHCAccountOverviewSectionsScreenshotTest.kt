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
import edu.stanford.myheartcounts.account.overview.WithdrawFromStudySection.WithdrawalState
import edu.stanford.myheartcounts.ui.ToggleSection
import kotlinx.coroutines.flow.MutableStateFlow
import org.grovealliance.testing.screenshot.ScreenshotTest
import org.grovealliance.ui.AsyncTextButton
import org.grovealliance.ui.StaticGroveScaffold
import org.grovealliance.ui.StringResource
import org.grovealliance.ui.account.AccountOverviewItem
import org.grovealliance.ui.account.AccountOverviewLayout
import org.grovealliance.ui.account.AccountOverviewSection
import org.grovealliance.ui.account.AccountProfileHeader
import org.grovealliance.ui.account.StringDataDisplay
import org.grovealliance.ui.groveAppBar
import org.grovealliance.ui.theme.Colors
import org.junit.Test

class MHCAccountOverviewSectionsScreenshotTest : ScreenshotTest() {

    @Test
    fun `MHCAccountOverviewSections in the account overview screenshot`() {
        screenshot {
            StaticGroveScaffold(appBar = groveAppBar { title("Account Overview") }) {
                overview(state = WithdrawalState.Idle).Content(modifier = Modifier.fillMaxSize())
            }
        }
    }

    @Test
    fun `MHCAccountOverviewSections while withdrawing screenshot`() {
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
        sections = listOf(
            AccountOverviewSection(
                title = StringResource("NAME"),
                items = listOf(
                    AccountOverviewItem(
                        title = StringResource("Name"),
                        valueDisplay = StringDataDisplay(),
                        value = "Leland Stanford",
                        leadingImage = null,
                        showArrow = true,
                        onClick = {},
                    ),
                ),
            ),
        ),
        customSection = MHCAccountOverviewSections(
            items = listOf(
                ToggleSection(
                    title = StringResource(MHCStrings.account_post_trial_nudges_title),
                    subtitle = StringResource(MHCStrings.account_post_trial_nudges_subtitle),
                    isOn = MutableStateFlow(value = true),
                    onToggle = {},
                ),
                SupportSection(onSendFeedbackSelected = {}),
                AppInfoSection(
                    version = "1.2.0",
                    onPrivacyPolicySelected = {},
                ),
                WithdrawFromStudySection(
                    state = MutableStateFlow(value = state),
                    onWithdrawSelected = {},
                    onWithdrawConfirmed = {},
                    onDismissed = {},
                ),
            ),
        ),
        logout = AsyncTextButton(
            title = StringResource("Logout"),
            containerColor = { Colors.error },
            action = {},
        ),
    )
}
