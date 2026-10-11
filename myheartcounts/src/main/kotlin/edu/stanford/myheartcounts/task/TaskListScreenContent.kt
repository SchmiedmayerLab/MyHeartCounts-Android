//
// This source file is part of the My Heart Counts Android open-source project
//
// SPDX-FileCopyrightText: 2026 Stanford University and the project authors (see CONTRIBUTORS.md)
//
// SPDX-License-Identifier: MIT

package edu.stanford.myheartcounts.task

import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import kotlinx.coroutines.flow.StateFlow
import org.grovealliance.ui.ComposableContent
import org.grovealliance.ui.GroveScaffold
import org.grovealliance.ui.GroveScaffoldState
import org.grovealliance.ui.theme.Spacings

/**
 * A screen listing the participant's tasks, rendering whichever layout the current [state] carries;
 * the list scrolls.
 */
data class TaskListScreenContent(
    val scaffoldState: GroveScaffoldState,
    val state: StateFlow<TaskListScreenState>,
) : ComposableContent {
    @Composable
    override fun Content(modifier: Modifier) {
        GroveScaffold(state = scaffoldState) {
            val layoutState by state.collectAsStateWithLifecycle()
            when (val current = layoutState) {
                is TaskListScreenState.Content -> current.layout.Content(
                    modifier = modifier
                        .fillMaxSize()
                        .verticalScroll(rememberScrollState())
                        .padding(Spacings.medium),
                )
                else -> current.layout.Content(modifier)
            }
        }
    }
}
