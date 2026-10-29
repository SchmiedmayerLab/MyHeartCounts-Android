//
// This source file is part of the My Heart Counts Android open-source project
//
// SPDX-FileCopyrightText: 2026 Stanford University and the project authors (see CONTRIBUTORS.md)
//
// SPDX-License-Identifier: MIT

package edu.stanford.myheartcounts.dashboard

import androidx.health.connect.client.records.BloodGlucoseRecord
import androidx.health.connect.client.records.metadata.Metadata
import androidx.health.connect.client.units.BloodGlucose
import edu.stanford.myheartcounts.customsamples.CustomQuantitySample
import edu.stanford.myheartcounts.customsamples.CustomQuantitySampleType
import edu.stanford.myheartcounts.healthstats.BloodPressureStatsSample
import edu.stanford.myheartcounts.healthstats.HealthStatsDiagnostic
import edu.stanford.myheartcounts.healthstats.HealthStatsQuantitySample
import edu.stanford.myheartcounts.healthstats.HealthStatsSnapshot
import edu.stanford.myheartcounts.healthstats.SleepSessionStatsSample
import org.grovealliance.account.AccountDetails
import org.grovealliance.account.AccountIdKey
import org.grovealliance.scheduler.InstantRange
import java.time.Instant
import java.util.UUID

object HeartHealthScoreFixtures {
    val NOW: Instant = Instant.parse("2026-01-04T12:00:00Z")
    val RANGE = InstantRange(
        start = NOW.minusSeconds(3600),
        endExclusive = NOW,
    )

    fun input() = HeartHealthScoreInput(
        exerciseRange = RANGE,
        exercise = emptyList(),
        steps = null,
        sleep = emptyList(),
        bodyMassIndex = emptyList(),
        weight = emptyList(),
        height = emptyList(),
        bloodPressure = emptyList(),
        custom = emptyMap(),
        glucose = null,
        usesAsianBodyMassIndex = false,
    )

    fun quantity(value: Double, date: Instant = NOW) = HealthStatsQuantitySample(
        timeRange = InstantRange(
            start = date,
            endExclusive = date,
        ),
        value = value,
    )

    fun sleep(value: Double, date: Instant = NOW) = SleepSessionStatsSample(
        timeRange = InstantRange(
            start = date.minusSeconds(3600),
            endExclusive = date,
        ),
        hoursAsleep = value,
    )

    fun pressure(systolic: Double, diastolic: Double, date: Instant = NOW) = BloodPressureStatsSample(
        date = date,
        systolic = systolic,
        diastolic = diastolic,
    )

    fun custom(type: CustomQuantitySampleType, value: Double, date: Instant = NOW) = CustomQuantitySample(
        id = UUID.nameUUIDFromBytes(type.id.toByteArray()),
        type = type,
        value = value,
        timeRange = InstantRange(
            start = date,
            endExclusive = date,
        ),
    )

    fun glucose(value: Double, date: Instant = NOW) = HeartHealthScoreInput.Glucose(
        value = value,
        measuredAt = date,
    )

    fun <T> snapshot(elements: List<T>, diagnostics: List<HealthStatsDiagnostic> = emptyList()) = HealthStatsSnapshot(
        elements = elements,
        diagnostics = diagnostics,
        contributingSourceIds = setOf("healthKit"),
        isFromCache = false,
        hasPendingWrites = false,
    )

    fun glucoseRecord(millimolesPerLiter: Double) = BloodGlucoseRecord(
        time = NOW,
        zoneOffset = null,
        level = BloodGlucose.millimolesPerLiter(millimolesPerLiter),
        metadata = Metadata.manualEntry(),
    )

    fun account(id: String = "participant") = AccountDetails().apply { this[AccountIdKey::class] = id }

    fun componentInput(component: HeartScoreComponent, value: Double): HeartHealthScoreInput = when (component) {
        HeartScoreComponent.ExerciseMinutes -> input().copy(exercise = listOf(quantity(value = value)))
        HeartScoreComponent.StepCount -> input().copy(steps = snapshot(elements = listOf(quantity(value = value))))
        HeartScoreComponent.Sleep -> input().copy(sleep = listOf(sleep(value = value)))
        HeartScoreComponent.BodyMassIndex -> input().copy(bodyMassIndex = listOf(quantity(value = value)))
        HeartScoreComponent.BloodGlucose -> input().copy(glucose = glucose(value = value))
        else -> {
            val type = when (component) {
                HeartScoreComponent.Diet -> CustomQuantitySampleType.DietScore
                HeartScoreComponent.MentalWellbeing -> CustomQuantitySampleType.MentalWellbeingScore
                HeartScoreComponent.BloodLipids -> CustomQuantitySampleType.BloodLipids
                else -> CustomQuantitySampleType.NicotineExposure
            }
            input().copy(custom = mapOf(type to listOf(custom(type = type, value = value))))
        }
    }
}
