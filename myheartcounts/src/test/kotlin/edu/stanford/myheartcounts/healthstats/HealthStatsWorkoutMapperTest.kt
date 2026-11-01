//
// This source file is part of the My Heart Counts Android open-source project
//
// SPDX-FileCopyrightText: 2026 Stanford University and the project authors (see CONTRIBUTORS.md)
//
// SPDX-License-Identifier: MIT

package edu.stanford.myheartcounts.healthstats

import androidx.health.connect.client.records.ExerciseSessionRecord
import com.google.common.truth.Truth.assertThat
import org.junit.Test

class HealthStatsWorkoutMapperTest {
    private val mapper = HealthStatsWorkoutMapperImpl()

    @Test
    fun `walking uses the stable activity code and excludes explicit pauses`() {
        // given
        val record = HealthStatsWritingFixtures.workout(type = ExerciseSessionRecord.EXERCISE_TYPE_WALKING)

        // when
        val entry = mapper.map(record = record)

        // then
        assertThat(entry.activityType).isEqualTo(52)
        assertThat(entry.durationSeconds).isEqualTo(3300.0)
        assertThat(entry.id).startsWith("healthconnect:")
    }

    @Test
    fun `running and cycling map to their stable codes`() {
        // when / then
        assertThat(
            mapper.map(record = HealthStatsWritingFixtures.workout(type = ExerciseSessionRecord.EXERCISE_TYPE_RUNNING)).activityType,
        ).isEqualTo(37)
        assertThat(
            mapper.map(record = HealthStatsWritingFixtures.workout(type = ExerciseSessionRecord.EXERCISE_TYPE_BIKING)).activityType,
        ).isEqualTo(13)
    }

    @Test
    fun `other workouts use the shared fallback code`() {
        // when
        val entry = mapper.map(record = HealthStatsWritingFixtures.workout(type = ExerciseSessionRecord.EXERCISE_TYPE_OTHER_WORKOUT))

        // then
        assertThat(entry.activityType).isEqualTo(3000)
    }
}
