//
// This source file is part of the My Heart Counts Android open-source project
//
// SPDX-FileCopyrightText: 2026 Stanford University and the project authors (see CONTRIBUTORS.md)
//
// SPDX-License-Identifier: MIT

package edu.stanford.myheartcounts.demographics

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.ui.text.input.KeyboardType
import edu.stanford.myheartcounts.MHCStrings
import edu.stanford.myheartcounts.demographics.ui.DemographicsDatePicker
import edu.stanford.myheartcounts.demographics.ui.DemographicsEntrySheet
import edu.stanford.myheartcounts.demographics.ui.DemographicsLayout
import edu.stanford.myheartcounts.demographics.ui.DemographicsMenu
import edu.stanford.myheartcounts.model.demographics.BiologicalSex
import edu.stanford.myheartcounts.model.demographics.BloodType
import edu.stanford.myheartcounts.model.demographics.MHCGenderIdentity
import edu.stanford.myheartcounts.standard.MeasurementUnits
import edu.stanford.myheartcounts.standard.MeasurementUnitsProvider
import kotlinx.coroutines.flow.StateFlow
import org.grovealliance.core.time.TimeProvider
import org.grovealliance.ui.ActionSink
import org.grovealliance.ui.GroveAppBar
import org.grovealliance.ui.GroveIconButton
import org.grovealliance.ui.ImageResource
import org.grovealliance.ui.StringResource
import org.grovealliance.ui.groveAppBar

/**
 * Builds what opens from a question of the [DemographicsLayout] to answer it: menus, the date
 * picker and the entry sheets. Each starts from the current answer in
 * [DemographicsLayoutInput.response] and reports through [DemographicsLayoutInput.actionSink].
 */
interface DemographicsPickerMapper {

    /**
     * Maps the gender identity answers, marking the current one.
     */
    fun mapGenderIdentityMenu(input: DemographicsLayoutInput): DemographicsMenu

    /**
     * Maps the biological sex at birth answers, marking the current one.
     */
    fun mapBiologicalSexAtBirthMenu(input: DemographicsLayoutInput): DemographicsMenu

    /**
     * Maps the known blood types, marking the current one.
     */
    fun mapBloodTypeMenu(input: DemographicsLayoutInput): DemographicsMenu

    /**
     * Maps the date of birth picker, allowing no date after today.
     */
    fun mapDateOfBirthPicker(input: DemographicsLayoutInput): DemographicsDatePicker

    /**
     * Maps the current answer to [entry] into the texts the fields of the sheet typing it in start
     * from, empty when it is unanswered.
     */
    fun mapEntryText(entry: DemographicsEntry, input: DemographicsLayoutInput): List<String>

    /**
     * Maps the sheet typing in the answer to [entry] in the participant's units, showing [text]
     * beneath [appBar].
     */
    fun mapEntrySheet(
        entry: DemographicsEntry,
        text: StateFlow<List<String>>,
        appBar: StateFlow<GroveAppBar>,
        input: DemographicsLayoutInput,
    ): DemographicsEntrySheet

    /**
     * Maps the app bar of the sheet typing in the answer to [entry], offering to save only while
     * [canSave].
     */
    fun mapEntryAppBar(
        entry: DemographicsEntry,
        canSave: Boolean,
        input: DemographicsLayoutInput,
    ): GroveAppBar
}

/**
 * Default [DemographicsPickerMapper] implementation, mapping answers into text through [answerMapper]
 * and measurements through [measurementMapper], in the units of [unitsProvider].
 */
class DemographicsPickerMapperImpl(
    private val answerMapper: DemographicsAnswerMapper,
    private val measurementMapper: DemographicsMeasurementMapper,
    private val unitsProvider: MeasurementUnitsProvider,
    private val timeProvider: TimeProvider,
) : DemographicsPickerMapper {

    override fun mapGenderIdentityMenu(input: DemographicsLayoutInput) = menu(
        field = DemographicsField.GenderIdentity,
        answers = MHCGenderIdentity.entries,
        current = input.response.value.genderIdentity,
        label = answerMapper::mapText,
        change = { DemographicsAction.GenderIdentityChanged(value = it) },
        actionSink = input.actionSink,
    )

    override fun mapBiologicalSexAtBirthMenu(input: DemographicsLayoutInput) = menu(
        field = DemographicsField.SexAtBirth,
        answers = BiologicalSex.entries,
        current = input.response.value.biologicalSexAtBirth,
        label = answerMapper::mapText,
        change = { DemographicsAction.BiologicalSexAtBirthChanged(value = it) },
        actionSink = input.actionSink,
    )

    override fun mapBloodTypeMenu(input: DemographicsLayoutInput) = menu(
        field = DemographicsField.BloodType,
        answers = BloodType.entries - BloodType.NOT_SET,
        current = input.response.value.bloodType,
        label = answerMapper::mapText,
        change = { DemographicsAction.BloodTypeChanged(value = it) },
        actionSink = input.actionSink,
    )

    override fun mapDateOfBirthPicker(input: DemographicsLayoutInput) = DemographicsDatePicker(
        selection = input.response.value.dateOfBirth,
        latest = timeProvider.nowLocalDate(),
        confirmTitle = StringResource(MHCStrings.demographics_date_picker_confirm),
        dismissTitle = StringResource(MHCStrings.demographics_date_picker_dismiss),
        onSelect = { input.actionSink.push(DemographicsAction.DateOfBirthChanged(value = it)) },
        onDismiss = { input.actionSink.push(DemographicsAction.DatePickerDismissed) },
    )

    override fun mapEntryText(entry: DemographicsEntry, input: DemographicsLayoutInput): List<String> {
        val response = input.response.value
        return when (entry) {
            DemographicsEntry.Height -> measurementMapper.mapHeightEntry(heightInCm = response.heightInCm)
            DemographicsEntry.Weight -> measurementMapper.mapWeightEntry(weightInKg = response.weightInKg)
            DemographicsEntry.NHSNumber -> listOf(response.nhsNumber?.stringValue.orEmpty())
        }
    }

    override fun mapEntrySheet(
        entry: DemographicsEntry,
        text: StateFlow<List<String>>,
        appBar: StateFlow<GroveAppBar>,
        input: DemographicsLayoutInput,
    ) = DemographicsEntrySheet(
        appBar = appBar,
        fields = entryFields(entry = entry),
        entry = text,
        help = if (entry == DemographicsEntry.NHSNumber) {
            DemographicsEntrySheet.Help(
                title = StringResource(MHCStrings.demographics_nhs_number_help),
                onClick = { input.actionSink.push(DemographicsAction.NHSNumberHelpSelected) },
            )
        } else {
            null
        },
        onEntryChange = { index, text ->
            input.actionSink.push(
                DemographicsAction.EntryChanged(
                    index = index,
                    text = text,
                ),
            )
        },
    )

    override fun mapEntryAppBar(
        entry: DemographicsEntry,
        canSave: Boolean,
        input: DemographicsLayoutInput,
    ) = groveAppBar {
        title(
            when (entry) {
                DemographicsEntry.Height -> MHCStrings.demographics_height_sheet_title
                DemographicsEntry.Weight -> MHCStrings.demographics_weight_sheet_title
                DemographicsEntry.NHSNumber -> MHCStrings.demographics_nhs_number_sheet_title
            },
        )
        close { input.actionSink.push(DemographicsAction.SheetClosed) }
        action(
            icon = GroveIconButton(
                image = ImageResource(image = Icons.Default.Check),
                enabled = canSave,
                onClick = { input.actionSink.push(DemographicsAction.EntrySaved) },
            ),
        )
    }

    private fun entryFields(entry: DemographicsEntry): List<DemographicsEntrySheet.Field> = when (entry) {
        DemographicsEntry.Height -> when (unitsProvider.currentUnits.height) {
            MeasurementUnits.Height.Centimetres -> listOf(
                measurementField(unit = MHCStrings.demographics_unit_cm),
            )
            MeasurementUnits.Height.FeetAndInches -> listOf(
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
            )
        }
        DemographicsEntry.Weight -> listOf(
            measurementField(
                unit = when (unitsProvider.currentUnits.weight) {
                    MeasurementUnits.Weight.Kilograms -> MHCStrings.demographics_unit_kg
                    MeasurementUnits.Weight.Pounds -> MHCStrings.demographics_unit_lb
                },
            ),
        )
        DemographicsEntry.NHSNumber -> listOf(
            DemographicsEntrySheet.Field(
                label = StringResource(MHCStrings.demographics_field_nhsnumber),
                unit = null,
                placeholder = null,
                keyboardType = KeyboardType.Number,
            ),
        )
    }

    private fun measurementField(unit: Int) = DemographicsEntrySheet.Field(
        label = StringResource(MHCStrings.demographics_measurement_value),
        unit = StringResource(unit),
        placeholder = StringResource(MHCStrings.demographics_measurement_placeholder),
        keyboardType = KeyboardType.Decimal,
    )

    private fun <T : Any> menu(
        field: DemographicsField,
        answers: List<T>,
        current: T?,
        label: (T) -> StringResource,
        change: (T?) -> DemographicsAction.ResponseChanged,
        actionSink: ActionSink<DemographicsAction>,
    ): DemographicsMenu {
        val choices = listOf(null) + answers
        return DemographicsMenu(
            field = field,
            options = choices.mapIndexed { index, choice ->
                DemographicsMenu.Option(
                    index = index,
                    label = choice?.let(label) ?: StringResource(MHCStrings.demographics_value_empty),
                    isSelected = choice == current,
                    isFollowedByDivider = choice == null,
                )
            },
            onSelect = { index -> actionSink.push(change(choices[index])) },
            onDismiss = { actionSink.push(DemographicsAction.MenuDismissed) },
        )
    }
}
