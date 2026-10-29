//
// This source file is part of the My Heart Counts Android open-source project
//
// SPDX-FileCopyrightText: 2026 Stanford University and the project authors (see CONTRIBUTORS.md)
//
// SPDX-License-Identifier: MIT

package edu.stanford.myheartcounts.ui

import edu.stanford.myheartcounts.model.StudyVariant
import edu.stanford.myheartcounts.study.StudyVariantProvider
import org.grovealliance.core.time.TimeProvider
import org.grovealliance.markdown.MarkdownBlock
import org.grovealliance.markdown.MarkdownDocument
import org.grovealliance.ui.GroveAppBar
import org.grovealliance.ui.OnActionVoid
import org.grovealliance.ui.StringResource
import java.net.URI
import java.net.URISyntaxException
import java.time.Instant
import java.time.format.DateTimeFormatter
import java.time.format.DateTimeParseException
import java.time.format.FormatStyle
import java.util.Locale

/**
 * Prepares an article's header and body for presentation in a sheet.
 */
interface ArticleMapper {
    fun map(document: MarkdownDocument, appBar: GroveAppBar, onDisplayed: OnActionVoid): MarkdownSheet
}

class ArticleMapperImpl(
    private val timeProvider: TimeProvider,
    private val variantProvider: StudyVariantProvider,
) : ArticleMapper {

    override fun map(
        document: MarkdownDocument,
        appBar: GroveAppBar,
        onDisplayed: OnActionVoid,
    ): MarkdownSheet = MarkdownSheet(
        appBar = appBar,
        document = body(document = document),
        header = header(document = document),
        onDisplayed = onDisplayed,
    )

    private fun header(document: MarkdownDocument): ArticleHeader = ArticleHeader(
        title = StringResource(text = document.metadata.title.orEmpty()),
        date = document.metadata["date"]?.let(::date),
        tags = document.metadata["tags"].orEmpty()
            .split(',')
            .map(String::trim)
            .filter(String::isNotEmpty)
            .map { StringResource(text = it) },
        image = headerImage(value = document.metadata["headerImage"]),
    )

    private fun body(document: MarkdownDocument): MarkdownDocument {
        val first = document.blocks.firstOrNull()
        if (document.metadata.title.isNullOrBlank() || first !is MarkdownBlock.Markdown) return document
        val contents = first.rawContents.replaceFirst(LEADING_TITLE, "").trimStart('\r', '\n')
        if (contents == first.rawContents) return document
        val remaining = document.blocks.drop(1)
        return document.copy(
            blocks = if (contents.isBlank()) remaining else listOf(first.copy(rawContents = contents)) + remaining,
        )
    }

    private fun date(value: String): StringResource? = try {
        val instant = Instant.parse(value)
        val formatter = DateTimeFormatter.ofLocalizedDate(FormatStyle.MEDIUM)
            .withLocale(Locale.getDefault())
            .withZone(timeProvider.currentZone())
        StringResource(text = formatter.format(instant))
    } catch (_: DateTimeParseException) {
        null
    }

    private fun headerImage(value: String?): ArticleHeader.HeaderImage? = when {
        value == null -> ArticleHeader.HeaderImage.Stanford.takeIf { variantProvider.currentVariant == StudyVariant.Stanford }
        value == "stanford" -> ArticleHeader.HeaderImage.Stanford
        value.startsWith("https://") && isRemoteImage(value = value) -> ArticleHeader.HeaderImage.Remote(url = value)
        else -> null
    }

    private fun isRemoteImage(value: String): Boolean = try {
        !URI(value).host.isNullOrBlank()
    } catch (_: URISyntaxException) {
        false
    }

    private companion object {
        val LEADING_TITLE = Regex("\\A(?:[ \\t]*\\r?\\n)* {0,3}#(?:[ \\t]+[^\\r\\n]*)?[ \\t]*(?:\\r?\\n|$)")
    }
}
