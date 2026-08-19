//
// This source file is part of the My Heart Counts Android open-source project
//
// SPDX-FileCopyrightText: 2026 Stanford University and the project authors (see CONTRIBUTORS.md)
//
// SPDX-License-Identifier: MIT

package org.grovealliance.markdown

import com.google.common.truth.Truth.assertThat
import org.grovealliance.foundation.SemanticVersion
import org.junit.Assert.assertThrows
import org.junit.Test

class MarkdownDocumentTest {

    private val elementNames = setOf("toggle", "select", "option", "signature")

    private val markdown = """
        ---
        title: Heart Study Consent
        version: 1.2.0
        ---
        # Welcome
        Thanks for participating.

        ## Your rights
        You may withdraw at any time.

        <toggle id="data-sharing" expected-value=true>
            I agree to share my data.
        </toggle>

        <select id="trial" expected-value=trial-yes>
            Do you want to join the trial?
            <option id="trial-yes">Yes</option>
            <option id="trial-no">No</option>
        </select>

        <signature id="sig" />
    """.trimIndent()

    @Test
    fun `it should parse frontmatter metadata`() {
        // given

        // when
        val document = MarkdownDocument.process(markdown, elementNames)

        // then
        assertThat(document.metadata.title).isEqualTo("Heart Study Consent")
        assertThat(document.metadata.version).isEqualTo(SemanticVersion(major = 1, minor = 2, patch = 0))
    }

    @Test
    fun `it should split text into markdown blocks at headings`() {
        // given

        // when
        val textBlocks = MarkdownDocument.process(markdown, elementNames)
            .blocks.filterIsInstance<MarkdownBlock.Markdown>()

        // then
        assertThat(textBlocks).hasSize(2)
        assertThat(textBlocks[0].rawContents).startsWith("# Welcome")
        assertThat(textBlocks[1].rawContents).startsWith("## Your rights")
    }

    @Test
    fun `it should extract declared custom elements`() {
        // given

        // when
        val elements = MarkdownDocument.process(markdown, elementNames)
            .blocks.filterIsInstance<MarkdownBlock.Element>()

        // then
        assertThat(elements.map { it.name }).containsExactly("toggle", "select", "signature").inOrder()
    }

    @Test
    fun `it should expose custom element attributes`() {
        // given
        val document = MarkdownDocument.process(markdown, elementNames)

        // when
        val toggle = document.blocks.filterIsInstance<MarkdownBlock.Element>().single { it.name == "toggle" }

        // then
        assertThat(toggle.attribute("id")).isEqualTo("data-sharing")
        assertThat(toggle.attribute("expected-value")).isEqualTo("true")
    }

    @Test
    fun `it should retain nested elements`() {
        // given
        val document = MarkdownDocument.process(markdown, elementNames)

        // when
        val options = document.blocks.filterIsInstance<MarkdownBlock.Element>().single { it.name == "select" }
            .content.filterIsInstance<MarkdownBlock.Element.Content.Element>().map { it.element }

        // then
        assertThat(options.map { it.attribute("id") }).containsExactly("trial-yes", "trial-no").inOrder()
    }

    @Test
    fun `it should parse self-closing elements`() {
        // given
        val document = MarkdownDocument.process(markdown, elementNames)

        // when
        val signature = document.blocks.filterIsInstance<MarkdownBlock.Element>().single { it.name == "signature" }

        // then
        assertThat(signature.attribute("id")).isEqualTo("sig")
        assertThat(signature.content).isEmpty()
    }

    @Test
    fun `it should leave undeclared element tags in the markdown text`() {
        // given
        val source = "<toggle id=x>hello</toggle>"

        // when
        val document = MarkdownDocument.process(source)

        // then
        assertThat(document.blocks.filterIsInstance<MarkdownBlock.Element>()).isEmpty()
        val text = document.blocks.filterIsInstance<MarkdownBlock.Markdown>().joinToString("") { it.rawContents }
        assertThat(text).contains("<toggle id=x>hello</toggle>")
    }

    @Test
    fun `it should fail on unterminated quoted attributes`() {
        // given
        val source = """<toggle id="data-sharing>"""

        // when
        val exception = assertThrows(MarkdownParseException::class.java) {
            MarkdownDocument.process(source, elementNames)
        }

        // then
        assertThat(exception.kind).isEqualTo(MarkdownParseException.Kind.Eof)
    }

    @Test
    fun `it should treat alternative newline styles like LF`() {
        // given
        val reference = MarkdownDocument.process(markdown, elementNames)
        val newlineStyles = listOf("\r\n", "\r", "\u2028", "\u2029")

        // when
        val documents = newlineStyles.map { MarkdownDocument.process(markdown.replace("\n", it), elementNames) }

        // then
        documents.forEach { document ->
            assertThat(document.metadata.title).isEqualTo(reference.metadata.title)
            assertThat(document.blocks.map { it::class }).isEqualTo(reference.blocks.map { it::class })
        }
    }
}
