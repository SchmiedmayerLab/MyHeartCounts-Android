//
// This source file is part of the My Heart Counts Android open-source project
//
// SPDX-FileCopyrightText: 2026 Stanford University and the project authors (see CONTRIBUTORS.md)
//
// SPDX-License-Identifier: MIT

package edu.stanford.myheartcounts.demographics.ui

import edu.stanford.myheartcounts.MHCStrings
import edu.stanford.myheartcounts.ui.choice.ChoiceFollowUpRow
import edu.stanford.myheartcounts.ui.choice.ChoiceRow
import edu.stanford.myheartcounts.ui.choice.ChoiceSection
import kotlinx.coroutines.flow.MutableStateFlow
import org.grovealliance.testing.screenshot.ScreenshotTest
import org.grovealliance.ui.StringResource
import org.grovealliance.ui.groveAppBar
import org.junit.Test

class DemographicsChoiceSheetScreenshotTest : ScreenshotTest() {
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
            title(MHCStrings.comorbidities_title)
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
                                label = StringResource(MHCStrings.comorbidities_since),
                                value = StringResource("Mar 2020"),
                                isSet = true,
                                onClick = {},
                            ),
                        ),
                        option.copy(
                            title = StringResource("Heart Failure"),
                            isSelected = true,
                            followUp = ChoiceFollowUpRow(
                                label = StringResource(MHCStrings.comorbidities_since),
                                value = StringResource(MHCStrings.comorbidities_since_unknown),
                                isSet = false,
                                onClick = {},
                            ),
                        ),
                        option.copy(
                            title = StringResource("Coronary Artery Disease"),
                        ),
                    ),
                    footer = null,
                ),
                ChoiceSection(
                    header = null,
                    options = listOf(
                        option.copy(
                            title = StringResource("Stroke"),
                        ),
                        option.copy(
                            title = StringResource("Kidney Disease"),
                        ),
                    ),
                    footer = null,
                ),
            ),
        ),
        monthYearPicker = MutableStateFlow(null),
    )

    @Test
    fun `DemographicsChoiceSheet screenshot`() {
        // when / then
        screenshot {
            base.Content()
        }
    }
}
