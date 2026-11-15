//
// This source file is part of the My Heart Counts Android open-source project
//
// SPDX-FileCopyrightText: 2026 Stanford University and the project authors (see CONTRIBUTORS.md)
//
// SPDX-License-Identifier: MIT

package edu.stanford.myheartcounts.workoutpreference

import edu.stanford.myheartcounts.ui.choice.ChoiceRow
import edu.stanford.myheartcounts.workoutpreference.ui.NotificationTimeRow
import edu.stanford.myheartcounts.workoutpreference.ui.WorkoutPreferenceLayout
import org.grovealliance.ui.AsyncTextButton
import org.grovealliance.ui.GroveSection

internal fun WorkoutPreferenceLayout.section(key: String) = sections.value.sections.single { it.key == key }

internal fun WorkoutPreferenceLayout.workoutTypes(): List<ChoiceRow> =
    section(key = "workout-types").cardRows().map { it.content as ChoiceRow }

internal fun WorkoutPreferenceLayout.notificationTime() =
    section(key = "notification-time").cardRows().single().content as NotificationTimeRow

internal fun WorkoutPreferenceLayout.continueButton() = sections.value.sections
    .find { it.key == "continue" }
    ?.let { (it.elements.single() as GroveSection.Element.Content).content as AsyncTextButton }

private fun GroveSection.cardRows() = (elements.single() as GroveSection.Element.Card).rows
