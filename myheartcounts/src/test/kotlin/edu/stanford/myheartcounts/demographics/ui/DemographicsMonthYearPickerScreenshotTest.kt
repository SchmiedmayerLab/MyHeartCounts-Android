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
import java.time.Month
import java.time.format.TextStyle
import java.util.Locale

class DemographicsMonthYearPickerScreenshotTest : ScreenshotTest() {
    private val base = DemographicsMonthYearPicker(
        title = StringResource(MHCStrings.demographics_condition_diabetes),
        years = (2026 downTo 1926).toList(),
        months = Month.entries.map { StringResource(it.getDisplayName(TextStyle.FULL, Locale.US)) },
        year = 2020,
        month = 3,
        yearLabel = StringResource(MHCStrings.demographics_diagnosis_year),
        monthLabel = StringResource(MHCStrings.demographics_diagnosis_month),
        noMonth = StringResource(MHCStrings.demographics_value_empty),
        confirmTitle = StringResource(MHCStrings.demographics_date_picker_confirm),
        clearTitle = StringResource(MHCStrings.demographics_diagnosis_unknown),
        dismissTitle = StringResource(MHCStrings.demographics_date_picker_dismiss),
        onSelect = { _, _ -> },
        onClear = {},
        onDismiss = {},
    )

    @Test
    fun `DemographicsMonthYearPicker screenshot`() {
        // when / then
        screenshot {
            base.Content()
        }
    }
}
