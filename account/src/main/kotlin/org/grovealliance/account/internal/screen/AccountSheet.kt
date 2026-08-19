//
// This source file is part of the My Heart Counts Android open-source project
//
// SPDX-FileCopyrightText: 2026 Stanford University and the project authors (see CONTRIBUTORS.md)
//
// SPDX-License-Identifier: MIT

package org.grovealliance.account.internal.screen

import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import kotlinx.coroutines.flow.Flow
import org.grovealliance.ui.BottomSheetComposableContent
import org.grovealliance.ui.ComposableContent
import org.grovealliance.ui.GroveScaffold
import org.grovealliance.ui.GroveScaffoldState
import org.grovealliance.ui.OnActionVoid

/**
 * Base bottom sheet for account screens that renders a reactive layout driven by a [Flow] of [ComposableContent].
 *
 * @param scaffoldState Drives the app bar, toasts, and nested bottom sheets.
 * @param layout Emits the content to render as the sheet body.
 * @param onDismiss Called when the sheet is dismissed.
 * @param draggable Whether the sheet can be dismissed by dragging. Defaults to `true`.
 */
internal data class AccountSheet(
    val scaffoldState: GroveScaffoldState,
    val layout: Flow<ComposableContent>,
    override val onDismiss: OnActionVoid,
    private val draggable: Boolean = true,
) : BottomSheetComposableContent {

    override val isDraggable: Boolean get() = draggable

    override val onConfirmSheetState: (BottomSheetComposableContent.State) -> Boolean
        get() = { state ->
            if (!isDraggable) {
                state == BottomSheetComposableContent.State.Expanded
            } else {
                true
            }
        }

    @Composable
    override fun Content(modifier: Modifier) {
        GroveScaffold(
            state = scaffoldState,
            content = {
                val currentLayout by layout.collectAsStateWithLifecycle(initialValue = null)
                currentLayout?.Content(modifier = Modifier.fillMaxSize())
            },
        )
    }
}
