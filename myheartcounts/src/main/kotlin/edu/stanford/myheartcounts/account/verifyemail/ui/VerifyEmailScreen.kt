//
// This source file is part of the My Heart Counts Android open-source project
//
// SPDX-FileCopyrightText: 2026 Stanford University and the project authors (see CONTRIBUTORS.md)
//
// SPDX-License-Identifier: MIT

package edu.stanford.myheartcounts.account.verifyemail.ui

import androidx.compose.runtime.Composable
import edu.stanford.myheartcounts.account.verifyemail.VerifyEmailViewModel
import org.grovealliance.core.viewmodel.groveViewModel

/**
 * Whether the participant's email address is verified and, until it is, how to verify it.
 */
@Composable
fun VerifyEmailScreen() {
    val viewModel = groveViewModel<VerifyEmailViewModel>()
    viewModel.screen.Content()
}
