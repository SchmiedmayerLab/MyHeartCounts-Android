//
// This source file is part of the My Heart Counts Android open-source project
//
// SPDX-FileCopyrightText: 2026 Stanford University and the project authors (see CONTRIBUTORS.md)
//
// SPDX-License-Identifier: MIT

package edu.stanford.myheartcounts.standard.consent

import android.graphics.Color
import android.graphics.Paint
import android.graphics.Path
import android.graphics.Typeface
import android.graphics.pdf.PdfDocument
import android.text.Layout
import android.text.SpannableString
import android.text.Spanned
import android.text.StaticLayout
import android.text.TextPaint
import android.text.style.StyleSpan
import org.grovealliance.consent.ConsentResponses
import org.grovealliance.consent.SignatureMetadata
import org.grovealliance.markdown.EmphasisStyle
import org.grovealliance.markdown.MarkdownBlock
import org.grovealliance.markdown.MarkdownDocument
import org.grovealliance.markdown.MarkdownNode
import org.grovealliance.markdown.nodes
import org.grovealliance.markdown.parseEmphasis
import java.io.ByteArrayOutputStream
import kotlin.math.max
import kotlin.math.min

/**
 * The page size a consent PDF is rendered at.
 *
 * Dimensions are in PostScript points (1/72 inch), the unit [PdfDocument] works in.
 */
enum class ConsentPaperSize(val widthPoints: Int, val heightPoints: Int) {

    /**
     * 8.5 × 11 inches. Used for participants in the United States.
     */
    US_LETTER(widthPoints = 612, heightPoints = 792),

    /**
     * 210 × 297 mm. Used everywhere else. Matches iOS, which picks the paper size the same way.
     */
    A4(widthPoints = 595, heightPoints = 842),
    ;

    companion object {

        /**
         * The paper size used in [regionCode].
         *
         * @param regionCode An ISO 3166-1 alpha-2 country code.
         */
        fun forRegion(regionCode: String?): ConsentPaperSize =
            if (regionCode == US_REGION) US_LETTER else A4

        private const val US_REGION = "US"
    }
}

/**
 * Renders a signed consent document to a PDF.
 *
 * The study keeps a copy of what each participant actually agreed to, and a PDF is what the ethics
 * approval and the participant themselves expect to be able to read back. iOS gets its PDF from
 * SpeziOnboarding's export machinery; Android has no equivalent, so the pages are drawn here — but
 * off Grove's own [MarkdownDocument] model rather than a second markdown implementation, so what
 * this renders and what the consent screen showed cannot drift apart.
 *
 * Interactive elements are rendered with the participant's own answer beside them, because a consent
 * record that omits what they chose is not a record of their consent.
 */
class ConsentPdfRenderer {

    /**
     * Renders [document] with the participant's [responses] to a PDF.
     *
     * @param document The consent document, parsed by Grove's markdown module.
     * @param responses What the participant answered, whose signature is drawn at the end.
     * @param paperSize The page size to render at.
     * @return The PDF bytes.
     */
    fun render(
        document: MarkdownDocument,
        responses: ConsentResponses,
        paperSize: ConsentPaperSize,
    ): ByteArray {
        val pdf = PdfDocument()
        val writer = PageWriter(pdf = pdf, paperSize = paperSize)
        try {
            document.metadata.title?.let { writer.writeHeading(text = it, level = 1) }
            document.blocks.forEach { block -> writeBlock(writer, block, responses) }
            writer.writeSignatures(signatures = responses.signatures.values.toList())
            writer.finish()

            return ByteArrayOutputStream().use { stream ->
                pdf.writeTo(stream)
                stream.toByteArray()
            }
        } finally {
            // Closing a document with a page still open throws, which on a failure part-way through
            // would replace whatever actually went wrong with a useless "Current page not finished".
            // `finish` is idempotent, so this is a no-op on the success path.
            writer.finish()
            pdf.close()
        }
    }

    private fun writeBlock(writer: PageWriter, block: MarkdownBlock, responses: ConsentResponses) {
        when (block) {
            is MarkdownBlock.Markdown -> block.nodes().forEach { node -> writeNode(writer, node) }
            is MarkdownBlock.Element -> writeElement(writer, block, responses)
        }
    }

    private fun writeNode(writer: PageWriter, node: MarkdownNode) {
        when (node) {
            is MarkdownNode.Heading -> writer.writeHeading(text = node.text, level = node.level)
            is MarkdownNode.Paragraph -> writer.writeParagraph(text = node.text)
            is MarkdownNode.ListItem -> writer.writeParagraph(
                text = "$BULLET ${node.text}",
                indent = node.nestingLevel * LIST_INDENT,
            )
        }
    }

    /**
     * Writes an interactive element as its prompt followed by what the participant answered.
     *
     * A signature element contributes nothing here: its strokes are drawn once at the end of the
     * document, where a signature belongs.
     */
    private fun writeElement(
        writer: PageWriter,
        element: MarkdownBlock.Element,
        responses: ConsentResponses,
    ) {
        if (element.name == TAG_SIGNATURE) return

        val prompt = element.content
            .filterIsInstance<MarkdownBlock.Element.Content.Text>()
            .joinToString(separator = " ") { it.text.trim() }
            .trim()
        if (prompt.isNotEmpty()) writer.writeParagraph(text = prompt)

        val answer = when (element.name) {
            TAG_TOGGLE -> responses.toggles[element.id]?.let { if (it) ANSWER_YES else ANSWER_NO }
            TAG_SELECT -> responses.selects[element.id]?.let { optionId ->
                optionLabel(element = element, optionId = optionId) ?: optionId
            }
            else -> null
        } ?: return
        writer.writeParagraph(text = "$ANSWER_PREFIX $answer", bold = true, indent = LIST_INDENT)
    }

    /**
     * The label of the `<option>` with [optionId], so the PDF reads back what the participant saw
     * rather than the identifier the document happens to use for it.
     */
    private fun optionLabel(element: MarkdownBlock.Element, optionId: String): String? =
        element.content
            .filterIsInstance<MarkdownBlock.Element.Content.Element>()
            .map { it.element }
            .firstOrNull { it.name == TAG_OPTION && it.id == optionId }
            ?.content
            ?.filterIsInstance<MarkdownBlock.Element.Content.Text>()
            ?.joinToString(separator = " ") { it.text.trim() }
            ?.trim()
            ?.takeIf { it.isNotEmpty() }

    /**
     * Lays out the document top to bottom, starting a new page whenever the current one fills up.
     */
    private class PageWriter(
        private val pdf: PdfDocument,
        private val paperSize: ConsentPaperSize,
    ) {

        private var page: PdfDocument.Page = startPage(number = 1)
        private var pageNumber = 1
        private var cursorY = MARGIN
        private var isFinished = false

        fun writeHeading(text: String, level: Int) {
            val size = HEADING_TEXT_SIZE - (level - 1) * HEADING_SIZE_STEP
            write(
                text = text,
                paint = paint(size = max(size, BODY_TEXT_SIZE), bold = true),
                indent = 0f,
                spacingBefore = HEADING_SPACING,
            )
        }

        fun writeParagraph(text: String, bold: Boolean = false, indent: Float = 0f) {
            write(
                text = text,
                paint = paint(size = BODY_TEXT_SIZE, bold = bold),
                indent = indent,
                spacingBefore = PARAGRAPH_SPACING,
            )
        }

        /**
         * Draws each signature's strokes above a rule with the signer's name below it.
         *
         * The strokes come in the signature canvas' own coordinate space, so they are scaled to the
         * box drawn for them — by one factor on both axes, so the signature is not distorted.
         */
        fun writeSignatures(signatures: List<SignatureMetadata>) {
            if (signatures.isEmpty()) return
            val strokePaint = Paint().apply {
                color = Color.BLACK
                style = Paint.Style.STROKE
                strokeWidth = SIGNATURE_STROKE_WIDTH
                isAntiAlias = true
            }
            val contentWidth = paperSize.widthPoints - 2 * MARGIN

            signatures.forEach { signature ->
                ensureSpace(height = SIGNATURE_HEIGHT + SIGNATURE_BLOCK_EXTRA)
                cursorY += PARAGRAPH_SPACING
                drawStrokes(
                    signature = signature,
                    left = MARGIN,
                    top = cursorY,
                    width = contentWidth,
                    height = SIGNATURE_HEIGHT,
                    paint = strokePaint,
                )
                cursorY += SIGNATURE_HEIGHT
                page.canvas.drawLine(MARGIN, cursorY, MARGIN + contentWidth, cursorY, strokePaint)
                cursorY += PARAGRAPH_SPACING
                writeParagraph(text = "${signature.givenName} ${signature.familyName}".trim())
            }
        }

        /**
         * Closes the page currently being written. Safe to call more than once, so the caller can
         * use it both on the success path and from a `finally`.
         */
        fun finish() {
            if (isFinished) return
            isFinished = true
            pdf.finishPage(page)
        }

        /**
         * Draws [text] at the cursor, splitting it across pages when it does not fit on one.
         *
         * `StaticLayout` measures the whole run as one block, so a page break falls on a line
         * boundary — the offset of the first line that no longer fits — rather than mid-line.
         */
        private fun write(text: String, paint: TextPaint, indent: Float, spacingBefore: Float) {
            if (text.isBlank()) return
            val width = (paperSize.widthPoints - 2 * MARGIN - indent).toInt()
            // Measured over the styled text's own length: stripping the emphasis markers makes it
            // shorter than the source, so the source's length would run off the end of it.
            val styled = styled(text)
            val layout = StaticLayout.Builder
                .obtain(styled, 0, styled.length, paint, width)
                .setAlignment(Layout.Alignment.ALIGN_NORMAL)
                .build()

            cursorY += spacingBefore
            var line = 0
            while (line < layout.lineCount) {
                ensureSpace(height = (layout.getLineBottom(line) - layout.getLineTop(line)).toFloat())
                val top = layout.getLineTop(line)
                val last = lastLineFitting(layout = layout, from = line, height = remainingHeight())
                val bottom = layout.getLineBottom(last)

                page.canvas.save()
                page.canvas.translate(MARGIN + indent, cursorY - top)
                page.canvas.clipRect(0f, top.toFloat(), width.toFloat(), bottom.toFloat())
                layout.draw(page.canvas)
                page.canvas.restore()

                cursorY += (bottom - top).toFloat()
                line = last + 1
            }
        }

        /**
         * [text] with its markdown emphasis markers turned into real bold and italic runs, via
         * Grove's own emphasis parser.
         */
        private fun styled(text: String): CharSequence {
            val emphasis = parseEmphasis(text)
            val spannable = SpannableString(emphasis.text)
            emphasis.spans.forEach { span ->
                val style = when (span.style) {
                    EmphasisStyle.Bold -> Typeface.BOLD
                    EmphasisStyle.Italic -> Typeface.ITALIC
                }
                val end = min(span.range.last + 1, spannable.length)
                if (span.range.first < end) {
                    spannable.setSpan(StyleSpan(style), span.range.first, end, Spanned.SPAN_INCLUSIVE_EXCLUSIVE)
                }
            }
            return spannable
        }

        private fun drawStrokes(
            signature: SignatureMetadata,
            left: Float,
            top: Float,
            width: Float,
            height: Float,
            paint: Paint,
        ) {
            val points = signature.strokes.flatMap { it.points }
            if (points.isEmpty()) return

            val minX = points.minOf { it.x }
            val minY = points.minOf { it.y }
            val scale = min(
                width / max(points.maxOf { it.x } - minX, MIN_EXTENT),
                height / max(points.maxOf { it.y } - minY, MIN_EXTENT),
            )
            signature.strokes.forEach { stroke ->
                val path = Path()
                stroke.points.forEachIndexed { index, point ->
                    val pointX = left + (point.x - minX) * scale
                    val pointY = top + (point.y - minY) * scale
                    if (index == 0) path.moveTo(pointX, pointY) else path.lineTo(pointX, pointY)
                }
                page.canvas.drawPath(path, paint)
            }
        }

        /**
         * Starts a new page when less than [height] is left on the current one.
         */
        private fun ensureSpace(height: Float) {
            if (remainingHeight() >= height) return
            pdf.finishPage(page)
            pageNumber++
            page = startPage(number = pageNumber)
            cursorY = MARGIN
        }

        private fun remainingHeight(): Float = paperSize.heightPoints - MARGIN - cursorY

        /**
         * The last line of [layout], starting at [from], that still fits within [height].
         *
         * Never less than [from]: a line taller than a whole page still has to be drawn somewhere,
         * and returning less would not make progress.
         */
        private fun lastLineFitting(layout: StaticLayout, from: Int, height: Float): Int {
            val top = layout.getLineTop(from)
            var last = from
            while (last + 1 < layout.lineCount && layout.getLineBottom(last + 1) - top <= height) {
                last++
            }
            return last
        }

        private fun paint(size: Float, bold: Boolean) = TextPaint().apply {
            color = Color.BLACK
            textSize = size
            isAntiAlias = true
            if (bold) typeface = Typeface.DEFAULT_BOLD
        }

        private fun startPage(number: Int): PdfDocument.Page = pdf.startPage(
            PdfDocument.PageInfo
                .Builder(paperSize.widthPoints, paperSize.heightPoints, number)
                .create(),
        )
    }

    private companion object {
        const val MARGIN = 48f
        const val BODY_TEXT_SIZE = 11f
        const val HEADING_TEXT_SIZE = 20f
        const val HEADING_SIZE_STEP = 2f
        const val PARAGRAPH_SPACING = 8f
        const val HEADING_SPACING = 16f
        const val LIST_INDENT = 16f
        const val SIGNATURE_HEIGHT = 80f
        const val SIGNATURE_STROKE_WIDTH = 1.5f

        /**
         * Room for the rule and the printed name below a signature.
         */
        const val SIGNATURE_BLOCK_EXTRA = 48f

        /**
         * Guards against dividing by zero when a signature is a single point or a straight line.
         */
        const val MIN_EXTENT = 1f

        const val BULLET = "•"
        const val ANSWER_PREFIX = "Answer:"
        const val ANSWER_YES = "Yes"
        const val ANSWER_NO = "No"

        const val TAG_TOGGLE = MHCConsentDocumentProvider.TAG_TOGGLE
        const val TAG_SELECT = MHCConsentDocumentProvider.TAG_SELECT
        const val TAG_SIGNATURE = MHCConsentDocumentProvider.TAG_SIGNATURE
        const val TAG_OPTION = MHCConsentDocumentProvider.TAG_OPTION
    }
}
