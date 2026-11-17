//
// This source file is part of the My Heart Counts Android open-source project
//
// SPDX-FileCopyrightText: 2026 Stanford University and the project authors (see CONTRIBUTORS.md)
//
// SPDX-License-Identifier: MIT

package edu.stanford.myheartcounts.standard

import edu.stanford.myheartcounts.firebase.FirebaseSchema
import java.util.Locale

/**
 * Provides the units body measurements are shown and entered in for the participant.
 */
interface MeasurementUnitsProvider {

    /**
     * The units for the participant's measurement system as of now.
     */
    val currentUnits: MeasurementUnits
}

/**
 * The units body measurements are shown and entered in. Stored values are always metric.
 *
 * @param height the unit heights are shown and entered in
 * @param weight the unit weights are shown and entered in
 */
data class MeasurementUnits(
    val height: Height,
    val weight: Weight,
) {

    /**
     * A unit for heights.
     */
    enum class Height {
        Centimetres,
        FeetAndInches,
    }

    /**
     * A unit for weights.
     */
    enum class Weight {
        Kilograms,
        Pounds,
    }

    companion object {
        /**
         * Centimetres and kilograms.
         */
        val METRIC = MeasurementUnits(
            height = Height.Centimetres,
            weight = Weight.Kilograms,
        )

        /**
         * Feet and inches, and pounds: the US customary system.
         */
        val US = MeasurementUnits(
            height = Height.FeetAndInches,
            weight = Weight.Pounds,
        )

        /**
         * Centimetres and pounds: the UK system.
         */
        val UK = MeasurementUnits(
            height = Height.Centimetres,
            weight = Weight.Pounds,
        )
    }
}

/**
 * Default [MeasurementUnitsProvider] implementation. Follows the measurement system of the device
 * locale: heights in feet and inches for the US system, weights in pounds for the US and UK systems,
 * and metric units otherwise.
 */
class MeasurementUnitsProviderImpl : MeasurementUnitsProvider {

    override val currentUnits: MeasurementUnits
        get() = units(system = MHCEnvironmentTracker.measurementSystem(locale = Locale.getDefault()))

    private fun units(system: String) = when (system) {
        FirebaseSchema.MeasurementSystem.US -> MeasurementUnits.US
        FirebaseSchema.MeasurementSystem.UK -> MeasurementUnits.UK
        else -> MeasurementUnits.METRIC
    }
}
