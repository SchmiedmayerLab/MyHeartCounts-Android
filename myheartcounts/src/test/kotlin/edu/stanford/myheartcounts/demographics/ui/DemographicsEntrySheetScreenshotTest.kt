//
// This source file is part of the My Heart Counts Android open-source project
//
// SPDX-FileCopyrightText: 2026 Stanford University and the project authors (see CONTRIBUTORS.md)
//
// SPDX-License-Identifier: MIT

package edu.stanford.myheartcounts.demographics.ui

import androidx.compose.ui.text.input.KeyboardType
import edu.stanford.myheartcounts.MHCStrings
import kotlinx.coroutines.flow.MutableStateFlow
import org.grovealliance.testing.screenshot.ScreenshotTest
import org.grovealliance.ui.StringResource
import org.grovealliance.ui.groveAppBar
import org.junit.Test

class DemographicsEntrySheetScreenshotTest : ScreenshotTest() {
    private val base = DemographicsEntrySheet(
        appBar = MutableStateFlow(
            groveAppBar {
                title(MHCStrings.demographics_nhs_number_sheet_title)
                close {}
            },
        ),
        fields = listOf(
            DemographicsEntrySheet.Field(
                label = StringResource(MHCStrings.demographics_field_nhsnumber),
                unit = null,
                placeholder = null,
                keyboardType = KeyboardType.Number,
            ),
        ),
        entry = MutableStateFlow(listOf("943 476 5919")),
        help = DemographicsEntrySheet.Help(
            title = StringResource(MHCStrings.demographics_nhs_number_help),
            onClick = {},
        ),
        onEntryChange = { _, _ -> },
    )

    @Test
    fun `DemographicsEntrySheet screenshot`() {
        // when / then
        screenshot {
            base.Content()
        }
    }

    @Test
    fun `DemographicsEntrySheet feet and inches screenshot`() {
        // given
        val sheet = base.copy(
            appBar = MutableStateFlow(
                groveAppBar {
                    title(MHCStrings.demographics_height_sheet_title)
                    close {}
                },
            ),
            fields = listOf(
                DemographicsEntrySheet.Field(
                    label = StringResource(MHCStrings.demographics_measurement_feet),
                    unit = null,
                    placeholder = StringResource(MHCStrings.demographics_measurement_feet_placeholder),
                    keyboardType = KeyboardType.Number,
                ),
                DemographicsEntrySheet.Field(
                    label = StringResource(MHCStrings.demographics_measurement_inches),
                    unit = null,
                    placeholder = StringResource(MHCStrings.demographics_measurement_inches_placeholder),
                    keyboardType = KeyboardType.Number,
                ),
            ),
            entry = MutableStateFlow(listOf("", "")),
            help = null,
        )

        // when / then
        screenshot {
            sheet.Content()
        }
    }
}
