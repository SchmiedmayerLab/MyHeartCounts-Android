//
// This source file is part of the My Heart Counts Android open-source project
//
// SPDX-FileCopyrightText: 2026 Stanford University and the project authors (see CONTRIBUTORS.md)
//
// SPDX-License-Identifier: MIT

package edu.stanford.myheartcounts.demographics.ui

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.UnfoldMore
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
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
import java.time.Month
import java.time.format.TextStyle
import java.util.Locale

/**
 * A dialog for choosing a year and, optionally, a month within it, open for as long as it is shown.
 * It opens on [year] and [month], or on the first of [years] without a month.
 *
 * @param title the dialog title
 * @param years the years that can be chosen, in display order
 * @param months the twelve month names, January first
 * @param year the year chosen so far, if any
 * @param month the month chosen so far, from 1 to 12, if any
 * @param yearLabel the label of the year field
 * @param monthLabel the label of the month field
 * @param noMonth stands in for an unchosen month
 * @param confirmTitle the title of the button choosing the selection
 * @param clearTitle the title of the button clearing the date
 * @param dismissTitle the title of the button closing the dialog unchanged
 * @param onSelect called with the chosen year and month, if any
 * @param onClear called when the participant clears the date
 * @param onDismiss called when the dialog closes unchanged
 */
data class DemographicsMonthYearPicker(
    val title: StringResource,
    val years: List<Int>,
    val months: List<StringResource>,
    val year: Int?,
    val month: Int?,
    val yearLabel: StringResource,
    val monthLabel: StringResource,
    val noMonth: StringResource,
    val confirmTitle: StringResource,
    val clearTitle: StringResource,
    val dismissTitle: StringResource,
    val onSelect: (Int, Int?) -> Unit,
    val onClear: () -> Unit,
    val onDismiss: () -> Unit,
) : ComposableContent {

    @Composable
    override fun Content(modifier: Modifier) {
        var selectedYear by rememberSaveable { mutableStateOf(year ?: years.first()) }
        var selectedMonth by rememberSaveable { mutableStateOf(month) }
        val noMonthText = noMonth.text()
        val monthTexts = months.map { it.text() }

        AlertDialog(
            modifier = modifier,
            onDismissRequest = onDismiss,
            title = { Text(text = title.text()) },
            text = {
                Column {
                    Field(
                        label = yearLabel.text(),
                        value = selectedYear.toString(),
                        choices = years.map { it.toString() },
                        onChoose = { selectedYear = years[it] },
                    )
                    HorizontalDivider()
                    Field(
                        label = monthLabel.text(),
                        value = selectedMonth?.let { monthTexts[it - 1] } ?: noMonthText,
                        choices = listOf(noMonthText) + monthTexts,
                        onChoose = { selectedMonth = it.takeIf { index -> index > 0 } },
                    )
                }
            },
            confirmButton = {
                TextButton(onClick = { onSelect(selectedYear, selectedMonth) }) {
                    Text(text = confirmTitle.text())
                }
            },
            dismissButton = {
                Row {
                    TextButton(onClick = onClear) {
                        Text(text = clearTitle.text())
                    }
                    TextButton(onClick = onDismiss) {
                        Text(text = dismissTitle.text())
                    }
                }
            },
        )
    }

    @Composable
    private fun Field(
        label: String,
        value: String,
        choices: List<String>,
        onChoose: (Int) -> Unit,
    ) {
        var isExpanded by rememberSaveable { mutableStateOf(false) }

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .heightIn(min = Sizes.Content.large)
                .clickable { isExpanded = true }
                .padding(vertical = Spacings.small),
            horizontalArrangement = Arrangement.spacedBy(Spacings.small),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                modifier = Modifier.weight(1f),
                text = label,
                style = TextStyles.bodyLarge,
                color = Colors.onSurface,
            )
            Box {
                Text(
                    text = value,
                    style = TextStyles.bodyLarge,
                    color = Colors.onSurfaceVariant,
                )
                DropdownMenu(
                    expanded = isExpanded,
                    onDismissRequest = { isExpanded = false },
                ) {
                    choices.forEachIndexed { index, choice ->
                        DropdownMenuItem(
                            text = { Text(text = choice) },
                            onClick = {
                                onChoose(index)
                                isExpanded = false
                            },
                        )
                    }
                }
            }
            Icon(
                imageVector = Icons.Default.UnfoldMore,
                modifier = Modifier.size(Sizes.Icon.extraSmall),
                tint = Colors.primary,
                contentDescription = null,
            )
        }
    }
}

private class DemographicsMonthYearPickerPreviewProvider : PreviewParameterProvider<DemographicsMonthYearPicker> {
    private val base = DemographicsMonthYearPicker(
        title = StringResource("Diabetes"),
        years = (2026 downTo 2016).toList(),
        months = Month.entries.map { StringResource(it.getDisplayName(TextStyle.FULL, Locale.US)) },
        year = 2020,
        month = 3,
        yearLabel = StringResource("Year"),
        monthLabel = StringResource("Month"),
        noMonth = StringResource("—"),
        confirmTitle = StringResource("OK"),
        clearTitle = StringResource("Unknown"),
        dismissTitle = StringResource("Cancel"),
        onSelect = { _, _ -> },
        onClear = {},
        onDismiss = {},
    )
    override val values = sequenceOf(
        base,
        base.copy(
            year = null,
            month = null,
        ),
    )
}

@ThemePreviews
@Composable
private fun Preview(
    @PreviewParameter(DemographicsMonthYearPickerPreviewProvider::class) content: DemographicsMonthYearPicker,
) {
    GroveTheme { content.Content() }
}
