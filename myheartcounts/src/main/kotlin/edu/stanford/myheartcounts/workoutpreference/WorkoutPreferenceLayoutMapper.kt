//
// This source file is part of the My Heart Counts Android open-source project
//
// SPDX-FileCopyrightText: 2026 Stanford University and the project authors (see CONTRIBUTORS.md)
//
// SPDX-License-Identifier: MIT

package edu.stanford.myheartcounts.workoutpreference

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.DirectionsBike
import androidx.compose.material.icons.automirrored.filled.DirectionsRun
import androidx.compose.material.icons.automirrored.filled.DirectionsWalk
import androidx.compose.material.icons.filled.EmojiPeople
import androidx.compose.material.icons.filled.FitnessCenter
import androidx.compose.material.icons.filled.Pool
import androidx.compose.material.icons.filled.SelfImprovement
import androidx.compose.material.icons.filled.SportsGymnastics
import androidx.compose.material.icons.filled.SportsSoccer
import androidx.compose.ui.graphics.vector.ImageVector
import edu.stanford.myheartcounts.MHCStrings
import edu.stanford.myheartcounts.model.workout.NotificationTime
import edu.stanford.myheartcounts.model.workout.WorkoutType
import edu.stanford.myheartcounts.ui.AppBarTextAction
import edu.stanford.myheartcounts.ui.ValueBadge
import edu.stanford.myheartcounts.ui.choice.ChoiceRow
import edu.stanford.myheartcounts.ui.choice.ChoiceSection
import edu.stanford.myheartcounts.workoutpreference.ui.NotificationTimePicker
import edu.stanford.myheartcounts.workoutpreference.ui.NotificationTimeRow
import edu.stanford.myheartcounts.workoutpreference.ui.WorkoutPreferenceLayout
import edu.stanford.myheartcounts.workoutpreference.ui.WorkoutPreferenceScreenMode
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import org.grovealliance.ui.ActionSink
import org.grovealliance.ui.AsyncTextButton
import org.grovealliance.ui.GroveAppBar
import org.grovealliance.ui.ImageResource
import org.grovealliance.ui.StringResource
import org.grovealliance.ui.groveAppBar
import java.time.LocalTime
import java.time.format.DateTimeFormatter
import java.time.format.FormatStyle

/**
 * Builds the workout preference form and its app bar. Interactions are dispatched as
 * [WorkoutPreferenceAction]s.
 */
interface WorkoutPreferenceLayoutMapper {

    /**
     * The form showing [WorkoutPreferenceLayoutInput.preference] as it changes, with the time picker
     * open while [WorkoutPreferenceLayoutInput.isTimePickerOpen].
     */
    fun map(input: WorkoutPreferenceLayoutInput): WorkoutPreferenceLayout

    /**
     * The app bar: the title with a back button and, when standalone, a Save action enabled while
     * [WorkoutPreferenceLayoutInput.hasChanges].
     */
    fun mapAppBar(input: WorkoutPreferenceLayoutInput): GroveAppBar
}

/**
 * What the workout preference form is built from.
 *
 * @param mode how the form is presented, which decides whether it ends with Continue
 * @param preference the preference as currently edited
 * @param isTimePickerOpen whether the time picker is open
 * @param hasChanges whether the preference differs from the one the screen was displayed with
 * @param scope the scope the form's state and its button run in
 * @param actionSink receives the form's interactions
 * @param onConfirm called when the participant confirms the preference, with Continue or Save
 * @param onDismiss called when the participant goes back
 */
data class WorkoutPreferenceLayoutInput(
    val mode: WorkoutPreferenceScreenMode,
    val preference: StateFlow<WorkoutPreference>,
    val isTimePickerOpen: StateFlow<Boolean>,
    val hasChanges: StateFlow<Boolean>,
    val scope: CoroutineScope,
    val actionSink: ActionSink<WorkoutPreferenceAction>,
    val onConfirm: suspend () -> Unit,
    val onDismiss: () -> Unit,
)

/**
 * Default [WorkoutPreferenceLayoutMapper] implementation.
 */
class WorkoutPreferenceLayoutMapperImpl : WorkoutPreferenceLayoutMapper {

    override fun map(input: WorkoutPreferenceLayoutInput): WorkoutPreferenceLayout = WorkoutPreferenceLayout(
        introduction = StringResource(MHCStrings.workout_preference_text),
        workoutTypes = input.preference.stateMap(scope = input.scope) {
            workoutTypes(preference = it, input = input)
        },
        notificationTime = NotificationTimeRow(
            title = StringResource(MHCStrings.workout_preference_notification_time),
            subtitle = StringResource(MHCStrings.workout_preference_notification_time_subtitle),
            badge = input.preference.stateMap(scope = input.scope) {
                ValueBadge(
                    text = mapTime(time = it.notificationTime),
                    onClick = { input.actionSink.push(WorkoutPreferenceAction.TimePickerRequested) },
                )
            },
        ),
        timePicker = input.isTimePickerOpen
            .map { isOpen -> if (isOpen) timePicker(input = input) else null }
            .stateIn(
                scope = input.scope,
                started = SharingStarted.WhileSubscribed(),
                initialValue = null,
            ),
        continueButton = when (input.mode) {
            WorkoutPreferenceScreenMode.Step -> AsyncTextButton(
                title = StringResource(MHCStrings.workout_preference_continue),
                coroutineScope = { input.scope },
                action = { input.onConfirm() },
            )
            WorkoutPreferenceScreenMode.Standalone -> null
        },
    )

    override fun mapAppBar(input: WorkoutPreferenceLayoutInput) = groveAppBar {
        back { input.onDismiss() }
        title(MHCStrings.workout_preference_title)
        if (input.mode == WorkoutPreferenceScreenMode.Standalone) {
            action(
                content = AppBarTextAction(
                    title = StringResource(MHCStrings.workout_preference_save),
                    enabled = input.hasChanges,
                    action = input.onConfirm,
                ),
            )
        }
    }

    private fun timePicker(input: WorkoutPreferenceLayoutInput): NotificationTimePicker {
        val time = input.preference.value.notificationTime
        return NotificationTimePicker(
            selection = NotificationTimePicker.State(
                hour = time.hour,
                minute = time.minute,
            ),
            confirmTitle = StringResource(MHCStrings.workout_preference_time_confirm),
            dismissTitle = StringResource(MHCStrings.workout_preference_time_cancel),
            onSelect = { selected ->
                val selectedTime = NotificationTime(
                    hour = selected.hour,
                    minute = selected.minute,
                )
                input.actionSink.push(WorkoutPreferenceAction.TimeSelected(time = selectedTime))
            },
            onDismiss = { input.actionSink.push(WorkoutPreferenceAction.TimePickerDismissed) },
        )
    }

    private fun workoutTypes(preference: WorkoutPreference, input: WorkoutPreferenceLayoutInput) = ChoiceSection(
        header = StringResource(MHCStrings.workout_preference_types_header),
        options = WorkoutType.entries.map { type ->
            ChoiceRow(
                icon = ImageResource(image = icon(type = type)),
                title = StringResource(title(type = type)),
                subtitle = null,
                detail = null,
                isSelected = type in preference.workoutTypes,
                followUp = null,
                onClick = { input.actionSink.push(WorkoutPreferenceAction.TypeToggled(type = type)) },
            )
        },
        footer = null,
    )

    private fun mapTime(time: NotificationTime) = StringResource(
        DateTimeFormatter.ofLocalizedTime(FormatStyle.SHORT).format(
            LocalTime.of(time.hour, time.minute),
        ),
    )

    private fun icon(type: WorkoutType): ImageVector = when (type) {
        WorkoutType.WALK -> Icons.AutoMirrored.Filled.DirectionsWalk
        WorkoutType.RUN -> Icons.AutoMirrored.Filled.DirectionsRun
        WorkoutType.BICYCLE -> Icons.AutoMirrored.Filled.DirectionsBike
        WorkoutType.SWIM -> Icons.Default.Pool
        WorkoutType.STRENGTH -> Icons.Default.FitnessCenter
        WorkoutType.HIIT -> Icons.Default.SportsGymnastics
        WorkoutType.YOGA_PILATES -> Icons.Default.SelfImprovement
        WorkoutType.SPORT -> Icons.Default.SportsSoccer
        WorkoutType.OTHER -> Icons.Default.EmojiPeople
    }

    private fun title(type: WorkoutType): Int = when (type) {
        WorkoutType.WALK -> MHCStrings.workout_preference_type_walk
        WorkoutType.RUN -> MHCStrings.workout_preference_type_run
        WorkoutType.BICYCLE -> MHCStrings.workout_preference_type_bicycle
        WorkoutType.SWIM -> MHCStrings.workout_preference_type_swim
        WorkoutType.STRENGTH -> MHCStrings.workout_preference_type_strength
        WorkoutType.HIIT -> MHCStrings.workout_preference_type_hiit
        WorkoutType.YOGA_PILATES -> MHCStrings.workout_preference_type_yoga_pilates
        WorkoutType.SPORT -> MHCStrings.workout_preference_type_sport
        WorkoutType.OTHER -> MHCStrings.workout_preference_type_other
    }

    private fun <T> StateFlow<WorkoutPreference>.stateMap(
        scope: CoroutineScope,
        transform: (WorkoutPreference) -> T,
    ): StateFlow<T> = map(transform).stateIn(
        scope = scope,
        started = SharingStarted.WhileSubscribed(),
        initialValue = transform(value),
    )
}
