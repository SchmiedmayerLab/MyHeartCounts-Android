//
// This source file is part of the My Heart Counts Android open-source project
//
// SPDX-FileCopyrightText: 2026 Stanford University and the project authors (see CONTRIBUTORS.md)
//
// SPDX-License-Identifier: MIT

package edu.stanford.myheartcounts.account.overview

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import org.grovealliance.account.AccountOverviewScreen
import org.grovealliance.ui.ComposableContent
import org.grovealliance.ui.GroveScaffold
import org.grovealliance.ui.GroveScaffoldState

/**
 * The account overview: the account's details and sign-out, followed by the study-specific
 * [sections]. [onDismiss] is called when the overview is dismissed, including after a sign-out.
 * [scaffoldState] carries what the sections show on top of the overview, such as error toasts.
 *
 * The overview fills the screen; [Content]'s modifier is not applied.
 */
data class MHCAccountOverview(
    val sections: MHCAccountOverviewSections,
    val scaffoldState: GroveScaffoldState,
    val onDismiss: () -> Unit,
) : ComposableContent {

    @Composable
    override fun Content(modifier: Modifier) {
        GroveScaffold(state = scaffoldState) {
            AccountOverviewScreen(
                customSection = sections,
                onDismiss = onDismiss,
            )
        }
    }
}
