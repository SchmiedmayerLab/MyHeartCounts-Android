//
// This source file is part of the My Heart Counts Android open-source project
//
// SPDX-FileCopyrightText: 2026 Stanford University and the project authors (see CONTRIBUTORS.md)
//
// SPDX-License-Identifier: MIT

package edu.stanford.myheartcounts.workoutpreference

import edu.stanford.myheartcounts.account.PreferredNudgeNotificationTimeKey
import edu.stanford.myheartcounts.account.PreferredWorkoutTypesKey
import edu.stanford.myheartcounts.account.preferredNudgeNotificationTime
import edu.stanford.myheartcounts.account.preferredWorkoutTypes
import edu.stanford.myheartcounts.model.workout.NotificationTime
import edu.stanford.myheartcounts.model.workout.WorkoutType
import edu.stanford.myheartcounts.model.workout.WorkoutTypes
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import org.grovealliance.account.Account
import org.grovealliance.account.AccountDetails
import org.grovealliance.account.AccountModifications
import org.grovealliance.account.AccountService

/**
 * Holds the participant's workout preference while it is edited, and stores it on their account.
 */
interface WorkoutPreferenceEditor {

    /**
     * The preference as currently edited.
     */
    val state: StateFlow<WorkoutPreference>

    /**
     * Discards the edits and starts over from the preference stored on the signed-in participant's
     * account. Without one, no workout type is preferred and notifications are sent at 09:00.
     */
    fun load()

    /**
     * Adds [type] to the preferred workout types, or removes it when it is preferred already.
     */
    fun toggle(type: WorkoutType)

    /**
     * Sets the notification time to [time], rounded to the nearest quarter hour.
     */
    fun select(time: NotificationTime)

    /**
     * Stores the preference as currently edited on the participant's account.
     */
    suspend fun save(): Result<Unit>
}

/**
 * A participant's preferred [workoutTypes] and the time of day they would like to receive
 * activity-related notifications at.
 */
data class WorkoutPreference(
    val workoutTypes: Set<WorkoutType>,
    val notificationTime: NotificationTime,
)

/**
 * [WorkoutPreferenceEditor] storing the preference in the account's `preferredWorkoutTypes` and
 * `preferredNotificationTime`.
 */
class WorkoutPreferenceEditorImpl(
    private val account: Account,
    private val accountService: AccountService,
) : WorkoutPreferenceEditor {

    private val mutableState = MutableStateFlow(stored())

    override val state: StateFlow<WorkoutPreference> = mutableState.asStateFlow()

    override fun load() {
        mutableState.value = stored()
    }

    override fun toggle(type: WorkoutType) {
        mutableState.update { preference ->
            val types = preference.workoutTypes
            preference.copy(workoutTypes = if (type in types) types - type else types + type)
        }
    }

    override fun select(time: NotificationTime) {
        val minutes = time.hour * MINUTES_PER_HOUR + time.minute
        val rounded = (minutes + STEP_MINUTES / 2) / STEP_MINUTES * STEP_MINUTES % MINUTES_PER_DAY
        mutableState.update {
            it.copy(
                notificationTime = NotificationTime(
                    hour = rounded / MINUTES_PER_HOUR,
                    minute = rounded % MINUTES_PER_HOUR,
                ),
            )
        }
    }

    override suspend fun save(): Result<Unit> {
        val preference = mutableState.value
        val modifiedDetails = AccountDetails()
        modifiedDetails[PreferredWorkoutTypesKey::class] = WorkoutTypes(elements = preference.workoutTypes)
        modifiedDetails[PreferredNudgeNotificationTimeKey::class] = preference.notificationTime
        return AccountModifications(modifiedDetails = modifiedDetails)
            .mapCatching { accountService.updateAccountDetails(it).getOrThrow() }
    }

    private fun stored(): WorkoutPreference {
        val details = account.details.value
        return WorkoutPreference(
            workoutTypes = details?.preferredWorkoutTypes?.elements.orEmpty(),
            notificationTime = details?.preferredNudgeNotificationTime ?: NotificationTime(
                hour = DEFAULT_HOUR,
                minute = 0,
            ),
        )
    }

    private companion object {
        const val DEFAULT_HOUR = 9
        const val STEP_MINUTES = 15
        const val MINUTES_PER_HOUR = 60
        const val MINUTES_PER_DAY = 24 * MINUTES_PER_HOUR
    }
}
