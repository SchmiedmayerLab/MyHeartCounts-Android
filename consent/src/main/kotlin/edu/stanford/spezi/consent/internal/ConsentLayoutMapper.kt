package edu.stanford.spezi.consent.internal

import edu.stanford.spezi.core.time.DateFormat
import edu.stanford.spezi.core.time.DateFormatter
import edu.stanford.spezi.core.time.TimeProvider
import edu.stanford.spezi.markdown.MarkdownBlock
import edu.stanford.spezi.markdown.MarkdownDocument
import edu.stanford.spezi.resources.Strings
import edu.stanford.spezi.ui.AsyncTextButton
import edu.stanford.spezi.ui.ComposableContent
import edu.stanford.spezi.ui.StringResource
import kotlinx.coroutines.flow.map

/**
 * Maps a [MarkdownDocument] to an ordered list of consent form content items.
 *
 * Elements that cannot be mapped (missing id, missing required text, unknown tag) are silently
 * skipped rather than raising an error, so a document with future or unrecognised elements
 * degrades gracefully.
 */
internal interface ConsentLayoutMapper {
    fun map(input: ConsentLayoutInput): ConsentContentLayout
}

internal class ConsentLayoutMapperImpl(
    private val dateFormatter: DateFormatter,
    private val timeProvider: TimeProvider,
) : ConsentLayoutMapper {

    override fun map(input: ConsentLayoutInput): ConsentContentLayout {
        val document = input.document
        val sections = document.blocks.mapNotNull { block ->
            when (block) {
                is MarkdownBlock.Markdown -> ConsentMarkdownContent(block)
                is MarkdownBlock.Element -> {
                    val id = block.id ?: return@mapNotNull null
                    when (block.name) {
                        ConsentConstants.TAG_TOGGLE -> mapToggle(block, id, input)
                        ConsentConstants.TAG_SELECT -> mapSelect(block, id, input)
                        ConsentConstants.TAG_SIGNATURE -> mapSignature(id, input)
                        else -> null
                    }
                }
            }
        }
        return ConsentContentLayout(
            sections = sections,
            actionButton = input.mainActionEnabled.map { enabled ->
                AsyncTextButton(
                    title = StringResource(Strings.consent_continue_button),
                    enabled = enabled,
                    action = { input.mainAction() }
                )
            }
        )
    }

    private fun mapToggle(element: MarkdownBlock.Element, id: String, input: ConsentLayoutInput): ConsentToggleSection? {
        val text = element.content
            .filterIsInstance<MarkdownBlock.Element.Content.Text>()
            .joinToString("") { it.text }
            .trim()
            .takeIf { it.isNotEmpty() } ?: return null
        val initialValue = element.attribute(ConsentConstants.ATTR_INITIAL_VALUE)?.toBooleanStrictOrNull() ?: false
        val expectedValue = element.attribute(ConsentConstants.ATTR_EXPECTED_VALUE)?.toBooleanStrictOrNull()
        return ConsentToggleSection(
            id = id,
            text = text,
            initialValue = initialValue,
            expectedValue = expectedValue,
            checked = input.responses.map { it.toggles[id] ?: initialValue },
            onValueChanged = { value -> input.actionSink.push(ConsentAction.ToggleChanged(id, value)) },
        )
    }

    @Suppress("detekt:CyclomaticComplexMethod")
    private fun mapSelect(
        element: MarkdownBlock.Element,
        id: String,
        input: ConsentLayoutInput,
    ): ConsentSelectSection? {
        val text = element.content
            .filterIsInstance<MarkdownBlock.Element.Content.Text>()
            .joinToString("") { it.text }
            .trim()
            .takeIf { it.isNotEmpty() } ?: return null
        val footnote = element.content
            .filterIsInstance<MarkdownBlock.Element.Content.Element>()
            .firstOrNull { it.element.name == ConsentConstants.TAG_FOOTNOTE }
            ?.element?.content
            ?.filterIsInstance<MarkdownBlock.Element.Content.Text>()
            ?.joinToString("") { it.text }
            ?.trim()
            ?.takeIf { it.isNotEmpty() }
        val options = element.content.mapNotNull { item ->
            val nested = (item as? MarkdownBlock.Element.Content.Element)?.element
            val title = (nested?.content?.firstOrNull() as? MarkdownBlock.Element.Content.Text)
                ?.text
                ?.takeIf { it.isNotEmpty() }
            val id = nested?.id
            if (
                nested?.name == ConsentConstants.TAG_OPTION &&
                id != null &&
                title != null
            ) {
                SelectionOption(id, title)
            } else {
                null
            }
        }
        val initialValue = element.attribute(ConsentConstants.ATTR_INITIAL_VALUE)
            ?.takeIf { it.isNotEmpty() && options.any { opt -> opt.id == it } }
            ?: ConsentSelectSection.EMPTY_SELECTION
        val expectedSelection = when (val raw = element.attribute(ConsentConstants.ATTR_EXPECTED_VALUE)) {
            null, "" -> ExpectedSelection.Anything(allowEmptySelection = true)
            ConsentConstants.EXPECTED_SELECTION_ANY -> ExpectedSelection.Anything(allowEmptySelection = false)
            else -> if (options.any { it.id == raw }) {
                ExpectedSelection.Option(raw)
            } else {
                ExpectedSelection.Anything(allowEmptySelection = true)
            }
        }
        return ConsentSelectSection(
            id = id,
            text = text,
            footnote = footnote,
            options = options,
            initialValue = initialValue,
            expectedSelection = expectedSelection,
            selectedId = input.responses.map { it.selects[id] },
            onOptionSelected = { value -> input.actionSink.push(ConsentAction.SelectionChanged(id, value)) },
        )
    }

    private fun mapSignature(id: String, input: ConsentLayoutInput): ComposableContent =
        ConsentSignatureSection(
            id = id,
            metadata = input.responses.map { it.signatures[id] ?: input.initialMetadata },
            dateText = dateFormatter.formatDefaultZoneId(
                instant = timeProvider.nowInstant(),
                format = DateFormat.Custom("dd.MM.yyyy"),
            ),
            onFirstNameChanged = { value -> input.actionSink.push(ConsentAction.FirstNameChanged(id, value)) },
            onLastNameChanged = { value -> input.actionSink.push(ConsentAction.LastNameChanged(id, value)) },
            onSignatureStrokesChanged = { strokes -> input.actionSink.push(ConsentAction.SignatureStrokesChanged(id, strokes)) },
        )
}
