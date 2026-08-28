//
// This source file belongs to the My Heart Counts Android project
//
// SPDX-FileCopyrightText: 2026 Stanford University and the project authors (see CONTRIBUTORS.md)
//
// SPDX-License-Identifier: MIT

package org.grovealliance.health.fhir

import androidx.health.connect.client.records.NutritionRecord
import org.hl7.fhir.r4.model.Coding
import java.time.Instant

/** The shapes the converter states one output in: a quantity, a series sample, a coded value. */
internal data class MobileQuantitySpec(
    val profile: String,
    val category: String?,
    val codeSystem: String,
    val code: String,
    val display: String,
    val unitCode: String,
    val unitDisplay: String,
    val minimum: Double? = 0.0,
    val adapterSpecific: Boolean = false,
)

internal data class SeriesSample(
    val time: Instant,
    val value: Double,
)

internal data class NutrientSpec(
    val measurement: String,
    val spec: MobileQuantitySpec,
    val extract: (NutritionRecord) -> Double?,
)

internal data class SourceCodedValue(
    val sharedCode: String,
    val sharedDisplay: String,
    val sourceCode: String,
    val sourceDisplay: String,
)

internal data class MobileCodedSpec(
    val profile: String,
    val category: String?,
    val code: String,
    val display: String,
    val codeSystem: String = HealthConnectContract.GROVE_MOBILE_MEASUREMENT,
    val adapterSpecific: Boolean = false,
)

internal data class BloodGlucoseDefinition(
    val profile: String,
    val loinc: String,
    val loincDisplay: String,
    val specimenSourceCode: String,
    val specimenType: Coding,
)
