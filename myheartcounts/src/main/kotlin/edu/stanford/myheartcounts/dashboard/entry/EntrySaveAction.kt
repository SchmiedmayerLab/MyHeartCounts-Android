//
// This source file is part of the My Heart Counts Android open-source project
//
// SPDX-FileCopyrightText: 2026 Stanford University and the project authors (see CONTRIBUTORS.md)
// SPDX-License-Identifier: MIT
//

package edu.stanford.myheartcounts.dashboard.entry

import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.PreviewParameter
import androidx.compose.ui.tooling.preview.PreviewParameterProvider
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import org.grovealliance.ui.AsyncTextButton
import org.grovealliance.ui.ComposableContent
import org.grovealliance.ui.StringResource
import org.grovealliance.ui.theme.Colors
import org.grovealliance.ui.theme.GroveTheme
import org.grovealliance.ui.theme.ThemePreviews

/**
 * A toolbar save action using the shared asynchronous text button.
 *
 * @param title the action label
 * @param isEnabled whether the action is interactive
 * @param isLoading whether to show save progress
 * @param onClick called when the action is tapped
 */
@Immutable
data class EntrySaveAction(
    val title: StringResource,
    val isEnabled: StateFlow<Boolean>,
    val isLoading: StateFlow<Boolean>,
    val onClick: () -> Unit,
) : ComposableContent {
    @Composable
    override fun Content(modifier: Modifier) {
        val enabled by isEnabled.collectAsStateWithLifecycle()
        val loading by isLoading.collectAsStateWithLifecycle()
        AsyncTextButton(
            text = title.text(),
            enabled = enabled,
            isLoading = loading,
            containerColor = Colors.transparent,
            textColor = if (enabled) Colors.primary else Colors.onSurfaceVariant,
            onClick = onClick,
            modifier = modifier,
        )
    }
}

private class EntrySaveActionPreviewProvider : PreviewParameterProvider<EntrySaveAction> {
    private val base = EntrySaveAction(
        title = StringResource("Save"),
        isEnabled = MutableStateFlow(true),
        isLoading = MutableStateFlow(false),
        onClick = {},
    )
    override val values = sequenceOf(base)
}

@ThemePreviews
@Composable
private fun Preview(@PreviewParameter(EntrySaveActionPreviewProvider::class) content: EntrySaveAction) {
    GroveTheme { content.Content() }
}
