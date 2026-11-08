//
// This source file is part of the My Heart Counts Android open-source project
//
// SPDX-FileCopyrightText: 2026 Stanford University and the project authors (see CONTRIBUTORS.md)
//
// SPDX-License-Identifier: MIT

package edu.stanford.myheartcounts.workoutpreference.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
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
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.tooling.preview.PreviewParameter
import androidx.compose.ui.tooling.preview.PreviewParameterProvider
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import edu.stanford.myheartcounts.ui.ValueBadge
import edu.stanford.myheartcounts.ui.choice.ChoiceRow
import edu.stanford.myheartcounts.ui.choice.ChoiceSection
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import org.grovealliance.ui.AsyncTextButton
import org.grovealliance.ui.ComposableContent
import org.grovealliance.ui.GroveCard
import org.grovealliance.ui.ImageResource
import org.grovealliance.ui.StringResource
import org.grovealliance.ui.theme.Colors
import org.grovealliance.ui.theme.GroveShapes
import org.grovealliance.ui.theme.GroveTheme
import org.grovealliance.ui.theme.Spacings
import org.grovealliance.ui.theme.TextStyles
import org.grovealliance.ui.theme.ThemePreviews

/**
 * The workout preference form: an [introduction] card, the [workoutTypes] to choose from, the
 * [notificationTime] card and, if any, the [continueButton] at the end, scrolling together, with
 * the [timePicker], if any, open above them.
 *
 * @param introduction what the preferences are used for
 * @param workoutTypes the workout types, the preferred ones checked
 * @param notificationTime the card showing the preferred notification time
 * @param timePicker the notification time picker currently open
 * @param continueButton the button confirming the preferences, after the last card; absent when they
 *   are confirmed elsewhere
 */
data class WorkoutPreferenceLayout(
    val introduction: StringResource,
    val workoutTypes: StateFlow<ChoiceSection>,
    val notificationTime: NotificationTimeRow,
    val timePicker: StateFlow<NotificationTimePicker?>,
    val continueButton: AsyncTextButton?,
) : ComposableContent {

    @Composable
    override fun Content(modifier: Modifier) {
        val workoutTypes by workoutTypes.collectAsStateWithLifecycle()
        val timePicker by timePicker.collectAsStateWithLifecycle()

        LazyColumn(
            modifier = modifier
                .fillMaxSize()
                .background(Colors.surfaceContainer),
            contentPadding = PaddingValues(Spacings.medium),
            verticalArrangement = Arrangement.spacedBy(Spacings.large),
        ) {
            item {
                GroveCard(
                    modifier = Modifier.fillMaxWidth(),
                    shape = GroveShapes.large,
                ) {
                    Text(
                        modifier = Modifier.padding(Spacings.medium),
                        text = introduction.text(),
                        style = TextStyles.bodyLarge,
                        color = Colors.onSurface,
                    )
                }
            }
            item {
                workoutTypes.Content(modifier = Modifier.fillMaxWidth())
            }
            item {
                notificationTime.Content(modifier = Modifier.fillMaxWidth())
            }
            continueButton?.let {
                item {
                    it.Content(modifier = Modifier.fillMaxWidth())
                }
            }
        }
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
    private val base = WorkoutPreferenceLayout(
        introduction = StringResource(
            "My Heart Counts will send you personalized notifications to help you stay active. Tell us " +
                "which workouts you prefer and when during the day you’d like to receive reminders. This " +
                "helps us tailor your experience to your schedule and goals.",
        ),
        workoutTypes = MutableStateFlow(section(selected = setOf("Walking", "Running", "Yoga / Pilates"))),
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
            title = StringResource("Continue"),
            action = {},
        ),
    )
    override val values = sequenceOf(
        base,
        base.copy(workoutTypes = MutableStateFlow(section(selected = emptySet()))),
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

    private fun section(selected: Set<String>) = ChoiceSection(
        header = StringResource("Preferred Workout Types"),
        options = workoutTypes.map { (title, icon) -> option(title = title, icon = icon, isSelected = title in selected) },
        footer = null,
    )

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
