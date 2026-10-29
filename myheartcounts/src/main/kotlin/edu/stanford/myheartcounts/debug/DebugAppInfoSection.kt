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
import org.grovealliance.ui.ComposableContent
import org.grovealliance.ui.StringResource
import org.grovealliance.ui.theme.GroveTheme
import org.grovealliance.ui.theme.Spacings
import org.grovealliance.ui.theme.ThemePreviews

/**
 * The debug sheet's section about the installed app, the backend and the study, under its [title]:
 * one card of labelled values per group in [cards].
 */
data class DebugAppInfoSection(
    val title: DebugSectionTitle,
    val cards: List<DebugRowsCard>,
) : ComposableContent {

    @Composable
    override fun Content(modifier: Modifier) {
        Column(
            modifier = modifier,
            verticalArrangement = Arrangement.spacedBy(Spacings.medium),
        ) {
            title.Content()
            cards.forEach { it.Content(modifier = Modifier.fillMaxWidth()) }
        }
    }
}

private class DebugAppInfoSectionPreviewProvider : PreviewParameterProvider<DebugAppInfoSection> {
    private val base = DebugAppInfoSection(
        title = DebugSectionTitle(title = StringResource("App Info")),
        cards = listOf(
            DebugRowsCard(
                rows = listOf(
                    DebugRowsCard.Row(
                        label = StringResource("Version"),
                        value = "1.2.0",
                    ),
                    DebugRowsCard.Row(
                        label = StringResource("Build"),
                        value = "42",
                    ),
                ),
            ),
            DebugRowsCard(
                rows = listOf(
                    DebugRowsCard.Row(
                        label = StringResource("Account ID"),
                        value = null,
                    ),
                ),
            ),
        ),
    )

    override val values = sequenceOf(
        base,
        base.copy(cards = base.cards.take(1)),
    )
}

@ThemePreviews
@Composable
private fun Preview(@PreviewParameter(DebugAppInfoSectionPreviewProvider::class) content: DebugAppInfoSection) {
    GroveTheme { content.Content(modifier = Modifier.fillMaxWidth()) }
}
