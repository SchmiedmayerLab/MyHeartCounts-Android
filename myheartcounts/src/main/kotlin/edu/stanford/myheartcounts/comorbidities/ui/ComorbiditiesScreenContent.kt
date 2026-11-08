//
// This source file is part of the My Heart Counts Android open-source project
//
// SPDX-FileCopyrightText: 2026 Stanford University and the project authors (see CONTRIBUTORS.md)
//
// SPDX-License-Identifier: MIT

package edu.stanford.myheartcounts.comorbidities.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.HorizontalDivider
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.PreviewParameter
import androidx.compose.ui.tooling.preview.PreviewParameterProvider
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import edu.stanford.myheartcounts.ui.choice.ChoiceFollowUpRow
import edu.stanford.myheartcounts.ui.choice.ChoiceRow
import edu.stanford.myheartcounts.ui.choice.MonthYearPicker
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import org.grovealliance.ui.ComposableContent
import org.grovealliance.ui.GroveAppBar
import org.grovealliance.ui.GroveCard
import org.grovealliance.ui.StaticGroveScaffold
import org.grovealliance.ui.StringResource
import org.grovealliance.ui.groveAppBar
import org.grovealliance.ui.theme.Colors
import org.grovealliance.ui.theme.GroveShapes
import org.grovealliance.ui.theme.GroveTheme
import org.grovealliance.ui.theme.Spacings
import org.grovealliance.ui.theme.ThemePreviews

/**
 * The participant's comorbidities beneath [appBar]: the conditions grouped into [sections], each
 * shown as its own card, with the chosen ones checked and offering their date of diagnosis.
 *
 * @param appBar the screen's app bar
 * @param sections the condition groups, in display order
 * @param diagnosisDatePicker the date of diagnosis picker currently open above the screen
 */
data class ComorbiditiesScreenContent(
    val appBar: GroveAppBar,
    val sections: StateFlow<List<Section>>,
    val diagnosisDatePicker: StateFlow<MonthYearPicker?>,
) : ComposableContent {

    @Composable
    override fun Content(modifier: Modifier) {
        val sections by sections.collectAsStateWithLifecycle()
        val diagnosisDatePicker by diagnosisDatePicker.collectAsStateWithLifecycle()

        StaticGroveScaffold(appBar = appBar) {
            LazyColumn(
                modifier = modifier
                    .fillMaxSize()
                    .background(Colors.surfaceContainer),
                contentPadding = PaddingValues(Spacings.medium),
                verticalArrangement = Arrangement.spacedBy(Spacings.large),
            ) {
                items(sections) { section ->
                    SectionContent(section = section)
                }
            }
        }
        diagnosisDatePicker?.Content()
    }

    @Composable
    private fun SectionContent(section: Section) {
        GroveCard(
            modifier = Modifier.fillMaxWidth(),
            shape = GroveShapes.large,
        ) {
            section.conditions.forEachIndexed { position, condition ->
                if (position > 0) {
                    HorizontalDivider(modifier = Modifier.padding(horizontal = Spacings.medium))
                }
                condition.Content()
            }
        }
    }

    /**
     * A group of [conditions] shown as one card.
     */
    data class Section(
        val conditions: List<ChoiceRow>,
    )
}

private class ComorbiditiesScreenContentPreviewProvider : PreviewParameterProvider<ComorbiditiesScreenContent> {
    private val condition = ChoiceRow(
        icon = null,
        title = StringResource("Diabetes"),
        subtitle = null,
        detail = null,
        isSelected = false,
        followUp = null,
        onClick = {},
    )
    private val base = ComorbiditiesScreenContent(
        appBar = groveAppBar {
            back {}
            title(StringResource("Select Comorbidities"))
        },
        sections = MutableStateFlow(
            listOf(
                ComorbiditiesScreenContent.Section(
                    conditions = listOf(
                        condition.copy(
                            isSelected = true,
                            followUp = ChoiceFollowUpRow(
                                label = StringResource("Since"),
                                value = StringResource("Mar 2020"),
                                isSet = true,
                                onClick = {},
                            ),
                        ),
                        condition.copy(title = StringResource("Heart Failure")),
                    ),
                ),
                ComorbiditiesScreenContent.Section(
                    conditions = listOf(condition.copy(title = StringResource("Kidney Disease"))),
                ),
            ),
        ),
        diagnosisDatePicker = MutableStateFlow(null),
    )

    override val values = sequenceOf(
        base,
        base.copy(
            sections = MutableStateFlow(
                listOf(
                    ComorbiditiesScreenContent.Section(
                        conditions = listOf(condition.copy(title = StringResource("None"), isSelected = true)),
                    ),
                ),
            ),
        ),
    )
}

@ThemePreviews
@Composable
private fun Preview(
    @PreviewParameter(ComorbiditiesScreenContentPreviewProvider::class) content: ComorbiditiesScreenContent,
) {
    GroveTheme { content.Content() }
}
