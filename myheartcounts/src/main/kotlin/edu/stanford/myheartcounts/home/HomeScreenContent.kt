//
// This source file is part of the My Heart Counts Android open-source project
//
// SPDX-FileCopyrightText: 2026 Stanford University and the project authors (see CONTRIBUTORS.md)
//
// SPDX-License-Identifier: MIT

package edu.stanford.myheartcounts.home

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import kotlinx.coroutines.flow.StateFlow
import org.grovealliance.ui.ComposableContent
import org.grovealliance.ui.GroveScaffold
import org.grovealliance.ui.GroveScaffoldState

/**
 * The Home tab's root content, rendering whichever layout the current [state] carries.
 */
data class HomeScreenContent(
    val scaffoldState: GroveScaffoldState,
    val state: StateFlow<HomeLayoutState>,
) : ComposableContent {
    @Composable
    override fun Content(modifier: Modifier) {
        GroveScaffold(state = scaffoldState) {
            val layoutState by state.collectAsStateWithLifecycle()
            layoutState.layout.Content(modifier)
        }
    }
}
