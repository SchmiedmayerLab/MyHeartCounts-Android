//
// This source file is part of the My Heart Counts Android open-source project
//
// SPDX-FileCopyrightText: 2026 Stanford University and the project authors (see CONTRIBUTORS.md)
//
// SPDX-License-Identifier: MIT

package edu.stanford.myheartcounts.demographics.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.PreviewParameter
import androidx.compose.ui.tooling.preview.PreviewParameterProvider
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import edu.stanford.myheartcounts.ui.choice.ChoiceFollowUpRow
import edu.stanford.myheartcounts.ui.choice.ChoiceRow
import edu.stanford.myheartcounts.ui.choice.ChoiceSection
import edu.stanford.myheartcounts.ui.choice.MonthYearPicker
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
 * A full-height bottom sheet answering a question by choosing among options, beneath [appBar]. The
 * options are grouped into [sections], each shown as its own card, below an optional [prompt].
 * Chosen options are marked with a check.
 *
 * @param appBar the sheet's app bar
 * @param prompt the question as asked, shown above the options
 * @param sections the option groups, in display order
 * @param monthYearPicker the month and year picker currently open above the sheet
 */
data class DemographicsChoiceSheet(
    val appBar: GroveAppBar,
    val prompt: StringResource?,
    val sections: StateFlow<List<ChoiceSection>>,
    val monthYearPicker: StateFlow<MonthYearPicker?>,
) : BottomSheetComposableContent {

    override val skipPartiallyExpanded: Boolean = true

    @Composable
    override fun Content(modifier: Modifier) {
        val sections by sections.collectAsStateWithLifecycle()
        val monthYearPicker by monthYearPicker.collectAsStateWithLifecycle()

        StaticGroveScaffold(appBar = appBar) {
            LazyColumn(
                modifier = modifier
                    .fillMaxSize()
                    .background(Colors.surfaceContainer),
                contentPadding = PaddingValues(Spacings.medium),
                verticalArrangement = Arrangement.spacedBy(Spacings.large),
            ) {
                prompt?.let {
                    item {
                        GroveCard(
                            modifier = Modifier.fillMaxWidth(),
                            shape = GroveShapes.large,
                        ) {
                            Text(
                                modifier = Modifier.padding(Spacings.medium),
                                text = it.text(),
                                style = TextStyles.bodyLarge,
                                color = Colors.onSurface,
                            )
                        }
                    }
                }
                items(sections) { section ->
                    section.Content(modifier = Modifier.fillMaxWidth())
                }
            }
        }
        monthYearPicker?.Content()
    }
}

private class DemographicsChoiceSheetPreviewProvider : PreviewParameterProvider<DemographicsChoiceSheet> {
    private val option = ChoiceRow(
        icon = null,
        title = StringResource("Diabetes"),
        subtitle = null,
        detail = null,
        isSelected = false,
        followUp = null,
        onClick = {},
    )
    private val base = DemographicsChoiceSheet(
        appBar = groveAppBar {
            title(StringResource("Comorbidities"))
            close {}
        },
        prompt = null,
        sections = MutableStateFlow(
            listOf(
                ChoiceSection(
                    header = null,
                    options = listOf(
                        option.copy(
                            isSelected = true,
                            followUp = ChoiceFollowUpRow(
                                label = StringResource("Since"),
                                value = StringResource("Mar 2020"),
                                isSet = true,
                                onClick = {},
                            ),
                        ),
                        option.copy(
                            title = StringResource("Heart Failure"),
                        ),
                    ),
                    footer = null,
                ),
                ChoiceSection(
                    header = StringResource("Other Conditions"),
                    options = listOf(
                        option.copy(
                            title = StringResource("Chronic Kidney Disease"),
                            subtitle = StringResource("Stage 3 or worse"),
                        ),
                    ),
                    footer = StringResource("Choose every condition that applies."),
                ),
            ),
        ),
        monthYearPicker = MutableStateFlow(null),
    )
    override val values = sequenceOf(
        base,
        base.copy(
            prompt = StringResource("Which statement describes you best?"),
            sections = MutableStateFlow(
                listOf(
                    ChoiceSection(
                        header = null,
                        options = listOf(
                            option.copy(
                                title = StringResource("A"),
                                subtitle = StringResource("I am not active and do not intend to start."),
                                isSelected = true,
                            ),
                        ),
                        footer = null,
                    ),
                    ChoiceSection(
                        header = null,
                        options = listOf(
                            option.copy(
                                title = StringResource("B"),
                                subtitle = StringResource("I am active and intend to stay so."),
                            ),
                        ),
                        footer = null,
                    ),
                ),
            ),
        ),
    )
}

@ThemePreviews
@Composable
private fun Preview(@PreviewParameter(DemographicsChoiceSheetPreviewProvider::class) content: DemographicsChoiceSheet) {
    GroveTheme { content.Content() }
}
