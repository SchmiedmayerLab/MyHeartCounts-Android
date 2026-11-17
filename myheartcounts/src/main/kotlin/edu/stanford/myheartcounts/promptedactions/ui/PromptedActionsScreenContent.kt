//
// This source file is part of the My Heart Counts Android open-source project
//
// SPDX-FileCopyrightText: 2026 Stanford University and the project authors (see CONTRIBUTORS.md)
//
// SPDX-License-Identifier: MIT

package edu.stanford.myheartcounts.promptedactions.ui

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.PreviewParameter
import androidx.compose.ui.tooling.preview.PreviewParameterProvider
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import org.grovealliance.ui.ComposableContent
import org.grovealliance.ui.GroveScaffold
import org.grovealliance.ui.GroveScaffoldState
import org.grovealliance.ui.GroveSectionList
import org.grovealliance.ui.StringResource
import org.grovealliance.ui.groveAppBar
import org.grovealliance.ui.groveSectionList
import org.grovealliance.ui.theme.GroveTheme
import org.grovealliance.ui.theme.ThemePreviews

/**
 * The prompted actions in their [sections] beneath the app bar, following their changes.
 */
data class PromptedActionsScreenContent(
    val scaffoldState: GroveScaffoldState,
    val sections: StateFlow<GroveSectionList>,
) : ComposableContent {

    @Composable
    override fun Content(modifier: Modifier) {
        val sections by sections.collectAsStateWithLifecycle()
        GroveScaffold(state = scaffoldState) {
            sections.Content(modifier = modifier)
        }
    }
}

private class PromptedActionsScreenContentPreviewProvider : PreviewParameterProvider<PromptedActionsScreenContent> {
    private val base = PromptedActionsScreenContent(
        scaffoldState = GroveScaffoldState(
            appBar = groveAppBar {
                close {}
                title(StringResource("Suggested for You"))
            },
        ),
        sections = MutableStateFlow(groveSectionList {}),
    )
    override val values = sequenceOf(base)
}

@ThemePreviews
@Composable
private fun Preview(
    @PreviewParameter(PromptedActionsScreenContentPreviewProvider::class) content: PromptedActionsScreenContent,
) {
    GroveTheme { content.Content() }
}
