//
// This source file is part of the My Heart Counts Android open-source project
//
// SPDX-FileCopyrightText: 2026 Stanford University and the project authors (see CONTRIBUTORS.md)
//
// SPDX-License-Identifier: MIT

package edu.stanford.spezi.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Search
import androidx.compose.material3.Icon
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.tooling.preview.PreviewParameter
import androidx.compose.ui.tooling.preview.PreviewParameterProvider
import androidx.compose.ui.unit.dp
import edu.stanford.spezi.ui.theme.Colors
import edu.stanford.spezi.ui.theme.Sizes
import edu.stanford.spezi.ui.theme.Spacings
import edu.stanford.spezi.ui.theme.SpeziTheme
import edu.stanford.spezi.ui.theme.ThemePreviews

/**
 * A single-line search field: a leading search icon pinned at the start, followed by a borderless
 * text input that fills the remaining width.
 *
 * @property value The current query text.
 * @property placeholder Prompt shown while [value] is empty.
 * @property onValueChanged Invoked with the updated query on every edit.
 */
data class SpeziSearchField(
    val value: String,
    val placeholder: StringResource? = null,
    val onValueChanged: (String) -> Unit,
) : ComposableContent {

    @Composable
    override fun Content(modifier: Modifier) {
        SpeziSearchFieldComposable(
            modifier = modifier,
            value = value,
            placeholder = placeholder?.text(),
            onValueChanged = onValueChanged,
        )
    }
}

@Composable
fun SpeziSearchFieldComposable(
    modifier: Modifier = Modifier,
    value: String,
    placeholder: String? = null,
    onValueChanged: (String) -> Unit,
) {
    Row(
        modifier = modifier
            .clip(CircleShape)
            .background(color = Colors.surfaceContainerLowest),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Icon(
            imageVector = Icons.Outlined.Search,
            contentDescription = null,
            tint = Colors.primary,
            modifier = Modifier
                .padding(start = Spacings.small)
                .size(Sizes.Icon.small),
        )
        SpeziInputFieldComposable(
            modifier = Modifier.weight(1f),
            value = value,
            placeholder = placeholder,
            showBorder = false,
            imeAction = ImeAction.Search,
            onValueChanged = onValueChanged,
        )
    }
}

private class SearchFieldPreviewParams : PreviewParameterProvider<SpeziSearchField> {
    private val base = SpeziSearchField(
        value = "",
        placeholder = StringResource("Search"),
        onValueChanged = {},
    )

    override val values: Sequence<SpeziSearchField> = sequenceOf(
        base,
        base.copy(value = "Cardiology"),
    )
}

@ThemePreviews
@Composable
private fun Preview(@PreviewParameter(SearchFieldPreviewParams::class) input: SpeziSearchField) {
    SpeziTheme {
        input.Content(modifier = Modifier.width(300.dp))
    }
}
