//
// This source file is part of the My Heart Counts Android open-source project
//
// SPDX-FileCopyrightText: 2026 Stanford University and the project authors (see CONTRIBUTORS.md)
//
// SPDX-License-Identifier: MIT

@file:Suppress("detekt:MagicNumber")

package edu.stanford.myheartcounts.healthstats

import androidx.health.connect.client.records.ExerciseSegment
import androidx.health.connect.client.records.ExerciseSessionRecord
import java.time.Duration

/**
 * Maps a workout to its stable activity code and duration excluding explicit pauses.
 */
interface HealthStatsWorkoutMapper {
    fun map(record: ExerciseSessionRecord): HealthStatsEntry.Workout
}

class HealthStatsWorkoutMapperImpl : HealthStatsWorkoutMapper {
    override fun map(record: ExerciseSessionRecord): HealthStatsEntry.Workout {
        val pauses = record.segments.filter { it.segmentType == ExerciseSegment.EXERCISE_SEGMENT_TYPE_PAUSE }
            .fold(Duration.ZERO) { total, pause -> total.plus(Duration.between(pause.startTime, pause.endTime)) }
        val duration = Duration.between(record.startTime, record.endTime).minus(pauses)
        return HealthStatsEntry.Workout(
            id = "healthconnect:${record.metadata.id}",
            date = record.startTime,
            endDate = record.endTime,
            durationSeconds = duration.seconds + duration.nano / NANOS_PER_SECOND,
            activityType = ACTIVITY_TYPES[record.exerciseType] ?: OTHER_ACTIVITY,
        )
    }

    private companion object {
        const val NANOS_PER_SECOND = 1_000_000_000.0
        const val OTHER_ACTIVITY = 3000L
        val ACTIVITY_TYPES = mapOf(
            ExerciseSessionRecord.EXERCISE_TYPE_BADMINTON to 4L,
            ExerciseSessionRecord.EXERCISE_TYPE_BASEBALL to 5L,
            ExerciseSessionRecord.EXERCISE_TYPE_BASKETBALL to 6L,
            ExerciseSessionRecord.EXERCISE_TYPE_BIKING to 13L,
            ExerciseSessionRecord.EXERCISE_TYPE_BIKING_STATIONARY to 13L,
            ExerciseSessionRecord.EXERCISE_TYPE_BOOT_CAMP to 11L,
            ExerciseSessionRecord.EXERCISE_TYPE_BOXING to 8L,
            ExerciseSessionRecord.EXERCISE_TYPE_CALISTHENICS to 20L,
            ExerciseSessionRecord.EXERCISE_TYPE_CRICKET to 10L,
            ExerciseSessionRecord.EXERCISE_TYPE_DANCING to 77L,
            ExerciseSessionRecord.EXERCISE_TYPE_ELLIPTICAL to 16L,
            ExerciseSessionRecord.EXERCISE_TYPE_EXERCISE_CLASS to 73L,
            ExerciseSessionRecord.EXERCISE_TYPE_FENCING to 18L,
            ExerciseSessionRecord.EXERCISE_TYPE_FOOTBALL_AMERICAN to 1L,
            ExerciseSessionRecord.EXERCISE_TYPE_FOOTBALL_AUSTRALIAN to 3L,
            ExerciseSessionRecord.EXERCISE_TYPE_FRISBEE_DISC to 75L,
            ExerciseSessionRecord.EXERCISE_TYPE_GOLF to 21L,
            ExerciseSessionRecord.EXERCISE_TYPE_GUIDED_BREATHING to 29L,
            ExerciseSessionRecord.EXERCISE_TYPE_GYMNASTICS to 22L,
            ExerciseSessionRecord.EXERCISE_TYPE_HANDBALL to 23L,
            ExerciseSessionRecord.EXERCISE_TYPE_HIGH_INTENSITY_INTERVAL_TRAINING to 63L,
            ExerciseSessionRecord.EXERCISE_TYPE_HIKING to 24L,
            ExerciseSessionRecord.EXERCISE_TYPE_ICE_HOCKEY to 25L,
            ExerciseSessionRecord.EXERCISE_TYPE_ICE_SKATING to 39L,
            ExerciseSessionRecord.EXERCISE_TYPE_MARTIAL_ARTS to 28L,
            ExerciseSessionRecord.EXERCISE_TYPE_PADDLING to 31L,
            ExerciseSessionRecord.EXERCISE_TYPE_PARAGLIDING to 3000L,
            ExerciseSessionRecord.EXERCISE_TYPE_PILATES to 66L,
            ExerciseSessionRecord.EXERCISE_TYPE_RACQUETBALL to 34L,
            ExerciseSessionRecord.EXERCISE_TYPE_ROCK_CLIMBING to 9L,
            ExerciseSessionRecord.EXERCISE_TYPE_ROLLER_HOCKEY to 25L,
            ExerciseSessionRecord.EXERCISE_TYPE_ROWING to 35L,
            ExerciseSessionRecord.EXERCISE_TYPE_ROWING_MACHINE to 35L,
            ExerciseSessionRecord.EXERCISE_TYPE_RUGBY to 36L,
            ExerciseSessionRecord.EXERCISE_TYPE_RUNNING to 37L,
            ExerciseSessionRecord.EXERCISE_TYPE_RUNNING_TREADMILL to 37L,
            ExerciseSessionRecord.EXERCISE_TYPE_SAILING to 38L,
            ExerciseSessionRecord.EXERCISE_TYPE_SCUBA_DIVING to 84L,
            ExerciseSessionRecord.EXERCISE_TYPE_SKATING to 39L,
            ExerciseSessionRecord.EXERCISE_TYPE_SKIING to 40L,
            ExerciseSessionRecord.EXERCISE_TYPE_SNOWBOARDING to 67L,
            ExerciseSessionRecord.EXERCISE_TYPE_SNOWSHOEING to 40L,
            ExerciseSessionRecord.EXERCISE_TYPE_SOCCER to 41L,
            ExerciseSessionRecord.EXERCISE_TYPE_SOFTBALL to 42L,
            ExerciseSessionRecord.EXERCISE_TYPE_SQUASH to 43L,
            ExerciseSessionRecord.EXERCISE_TYPE_STAIR_CLIMBING to 44L,
            ExerciseSessionRecord.EXERCISE_TYPE_STAIR_CLIMBING_MACHINE to 44L,
            ExerciseSessionRecord.EXERCISE_TYPE_STRENGTH_TRAINING to 50L,
            ExerciseSessionRecord.EXERCISE_TYPE_STRETCHING to 62L,
            ExerciseSessionRecord.EXERCISE_TYPE_SURFING to 45L,
            ExerciseSessionRecord.EXERCISE_TYPE_SWIMMING_OPEN_WATER to 46L,
            ExerciseSessionRecord.EXERCISE_TYPE_SWIMMING_POOL to 46L,
            ExerciseSessionRecord.EXERCISE_TYPE_TABLE_TENNIS to 47L,
            ExerciseSessionRecord.EXERCISE_TYPE_TENNIS to 48L,
            ExerciseSessionRecord.EXERCISE_TYPE_VOLLEYBALL to 51L,
            ExerciseSessionRecord.EXERCISE_TYPE_WALKING to 52L,
            ExerciseSessionRecord.EXERCISE_TYPE_WATER_POLO to 54L,
            ExerciseSessionRecord.EXERCISE_TYPE_WEIGHTLIFTING to 50L,
            ExerciseSessionRecord.EXERCISE_TYPE_WHEELCHAIR to 70L,
            ExerciseSessionRecord.EXERCISE_TYPE_YOGA to 57L,
        )
    }
}
