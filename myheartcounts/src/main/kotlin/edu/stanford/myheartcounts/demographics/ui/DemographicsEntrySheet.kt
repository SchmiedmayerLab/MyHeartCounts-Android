//
// This source file is part of the My Heart Counts Android open-source project
//
// SPDX-FileCopyrightText: 2026 Stanford University and the project authors (see CONTRIBUTORS.md)
//
// SPDX-License-Identifier: MIT

package edu.stanford.myheartcounts.demographics.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.PreviewParameter
import androidx.compose.ui.tooling.preview.PreviewParameterProvider
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import org.grovealliance.ui.BottomSheetComposableContent
import org.grovealliance.ui.GroveAppBar
import org.grovealliance.ui.GroveCard
import org.grovealliance.ui.StaticGroveScaffold
import org.grovealliance.ui.StringResource
import org.grovealliance.ui.groveAppBar
import org.grovealliance.ui.theme.Colors
import org.grovealliance.ui.theme.GroveShapes
import org.grovealliance.ui.theme.GroveTheme
import org.grovealliance.ui.theme.Spacings
import org.grovealliance.ui.theme.TextStyles
import org.grovealliance.ui.theme.ThemePreviews

/**
 * A full-height bottom sheet for typing in one answer across one or more [fields], beneath [appBar],
 * which carries closing and saving.
 *
 * @param appBar the sheet's app bar
 * @param fields the fields the answer is typed into, in order
 * @param entry the text typed into each of [fields] so far, in the same order
 * @param help a link below the entry explaining where to find the answer, if any
 * @param onEntryChange called with every edit of a field, with the field's index and its new text
 */
data class DemographicsEntrySheet(
    val appBar: StateFlow<GroveAppBar>,
    val fields: List<Field>,
    val entry: StateFlow<List<String>>,
    val help: Help?,
    val onEntryChange: (index: Int, text: String) -> Unit,
) : BottomSheetComposableContent {

    override val skipPartiallyExpanded: Boolean = true

    @Composable
    override fun Content(modifier: Modifier) {
        val appBar by appBar.collectAsStateWithLifecycle()
        val entry by entry.collectAsStateWithLifecycle()
        val focusRequester = remember { FocusRequester() }

        StaticGroveScaffold(appBar = appBar) {
            Column(
                modifier = modifier
                    .fillMaxSize()
                    .background(Colors.surfaceContainer)
                    .padding(Spacings.medium),
                verticalArrangement = Arrangement.spacedBy(Spacings.small),
            ) {
                GroveCard(shape = GroveShapes.large) {
                    Column {
                        fields.forEachIndexed { index, field ->
                            if (index > 0) HorizontalDivider(modifier = Modifier.padding(start = Spacings.medium))
                            FieldRow(
                                field = field,
                                text = entry.getOrNull(index).orEmpty(),
                                isLast = index == fields.lastIndex,
                                modifier = if (index == 0) Modifier.focusRequester(focusRequester) else Modifier,
                                onTextChange = { onEntryChange(index, it) },
                            )
                        }
                    }
                }
                help?.let {
                    Text(
                        modifier = Modifier
                            .clickable(onClick = it.onClick)
                            .padding(horizontal = Spacings.medium),
                        text = it.title.text(),
                        style = TextStyles.bodySmall,
                        color = Colors.primary,
                    )
                }
            }
        }

        LaunchedEffect(Unit) {
            focusRequester.requestFocus()
        }
    }

    @Composable
    private fun FieldRow(
        field: Field,
        text: String,
        isLast: Boolean,
        modifier: Modifier,
        onTextChange: (String) -> Unit,
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(Spacings.medium),
            horizontalArrangement = Arrangement.spacedBy(Spacings.small),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                text = field.label.text(),
                style = TextStyles.bodyLarge,
                color = Colors.onSurface,
            )
            BasicTextField(
                modifier = modifier.weight(1f),
                value = text,
                onValueChange = onTextChange,
                singleLine = true,
                textStyle = TextStyles.bodyLarge.copy(
                    color = Colors.onSurface,
                    textAlign = TextAlign.End,
                ),
                keyboardOptions = KeyboardOptions(
                    keyboardType = field.keyboardType,
                    imeAction = if (isLast) ImeAction.Done else ImeAction.Next,
                ),
                cursorBrush = SolidColor(Colors.primary),
                decorationBox = { innerTextField ->
                    Box(contentAlignment = Alignment.CenterEnd) {
                        if (text.isEmpty()) {
                            field.placeholder?.let {
                                Text(
                                    text = it.text(),
                                    style = TextStyles.bodyLarge,
                                    color = Colors.onSurfaceVariant,
                                )
                            }
                        }
                        innerTextField()
                    }
                },
            )
            field.unit?.let {
                Text(
                    text = it.text(),
                    style = TextStyles.bodyLarge,
                    color = Colors.onSurfaceVariant,
                )
            }
        }
    }

    /**
     * A field labelled [label], typed in with [keyboardType] and in [unit], if any, hinting at
     * [placeholder] while it is empty.
     */
    data class Field(
        val label: StringResource,
        val unit: StringResource?,
        val placeholder: StringResource?,
        val keyboardType: KeyboardType,
    )

    /**
     * A link titled [title], opened by [onClick].
     */
    data class Help(
        val title: StringResource,
        val onClick: () -> Unit,
    )
}

private class DemographicsEntrySheetPreviewProvider : PreviewParameterProvider<DemographicsEntrySheet> {
    private val base = DemographicsEntrySheet(
        appBar = MutableStateFlow(
            groveAppBar {
                title(StringResource("NHS Number"))
                close {}
            },
        ),
        fields = listOf(
            DemographicsEntrySheet.Field(
                label = StringResource("NHS Number"),
                unit = null,
                placeholder = null,
                keyboardType = KeyboardType.Number,
            ),
        ),
        entry = MutableStateFlow(listOf("943 476 5919")),
        help = DemographicsEntrySheet.Help(
            title = StringResource("Find your NHS Number"),
            onClick = {},
        ),
        onEntryChange = { _, _ -> },
    )
    override val values = sequenceOf(
        base,
        base.copy(
            fields = listOf(
                DemographicsEntrySheet.Field(
                    label = StringResource("Value"),
                    unit = StringResource("cm"),
                    placeholder = StringResource("0"),
                    keyboardType = KeyboardType.Decimal,
                ),
            ),
            entry = MutableStateFlow(listOf("172.5")),
            help = null,
        ),
        base.copy(
            fields = listOf(
                DemographicsEntrySheet.Field(
                    label = StringResource("Feet"),
                    unit = null,
                    placeholder = StringResource("5"),
                    keyboardType = KeyboardType.Number,
                ),
                DemographicsEntrySheet.Field(
                    label = StringResource("Inches"),
                    unit = null,
                    placeholder = StringResource("4"),
                    keyboardType = KeyboardType.Number,
                ),
            ),
            entry = MutableStateFlow(listOf("", "")),
            help = null,
        ),
    )
}

@ThemePreviews
@Composable
private fun Preview(@PreviewParameter(DemographicsEntrySheetPreviewProvider::class) content: DemographicsEntrySheet) {
    GroveTheme { content.Content() }
}
