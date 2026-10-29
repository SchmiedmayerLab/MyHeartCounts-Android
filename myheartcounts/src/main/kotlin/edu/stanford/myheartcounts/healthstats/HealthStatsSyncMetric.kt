//
// This source file is part of the My Heart Counts Android open-source project
//
// SPDX-FileCopyrightText: 2026 Stanford University and the project authors (see CONTRIBUTORS.md)
//
// SPDX-License-Identifier: MIT

package edu.stanford.myheartcounts.healthstats

import edu.stanford.myheartcounts.firebase.FirebaseSchema
import org.grovealliance.health.AnyRecordType
import org.grovealliance.health.RecordType

/**
 * A metric the device can contribute to monthly statistics.
 */
enum class HealthStatsSyncMetric(val id: String) {
    Steps(id = FirebaseSchema.StatsMetric.STEPS),
    ExerciseTime(id = FirebaseSchema.StatsMetric.EXERCISE_TIME),
    ActiveEnergy(id = FirebaseSchema.StatsMetric.ACTIVE_ENERGY),
    Distance(id = FirebaseSchema.StatsMetric.WALKING_RUNNING_DISTANCE),
    FlightsClimbed(id = FirebaseSchema.StatsMetric.FLIGHTS_CLIMBED),
    HeartRate(id = FirebaseSchema.StatsMetric.HEART_RATE),
    Sleep(id = FirebaseSchema.StatsMetric.SLEEP),
    BloodPressure(id = FirebaseSchema.StatsMetric.BLOOD_PRESSURE),
    Weight(id = FirebaseSchema.StatsMetric.WEIGHT),
    Height(id = FirebaseSchema.StatsMetric.HEIGHT),
    BodyMassIndex(id = FirebaseSchema.StatsMetric.BMI),
    RestingHeartRate(id = FirebaseSchema.StatsMetric.RESTING_HEART_RATE),
    Workouts(id = FirebaseSchema.StatsMetric.WORKOUTS),
    ;

    val collection: HealthStatsEntryCollection
        get() = when (this) {
            Steps, ExerciseTime, ActiveEnergy, Distance, FlightsClimbed, HeartRate -> HealthStatsEntryCollection.Hourly
            Sleep -> HealthStatsEntryCollection.Sessions
            else -> HealthStatsEntryCollection.Samples
        }

    val recordTypes: List<AnyRecordType>
        get() = when (this) {
            Steps -> listOf(RecordType.steps)
            ExerciseTime, Workouts -> listOf(RecordType.exerciseSession)
            ActiveEnergy -> listOf(RecordType.activeCaloriesBurned)
            Distance -> listOf(RecordType.distance)
            FlightsClimbed -> listOf(RecordType.floorsClimbed)
            HeartRate -> listOf(RecordType.heartRate)
            Sleep -> listOf(RecordType.sleepSession)
            BloodPressure -> listOf(RecordType.bloodPressure)
            Weight -> listOf(RecordType.weight)
            Height -> listOf(RecordType.height)
            BodyMassIndex -> listOf(RecordType.weight, RecordType.height)
            RestingHeartRate -> listOf(RecordType.restingHeartRate)
        }
}
