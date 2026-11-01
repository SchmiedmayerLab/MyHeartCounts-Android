//
// This source file is part of the My Heart Counts Android open-source project
//
// SPDX-FileCopyrightText: 2026 Stanford University and the project authors (see CONTRIBUTORS.md)
// SPDX-License-Identifier: MIT
//

package edu.stanford.myheartcounts.navigation

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.ui.Modifier
import androidx.navigation3.runtime.NavEntry
import org.grovealliance.ui.BottomSheetComposableContent

/**
 * A navigation destination presented in a dismissible sheet while retaining its entry scope.
 */
@Immutable
internal data class NavigationSheet<T : Any>(
    val entry: NavEntry<T>,
    override val onDismiss: () -> Unit,
) : BottomSheetComposableContent {
    override val skipPartiallyExpanded: Boolean = true
    override val dragHandle: (@Composable () -> Unit)? = null

    @Composable
    override fun Content(modifier: Modifier) {
        Box(modifier = modifier.fillMaxWidth().fillMaxHeight()) { entry.Content() }
    }
}
