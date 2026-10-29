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
import edu.stanford.myheartcounts.demographics.ui.DemographicsLayout
import edu.stanford.myheartcounts.demographics.ui.DemographicsScreenMode
import edu.stanford.myheartcounts.model.demographics.BiologicalSex
import edu.stanford.myheartcounts.model.demographics.Comorbidities
import edu.stanford.myheartcounts.model.demographics.MHCGenderIdentity
import edu.stanford.myheartcounts.navigation.FakeNavigator
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.setMain
import org.grovealliance.core.time.FakeTimeProvider
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
        viewModel.screen.layout.healthImport.onClick()

        // then
        assertThat(editor.state.value.response.heightInCm).isEqualTo(170.0)
        assertThat(editor.state.value.response.weightInKg).isEqualTo(82.5)
    }

    @Test
    fun `a health import without measurements says so`() {
        // given
        val viewModel = viewModel()

        // when
        viewModel.screen.layout.healthImport.onClick()

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
        viewModel.screen.layout.healthImport.onClick()

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
            rules = DemographicsRules(),
        ),
        pickerMapper = DemographicsPickerMapperImpl(
            answerMapper = answerMapper,
            timeProvider = timeProvider,
        ),
        choiceSheetMapper = DemographicsChoiceSheetMapperImpl(
            catalog = catalog,
            optionIdMapper = optionIdMapper,
            groupMapper = DemographicsChoiceGroupMapperImpl(catalog = catalog),
            rules = DemographicsRules(),
            timeProvider = timeProvider,
        ),
        editor = editor,
        autofill = autofill,
        navigator = navigator,
    )

    private fun DemographicsLayout.fields() = sections.flatMap { section -> section.rows.map { it.field } }

    private fun DemographicsLayout.row(field: DemographicsField) = sections.flatMap { it.rows }.single { it.field == field }
}
