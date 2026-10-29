//
// This source file is part of the My Heart Counts Android open-source project
//
// SPDX-FileCopyrightText: 2026 Stanford University and the project authors (see CONTRIBUTORS.md)
//
// SPDX-License-Identifier: MIT

package edu.stanford.myheartcounts.debug

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.PreviewParameter
import androidx.compose.ui.tooling.preview.PreviewParameterProvider
import edu.stanford.myheartcounts.ui.ToggleSection
import kotlinx.coroutines.flow.MutableStateFlow
import org.grovealliance.ui.ComposableContent
import org.grovealliance.ui.StringResource
import org.grovealliance.ui.theme.GroveTheme
import org.grovealliance.ui.theme.Spacings
import org.grovealliance.ui.theme.ThemePreviews

/**
 * The debug sheet's section about the sheet itself, under its [title]: the [toggle] deciding
 * whether shaking the device opens it.
 */
data class DebugShakeSection(
    val title: DebugSectionTitle,
    val toggle: ToggleSection,
) : ComposableContent {

    @Composable
    override fun Content(modifier: Modifier) {
        Column(
            modifier = modifier,
            verticalArrangement = Arrangement.spacedBy(Spacings.medium),
        ) {
            title.Content()
            toggle.Content(modifier = Modifier.fillMaxWidth())
        }
    }
}

private class DebugShakeSectionPreviewProvider : PreviewParameterProvider<DebugShakeSection> {
    private val base = DebugShakeSection(
        title = DebugSectionTitle(title = StringResource("Debug Sheet")),
        toggle = ToggleSection(
            title = StringResource("Open on Shake"),
            subtitle = StringResource(
                "Does this sheet keep popping up while you're running or moving? " +
                    "Turn this off to stop shaking from opening it. To turn it back on, relaunch the app.",
            ),
            isOn = MutableStateFlow(value = true),
            onToggle = {},
        ),
    )

    override val values = sequenceOf(base)
}

@ThemePreviews
@Composable
private fun Preview(@PreviewParameter(DebugShakeSectionPreviewProvider::class) content: DebugShakeSection) {
    GroveTheme { content.Content(modifier = Modifier.fillMaxWidth()) }
}
