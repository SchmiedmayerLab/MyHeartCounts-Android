//
// This source file is part of the My Heart Counts Android open-source project
//
// SPDX-FileCopyrightText: 2026 Stanford University and the project authors (see CONTRIBUTORS.md)
//
// SPDX-License-Identifier: MIT

package org.grovealliance.ui

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import org.grovealliance.ui.theme.GroveShapes
import org.grovealliance.ui.theme.Spacings

/**
 * A [ComposableContent] with additional capability to render itself in as a model dialog
 */
interface DialogComposableContent : ComposableContent {
    val onDismiss: () -> Unit
    val dialogProperties: DialogProperties
        get() = DialogProperties()

    @Composable
    fun DialogContent() {
        Dialog(
            onDismissRequest = onDismiss,
            properties = dialogProperties,
        ) {
            Surface(
                shape = GroveShapes.medium,
                modifier = Modifier.fillMaxWidth()
            ) {
                Box(modifier = Modifier.padding(Spacings.medium)) {
                    Content()
                }
            }
        }
    }
}
