//
// This source file is part of the My Heart Counts Android open-source project
//
// SPDX-FileCopyrightText: 2026 Stanford University and the project authors (see CONTRIBUTORS.md)
//
// SPDX-License-Identifier: MIT

package edu.stanford.myheartcounts.comorbidities

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import edu.stanford.myheartcounts.comorbidities.ui.ComorbiditiesScreenContent
import edu.stanford.myheartcounts.model.demographics.Comorbidities
import edu.stanford.myheartcounts.navigation.MHCRoute
import edu.stanford.myheartcounts.navigation.NavigationEvent
import edu.stanford.myheartcounts.navigation.Navigator
import edu.stanford.myheartcounts.ui.choice.MonthYearPicker
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import org.grovealliance.ui.ActionSource

/**
 * Coordinates the comorbidities screen: edits the participant's stored comorbidities and stores
 * them once the screen goes away, if they changed.
 */
class ComorbiditiesViewModel(
    private val mapper: ComorbiditiesMapper,
    private val rules: ComorbiditiesRules,
    private val storage: ComorbiditiesStorage,
    private val navigator: Navigator,
) : ViewModel() {

    private val actionSource = ActionSource(::onAction)
    private val stored = storage.currentComorbidities
    private val comorbidities = MutableStateFlow(stored)
    private val diagnosisDatePicker = MutableStateFlow<MonthYearPicker?>(null)
    private val input = ComorbiditiesInput(
        comorbidities = comorbidities.asStateFlow(),
        diagnosisDatePicker = diagnosisDatePicker.asStateFlow(),
        scope = viewModelScope,
        actionSink = actionSource.sink(),
    )

    /**
     * The comorbidities screen.
     */
    val screen: ComorbiditiesScreenContent = mapper.map(input = input)

    public override fun onCleared() {
        val edited = comorbidities.value
        if (edited != null && edited != stored) storage.storeSoon(comorbidities = edited)
    }

    private fun onAction(action: ComorbiditiesAction) {
        when (action) {
            is ComorbiditiesAction.ConditionSelected -> comorbidities.value = rules.toggled(
                current = comorbidities.value,
                condition = action.condition,
            )
            ComorbiditiesAction.NoneSelected -> comorbidities.value = Comorbidities(entries = emptyMap())
            is ComorbiditiesAction.DiagnosisDateSelected -> diagnosisDatePicker.value = mapper.mapDiagnosisDatePicker(
                condition = action.condition,
                input = input,
            )
            is ComorbiditiesAction.DiagnosisDateChanged -> {
                comorbidities.value = rules.diagnosed(
                    current = comorbidities.value,
                    condition = action.condition,
                    date = action.date,
                )
                diagnosisDatePicker.value = null
            }
            ComorbiditiesAction.DiagnosisDatePickerDismissed -> diagnosisDatePicker.value = null
            ComorbiditiesAction.BackSelected -> navigator.push(event = NavigationEvent.Close(route = MHCRoute.Comorbidities))
        }
    }
}
