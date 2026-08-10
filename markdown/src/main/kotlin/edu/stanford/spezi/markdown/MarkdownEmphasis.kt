//
// This source file is part of the My Heart Counts Android open-source project
//
// SPDX-FileCopyrightText: 2026 Stanford University and the project authors (see CONTRIBUTORS.md)
//
// SPDX-License-Identifier: MIT

package edu.stanford.spezi.markdown

/**
 * An inline emphasis applied to a run of markdown text.
 */
enum class EmphasisStyle {
    /**
     * Strong emphasis introduced by paired `**` or `__` delimiters.
     */
    Bold,

    /**
     * Emphasis introduced by paired `*` or `_` delimiters.
     */
    Italic,
}

/**
 * Markdown text with its inline emphasis delimiters removed and recorded as [spans].
 *
 * [text] is the source with `*`/`_` emphasis markers stripped; each [EmphasisSpan.range] indexes
 * into it. Nested emphasis produces overlapping spans (e.g. italic within bold).
 */
data class EmphasisText(val text: String, val spans: List<EmphasisSpan>)

/**
 * An [EmphasisStyle] applied to the index [range] of an [EmphasisText.text].
 *
 * @property range The inclusive index range within the emphasis-stripped text.
 */
data class EmphasisSpan(val range: IntRange, val style: EmphasisStyle)

/**
 * Strips inline emphasis markers from [text], returning the cleaned text with the [EmphasisStyle]
 * spans that applied to it.
 *
 * Recognizes `**`/`__` as [EmphasisStyle.Bold] and `*`/`_` as [EmphasisStyle.Italic], nested to any
 * depth. Delimiters without a matching closer are left in the text verbatim.
 */
fun parseEmphasis(text: String): EmphasisText {
    val out = StringBuilder()
    val spans = mutableListOf<EmphasisSpan>()
    appendEmphasis(text, out, spans)
    return EmphasisText(out.toString(), spans)
}

// --- Private helpers ---

private data class EmphasisDelimiter(val literal: String, val style: EmphasisStyle)

private fun appendEmphasis(segment: String, out: StringBuilder, spans: MutableList<EmphasisSpan>) {
    var index = 0
    while (index < segment.length) {
        val delimiter = emphasisDelimiterAt(segment, index)
        val close = delimiter?.let { segment.indexOf(it.literal, startIndex = index + it.literal.length) } ?: -1
        if (delimiter != null && close >= 0) {
            val start = out.length
            appendEmphasis(segment.substring(index + delimiter.literal.length, close), out, spans)
            if (out.length > start) spans.add(EmphasisSpan(start until out.length, delimiter.style))
            index = close + delimiter.literal.length
        } else {
            out.append(segment[index])
            index++
        }
    }
}

private fun emphasisDelimiterAt(text: String, index: Int): EmphasisDelimiter? {
    val char = text[index]
    if (char != '*' && char != '_') return null
    val isStrong = index + 1 < text.length && text[index + 1] == char
    return if (isStrong) {
        EmphasisDelimiter("$char$char", EmphasisStyle.Bold)
    } else {
        EmphasisDelimiter("$char", EmphasisStyle.Italic)
    }
}
