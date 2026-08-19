//
// This source file is part of the My Heart Counts Android open-source project
//
// SPDX-FileCopyrightText: 2026 Stanford University and the project authors (see CONTRIBUTORS.md)
//
// SPDX-License-Identifier: MIT

package org.grovealliance.ui.account

import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import kotlinx.coroutines.flow.Flow
import org.grovealliance.resources.Strings
import org.grovealliance.ui.ComposableContent

/**
 * A button that toggles between "Edit" and "Done" labels depending on the current edit state,
 * and is only shown when [visible] emits `true`.
 *
 * @param visible Controls whether the button is shown.
 * @param isEditMode Drives the displayed label.
 * @param onClick Invoked when the button is tapped.
 */
data class AccountEditButton(
    val visible: Flow<Boolean>,
    val isEditMode: Flow<Boolean>,
    val onClick: () -> Unit,
) : ComposableContent {

    @Composable
    override fun Content(modifier: Modifier) {
        val isVisible by visible.collectAsStateWithLifecycle(initialValue = false)
        if (isVisible) {
            TextButton(modifier = modifier, onClick = onClick) {
                val inEditMode by isEditMode.collectAsStateWithLifecycle(initialValue = false)
                Text(
                    text = if (inEditMode) {
                        stringResource(Strings.account_overview_done_button)
                    } else {
                        stringResource(Strings.account_overview_edit_button)
                    }
                )
            }
        }
    }
}
