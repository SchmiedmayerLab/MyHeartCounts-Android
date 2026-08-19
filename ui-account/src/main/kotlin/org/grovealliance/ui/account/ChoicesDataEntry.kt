//
// This source file is part of the My Heart Counts Android open-source project
//
// SPDX-FileCopyrightText: 2026 Stanford University and the project authors (see CONTRIBUTORS.md)
//
// SPDX-License-Identifier: MIT

package org.grovealliance.ui.account

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import org.grovealliance.ui.ChoicesFormFieldItem
import org.grovealliance.ui.ChoicesFormFieldItemComposable
import org.grovealliance.ui.StringResource
import org.grovealliance.ui.theme.GroveTheme
import org.grovealliance.ui.theme.ThemePreviews

/**
 * Composable for displaying a data entry with a list of choices.
 *
 * @param choices The list of choices.
 * @param style The style of the choices.
 * @param optionTransformer A function that transforms a choice into a [ChoicesFormFieldItem.Option].
 * @param valueTransformer A function that transforms an option id into a value.
 */
data class ChoicesDataEntry<Value>(
    val choices: List<Value>,
    val style: ChoicesFormFieldItem.Style = ChoicesFormFieldItem.Style.Radios,
    val optionTransformer: (Value) -> ChoicesFormFieldItem.Option,
    val valueTransformer: (id: String) -> Value,
) : DataEntryComposable<Value> {

    @Composable
    override fun Content(value: Value, onValueChange: (Value) -> Unit, modifier: Modifier) {
        ChoicesFormFieldItemComposable(
            modifier = modifier,
            style = style,
            options = choices.map { optionTransformer(it) },
            selectedIds = setOf(optionTransformer(value).id),
            onOptionClicked = { onValueChange(valueTransformer(it)) },
        )
    }
}

@ThemePreviews
@Composable
private fun Preview() {
    val options = listOf(1, 2)
    val entry = ChoicesDataEntry(
        choices = options,
        optionTransformer = { ChoicesFormFieldItem.Option(id = it.toString(), label = StringResource("Option: $it")) },
        valueTransformer = { it },
    )

    GroveTheme {
        entry.Content(value = "1", onValueChange = {}, modifier = Modifier)
    }
}
