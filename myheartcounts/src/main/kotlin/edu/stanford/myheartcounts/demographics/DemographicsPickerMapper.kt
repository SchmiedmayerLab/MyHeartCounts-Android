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
     * Maps the current answer to [entry] into the text the sheet typing it in starts from, or an empty
     * text when it is unanswered.
     */
    fun mapEntryText(entry: DemographicsEntry, input: DemographicsLayoutInput): String

    /**
     * Maps the sheet typing in the answer to [entry], showing [text] beneath [appBar].
     */
    fun mapEntrySheet(
        entry: DemographicsEntry,
        text: StateFlow<String>,
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
 * Default [DemographicsPickerMapper] implementation, mapping answers and measurements into text through
 * [answerMapper].
 */
class DemographicsPickerMapperImpl(
    private val answerMapper: DemographicsAnswerMapper,
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

    override fun mapEntryText(entry: DemographicsEntry, input: DemographicsLayoutInput): String {
        val response = input.response.value
        val text = when (entry) {
            DemographicsEntry.Height -> response.heightInCm?.let(answerMapper::mapEntryText)
            DemographicsEntry.Weight -> response.weightInKg?.let(answerMapper::mapEntryText)
            DemographicsEntry.NHSNumber -> response.nhsNumber?.stringValue
        }
        return text.orEmpty()
    }

    override fun mapEntrySheet(
        entry: DemographicsEntry,
        text: StateFlow<String>,
        appBar: StateFlow<GroveAppBar>,
        input: DemographicsLayoutInput,
    ) = DemographicsEntrySheet(
        appBar = appBar,
        label = StringResource(
            when (entry) {
                DemographicsEntry.Height, DemographicsEntry.Weight -> MHCStrings.demographics_measurement_value
                DemographicsEntry.NHSNumber -> MHCStrings.demographics_field_nhsnumber
            },
        ),
        unit = when (entry) {
            DemographicsEntry.Height -> StringResource(MHCStrings.demographics_unit_cm)
            DemographicsEntry.Weight -> StringResource(MHCStrings.demographics_unit_kg)
            DemographicsEntry.NHSNumber -> null
        },
        keyboardType = when (entry) {
            DemographicsEntry.Height, DemographicsEntry.Weight -> KeyboardType.Decimal
            DemographicsEntry.NHSNumber -> KeyboardType.Number
        },
        entry = text,
        help = if (entry == DemographicsEntry.NHSNumber) {
            DemographicsEntrySheet.Help(
                title = StringResource(MHCStrings.demographics_nhs_number_help),
                onClick = { input.actionSink.push(DemographicsAction.NHSNumberHelpSelected) },
            )
        } else {
            null
        },
        onEntryChange = { input.actionSink.push(DemographicsAction.EntryChanged(entry = it)) },
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
