//
// This source file is part of the My Heart Counts Android open-source project
//
// SPDX-FileCopyrightText: 2026 Stanford University and the project authors (see CONTRIBUTORS.md)
//
// SPDX-License-Identifier: MIT

package edu.stanford.myheartcounts.demographics.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.PreviewParameter
import androidx.compose.ui.tooling.preview.PreviewParameterProvider
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import edu.stanford.myheartcounts.ui.TextRow
import edu.stanford.myheartcounts.ui.choice.ChoiceFollowUpRow
import edu.stanford.myheartcounts.ui.choice.ChoiceRow
import edu.stanford.myheartcounts.ui.choice.MonthYearPicker
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import org.grovealliance.ui.BottomSheetComposableContent
import org.grovealliance.ui.GroveAppBar
import org.grovealliance.ui.GroveSectionList
import org.grovealliance.ui.StaticGroveScaffold
import org.grovealliance.ui.StringResource
import org.grovealliance.ui.groveAppBar
import org.grovealliance.ui.groveSectionList
import org.grovealliance.ui.theme.Colors
import org.grovealliance.ui.theme.GroveTheme
import org.grovealliance.ui.theme.ThemePreviews

/**
 * A full-height bottom sheet answering a question by choosing among options, beneath [appBar]. The
 * question as asked, if shown, and the option groups make up its [sections], each group shown as its
 * own card. Chosen options are marked with a check.
 *
 * @param appBar the sheet's app bar
 * @param sections the question as asked and the option groups, in display order
 * @param monthYearPicker the month and year picker currently open above the sheet
 */
data class DemographicsChoiceSheet(
    val appBar: GroveAppBar,
    val sections: StateFlow<GroveSectionList>,
    val monthYearPicker: StateFlow<MonthYearPicker?>,
) : BottomSheetComposableContent {

    override val skipPartiallyExpanded: Boolean = true

    @Composable
    override fun Content(modifier: Modifier) {
        val sections by sections.collectAsStateWithLifecycle()
        val monthYearPicker by monthYearPicker.collectAsStateWithLifecycle()

        StaticGroveScaffold(appBar = appBar) {
            sections.Content(
                modifier = modifier
                    .fillMaxSize()
                    .background(Colors.surfaceContainer),
            )
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
        sections = MutableStateFlow(
            groveSectionList {
                section(key = "primary") {
                    card(key = "options") {
                        row(
                            key = "diabetes",
                            content = option.copy(
                                isSelected = true,
                                followUp = ChoiceFollowUpRow(
                                    label = StringResource("Since"),
                                    value = StringResource("Mar 2020"),
                                    isSet = true,
                                    onClick = {},
                                ),
                            ),
                        )
                        row(
                            key = "heart-failure",
                            content = option.copy(title = StringResource("Heart Failure")),
                        )
                    }
                }
                section(key = "other") {
                    title("Other Conditions")
                    card(key = "options") {
                        row(
                            key = "kidney-disease",
                            content = option.copy(
                                title = StringResource("Chronic Kidney Disease"),
                                subtitle = StringResource("Stage 3 or worse"),
                            ),
                        )
                    }
                    footer("Choose every condition that applies.")
                }
            },
        ),
        monthYearPicker = MutableStateFlow(null),
    )
    override val values = sequenceOf(
        base,
        base.copy(
            sections = MutableStateFlow(
                groveSectionList {
                    section(key = "prompt") {
                        card(key = "prompt") {
                            row(
                                key = "prompt",
                                content = TextRow(text = StringResource("Which statement describes you best?")),
                            )
                        }
                    }
                    section(key = "a") {
                        card(key = "options") {
                            row(
                                key = "a",
                                content = option.copy(
                                    title = StringResource("A"),
                                    subtitle = StringResource("I am not active and do not intend to start."),
                                    isSelected = true,
                                ),
                            )
                        }
                    }
                    section(key = "b") {
                        card(key = "options") {
                            row(
                                key = "b",
                                content = option.copy(
                                    title = StringResource("B"),
                                    subtitle = StringResource("I am active and intend to stay so."),
                                ),
                            )
                        }
                    }
                },
            ),
        ),
    )
}

@ThemePreviews
@Composable
private fun Preview(@PreviewParameter(DemographicsChoiceSheetPreviewProvider::class) content: DemographicsChoiceSheet) {
    GroveTheme { content.Content() }
}
