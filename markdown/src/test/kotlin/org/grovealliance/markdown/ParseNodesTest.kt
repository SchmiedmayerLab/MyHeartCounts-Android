//
// This source file is part of the My Heart Counts Android open-source project
//
// SPDX-FileCopyrightText: 2026 Stanford University and the project authors (see CONTRIBUTORS.md)
//
// SPDX-License-Identifier: MIT

package org.grovealliance.markdown

import com.google.common.truth.Truth.assertThat
import org.junit.Test

class ParseNodesTest {

    // --- ATX headings ---

    @Test
    fun `it should parse an h1 ATX heading`() {
        // given
        val text = "# Welcome"

        // when
        val nodes = parseNodes(text)

        // then
        assertThat(nodes).containsExactly(MarkdownNode.Heading(1, "Welcome"))
    }

    @Test
    fun `it should parse an h2 ATX heading`() {
        // given
        val text = "## Your rights"

        // when
        val nodes = parseNodes(text)

        // then
        assertThat(nodes).containsExactly(MarkdownNode.Heading(2, "Your rights"))
    }

    @Test
    fun `it should parse an h3 ATX heading`() {
        // given
        val text = "### Details"

        // when
        val nodes = parseNodes(text)

        // then
        assertThat(nodes).containsExactly(MarkdownNode.Heading(3, "Details"))
    }

    @Test
    fun `it should parse an h6 ATX heading`() {
        // given
        val text = "###### Deep"

        // when
        val nodes = parseNodes(text)

        // then
        assertThat(nodes).containsExactly(MarkdownNode.Heading(6, "Deep"))
    }

    @Test
    fun `it should strip optional closing hashes from an ATX heading`() {
        // given
        val text = "## Section ##"

        // when
        val nodes = parseNodes(text)

        // then
        assertThat(nodes).containsExactly(MarkdownNode.Heading(2, "Section"))
    }

    @Test
    fun `it should treat a line with more than 6 hashes as a paragraph`() {
        // given
        val text = "####### Not a heading"

        // when
        val nodes = parseNodes(text)

        // then
        assertThat(nodes).containsExactly(MarkdownNode.Paragraph("####### Not a heading"))
    }

    // --- Setext headings ---

    @Test
    fun `it should parse a setext h1 heading underlined with equals signs`() {
        // given
        val text = """
            Introduction
            ============
        """.trimIndent()

        // when
        val nodes = parseNodes(text)

        // then
        assertThat(nodes).containsExactly(MarkdownNode.Heading(1, "Introduction"))
    }

    @Test
    fun `it should parse a setext h2 heading underlined with dashes`() {
        // given
        val text = """
            Background
            ----------
        """.trimIndent()

        // when
        val nodes = parseNodes(text)

        // then
        assertThat(nodes).containsExactly(MarkdownNode.Heading(2, "Background"))
    }

    // --- Unordered list items ---

    @Test
    fun `it should parse a dash list item`() {
        // given
        val text = "- First item"

        // when
        val nodes = parseNodes(text)

        // then
        assertThat(nodes).containsExactly(MarkdownNode.ListItem(MarkdownNode.ListStyle.Dash, "First item"))
    }

    @Test
    fun `it should parse a star list item`() {
        // given
        val text = "* Bullet point"

        // when
        val nodes = parseNodes(text)

        // then
        assertThat(nodes).containsExactly(MarkdownNode.ListItem(MarkdownNode.ListStyle.Star, "Bullet point"))
    }

    @Test
    fun `it should parse a plus list item`() {
        // given
        val text = "+ Another item"

        // when
        val nodes = parseNodes(text)

        // then
        assertThat(nodes).containsExactly(MarkdownNode.ListItem(MarkdownNode.ListStyle.Plus, "Another item"))
    }

    @Test
    fun `it should parse nested unordered list item indentation`() {
        // given
        val text = """
            - Parent
                - Child
                    - Grandchild
        """.trimIndent()

        // when
        val nodes = parseNodes(text)

        // then
        assertThat(nodes).containsExactly(
            MarkdownNode.ListItem(MarkdownNode.ListStyle.Dash, "Parent", nestingLevel = 0),
            MarkdownNode.ListItem(MarkdownNode.ListStyle.Dash, "Child", nestingLevel = 1),
            MarkdownNode.ListItem(MarkdownNode.ListStyle.Dash, "Grandchild", nestingLevel = 2),
        ).inOrder()
    }

    // --- Ordered list items ---

    @Test
    fun `it should parse a numbered list item with dot separator`() {
        // given
        val text = "1. First"

        // when
        val nodes = parseNodes(text)

        // then
        assertThat(nodes).containsExactly(MarkdownNode.ListItem(MarkdownNode.ListStyle.Numbered, "First"))
    }

    @Test
    fun `it should parse a numbered list item with parenthesis separator`() {
        // given
        val text = "2) Second"

        // when
        val nodes = parseNodes(text)

        // then
        assertThat(nodes).containsExactly(MarkdownNode.ListItem(MarkdownNode.ListStyle.Numbered, "Second"))
    }

    @Test
    fun `it should parse a lettered list item with dot separator`() {
        // given
        val text = "a. Alpha"

        // when
        val nodes = parseNodes(text)

        // then
        assertThat(nodes).containsExactly(MarkdownNode.ListItem(MarkdownNode.ListStyle.Lettered, "Alpha"))
    }

    @Test
    fun `it should parse a lettered list item with parenthesis separator`() {
        // given
        val text = "b) Beta"

        // when
        val nodes = parseNodes(text)

        // then
        assertThat(nodes).containsExactly(MarkdownNode.ListItem(MarkdownNode.ListStyle.Lettered, "Beta"))
    }

    @Test
    fun `it should parse nested ordered list item indentation`() {
        // given
        val text = """
            1. Parent
                a. Child
                    1. Grandchild
        """.trimIndent()

        // when
        val nodes = parseNodes(text)

        // then
        assertThat(nodes).containsExactly(
            MarkdownNode.ListItem(MarkdownNode.ListStyle.Numbered, "Parent", nestingLevel = 0),
            MarkdownNode.ListItem(MarkdownNode.ListStyle.Lettered, "Child", nestingLevel = 1),
            MarkdownNode.ListItem(MarkdownNode.ListStyle.Numbered, "Grandchild", nestingLevel = 2),
        ).inOrder()
    }

    // --- Paragraph ---

    @Test
    fun `it should parse a plain line as a paragraph`() {
        // given
        val text = "This is plain paragraph text."

        // when
        val nodes = parseNodes(text)

        // then
        assertThat(nodes).containsExactly(MarkdownNode.Paragraph("This is plain paragraph text."))
    }

    // --- Blank line handling ---

    @Test
    fun `it should skip blank lines`() {
        // given
        val text = """
            # Title

            Some text.
        """.trimIndent()

        // when
        val nodes = parseNodes(text)

        // then
        assertThat(nodes).containsExactly(
            MarkdownNode.Heading(1, "Title"),
            MarkdownNode.Paragraph("Some text."),
        )
    }

    // --- Mixed content ---

    @Test
    fun `it should parse a mixed block into ordered nodes`() {
        // given
        val text = """
            # Overview
            Read the following carefully.
            - Clause one
            - Clause two
            1. Step one
            a. Point alpha
        """.trimIndent()

        // when
        val nodes = parseNodes(text)

        // then
        assertThat(nodes).containsExactly(
            MarkdownNode.Heading(1, "Overview"),
            MarkdownNode.Paragraph("Read the following carefully."),
            MarkdownNode.ListItem(MarkdownNode.ListStyle.Dash, "Clause one"),
            MarkdownNode.ListItem(MarkdownNode.ListStyle.Dash, "Clause two"),
            MarkdownNode.ListItem(MarkdownNode.ListStyle.Numbered, "Step one"),
            MarkdownNode.ListItem(MarkdownNode.ListStyle.Lettered, "Point alpha"),
        ).inOrder()
    }

    // --- Edge cases ---

    @Test
    fun `it should not misread a dash list item as a setext heading underline`() {
        // given
        val text = """
            - item
            ------
        """.trimIndent()

        // when
        val nodes = parseNodes(text)

        // then
        assertThat(nodes).hasSize(2)
        assertThat(nodes[0]).isEqualTo(MarkdownNode.ListItem(MarkdownNode.ListStyle.Dash, "item"))
        assertThat(nodes[1]).isInstanceOf(MarkdownNode.Paragraph::class.java)
    }

    @Test
    fun `it should return an empty list for blank input`() {
        // given
        val text = "   \n\n   "

        // when
        val nodes = parseNodes(text)

        // then
        assertThat(nodes).isEmpty()
    }

    // --- nodes() extension ---

    @Test
    fun `it should surface nodes via the MarkdownBlock Markdown extension`() {
        // given
        val block = MarkdownBlock.Markdown(
            id = null,
            rawContents = "## Summary\n- Point one\n- Point two",
        )

        // when
        val nodes = block.nodes()

        // then
        assertThat(nodes).containsExactly(
            MarkdownNode.Heading(2, "Summary"),
            MarkdownNode.ListItem(MarkdownNode.ListStyle.Dash, "Point one"),
            MarkdownNode.ListItem(MarkdownNode.ListStyle.Dash, "Point two"),
        ).inOrder()
    }

    // --- links() on nodes ---

    @Test
    fun `it should detect a link inside a paragraph node`() {
        // given
        val node = MarkdownNode.Paragraph("Visit https://example.com for details.")

        // when
        val links = node.links()

        // then
        assertThat(links.map { it.link }).containsExactly(MarkdownLink.Web("https://example.com"))
    }

    @Test
    fun `it should detect a link inside a list item node`() {
        // given
        val node = MarkdownNode.ListItem(MarkdownNode.ListStyle.Dash, "Email support@example.com now")

        // when
        val links = node.links()

        // then
        assertThat(links.map { it.link }).containsExactly(MarkdownLink.Email("support@example.com"))
    }

    @Test
    fun `it should detect a link inside a heading node`() {
        // given
        val node = MarkdownNode.Heading(2, "See https://example.org")

        // when
        val links = node.links()

        // then
        assertThat(links.map { it.link }).containsExactly(MarkdownLink.Web("https://example.org"))
    }

    @Test
    fun `it should return an empty list when a node contains no links`() {
        // given
        val node = MarkdownNode.Paragraph("No links here at all.")

        // when
        val links = node.links()

        // then
        assertThat(links).isEmpty()
    }

    @Test
    fun `it should detect links across multiple nodes produced from a block`() {
        // given
        val block = MarkdownBlock.Markdown(
            id = null,
            rawContents = "# Contact\n- Email support@example.com\n- Visit https://example.com",
        )

        // when
        val allLinks = block.nodes().flatMap { it.links() }

        // then
        assertThat(allLinks.map { it.link }).containsExactly(
            MarkdownLink.Email("support@example.com"),
            MarkdownLink.Web("https://example.com"),
        ).inOrder()
    }
}
