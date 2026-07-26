//
// This source file is part of the My Heart Counts open-source project
//
// SPDX-FileCopyrightText: 2026 Stanford University and the project authors (see CONTRIBUTORS.md)
//
// SPDX-License-Identifier: MIT

package edu.stanford.spezi.consent.internal

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.LocalTextStyle
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import edu.stanford.spezi.markdown.rememberMarkdownAnnotatedString
import edu.stanford.spezi.resources.Strings
import edu.stanford.spezi.ui.ComposableContent
import edu.stanford.spezi.ui.SpeziCard
import edu.stanford.spezi.ui.theme.Colors
import edu.stanford.spezi.ui.theme.Spacings
import edu.stanford.spezi.ui.theme.SpeziTheme
import edu.stanford.spezi.ui.theme.TextStyles
import edu.stanford.spezi.ui.theme.ThemePreviews
import edu.stanford.spezi.ui.theme.medium
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.flowOf

/**
 * A consent form section presenting a labelled set of radio options.
 *
 * Completion is governed by [expectedSelection]; the continue button is only enabled once that
 * constraint is satisfied.
 */
internal data class ConsentSelectSection(
    val id: String,
    val text: String,
    val footnote: String?,
    val options: List<SelectionOption>,
    val initialValue: String,
    val expectedSelection: ExpectedSelection = ExpectedSelection.Anything(allowEmptySelection = true),
    val selectedId: Flow<String?>,
    val onOptionSelected: (String) -> Unit,
) : ComposableContent {

    companion object {
        const val EMPTY_SELECTION = ""
    }

    @Composable
    override fun Content(modifier: Modifier) {
        val selectedId by selectedId.collectAsStateWithLifecycle(null)
        val footnoteText = footnote?.let { rememberMarkdownAnnotatedString(it) }
        SpeziCard(modifier = modifier) {
            Column(modifier = Modifier.fillMaxWidth()) {
                Text(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = Spacings.medium)
                        .padding(
                            top = Spacings.medium,
                            bottom = if (footnoteText != null) Spacings.extraSmall else Spacings.small,
                        ),
                    text = text,
                    style = LocalTextStyle.current.medium(),
                )
                footnoteText?.let {
                    Text(
                        modifier = Modifier
                            .padding(horizontal = Spacings.medium)
                            .padding(bottom = Spacings.small),
                        text = it,
                        style = TextStyles.bodySmall,
                        color = Colors.onSurfaceVariant,
                    )
                }
                HorizontalDivider(modifier = Modifier.padding(horizontal = Spacings.medium))
                val isEmpty = selectedId == null || selectedId == EMPTY_SELECTION
                val requiresSelection = when (val exp = expectedSelection) {
                    is ExpectedSelection.Anything -> !exp.allowEmptySelection
                    is ExpectedSelection.Option -> true
                }
                SelectOptionRow(
                    label = stringResource(Strings.consent_no_selection),
                    selected = isEmpty,
                    labelColor = if (isEmpty && requiresSelection) Colors.error else Colors.onSurfaceVariant,
                    onClick = { onOptionSelected(EMPTY_SELECTION) },
                )
                options.forEach { option ->
                    HorizontalDivider(modifier = Modifier.padding(horizontal = Spacings.medium))
                    SelectOptionRow(
                        label = option.title,
                        selected = selectedId == option.id,
                        onClick = { onOptionSelected(option.id) },
                    )
                }
            }
        }
    }
}

@Composable
private fun SelectOptionRow(
    label: String,
    selected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    labelColor: Color = Colors.onSurface,
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(Spacings.medium),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(Spacings.medium),
    ) {
        RadioButton(selected = selected, onClick = null)
        Text(
            text = label,
            color = labelColor,
            style = TextStyles.bodyMedium,
        )
    }
}

@ThemePreviews
@Composable
private fun ConsentSelectContentSelectedPreview() {
    SpeziTheme {
        ConsentSelectSection(
            id = "trial",
            text = "Would you like to join the short term physical activity promoting trial?",
            footnote = "You can select **yes** or **no**",
            options = listOf(SelectionOption("trial-yes", "Yes"), SelectionOption("trial-no", "No")),
            initialValue = "",
            expectedSelection = ExpectedSelection.Anything(allowEmptySelection = false),
            selectedId = MutableStateFlow("trial-yes"),
            onOptionSelected = {},
        ).Content(modifier = Modifier.fillMaxWidth())
    }
}

@ThemePreviews
@Composable
private fun ConsentSelectContentEmptyPreview() {
    SpeziTheme {
        ConsentSelectSection(
            id = "trial",
            text = "Would you like to join the short term physical activity promoting trial?",
            footnote = null,
            options = listOf(SelectionOption("trial-yes", "Yes"), SelectionOption("trial-no", "No")),
            initialValue = "",
            expectedSelection = ExpectedSelection.Anything(allowEmptySelection = false),
            selectedId = flowOf(null),
            onOptionSelected = {},
        ).Content(modifier = Modifier.fillMaxWidth())
    }
}
