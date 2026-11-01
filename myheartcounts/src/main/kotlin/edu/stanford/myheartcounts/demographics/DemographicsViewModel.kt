//
// This source file is part of the My Heart Counts Android open-source project
//
// SPDX-FileCopyrightText: 2026 Stanford University and the project authors (see CONTRIBUTORS.md)
//
// SPDX-License-Identifier: MIT

package edu.stanford.myheartcounts.demographics

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import edu.stanford.myheartcounts.MHCStrings
import edu.stanford.myheartcounts.demographics.ui.DemographicsChoiceSheet
import edu.stanford.myheartcounts.demographics.ui.DemographicsDatePicker
import edu.stanford.myheartcounts.demographics.ui.DemographicsMenu
import edu.stanford.myheartcounts.demographics.ui.DemographicsMonthYearPicker
import edu.stanford.myheartcounts.demographics.ui.DemographicsScreenContent
import edu.stanford.myheartcounts.demographics.ui.DemographicsScreenMode
import edu.stanford.myheartcounts.model.demographics.Comorbidities
import edu.stanford.myheartcounts.model.demographics.NHSNumber
import edu.stanford.myheartcounts.navigation.MHCRoute
import edu.stanford.myheartcounts.navigation.NavigationEvent
import edu.stanford.myheartcounts.navigation.Navigator
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import org.grovealliance.core.logging.groveLogger
import org.grovealliance.ui.ActionSource
import org.grovealliance.ui.GroveAppBar
import org.grovealliance.ui.StringResource
import org.grovealliance.ui.groveAppBar
import org.grovealliance.ui.mutableScaffoldState

/**
 * Coordinates the demographics questions: builds the screen once, in the [DemographicsScreenMode]
 * passed in [savedStateHandle] under [KEY_MODE], edits the participant's answers through the
 * [editor] and carries out what the layout offers. Answers not stored yet are stored when the
 * screen goes away.
 */
@Suppress("detekt:LongParameterList")
class DemographicsViewModel(
    savedStateHandle: SavedStateHandle,
    layoutMapper: DemographicsLayoutMapper,
    private val pickerMapper: DemographicsPickerMapper,
    private val choiceSheetMapper: DemographicsChoiceSheetMapper,
    private val editor: DemographicsEditor,
    private val autofill: DemographicsAutofill,
    private val navigator: Navigator,
) : ViewModel() {

    private val logger by groveLogger()
    private val actionSource = ActionSource(::onAction)
    private val menu = MutableStateFlow<DemographicsMenu?>(null)
    private val datePicker = MutableStateFlow<DemographicsDatePicker?>(null)
    private val monthYearPicker = MutableStateFlow<DemographicsMonthYearPicker?>(null)
    private val scaffoldState = mutableScaffoldState()
    private var entry: DemographicsEntry? = null
    private val entryText = MutableStateFlow("")
    private val entryAppBar = MutableStateFlow(GroveAppBar.Empty)
    private val input = DemographicsLayoutInput(
        region = editor.state.value.region,
        response = editor.state
            .map { it.response }
            .stateIn(
                scope = viewModelScope,
                started = SharingStarted.Eagerly,
                initialValue = editor.state.value.response,
            ),
        completion = editor.completion,
        menu = menu.asStateFlow(),
        datePicker = datePicker.asStateFlow(),
        monthYearPicker = monthYearPicker.asStateFlow(),
        scaffoldState = scaffoldState.asScaffoldState(),
        scope = viewModelScope,
        actionSink = actionSource.sink(),
    )

    /**
     * The demographics questions, beneath an app bar closing them in [DemographicsScreenMode.Complete]
     * and without one in [DemographicsScreenMode.Embedded].
     */
    val screen = DemographicsScreenContent(
        appBar = when (DemographicsScreenMode.valueOf(requireNotNull(savedStateHandle.get<String>(KEY_MODE)))) {
            DemographicsScreenMode.Complete -> groveAppBar {
                back { navigator.push(event = NavigationEvent.Close(route = MHCRoute.Demographics)) }
                title(MHCStrings.demographics_title)
            }
            DemographicsScreenMode.Embedded -> null
        },
        layout = layoutMapper.map(input = input),
    )

    init {
        viewModelScope.launch {
            editor.saveFailures.collect {
                scaffoldState.showToast(message = StringResource(MHCStrings.demographics_save_failed))
            }
        }
    }

    public override fun onCleared() {
        editor.flushSoon()
    }

    @Suppress("detekt:CyclomaticComplexMethod")
    private fun onAction(action: DemographicsAction) {
        when (action) {
            DemographicsAction.HealthImportSelected -> importFromHealth()
            is DemographicsAction.QuestionSelected -> openQuestion(field = action.field)
            DemographicsAction.MenuDismissed -> menu.value = null
            DemographicsAction.DatePickerDismissed -> datePicker.value = null
            is DemographicsAction.EntryChanged -> changeEntry(text = action.entry)
            DemographicsAction.EntrySaved -> saveEntry()
            DemographicsAction.NHSNumberHelpSelected -> navigator.push(
                event = NavigationEvent.OpenUrl(url = NHS_NUMBER_HELP_URL),
            )
            is DemographicsAction.UKNationSelected -> scaffoldState.showBottomSheet(
                sheet = choiceSheetMapper.mapUKCountySheet(
                    nation = action.nation,
                    input = input,
                ),
            )
            DemographicsAction.SheetClosed -> scaffoldState.dismissBottomSheet()
            is DemographicsAction.DiagnosisDateSelected -> {
                monthYearPicker.value = choiceSheetMapper.mapDiagnosisDatePicker(
                    condition = action.condition,
                    input = input,
                )
            }
            DemographicsAction.MonthYearPickerDismissed -> monthYearPicker.value = null
            is DemographicsAction.ResponseChanged -> {
                editor.update { it.updated(change = action) }
                menu.value = null
                datePicker.value = null
                monthYearPicker.value = null
            }
        }
    }

    private fun importFromHealth() {
        viewModelScope.launch {
            autofill.read()
                .onSuccess { measurements ->
                    if (measurements.heightInCm == null && measurements.weightInKg == null) {
                        scaffoldState.showToast(message = StringResource(MHCStrings.demographics_health_import_empty))
                    } else {
                        editor.update { response ->
                            response.copy(
                                heightInCm = measurements.heightInCm ?: response.heightInCm,
                                weightInKg = measurements.weightInKg ?: response.weightInKg,
                            )
                        }
                    }
                }
                .onFailure { throwable ->
                    logger.e(throwable) { "Reading demographics from Health Connect failed." }
                    scaffoldState.showToast(message = StringResource(MHCStrings.demographics_health_import_failed))
                }
        }
    }

    @Suppress("detekt:CyclomaticComplexMethod")
    private fun openQuestion(field: DemographicsField) {
        when (field) {
            DemographicsField.DateOfBirth -> datePicker.value = pickerMapper.mapDateOfBirthPicker(input = input)
            DemographicsField.GenderIdentity -> menu.value = pickerMapper.mapGenderIdentityMenu(input = input)
            DemographicsField.SexAtBirth -> menu.value = pickerMapper.mapBiologicalSexAtBirthMenu(input = input)
            DemographicsField.BloodType -> menu.value = pickerMapper.mapBloodTypeMenu(input = input)
            DemographicsField.Height -> openEntry(entry = DemographicsEntry.Height)
            DemographicsField.Weight -> openEntry(entry = DemographicsEntry.Weight)
            DemographicsField.NHSNumber -> openEntry(entry = DemographicsEntry.NHSNumber)
            DemographicsField.RaceEthnicity -> showSheet(sheet = choiceSheetMapper.mapRaceEthnicitySheet(input = input))
            DemographicsField.LatinoStatus -> showSheet(sheet = choiceSheetMapper.mapLatinoStatusSheet(input = input))
            DemographicsField.Comorbidities -> openComorbidities()
            DemographicsField.USRegion, DemographicsField.UKRegion ->
                showSheet(sheet = choiceSheetMapper.mapRegionSheet(input = input))
            DemographicsField.USEducation, DemographicsField.UKEducation ->
                showSheet(sheet = choiceSheetMapper.mapEducationSheet(input = input))
            DemographicsField.USIncome, DemographicsField.UKIncome ->
                showSheet(sheet = choiceSheetMapper.mapIncomeSheet(input = input))
            DemographicsField.StageOfChange -> showSheet(sheet = choiceSheetMapper.mapStageOfChangeSheet(input = input))
            DemographicsField.ReferralSource -> showSheet(sheet = choiceSheetMapper.mapReferralSourceSheet(input = input))
        }
    }

    // Looking through the conditions without choosing any answers that there are none.
    private fun openComorbidities() {
        if (input.response.value.comorbidities == null) {
            editor.update { it.copy(comorbidities = Comorbidities(entries = emptyMap())) }
        }
        showSheet(sheet = choiceSheetMapper.mapComorbiditiesSheet(input = input))
    }

    private fun showSheet(sheet: DemographicsChoiceSheet) {
        scaffoldState.showBottomSheet(sheet = sheet)
    }

    private fun openEntry(entry: DemographicsEntry) {
        this.entry = entry
        entryText.value = pickerMapper.mapEntryText(
            entry = entry,
            input = input,
        )
        entryAppBar.value = savableAppBar(canSave = canSave(entry = entry, text = entryText.value))
        scaffoldState.showBottomSheet(
            sheet = pickerMapper.mapEntrySheet(
                entry = entry,
                text = entryText.asStateFlow(),
                appBar = entryAppBar.asStateFlow(),
                input = input,
            ),
        )
    }

    private fun changeEntry(text: String) {
        val entry = entry ?: return
        val couldSave = canSave(entry = entry, text = entryText.value)
        val canSave = canSave(entry = entry, text = text)
        entryText.value = text
        if (couldSave != canSave) entryAppBar.value = savableAppBar(canSave = canSave)
    }

    private fun saveEntry() {
        val text = entryText.value
        when (entry) {
            DemographicsEntry.Height -> parsedMeasurement(text = text)?.let {
                onAction(action = DemographicsAction.HeightChanged(value = it))
            }
            DemographicsEntry.Weight -> parsedMeasurement(text = text)?.let {
                onAction(action = DemographicsAction.WeightChanged(value = it))
            }
            DemographicsEntry.NHSNumber -> if (text.isBlank()) {
                onAction(action = DemographicsAction.NHSNumberChanged(value = null))
            } else {
                NHSNumber.validating(input = text)?.let {
                    onAction(action = DemographicsAction.NHSNumberChanged(value = it))
                }
            }
            null -> Unit
        }
        scaffoldState.dismissBottomSheet()
    }

    private fun canSave(entry: DemographicsEntry, text: String) = when (entry) {
        DemographicsEntry.Height, DemographicsEntry.Weight -> parsedMeasurement(text = text) != null
        DemographicsEntry.NHSNumber -> text.isBlank() || NHSNumber.validating(input = text) != null
    }

    private fun savableAppBar(canSave: Boolean) = entry
        ?.let {
            pickerMapper.mapEntryAppBar(
                entry = it,
                canSave = canSave,
                input = input,
            )
        }
        ?: GroveAppBar.Empty

    private fun parsedMeasurement(text: String) = text
        .replace(oldChar = ',', newChar = '.')
        .toDoubleOrNull()
        ?.takeIf { it > 0 }

    @Suppress("detekt:CyclomaticComplexMethod")
    private fun DemographicsResponse.updated(change: DemographicsAction.ResponseChanged) = when (change) {
        is DemographicsAction.DateOfBirthChanged -> copy(dateOfBirth = change.value)
        is DemographicsAction.GenderIdentityChanged -> copy(genderIdentity = change.value)
        is DemographicsAction.BiologicalSexAtBirthChanged -> copy(biologicalSexAtBirth = change.value)
        is DemographicsAction.BloodTypeChanged -> copy(bloodType = change.value)
        is DemographicsAction.HeightChanged -> copy(heightInCm = change.value)
        is DemographicsAction.WeightChanged -> copy(weightInKg = change.value)
        is DemographicsAction.RaceEthnicityChanged -> copy(raceEthnicity = change.value)
        is DemographicsAction.LatinoStatusChanged -> copy(latinoStatus = change.value)
        is DemographicsAction.ComorbiditiesChanged -> copy(comorbidities = change.value)
        is DemographicsAction.USRegionChanged -> copy(usRegion = change.value)
        is DemographicsAction.UKRegionChanged -> copy(ukRegion = change.value)
        is DemographicsAction.EducationUSChanged -> copy(educationUS = change.value)
        is DemographicsAction.EducationUKChanged -> copy(educationUK = change.value)
        is DemographicsAction.HouseholdIncomeUSChanged -> copy(householdIncomeUS = change.value)
        is DemographicsAction.HouseholdIncomeUKChanged -> copy(householdIncomeUK = change.value)
        is DemographicsAction.NHSNumberChanged -> copy(nhsNumber = change.value)
        is DemographicsAction.StageOfChangeChanged -> copy(stageOfChange = change.value)
        is DemographicsAction.ReferralSourceChanged -> copy(referralSource = change.value)
    }

    companion object {
        /**
         * The key the [DemographicsScreenMode] is passed under, as its name.
         */
        const val KEY_MODE = "mode"

        private const val NHS_NUMBER_HELP_URL = "https://www.nhs.uk/nhs-services/online-services/find-nhs-number/"
    }
}
