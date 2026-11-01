//
// This source file is part of the My Heart Counts Android open-source project
//
// SPDX-FileCopyrightText: 2026 Stanford University and the project authors (see CONTRIBUTORS.md)
//
// SPDX-License-Identifier: MIT

package edu.stanford.myheartcounts.demographics.ui

import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.PreviewParameter
import androidx.compose.ui.tooling.preview.PreviewParameterProvider
import edu.stanford.myheartcounts.demographics.DemographicsField
import org.grovealliance.ui.ComposableContent
import org.grovealliance.ui.StringResource
import org.grovealliance.ui.theme.Colors
import org.grovealliance.ui.theme.GroveTheme
import org.grovealliance.ui.theme.Sizes
import org.grovealliance.ui.theme.ThemePreviews

/**
 * A menu of answers anchored to the question for [field], open for as long as it is shown. The
 * selected option is marked with a check.
 *
 * @param field the question the menu answers
 * @param options the answers, in display order
 * @param onSelect called with the [Option.index] of the chosen answer
 * @param onDismiss called when the menu is dismissed without choosing
 */
data class DemographicsMenu(
    val field: DemographicsField,
    val options: List<Option>,
    val onSelect: (Int) -> Unit,
    val onDismiss: () -> Unit,
) : ComposableContent {

    @Composable
    override fun Content(modifier: Modifier) {
        DropdownMenu(
            modifier = modifier,
            expanded = true,
            onDismissRequest = onDismiss,
        ) {
            options.forEach { option ->
                DropdownMenuItem(
                    text = { Text(text = option.label.text()) },
                    leadingIcon = {
                        if (option.isSelected) {
                            Icon(
                                imageVector = Icons.Default.Check,
                                modifier = Modifier.size(Sizes.Icon.small),
                                tint = Colors.onSurface,
                                contentDescription = null,
                            )
                        } else {
                            Spacer(modifier = Modifier.size(Sizes.Icon.small))
                        }
                    },
                    onClick = { onSelect(option.index) },
                )
                if (option.isFollowedByDivider) {
                    HorizontalDivider()
                }
            }
        }
    }

    /**
     * One answer in a [DemographicsMenu].
     *
     * @param index identifies the answer to [DemographicsMenu.onSelect]
     * @param label the answer
     * @param isSelected whether it is the current answer
     * @param isFollowedByDivider whether a divider separates it from the next answer
     */
    data class Option(
        val index: Int,
        val label: StringResource,
        val isSelected: Boolean,
        val isFollowedByDivider: Boolean,
    )
}

private class DemographicsMenuPreviewProvider : PreviewParameterProvider<DemographicsMenu> {
    private val option = DemographicsMenu.Option(
        index = 0,
        label = StringResource("Female"),
        isSelected = true,
        isFollowedByDivider = false,
    )
    private val base = DemographicsMenu(
        field = DemographicsField.GenderIdentity,
        options = listOf(
            option,
            option.copy(
                index = 1,
                label = StringResource("Male"),
                isSelected = false,
                isFollowedByDivider = true,
            ),
            option.copy(
                index = 2,
                label = StringResource("Prefer not to state"),
                isSelected = false,
            ),
        ),
        onSelect = {},
        onDismiss = {},
    )
    override val values = sequenceOf(base)
}

@ThemePreviews
@Composable
private fun Preview(@PreviewParameter(DemographicsMenuPreviewProvider::class) content: DemographicsMenu) {
    GroveTheme { content.Content() }
}
