//
// This source file is part of the My Heart Counts Android open-source project
//
// SPDX-FileCopyrightText: 2026 Stanford University and the project authors (see CONTRIBUTORS.md)
//
// SPDX-License-Identifier: MIT

package org.grovealliance.consent.internal

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import org.grovealliance.markdown.MarkdownBlock
import org.grovealliance.markdown.MarkdownTextBlock
import org.grovealliance.ui.ComposableContent

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
