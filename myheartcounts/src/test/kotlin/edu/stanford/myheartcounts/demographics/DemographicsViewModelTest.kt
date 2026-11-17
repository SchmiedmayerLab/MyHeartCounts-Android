//
// This source file is part of the My Heart Counts Android open-source project
//
// SPDX-FileCopyrightText: 2026 Stanford University and the project authors (see CONTRIBUTORS.md)
//
// SPDX-License-Identifier: MIT

package edu.stanford.myheartcounts.demographics

import androidx.lifecycle.SavedStateHandle
import com.google.common.truth.Truth.assertThat
import edu.stanford.myheartcounts.MHCStrings
import edu.stanford.myheartcounts.comorbidities.ComorbiditiesConditionsProviderImpl
import edu.stanford.myheartcounts.comorbidities.ComorbiditiesRules
import edu.stanford.myheartcounts.comorbidities.DiagnosisDateMapperImpl
import edu.stanford.myheartcounts.demographics.ui.DemographicsEntrySheet
import edu.stanford.myheartcounts.demographics.ui.DemographicsLayout
import edu.stanford.myheartcounts.demographics.ui.DemographicsScreenMode
import edu.stanford.myheartcounts.model.demographics.BiologicalSex
import edu.stanford.myheartcounts.model.demographics.Comorbidities
import edu.stanford.myheartcounts.model.demographics.MHCGenderIdentity
import edu.stanford.myheartcounts.navigation.FakeNavigator
import edu.stanford.myheartcounts.standard.FakeMeasurementUnitsProvider
import edu.stanford.myheartcounts.standard.MeasurementUnits
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.setMain
import org.grovealliance.core.time.FakeTimeProvider
import org.grovealliance.ui.GroveAppBar
import org.grovealliance.ui.StringResource
import org.junit.After
import org.junit.Before
import org.junit.Test
import java.time.LocalDate
import java.time.ZoneOffset

@OptIn(ExperimentalCoroutinesApi::class)
class DemographicsViewModelTest {
    private val dispatcher = UnconfinedTestDispatcher()
    private val editor = FakeDemographicsEditor()
    private val autofill = FakeDemographicsAutofill()
    private val navigator = FakeNavigator()
    private val timeProvider = FakeTimeProvider()
    private val catalog = DemographicsCatalogFixtures.create()
    private val optionIdMapper = DemographicsOptionIdMapperImpl()
    private val answerMapper = DemographicsAnswerMapperImpl(optionIdMapper = optionIdMapper)
    private val unitsProvider = FakeMeasurementUnitsProvider()
    private val measurementMapper = DemographicsMeasurementMapperImpl(unitsProvider = unitsProvider)
    private val entryPickerMapper = DemographicsPickerMapperImpl(
        answerMapper = answerMapper,
        measurementMapper = measurementMapper,
        unitsProvider = unitsProvider,
        timeProvider = timeProvider,
    )
    private val pickerMapper = CapturingPickerMapper()

    @Before
    fun setup() {
        Dispatchers.setMain(dispatcher = dispatcher)
        timeProvider.setZone(zone = ZoneOffset.UTC)
        timeProvider.setNow(instant = LocalDate.of(2026, 1, 1).atStartOfDay(ZoneOffset.UTC).toInstant())
        editor.setState(state = DemographicsStateFixtures.create(response = DemographicsResponse.empty))
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun `the layout asks the questions of the editor's region`() {
        // given
        editor.setState(state = DemographicsStateFixtures.create(region = DemographicsRegion.UK))

        // when
        val viewModel = viewModel()

        // then
        assertThat(viewModel.screen.layout.fields()).contains(DemographicsField.NHSNumber)
        assertThat(viewModel.screen.layout.fields()).doesNotContain(DemographicsField.LatinoStatus)
    }

    @Test
    fun `rows follow the editor's answers`() {
        // given
        val viewModel = viewModel()
        val row = viewModel.screen.layout.row(field = DemographicsField.GenderIdentity)

        // when
        editor.update { it.copy(genderIdentity = MHCGenderIdentity.FEMALE) }

        // then
        assertThat(row.value.value.text).isEqualTo(StringResource(MHCStrings.demographics_gender_identity_2))
    }

    @Test
    fun `an answer chosen in a menu goes to the editor and closes the menu`() {
        // given
        val viewModel = viewModel()
        val row = viewModel.screen.layout.row(field = DemographicsField.SexAtBirth)
        row.onClick()
        val menu = row.menu.value

        // when
        menu?.onSelect(menu.options.single { it.label == StringResource(MHCStrings.demographics_sex_at_birth_2) }.index)

        // then
        assertThat(menu?.field).isEqualTo(DemographicsField.SexAtBirth)
        assertThat(editor.state.value.response.biologicalSexAtBirth).isEqualTo(BiologicalSex.FEMALE)
        assertThat(row.menu.value).isNull()
    }

    @Test
    fun `a date chosen for the date of birth goes to the editor`() {
        // given
        val viewModel = viewModel()
        viewModel.screen.layout.row(field = DemographicsField.DateOfBirth).onClick()

        // when
        viewModel.screen.layout.datePicker.value?.onSelect(LocalDate.of(1980, 5, 17))

        // then
        assertThat(editor.state.value.response.dateOfBirth).isEqualTo(LocalDate.of(1980, 5, 17))
        assertThat(viewModel.screen.layout.datePicker.value).isNull()
    }

    @Test
    fun `questions answered on a sheet open it`() {
        // given
        val viewModel = viewModel()

        // when
        viewModel.screen.layout.row(field = DemographicsField.USRegion).onClick()

        // then
        assertThat(viewModel.screen.layout.scaffoldState.widgets.value.bottomSheet).isNotNull()
    }

    @Test
    fun `a metric height and weight are stored as typed`() {
        // given
        val viewModel = viewModel()

        // when
        enter(
            viewModel = viewModel,
            field = DemographicsField.Height,
            texts = listOf("180.5"),
        )
        enter(
            viewModel = viewModel,
            field = DemographicsField.Weight,
            texts = listOf("81"),
        )

        // then
        assertThat(editor.state.value.response.heightInCm).isEqualTo(180.5)
        assertThat(editor.state.value.response.weightInKg).isEqualTo(81.0)
    }

    @Test
    fun `a height in feet and inches and a weight in pounds are stored in centimetres and kilograms`() {
        // given
        unitsProvider.setUnits(units = MeasurementUnits.US)
        val viewModel = viewModel()

        // when
        enter(
            viewModel = viewModel,
            field = DemographicsField.Height,
            texts = listOf("5", "8"),
        )
        enter(
            viewModel = viewModel,
            field = DemographicsField.Weight,
            texts = listOf("150"),
        )

        // then
        assertThat(editor.state.value.response.heightInCm).isWithin(TOLERANCE).of(172.72)
        assertThat(editor.state.value.response.weightInKg).isWithin(TOLERANCE).of(68.0388555)
    }

    @Test
    fun `an entry outside the accepted range is not saved`() {
        // given
        unitsProvider.setUnits(units = MeasurementUnits.UK)
        val viewModel = viewModel()

        // when
        enter(
            viewModel = viewModel,
            field = DemographicsField.Weight,
            texts = listOf("50"),
        )

        // then
        assertThat(editor.state.value.response.weightInKg).isNull()
    }

    @Test
    fun `opening the comorbidities answers that there are none`() {
        // given
        val viewModel = viewModel()

        // when
        viewModel.screen.layout.row(field = DemographicsField.Comorbidities).onClick()

        // then
        assertThat(editor.state.value.response.comorbidities).isEqualTo(Comorbidities(entries = emptyMap()))
    }

    @Test
    fun `opening the comorbidities keeps the chosen conditions`() {
        // given
        val chosen = Comorbidities(entries = mapOf("diabetes" to "2020"))
        editor.setState(
            state = DemographicsStateFixtures.create(
                response = DemographicsResponse.empty.copy(comorbidities = chosen),
            ),
        )
        val viewModel = viewModel()

        // when
        viewModel.screen.layout.row(field = DemographicsField.Comorbidities).onClick()

        // then
        assertThat(editor.state.value.response.comorbidities).isEqualTo(chosen)
    }

    @Test
    fun `the health import fills the found measurements and keeps the others`() {
        // given
        editor.setState(
            state = DemographicsStateFixtures.create(
                response = DemographicsResponse.empty.copy(
                    heightInCm = 170.0,
                    weightInKg = 70.0,
                ),
            ),
        )
        autofill.setResult(
            result = Result.success(
                DemographicsMeasurements(
                    heightInCm = null,
                    weightInKg = 82.5,
                ),
            ),
        )
        val viewModel = viewModel()

        // when
        viewModel.screen.layout.healthImport().onClick()

        // then
        assertThat(editor.state.value.response.heightInCm).isEqualTo(170.0)
        assertThat(editor.state.value.response.weightInKg).isEqualTo(82.5)
    }

    @Test
    fun `a health import without measurements says so`() {
        // given
        val viewModel = viewModel()

        // when
        viewModel.screen.layout.healthImport().onClick()

        // then
        assertThat(viewModel.screen.layout.scaffoldState.widgets.value.toast?.message)
            .isEqualTo(StringResource(MHCStrings.demographics_health_import_empty))
    }

    @Test
    fun `a failed health import says so`() {
        // given
        autofill.setResult(result = Result.failure(IllegalStateException("unavailable")))
        val viewModel = viewModel()

        // when
        viewModel.screen.layout.healthImport().onClick()

        // then
        assertThat(viewModel.screen.layout.scaffoldState.widgets.value.toast?.message)
            .isEqualTo(StringResource(MHCStrings.demographics_health_import_failed))
    }

    @Test
    fun `a failed save says so`() {
        // given
        val viewModel = viewModel()

        // when
        editor.emitSaveFailure()

        // then
        assertThat(viewModel.screen.layout.scaffoldState.widgets.value.toast?.message)
            .isEqualTo(StringResource(MHCStrings.demographics_save_failed))
    }

    @Test
    fun `leaving the screen stores the answers`() {
        // given
        val viewModel = viewModel()

        // when
        viewModel.onCleared()

        // then
        assertThat(editor.flushSoonCalls).isEqualTo(1)
    }

    private fun viewModel() = DemographicsViewModel(
        savedStateHandle = SavedStateHandle(
            mapOf(DemographicsViewModel.KEY_MODE to DemographicsScreenMode.Complete.name),
        ),
        layoutMapper = DemographicsLayoutMapperImpl(
            catalog = catalog,
            optionIdMapper = optionIdMapper,
            answerMapper = answerMapper,
            measurementMapper = measurementMapper,
            rules = DemographicsRules(),
        ),
        pickerMapper = pickerMapper,
        choiceSheetMapper = DemographicsChoiceSheetMapperImpl(
            catalog = catalog,
            optionIdMapper = optionIdMapper,
            groupMapper = DemographicsChoiceGroupMapperImpl(catalog = catalog),
            rules = DemographicsRules(),
            comorbiditiesRules = ComorbiditiesRules(),
            diagnosisDateMapper = DiagnosisDateMapperImpl(
                conditionsProvider = ComorbiditiesConditionsProviderImpl(),
                timeProvider = timeProvider,
            ),
        ),
        measurementMapper = measurementMapper,
        editor = editor,
        autofill = autofill,
        navigator = navigator,
    )

    private fun enter(viewModel: DemographicsViewModel, field: DemographicsField, texts: List<String>) {
        viewModel.screen.layout.row(field = field).onClick()
        val sheet = requireNotNull(pickerMapper.lastEntrySheet)
        texts.forEachIndexed { index, text -> sheet.onEntryChange(index, text) }
        requireNotNull(pickerMapper.lastInput).actionSink.push(DemographicsAction.EntrySaved)
    }

    private fun DemographicsLayout.fields() = questionSections().flatten().map { it.field }

    private fun DemographicsLayout.row(field: DemographicsField) = questionSections().flatten().single { it.field == field }

    private inner class CapturingPickerMapper : DemographicsPickerMapper by entryPickerMapper {
        var lastEntrySheet: DemographicsEntrySheet? = null
            private set
        var lastInput: DemographicsLayoutInput? = null
            private set

        override fun mapEntrySheet(
            entry: DemographicsEntry,
            text: StateFlow<List<String>>,
            appBar: StateFlow<GroveAppBar>,
            input: DemographicsLayoutInput,
        ): DemographicsEntrySheet {
            val sheet = entryPickerMapper.mapEntrySheet(
                entry = entry,
                text = text,
                appBar = appBar,
                input = input,
            )
            lastEntrySheet = sheet
            lastInput = input
            return sheet
        }
    }

    private companion object {
        const val TOLERANCE = 0.000001
    }
}
