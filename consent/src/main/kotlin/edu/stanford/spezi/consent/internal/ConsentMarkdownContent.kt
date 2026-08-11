//
// This source file is part of the My Heart Counts Android open-source project
//
// SPDX-FileCopyrightText: 2026 Stanford University and the project authors (see CONTRIBUTORS.md)
//
// SPDX-License-Identifier: MIT

package edu.stanford.spezi.consent.internal

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import edu.stanford.spezi.markdown.MarkdownBlock
import edu.stanford.spezi.markdown.MarkdownTextBlock
import edu.stanford.spezi.ui.ComposableContent

/**
 * Renders a plain markdown block within the consent form.
 */
internal data class ConsentMarkdownContent(
    private val block: MarkdownBlock.Markdown,
) : ComposableContent {

    @Composable
    override fun Content(modifier: Modifier) {
        MarkdownTextBlock(
            modifier = modifier,
            block = block,
        )
    }
}
