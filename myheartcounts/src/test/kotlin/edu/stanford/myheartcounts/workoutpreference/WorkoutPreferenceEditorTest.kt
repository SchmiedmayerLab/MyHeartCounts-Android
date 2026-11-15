//
// This source file is part of the My Heart Counts Android open-source project
//
// SPDX-FileCopyrightText: 2026 Stanford University and the project authors (see CONTRIBUTORS.md)
//
// SPDX-License-Identifier: MIT

package edu.stanford.myheartcounts.workoutpreference

import com.google.common.truth.Truth.assertThat
import edu.stanford.myheartcounts.account.FakeAccountService
import edu.stanford.myheartcounts.account.PreferredNudgeNotificationTimeKey
import edu.stanford.myheartcounts.account.PreferredWorkoutTypesKey
import edu.stanford.myheartcounts.model.workout.NotificationTime
import edu.stanford.myheartcounts.model.workout.WorkoutType
import edu.stanford.myheartcounts.model.workout.WorkoutTypes
import kotlinx.coroutines.test.runTest
import org.grovealliance.account.AccountDetails
import org.grovealliance.account.FakeAccount
import org.junit.Test

class WorkoutPreferenceEditorTest {

    private val account = FakeAccount()
    private val accountService = FakeAccountService()
    private val editor = WorkoutPreferenceEditorImpl(
        account = account,
        accountService = accountService,
    )

    @Test
    fun `loads the preference stored on the account`() {
        // given
        val details = AccountDetails()
        details[PreferredWorkoutTypesKey::class] = WorkoutTypes(elements = setOf(WorkoutType.SWIM, WorkoutType.SPORT))
        details[PreferredNudgeNotificationTimeKey::class] = NotificationTime(
            hour = 18,
            minute = 30,
        )
        account.supplyUserDetails(details = details)

        // when
        editor.load()

        // then
        assertThat(editor.state.value).isEqualTo(
            WorkoutPreferenceFixtures.create(
                workoutTypes = setOf(WorkoutType.SWIM, WorkoutType.SPORT),
                notificationTime = NotificationTime(
                    hour = 18,
                    minute = 30,
                ),
            ),
        )
    }

    @Test
    fun `starts without preferred types at 09 00 when nothing is stored`() {
        // given
        account.supplyUserDetails(details = AccountDetails())

        // when
        editor.load()

        // then
        assertThat(editor.state.value).isEqualTo(WorkoutPreferenceFixtures.create())
    }

    @Test
    fun `loading discards edits`() {
        // given
        account.supplyUserDetails(details = AccountDetails())
        editor.toggle(type = WorkoutType.RUN)

        // when
        editor.load()

        // then
        assertThat(editor.state.value.workoutTypes).isEmpty()
    }

    @Test
    fun `toggling adds a type and removes it again`() {
        // given
        account.supplyUserDetails(details = AccountDetails())
        editor.load()

        // when
        editor.toggle(type = WorkoutType.WALK)
        editor.toggle(type = WorkoutType.HIIT)
        val afterAdding = editor.state.value.workoutTypes
        editor.toggle(type = WorkoutType.WALK)

        // then
        assertThat(afterAdding).containsExactly(WorkoutType.WALK, WorkoutType.HIIT)
        assertThat(editor.state.value.workoutTypes).containsExactly(WorkoutType.HIIT)
    }

    @Test
    fun `selected times are rounded to the nearest quarter hour`() {
        // when
        val rounded = listOf(
            9 to 7,
            9 to 8,
            9 to 52,
            23 to 53,
        ).map { (hour, minute) ->
            editor.select(
                time = NotificationTime(
                    hour = hour,
                    minute = minute,
                ),
            )
            editor.state.value.notificationTime.description
        }

        // then
        assertThat(rounded).containsExactly("09:00", "09:15", "09:45", "00:00").inOrder()
    }

    @Test
    fun `saving stores both values on the account`() = runTest {
        // given
        account.supplyUserDetails(details = AccountDetails())
        editor.load()
        editor.toggle(type = WorkoutType.WALK)
        editor.toggle(type = WorkoutType.RUN)
        editor.select(
            time = NotificationTime(
                hour = 7,
                minute = 30,
            ),
        )

        // when
        val result = editor.save()

        // then
        val stored = accountService.updates.single().modifiedDetails
        assertThat(result.isSuccess).isTrue()
        assertThat(stored[PreferredWorkoutTypesKey::class]?.elements).containsExactly(WorkoutType.WALK, WorkoutType.RUN)
        assertThat(stored[PreferredNudgeNotificationTimeKey::class]).isEqualTo(
            NotificationTime(
                hour = 7,
                minute = 30,
            ),
        )
    }

    @Test
    fun `a failed save is returned`() = runTest {
        // given
        accountService.setUpdateResult(result = Result.failure(IllegalStateException("offline")))

        // when
        val result = editor.save()

        // then
        assertThat(result.isFailure).isTrue()
    }
}
