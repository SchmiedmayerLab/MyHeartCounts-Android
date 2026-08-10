//
// This source file is part of the My Heart Counts Android open-source project
//
// SPDX-FileCopyrightText: 2026 Stanford University and the project authors (see CONTRIBUTORS.md)
//
// SPDX-License-Identifier: MIT

package edu.stanford.myheartcounts.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import edu.stanford.spezi.ui.ComposableContent
import edu.stanford.spezi.ui.SpeziScaffold
import edu.stanford.spezi.ui.SpeziScaffoldState
import edu.stanford.spezi.ui.StringResource
import edu.stanford.spezi.ui.theme.Colors

/**
 * A placeholder screen rendering [title] centered within the scaffold.
 */
data class PlaceholderScreenContent(
    val scaffoldState: SpeziScaffoldState,
    val title: StringResource,
) : ComposableContent {
    @Composable
    override fun Content(modifier: Modifier) {
        SpeziScaffold(
            state = scaffoldState,
        ) {
            Box(
                modifier = modifier
                    .fillMaxSize()
                    .background(Colors.primaryContainer),
                contentAlignment = Alignment.Center,
            ) {
                Text(text = title.text())
            }
        }
    }
}
