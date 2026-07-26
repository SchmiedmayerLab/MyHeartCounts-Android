//
// This source file is part of the My Heart Counts open-source project
//
// SPDX-FileCopyrightText: 2026 Stanford University and the project authors (see CONTRIBUTORS.md)
//
// SPDX-License-Identifier: MIT

@file:Suppress("MagicNumber")

package edu.stanford.spezi.markdown

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.LinkAnnotation
import androidx.compose.ui.text.LinkInteractionListener
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.TextLinkStyles
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextDecoration
import edu.stanford.spezi.ui.theme.Colors
import edu.stanford.spezi.ui.theme.Spacings
import edu.stanford.spezi.ui.theme.SpeziTheme
import edu.stanford.spezi.ui.theme.TextStyles
import edu.stanford.spezi.ui.theme.ThemePreviews
import edu.stanford.spezi.ui.theme.medium

/**
 * Renders the nodes of a [MarkdownBlock.Markdown] block as a vertical sequence of composables.
 *
 * Each node is delegated to [MarkdownNodeView]; list counters are shared across all nodes in the
 * block so that ordered-list markers increment correctly.
 */
@Composable
fun MarkdownTextBlock(
    block: MarkdownBlock.Markdown,
    modifier: Modifier = Modifier,
    linkClickStrategy: LinkClickStrategy = LocalLinkClickStrategy.current,
) {
    MarkdownTextBlock(
        text = block.rawContents,
        modifier = modifier,
        linkClickStrategy = linkClickStrategy,
    )
}

@Composable
fun MarkdownTextBlock(
    text: String,
    modifier: Modifier = Modifier,
    linkClickStrategy: LinkClickStrategy = LocalLinkClickStrategy.current,
) {
    val nodes = remember(text) { parseNodes(text) }
    val counters = remember(nodes) { ListCounters() }
    Column(
        modifier = modifier,
        verticalArrangement = Arrangement.spacedBy(Spacings.small),
    ) {
        nodes.forEach { node ->
            MarkdownNodeView(node = node, counters = counters, linkClickStrategy = linkClickStrategy)
        }
    }
}

/**
 * Renders a single [MarkdownNode].
 *
 * Headings map to the corresponding [TextStyles] scale; paragraphs use [TextStyles.bodyMedium];
 * list items delegate to [MarkdownListItemView] with an auto-generated marker from [counters].
 */
@Composable
fun MarkdownNodeView(
    node: MarkdownNode,
    modifier: Modifier = Modifier,
    counters: ListCounters = remember { ListCounters() },
    linkClickStrategy: LinkClickStrategy = LocalLinkClickStrategy.current,
) {
    when (node) {
        is MarkdownNode.Heading -> HeadingText(node = node, linkClickStrategy = linkClickStrategy, modifier = modifier)
        is MarkdownNode.Paragraph -> ParagraphText(
            text = node.text,
            linkClickStrategy = linkClickStrategy,
            modifier = modifier,
        )

        is MarkdownNode.ListItem -> MarkdownListItemView(
            item = node,
            marker = counters.next(node.style),
            linkClickStrategy = linkClickStrategy,
            modifier = modifier,
        )
    }
}

/**
 * Renders a single [MarkdownNode.ListItem] with the supplied [marker] string.
 *
 * Inline links within the item text are rendered as tappable underlined spans.
 */
@Composable
fun MarkdownListItemView(
    item: MarkdownNode.ListItem,
    marker: String,
    modifier: Modifier = Modifier,
    linkClickStrategy: LinkClickStrategy = LocalLinkClickStrategy.current,
) {
    Row(
        modifier = modifier.padding(start = Spacings.small * (item.nestingLevel + 1)),
        horizontalArrangement = Arrangement.spacedBy(Spacings.small),
    ) {
        Text(
            text = marker,
            style = TextStyles.bodyMedium,
            modifier = Modifier.width(Spacings.large),
        )
        Text(
            text = rememberMarkdownAnnotatedString(item.text, linkClickStrategy),
            style = TextStyles.bodyMedium,
            modifier = Modifier.weight(1f),
        )
    }
}

// --- Private helpers ---

@Composable
private fun HeadingText(
    node: MarkdownNode.Heading,
    linkClickStrategy: LinkClickStrategy,
    modifier: Modifier = Modifier,
) {
    val style = when (node.level) {
        1 -> TextStyles.headlineLarge
        2 -> TextStyles.headlineMedium
        3 -> TextStyles.headlineSmall
        4 -> TextStyles.titleLarge
        5 -> TextStyles.titleMedium
        else -> TextStyles.titleSmall
    }
    Text(
        text = rememberMarkdownAnnotatedString(node.text, linkClickStrategy),
        style = style.medium(),
        modifier = modifier,
    )
}

@Composable
private fun ParagraphText(
    text: String,
    linkClickStrategy: LinkClickStrategy,
    modifier: Modifier = Modifier,
) {
    Text(
        text = rememberMarkdownAnnotatedString(text, linkClickStrategy),
        style = TextStyles.bodyMedium,
        modifier = modifier,
    )
}

/**
 * Remembers a styled [AnnotatedString] from [text] with inline emphasis and tappable links applied,
 * or `null` if [text] is `null`.
 *
 * Bold (`**`/`__`) maps to [FontWeight.Bold]; italic (`*`/`_`) to [FontStyle.Italic].
 * Detected URLs and email/tel links are rendered as underlined [Colors.primary] spans.
 */
@Composable
fun rememberMarkdownAnnotatedString(
    text: String,
    linkClickStrategy: LinkClickStrategy = LocalLinkClickStrategy.current,
): AnnotatedString {
    val linkColor = Colors.primary
    return remember(text, linkClickStrategy) {
        val emphasis = parseEmphasis(text)
        buildAnnotatedString {
            append(emphasis.text)
            for (span in emphasis.spans) {
                addStyle(span.style.toSpanStyle(), span.range.first, span.range.last + 1)
            }
            for (detected in detectLinks(emphasis.text)) {
                val url = detected.link.href()
                val listener = when (linkClickStrategy) {
                    LinkClickStrategy.Default -> null
                    is LinkClickStrategy.OnClick -> LinkInteractionListener { linkClickStrategy.onClick(detected.link) }
                    is LinkClickStrategy.Custom -> customLinkInteractionListener(
                        strategy = linkClickStrategy,
                        link = detected.link,
                    )
                }
                addLink(
                    url = LinkAnnotation.Url(
                        url = url,
                        styles = TextLinkStyles(
                            style = SpanStyle(color = linkColor, textDecoration = TextDecoration.Underline),
                        ),
                        linkInteractionListener = listener,
                    ),
                    start = detected.range.first,
                    end = detected.range.last + 1,
                )
            }
        }
    }
}

private fun customLinkInteractionListener(
    strategy: LinkClickStrategy.Custom,
    link: MarkdownLink,
): LinkInteractionListener? {
    return if (link::class in strategy.links) {
        LinkInteractionListener { strategy.onClick(link) }
    } else {
        null
    }
}

private fun EmphasisStyle.toSpanStyle(): SpanStyle = when (this) {
    EmphasisStyle.Bold -> SpanStyle(fontWeight = FontWeight.Bold)
    EmphasisStyle.Italic -> SpanStyle(fontStyle = FontStyle.Italic)
}

/**
 * Tracks sequential counters for each [MarkdownNode.ListStyle] within a rendered block.
 *
 * Ordered-list counters (numbered and lettered) reset when a different style interrupts the
 * sequence. Unordered styles always emit a bullet.
 */
class ListCounters {
    private var lastStyle: MarkdownNode.ListStyle? = null
    private val counts = mutableMapOf<MarkdownNode.ListStyle, Int>()

    /**
     * Returns the next marker string for [style], advancing its counter if ordered.
     */
    fun next(style: MarkdownNode.ListStyle): String {
        if (style != lastStyle && style.isOrdered()) counts.remove(style)
        lastStyle = style
        return when (style) {
            MarkdownNode.ListStyle.Dash,
            MarkdownNode.ListStyle.Star,
            MarkdownNode.ListStyle.Plus,
            -> BULLET

            MarkdownNode.ListStyle.Numbered -> {
                val number = (counts[style] ?: 0) + 1
                counts[style] = number
                "$number."
            }

            MarkdownNode.ListStyle.Lettered -> {
                val index = (counts[style] ?: 0) + 1
                counts[style] = index
                ('a' + index - 1).toString() + "."
            }
        }
    }

    private fun MarkdownNode.ListStyle.isOrdered() =
        this == MarkdownNode.ListStyle.Numbered || this == MarkdownNode.ListStyle.Lettered

    private companion object {
        const val BULLET = "•"
    }
}

// --- Previews ---

@ThemePreviews
@Composable
private fun MarkdownTextBlockPreview() {
    SpeziTheme {
        MarkdownTextBlock(
            block = MarkdownBlock.Markdown(
                id = "preview",
                rawContents = "## Section heading\nThis has **bold** and *italic*: https://example.com\n" +
                    "- Bullet one\n- Bullet two\n1. One\n2. Two"
            )
        )
    }
}

@ThemePreviews
@Composable
private fun MarkdownListItemViewPreview() {
    SpeziTheme {
        Column {
            MarkdownListItemView(MarkdownNode.ListItem(MarkdownNode.ListStyle.Star, "Star bullet item"), "•")
            MarkdownListItemView(MarkdownNode.ListItem(MarkdownNode.ListStyle.Numbered, "First numbered"), "1.")
            MarkdownListItemView(MarkdownNode.ListItem(MarkdownNode.ListStyle.Numbered, "Second numbered"), "2.")
            MarkdownListItemView(MarkdownNode.ListItem(MarkdownNode.ListStyle.Lettered, "Alpha lettered"), "a.")
            MarkdownListItemView(MarkdownNode.ListItem(MarkdownNode.ListStyle.Lettered, "Beta lettered"), "b.")
            MarkdownListItemView(
                item = MarkdownNode.ListItem(MarkdownNode.ListStyle.Dash, "Item with link: https://example.com"),
                marker = "•",
            )
        }
    }
}
