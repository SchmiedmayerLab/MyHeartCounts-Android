//
// This source file is part of the My Heart Counts Android open-source project
//
// SPDX-FileCopyrightText: 2026 Stanford University and the project authors (see CONTRIBUTORS.md)
//
// SPDX-License-Identifier: MIT

package org.grovealliance.markdown

/**
 * A typed structural element parsed from the raw contents of a [MarkdownBlock.Markdown] block.
 *
 * Produced by [parseNodes] and surfaced via the [MarkdownBlock.Markdown.nodes] extension.
 */
sealed interface MarkdownNode {

    /**
     * An ATX-style (`# Heading`) or setext-style (underlined) heading.
     *
     * [level] is in the range 1–6, matching the number of leading `#` characters or the
     * underline type (`=` → level 1, `-` → level 2).
     */
    data class Heading(val level: Int, val text: String) : MarkdownNode

    /**
     * A single list item introduced by the given [style] marker.
     *
     * [nestingLevel] is derived from leading spaces in groups of four.
     */
    data class ListItem(val style: ListStyle, val text: String, val nestingLevel: Int = 0) : MarkdownNode

    /**
     * A non-empty run of text that is neither a heading nor a list item.
     */
    data class Paragraph(val text: String) : MarkdownNode

    /**
     * The marker convention used to introduce a [ListItem].
     */
    enum class ListStyle {
        /**
         * Introduced by `- `.
         */
        Dash,

        /**
         * Introduced by `* `.
         */
        Star,

        /**
         * Introduced by `+ `.
         */
        Plus,

        /**
         * Introduced by a decimal integer followed by `.` or `)` (e.g. `1.`, `2)`).
         */
        Numbered,

        /**
         * Introduced by a single lowercase letter followed by `.` or `)` (e.g. `a.`, `b)`).
         */
        Lettered,
    }
}

/**
 * Parses [text] into an ordered list of [MarkdownNode]s, detecting headings (ATX and setext),
 * unordered bullet lists (`-`, `*`, `+`), numbered and lettered ordered lists, and paragraph text.
 *
 * Blank lines are skipped. Lines that match no recognised pattern are emitted as
 * [MarkdownNode.Paragraph].
 */
fun parseNodes(text: String): List<MarkdownNode> {
    val lines = text.lines()
    val result = mutableListOf<MarkdownNode>()
    var index = 0
    while (index < lines.size) {
        val trimmed = lines[index].trim()
        if (trimmed.isEmpty()) {
            index++
            continue
        }
        val (node, advance) = parseNodeAt(lines, index, trimmed)
        result.add(node)
        index += advance
    }
    return result
}

/**
 * The links detected within this node's text content.
 *
 * Delegates to [detectLinks] on the node's text field. Headings, list items, and paragraphs
 * may all contain inline links.
 */
fun MarkdownNode.links(): List<DetectedLink> = detectLinks(
    when (this) {
        is MarkdownNode.Heading -> text
        is MarkdownNode.ListItem -> text
        is MarkdownNode.Paragraph -> text
    }
)

// --- Private helpers ---

private const val MAX_HEADING_LEVEL = 6

// ATX heading: 1–6 hashes, optional space + body text, optional trailing hashes.
private val ATX_HEADING_REGEX = Regex("""^(#{1,6})(?:\s+(.*?))?(?:\s+#+)?\s*$""")

// Numbered list: one or more digits followed by `.` or `)` and at least one space.
private val NUMBERED_LIST_REGEX = Regex("""^\d+[.)]\s+(.+)""")

// Lettered list: a single lowercase letter followed by `.` or `)` and at least one space.
private val LETTERED_LIST_REGEX = Regex("""^[a-z][.)]\s+(.+)""")

/**
 * Returns the node for [trimmed] at position [index] in [lines], together with how many lines to advance.
 */
private fun parseNodeAt(lines: List<String>, index: Int, trimmed: String): Pair<MarkdownNode, Int> {
    // List items are resolved before the setext look-ahead so that lines starting with `- ` are
    // never misread as paragraph text preceding a setext underline.
    val singleLine = tryParseListItem(lines[index]) ?: tryParseAtxHeading(trimmed)
    if (singleLine != null) return Pair(singleLine, 1)
    tryParseSetextHeading(lines, index, trimmed)?.let { return Pair(it, 2) }
    return Pair(MarkdownNode.Paragraph(trimmed), 1)
}

private fun tryParseAtxHeading(trimmed: String): MarkdownNode.Heading? {
    val match = ATX_HEADING_REGEX.matchEntire(trimmed) ?: return null
    val level = match.groupValues[1].length.coerceIn(1, MAX_HEADING_LEVEL)
    val headingText = match.groupValues[2].trimEnd('#').trim()
    return MarkdownNode.Heading(level, headingText)
}

private fun tryParseSetextHeading(lines: List<String>, index: Int, trimmed: String): MarkdownNode.Heading? {
    if (index + 1 >= lines.size) return null
    val next = lines[index + 1].trim()
    return when {
        next.isEmpty() -> null
        next.all { it == '=' } -> MarkdownNode.Heading(1, trimmed)
        next.all { it == '-' } -> MarkdownNode.Heading(2, trimmed)
        else -> null
    }
}

private fun tryParseListItem(line: String): MarkdownNode.ListItem? {
    val nestingLevel = line.takeWhile { it == ' ' }.length / LIST_INDENT_SPACES
    val trimmed = line.trimStart()
    return when {
        trimmed.startsWith("- ") ->
            MarkdownNode.ListItem(MarkdownNode.ListStyle.Dash, trimmed.removePrefix("- ").trim(), nestingLevel)
        trimmed.startsWith("* ") ->
            MarkdownNode.ListItem(MarkdownNode.ListStyle.Star, trimmed.removePrefix("* ").trim(), nestingLevel)
        trimmed.startsWith("+ ") ->
            MarkdownNode.ListItem(MarkdownNode.ListStyle.Plus, trimmed.removePrefix("+ ").trim(), nestingLevel)
        else -> {
            val numbered = NUMBERED_LIST_REGEX.matchEntire(trimmed)
            if (numbered != null) {
                MarkdownNode.ListItem(MarkdownNode.ListStyle.Numbered, numbered.groupValues[1].trim(), nestingLevel)
            } else {
                val lettered = LETTERED_LIST_REGEX.matchEntire(trimmed)
                lettered?.let {
                    MarkdownNode.ListItem(MarkdownNode.ListStyle.Lettered, it.groupValues[1].trim(), nestingLevel)
                }
            }
        }
    }
}

private const val LIST_INDENT_SPACES = 4
