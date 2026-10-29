//
// This source file is part of the My Heart Counts Android open-source project
//
// SPDX-FileCopyrightText: 2026 Stanford University and the project authors (see CONTRIBUTORS.md)
//
// SPDX-License-Identifier: MIT

package edu.stanford.myheartcounts.customsamples

import edu.stanford.myheartcounts.model.HealthUnit
import org.grovealliance.scheduler.InstantRange
import java.util.UUID

/**
 * A kind of value that is not recorded by the health platform but derived from the participant's
 * survey answers or entered by them, and stored with their other health observations.
 *
 * @property id the identifier naming the kind of value in its stored observations
 * @property unit the unit values of this kind are reported in
 */
enum class CustomQuantitySampleType(
    val id: String,
    val unit: HealthUnit,
) {
    /**
     * LDL cholesterol.
     */
    BloodLipids(id = "MHCCustomSampleTypeBloodLipidMeasurement", unit = HealthUnit.MilligramPerDeciliter),

    /**
     * The diet score computed from the diet survey.
     */
    DietScore(id = "MHCCustomSampleTypeDietMEPAScore", unit = HealthUnit.Count),

    /**
     * The well-being score computed from the WHO-5 survey.
     */
    MentalWellbeingScore(id = "MHCCustomSampleTypeWHO5Score", unit = HealthUnit.Count),

    /**
     * The nicotine exposure category computed from the nicotine survey.
     */
    NicotineExposure(id = "MHCCustomSampleTypeNicotineExposure", unit = HealthUnit.Count),
}

/**
 * One value of a [CustomQuantitySampleType].
 *
 * @property value the value, in the unit of its [type]
 * @property timeRange the time the value applies to; empty for a value taken at one instant
 */
data class CustomQuantitySample(
    val id: UUID,
    val type: CustomQuantitySampleType,
    val value: Double,
    val timeRange: InstantRange,
)
