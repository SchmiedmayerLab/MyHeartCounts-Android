//
// This source file is part of the My Heart Counts Android open-source project
//
// SPDX-FileCopyrightText: 2026 Stanford University and the project authors (see CONTRIBUTORS.md)
//
// SPDX-License-Identifier: MIT

package edu.stanford.myheartcounts.healthstats

import edu.stanford.myheartcounts.firebase.FirebaseSchema
import edu.stanford.myheartcounts.model.HealthUnit
/**
 * A quantity measured over time whose statistics are kept per participant and month.
 *
 * Sleep sessions, blood pressure, workouts and electrocardiograms have statistics of their own
 * shape and are read through their dedicated [HealthStatsRequest]s instead.
 *
 * @property id the stable identifier naming the metric in a statistics document and its location
 * @property unit the unit values of this metric are reported in
 */
enum class HealthStatsMetric(
    val id: String,
    val unit: HealthUnit,
) {
    Steps(id = FirebaseSchema.StatsMetric.STEPS, unit = HealthUnit.Count),
    ExerciseTime(id = FirebaseSchema.StatsMetric.EXERCISE_TIME, unit = HealthUnit.Minute),
    ActiveEnergy(id = FirebaseSchema.StatsMetric.ACTIVE_ENERGY, unit = HealthUnit.Kilocalorie),
    WalkingRunningDistance(id = FirebaseSchema.StatsMetric.WALKING_RUNNING_DISTANCE, unit = HealthUnit.Meter),
    FlightsClimbed(id = FirebaseSchema.StatsMetric.FLIGHTS_CLIMBED, unit = HealthUnit.Count),
    HeartRate(id = FirebaseSchema.StatsMetric.HEART_RATE, unit = HealthUnit.CountPerMinute),
    RestingHeartRate(id = FirebaseSchema.StatsMetric.RESTING_HEART_RATE, unit = HealthUnit.CountPerMinute),
    Weight(id = FirebaseSchema.StatsMetric.WEIGHT, unit = HealthUnit.Kilogram),
    Height(id = FirebaseSchema.StatsMetric.HEIGHT, unit = HealthUnit.Centimeter),
    BodyMassIndex(id = FirebaseSchema.StatsMetric.BMI, unit = HealthUnit.Count),
}
