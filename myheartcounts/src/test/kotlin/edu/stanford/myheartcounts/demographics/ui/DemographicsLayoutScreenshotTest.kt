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
import org.junit.Test

class DemographicsLayoutScreenshotTest : ScreenshotTest() {
    private val row = DemographicsLayout.Row(
        field = DemographicsField.DateOfBirth,
        label = StringResource("Date of Birth"),
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
    private val base = DemographicsLayout(
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
                        label = StringResource("Gender Identity"),
                        value = MutableStateFlow(
                            DemographicsLayout.Value(
                                text = StringResource("-"),
                                isOptional = false,
                            ),
                        ),
                        accessory = DemographicsLayout.Accessory.Menu,
                    ),
                    row.copy(
                        field = DemographicsField.SexAtBirth,
                        label = StringResource("Biological Sex at Birth"),
                        value = MutableStateFlow(
                            DemographicsLayout.Value(
                                text = StringResource("Female"),
                                isOptional = true,
                            ),
                        ),
                        accessory = DemographicsLayout.Accessory.Menu,
                    ),
                    row.copy(
                        field = DemographicsField.BloodType,
                        label = StringResource("Blood Type"),
                        value = MutableStateFlow(
                            DemographicsLayout.Value(
                                text = StringResource("-"),
                                isOptional = true,
                            ),
                        ),
                        accessory = DemographicsLayout.Accessory.Menu,
                    ),
                ),
            ),
            DemographicsLayout.Section(
                rows = listOf(
                    row.copy(
                        field = DemographicsField.Height,
                        label = StringResource("Height"),
                        value = MutableStateFlow(
                            DemographicsLayout.Value(
                                text = StringResource("172.5 cm"),
                                isOptional = false,
                            ),
                        ),
                    ),
                    row.copy(
                        field = DemographicsField.Weight,
                        label = StringResource("Weight"),
                        value = MutableStateFlow(
                            DemographicsLayout.Value(
                                text = StringResource("-"),
                                isOptional = false,
                            ),
                        ),
                    ),
                ),
            ),
            DemographicsLayout.Section(
                rows = listOf(
                    row.copy(
                        field = DemographicsField.RaceEthnicity,
                        label = StringResource("Race / Ethnicity"),
                        value = MutableStateFlow(
                            DemographicsLayout.Value(
                                text = StringResource("2 selected"),
                                isOptional = false,
                            ),
                        ),
                        accessory = DemographicsLayout.Accessory.Disclosure,
                    ),
                    row.copy(
                        field = DemographicsField.LatinoStatus,
                        label = StringResource("Are you Hispanic/Latino?"),
                        value = MutableStateFlow(
                            DemographicsLayout.Value(
                                text = StringResource("Not Set"),
                                isOptional = false,
                            ),
                        ),
                        accessory = DemographicsLayout.Accessory.Disclosure,
                    ),
                ),
            ),
            DemographicsLayout.Section(
                rows = listOf(
                    row.copy(
                        field = DemographicsField.Comorbidities,
                        label = StringResource("Comorbidities"),
                        value = MutableStateFlow(
                            DemographicsLayout.Value(
                                text = StringResource("None"),
                                isOptional = true,
                            ),
                        ),
                        accessory = DemographicsLayout.Accessory.Disclosure,
                    ),
                ),
            ),
            DemographicsLayout.Section(
                rows = listOf(
                    row.copy(
                        field = DemographicsField.USRegion,
                        label = StringResource("US State / Territory"),
                        value = MutableStateFlow(
                            DemographicsLayout.Value(
                                text = StringResource("DC"),
                                isOptional = true,
                            ),
                        ),
                        accessory = DemographicsLayout.Accessory.Disclosure,
                    ),
                    row.copy(
                        field = DemographicsField.USEducation,
                        label = StringResource("Education Level"),
                        value = MutableStateFlow(
                            DemographicsLayout.Value(
                                text = StringResource("Not Set"),
                                isOptional = true,
                            ),
                        ),
                        accessory = DemographicsLayout.Accessory.Disclosure,
                    ),
                    row.copy(
                        field = DemographicsField.USIncome,
                        label = StringResource("Total Household Income"),
                        value = MutableStateFlow(
                            DemographicsLayout.Value(
                                text = StringResource("Not Set"),
                                isOptional = true,
                            ),
                        ),
                        accessory = DemographicsLayout.Accessory.Disclosure,
                    ),
                ),
            ),
            DemographicsLayout.Section(
                rows = listOf(
                    row.copy(
                        field = DemographicsField.StageOfChange,
                        label = StringResource("Stage of Change"),
                        value = MutableStateFlow(
                            DemographicsLayout.Value(
                                text = StringResource("No Selection"),
                                isOptional = true,
                            ),
                        ),
                        accessory = DemographicsLayout.Accessory.Disclosure,
                    ),
                ),
            ),
            DemographicsLayout.Section(
                rows = listOf(
                    row.copy(
                        field = DemographicsField.ReferralSource,
                        label = StringResource("Referral Source"),
                        value = MutableStateFlow(
                            DemographicsLayout.Value(
                                text = StringResource("Not Set"),
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
    )

    @Test
    fun `DemographicsLayout screenshot`() {
        // when / then
        screenshot {
            base.Content()
        }
    }

    @Test
    fun `DemographicsLayout long answer screenshot`() {
        // given
        val layout = base.copy(
            sections = listOf(
                DemographicsLayout.Section(
                    rows = listOf(
                        row.copy(
                            field = DemographicsField.ReferralSource,
                            label = StringResource("Referral Source"),
                            value = MutableStateFlow(
                                DemographicsLayout.Value(
                                    text = StringResource("Healthcare provider (doctor, cardiologist, nurse)"),
                                    isOptional = true,
                                ),
                            ),
                            accessory = DemographicsLayout.Accessory.Disclosure,
                        ),
                    ),
                ),
            ),
        )

        // when / then
        screenshot {
            layout.Content()
        }
    }
}
