//
// This source file is part of the My Heart Counts Android open-source project
//
// SPDX-FileCopyrightText: 2026 Stanford University and the project authors (see CONTRIBUTORS.md)
//
// SPDX-License-Identifier: MIT

package edu.stanford.myheartcounts.demographics.ui

import edu.stanford.myheartcounts.MHCStrings
import kotlinx.coroutines.flow.MutableStateFlow
import org.grovealliance.testing.screenshot.ScreenshotTest
import org.grovealliance.ui.StringResource
import org.grovealliance.ui.groveAppBar
import org.junit.Test

class DemographicsChoiceSheetScreenshotTest : ScreenshotTest() {
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
            title(MHCStrings.demographics_comorbidities_sheet_title)
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
                                label = StringResource(MHCStrings.demographics_comorbidity_since),
                                value = StringResource("Mar 2020"),
                                isSet = true,
                                onClick = {},
                            ),
                        ),
                        option.copy(
                            index = 1,
                            title = StringResource("Heart Failure"),
                            isSelected = true,
                            followUp = DemographicsChoiceSheet.FollowUp(
                                label = StringResource(MHCStrings.demographics_comorbidity_since),
                                value = StringResource(MHCStrings.demographics_comorbidity_since_unknown),
                                isSet = false,
                                onClick = {},
                            ),
                        ),
                        option.copy(
                            index = 2,
                            title = StringResource("Coronary Artery Disease"),
                        ),
                    ),
                    footer = null,
                ),
                DemographicsChoiceSheet.Section(
                    header = null,
                    options = listOf(
                        option.copy(
                            index = 3,
                            title = StringResource("Stroke"),
                        ),
                        option.copy(
                            index = 4,
                            title = StringResource("Kidney Disease"),
                        ),
                    ),
                    footer = null,
                ),
            ),
        ),
        monthYearPicker = MutableStateFlow(null),
        onSelect = {},
    )

    @Test
    fun `DemographicsChoiceSheet screenshot`() {
        // when / then
        screenshot {
            base.Content()
        }
    }
}
