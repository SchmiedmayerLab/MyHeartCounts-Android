//
// This source file is part of the My Heart Counts Android open-source project
//
// SPDX-FileCopyrightText: 2026 Stanford University and the project authors (see CONTRIBUTORS.md)
//
// SPDX-License-Identifier: MIT

package edu.stanford.myheartcounts.task

import androidx.annotation.StringRes
import edu.stanford.myheartcounts.MHCStrings
import org.grovealliance.scheduler.Event
import org.grovealliance.study.ScheduledTaskAction
import org.grovealliance.study.ScheduledTaskActionKey
import org.grovealliance.study.StudyTaskCategories
import org.grovealliance.studydefinition.TimedWalkingTestConfiguration
import java.time.Duration

/**
 * A task a participant may perform whenever they choose, independently of their schedule.
 */
enum class AlwaysAvailableTask(
    @get:StringRes val titleId: Int,
    @get:StringRes val subtitleId: Int?,
    @get:StringRes val actionTitleId: Int,
) {
    ELECTROCARDIOGRAM(
        titleId = MHCStrings.always_available_ecg_title,
        subtitleId = MHCStrings.always_available_ecg_subtitle,
        actionTitleId = MHCStrings.task_action_take_ecg,
    ),
    SIX_MINUTE_WALK(
        titleId = MHCStrings.always_available_six_minute_walk_title,
        subtitleId = null,
        actionTitleId = MHCStrings.task_action_take_test,
    ),
    TWELVE_MINUTE_RUN(
        titleId = MHCStrings.always_available_twelve_minute_run_title,
        subtitleId = null,
        actionTitleId = MHCStrings.task_action_take_test,
    ),
    ;

    /**
     * Whether one of [events] already asks for this task: the electrocardiogram by its category, a
     * timed test by its test configuration.
     */
    fun isScheduledIn(events: List<Event>): Boolean = events.any { event ->
        when (this) {
            ELECTROCARDIOGRAM -> event.task.category == StudyTaskCategories.customActiveTask(ECG_IDENTIFIER)
            SIX_MINUTE_WALK, TWELVE_MINUTE_RUN -> {
                val action = event.task.context[ScheduledTaskActionKey]
                action is ScheduledTaskAction.PromptTimedWalkingTest && action.component.test == timedTest()
            }
        }
    }

    private fun timedTest(): TimedWalkingTestConfiguration? = when (this) {
        ELECTROCARDIOGRAM -> null
        SIX_MINUTE_WALK -> TimedWalkingTestConfiguration(
            duration = Duration.ofMinutes(SIX_MINUTES),
            kind = TimedWalkingTestConfiguration.Kind.WALKING,
        )
        TWELVE_MINUTE_RUN -> TimedWalkingTestConfiguration(
            duration = Duration.ofMinutes(TWELVE_MINUTES),
            kind = TimedWalkingTestConfiguration.Kind.RUNNING,
        )
    }

    companion object {
        /**
         * The identifier of the electrocardiogram's custom active task.
         */
        const val ECG_IDENTIFIER = "edu.stanford.MyHeartCounts.activeTask.ecg"

        private const val SIX_MINUTES = 6L
        private const val TWELVE_MINUTES = 12L
    }
}
