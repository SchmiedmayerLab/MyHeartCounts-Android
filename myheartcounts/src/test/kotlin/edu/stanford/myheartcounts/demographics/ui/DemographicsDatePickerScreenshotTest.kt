//
// This source file is part of the My Heart Counts Android open-source project
//
// SPDX-FileCopyrightText: 2026 Stanford University and the project authors (see CONTRIBUTORS.md)
//
// SPDX-License-Identifier: MIT
package edu.stanford.myheartcounts.demographics.ui

import edu.stanford.myheartcounts.MHCStrings
import org.grovealliance.testing.screenshot.ScreenshotTest
import org.grovealliance.ui.StringResource
import org.junit.Test
import java.time.LocalDate

class DemographicsDatePickerScreenshotTest : ScreenshotTest() {
    private val base = DemographicsDatePicker(
        selection = LocalDate.parse("1990-06-15"),
        latest = LocalDate.parse("2026-01-01"),
        confirmTitle = StringResource(MHCStrings.demographics_date_picker_confirm),
        dismissTitle = StringResource(MHCStrings.demographics_date_picker_dismiss),
        onSelect = {},
        onDismiss = {},
    )

    @Test
    fun `DemographicsDatePicker screenshot`() {
        // when / then
        screenshot {
            base.Content()
        }
    }
}
