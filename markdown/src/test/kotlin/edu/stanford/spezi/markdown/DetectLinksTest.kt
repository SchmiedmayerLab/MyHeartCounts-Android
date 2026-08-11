//
// This source file is part of the My Heart Counts Android open-source project
//
// SPDX-FileCopyrightText: 2026 Stanford University and the project authors (see CONTRIBUTORS.md)
//
// SPDX-License-Identifier: MIT

package edu.stanford.spezi.markdown

import com.google.common.truth.Truth.assertThat
import org.junit.Test

class DetectLinksTest {

    @Test
    fun `it should detect an http web url`() {
        // given
        val text = "See https://myheartcounts.stanford.edu for details."

        // when
        val links = detectLinks(text)

        // then
        assertThat(links.map { it.link }).containsExactly(MarkdownLink.Web("https://myheartcounts.stanford.edu"))
    }

    @Test
    fun `it should detect a www web url`() {
        // given
        val text = "Go to www.example.com now"

        // when
        val links = detectLinks(text)

        // then
        assertThat(links.map { it.link }).containsExactly(MarkdownLink.Web("www.example.com"))
    }

    @Test
    fun `it should trim trailing punctuation from a web url`() {
        // given
        val text = "Visit https://example.org."

        // when
        val link = detectLinks(text).single()

        // then
        assertThat(link.link).isEqualTo(MarkdownLink.Web("https://example.org"))
        assertThat(link.text).isEqualTo("https://example.org")
    }

    @Test
    fun `it should detect an email address`() {
        // given
        val text = "Contact myheartcounts@stanford.edu please"

        // when
        val links = detectLinks(text)

        // then
        assertThat(links.map { it.link }).containsExactly(MarkdownLink.Email("myheartcounts@stanford.edu"))
    }

    @Test
    fun `it should detect a mailto link as an email`() {
        // given
        val text = "Write mailto:foo@bar.com"

        // when
        val links = detectLinks(text)

        // then
        assertThat(links.map { it.link }).containsExactly(MarkdownLink.Email("foo@bar.com"))
    }

    @Test
    fun `it should detect a tel link as a phone number`() {
        // given
        val text = "Call tel:+16504984900 today"

        // when
        val links = detectLinks(text)

        // then
        assertThat(links.map { it.link }).containsExactly(MarkdownLink.Phone("+16504984900"))
    }

    @Test
    fun `it should not detect bare phone numbers`() {
        // given
        val text = "Reach us at (650) 498-4900 or 1-866-680-2906."

        // when
        val links = detectLinks(text)

        // then
        assertThat(links).isEmpty()
    }

    @Test
    fun `it should report the source range of a link`() {
        // given
        val text = "x myheartcounts@stanford.edu"

        // when
        val link = detectLinks(text).single()

        // then
        assertThat(text.substring(link.range)).isEqualTo("myheartcounts@stanford.edu")
    }

    @Test
    fun `it should resolve a www host to an https href`() {
        // given
        val link = MarkdownLink.Web("www.example.com")

        // when
        val href = link.href()

        // then
        assertThat(href).isEqualTo("https://www.example.com")
    }

    @Test
    fun `it should leave an https web href unchanged`() {
        // given
        val link = MarkdownLink.Web("https://example.org")

        // when
        val href = link.href()

        // then
        assertThat(href).isEqualTo("https://example.org")
    }

    @Test
    fun `it should resolve an email href to a mailto uri`() {
        // given
        val link = MarkdownLink.Email("foo@bar.com")

        // when
        val href = link.href()

        // then
        assertThat(href).isEqualTo("mailto:foo@bar.com")
    }

    @Test
    fun `it should resolve a phone href to a tel uri`() {
        // given
        val link = MarkdownLink.Phone("+16504984900")

        // when
        val href = link.href()

        // then
        assertThat(href).isEqualTo("tel:+16504984900")
    }

    @Test
    fun `it should not double-prefix an email that already has a mailto scheme`() {
        // given
        val link = MarkdownLink.Email("mailto:foo@bar.com")

        // when
        val href = link.href()

        // then
        assertThat(href).isEqualTo("mailto:foo@bar.com")
    }

    @Test
    fun `it should not double-prefix a phone that already has a tel scheme`() {
        // given
        val link = MarkdownLink.Phone("tel:+16504984900")

        // when
        val href = link.href()

        // then
        assertThat(href).isEqualTo("tel:+16504984900")
    }

    @Test
    fun `it should detect links within a markdown block`() {
        // given
        val block = MarkdownBlock.Markdown(id = null, rawContents = "Email myheartcounts@stanford.edu")

        // when
        val links = block.links()

        // then
        assertThat(links.map { it.link }).containsExactly(MarkdownLink.Email("myheartcounts@stanford.edu"))
    }
}
