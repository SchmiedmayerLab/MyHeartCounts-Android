//
// This source file is part of the My Heart Counts Android open-source project
//
// SPDX-FileCopyrightText: 2026 Stanford University and the project authors (see CONTRIBUTORS.md)
//
// SPDX-License-Identifier: MIT

package edu.stanford.myheartcounts.workoutpreference.ui

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.DirectionsBike
import androidx.compose.material.icons.automirrored.filled.DirectionsRun
import androidx.compose.material.icons.automirrored.filled.DirectionsWalk
import androidx.compose.material.icons.filled.FitnessCenter
import androidx.compose.material.icons.filled.Pool
import androidx.compose.material.icons.filled.SelfImprovement
import androidx.compose.material.icons.filled.SportsGymnastics
import edu.stanford.myheartcounts.MHCStrings
import edu.stanford.myheartcounts.ui.AppBarTextAction
import edu.stanford.myheartcounts.ui.ValueBadge
import edu.stanford.myheartcounts.ui.choice.ChoiceRow
import edu.stanford.myheartcounts.ui.choice.ChoiceSection
import kotlinx.coroutines.flow.MutableStateFlow
import org.grovealliance.testing.screenshot.ScreenshotTest
import org.grovealliance.ui.AsyncTextButton
import org.grovealliance.ui.GroveScaffoldState
import org.grovealliance.ui.ImageResource
import org.grovealliance.ui.StringResource
import org.grovealliance.ui.groveAppBar
import org.junit.Test

class WorkoutPreferenceScreenContentScreenshotTest : ScreenshotTest() {
    private val option = ChoiceRow(
        icon = ImageResource(image = Icons.AutoMirrored.Filled.DirectionsWalk),
        title = StringResource(MHCStrings.workout_preference_type_walk),
        subtitle = null,
        detail = null,
        isSelected = false,
        followUp = null,
        onClick = {},
    )
    private val options = listOf(
        option,
        option.copy(
            icon = ImageResource(image = Icons.AutoMirrored.Filled.DirectionsRun),
            title = StringResource(MHCStrings.workout_preference_type_run),
        ),
        option.copy(
            icon = ImageResource(image = Icons.AutoMirrored.Filled.DirectionsBike),
            title = StringResource(MHCStrings.workout_preference_type_bicycle),
        ),
        option.copy(
            icon = ImageResource(image = Icons.Default.Pool),
            title = StringResource(MHCStrings.workout_preference_type_swim),
        ),
        option.copy(
            icon = ImageResource(image = Icons.Default.FitnessCenter),
            title = StringResource(MHCStrings.workout_preference_type_strength),
        ),
        option.copy(
            icon = ImageResource(image = Icons.Default.SportsGymnastics),
            title = StringResource(MHCStrings.workout_preference_type_hiit),
        ),
        option.copy(
            icon = ImageResource(image = Icons.Default.SelfImprovement),
            title = StringResource(MHCStrings.workout_preference_type_yoga_pilates),
        ),
    )
    private val chosenOptions = options.mapIndexed { position, row ->
        row.copy(isSelected = position in CHOSEN_POSITIONS)
    }
    private val workoutTypes = ChoiceSection(
        header = StringResource(MHCStrings.workout_preference_types_header),
        options = options,
        footer = null,
    )
    private val notificationTime = NotificationTimeRow(
        title = StringResource(MHCStrings.workout_preference_notification_time),
        subtitle = StringResource(MHCStrings.workout_preference_notification_time_subtitle),
        badge = MutableStateFlow(
            ValueBadge(
                text = StringResource("9:00 AM"),
                onClick = {},
            ),
        ),
    )
    private val layout = WorkoutPreferenceLayout(
        introduction = StringResource(MHCStrings.workout_preference_text),
        workoutTypes = MutableStateFlow(workoutTypes),
        notificationTime = notificationTime,
        timePicker = MutableStateFlow(null),
        continueButton = AsyncTextButton(
            title = StringResource(MHCStrings.workout_preference_continue),
            action = {},
        ),
    )
    private val base = WorkoutPreferenceScreenContent(
        scaffoldState = GroveScaffoldState(
            appBar = groveAppBar {
                back {}
                title(MHCStrings.workout_preference_title)
            },
        ),
        layout = layout,
        onOpened = {},
    )

    @Test
    fun `WorkoutPreferenceScreenContent step nothing chosen`() {
        // when / then
        screenshot { base.Content() }
    }

    @Test
    fun `WorkoutPreferenceScreenContent step types chosen`() {
        // given
        val content = base.copy(
            layout = layout.copy(
                workoutTypes = MutableStateFlow(workoutTypes.copy(options = chosenOptions)),
                notificationTime = notificationTime.copy(
                    badge = MutableStateFlow(
                        ValueBadge(
                            text = StringResource("6:30 PM"),
                            onClick = {},
                        ),
                    ),
                ),
            ),
        )

        // when / then
        screenshot { content.Content() }
    }

    @Test
    fun `WorkoutPreferenceScreenContent standalone with changes`() {
        // given
        val content = base.copy(
            scaffoldState = GroveScaffoldState(
                appBar = groveAppBar {
                    back {}
                    title(MHCStrings.workout_preference_title)
                    action(
                        content = AppBarTextAction(
                            title = StringResource(MHCStrings.workout_preference_save),
                            enabled = MutableStateFlow(true),
                            action = {},
                        ),
                    )
                },
            ),
            layout = layout.copy(
                workoutTypes = MutableStateFlow(workoutTypes.copy(options = chosenOptions)),
                continueButton = null,
            ),
        )

        // when / then
        screenshot { content.Content() }
    }

    private companion object {
        val CHOSEN_POSITIONS = setOf(0, 1, 6)
    }
}
