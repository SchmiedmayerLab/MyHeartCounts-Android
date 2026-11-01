//
// This source file is part of the My Heart Counts Android open-source project
//
// SPDX-FileCopyrightText: 2026 Stanford University and the project authors (see CONTRIBUTORS.md)
//
// SPDX-License-Identifier: MIT

package edu.stanford.myheartcounts.stats

import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.PreviewParameter
import androidx.compose.ui.tooling.preview.PreviewParameterProvider
import org.grovealliance.ui.ComposableContent
import org.grovealliance.ui.StringResource
import org.grovealliance.ui.theme.Colors
import org.grovealliance.ui.theme.GroveTheme
import org.grovealliance.ui.theme.Spacings
import org.grovealliance.ui.theme.TextStyles
import org.grovealliance.ui.theme.ThemePreviews

/**
 * What the participation stats cover, as scrolling body text.
 */
data class ParticipationStatsExplainer(
    val text: StringResource,
) : ComposableContent {

    @Composable
    override fun Content(modifier: Modifier) {
        Text(
            text = text.text(),
            style = TextStyles.bodyLarge,
            color = Colors.onSurface,
            modifier = modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(Spacings.medium),
        )
    }
}

private class ParticipationStatsExplainerPreviewProvider : PreviewParameterProvider<ParticipationStatsExplainer> {
    private val base = ParticipationStatsExplainer(
        text = StringResource("Participation Stats cover all activity recorded since you enrolled."),
    )

    override val values = sequenceOf(base)
}

@ThemePreviews
@Composable
private fun Preview(
    @PreviewParameter(ParticipationStatsExplainerPreviewProvider::class) content: ParticipationStatsExplainer,
) {
    GroveTheme {
        content.Content()
    }
}
