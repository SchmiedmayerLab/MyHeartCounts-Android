//
// This source file is part of the My Heart Counts Android open-source project
//
// SPDX-FileCopyrightText: 2026 Stanford University and the project authors (see CONTRIBUTORS.md)
//
// SPDX-License-Identifier: MIT

package edu.stanford.myheartcounts.dashboard.ui.chart

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.UnfoldMore
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.selected
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.stateDescription
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.PreviewParameter
import androidx.compose.ui.tooling.preview.PreviewParameterProvider
import org.grovealliance.ui.ComposableContent
import org.grovealliance.ui.StringResource
import org.grovealliance.ui.theme.Colors
import org.grovealliance.ui.theme.GroveTheme
import org.grovealliance.ui.theme.Sizes
import org.grovealliance.ui.theme.Spacings
import org.grovealliance.ui.theme.TextStyles
import org.grovealliance.ui.theme.ThemePreviews

/**
 * A selected option that opens a dropdown menu, with a check beside the current option.
 *
 * @param label the screen reader label describing the choice
 * @param options the available options, in display order
 * @param selectedIndex the index of the selected option; must be within [options]
 * @param onSelected invoked with the index of the option chosen from the menu
 */
@Immutable
data class MenuPicker(
    val label: StringResource,
    val options: List<StringResource>,
    val selectedIndex: Int,
    val onSelected: (Int) -> Unit,
) : ComposableContent {

    init {
        require(selectedIndex in options.indices) { "selectedIndex must be within options" }
    }

    @Composable
    override fun Content(modifier: Modifier) {
        var isExpanded by remember(options, selectedIndex) { mutableStateOf(false) }
        val labelText = label.text()
        val selectedText = options[selectedIndex].text()
        Box(modifier = modifier) {
            Row(
                modifier = Modifier
                    .clickable(role = Role.DropdownList) { isExpanded = true }
                    .semantics(mergeDescendants = true) {
                        contentDescription = labelText
                        stateDescription = selectedText
                    }
                    .padding(vertical = Spacings.small),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    text = selectedText,
                    style = TextStyles.bodyMedium,
                    color = Colors.onSurfaceVariant,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.weight(weight = 1f, fill = false),
                )
                Icon(
                    imageVector = Icons.Default.UnfoldMore,
                    contentDescription = null,
                    tint = Colors.onSurfaceVariant,
                    modifier = Modifier.size(Sizes.Icon.small),
                )
            }
            DropdownMenu(
                expanded = isExpanded,
                onDismissRequest = { isExpanded = false },
            ) {
                options.forEachIndexed { index, option ->
                    DropdownMenuItem(
                        text = { Text(text = option.text(), style = TextStyles.bodyMedium) },
                        trailingIcon = {
                            if (index == selectedIndex) {
                                Icon(
                                    imageVector = Icons.Default.Check,
                                    contentDescription = null,
                                    modifier = Modifier.size(Sizes.Icon.small),
                                )
                            }
                        },
                        onClick = {
                            isExpanded = false
                            onSelected(index)
                        },
                        modifier = Modifier.semantics { selected = index == selectedIndex },
                    )
                }
            }
        }
    }
}

private class MenuPickerPreviewProvider : PreviewParameterProvider<MenuPicker> {
    private val base = MenuPicker(
        label = StringResource("Time range"),
        options = listOf(StringResource("Last 7 days"), StringResource("Last 14 days"), StringResource("Last month")),
        selectedIndex = 1,
        onSelected = {},
    )

    override val values: Sequence<MenuPicker> = sequenceOf(base, base.copy(selectedIndex = 0))
}

@ThemePreviews
@Composable
private fun Preview(@PreviewParameter(MenuPickerPreviewProvider::class) content: MenuPicker) {
    GroveTheme {
        content.Content(modifier = Modifier.padding(Spacings.medium))
    }
}
