//
// This source file is part of the My Heart Counts Android open-source project
//
// SPDX-FileCopyrightText: 2026 Stanford University and the project authors (see CONTRIBUTORS.md)
//
// SPDX-License-Identifier: MIT

package edu.stanford.myheartcounts.ui

import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.PreviewParameter
import androidx.compose.ui.tooling.preview.PreviewParameterProvider
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import org.grovealliance.ui.AsyncTextButton
import org.grovealliance.ui.ComposableContent
import org.grovealliance.ui.StringResource
import org.grovealliance.ui.theme.Colors
import org.grovealliance.ui.theme.GroveTheme
import org.grovealliance.ui.theme.ThemePreviews

/**
 * A text action for an app bar, such as Save. It shows progress while [action] runs and can't be
 * tapped again until it returns, nor while not [enabled].
 *
 * @param title the action's label
 * @param enabled whether the action can be tapped
 * @param action what the action does
 */
@Immutable
data class AppBarTextAction(
    val title: StringResource,
    val enabled: StateFlow<Boolean>,
    val action: suspend () -> Unit,
) : ComposableContent {

    @Composable
    override fun Content(modifier: Modifier) {
        val enabled by enabled.collectAsStateWithLifecycle()
        val scope = rememberCoroutineScope()
        var isLoading by remember { mutableStateOf(false) }

        AsyncTextButton(
            modifier = modifier,
            text = title.text(),
            enabled = enabled,
            isLoading = isLoading,
            containerColor = Colors.transparent,
            textColor = if (enabled) Colors.primary else Colors.onSurfaceVariant,
            onClick = {
                if (isLoading) return@AsyncTextButton
                isLoading = true
                scope.launch {
                    try {
                        action()
                    } finally {
                        isLoading = false
                    }
                }
            },
        )
    }
}

private class AppBarTextActionPreviewProvider : PreviewParameterProvider<AppBarTextAction> {
    private val base = AppBarTextAction(
        title = StringResource("Save"),
        enabled = MutableStateFlow(true),
        action = {},
    )
    override val values = sequenceOf(
        base,
        base.copy(enabled = MutableStateFlow(false)),
    )
}

@ThemePreviews
@Composable
private fun Preview(@PreviewParameter(AppBarTextActionPreviewProvider::class) content: AppBarTextAction) {
    GroveTheme { content.Content() }
}
