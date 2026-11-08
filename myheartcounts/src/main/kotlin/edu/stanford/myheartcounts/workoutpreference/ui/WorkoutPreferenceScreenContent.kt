//
// This source file is part of the My Heart Counts Android open-source project
//
// SPDX-FileCopyrightText: 2026 Stanford University and the project authors (see CONTRIBUTORS.md)
//
// SPDX-License-Identifier: MIT

package edu.stanford.myheartcounts.workoutpreference.ui

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.PreviewParameter
import androidx.compose.ui.tooling.preview.PreviewParameterProvider
import edu.stanford.myheartcounts.ui.ValueBadge
import edu.stanford.myheartcounts.ui.choice.ChoiceSection
import kotlinx.coroutines.flow.MutableStateFlow
import org.grovealliance.ui.AsyncTextButton
import org.grovealliance.ui.ComposableContent
import org.grovealliance.ui.DisplayedEffect
import org.grovealliance.ui.GroveScaffold
import org.grovealliance.ui.GroveScaffoldState
import org.grovealliance.ui.StringResource
import org.grovealliance.ui.groveAppBar
import org.grovealliance.ui.theme.GroveTheme
import org.grovealliance.ui.theme.ThemePreviews

/**
 * The workout preference [layout] in a scaffold carrying its app bar and messages; [onOpened] is
 * called each time the screen is displayed.
 */
data class WorkoutPreferenceScreenContent(
    val scaffoldState: GroveScaffoldState,
    val layout: WorkoutPreferenceLayout,
    val onOpened: () -> Unit,
) : ComposableContent {

    @Composable
    override fun Content(modifier: Modifier) {
        DisplayedEffect(onDisplayed = onOpened)
        GroveScaffold(state = scaffoldState) {
            layout.Content(modifier = modifier)
        }
    }
}

private class WorkoutPreferenceScreenContentPreviewProvider : PreviewParameterProvider<WorkoutPreferenceScreenContent> {
    private val base = WorkoutPreferenceScreenContent(
        scaffoldState = GroveScaffoldState(
            appBar = groveAppBar {
                back {}
                title(StringResource("Workout Preference"))
            },
        ),
        layout = WorkoutPreferenceLayout(
            introduction = StringResource("Tell us which workouts you prefer and when you'd like to receive reminders."),
            workoutTypes = MutableStateFlow(
                ChoiceSection(
                    header = StringResource("Preferred Workout Types"),
                    options = emptyList(),
                    footer = null,
                ),
            ),
            notificationTime = NotificationTimeRow(
                title = StringResource("Notification Time"),
                subtitle = StringResource("The preferred time for activity-related notifications."),
                badge = MutableStateFlow(
                    ValueBadge(
                        text = StringResource("09:00"),
                        onClick = {},
                    ),
                ),
            ),
            timePicker = MutableStateFlow(null),
            continueButton = AsyncTextButton(
                title = StringResource("Save"),
                action = {},
            ),
        ),
        onOpened = {},
    )
    override val values = sequenceOf(base)
}

@ThemePreviews
@Composable
private fun Preview(
    @PreviewParameter(WorkoutPreferenceScreenContentPreviewProvider::class) content: WorkoutPreferenceScreenContent,
) {
    GroveTheme { content.Content() }
}
