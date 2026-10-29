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
        label = StringResource(MHCStrings.demographics_field_nhsnumber),
        unit = null,
        keyboardType = KeyboardType.Number,
        entry = MutableStateFlow("943 476 5919"),
        help = DemographicsEntrySheet.Help(
            title = StringResource(MHCStrings.demographics_nhs_number_help),
            onClick = {},
        ),
        onEntryChange = {},
    )

    @Test
    fun `DemographicsEntrySheet screenshot`() {
        // when / then
        screenshot {
            base.Content()
        }
    }
}
