//
// This source file is part of the My Heart Counts Android open-source project
//
// SPDX-FileCopyrightText: 2026 Stanford University and the project authors (see CONTRIBUTORS.md)
//
// SPDX-License-Identifier: MIT

package edu.stanford.myheartcounts.debug

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import org.grovealliance.core.viewmodel.groveViewModel

/**
 * Shows the debug sheet over the app whenever the device is shaken, in the testing environment only.
 */
@Composable
fun DebugHost() {
    val viewModel = groveViewModel<DebugViewModel>()
    val sheet by viewModel.sheet.collectAsStateWithLifecycle()
    sheet?.Sheet(modifier = Modifier)
}
