//
// This source file is part of the My Heart Counts Android open-source project
//
// SPDX-FileCopyrightText: 2026 Stanford University and the project authors (see CONTRIBUTORS.md)
//
// SPDX-License-Identifier: MIT

package edu.stanford.myheartcounts.consentforms.ui

import androidx.compose.runtime.Composable
import edu.stanford.myheartcounts.consentforms.SignedConsentFormsViewModel
import org.grovealliance.core.viewmodel.groveViewModel

/**
 * The consent forms the participant has signed, each of which can be opened and shared.
 */
@Composable
fun SignedConsentFormsScreen() {
    val viewModel = groveViewModel<SignedConsentFormsViewModel>()
    viewModel.screen.Content()
}
