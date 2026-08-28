//
// This source file belongs to the My Heart Counts Android project
//
// SPDX-FileCopyrightText: 2026 Stanford University and the project authors (see CONTRIBUTORS.md)
//
// SPDX-License-Identifier: MIT

package org.grovealliance.health.fhir

import androidx.health.connect.client.records.ExerciseSegment
import androidx.health.connect.client.records.ExerciseSessionRecord

/** A shared workout classification together with the exact Health Connect token it absorbed. */
internal data class WorkoutClassification(
    val sharedSystem: String,
    val value: SourceCodedValue,
)

private data class WorkoutCoding(
    val sourceToken: String,
    val sharedCode: String,
)

private fun activity(exerciseType: Int, sourceToken: String, sharedCode: String): Pair<Int, WorkoutCoding> =
    exerciseType to WorkoutCoding(sourceToken, sharedCode)

private fun segment(segmentType: Int, sourceToken: String, sharedCode: String): Pair<Int, WorkoutCoding> =
    segmentType to WorkoutCoding(sourceToken, sharedCode)

private val SHARED_ACTIVITY_DISPLAYS: Map<String, String> = mapOf(
    "running" to "Running",
    "walking" to "Walking",
    "cycling" to "Cycling",
    "hiking" to "Hiking",
    "swimming" to "Swimming",
    "strength-training" to "Strength training",
    "high-intensity-interval-training" to "High-intensity interval training",
    "yoga" to "Yoga",
    "pilates" to "Pilates",
    "rowing" to "Rowing",
    "elliptical" to "Elliptical",
    "stair-climbing" to "Stair climbing",
    "dancing" to "Dancing",
    "tennis" to "Tennis",
    "table-tennis" to "Table tennis",
    "badminton" to "Badminton",
    "squash" to "Squash",
    "basketball" to "Basketball",
    "soccer" to "Soccer",
    "american-football" to "American football",
    "baseball" to "Baseball",
    "volleyball" to "Volleyball",
    "golf" to "Golf",
    "boxing" to "Boxing",
    "martial-arts" to "Martial arts",
    "skiing" to "Skiing",
    "snowboarding" to "Snowboarding",
    "other" to "Other activity",
)

private val SHARED_SEGMENT_DISPLAYS: Map<String, String> = mapOf(
    "lap" to "Lap",
    "pause" to "Pause",
    "resume" to "Resume",
    "motion-paused" to "Motion paused",
    "motion-resumed" to "Motion resumed",
    "pause-or-resume-request" to "Pause or resume request",
    "marker" to "Marker",
    "segment-generic" to "Segment",
    "rest" to "Rest",
    "other-workout" to "Other workout",
    "unknown" to "Unknown",
)

/** Every AndroidX 1.1 exercise type; a type without a shared equivalent absorbs into #other. */
private val EXERCISE_TYPES: Map<Int, WorkoutCoding> = mapOf(
    activity(ExerciseSessionRecord.EXERCISE_TYPE_OTHER_WORKOUT, "EXERCISE_TYPE_OTHER_WORKOUT", "other"),
    activity(ExerciseSessionRecord.EXERCISE_TYPE_BADMINTON, "EXERCISE_TYPE_BADMINTON", "badminton"),
    activity(ExerciseSessionRecord.EXERCISE_TYPE_BASEBALL, "EXERCISE_TYPE_BASEBALL", "baseball"),
    activity(ExerciseSessionRecord.EXERCISE_TYPE_BASKETBALL, "EXERCISE_TYPE_BASKETBALL", "basketball"),
    activity(ExerciseSessionRecord.EXERCISE_TYPE_BIKING, "EXERCISE_TYPE_BIKING", "cycling"),
    activity(ExerciseSessionRecord.EXERCISE_TYPE_BIKING_STATIONARY, "EXERCISE_TYPE_BIKING_STATIONARY", "cycling"),
    activity(ExerciseSessionRecord.EXERCISE_TYPE_BOOT_CAMP, "EXERCISE_TYPE_BOOT_CAMP", "other"),
    activity(ExerciseSessionRecord.EXERCISE_TYPE_BOXING, "EXERCISE_TYPE_BOXING", "boxing"),
    activity(ExerciseSessionRecord.EXERCISE_TYPE_CALISTHENICS, "EXERCISE_TYPE_CALISTHENICS", "other"),
    activity(ExerciseSessionRecord.EXERCISE_TYPE_CRICKET, "EXERCISE_TYPE_CRICKET", "other"),
    activity(ExerciseSessionRecord.EXERCISE_TYPE_DANCING, "EXERCISE_TYPE_DANCING", "dancing"),
    activity(ExerciseSessionRecord.EXERCISE_TYPE_ELLIPTICAL, "EXERCISE_TYPE_ELLIPTICAL", "elliptical"),
    activity(ExerciseSessionRecord.EXERCISE_TYPE_EXERCISE_CLASS, "EXERCISE_TYPE_EXERCISE_CLASS", "other"),
    activity(ExerciseSessionRecord.EXERCISE_TYPE_FENCING, "EXERCISE_TYPE_FENCING", "other"),
    activity(
        ExerciseSessionRecord.EXERCISE_TYPE_FOOTBALL_AMERICAN,
        "EXERCISE_TYPE_FOOTBALL_AMERICAN",
        "american-football",
    ),
    activity(ExerciseSessionRecord.EXERCISE_TYPE_FOOTBALL_AUSTRALIAN, "EXERCISE_TYPE_FOOTBALL_AUSTRALIAN", "other"),
    activity(ExerciseSessionRecord.EXERCISE_TYPE_FRISBEE_DISC, "EXERCISE_TYPE_FRISBEE_DISC", "other"),
    activity(ExerciseSessionRecord.EXERCISE_TYPE_GOLF, "EXERCISE_TYPE_GOLF", "golf"),
    activity(ExerciseSessionRecord.EXERCISE_TYPE_GUIDED_BREATHING, "EXERCISE_TYPE_GUIDED_BREATHING", "other"),
    activity(ExerciseSessionRecord.EXERCISE_TYPE_GYMNASTICS, "EXERCISE_TYPE_GYMNASTICS", "other"),
    activity(ExerciseSessionRecord.EXERCISE_TYPE_HANDBALL, "EXERCISE_TYPE_HANDBALL", "other"),
    activity(
        ExerciseSessionRecord.EXERCISE_TYPE_HIGH_INTENSITY_INTERVAL_TRAINING,
        "EXERCISE_TYPE_HIGH_INTENSITY_INTERVAL_TRAINING",
        "high-intensity-interval-training",
    ),
    activity(ExerciseSessionRecord.EXERCISE_TYPE_HIKING, "EXERCISE_TYPE_HIKING", "hiking"),
    activity(ExerciseSessionRecord.EXERCISE_TYPE_ICE_HOCKEY, "EXERCISE_TYPE_ICE_HOCKEY", "other"),
    activity(ExerciseSessionRecord.EXERCISE_TYPE_ICE_SKATING, "EXERCISE_TYPE_ICE_SKATING", "other"),
    activity(ExerciseSessionRecord.EXERCISE_TYPE_MARTIAL_ARTS, "EXERCISE_TYPE_MARTIAL_ARTS", "martial-arts"),
    activity(ExerciseSessionRecord.EXERCISE_TYPE_PADDLING, "EXERCISE_TYPE_PADDLING", "other"),
    activity(ExerciseSessionRecord.EXERCISE_TYPE_PARAGLIDING, "EXERCISE_TYPE_PARAGLIDING", "other"),
    activity(ExerciseSessionRecord.EXERCISE_TYPE_PILATES, "EXERCISE_TYPE_PILATES", "pilates"),
    activity(ExerciseSessionRecord.EXERCISE_TYPE_RACQUETBALL, "EXERCISE_TYPE_RACQUETBALL", "other"),
    activity(ExerciseSessionRecord.EXERCISE_TYPE_ROCK_CLIMBING, "EXERCISE_TYPE_ROCK_CLIMBING", "other"),
    activity(ExerciseSessionRecord.EXERCISE_TYPE_ROLLER_HOCKEY, "EXERCISE_TYPE_ROLLER_HOCKEY", "other"),
    activity(ExerciseSessionRecord.EXERCISE_TYPE_ROWING, "EXERCISE_TYPE_ROWING", "rowing"),
    activity(ExerciseSessionRecord.EXERCISE_TYPE_ROWING_MACHINE, "EXERCISE_TYPE_ROWING_MACHINE", "rowing"),
    activity(ExerciseSessionRecord.EXERCISE_TYPE_RUGBY, "EXERCISE_TYPE_RUGBY", "other"),
    activity(ExerciseSessionRecord.EXERCISE_TYPE_RUNNING, "EXERCISE_TYPE_RUNNING", "running"),
    activity(ExerciseSessionRecord.EXERCISE_TYPE_RUNNING_TREADMILL, "EXERCISE_TYPE_RUNNING_TREADMILL", "running"),
    activity(ExerciseSessionRecord.EXERCISE_TYPE_SAILING, "EXERCISE_TYPE_SAILING", "other"),
    activity(ExerciseSessionRecord.EXERCISE_TYPE_SCUBA_DIVING, "EXERCISE_TYPE_SCUBA_DIVING", "other"),
    activity(ExerciseSessionRecord.EXERCISE_TYPE_SKATING, "EXERCISE_TYPE_SKATING", "other"),
    activity(ExerciseSessionRecord.EXERCISE_TYPE_SKIING, "EXERCISE_TYPE_SKIING", "skiing"),
    activity(ExerciseSessionRecord.EXERCISE_TYPE_SNOWBOARDING, "EXERCISE_TYPE_SNOWBOARDING", "snowboarding"),
    activity(ExerciseSessionRecord.EXERCISE_TYPE_SNOWSHOEING, "EXERCISE_TYPE_SNOWSHOEING", "other"),
    activity(ExerciseSessionRecord.EXERCISE_TYPE_SOCCER, "EXERCISE_TYPE_SOCCER", "soccer"),
    activity(ExerciseSessionRecord.EXERCISE_TYPE_SOFTBALL, "EXERCISE_TYPE_SOFTBALL", "other"),
    activity(ExerciseSessionRecord.EXERCISE_TYPE_SQUASH, "EXERCISE_TYPE_SQUASH", "squash"),
    activity(ExerciseSessionRecord.EXERCISE_TYPE_STAIR_CLIMBING, "EXERCISE_TYPE_STAIR_CLIMBING", "stair-climbing"),
    activity(
        ExerciseSessionRecord.EXERCISE_TYPE_STAIR_CLIMBING_MACHINE,
        "EXERCISE_TYPE_STAIR_CLIMBING_MACHINE",
        "stair-climbing",
    ),
    activity(
        ExerciseSessionRecord.EXERCISE_TYPE_STRENGTH_TRAINING,
        "EXERCISE_TYPE_STRENGTH_TRAINING",
        "strength-training",
    ),
    activity(ExerciseSessionRecord.EXERCISE_TYPE_STRETCHING, "EXERCISE_TYPE_STRETCHING", "other"),
    activity(ExerciseSessionRecord.EXERCISE_TYPE_SURFING, "EXERCISE_TYPE_SURFING", "other"),
    activity(ExerciseSessionRecord.EXERCISE_TYPE_SWIMMING_OPEN_WATER, "EXERCISE_TYPE_SWIMMING_OPEN_WATER", "swimming"),
    activity(ExerciseSessionRecord.EXERCISE_TYPE_SWIMMING_POOL, "EXERCISE_TYPE_SWIMMING_POOL", "swimming"),
    activity(ExerciseSessionRecord.EXERCISE_TYPE_TABLE_TENNIS, "EXERCISE_TYPE_TABLE_TENNIS", "table-tennis"),
    activity(ExerciseSessionRecord.EXERCISE_TYPE_TENNIS, "EXERCISE_TYPE_TENNIS", "tennis"),
    activity(ExerciseSessionRecord.EXERCISE_TYPE_VOLLEYBALL, "EXERCISE_TYPE_VOLLEYBALL", "volleyball"),
    activity(ExerciseSessionRecord.EXERCISE_TYPE_WALKING, "EXERCISE_TYPE_WALKING", "walking"),
    activity(ExerciseSessionRecord.EXERCISE_TYPE_WATER_POLO, "EXERCISE_TYPE_WATER_POLO", "other"),
    activity(
        ExerciseSessionRecord.EXERCISE_TYPE_WEIGHTLIFTING,
        "EXERCISE_TYPE_WEIGHTLIFTING",
        "strength-training",
    ),
    activity(ExerciseSessionRecord.EXERCISE_TYPE_WHEELCHAIR, "EXERCISE_TYPE_WHEELCHAIR", "other"),
    activity(ExerciseSessionRecord.EXERCISE_TYPE_YOGA, "EXERCISE_TYPE_YOGA", "yoga"),
)

/**
 * Every AndroidX 1.1 segment type. Health Connect states structure only for unknown, pause, rest,
 * and nested other-workout segments; every remaining token is an activity segment, so it absorbs
 * into the shared activity codes and falls back to #other.
 */
private val EXERCISE_SEGMENT_TYPES: Map<Int, WorkoutCoding> = mapOf(
    segment(ExerciseSegment.EXERCISE_SEGMENT_TYPE_UNKNOWN, "EXERCISE_SEGMENT_TYPE_UNKNOWN", "unknown"),
    segment(ExerciseSegment.EXERCISE_SEGMENT_TYPE_OTHER_WORKOUT, "EXERCISE_SEGMENT_TYPE_OTHER_WORKOUT", "other-workout"),
    segment(ExerciseSegment.EXERCISE_SEGMENT_TYPE_PAUSE, "EXERCISE_SEGMENT_TYPE_PAUSE", "pause"),
    segment(ExerciseSegment.EXERCISE_SEGMENT_TYPE_REST, "EXERCISE_SEGMENT_TYPE_REST", "rest"),
    segment(ExerciseSegment.EXERCISE_SEGMENT_TYPE_ARM_CURL, "EXERCISE_SEGMENT_TYPE_ARM_CURL", "strength-training"),
    segment(
        ExerciseSegment.EXERCISE_SEGMENT_TYPE_BACK_EXTENSION,
        "EXERCISE_SEGMENT_TYPE_BACK_EXTENSION",
        "strength-training",
    ),
    segment(ExerciseSegment.EXERCISE_SEGMENT_TYPE_BALL_SLAM, "EXERCISE_SEGMENT_TYPE_BALL_SLAM", "strength-training"),
    segment(
        ExerciseSegment.EXERCISE_SEGMENT_TYPE_BARBELL_SHOULDER_PRESS,
        "EXERCISE_SEGMENT_TYPE_BARBELL_SHOULDER_PRESS",
        "strength-training",
    ),
    segment(ExerciseSegment.EXERCISE_SEGMENT_TYPE_BENCH_PRESS, "EXERCISE_SEGMENT_TYPE_BENCH_PRESS", "strength-training"),
    segment(
        ExerciseSegment.EXERCISE_SEGMENT_TYPE_BENCH_SIT_UP,
        "EXERCISE_SEGMENT_TYPE_BENCH_SIT_UP",
        "strength-training",
    ),
    segment(ExerciseSegment.EXERCISE_SEGMENT_TYPE_BIKING, "EXERCISE_SEGMENT_TYPE_BIKING", "cycling"),
    segment(ExerciseSegment.EXERCISE_SEGMENT_TYPE_BIKING_STATIONARY, "EXERCISE_SEGMENT_TYPE_BIKING_STATIONARY", "cycling"),
    segment(ExerciseSegment.EXERCISE_SEGMENT_TYPE_BURPEE, "EXERCISE_SEGMENT_TYPE_BURPEE", "other"),
    segment(ExerciseSegment.EXERCISE_SEGMENT_TYPE_CRUNCH, "EXERCISE_SEGMENT_TYPE_CRUNCH", "strength-training"),
    segment(ExerciseSegment.EXERCISE_SEGMENT_TYPE_DEADLIFT, "EXERCISE_SEGMENT_TYPE_DEADLIFT", "strength-training"),
    segment(
        ExerciseSegment.EXERCISE_SEGMENT_TYPE_DOUBLE_ARM_TRICEPS_EXTENSION,
        "EXERCISE_SEGMENT_TYPE_DOUBLE_ARM_TRICEPS_EXTENSION",
        "strength-training",
    ),
    segment(
        ExerciseSegment.EXERCISE_SEGMENT_TYPE_DUMBBELL_CURL_LEFT_ARM,
        "EXERCISE_SEGMENT_TYPE_DUMBBELL_CURL_LEFT_ARM",
        "strength-training",
    ),
    segment(
        ExerciseSegment.EXERCISE_SEGMENT_TYPE_DUMBBELL_CURL_RIGHT_ARM,
        "EXERCISE_SEGMENT_TYPE_DUMBBELL_CURL_RIGHT_ARM",
        "strength-training",
    ),
    segment(
        ExerciseSegment.EXERCISE_SEGMENT_TYPE_DUMBBELL_FRONT_RAISE,
        "EXERCISE_SEGMENT_TYPE_DUMBBELL_FRONT_RAISE",
        "strength-training",
    ),
    segment(
        ExerciseSegment.EXERCISE_SEGMENT_TYPE_DUMBBELL_LATERAL_RAISE,
        "EXERCISE_SEGMENT_TYPE_DUMBBELL_LATERAL_RAISE",
        "strength-training",
    ),
    segment(ExerciseSegment.EXERCISE_SEGMENT_TYPE_DUMBBELL_ROW, "EXERCISE_SEGMENT_TYPE_DUMBBELL_ROW", "strength-training"),
    segment(
        ExerciseSegment.EXERCISE_SEGMENT_TYPE_DUMBBELL_TRICEPS_EXTENSION_LEFT_ARM,
        "EXERCISE_SEGMENT_TYPE_DUMBBELL_TRICEPS_EXTENSION_LEFT_ARM",
        "strength-training",
    ),
    segment(
        ExerciseSegment.EXERCISE_SEGMENT_TYPE_DUMBBELL_TRICEPS_EXTENSION_RIGHT_ARM,
        "EXERCISE_SEGMENT_TYPE_DUMBBELL_TRICEPS_EXTENSION_RIGHT_ARM",
        "strength-training",
    ),
    segment(
        ExerciseSegment.EXERCISE_SEGMENT_TYPE_DUMBBELL_TRICEPS_EXTENSION_TWO_ARM,
        "EXERCISE_SEGMENT_TYPE_DUMBBELL_TRICEPS_EXTENSION_TWO_ARM",
        "strength-training",
    ),
    segment(ExerciseSegment.EXERCISE_SEGMENT_TYPE_ELLIPTICAL, "EXERCISE_SEGMENT_TYPE_ELLIPTICAL", "elliptical"),
    segment(ExerciseSegment.EXERCISE_SEGMENT_TYPE_FORWARD_TWIST, "EXERCISE_SEGMENT_TYPE_FORWARD_TWIST", "other"),
    segment(ExerciseSegment.EXERCISE_SEGMENT_TYPE_FRONT_RAISE, "EXERCISE_SEGMENT_TYPE_FRONT_RAISE", "strength-training"),
    segment(
        ExerciseSegment.EXERCISE_SEGMENT_TYPE_HIGH_INTENSITY_INTERVAL_TRAINING,
        "EXERCISE_SEGMENT_TYPE_HIGH_INTENSITY_INTERVAL_TRAINING",
        "high-intensity-interval-training",
    ),
    segment(ExerciseSegment.EXERCISE_SEGMENT_TYPE_HIP_THRUST, "EXERCISE_SEGMENT_TYPE_HIP_THRUST", "strength-training"),
    segment(ExerciseSegment.EXERCISE_SEGMENT_TYPE_HULA_HOOP, "EXERCISE_SEGMENT_TYPE_HULA_HOOP", "other"),
    segment(ExerciseSegment.EXERCISE_SEGMENT_TYPE_JUMPING_JACK, "EXERCISE_SEGMENT_TYPE_JUMPING_JACK", "other"),
    segment(ExerciseSegment.EXERCISE_SEGMENT_TYPE_JUMP_ROPE, "EXERCISE_SEGMENT_TYPE_JUMP_ROPE", "other"),
    segment(
        ExerciseSegment.EXERCISE_SEGMENT_TYPE_KETTLEBELL_SWING,
        "EXERCISE_SEGMENT_TYPE_KETTLEBELL_SWING",
        "strength-training",
    ),
    segment(
        ExerciseSegment.EXERCISE_SEGMENT_TYPE_LATERAL_RAISE,
        "EXERCISE_SEGMENT_TYPE_LATERAL_RAISE",
        "strength-training",
    ),
    segment(
        ExerciseSegment.EXERCISE_SEGMENT_TYPE_LAT_PULL_DOWN,
        "EXERCISE_SEGMENT_TYPE_LAT_PULL_DOWN",
        "strength-training",
    ),
    segment(ExerciseSegment.EXERCISE_SEGMENT_TYPE_LEG_CURL, "EXERCISE_SEGMENT_TYPE_LEG_CURL", "strength-training"),
    segment(
        ExerciseSegment.EXERCISE_SEGMENT_TYPE_LEG_EXTENSION,
        "EXERCISE_SEGMENT_TYPE_LEG_EXTENSION",
        "strength-training",
    ),
    segment(ExerciseSegment.EXERCISE_SEGMENT_TYPE_LEG_PRESS, "EXERCISE_SEGMENT_TYPE_LEG_PRESS", "strength-training"),
    segment(ExerciseSegment.EXERCISE_SEGMENT_TYPE_LEG_RAISE, "EXERCISE_SEGMENT_TYPE_LEG_RAISE", "strength-training"),
    segment(ExerciseSegment.EXERCISE_SEGMENT_TYPE_LUNGE, "EXERCISE_SEGMENT_TYPE_LUNGE", "strength-training"),
    segment(ExerciseSegment.EXERCISE_SEGMENT_TYPE_MOUNTAIN_CLIMBER, "EXERCISE_SEGMENT_TYPE_MOUNTAIN_CLIMBER", "other"),
    segment(ExerciseSegment.EXERCISE_SEGMENT_TYPE_PILATES, "EXERCISE_SEGMENT_TYPE_PILATES", "pilates"),
    segment(ExerciseSegment.EXERCISE_SEGMENT_TYPE_PLANK, "EXERCISE_SEGMENT_TYPE_PLANK", "other"),
    segment(ExerciseSegment.EXERCISE_SEGMENT_TYPE_PULL_UP, "EXERCISE_SEGMENT_TYPE_PULL_UP", "strength-training"),
    segment(ExerciseSegment.EXERCISE_SEGMENT_TYPE_PUNCH, "EXERCISE_SEGMENT_TYPE_PUNCH", "other"),
    segment(ExerciseSegment.EXERCISE_SEGMENT_TYPE_ROWING_MACHINE, "EXERCISE_SEGMENT_TYPE_ROWING_MACHINE", "rowing"),
    segment(ExerciseSegment.EXERCISE_SEGMENT_TYPE_RUNNING, "EXERCISE_SEGMENT_TYPE_RUNNING", "running"),
    segment(ExerciseSegment.EXERCISE_SEGMENT_TYPE_RUNNING_TREADMILL, "EXERCISE_SEGMENT_TYPE_RUNNING_TREADMILL", "running"),
    segment(
        ExerciseSegment.EXERCISE_SEGMENT_TYPE_SHOULDER_PRESS,
        "EXERCISE_SEGMENT_TYPE_SHOULDER_PRESS",
        "strength-training",
    ),
    segment(
        ExerciseSegment.EXERCISE_SEGMENT_TYPE_SINGLE_ARM_TRICEPS_EXTENSION,
        "EXERCISE_SEGMENT_TYPE_SINGLE_ARM_TRICEPS_EXTENSION",
        "strength-training",
    ),
    segment(ExerciseSegment.EXERCISE_SEGMENT_TYPE_SIT_UP, "EXERCISE_SEGMENT_TYPE_SIT_UP", "strength-training"),
    segment(ExerciseSegment.EXERCISE_SEGMENT_TYPE_SQUAT, "EXERCISE_SEGMENT_TYPE_SQUAT", "strength-training"),
    segment(ExerciseSegment.EXERCISE_SEGMENT_TYPE_STAIR_CLIMBING, "EXERCISE_SEGMENT_TYPE_STAIR_CLIMBING", "stair-climbing"),
    segment(
        ExerciseSegment.EXERCISE_SEGMENT_TYPE_STAIR_CLIMBING_MACHINE,
        "EXERCISE_SEGMENT_TYPE_STAIR_CLIMBING_MACHINE",
        "stair-climbing",
    ),
    segment(ExerciseSegment.EXERCISE_SEGMENT_TYPE_STRETCHING, "EXERCISE_SEGMENT_TYPE_STRETCHING", "other"),
    segment(
        ExerciseSegment.EXERCISE_SEGMENT_TYPE_SWIMMING_BACKSTROKE,
        "EXERCISE_SEGMENT_TYPE_SWIMMING_BACKSTROKE",
        "swimming",
    ),
    segment(
        ExerciseSegment.EXERCISE_SEGMENT_TYPE_SWIMMING_BREASTSTROKE,
        "EXERCISE_SEGMENT_TYPE_SWIMMING_BREASTSTROKE",
        "swimming",
    ),
    segment(
        ExerciseSegment.EXERCISE_SEGMENT_TYPE_SWIMMING_BUTTERFLY,
        "EXERCISE_SEGMENT_TYPE_SWIMMING_BUTTERFLY",
        "swimming",
    ),
    segment(
        ExerciseSegment.EXERCISE_SEGMENT_TYPE_SWIMMING_FREESTYLE,
        "EXERCISE_SEGMENT_TYPE_SWIMMING_FREESTYLE",
        "swimming",
    ),
    segment(ExerciseSegment.EXERCISE_SEGMENT_TYPE_SWIMMING_MIXED, "EXERCISE_SEGMENT_TYPE_SWIMMING_MIXED", "swimming"),
    segment(
        ExerciseSegment.EXERCISE_SEGMENT_TYPE_SWIMMING_OPEN_WATER,
        "EXERCISE_SEGMENT_TYPE_SWIMMING_OPEN_WATER",
        "swimming",
    ),
    segment(ExerciseSegment.EXERCISE_SEGMENT_TYPE_SWIMMING_OTHER, "EXERCISE_SEGMENT_TYPE_SWIMMING_OTHER", "swimming"),
    segment(ExerciseSegment.EXERCISE_SEGMENT_TYPE_SWIMMING_POOL, "EXERCISE_SEGMENT_TYPE_SWIMMING_POOL", "swimming"),
    segment(ExerciseSegment.EXERCISE_SEGMENT_TYPE_UPPER_TWIST, "EXERCISE_SEGMENT_TYPE_UPPER_TWIST", "other"),
    segment(ExerciseSegment.EXERCISE_SEGMENT_TYPE_WALKING, "EXERCISE_SEGMENT_TYPE_WALKING", "walking"),
    segment(
        ExerciseSegment.EXERCISE_SEGMENT_TYPE_WEIGHTLIFTING,
        "EXERCISE_SEGMENT_TYPE_WEIGHTLIFTING",
        "strength-training",
    ),
    segment(ExerciseSegment.EXERCISE_SEGMENT_TYPE_WHEELCHAIR, "EXERCISE_SEGMENT_TYPE_WHEELCHAIR", "other"),
    segment(ExerciseSegment.EXERCISE_SEGMENT_TYPE_YOGA, "EXERCISE_SEGMENT_TYPE_YOGA", "yoga"),
)

/** Absorbs the exact AndroidX exercise vocabularies into the shared Grove workout terminology. */
internal object HealthConnectWorkoutVocabulary {
    /** Health Connect states a lap structurally rather than as an enumerated token. */
    const val LAP_TOKEN = "EXERCISE_LAP"
    const val LAP_CODE = "lap"

    /** Every token a workout-segment output identity may name, laps included. */
    val segmentIdentityTokens: Set<String> =
        EXERCISE_SEGMENT_TYPES.values.mapTo(mutableSetOf()) { it.sourceToken } + LAP_TOKEN

    fun activity(exerciseType: Int): SourceCodedValue = EXERCISE_TYPES[exerciseType]
        ?.coded(SHARED_ACTIVITY_DISPLAYS)
        ?: throw InvalidHealthConnectRecord("Unsupported Health Connect exercise type: $exerciseType")

    fun segment(segmentType: Int): WorkoutClassification = EXERCISE_SEGMENT_TYPES[segmentType]
        ?.let { WorkoutClassification(it.sharedSystem(), it.coded(SHARED_SEGMENT_DISPLAYS + SHARED_ACTIVITY_DISPLAYS)) }
        ?: throw InvalidHealthConnectRecord("Unsupported Health Connect exercise segment type: $segmentType")

    fun lap(): WorkoutClassification = WorkoutClassification(
        HealthConnectContract.GROVE_WORKOUT_SEGMENT_TYPE,
        SourceCodedValue(
            LAP_CODE,
            SHARED_SEGMENT_DISPLAYS.getValue(LAP_CODE),
            LAP_TOKEN,
            "Exercise lap",
        ),
    )
}

private fun WorkoutCoding.coded(displays: Map<String, String>): SourceCodedValue =
    SourceCodedValue(sharedCode, displays.getValue(sharedCode), sourceToken, sourceDisplay())

private fun WorkoutCoding.sharedSystem(): String = if (sharedCode in SHARED_SEGMENT_DISPLAYS) {
    HealthConnectContract.GROVE_WORKOUT_SEGMENT_TYPE
} else {
    HealthConnectContract.GROVE_WORKOUT_ACTIVITY
}

private fun WorkoutCoding.sourceDisplay(): String = sourceToken
    .substringAfter("TYPE_")
    .lowercase()
    .replace('_', ' ')
    .replaceFirstChar(Char::uppercaseChar)
