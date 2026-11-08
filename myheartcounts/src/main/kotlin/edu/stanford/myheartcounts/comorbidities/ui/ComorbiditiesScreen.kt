//
// This source file is part of the My Heart Counts Android open-source project
//
// SPDX-FileCopyrightText: 2026 Stanford University and the project authors (see CONTRIBUTORS.md)
//
// SPDX-License-Identifier: MIT

package edu.stanford.myheartcounts.comorbidities.ui

import androidx.compose.runtime.Composable
import edu.stanford.myheartcounts.comorbidities.ComorbiditiesViewModel
import org.grovealliance.core.viewmodel.groveViewModel

/**
 * The participant's comorbidities, edited on a screen of their own.
 */
@Composable
fun ComorbiditiesScreen() {
    val viewModel = groveViewModel<ComorbiditiesViewModel>()
    viewModel.screen.Content()
}
