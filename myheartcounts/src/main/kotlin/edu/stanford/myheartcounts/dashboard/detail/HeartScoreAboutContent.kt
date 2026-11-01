//
// This source file is part of the My Heart Counts Android open-source project
//
// SPDX-FileCopyrightText: 2026 Stanford University and the project authors (see CONTRIBUTORS.md)
//
// SPDX-License-Identifier: MIT

package edu.stanford.myheartcounts.dashboard.detail

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.HorizontalDivider
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import edu.stanford.myheartcounts.ui.ExternalLink
import org.grovealliance.markdown.MarkdownBlockView
import org.grovealliance.markdown.MarkdownDocument
import org.grovealliance.ui.ComposableContent
import org.grovealliance.ui.GroveCard
import org.grovealliance.ui.theme.GroveShapes
import org.grovealliance.ui.theme.Spacings

/**
 * The study's explanation of a factor, followed by its links, each set off by a divider.
 */
data class HeartScoreAboutContent(
    val document: MarkdownDocument,
    val links: List<ExternalLink>,
) : ComposableContent {
    @Composable
    override fun Content(modifier: Modifier) {
        GroveCard(modifier = modifier, shape = GroveShapes.large) {
            Column(
                modifier = Modifier.padding(Spacings.medium),
                verticalArrangement = Arrangement.spacedBy(Spacings.medium),
            ) {
                document.blocks.forEach { MarkdownBlockView(block = it) }
                links.forEach {
                    HorizontalDivider()
                    it.Content(modifier = Modifier.fillMaxWidth())
                }
            }
        }
    }
}
