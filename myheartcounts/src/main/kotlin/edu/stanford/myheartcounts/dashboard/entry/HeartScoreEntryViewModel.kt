//
// This source file is part of the My Heart Counts Android open-source project
//
// SPDX-FileCopyrightText: 2026 Stanford University and the project authors (see CONTRIBUTORS.md)
// SPDX-License-Identifier: MIT
//

package edu.stanford.myheartcounts.dashboard.entry

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import edu.stanford.myheartcounts.MHCStrings
import edu.stanford.myheartcounts.account.preferredMeasurementSystem
import edu.stanford.myheartcounts.dashboard.HeartScoreComponent
import edu.stanford.myheartcounts.firebase.FirebaseSchema
import edu.stanford.myheartcounts.navigation.MHCRoute
import edu.stanford.myheartcounts.navigation.NavigationEvent
import edu.stanford.myheartcounts.navigation.Navigator
import edu.stanford.myheartcounts.standard.MHCEnvironmentTracker
import edu.stanford.myheartcounts.standard.health.HealthAccess
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import org.grovealliance.account.Account
import org.grovealliance.core.coroutines.runCatchingAsync
import org.grovealliance.core.time.TimeProvider
import org.grovealliance.ui.ActionSource
import org.grovealliance.ui.StringResource
import org.grovealliance.ui.mutableScaffoldState
import java.time.LocalDateTime
import java.util.Locale
import java.util.UUID

/**
 * Keeps a manual measurement editable across recreation and closes only after a successful save.
 */
@Suppress("detekt:LongParameterList")
class HeartScoreEntryViewModel(
    private val savedStateHandle: SavedStateHandle,
    private val navigator: Navigator,
    private val writer: HeartScoreEntryWriter,
    private val healthAccess: HealthAccess,
    private val account: Account,
    private val timeProvider: TimeProvider,
    mapper: HeartScoreEntryContentMapper,
) : ViewModel() {
    private val component = HeartScoreComponent.valueOf(requireNotNull(savedStateHandle.get<String>(KEY_COMPONENT)))
    private val actions = ActionSource(::onAction)
    private val sink = actions.sink<HeartScoreEntryAction>()
    private val state = MutableStateFlow(state(form = restoredForm()))
    private val mapped = mapper.map(
        input = HeartScoreEntryContentInput(
            state = state.asStateFlow(),
            scope = viewModelScope,
            actionSink = sink,
        ),
    )
    private val scaffold = mutableScaffoldState(appBar = mapped.appBar)

    val screen = HeartScoreEntryScreenContent(
        scaffold = scaffold.asScaffoldState(),
        layout = mapped.layout,
    )

    private fun restoredForm(): HeartScoreEntryForm {
        val locale = Locale.getDefault()
        val system = measurementSystem(locale = locale)
        // Weight is entered in pounds in both the US and UK systems, height in feet and inches only in the US system.
        val heightInFeet = system == FirebaseSchema.MeasurementSystem.US
        val id = savedStateHandle.get<String>("entryId") ?: UUID.randomUUID().toString().also { savedStateHandle["entryId"] = it }
        return HeartScoreEntryForm(
            component = component,
            date = savedStateHandle.get<String>("entryDate")?.let { LocalDateTime.parse(it) }
                ?: timeProvider.nowInstant().atZone(timeProvider.currentZone()).toLocalDateTime(),
            weightInPounds = system == FirebaseSchema.MeasurementSystem.US || system == FirebaseSchema.MeasurementSystem.UK,
            heightInFeet = heightInFeet,
            locale = locale,
            zone = timeProvider.currentZone(),
            id = id,
            value = savedStateHandle["value"] ?: "",
            diastolic = savedStateHandle["diastolic"] ?: "",
            weight = savedStateHandle["weight"] ?: "",
            height = savedStateHandle["height"] ?: if (heightInFeet) "5" else "",
            inches = savedStateHandle["inches"] ?: "4",
            directBmi = savedStateHandle["directBmi"] ?: false,
        ).also { savedStateHandle["entryDate"] = it.date.toString() }
    }

    private fun measurementSystem(locale: Locale): String =
        account.details.value?.preferredMeasurementSystem ?: MHCEnvironmentTracker.measurementSystem(locale = locale)

    private fun needsNativeWrite(entry: HeartScoreEntry): Boolean = when (entry) {
        is HeartScoreEntry.Quantity -> entry.component == HeartScoreComponent.BloodGlucose
        is HeartScoreEntry.BodyMassIndex -> entry.heightMeters != null
        is HeartScoreEntry.Pressure -> true
    }

    private fun onAction(action: HeartScoreEntryAction) {
        if (state.value.saving) return
        val form = state.value.form
        when (action) {
            is HeartScoreEntryAction.FieldChanged -> {
                if (form.acceptsWholeNumbersOnly(field = action.field) && !action.value.all { it.isDigit() }) return
                val form = when (action.field) {
                    HeartScoreEntryAction.Field.Value -> form.copy(value = action.value)
                    HeartScoreEntryAction.Field.Diastolic -> form.copy(diastolic = action.value)
                    HeartScoreEntryAction.Field.Weight -> form.copy(weight = action.value)
                    HeartScoreEntryAction.Field.Height -> form.copy(height = action.value)
                    HeartScoreEntryAction.Field.Inches -> form.copy(inches = action.value)
                }
                update(form = form)
            }
            is HeartScoreEntryAction.DateChanged -> update(form = form.copy(date = action.value))
            is HeartScoreEntryAction.ModeChanged -> update(form = form.copy(directBmi = action.direct))
            HeartScoreEntryAction.Save -> save()
            HeartScoreEntryAction.Close -> navigator.push(
                event = NavigationEvent.Close(route = MHCRoute.HeartScoreEntry(component = component)),
            )
        }
    }

    private fun update(form: HeartScoreEntryForm) {
        savedStateHandle["entryDate"] = form.date.toString()
        savedStateHandle["value"] = form.value
        savedStateHandle["diastolic"] = form.diastolic
        savedStateHandle["weight"] = form.weight
        savedStateHandle["height"] = form.height
        savedStateHandle["inches"] = form.inches
        savedStateHandle["directBmi"] = form.directBmi
        state.value = state(form = form)
    }

    private fun state(form: HeartScoreEntryForm): HeartScoreEntryState {
        val entry = form.entry()
        return HeartScoreEntryState(
            form = form,
            canSave = entry != null && (!needsNativeWrite(entry = entry) || entry.date <= timeProvider.nowInstant()),
            error = if (entry != null && needsNativeWrite(entry = entry) && entry.date > timeProvider.nowInstant()) {
                StringResource(MHCStrings.heart_entry_future_time)
            } else {
                null
            },
            bmi = (entry as? HeartScoreEntry.BodyMassIndex)?.value,
        )
    }

    private fun save() {
        if (!state.value.canSave) return
        val entry = state.value.form.entry() ?: return
        state.value = state.value.copy(saving = true, error = null)
        viewModelScope.launch {
            runCatchingAsync {
                if (needsNativeWrite(entry = entry)) healthAccess.requestPermissions()
                writer.save(entry = entry).getOrThrow()
            }.fold(
                onSuccess = { navigator.push(event = NavigationEvent.Close(route = MHCRoute.HeartScoreEntry(component = component))) },
                onFailure = {
                    state.value = state.value.copy(
                        saving = false,
                        error = StringResource(
                            if (it is SecurityException) MHCStrings.heart_entry_permission_error else MHCStrings.heart_entry_error,
                        ),
                    )
                },
            )
        }
    }

    companion object {
        const val KEY_COMPONENT = "component"
    }
}
