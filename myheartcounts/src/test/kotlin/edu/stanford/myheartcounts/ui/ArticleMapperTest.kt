//
// This source file is part of the My Heart Counts Android open-source project
//
// SPDX-FileCopyrightText: 2026 Stanford University and the project authors (see CONTRIBUTORS.md)
//
// SPDX-License-Identifier: MIT

package edu.stanford.myheartcounts.ui

import com.google.common.truth.Truth.assertThat
import edu.stanford.myheartcounts.model.StudyVariant
import edu.stanford.myheartcounts.study.FakeStudyVariantProvider
import org.grovealliance.core.time.FakeTimeProvider
import org.grovealliance.markdown.MarkdownBlock
import org.grovealliance.markdown.MarkdownDocument
import org.grovealliance.ui.StringResource
import org.grovealliance.ui.groveAppBar
import org.junit.After
import org.junit.Before
import org.junit.Test
import java.time.ZoneId
import java.time.ZoneOffset
import java.util.Locale

class ArticleMapperTest {

    private val originalLocale = Locale.getDefault()
    private val timeProvider = FakeTimeProvider()
    private val variantProvider = FakeStudyVariantProvider()
    private val mapper = ArticleMapperImpl(
        timeProvider = timeProvider,
        variantProvider = variantProvider,
    )
    private val appBar = groveAppBar { close { } }

    @Before
    fun setup() {
        Locale.setDefault(Locale.US)
        timeProvider.setZone(zone = ZoneOffset.UTC)
    }

    @After
    fun tearDown() {
        Locale.setDefault(originalLocale)
    }

    @Test
    fun `uses the metadata title instead of the first heading`() {
        // when
        val header = header(metadata = "title: Study update")

        // then
        assertThat(header.title).isEqualTo(StringResource(text = "Study update"))
    }

    @Test
    fun `omits absent optional metadata and uses the bundled image`() {
        // when
        val header = header()

        // then
        assertThat(header.title).isEqualTo(StringResource(text = ""))
        assertThat(header.date).isNull()
        assertThat(header.tags).isEmpty()
        assertThat(header.image).isEqualTo(ArticleHeader.HeaderImage.Stanford)
    }

    @Test
    fun `trims tags and removes empty entries while preserving order`() {
        // when
        val header = header(metadata = "tags: Study, , Heart Health,, Study")

        // then
        assertThat(header.tags).containsExactly(
            StringResource(text = "Study"),
            StringResource(text = "Heart Health"),
            StringResource(text = "Study"),
        ).inOrder()
    }

    @Test
    fun `formats the publication date in the participant's zone`() {
        // given
        timeProvider.setZone(zone = ZoneId.of("America/Los_Angeles"))

        // when
        val header = header(metadata = "date: 2026-01-02T00:30:00Z")

        // then
        assertThat(header.date).isEqualTo(StringResource(text = "Jan 1, 2026"))
    }

    @Test
    fun `respects the locale when formatting a publication date with an offset`() {
        // given
        Locale.setDefault(Locale.UK)

        // when
        val header = header(metadata = "date: 2026-01-01T12:00:00.123+02:00")

        // then
        assertThat(header.date).isEqualTo(StringResource(text = "1 Jan 2026"))
    }

    @Test
    fun `ignores a malformed publication date`() {
        // when
        val header = header(metadata = "date: yesterday")

        // then
        assertThat(header.date).isNull()
    }

    @Test
    fun `shows no fallback image for the Imperial variant`() {
        // given
        variantProvider.select(variant = StudyVariant.Imperial)

        // when
        val header = header()

        // then
        assertThat(header.image).isNull()
    }

    @Test
    fun `resolves the named bundled image for every variant`() {
        // given
        variantProvider.select(variant = StudyVariant.Imperial)

        // when
        val header = header(metadata = "headerImage: stanford")

        // then
        assertThat(header.image).isEqualTo(ArticleHeader.HeaderImage.Stanford)
    }

    @Test
    fun `supports an explicit secure remote image`() {
        // given
        val url = "https://example.com/header.jpg"

        // when
        val header = header(metadata = "headerImage: $url")

        // then
        assertThat((header.image as ArticleHeader.HeaderImage.Remote).url).isEqualTo(url)
    }

    @Test
    fun `does not substitute branding for an unknown asset`() {
        // when
        val header = header(metadata = "headerImage: another-institution")

        // then
        assertThat(header.image).isNull()
    }

    @Test
    fun `does not load an insecure image`() {
        // when
        val header = header(metadata = "headerImage: http://example.com/header.jpg")

        // then
        assertThat(header.image).isNull()
    }

    @Test
    fun `ignores a malformed secure image URL`() {
        // when
        val header = header(metadata = "headerImage: https://invalid host/header.jpg")

        // then
        assertThat(header.image).isNull()
    }

    @Test
    fun `removes a leading title while keeping the article text and section headings`() {
        // given
        val document = article(
            metadata = "title: Study update",
            body = "# Welcome!\nIntroduction.\n\n## Surveys\nAnswer daily.\n\n# Further reading",
        )

        // when
        val sheet = mapper.map(document = document, appBar = appBar, onDisplayed = {})

        // then
        assertThat(sheet.bodyContents()).isEqualTo("Introduction.\n\n## Surveys\nAnswer daily.\n\n# Further reading")
        assertThat(sheet.document.metadata).isEqualTo(document.metadata)
        assertThat((document.blocks.first() as MarkdownBlock.Markdown).rawContents).startsWith("# Welcome!")
    }

    @Test
    fun `keeps the leading title when the header has no title`() {
        // given
        val document = article()

        // when
        val sheet = mapper.map(document = document, appBar = appBar, onDisplayed = {})

        // then
        assertThat(sheet.document).isEqualTo(document)
    }

    @Test
    fun `removes blank lines following a standalone title`() {
        // given
        val document = article(metadata = "title: Study update", body = "# Welcome!\n\nIntroduction.")

        // when
        val sheet = mapper.map(document = document, appBar = appBar, onDisplayed = {})

        // then
        assertThat(sheet.document.blocks).hasSize(1)
        assertThat(sheet.bodyContents()).isEqualTo("Introduction.")
    }

    @Test
    fun `removes the empty block when the article contains only a title`() {
        // given
        val document = article(metadata = "title: Study update", body = "# Welcome!")

        // when
        val sheet = mapper.map(document = document, appBar = appBar, onDisplayed = {})

        // then
        assertThat(sheet.document.blocks).isEmpty()
    }

    @Test
    fun `keeps the leading title when the header title is blank`() {
        // given
        val document = article(metadata = "title: ")

        // when
        val sheet = mapper.map(document = document, appBar = appBar, onDisplayed = {})

        // then
        assertThat(sheet.document).isEqualTo(document)
    }

    @Test
    fun `keeps a leading section heading`() {
        // given
        val document = article(metadata = "title: Study update", body = "## Congratulations!\nWeek one complete.")

        // when
        val sheet = mapper.map(document = document, appBar = appBar, onDisplayed = {})

        // then
        assertThat(sheet.document).isEqualTo(document)
    }

    @Test
    fun `keeps a title appearing after introductory text`() {
        // given
        val document = article(metadata = "title: Study update", body = "Introduction.\n\n# Further reading")

        // when
        val sheet = mapper.map(document = document, appBar = appBar, onDisplayed = {})

        // then
        assertThat(sheet.document).isEqualTo(document)
    }

    @Test
    fun `keeps hash text that is not a heading`() {
        // given
        val document = article(metadata = "title: Study update", body = "#hearthealth is a tag.")

        // when
        val sheet = mapper.map(document = document, appBar = appBar, onDisplayed = {})

        // then
        assertThat(sheet.document).isEqualTo(document)
    }

    @Test
    fun `keeps headings inside a code block`() {
        // given
        val document = article(metadata = "title: Study update", body = "```\n# Example heading\n```")

        // when
        val sheet = mapper.map(document = document, appBar = appBar, onDisplayed = {})

        // then
        assertThat(sheet.document).isEqualTo(document)
    }

    @Test
    fun `keeps the supplied app bar and presentation callback`() {
        // given
        val onDisplayed: () -> Unit = {}

        // when
        val sheet = mapper.map(document = article(), appBar = appBar, onDisplayed = onDisplayed)

        // then
        assertThat(sheet.appBar).isSameInstanceAs(appBar)
        assertThat(sheet.onDisplayed).isSameInstanceAs(onDisplayed)
    }

    private fun MarkdownSheet.bodyContents(): String = document.blocks
        .filterIsInstance<MarkdownBlock.Markdown>()
        .joinToString(separator = "\n\n") { it.rawContents }

    private fun header(metadata: String = ""): ArticleHeader = mapper.map(
        document = article(metadata = metadata),
        appBar = appBar,
        onDisplayed = {},
    ).header

    private fun article(
        metadata: String = "",
        body: String = "# Body heading\nArticle text.",
    ): MarkdownDocument = MarkdownDocument.process(
        text = if (metadata.isEmpty()) {
            body
        } else {
            "---\n$metadata\n---\n$body"
        },
    )
}
