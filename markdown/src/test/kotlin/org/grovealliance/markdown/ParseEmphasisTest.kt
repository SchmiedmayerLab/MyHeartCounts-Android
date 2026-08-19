//
// This source file is part of the My Heart Counts Android open-source project
//
// SPDX-FileCopyrightText: 2026 Stanford University and the project authors (see CONTRIBUTORS.md)
//
// SPDX-License-Identifier: MIT

package org.grovealliance.markdown

import com.google.common.truth.Truth.assertThat
import org.junit.Test

class ParseEmphasisTest {

    @Test
    fun `it should strip double-asterisk bold markers`() {
        // given
        val text = "Ends on **December 31, 2100** exactly."

        // when
        val result = parseEmphasis(text)

        // then
        assertThat(result.text).isEqualTo("Ends on December 31, 2100 exactly.")
    }

    @Test
    fun `it should record the range of a bold span`() {
        // given
        val text = "Ends on **December 31, 2100** exactly."

        // when
        val span = parseEmphasis(text).spans.single()

        // then
        assertThat(span.style).isEqualTo(EmphasisStyle.Bold)
        assertThat(parseEmphasis(text).text.substring(span.range)).isEqualTo("December 31, 2100")
    }

    @Test
    fun `it should detect single-asterisk italic`() {
        // given
        val text = "Select *yes* to continue"

        // when
        val result = parseEmphasis(text)

        // then
        assertThat(result.text).isEqualTo("Select yes to continue")
        assertThat(result.spans.single().style).isEqualTo(EmphasisStyle.Italic)
    }

    @Test
    fun `it should treat underscores as emphasis`() {
        // given
        val text = "__strong__ and _soft_"

        // when
        val result = parseEmphasis(text)

        // then
        assertThat(result.text).isEqualTo("strong and soft")
        assertThat(result.spans.map { it.style })
            .containsExactly(EmphasisStyle.Bold, EmphasisStyle.Italic)
            .inOrder()
    }

    @Test
    fun `it should record overlapping spans for italic nested in bold`() {
        // given
        val text = "**bold *and italic* here**"

        // when
        val result = parseEmphasis(text)

        // then
        assertThat(result.text).isEqualTo("bold and italic here")
        assertThat(result.spans.map { it.style })
            .containsExactly(EmphasisStyle.Italic, EmphasisStyle.Bold)
        val bold = result.spans.first { it.style == EmphasisStyle.Bold }
        val italic = result.spans.first { it.style == EmphasisStyle.Italic }
        assertThat(result.text.substring(bold.range)).isEqualTo("bold and italic here")
        assertThat(result.text.substring(italic.range)).isEqualTo("and italic")
    }

    @Test
    fun `it should leave an unmatched delimiter verbatim`() {
        // given
        val text = "a lone **start that never closes"

        // when
        val result = parseEmphasis(text)

        // then
        assertThat(result.text).isEqualTo(text)
        assertThat(result.spans).isEmpty()
    }

    @Test
    fun `it should produce no spans for plain text`() {
        // given
        val text = "Just ordinary prose."

        // when
        val result = parseEmphasis(text)

        // then
        assertThat(result.text).isEqualTo(text)
        assertThat(result.spans).isEmpty()
    }

    @Test
    fun `it should ignore empty emphasis`() {
        // given
        val text = "before **** after"

        // when
        val result = parseEmphasis(text)

        // then
        assertThat(result.text).isEqualTo("before  after")
        assertThat(result.spans).isEmpty()
    }
}
