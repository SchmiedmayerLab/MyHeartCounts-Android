//
// This source file is part of the My Heart Counts Android open-source project
//
// SPDX-FileCopyrightText: 2026 Stanford University and the project authors (see CONTRIBUTORS.md)
// SPDX-License-Identifier: MIT
//

package edu.stanford.myheartcounts.dashboard.entry

import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.ui.Modifier
import org.grovealliance.ui.ComposableContent
import org.grovealliance.ui.GroveScaffold
import org.grovealliance.ui.GroveScaffoldState

/**
 * A measurement form inside its scaffold.
 */
@Immutable
data class HeartScoreEntryScreenContent(
    val scaffold: GroveScaffoldState,
    val layout: EntryFormLayout,
) : ComposableContent {
    @Composable
    override fun Content(modifier: Modifier) {
        GroveScaffold(state = scaffold) { layout.Content(modifier = modifier) }
    }
}
