//
// This source file is part of the My Heart Counts open-source project
//
// SPDX-FileCopyrightText: 2026 Stanford University and the project authors (see CONTRIBUTORS.md)
//
// SPDX-License-Identifier: MIT

@file:Suppress(
    "detekt:LoopWithTooManyJumpStatements",
    "detekt:TooManyFunctions",
    "detekt:CyclomaticComplexMethod",
    "detekt:LongMethod",
    "detekt:NestedBlockDepth",
    "detekt:ReturnCount",
)

package edu.stanford.spezi.markdown.internal

import edu.stanford.spezi.markdown.MarkdownBlock
import edu.stanford.spezi.markdown.MarkdownDocument
import edu.stanford.spezi.markdown.MarkdownMetadata
import edu.stanford.spezi.markdown.MarkdownParseException
import edu.stanford.spezi.markdown.MarkdownParseException.Kind
import edu.stanford.spezi.markdown.MarkdownParseException.SourceLocation

/**
 * Processes markdown text into a [MarkdownDocument].
 *
 * Splits the input into a sequence of markdown text blocks and custom elements, parses metadata, and
 * handles HTML-style custom element syntax including nesting, self-closing tags, shorthand closing tags (`</>`) and
 * comments.
 */
internal class MarkdownParser(
    private val input: String,
    private val customElementNames: Set<String>,
) {
    private var pos: Int = 0

    fun parse(): MarkdownDocument {
        val metadata = parseMetadata()
        val blocks = mutableListOf<MarkdownBlock>()
        val currentBlockText = StringBuilder()

        fun terminateCurrentBlock(id: String? = null) {
            blocks.add(
                MarkdownBlock.Markdown(
                    id = id ?: markdownBlockId(currentBlockText),
                    rawContents = currentBlockText.toString(),
                )
            )
            currentBlockText.clear()
        }

        while (currentChar() != null) {
            if (isAtBeginningOfLine() && markdownBlockId(input.subSequence(pos, input.length)) != null) {
                terminateCurrentBlock()
            }
            val char = currentChar()
            val handledAsElement = if (char == '<' && isAtBeginningOfLine()) {
                val element = parseCustomElement()
                when {
                    element == null -> false
                    element.name in customElementNames -> {
                        terminateCurrentBlock()
                        blocks.add(element)
                        true
                    }
                    else -> {
                        currentBlockText.append(element.raw)
                        true
                    }
                }
            } else {
                false
            }
            if (!handledAsElement && !skipCommentIfApplicable()) {
                val remaining = currentChar()
                if (remaining != null) {
                    currentBlockText.append(remaining)
                    consume()
                }
            }
        }
        terminateCurrentBlock()

        val cleaned = blocks.mapNotNull { block ->
            when (block) {
                is MarkdownBlock.Markdown -> {
                    val trimmed = block.rawContents.trim()
                    if (trimmed.isEmpty()) null else MarkdownBlock.Markdown(block.id, trimmed)
                }
                is MarkdownBlock.Element -> block
            }
        }
        return MarkdownDocument(metadata = metadata, cleaned)
    }

    private fun parseMetadata(): MarkdownMetadata {
        if (currentLine() != METADATA_DELIMITER) return MarkdownMetadata(emptyMap())
        val values = LinkedHashMap<String, String>()
        consumeLine()
        while (true) {
            val key = tryParseIdentifier() ?: break
            expectAndConsume(':')
            consumeWhile { it.isWhitespace() && !it.isNewline() }
            val value = currentLine() ?: emitError(Kind.Eof)
            consumeLine()
            values[key] = value
            while (currentLine()?.isEmpty() == true) consumeLine()
        }
        if (currentLine() != METADATA_DELIMITER) emitError(Kind.Other("Unable to find end of metadata header"))
        consumeLine()
        return MarkdownMetadata(values = values.toMap())
    }

    private fun parseCustomElement(): MarkdownBlock.Element? {
        consumeWhile { it.isWhitespace() }
        val startPos = pos
        val next = peek()
        if (currentChar() != '<' || next == null || !next.isValidIdentifierStart()) return null
        expectAndConsume('<')
        val name = parseIdentifier()
        val attributes = mutableListOf<MarkdownBlock.Element.Attribute>()
        var elementIsClosed = false
        openingTag@ while (true) {
            val char = currentChar()
            when {
                char == null -> break@openingTag
                char == '>' -> { consume(); break@openingTag }
                char == '/' && peek() == '>' -> { consume(2); elementIsClosed = true; break@openingTag }
                char.isWhitespace() -> consume()
                else -> {
                    val attrName = parseIdentifier()
                    val attrValue = if (currentChar() == '=') { consume(); parseAttributeValue() } else ""
                    attributes.add(MarkdownBlock.Element.Attribute(attrName, attrValue))
                }
            }
        }
        if (elementIsClosed) {
            return MarkdownBlock.Element(name, attributes, raw = input.substring(startPos, pos))
        }

        val content = mutableListOf<MarkdownBlock.Element.Content>()
        attemptToClose(name, attributes, content, startPos)?.let { return it }
        while (true) {
            val child = parseCustomElement()
            if (child != null) {
                if (child.name in customElementNames) {
                    content.add(MarkdownBlock.Element.Content.Element(child))
                } else {
                    content.add(MarkdownBlock.Element.Content.Text(child.raw))
                }
            } else {
                val text = parseElementTextContents()
                if (text.isNotEmpty()) {
                    content.add(MarkdownBlock.Element.Content.Text(text))
                } else {
                    attemptToClose(name, attributes, content, startPos)?.let { return it }
                    emitError(Kind.Other("Unable to close $name"))
                }
            }
        }
    }

    private fun attemptToClose(
        name: String,
        attributes: List<MarkdownBlock.Element.Attribute>,
        content: List<MarkdownBlock.Element.Content>,
        startPos: Int,
    ): MarkdownBlock.Element? {
        for (tag in listOf("</>", "</$name>")) {
            if (input.startsWith(tag, pos)) {
                consume(tag.length)
                return MarkdownBlock.Element(name, attributes, content.toList(), raw = input.substring(startPos, pos))
            }
        }
        return null
    }

    /**
     * Parses the text content up to the next `<`.
     */
    private fun parseElementTextContents(): String {
        val text = StringBuilder()
        while (true) {
            val char = currentChar() ?: break
            if (char == '<') break
            text.append(char)
            consume()
        }
        return text.toString().trim()
    }

    private fun parseIdentifier(): String {
        val identifier = StringBuilder()
        val start = currentChar()
        if (start != null && start.isValidIdentifierStart()) {
            identifier.append(start)
            consume()
        } else {
            emitError(Kind.UnexpectedCharacter)
        }
        while (true) {
            val char = currentChar() ?: break
            if (!char.isValidIdentifierPart()) break
            identifier.append(char)
            consume()
        }
        return identifier.toString()
    }

    private fun tryParseIdentifier(): String? = runCatching { parseIdentifier() }.getOrNull()

    private fun parseInteger(): Int? {
        val initialPos = pos
        var value = 0
        val negative = if (currentChar() == '-') { consume(); true } else false
        val firstDigitPos = pos
        while (true) {
            val char = currentChar() ?: break
            if (char.code < ASCII_LIMIT && char in '0'..'9') {
                value = value * RADIX_DECIMAL + (char - '0')
                consume()
            } else {
                break
            }
        }
        if (pos == firstDigitPos) {
            pos = initialPos
            return null
        }
        return if (negative) -value else value
    }

    private fun parseAttributeValue(): String = when {
        currentChar() == '"' -> parseStringLiteral()
        else -> tryParseIdentifier() ?: parseInteger()?.toString() ?: ""
    }

    private fun parseStringLiteral(): String {
        expectAndConsume('"')
        val text = StringBuilder()
        while (true) {
            val char = currentChar() ?: emitError(Kind.Eof)
            val isEscaped = text.takeLastWhile { it == '\\' }.length % 2 != 0
            if (char == '"' && !isEscaped) break
            consume()
            text.append(char)
        }
        expectAndConsume('"')
        return text.toString()
    }

    private fun parseComment(): String? {
        if (!input.startsWith("<!--", pos)) return null
        consume(COMMENT_OPEN.length)
        val body = StringBuilder()
        while (true) {
            if (input.startsWith(COMMENT_CLOSE, pos)) {
                consume(COMMENT_CLOSE.length)
                return body.toString()
            }
            val char = currentChar() ?: break
            body.append(char)
            consume()
        }
        emitError(Kind.Other("Unterminated comment"))
    }

    private fun skipCommentIfApplicable(): Boolean = parseComment() != null

    private fun currentChar(): Char? = input.getOrNull(pos)

    private fun peek(offset: Int = 1): Char? = input.getOrNull(pos + offset)

    private fun currentLine(): String? {
        if (pos >= input.length) return null
        var end = pos
        while (end < input.length && !input[end].isNewline()) end++
        return input.substring(pos, end)
    }

    private fun isAtBeginningOfLine(): Boolean {
        if (pos == 0) return true
        val previous = input[pos - 1]
        if (!previous.isNewline()) return false
        // Treat a CRLF pair as a single line break: the position between \r and \n is not a line start.
        return !(previous == '\r' && currentChar() == '\n')
    }

    private fun consume(count: Int = 1) {
        if (count > 0) pos = minOf(input.length, pos + count)
    }

    private inline fun consumeWhile(predicate: (Char) -> Boolean) {
        while (true) {
            val char = currentChar() ?: break
            if (!predicate(char)) break
            consume()
        }
    }

    private fun consumeLine() {
        while (pos < input.length && !input[pos].isNewline()) pos++
        if (pos < input.length && input[pos].isNewline()) {
            val isCarriageReturn = input[pos] == '\r'
            consume()
            if (isCarriageReturn && currentChar() == '\n') consume()
        }
    }

    private fun expectAndConsume(char: Char) {
        if (currentChar() != char) emitError(Kind.UnexpectedCharacter)
        consume()
    }

    private fun emitError(kind: Kind): Nothing = throw MarkdownParseException(kind, sourceLocation())

    private fun sourceLocation(): SourceLocation {
        var line = 0
        var column = 0
        var index = 0
        while (index < pos) {
            val char = input[index]
            if (char.isNewline()) {
                line++
                column = 0
                // Count a CRLF pair as a single line break.
                if (char == '\r' && index + 1 < pos && input[index + 1] == '\n') index++
            } else {
                column++
            }
            index++
        }
        return SourceLocation(line, column)
    }

    private companion object {
        const val METADATA_DELIMITER = "---"
        const val COMMENT_OPEN = "<!--"
        const val COMMENT_CLOSE = "-->"
        const val ASCII_LIMIT = 128
        const val RADIX_DECIMAL = 10

        /**
         * Derives a hyphen-separated identifier from a markdown block's content if it begins with a heading,
         * matching the heading-anchor behaviour used by common markdown renderers.
         */
        fun markdownBlockId(content: CharSequence): String? {
            fun makeId(title: CharSequence): String? {
                val trimmed = title.trim()
                if (trimmed.isEmpty()) return null
                val isLetter = { char: Char -> char.code < ASCII_LIMIT && char.isLetter() }
                val lowered = trimmed.map { if (it in 'A'..'Z') it + ASCII_LOWERCASE_OFFSET else it }
                var start = 0
                var end = lowered.size
                while (start < end && !isLetter(lowered[start])) start++
                while (end > start && !isLetter(lowered[end - 1])) end--
                if (start >= end) return null
                return buildString {
                    for (index in start until end) {
                        val char = lowered[index]
                        append(if (isLetter(char)) char else '-')
                    }
                }
            }

            if (content.startsWith("#")) {
                val firstLineEnd = indexOfNewline(content).let { if (it == -1) content.length else it }
                val firstLine = content.subSequence(0, firstLineEnd)
                val spaceIndex = firstLine.indexOf(' ')
                if (spaceIndex == -1) return null
                return makeId(firstLine.subSequence(spaceIndex + 1, firstLine.length))
            }
            val firstLineEnd = indexOfNewline(content)
            if (firstLineEnd == -1) return null
            var secondStart = firstLineEnd + 1
            if (content[firstLineEnd] == '\r' && secondStart < content.length && content[secondStart] == '\n') {
                secondStart++
            }
            val secondLineEnd = indexOfNewline(content, secondStart).let { if (it == -1) content.length else it }
            val firstLine = content.subSequence(0, firstLineEnd)
            val secondLine = content.subSequence(secondStart, secondLineEnd)
            return if (secondLine.isNotEmpty() && (secondLine.all { it == '=' } || secondLine.all { it == '-' })) {
                makeId(firstLine)
            } else {
                null
            }
        }

        const val ASCII_LOWERCASE_OFFSET = 32

        fun Char.isValidIdentifierStart(): Boolean = this in 'a'..'z' || this in 'A'..'Z' || this == '_'

        fun Char.isValidIdentifierPart(): Boolean = isValidIdentifierStart() || this in '0'..'9' || this == '-'

        /**
         * Whether this character is a line break, matching the set of Unicode newline characters.
         */
        fun Char.isNewline(): Boolean = when (this) {
            '\n', // line feed
            '\u000B', // vertical tab
            '\u000C', // form feed
            '\r', // carriage return
            '\u0085', // next line
            '\u2028', // line separator
            '\u2029', // paragraph separator
            -> true

            else -> false
        }

        /**
         * The index of the first newline character in [content] at or after [start], or -1 if none.
         */
        fun indexOfNewline(content: CharSequence, start: Int = 0): Int {
            for (index in start until content.length) {
                if (content[index].isNewline()) return index
            }
            return -1
        }
    }
}
