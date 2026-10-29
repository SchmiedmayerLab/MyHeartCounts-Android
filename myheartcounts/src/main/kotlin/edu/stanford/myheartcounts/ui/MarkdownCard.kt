//
// This source file is part of the My Heart Counts Android open-source project
//
// SPDX-FileCopyrightText: 2026 Stanford University and the project authors (see CONTRIBUTORS.md)
//
// SPDX-License-Identifier: MIT

package edu.stanford.myheartcounts.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.ui.Modifier
import org.grovealliance.markdown.MarkdownBlockView
import org.grovealliance.markdown.MarkdownDocument
import org.grovealliance.ui.ComposableContent
import org.grovealliance.ui.GroveCard
import org.grovealliance.ui.theme.GroveShapes
import org.grovealliance.ui.theme.Spacings

/**
 * A card rendering [document] in full, sized to its content so it can sit in a scrolling page.
 */
@Immutable
data class MarkdownCard(
    val document: MarkdownDocument,
) : ComposableContent {

    @Composable
    override fun Content(modifier: Modifier) {
        GroveCard(
            modifier = modifier,
            shape = GroveShapes.large,
        ) {
            Column(
                modifier = Modifier.padding(Spacings.medium),
                verticalArrangement = Arrangement.spacedBy(Spacings.medium),
            ) {
                document.blocks.forEach { block -> MarkdownBlockView(block = block) }
            }
        }
    }
}
