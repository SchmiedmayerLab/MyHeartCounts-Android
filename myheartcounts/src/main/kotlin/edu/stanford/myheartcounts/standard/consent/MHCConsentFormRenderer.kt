//
// This source file is part of the My Heart Counts Android open-source project
//
// SPDX-FileCopyrightText: 2026 Stanford University and the project authors (see CONTRIBUTORS.md)
//
// SPDX-License-Identifier: MIT

package edu.stanford.myheartcounts.standard.consent

import edu.stanford.myheartcounts.study.StudyVariantProvider
import org.grovealliance.consent.ConsentResponses
import org.grovealliance.markdown.MarkdownBlock
import org.grovealliance.markdown.MarkdownDocument

/**
 * Renders a signed consent form to a PDF, as the participant saw it on the consent screen.
 */
interface MHCConsentFormRenderer {

    /**
     * Renders [document] with [responses] on the paper size of the participant's region.
     *
     * Elements the participant left untouched are rendered and returned with the initial value the
     * document gives them.
     */
    suspend fun render(
        document: MarkdownDocument,
        responses: ConsentResponses,
    ): RenderedConsentForm
}

/**
 * A consent form rendered to [pdf], together with the [responses] it shows.
 */
// Not a data class: generated equality would compare the PDF bytes by reference.
@Suppress("detekt:UseDataClass")
class RenderedConsentForm(
    val responses: ConsentResponses,
    val pdf: ByteArray,
)

/**
 * Default [MHCConsentFormRenderer] implementation.
 */
class MHCConsentFormRendererImpl(
    private val variantProvider: StudyVariantProvider,
    private val renderer: ConsentPdfRenderer,
) : MHCConsentFormRenderer {

    override suspend fun render(
        document: MarkdownDocument,
        responses: ConsentResponses,
    ): RenderedConsentForm {
        val completeResponses = responses.withInitialValues(document = document)
        val pdf = renderer.render(
            document = document,
            responses = completeResponses,
            paperSize = ConsentPaperSize.forRegion(
                regionCode = variantProvider.currentVariant.regionCode,
            ),
        )
        return RenderedConsentForm(
            responses = completeResponses,
            pdf = pdf,
        )
    }

    /**
     * These responses completed with the initial value [document] gives every toggle and select the
     * participant left untouched, so they hold each value as the consent screen showed it.
     *
     * A toggle without an initial value starts off; a select without one, or with one naming none of
     * its options, starts with no selection.
     */
    private fun ConsentResponses.withInitialValues(document: MarkdownDocument): ConsentResponses {
        val elements = document.blocks.filterIsInstance<MarkdownBlock.Element>()
        val initialToggles = elements
            .filter { it.name == MHCConsentDocumentProvider.TAG_TOGGLE }
            .mapNotNull { element ->
                element.id?.let { id -> id to (element.attribute(ATTR_INITIAL_VALUE)?.toBooleanStrictOrNull() ?: false) }
            }
            .toMap()
        val initialSelects = elements
            .filter { it.name == MHCConsentDocumentProvider.TAG_SELECT }
            .mapNotNull { element -> element.id?.let { id -> id to initialSelection(select = element) } }
            .toMap()
        return copy(
            toggles = initialToggles + toggles,
            selects = initialSelects + selects,
        )
    }

    /**
     * The option [select] starts with, or no selection when it names none of its options.
     */
    private fun initialSelection(select: MarkdownBlock.Element): String {
        val optionIds = select.content
            .filterIsInstance<MarkdownBlock.Element.Content.Element>()
            .map { it.element }
            .filter { it.name == MHCConsentDocumentProvider.TAG_OPTION }
            .mapNotNull { it.id }
        return select.attribute(ATTR_INITIAL_VALUE)?.takeIf { it in optionIds } ?: NO_SELECTION
    }

    private companion object {
        const val ATTR_INITIAL_VALUE = "initial-value"
        const val NO_SELECTION = ""
    }
}
