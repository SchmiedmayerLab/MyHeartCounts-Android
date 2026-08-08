//
// This source file is part of the My Heart Counts open-source project
//
// SPDX-FileCopyrightText: 2026 Stanford University and the project authors (see CONTRIBUTORS.md)
//
// SPDX-License-Identifier: MIT

package edu.stanford.myheartcounts.home

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import edu.stanford.spezi.ui.ComposableContent
import edu.stanford.spezi.ui.SpeziScaffold
import edu.stanford.spezi.ui.SpeziScaffoldState
import kotlinx.coroutines.flow.StateFlow

/**
 * The Home tab's root content, rendering whichever layout the current [state] carries.
 */
data class HomeScreenContent(
    val scaffoldState: SpeziScaffoldState,
    val state: StateFlow<HomeLayoutState>,
) : ComposableContent {
    @Composable
    override fun Content(modifier: Modifier) {
        SpeziScaffold(state = scaffoldState) {
            val layoutState by state.collectAsStateWithLifecycle()
            layoutState.layout.Content(modifier)
        }
    }
}
