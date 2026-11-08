//
// This source file is part of the My Heart Counts Android open-source project
//
// SPDX-FileCopyrightText: 2026 Stanford University and the project authors (see CONTRIBUTORS.md)
//
// SPDX-License-Identifier: MIT

package edu.stanford.myheartcounts.comorbidities

import com.google.common.truth.Truth.assertThat
import edu.stanford.myheartcounts.MHCStrings
import edu.stanford.myheartcounts.model.demographics.Comorbidities
import edu.stanford.myheartcounts.navigation.FakeNavigator
import edu.stanford.myheartcounts.ui.choice.ChoiceRow
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
class ComorbiditiesViewModelTest {
    private val dispatcher = UnconfinedTestDispatcher()
    private val storage = FakeComorbiditiesStorage()
    private val navigator = FakeNavigator()
    private val timeProvider = FakeTimeProvider()
    private val none = StringResource(MHCStrings.comorbidities_none)
    private val diabetes = StringResource(MHCStrings.comorbidities_condition_diabetes)

    @Before
    fun setup() {
        Dispatchers.setMain(dispatcher = dispatcher)
        timeProvider.setZone(zone = ZoneOffset.UTC)
        timeProvider.setNow(instant = LocalDate.of(2026, 1, 1).atStartOfDay(ZoneOffset.UTC).toInstant())
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun `opens on the stored comorbidities`() {
        // given
        storage.setCurrentComorbidities(comorbidities = Comorbidities(entries = mapOf("diabetes" to "2020-03")))

        // when
        val viewModel = viewModel()

        // then
        assertThat(viewModel.condition(title = diabetes).isSelected).isTrue()
        assertThat(viewModel.condition(title = diabetes).followUp?.isSet).isTrue()
        assertThat(viewModel.titles()).doesNotContain(none)
    }

    @Test
    fun `conditions carry their explanation`() {
        // when
        val viewModel = viewModel()

        // then
        assertThat(viewModel.condition(title = StringResource(MHCStrings.comorbidities_condition_chronickidneydisease)).subtitle)
            .isEqualTo(StringResource(MHCStrings.comorbidities_condition_chronickidneydisease_detail))
    }

    @Test
    fun `offers none unchosen while nothing was reported`() {
        // when
        val viewModel = viewModel()

        // then
        assertThat(viewModel.condition(title = none).isSelected).isFalse()
        assertThat(viewModel.screen.sections.value.flatMap { it.conditions }.none { it.isSelected }).isTrue()
    }

    @Test
    fun `choosing none marks it chosen`() {
        // given
        val viewModel = viewModel()

        // when
        viewModel.condition(title = none).onClick()

        // then
        assertThat(viewModel.condition(title = none).isSelected).isTrue()
    }

    @Test
    fun `tapping a condition chooses it and tapping it again unchooses it`() {
        // given
        val viewModel = viewModel()

        // when
        viewModel.condition(title = diabetes).onClick()
        val chosen = viewModel.condition(title = diabetes).isSelected
        val titlesWhileChosen = viewModel.titles()
        viewModel.condition(title = diabetes).onClick()

        // then
        assertThat(chosen).isTrue()
        assertThat(titlesWhileChosen).doesNotContain(none)
        assertThat(viewModel.condition(title = diabetes).isSelected).isFalse()
    }

    @Test
    fun `a chosen diagnosis date is shown and closes the picker`() {
        // given
        storage.setCurrentComorbidities(comorbidities = Comorbidities(entries = mapOf("diabetes" to "")))
        val viewModel = viewModel()
        viewModel.condition(title = diabetes).followUp?.onClick?.invoke()
        val picker = viewModel.screen.diagnosisDatePicker.value

        // when
        picker?.onSelect(2019, null)

        // then
        assertThat(picker?.title).isEqualTo(diabetes)
        assertThat(viewModel.condition(title = diabetes).followUp?.value).isEqualTo(StringResource("2019"))
        assertThat(viewModel.screen.diagnosisDatePicker.value).isNull()
    }

    @Test
    fun `an unknown diagnosis date is cleared`() {
        // given
        storage.setCurrentComorbidities(comorbidities = Comorbidities(entries = mapOf("diabetes" to "2020-03")))
        val viewModel = viewModel()
        viewModel.condition(title = diabetes).followUp?.onClick?.invoke()

        // when
        viewModel.screen.diagnosisDatePicker.value?.onClear?.invoke()
        viewModel.onCleared()

        // then
        assertThat(storage.stored).containsExactly(Comorbidities(entries = mapOf("diabetes" to "")))
    }

    @Test
    fun `dismissing the picker keeps the diagnosis date`() {
        // given
        storage.setCurrentComorbidities(comorbidities = Comorbidities(entries = mapOf("diabetes" to "2020-03")))
        val viewModel = viewModel()
        viewModel.condition(title = diabetes).followUp?.onClick?.invoke()

        // when
        viewModel.screen.diagnosisDatePicker.value?.onDismiss?.invoke()
        viewModel.onCleared()

        // then
        assertThat(viewModel.screen.diagnosisDatePicker.value).isNull()
        assertThat(storage.stored).isEmpty()
    }

    @Test
    fun `leaving the screen stores changed comorbidities`() {
        // given
        val viewModel = viewModel()
        viewModel.condition(title = diabetes).onClick()

        // when
        viewModel.onCleared()

        // then
        assertThat(storage.stored).containsExactly(Comorbidities(entries = mapOf("diabetes" to "")))
    }

    @Test
    fun `leaving the screen unchanged stores nothing`() {
        // given
        storage.setCurrentComorbidities(comorbidities = Comorbidities(entries = mapOf("diabetes" to "2020")))
        val viewModel = viewModel()

        // when
        viewModel.onCleared()

        // then
        assertThat(storage.stored).isEmpty()
    }

    private fun viewModel() = ComorbiditiesViewModel(
        mapper = ComorbiditiesMapperImpl(
            conditionsProvider = ComorbiditiesConditionsProviderImpl(),
            diagnosisDateMapper = DiagnosisDateMapperImpl(
                conditionsProvider = ComorbiditiesConditionsProviderImpl(),
                timeProvider = timeProvider,
            ),
            rules = ComorbiditiesRules(),
        ),
        rules = ComorbiditiesRules(),
        storage = storage,
        navigator = navigator,
    )

    private fun ComorbiditiesViewModel.titles() = screen.sections.value.flatMap { section -> section.conditions.map { it.title } }

    private fun ComorbiditiesViewModel.condition(title: StringResource): ChoiceRow =
        screen.sections.value.flatMap { it.conditions }.single { it.title == title }
}
