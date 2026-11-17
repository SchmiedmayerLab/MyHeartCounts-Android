//
// This source file is part of the My Heart Counts Android open-source project
//
// SPDX-FileCopyrightText: 2026 Stanford University and the project authors (see CONTRIBUTORS.md)
//
// SPDX-License-Identifier: MIT

package edu.stanford.myheartcounts.demographics

import edu.stanford.myheartcounts.MHCStrings
import edu.stanford.myheartcounts.standard.MeasurementUnits
import edu.stanford.myheartcounts.standard.MeasurementUnitsProvider
import org.grovealliance.ui.StringResource
import java.math.RoundingMode
import kotlin.math.roundToInt

/**
 * Maps height and weight between their stored metric values and the participant's units, both for
 * reading and for typing them in. An entry is the list of texts typed into the fields of its sheet,
 * in order: one field, or feet and then inches for a height in feet and inches.
 */
interface DemographicsMeasurementMapper {

    /**
     * The height [heightInCm], in the participant's units.
     */
    fun mapHeightText(heightInCm: Double): StringResource

    /**
     * The weight [weightInKg], in the participant's units.
     */
    fun mapWeightText(weightInKg: Double): StringResource

    /**
     * The entry a height sheet starts from: [heightInCm] in the participant's units, or empty fields
     * when it is unanswered.
     */
    fun mapHeightEntry(heightInCm: Double?): List<String>

    /**
     * The entry a weight sheet starts from: [weightInKg] in the participant's units, or an empty field
     * when it is unanswered.
     */
    fun mapWeightEntry(weightInKg: Double?): List<String>

    /**
     * The height typed in as [entry], in centimetres, or `null` when it is not a number within the
     * accepted range.
     */
    fun mapHeightInCm(entry: List<String>): Double?

    /**
     * The weight typed in as [entry], in kilograms, or `null` when it is not a number within the
     * accepted range.
     */
    fun mapWeightInKg(entry: List<String>): Double?
}

/**
 * Default [DemographicsMeasurementMapper] implementation, in the units of [unitsProvider]. Values are
 * stored as converted and shown with at most two decimals.
 */
class DemographicsMeasurementMapperImpl(
    private val unitsProvider: MeasurementUnitsProvider,
) : DemographicsMeasurementMapper {

    override fun mapHeightText(heightInCm: Double): StringResource = when (unitsProvider.currentUnits.height) {
        MeasurementUnits.Height.Centimetres -> StringResource(MHCStrings.demographics_value_cm, roundedText(value = heightInCm))
        MeasurementUnits.Height.FeetAndInches -> {
            val (feet, inches) = feetAndInches(heightInCm = heightInCm)
            StringResource(MHCStrings.demographics_value_feet_inches, feet, inches)
        }
    }

    override fun mapWeightText(weightInKg: Double): StringResource = when (unitsProvider.currentUnits.weight) {
        MeasurementUnits.Weight.Kilograms -> StringResource(MHCStrings.demographics_value_kg, roundedText(value = weightInKg))
        MeasurementUnits.Weight.Pounds ->
            StringResource(MHCStrings.demographics_value_lb, roundedText(value = weightInKg / KG_PER_POUND))
    }

    override fun mapHeightEntry(heightInCm: Double?): List<String> = when (unitsProvider.currentUnits.height) {
        MeasurementUnits.Height.Centimetres -> listOf(heightInCm?.let { roundedText(value = it) }.orEmpty())
        MeasurementUnits.Height.FeetAndInches -> heightInCm
            ?.let { feetAndInches(heightInCm = it) }
            ?.let { (feet, inches) -> listOf(feet.toString(), inches.toString()) }
            ?: listOf("", "")
    }

    override fun mapWeightEntry(weightInKg: Double?): List<String> = listOf(
        when (unitsProvider.currentUnits.weight) {
            MeasurementUnits.Weight.Kilograms -> weightInKg?.let { roundedText(value = it) }
            MeasurementUnits.Weight.Pounds -> weightInKg?.let { roundedText(value = it / KG_PER_POUND) }
        }.orEmpty(),
    )

    override fun mapHeightInCm(entry: List<String>): Double? = when (unitsProvider.currentUnits.height) {
        MeasurementUnits.Height.Centimetres -> decimal(text = entry.firstOrNull())?.takeIf { it >= MIN_CM && it < MAX_CM }
        MeasurementUnits.Height.FeetAndInches -> {
            val feet = entry.getOrNull(0)?.trim()?.toIntOrNull()?.takeIf { it in FEET_RANGE }
            val inchesText = entry.getOrNull(1)?.trim().orEmpty()
            val inches = if (inchesText.isEmpty()) 0 else inchesText.toIntOrNull()?.takeIf { it in INCHES_RANGE }
            if (feet == null || inches == null) null else (feet * INCHES_PER_FOOT + inches) * CM_PER_INCH
        }
    }

    override fun mapWeightInKg(entry: List<String>): Double? {
        val value = decimal(text = entry.firstOrNull()) ?: return null
        return when (unitsProvider.currentUnits.weight) {
            MeasurementUnits.Weight.Kilograms -> value.takeIf { it >= MIN_KG && it < MAX_KG }
            MeasurementUnits.Weight.Pounds -> value.takeIf { it >= MIN_LB && it < MAX_LB }?.let { it * KG_PER_POUND }
        }
    }

    private fun feetAndInches(heightInCm: Double): Pair<Int, Int> {
        val totalInches = (heightInCm / CM_PER_INCH).roundToInt()
        return totalInches / INCHES_PER_FOOT to totalInches % INCHES_PER_FOOT
    }

    private fun decimal(text: String?) = text
        ?.trim()
        ?.replace(oldChar = ',', newChar = '.')
        ?.toDoubleOrNull()

    private fun roundedText(value: Double) = value
        .toBigDecimal()
        .setScale(DECIMALS, RoundingMode.HALF_UP)
        .stripTrailingZeros()
        .toPlainString()

    private companion object {
        const val INCHES_PER_FOOT = 12
        const val CM_PER_INCH = 2.54
        const val KG_PER_POUND = 0.45359237
        const val DECIMALS = 2
        const val MIN_CM = 90.0
        const val MAX_CM = 250.0
        const val MIN_KG = 25.0
        const val MAX_KG = 450.0
        const val MIN_LB = 55.0
        const val MAX_LB = 1000.0
        val FEET_RANGE = 3..8
        val INCHES_RANGE = 0..11
    }
}
