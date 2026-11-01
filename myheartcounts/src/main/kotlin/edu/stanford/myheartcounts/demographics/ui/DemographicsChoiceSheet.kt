//
// This source file is part of the My Heart Counts Android open-source project
//
// SPDX-FileCopyrightText: 2026 Stanford University and the project authors (see CONTRIBUTORS.md)
//
// SPDX-License-Identifier: MIT

package edu.stanford.myheartcounts.demographics.ui

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.UnfoldMore
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.PreviewParameter
import androidx.compose.ui.tooling.preview.PreviewParameterProvider
import androidx.lifecycle.compose.collectAsStateWithLifecycle
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
import org.grovealliance.ui.theme.Sizes
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
 * @param onSelect called with the [Option.index] of a tapped option
 */
data class DemographicsChoiceSheet(
    val appBar: GroveAppBar,
    val prompt: StringResource?,
    val sections: StateFlow<List<Section>>,
    val monthYearPicker: StateFlow<DemographicsMonthYearPicker?>,
    val onSelect: (Int) -> Unit,
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
                    SectionContent(section = section)
                }
            }
        }
        monthYearPicker?.Content()
    }

    @Composable
    private fun SectionContent(section: Section) {
        Column(verticalArrangement = Arrangement.spacedBy(Spacings.small)) {
            section.header?.let {
                Text(
                    modifier = Modifier.padding(horizontal = Spacings.medium),
                    text = it.text(),
                    style = TextStyles.titleSmall,
                    color = Colors.onSurfaceVariant,
                )
            }
            GroveCard(
                modifier = Modifier.fillMaxWidth(),
                shape = GroveShapes.large,
            ) {
                section.options.forEachIndexed { position, option ->
                    if (position > 0) {
                        HorizontalDivider(modifier = Modifier.padding(horizontal = Spacings.medium))
                    }
                    OptionRow(option = option)
                    option.followUp?.let { followUp ->
                        AnimatedVisibility(visible = option.isSelected) {
                            FollowUpRow(followUp = followUp)
                        }
                    }
                }
            }
            section.footer?.let {
                Text(
                    modifier = Modifier.padding(horizontal = Spacings.medium),
                    text = it.text(),
                    style = TextStyles.bodySmall,
                    color = Colors.onSurfaceVariant,
                )
            }
        }
    }

    @Composable
    private fun OptionRow(option: Option) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clickable { onSelect(option.index) }
                .padding(Spacings.medium),
            horizontalArrangement = Arrangement.spacedBy(Spacings.small),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(Spacings.extraSmall),
            ) {
                Text(
                    text = option.title.text(),
                    style = TextStyles.bodyLarge,
                    color = Colors.onSurface,
                )
                option.subtitle?.let {
                    Text(
                        text = it.text(),
                        style = TextStyles.bodyMedium,
                        color = Colors.onSurfaceVariant,
                    )
                }
            }
            option.detail?.let {
                Text(
                    text = it.text(),
                    style = TextStyles.bodyLarge,
                    color = Colors.onSurfaceVariant,
                )
            }
            if (option.isSelected) {
                Icon(
                    imageVector = Icons.Default.Check,
                    modifier = Modifier.size(Sizes.Icon.small),
                    tint = Colors.primary,
                    contentDescription = null,
                )
            }
        }
    }

    @Composable
    private fun FollowUpRow(followUp: FollowUp) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clickable(onClick = followUp.onClick)
                .padding(Spacings.medium)
                .heightIn(min = Sizes.Icon.small),
            horizontalArrangement = Arrangement.spacedBy(Spacings.small),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                modifier = Modifier.weight(1f),
                text = followUp.label.text(),
                style = TextStyles.bodyMedium,
                color = Colors.onSurfaceVariant,
            )
            Text(
                text = followUp.value.text(),
                style = TextStyles.bodyMedium,
                color = if (followUp.isSet) Colors.onSurface else Colors.onSurfaceVariant,
            )
            Icon(
                imageVector = Icons.Default.UnfoldMore,
                modifier = Modifier.size(Sizes.Icon.extraSmall),
                tint = Colors.primary,
                contentDescription = null,
            )
        }
    }

    /**
     * A group of [options] shown as one card, with an optional [header] above and [footer] below.
     */
    data class Section(
        val header: StringResource?,
        val options: List<Option>,
        val footer: StringResource?,
    )

    /**
     * One option of a [DemographicsChoiceSheet].
     *
     * @param index identifies the option to [DemographicsChoiceSheet.onSelect]
     * @param title the option
     * @param subtitle further explanation below the title
     * @param detail a short trailing value, such as an abbreviation
     * @param isSelected whether the option is chosen
     * @param followUp a further detail of the option, shown below it while it is chosen
     */
    data class Option(
        val index: Int,
        val title: StringResource,
        val subtitle: StringResource?,
        val detail: StringResource?,
        val isSelected: Boolean,
        val followUp: FollowUp?,
    )

    /**
     * A further detail of a chosen [Option], such as since when it applies.
     *
     * @param label names the detail
     * @param value the detail, or what stands in for an unknown one
     * @param isSet whether the detail is known
     * @param onClick called when the detail is tapped to change it
     */
    data class FollowUp(
        val label: StringResource,
        val value: StringResource,
        val isSet: Boolean,
        val onClick: () -> Unit,
    )
}

private class DemographicsChoiceSheetPreviewProvider : PreviewParameterProvider<DemographicsChoiceSheet> {
    private val option = DemographicsChoiceSheet.Option(
        index = 0,
        title = StringResource("Diabetes"),
        subtitle = null,
        detail = null,
        isSelected = false,
        followUp = null,
    )
    private val base = DemographicsChoiceSheet(
        appBar = groveAppBar {
            title(StringResource("Comorbidities"))
            close {}
        },
        prompt = null,
        sections = MutableStateFlow(
            listOf(
                DemographicsChoiceSheet.Section(
                    header = null,
                    options = listOf(
                        option.copy(
                            isSelected = true,
                            followUp = DemographicsChoiceSheet.FollowUp(
                                label = StringResource("Since"),
                                value = StringResource("Mar 2020"),
                                isSet = true,
                                onClick = {},
                            ),
                        ),
                        option.copy(
                            index = 1,
                            title = StringResource("Heart Failure"),
                        ),
                    ),
                    footer = null,
                ),
                DemographicsChoiceSheet.Section(
                    header = StringResource("Other Conditions"),
                    options = listOf(
                        option.copy(
                            index = 2,
                            title = StringResource("Chronic Kidney Disease"),
                            subtitle = StringResource("Stage 3 or worse"),
                        ),
                    ),
                    footer = StringResource("Choose every condition that applies."),
                ),
            ),
        ),
        monthYearPicker = MutableStateFlow(null),
        onSelect = {},
    )
    override val values = sequenceOf(
        base,
        base.copy(
            prompt = StringResource("Which statement describes you best?"),
            sections = MutableStateFlow(
                listOf(
                    DemographicsChoiceSheet.Section(
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
                    DemographicsChoiceSheet.Section(
                        header = null,
                        options = listOf(
                            option.copy(
                                index = 1,
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
