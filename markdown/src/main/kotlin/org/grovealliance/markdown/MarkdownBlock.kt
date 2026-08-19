//
// This source file is part of the My Heart Counts Android open-source project
//
// SPDX-FileCopyrightText: 2026 Stanford University and the project authors (see CONTRIBUTORS.md)
//
// SPDX-License-Identifier: MIT

package org.grovealliance.markdown

/**
 * A unit of content within a [MarkdownDocument]: either a section of markdown text or a parsed [Element].
 */
sealed interface MarkdownBlock {
    /**
     * The block's stable identifier, if available.
     */
    val id: String?

    /**
     * A block of markdown-formatted text.
     *
     * @property id Stable identifier for the block, if available.
     * @property rawContents Source markdown text for this block.
     */
    data class Markdown(override val id: String?, val rawContents: String) : MarkdownBlock

    /**
     * A custom HTML-style element extracted from a markdown document.
     *
     * @property name The element's tag name.
     * @property attributes The element's attributes.
     * @property content The element's content, a series of text fragments and nested elements.
     * @property raw The unprocessed source text from which the element was parsed.
     */
    data class Element(
        val name: String,
        val attributes: List<Attribute> = emptyList(),
        val content: List<Content> = emptyList(),
        val raw: String = "",
    ) : MarkdownBlock {
        override val id: String? = attribute("id")

        /**
         * A key-value attribute of a [Element].
         *
         * @property name Attribute name.
         * @property value Attribute value with surrounding quotes removed.
         */
        data class Attribute(val name: String, val value: String)

        /**
         * A single piece of a [Element]'s content.
         */
        sealed interface Content {
            /**
             * Plain text content, which may itself contain markdown.
             */
            data class Text(val text: String) : Content

            /**
             * A nested [MarkdownBlock.Element].
             */
            data class Element(val element: MarkdownBlock.Element) : Content
        }

        /**
         * The value of the first attribute named [name], if present.
         */
        fun attribute(name: String): String? = attributes.firstOrNull { it.name == name }?.value
    }
}

/**
 * The nodes parsed from this block's raw contents.
 */
fun MarkdownBlock.Markdown.nodes(): List<MarkdownNode> = parseNodes(rawContents)

/**
 * The links detected within this block's contents.
 */
fun MarkdownBlock.Markdown.links(): List<DetectedLink> = detectLinks(rawContents)
