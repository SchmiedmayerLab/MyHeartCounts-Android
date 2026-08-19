//
// This source file is part of the My Heart Counts Android open-source project
//
// SPDX-FileCopyrightText: 2026 Stanford University and the project authors (see CONTRIBUTORS.md)
//
// SPDX-License-Identifier: MIT

package org.grovealliance.ui.account

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier

/**
 * A composable that can be used to display a value of type [Value] to be used for displaying account keys or values in the UI.
 */
interface DataDisplayComposable<Value> {

    /**
     * Displays the given [value] of type [Value] in the UI
     */
    @Composable
    fun Content(
        value: Value,
        modifier: Modifier = Modifier,
    )
}

/**
 * A composable that can be used to enter or edit a value of type [Value] to be used for displaying account keys or values in the UI.
 */
interface DataEntryComposable<Value> {

    /**
     * Displays an input field for the given [value] of type [Value] in the UI and calls [onValueChange] when the value changes.
     */
    @Composable
    fun Content(
        value: Value,
        onValueChange: (Value) -> Unit,
        modifier: Modifier = Modifier,
    )
}
