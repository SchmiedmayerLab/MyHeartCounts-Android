//
// This source file is part of the My Heart Counts Android open-source project
//
// SPDX-FileCopyrightText: 2026 Stanford University and the project authors (see CONTRIBUTORS.md)
// SPDX-License-Identifier: MIT
//

package edu.stanford.myheartcounts.dashboard.entry

import androidx.compose.ui.text.input.KeyboardType
import edu.stanford.myheartcounts.MHCStrings
import edu.stanford.myheartcounts.dashboard.HeartScoreComponent
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import org.grovealliance.core.time.TimeProvider
import org.grovealliance.ui.ActionSink
import org.grovealliance.ui.GroveAppBar
import org.grovealliance.ui.StringResource
import org.grovealliance.ui.groveAppBar
import java.text.NumberFormat
import java.time.ZoneOffset
import java.time.format.DateTimeFormatter
import java.time.format.FormatStyle

/**
 * The measurement state and its event destination, used to build the form once.
 */
data class HeartScoreEntryContentInput(
    val state: StateFlow<HeartScoreEntryState>,
    val scope: CoroutineScope,
    val actionSink: ActionSink<HeartScoreEntryAction>,
)

/**
 * The toolbar and body of the measurement form.
 */
data class HeartScoreEntryMappedLayout(
    val appBar: GroveAppBar,
    val layout: EntryFormLayout,
)

interface HeartScoreEntryContentMapper {
    fun map(input: HeartScoreEntryContentInput): HeartScoreEntryMappedLayout
}

class HeartScoreEntryContentMapperImpl(
    private val timeProvider: TimeProvider,
) : HeartScoreEntryContentMapper {
    override fun map(input: HeartScoreEntryContentInput): HeartScoreEntryMappedLayout {
        val form = input.state.value.form
        val bmi = form.component == HeartScoreComponent.BodyMassIndex
        val selected = input.display { if (bmi && !it.form.directBmi) 1 else 0 }
        val number = NumberFormat.getNumberInstance(form.locale).apply { maximumFractionDigits = 1 }
        return HeartScoreEntryMappedLayout(
            appBar = appBar(input = input),
            layout = EntryFormLayout(
                title = if (bmi) {
                    StringResource(MHCStrings.heart_entry_bmi_title)
                } else {
                    StringResource(
                        MHCStrings.heart_entry_enter_component,
                        StringResource(form.component.title),
                    )
                },
                picker = if (bmi) {
                    EntryModePicker(
                        options = listOf(
                            StringResource(MHCStrings.heart_entry_bmi_mode),
                            StringResource(MHCStrings.heart_entry_weight_height),
                        ),
                        selectedIndex = selected,
                        onSelected = {
                            input.actionSink.push(HeartScoreEntryAction.ModeChanged(direct = it == 0))
                        },
                    )
                } else {
                    null
                },
                dateRow = dateRow(input = input),
                panels = panels(input = input),
                selectedPanel = selected,
                feedback = EntryFormLayout.Feedback(
                    footnote = input.display { state ->
                        state.bmi?.takeIf { !state.form.directBmi }?.let {
                            StringResource(
                                MHCStrings.heart_entry_calculated_bmi,
                                number.format(it),
                            )
                        }
                    },
                    error = input.display { it.error },
                ),
            ),
        )
    }

    private fun appBar(input: HeartScoreEntryContentInput) = groveAppBar {
        close { input.actionSink.push(HeartScoreEntryAction.Close) }
        action(
            content = EntrySaveAction(
                title = StringResource(MHCStrings.heart_entry_save),
                isEnabled = input.display { it.canSave && !it.saving },
                isLoading = input.display { it.saving },
                onClick = { input.actionSink.push(HeartScoreEntryAction.Save) },
            ),
        )
    }

    private fun dateRow(input: HeartScoreEntryContentInput): EntryDateRow {
        val locale = input.state.value.form.locale
        val dateFormat = DateTimeFormatter.ofLocalizedDate(FormatStyle.MEDIUM).withLocale(locale)
        val timeFormat = DateTimeFormatter.ofLocalizedTime(FormatStyle.SHORT).withLocale(locale)
        return EntryDateRow(
            label = StringResource(MHCStrings.heart_entry_date),
            date = input.display { StringResource(dateFormat.format(it.form.date)) },
            time = input.display {
                EntryDateRow.Time(
                    label = StringResource(timeFormat.format(it.form.date)),
                    hour = it.form.date.hour,
                    minute = it.form.date.minute,
                )
            },
            dialog = EntryDateRow.DialogLabels(
                title = StringResource(MHCStrings.heart_entry_time),
                confirm = StringResource(MHCStrings.heart_entry_confirm),
                cancel = StringResource(MHCStrings.heart_entry_cancel),
            ),
            isSelectableDate = { it.atZone(ZoneOffset.UTC).toLocalDate() <= timeProvider.nowLocalDate() },
            onDateSelected = {
                val date = it.atZone(ZoneOffset.UTC).toLocalDate().atTime(input.state.value.form.date.toLocalTime())
                input.actionSink.push(HeartScoreEntryAction.DateChanged(value = date))
            },
            onTimeSelected = { hour, minute ->
                input.actionSink.push(
                    HeartScoreEntryAction.DateChanged(
                        value = input.state.value.form.date.withHour(hour).withMinute(minute),
                    ),
                )
            },
        )
    }

    private fun panels(input: HeartScoreEntryContentInput): List<EntryFieldGroup> {
        val form = input.state.value.form
        val rows = when (form.component) {
            HeartScoreComponent.BloodPressure ->
                listOf(
                    field(
                        input = input,
                        label = MHCStrings.heart_entry_systolic,
                        unit = "mmHg",
                        field = HeartScoreEntryAction.Field.Value,
                    ),
                    field(
                        input = input,
                        label = MHCStrings.heart_entry_diastolic,
                        unit = "mmHg",
                        field = HeartScoreEntryAction.Field.Diastolic,
                    ),
                )
            HeartScoreComponent.BodyMassIndex ->
                return listOf(
                    EntryFieldGroup(
                        rows = listOf(
                            field(
                                input = input,
                                label = form.component.title,
                                unit = null,
                                field = HeartScoreEntryAction.Field.Value,
                            ),
                        ),
                        options = null,
                    ),
                    physicalPanel(input = input),
                )
            else ->
                listOf(
                    field(
                        input = input,
                        label = form.component.title,
                        unit = "mg/dL",
                        field = HeartScoreEntryAction.Field.Value,
                    ),
                )
        }
        return listOf(
            EntryFieldGroup(
                rows = rows,
                options = null,
            ),
        )
    }

    private fun physicalPanel(input: HeartScoreEntryContentInput): EntryFieldGroup {
        val form = input.state.value.form
        return EntryFieldGroup(
            rows = listOfNotNull(
                field(
                    input = input,
                    label = MHCStrings.heart_entry_weight,
                    unit = if (form.weightInPounds) "lb" else "kg",
                    field = HeartScoreEntryAction.Field.Weight,
                ),
                if (form.heightInFeet) {
                    null
                } else {
                    field(
                        input = input,
                        label = MHCStrings.heart_entry_height,
                        unit = "cm",
                        field = HeartScoreEntryAction.Field.Height,
                    )
                },
            ),
            options = if (form.heightInFeet) {
                imperialHeightOptions(input = input)
            } else {
                null
            },
        )
    }

    private fun imperialHeightOptions(input: HeartScoreEntryContentInput): EntryOptionsRow = EntryOptionsRow(
        label = StringResource(MHCStrings.heart_entry_height),
        pickers = listOf(
            picker(
                input = input,
                field = HeartScoreEntryAction.Field.Height,
                label = MHCStrings.heart_entry_feet_value,
            ),
            picker(
                input = input,
                field = HeartScoreEntryAction.Field.Inches,
                label = MHCStrings.heart_entry_inches_value,
            ),
        ),
    )

    private fun picker(
        input: HeartScoreEntryContentInput,
        field: HeartScoreEntryAction.Field,
        label: Int,
    ): EntryOptionsRow.Picker {
        val values = input.state.value.form.limits(field = field)?.let { it.min.toInt() until it.max.toInt() } ?: IntRange.EMPTY
        return EntryOptionsRow.Picker(
            options = values.map { StringResource(label, it) },
            selectedIndex = input.display { state -> values.indexOf(state.form.text(field = field).toIntOrNull()).coerceAtLeast(0) },
            onSelected = {
                input.actionSink.push(
                    HeartScoreEntryAction.FieldChanged(
                        field = field,
                        value = values.elementAt(it).toString(),
                    ),
                )
            },
        )
    }

    private fun field(
        input: HeartScoreEntryContentInput,
        label: Int,
        unit: String?,
        field: HeartScoreEntryAction.Field,
    ): EntryFieldRow {
        val form = input.state.value.form
        val number = NumberFormat.getNumberInstance(form.locale).apply { maximumFractionDigits = 2 }
        return EntryFieldRow(
            label = StringResource(label),
            value = input.display { it.form.text(field = field) },
            placeholder = StringResource("0"),
            unit = unit?.let { StringResource(it) },
            keyboardType = if (form.acceptsWholeNumbersOnly(field = field)) KeyboardType.Number else KeyboardType.Decimal,
            validationMessage = input.display { state ->
                state.form.violatedLimits(field = field)?.let {
                    StringResource(
                        MHCStrings.heart_entry_out_of_range,
                        number.format(it.min),
                        number.format(it.max),
                    )
                }
            },
            onValueChange = {
                input.actionSink.push(
                    HeartScoreEntryAction.FieldChanged(
                        field = field,
                        value = it,
                    ),
                )
            },
        )
    }

    private fun <T> HeartScoreEntryContentInput.display(
        transform: (HeartScoreEntryState) -> T,
    ): StateFlow<T> = state.map(transform).stateIn(
        scope = scope,
        started = SharingStarted.Eagerly,
        initialValue = transform(state.value),
    )
}
