//
// This source file is part of the My Heart Counts Android open-source project
//
// SPDX-FileCopyrightText: 2026 Stanford University and the project authors (see CONTRIBUTORS.md)
//
// SPDX-License-Identifier: MIT

package edu.stanford.myheartcounts.dashboard.entry

import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.KeyboardType
import edu.stanford.myheartcounts.MHCStrings
import kotlinx.coroutines.flow.MutableStateFlow
import org.grovealliance.testing.screenshot.ScreenshotTest
import org.grovealliance.ui.GroveScaffoldState
import org.grovealliance.ui.StringResource
import org.grovealliance.ui.groveAppBar
import org.junit.Test

class HeartScoreEntryScreenContentScreenshotTest : ScreenshotTest() {

    @Test
    fun `HeartScoreEntryScreenContent blood pressure screenshot`() {
        val screen = screen(
            title = StringResource(MHCStrings.heart_entry_enter_component, StringResource(MHCStrings.heart_score_blood_pressure)),
            panels = listOf(
                panel(
                    row(label = MHCStrings.heart_entry_systolic, value = "120", unit = "mmHg"),
                    row(label = MHCStrings.heart_entry_diastolic, value = "80", unit = "mmHg"),
                ),
            ),
        )

        screenshot {
            screen.Content(modifier = Modifier.fillMaxSize())
        }
    }

    @Test
    fun `HeartScoreEntryScreenContent empty LDL cholesterol screenshot`() {
        val screen = screen(
            title = StringResource(MHCStrings.heart_entry_enter_component, StringResource(MHCStrings.heart_score_blood_lipids)),
            panels = listOf(
                panel(row(label = MHCStrings.heart_score_blood_lipids, value = "", unit = "mg/dL")),
            ),
            canSave = false,
        )

        screenshot {
            screen.Content(modifier = Modifier.fillMaxSize())
        }
    }

    @Test
    fun `HeartScoreEntryScreenContent direct BMI screenshot`() {
        val screen = screen(
            title = StringResource(MHCStrings.heart_entry_bmi_title),
            picker = bmiPicker(selectedIndex = 0),
            panels = bmiPanels(physical = panel(row(label = MHCStrings.heart_entry_weight, value = "", unit = "kg"))),
            selectedPanel = 0,
        )

        screenshot {
            screen.Content(modifier = Modifier.fillMaxSize())
        }
    }

    @Test
    fun `HeartScoreEntryScreenContent metric BMI screenshot`() {
        val screen = screen(
            title = StringResource(MHCStrings.heart_entry_bmi_title),
            picker = bmiPicker(selectedIndex = 1),
            panels = bmiPanels(
                physical = panel(
                    row(label = MHCStrings.heart_entry_weight, value = "81", unit = "kg"),
                    row(label = MHCStrings.heart_entry_height, value = "180", unit = "cm"),
                ),
            ),
            selectedPanel = 1,
            footnote = StringResource(MHCStrings.heart_entry_calculated_bmi, "25"),
        )

        screenshot {
            screen.Content(modifier = Modifier.fillMaxSize())
        }
    }

    @Test
    fun `HeartScoreEntryScreenContent imperial BMI screenshot`() {
        val screen = screen(
            title = StringResource(MHCStrings.heart_entry_bmi_title),
            picker = bmiPicker(selectedIndex = 1),
            panels = bmiPanels(
                physical = panel(
                    row(label = MHCStrings.heart_entry_weight, value = "180", unit = "lb"),
                    options = EntryOptionsRow(
                        label = StringResource(MHCStrings.heart_entry_height),
                        pickers = listOf(
                            picker(
                                options = (MIN_FEET..MAX_FEET).map { StringResource(MHCStrings.heart_entry_feet_value, it) },
                                selectedIndex = 5 - MIN_FEET,
                            ),
                            picker(
                                options = (0..MAX_INCHES).map { StringResource(MHCStrings.heart_entry_inches_value, it) },
                                selectedIndex = 4,
                            ),
                        ),
                    ),
                ),
            ),
            selectedPanel = 1,
            footnote = StringResource(MHCStrings.heart_entry_calculated_bmi, "30.9"),
        )

        screenshot {
            screen.Content(modifier = Modifier.fillMaxSize())
        }
    }

    @Test
    fun `HeartScoreEntryScreenContent out of range screenshot`() {
        val screen = screen(
            title = StringResource(MHCStrings.heart_entry_enter_component, StringResource(MHCStrings.heart_score_blood_pressure)),
            panels = listOf(
                panel(
                    row(
                        label = MHCStrings.heart_entry_systolic,
                        value = "300",
                        unit = "mmHg",
                        validationMessage = StringResource(MHCStrings.heart_entry_out_of_range, "60", "250"),
                    ),
                    row(label = MHCStrings.heart_entry_diastolic, value = "80", unit = "mmHg"),
                ),
            ),
            canSave = false,
        )

        screenshot {
            screen.Content(modifier = Modifier.fillMaxSize())
        }
    }

    @Test
    fun `HeartScoreEntryScreenContent saving screenshot`() {
        val screen = screen(
            title = StringResource(MHCStrings.heart_entry_enter_component, StringResource(MHCStrings.heart_score_blood_glucose)),
            panels = listOf(
                panel(row(label = MHCStrings.heart_score_blood_glucose, value = "90", unit = "mg/dL")),
            ),
            canSave = false,
            saving = true,
        )

        screenshot {
            screen.Content(modifier = Modifier.fillMaxSize())
        }
    }

    @Test
    fun `HeartScoreEntryScreenContent save failure screenshot`() {
        val screen = screen(
            title = StringResource(MHCStrings.heart_entry_enter_component, StringResource(MHCStrings.heart_score_blood_glucose)),
            panels = listOf(
                panel(row(label = MHCStrings.heart_score_blood_glucose, value = "90", unit = "mg/dL")),
            ),
            error = StringResource(MHCStrings.heart_entry_permission_error),
        )

        screenshot {
            screen.Content(modifier = Modifier.fillMaxSize())
        }
    }

    @Test
    fun `HeartScoreEntryScreenContent future time screenshot`() {
        val screen = screen(
            title = StringResource(MHCStrings.heart_entry_enter_component, StringResource(MHCStrings.heart_score_blood_pressure)),
            panels = listOf(
                panel(
                    row(label = MHCStrings.heart_entry_systolic, value = "120", unit = "mmHg"),
                    row(label = MHCStrings.heart_entry_diastolic, value = "80", unit = "mmHg"),
                ),
            ),
            canSave = false,
            error = StringResource(MHCStrings.heart_entry_future_time),
        )

        screenshot {
            screen.Content(modifier = Modifier.fillMaxSize())
        }
    }

    @Suppress("detekt:LongParameterList")
    private fun screen(
        title: StringResource,
        panels: List<EntryFieldGroup>,
        picker: EntryModePicker? = null,
        selectedPanel: Int = 0,
        canSave: Boolean = true,
        saving: Boolean = false,
        footnote: StringResource? = null,
        error: StringResource? = null,
    ) = HeartScoreEntryScreenContent(
        scaffold = GroveScaffoldState(
            appBar = groveAppBar {
                close {}
                action(
                    content = EntrySaveAction(
                        title = StringResource(MHCStrings.heart_entry_save),
                        isEnabled = MutableStateFlow(canSave),
                        isLoading = MutableStateFlow(saving),
                        onClick = {},
                    ),
                )
            },
        ),
        layout = EntryFormLayout(
            title = title,
            picker = picker,
            dateRow = EntryDateRow(
                label = StringResource(MHCStrings.heart_entry_date),
                date = MutableStateFlow(StringResource("Jan 1, 2026")),
                time = MutableStateFlow(
                    EntryDateRow.Time(
                        label = StringResource("9:15 AM"),
                        hour = 9,
                        minute = 15,
                    ),
                ),
                dialog = EntryDateRow.DialogLabels(
                    title = StringResource(MHCStrings.heart_entry_time),
                    confirm = StringResource(MHCStrings.heart_entry_confirm),
                    cancel = StringResource(MHCStrings.heart_entry_cancel),
                ),
                isSelectableDate = { true },
                onDateSelected = {},
                onTimeSelected = { _, _ -> },
            ),
            panels = panels,
            selectedPanel = MutableStateFlow(selectedPanel),
            feedback = EntryFormLayout.Feedback(
                footnote = MutableStateFlow(footnote),
                error = MutableStateFlow(error),
            ),
        ),
    )

    private fun bmiPicker(selectedIndex: Int) = EntryModePicker(
        options = listOf(
            StringResource(MHCStrings.heart_entry_bmi_mode),
            StringResource(MHCStrings.heart_entry_weight_height),
        ),
        selectedIndex = MutableStateFlow(selectedIndex),
        onSelected = {},
    )

    private fun bmiPanels(physical: EntryFieldGroup) = listOf(
        panel(row(label = MHCStrings.heart_score_body_mass_index, value = "23.5", unit = null)),
        physical,
    )

    private fun panel(vararg rows: EntryFieldRow, options: EntryOptionsRow? = null) = EntryFieldGroup(
        rows = rows.toList(),
        options = options,
    )

    private fun row(
        label: Int,
        value: String,
        unit: String?,
        validationMessage: StringResource? = null,
    ) = EntryFieldRow(
        label = StringResource(label),
        value = MutableStateFlow(value),
        placeholder = StringResource("0"),
        unit = unit?.let { StringResource(it) },
        keyboardType = KeyboardType.Number,
        validationMessage = MutableStateFlow(validationMessage),
        onValueChange = {},
    )

    private fun picker(options: List<StringResource>, selectedIndex: Int) = EntryOptionsRow.Picker(
        options = options,
        selectedIndex = MutableStateFlow(selectedIndex),
        onSelected = {},
    )

    private companion object {
        const val MIN_FEET = 3
        const val MAX_FEET = 8
        const val MAX_INCHES = 11
    }
}
