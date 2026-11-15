//
// This source file is part of the My Heart Counts Android open-source project
//
// SPDX-FileCopyrightText: 2026 Stanford University and the project authors (see CONTRIBUTORS.md)
//
// SPDX-License-Identifier: MIT

package edu.stanford.myheartcounts.workoutpreference.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.fillMaxSize
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
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.tooling.preview.PreviewParameter
import androidx.compose.ui.tooling.preview.PreviewParameterProvider
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import edu.stanford.myheartcounts.ui.TextRow
import edu.stanford.myheartcounts.ui.ValueBadge
import edu.stanford.myheartcounts.ui.choice.ChoiceRow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import org.grovealliance.ui.AsyncTextButton
import org.grovealliance.ui.ComposableContent
import org.grovealliance.ui.GroveSectionList
import org.grovealliance.ui.ImageResource
import org.grovealliance.ui.StringResource
import org.grovealliance.ui.groveSectionList
import org.grovealliance.ui.theme.Colors
import org.grovealliance.ui.theme.GroveTheme
import org.grovealliance.ui.theme.ThemePreviews

/**
 * The workout preference form: the introduction, the workout types to choose from, the notification
 * time and, if any, the continue button in [sections], scrolling together, with the [timePicker], if
 * any, open above them.
 *
 * @param sections the form's sections, following the preference as it changes
 * @param timePicker the notification time picker currently open
 */
data class WorkoutPreferenceLayout(
    val sections: StateFlow<GroveSectionList>,
    val timePicker: StateFlow<NotificationTimePicker?>,
) : ComposableContent {

    @Composable
    override fun Content(modifier: Modifier) {
        val sections by sections.collectAsStateWithLifecycle()
        val timePicker by timePicker.collectAsStateWithLifecycle()

        sections.Content(
            modifier = modifier
                .fillMaxSize()
                .background(Colors.surfaceContainer),
        )
        timePicker?.Content()
    }
}

private class WorkoutPreferenceLayoutPreviewProvider : PreviewParameterProvider<WorkoutPreferenceLayout> {
    private val workoutTypes = listOf(
        "Walking" to Icons.AutoMirrored.Filled.DirectionsWalk,
        "Running" to Icons.AutoMirrored.Filled.DirectionsRun,
        "Cycling" to Icons.AutoMirrored.Filled.DirectionsBike,
        "Swimming" to Icons.Default.Pool,
        "Strength" to Icons.Default.FitnessCenter,
        "High-intensity Training" to Icons.Default.SportsGymnastics,
        "Yoga / Pilates" to Icons.Default.SelfImprovement,
        "Sport" to Icons.Default.SportsSoccer,
        "Other" to Icons.Default.EmojiPeople,
    )
    private val notificationTime = NotificationTimeRow(
        title = StringResource("Notification Time"),
        subtitle = StringResource("The preferred time for activity-related notifications."),
        badge = MutableStateFlow(
            ValueBadge(
                text = StringResource("09:00"),
                onClick = {},
            ),
        ),
    )
    private val base = WorkoutPreferenceLayout(
        sections = MutableStateFlow(sections(selected = setOf("Walking", "Running", "Yoga / Pilates"))),
        timePicker = MutableStateFlow(null),
    )
    override val values = sequenceOf(
        base,
        base.copy(sections = MutableStateFlow(sections(selected = emptySet()))),
        base.copy(
            timePicker = MutableStateFlow(
                NotificationTimePicker(
                    selection = NotificationTimePicker.State(
                        hour = 9,
                        minute = 0,
                    ),
                    confirmTitle = StringResource("OK"),
                    dismissTitle = StringResource("Cancel"),
                    onSelect = {},
                    onDismiss = {},
                ),
            ),
        ),
    )

    private fun sections(selected: Set<String>) = groveSectionList {
        section(key = "introduction") {
            card(key = "introduction") {
                row(
                    key = "introduction",
                    content = TextRow(
                        text = StringResource(
                            "My Heart Counts will send you personalized notifications to help you stay active. Tell " +
                                "us which workouts you prefer and when during the day you’d like to receive reminders.",
                        ),
                    ),
                )
            }
        }
        section(key = "workout-types") {
            title("Preferred Workout Types")
            card(key = "workout-types") {
                workoutTypes.forEach { (title, icon) ->
                    row(
                        key = title,
                        content = option(
                            title = title,
                            icon = icon,
                            isSelected = title in selected,
                        ),
                    )
                }
            }
        }
        section(key = "notification-time") {
            card(key = "notification-time") {
                row(
                    key = "notification-time",
                    content = notificationTime,
                )
            }
        }
        section(key = "continue") {
            content(
                key = "continue",
                content = AsyncTextButton(
                    title = StringResource("Continue"),
                    action = {},
                ),
            )
        }
    }

    private fun option(title: String, icon: ImageVector, isSelected: Boolean) = ChoiceRow(
        icon = ImageResource(image = icon),
        title = StringResource(title),
        subtitle = null,
        detail = null,
        isSelected = isSelected,
        followUp = null,
        onClick = {},
    )
}

@ThemePreviews
@Composable
private fun Preview(@PreviewParameter(WorkoutPreferenceLayoutPreviewProvider::class) content: WorkoutPreferenceLayout) {
    GroveTheme { content.Content() }
}
