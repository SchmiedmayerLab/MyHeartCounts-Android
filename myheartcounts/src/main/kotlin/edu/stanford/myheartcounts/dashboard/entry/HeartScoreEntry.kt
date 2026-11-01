//
// This source file is part of the My Heart Counts Android open-source project
//
// SPDX-FileCopyrightText: 2026 Stanford University and the project authors (see CONTRIBUTORS.md)
// SPDX-License-Identifier: MIT
//

package edu.stanford.myheartcounts.dashboard.entry

import edu.stanford.myheartcounts.dashboard.HeartScoreComponent
import java.text.NumberFormat
import java.text.ParsePosition
import java.time.Instant
import java.time.LocalDateTime
import java.time.ZoneId
import java.util.Locale
import java.util.UUID

/**
 * A manually entered measurement, expressed in canonical units.
 */
sealed interface HeartScoreEntry {
    val id: String
    val date: Instant

    data class Quantity(
        override val id: String,
        override val date: Instant,
        val component: HeartScoreComponent,
        val value: Double,
    ) : HeartScoreEntry

    data class Pressure(
        override val id: String,
        override val date: Instant,
        val systolic: Double,
        val diastolic: Double,
    ) : HeartScoreEntry

    data class BodyMassIndex(
        override val id: String,
        override val date: Instant,
        val value: Double,
        val weightKg: Double?,
        val heightMeters: Double?,
    ) : HeartScoreEntry
}

/**
 * Editable values and units of a measurement form. Only complete, finite and in-range inputs
 * produce an [entry]; an identifier is retained across retries.
 */
data class HeartScoreEntryForm(
    val component: HeartScoreComponent,
    val date: LocalDateTime,
    val weightInPounds: Boolean,
    val heightInFeet: Boolean,
    val locale: Locale,
    val zone: ZoneId,
    val id: String = UUID.randomUUID().toString(),
    val value: String = "",
    val diastolic: String = "",
    val weight: String = "",
    val height: String = "",
    val inches: String = "",
    val directBmi: Boolean = false,
) {
    fun entry(): HeartScoreEntry? = when (component) {
        HeartScoreComponent.BloodPressure -> pressure(instant = date.atZone(zone).toInstant())
        HeartScoreComponent.BloodLipids, HeartScoreComponent.BloodGlucose -> quantity(instant = date.atZone(zone).toInstant())
        HeartScoreComponent.BodyMassIndex -> bmi(instant = date.atZone(zone).toInstant())
        else -> null
    }

    /**
     * The accepted range of [field], or `null` when any positive number is accepted.
     */
    fun limits(field: HeartScoreEntryAction.Field): Limits? = when (field) {
        HeartScoreEntryAction.Field.Value -> when (component) {
            HeartScoreComponent.BloodPressure -> SYSTOLIC
            HeartScoreComponent.BloodLipids -> LDL
            HeartScoreComponent.BloodGlucose -> GLUCOSE
            else -> null
        }
        HeartScoreEntryAction.Field.Diastolic -> DIASTOLIC
        HeartScoreEntryAction.Field.Weight -> if (weightInPounds) POUNDS else KILOGRAMS
        HeartScoreEntryAction.Field.Height -> if (heightInFeet) FEET else CENTIMETERS
        HeartScoreEntryAction.Field.Inches -> INCHES
    }

    /**
     * Whether [field] accepts only whole numbers.
     */
    fun acceptsWholeNumbersOnly(field: HeartScoreEntryAction.Field): Boolean = when (field) {
        HeartScoreEntryAction.Field.Value -> component in WHOLE_NUMBER_COMPONENTS
        HeartScoreEntryAction.Field.Diastolic, HeartScoreEntryAction.Field.Inches -> true
        HeartScoreEntryAction.Field.Height -> heightInFeet
        HeartScoreEntryAction.Field.Weight -> false
    }

    /**
     * The limits of [field] when its text is a number outside them; `null` while it is empty, unparsable or within them.
     */
    fun violatedLimits(field: HeartScoreEntryAction.Field): Limits? {
        val limits = limits(field = field) ?: return null
        val number = number(text = text(field = field)) ?: return null
        return limits.takeIf { number !in it }
    }

    private fun pressure(instant: Instant): HeartScoreEntry.Pressure? {
        val systolic = accepted(field = HeartScoreEntryAction.Field.Value) ?: return null
        val lower = accepted(field = HeartScoreEntryAction.Field.Diastolic) ?: return null
        return HeartScoreEntry.Pressure(
            id = id,
            date = instant,
            systolic = systolic,
            diastolic = lower,
        )
    }

    private fun quantity(instant: Instant): HeartScoreEntry.Quantity? = accepted(field = HeartScoreEntryAction.Field.Value)?.let {
        HeartScoreEntry.Quantity(
            id = id,
            date = instant,
            component = component,
            value = it,
        )
    }

    private fun bmi(instant: Instant): HeartScoreEntry.BodyMassIndex? {
        if (directBmi) {
            return accepted(field = HeartScoreEntryAction.Field.Value)?.let {
                HeartScoreEntry.BodyMassIndex(
                    id = id,
                    date = instant,
                    value = it,
                    weightKg = null,
                    heightMeters = null,
                )
            }
        }
        val kg = weightKg()
        val meters = heightMeters()
        return if (kg != null && meters != null) {
            HeartScoreEntry.BodyMassIndex(
                id = id,
                date = instant,
                value = kg / (meters * meters),
                weightKg = kg,
                heightMeters = meters,
            )
        } else {
            null
        }
    }

    private fun weightKg(): Double? {
        val weight = accepted(field = HeartScoreEntryAction.Field.Weight) ?: return null
        return if (weightInPounds) weight * KG_PER_POUND else weight
    }

    private fun heightMeters(): Double? {
        val height = accepted(field = HeartScoreEntryAction.Field.Height) ?: return null
        return if (heightInFeet) {
            accepted(field = HeartScoreEntryAction.Field.Inches)?.let { inches -> (height * INCHES_PER_FOOT + inches) * METERS_PER_INCH }
        } else {
            height / CENTIMETERS_PER_METER
        }
    }

    private fun accepted(field: HeartScoreEntryAction.Field): Double? {
        val number = number(text = text(field = field)) ?: return null
        val inRange = limits(field = field)?.let { number in it } ?: (number > 0)
        val whole = !acceptsWholeNumbersOnly(field = field) || number % 1.0 == 0.0
        return number.takeIf { inRange && whole }
    }

    /**
     * The entered text of [field].
     */
    fun text(field: HeartScoreEntryAction.Field): String = when (field) {
        HeartScoreEntryAction.Field.Value -> value
        HeartScoreEntryAction.Field.Diastolic -> diastolic
        HeartScoreEntryAction.Field.Weight -> weight
        HeartScoreEntryAction.Field.Height -> height
        HeartScoreEntryAction.Field.Inches -> inches
    }

    private fun number(text: String): Double? {
        val input = text.trim()
        val position = ParsePosition(0)
        val number = NumberFormat.getNumberInstance(locale).apply { isGroupingUsed = false }.parse(input, position)?.toDouble()
        return number?.takeIf { position.index == input.length && it.isFinite() }
    }

    /**
     * A range of accepted values, from [min] inclusive to [max] exclusive.
     */
    data class Limits(
        val min: Double,
        val max: Double,
    ) {
        operator fun contains(value: Double): Boolean = value >= min && value < max
    }

    private companion object {
        val SYSTOLIC = Limits(
            min = 60.0,
            max = 250.0,
        )
        val DIASTOLIC = Limits(
            min = 30.0,
            max = 150.0,
        )
        val LDL = Limits(
            min = 30.0,
            max = 400.0,
        )
        val GLUCOSE = Limits(
            min = 40.0,
            max = 400.0,
        )
        val KILOGRAMS = Limits(
            min = 25.0,
            max = 450.0,
        )
        val POUNDS = Limits(
            min = 55.0,
            max = 1000.0,
        )
        val CENTIMETERS = Limits(
            min = 90.0,
            max = 250.0,
        )
        val FEET = Limits(
            min = 3.0,
            max = 9.0,
        )
        val INCHES = Limits(
            min = 0.0,
            max = 12.0,
        )
        val WHOLE_NUMBER_COMPONENTS = setOf(
            HeartScoreComponent.BloodPressure,
            HeartScoreComponent.BloodLipids,
            HeartScoreComponent.BloodGlucose,
        )
        const val INCHES_PER_FOOT = 12
        const val METERS_PER_INCH = 0.0254
        const val KG_PER_POUND = 0.45359237
        const val CENTIMETERS_PER_METER = 100
    }
}
