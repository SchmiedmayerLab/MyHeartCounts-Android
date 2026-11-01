//
// This source file is part of the My Heart Counts Android open-source project
//
// SPDX-FileCopyrightText: 2026 Stanford University and the project authors (see CONTRIBUTORS.md)
// SPDX-License-Identifier: MIT
//

package edu.stanford.myheartcounts.dashboard.entry

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.PreviewParameter
import androidx.compose.ui.tooling.preview.PreviewParameterProvider
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import org.grovealliance.ui.ComposableContent
import org.grovealliance.ui.GroveInputFieldComposable
import org.grovealliance.ui.StringResource
import org.grovealliance.ui.theme.Colors
import org.grovealliance.ui.theme.GroveTheme
import org.grovealliance.ui.theme.Sizes
import org.grovealliance.ui.theme.Spacings
import org.grovealliance.ui.theme.TextStyles
import org.grovealliance.ui.theme.ThemePreviews

/**
 * A labeled numeric field with an optional unit beside its editable value, which is aligned to the end so it reads next to the unit.
 *
 * @param label the field label
 * @param value the editable text
 * @param placeholder the text shown when the field is empty
 * @param unit the trailing unit, when present
 * @param keyboardType the keyboard offered for the value
 * @param validationMessage the message shown below the row while the value is not accepted
 * @param onValueChange called when the editable value changes
 */
@Immutable
data class EntryFieldRow(
    val label: StringResource,
    val value: StateFlow<String>,
    val placeholder: StringResource,
    val unit: StringResource?,
    val keyboardType: KeyboardType,
    val validationMessage: StateFlow<StringResource?>,
    val onValueChange: (String) -> Unit,
) : ComposableContent {
    @Composable
    override fun Content(modifier: Modifier) {
        Column(modifier = modifier.fillMaxWidth()) {
            Row(
                modifier = Modifier.fillMaxWidth().heightIn(min = Sizes.Content.large),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(Spacings.small),
            ) {
                EntryRowLabel(text = label.text())
                Value()
                unit?.let {
                    Text(
                        text = it.text(),
                        style = TextStyles.bodyLarge,
                        color = Colors.onSurfaceVariant,
                    )
                }
            }
            ValidationMessage()
        }
    }

    @Composable
    private fun ValidationMessage() {
        val message by validationMessage.collectAsStateWithLifecycle()
        message?.let {
            Text(
                text = it.text(),
                style = TextStyles.bodySmall,
                color = Colors.error,
                textAlign = TextAlign.End,
                modifier = Modifier.fillMaxWidth().padding(bottom = Spacings.small),
            )
        }
    }

    @Composable
    private fun Value() {
        val text by value.collectAsStateWithLifecycle()
        GroveInputFieldComposable(
            value = text,
            placeholder = placeholder.text(),
            showBorder = false,
            keyboardType = keyboardType,
            imeAction = ImeAction.Next,
            textAlign = TextAlign.End,
            contentPaddingValues = PaddingValues(Spacings.zero),
            onValueChanged = onValueChange,
            modifier = Modifier.width(Sizes.Content.large - Spacings.medium),
        )
    }
}

private class EntryFieldRowPreviewProvider : PreviewParameterProvider<EntryFieldRow> {
    private val base = EntryFieldRow(
        label = StringResource("Systolic Blood Pressure"),
        value = MutableStateFlow("120"),
        placeholder = StringResource("0"),
        unit = StringResource("mmHg"),
        keyboardType = KeyboardType.Number,
        validationMessage = MutableStateFlow(null),
        onValueChange = {},
    )
    override val values = sequenceOf(
        base,
        base.copy(
            value = MutableStateFlow("300"),
            validationMessage = MutableStateFlow(StringResource("Only values from 60 to 250 are allowed")),
        ),
    )
}

@ThemePreviews
@Composable
private fun Preview(@PreviewParameter(EntryFieldRowPreviewProvider::class) content: EntryFieldRow) {
    GroveTheme { content.Content() }
}
