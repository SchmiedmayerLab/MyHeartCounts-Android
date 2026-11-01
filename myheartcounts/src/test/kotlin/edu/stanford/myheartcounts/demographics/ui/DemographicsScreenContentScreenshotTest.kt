//
// This source file is part of the My Heart Counts Android open-source project
//
// SPDX-FileCopyrightText: 2026 Stanford University and the project authors (see CONTRIBUTORS.md)
//
// SPDX-License-Identifier: MIT
package edu.stanford.myheartcounts.demographics.ui

import edu.stanford.myheartcounts.MHCStrings
import edu.stanford.myheartcounts.demographics.DemographicsField
import kotlinx.coroutines.flow.MutableStateFlow
import org.grovealliance.testing.screenshot.ScreenshotTest
import org.grovealliance.ui.GroveScaffoldState
import org.grovealliance.ui.StringResource
import org.grovealliance.ui.groveAppBar
import org.junit.Test

class DemographicsScreenContentScreenshotTest : ScreenshotTest() {
    private val row = DemographicsLayout.Row(
        field = DemographicsField.DateOfBirth,
        label = StringResource(MHCStrings.demographics_field_dateofbirth),
        value = MutableStateFlow(
            DemographicsLayout.Value(
                text = StringResource("Jun 15, 1990"),
                isOptional = false,
            ),
        ),
        menu = MutableStateFlow(null),
        accessory = DemographicsLayout.Accessory.None,
        onClick = {},
    )
    private val base = DemographicsScreenContent(
        appBar = groveAppBar {
            back {}
            title(MHCStrings.demographics_title)
        },
        layout = DemographicsLayout(
            healthImport = HealthImportCard(
                title = StringResource(MHCStrings.demographics_health_import_title),
                description = StringResource(MHCStrings.demographics_health_import_description),
                onClick = {},
            ),
            sections = listOf(
                DemographicsLayout.Section(
                    rows = listOf(
                        row,
                        row.copy(
                            field = DemographicsField.GenderIdentity,
                            label = StringResource(MHCStrings.demographics_field_genderidentity),
                            value = MutableStateFlow(
                                DemographicsLayout.Value(
                                    text = StringResource(MHCStrings.demographics_gender_identity_2),
                                    isOptional = false,
                                ),
                            ),
                            accessory = DemographicsLayout.Accessory.Menu,
                        ),
                    ),
                ),
                DemographicsLayout.Section(
                    rows = listOf(
                        row.copy(
                            field = DemographicsField.ReferralSource,
                            label = StringResource(MHCStrings.demographics_field_referralsource),
                            value = MutableStateFlow(
                                DemographicsLayout.Value(
                                    text = StringResource(MHCStrings.demographics_value_not_set),
                                    isOptional = true,
                                ),
                            ),
                            accessory = DemographicsLayout.Accessory.Disclosure,
                        ),
                    ),
                ),
            ),
            datePicker = MutableStateFlow(null),
            scaffoldState = GroveScaffoldState(),
        ),
    )

    @Test
    fun `DemographicsScreenContent complete screenshot`() {
        // when / then
        screenshot {
            base.Content()
        }
    }

    @Test
    fun `DemographicsScreenContent embedded screenshot`() {
        // given
        val content = base.copy(appBar = null)

        // when / then
        screenshot {
            content.Content()
        }
    }
}
