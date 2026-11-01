//
// This source file is part of the My Heart Counts Android open-source project
//
// SPDX-FileCopyrightText: 2026 Stanford University and the project authors (see CONTRIBUTORS.md)
//
// SPDX-License-Identifier: MIT

package edu.stanford.myheartcounts.dashboard

import edu.stanford.myheartcounts.customsamples.CustomQuantitySampleType
import edu.stanford.myheartcounts.healthstats.HealthStatsDiagnostic
import edu.stanford.myheartcounts.healthstats.HealthStatsQuantitySample
import org.grovealliance.core.time.TimeProvider
import java.time.Duration
import java.time.Instant
import kotlin.math.pow
import kotlin.math.roundToLong

/**
 * Calculates component scores and the composite from the participant's observations.
 */
interface HeartHealthScoreCalculator {
    fun calculate(input: HeartHealthScoreInput): HeartHealthScores
}

class HeartHealthScoreCalculatorImpl(
    private val timeProvider: TimeProvider,
) : HeartHealthScoreCalculator {
    override fun calculate(input: HeartHealthScoreInput): HeartHealthScores {
        val exercise = exerciseScore(input = input)
        val sleep = input.sleep.maxByOrNull { it.timeRange.endExclusive }
        val pressure = input.bloodPressure.maxByOrNull { it.date }
        val components = listOf(
            exercise,
            result(component = HeartScoreComponent.Sleep, value = sleep?.hoursAsleep, date = sleep?.timeRange?.endExclusive),
            custom(input = input, type = CustomQuantitySampleType.DietScore, component = HeartScoreComponent.Diet),
            custom(input = input, type = CustomQuantitySampleType.MentalWellbeingScore, component = HeartScoreComponent.MentalWellbeing),
            HeartScoreResult(
                component = HeartScoreComponent.BloodPressure,
                score = pressure?.let { bloodPressure(systolic = it.systolic.toInt(), diastolic = it.diastolic.toInt()) },
                measuredAt = pressure?.date,
                measurement = pressure?.let {
                    HeartScoreMeasurement.BloodPressure(
                        systolic = it.systolic,
                        diastolic = it.diastolic,
                    )
                },
                usesAsianBodyMassIndex = false,
            ),
            custom(input = input, type = CustomQuantitySampleType.BloodLipids, component = HeartScoreComponent.BloodLipids),
            result(component = HeartScoreComponent.BloodGlucose, value = input.glucose?.value, date = input.glucose?.measuredAt),
            bodyMassIndex(input = input),
            custom(input = input, type = CustomQuantitySampleType.NicotineExposure, component = HeartScoreComponent.NicotineExposure),
        )
        val composite = components.filter { it.component != HeartScoreComponent.MentalWellbeing }.mapNotNull { it.score }
        return HeartHealthScores(
            overall = composite.takeIf { it.size >= MIN_COMPOSITE_COMPONENTS }?.average(),
            components = components,
        )
    }

    private fun exerciseScore(input: HeartHealthScoreInput): HeartScoreResult {
        val steps = input.steps
        val usesSteps = input.exercise.isEmpty() && !steps?.elements.isNullOrEmpty()
        val component = if (usesSteps) HeartScoreComponent.StepCount else HeartScoreComponent.ExerciseMinutes
        val value = when {
            !usesSteps -> input.exercise.takeIf { it.isNotEmpty() }?.sumOf { it.value }
            HealthStatsDiagnostic.UnalignedInterval in steps!!.diagnostics -> null
            else -> steps.elements.map { it.value }.average().roundToLong().toDouble()
        }
        return result(component = component, value = value, date = input.exerciseRange.endExclusive)
    }

    private fun custom(
        input: HeartHealthScoreInput,
        type: CustomQuantitySampleType,
        component: HeartScoreComponent,
    ): HeartScoreResult {
        val sample = input.custom[type]?.maxByOrNull { it.timeRange.start }
        return result(component = component, value = sample?.value, date = sample?.timeRange?.endExclusive)
    }

    private fun bodyMassIndex(input: HeartHealthScoreInput): HeartScoreResult {
        val bmi = input.bodyMassIndex.maxByOrNull { it.timeRange.endExclusive }
        val weight = input.weight.maxByOrNull { it.timeRange.endExclusive }
        val height = input.height.maxByOrNull { it.timeRange.endExclusive }
        val hasRecentWeight = weight != null &&
            Duration.between(weight.timeRange.endExclusive, timeProvider.nowInstant()) < MAX_WEIGHT_AGE
        val validHeight = height != null && height.value > 0
        val canDerive = (bmi != null || hasRecentWeight) && validHeight
        val derived = if (weight != null && height != null && canDerive) {
            HealthStatsQuantitySample(
                timeRange = if (weight.timeRange.endExclusive > height.timeRange.endExclusive) weight.timeRange else height.timeRange,
                value = weight.value / (height.value / CENTIMETERS_PER_METER).pow(2),
            )
        } else {
            null
        }
        val selected = when {
            bmi == null -> derived
            derived == null || bmi.timeRange.endExclusive > weight!!.timeRange.endExclusive -> bmi
            else -> derived
        }
        return result(
            component = HeartScoreComponent.BodyMassIndex,
            value = selected?.value,
            date = selected?.timeRange?.endExclusive,
            asianBodyMassIndex = input.usesAsianBodyMassIndex,
        )
    }

    private fun result(
        component: HeartScoreComponent,
        value: Double?,
        date: Instant?,
        asianBodyMassIndex: Boolean = false,
    ): HeartScoreResult {
        val score = value?.takeIf { it.isFinite() }?.let {
            score(component = component, value = it, asianBodyMassIndex = asianBodyMassIndex)
        }
        return HeartScoreResult(
            component = component,
            score = score,
            measuredAt = date.takeIf { score != null },
            measurement = value?.takeIf { score != null }?.let {
                HeartScoreMeasurement.Quantity(value = if (component == HeartScoreComponent.MentalWellbeing) it * WHO5_SCALE else it)
            },
            usesAsianBodyMassIndex = asianBodyMassIndex,
        )
    }

    private fun score(component: HeartScoreComponent, value: Double, asianBodyMassIndex: Boolean): Double? = when (component) {
        HeartScoreComponent.Diet -> DietScoreBand.of(value = value)?.score ?: 0.0
        HeartScoreComponent.ExerciseMinutes -> HeartScoreScale.ExerciseMinutes.score(value = value)
        HeartScoreComponent.StepCount -> HeartScoreScale.StepCount.score(value = value)
        HeartScoreComponent.Sleep -> HeartScoreScale.Sleep.score(value = value)
        HeartScoreComponent.BodyMassIndex -> if (asianBodyMassIndex) {
            HeartScoreScale.AsianBodyMassIndex.score(value = value)
        } else {
            HeartScoreScale.BodyMassIndex.score(value = value)
        }
        HeartScoreComponent.BloodLipids -> HeartScoreScale.BloodLipids.score(value = value)
        HeartScoreComponent.BloodGlucose -> HeartScoreScale.BloodGlucose.score(value = value)
        HeartScoreComponent.MentalWellbeing -> (value * WHO5_SCALE / PERCENT).coerceIn(0.0, 1.0)
        HeartScoreComponent.NicotineExposure -> NicotineExposureCategory.of(value = value)?.score
        HeartScoreComponent.BloodPressure -> null
    }

    private fun bloodPressure(systolic: Int, diastolic: Int): Double =
        HeartScoreScale.SystolicPressure.score(value = systolic.toDouble()) +
            HeartScoreScale.DiastolicPressure.score(value = diastolic.toDouble())

    private companion object {
        /**
         * The age past which a weight no longer counts towards a derived body mass index.
         */
        val MAX_WEIGHT_AGE: Duration = Duration.ofDays(365).dividedBy(2)

        /**
         * The factor turning a stored WHO-5 raw score into its 0–100 percentage.
         */
        const val WHO5_SCALE = 4

        /**
         * The points a full WHO-5 percentage is worth.
         */
        const val PERCENT = 100.0

        const val CENTIMETERS_PER_METER = 100.0

        /**
         * The fewest scored factors the overall score is calculated from.
         */
        const val MIN_COMPOSITE_COMPONENTS = 5
    }
}
