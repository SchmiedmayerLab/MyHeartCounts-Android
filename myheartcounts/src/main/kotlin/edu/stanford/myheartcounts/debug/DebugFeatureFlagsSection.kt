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
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.PreviewParameter
import androidx.compose.ui.tooling.preview.PreviewParameterProvider
import edu.stanford.myheartcounts.ui.ToggleSection
import kotlinx.coroutines.flow.MutableStateFlow
import org.grovealliance.ui.AsyncTextButton
import org.grovealliance.ui.ComposableContent
import org.grovealliance.ui.StringResource
import org.grovealliance.ui.theme.Colors
import org.grovealliance.ui.theme.GroveTheme
import org.grovealliance.ui.theme.Spacings
import org.grovealliance.ui.theme.TextStyles
import org.grovealliance.ui.theme.ThemePreviews

/**
 * The debug sheet's section of feature flags, under its [title]: one switch per flag in [flags],
 * a [note] on when changes apply, and a [restartButton] that applies them.
 */
data class DebugFeatureFlagsSection(
    val title: DebugSectionTitle,
    val flags: List<ToggleSection>,
    val note: StringResource,
    val restartButton: AsyncTextButton,
) : ComposableContent {

    @Composable
    override fun Content(modifier: Modifier) {
        Column(
            modifier = modifier,
            verticalArrangement = Arrangement.spacedBy(Spacings.medium),
        ) {
            title.Content()
            flags.forEach { it.Content(modifier = Modifier.fillMaxWidth()) }
            Text(
                text = note.text(),
                style = TextStyles.bodySmall,
                color = Colors.onSurfaceVariant,
            )
            restartButton.Content(modifier = Modifier.fillMaxWidth())
        }
    }
}

private class DebugFeatureFlagsSectionPreviewProvider : PreviewParameterProvider<DebugFeatureFlagsSection> {
    private val base = DebugFeatureFlagsSection(
        title = DebugSectionTitle(title = StringResource("Feature Flags")),
        flags = listOf(
            ToggleSection(
                title = StringResource("Simplify Onboarding"),
                subtitle = StringResource("Shortens onboarding to login, Health Connect access and enrollment."),
                isOn = MutableStateFlow(value = true),
                onToggle = {},
            ),
        ),
        note = StringResource("Changes apply once the app restarts."),
        restartButton = AsyncTextButton(
            title = StringResource("Restart"),
            action = {},
        ),
    )

    override val values = sequenceOf(
        base,
        base.copy(flags = emptyList()),
    )
}

@ThemePreviews
@Composable
private fun Preview(
    @PreviewParameter(DebugFeatureFlagsSectionPreviewProvider::class) content: DebugFeatureFlagsSection,
) {
    GroveTheme { content.Content(modifier = Modifier.fillMaxWidth()) }
}
