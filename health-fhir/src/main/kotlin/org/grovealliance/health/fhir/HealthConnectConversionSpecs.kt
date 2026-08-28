//
// This source file belongs to the My Heart Counts Android project
//
// SPDX-FileCopyrightText: 2026 Stanford University and the project authors (see CONTRIBUTORS.md)
//
// SPDX-License-Identifier: MIT

package org.grovealliance.health.fhir

import androidx.health.connect.client.records.NutritionRecord
import org.hl7.fhir.r4.model.Coding
import java.math.BigDecimal
import java.time.Instant

/** One catalog-fixed UCUM system/code pair for an Observation Quantity. */
internal data class QuantitySemantics(
    val system: String,
    val code: String,
)

/** Representational limits stated by one catalog Quantity, independent of plausibility policy. */
internal data class QuantityValueDomain(
    val minimum: BigDecimal? = BigDecimal.ZERO,
    val maximum: BigDecimal? = null,
    val integerOnly: Boolean = false,
) {
    init {
        require(minimum == null || maximum == null || minimum <= maximum) {
            "A Quantity value domain cannot have a minimum above its maximum."
        }
    }

    fun requireValue(value: BigDecimal, field: String): BigDecimal {
        val violation = when {
            minimum != null && value < minimum -> "must be greater than or equal to $minimum"
            maximum != null && value > maximum -> "must be less than or equal to $maximum"
            integerOnly && value.stripTrailingZeros().scale() > 0 -> "must be an integer"
            else -> null
        }
        if (violation != null) throw InvalidHealthConnectRecord("$field $violation.")
        return value
    }

    companion object {
        val UNBOUNDED = QuantityValueDomain(minimum = null)
    }
}

/** The shapes the converter states one output in: a quantity, a series sample, a coded value. */
internal data class MobileQuantitySpec(
    val profile: String,
    val category: String?,
    val codeSystem: String,
    val code: String,
    val display: String,
    val unitCode: String,
    val unitDisplay: String,
    val valueDomain: QuantityValueDomain = QuantityValueDomain(),
    val adapterSpecific: Boolean = false,
) {
    val measurement: String = profile.measurementId()
}

internal data class SeriesSample(
    val time: Instant,
    val value: Double,
)

/** Binds duplicate coordinates to their exact source-list slot before any output sorting. */
internal fun <T, K> assignSourceListOccurrences(
    values: List<T>,
    coordinate: (T) -> K,
): List<Pair<T, Int>> {
    val occurrences = mutableMapOf<K, Int>()
    return values.map { value ->
        val key = coordinate(value)
        val occurrence = occurrences.getOrDefault(key, 0)
        occurrences[key] = occurrence + 1
        value to occurrence
    }
}

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
) {
    val measurement: String = profile.measurementId()
}

private fun String.measurementId(): String =
    substringAfterLast('/').removePrefix("grove-mobile-").removePrefix("health-connect-")

internal data class BloodGlucoseDefinition(
    val measurement: String,
    val profile: String,
    val loinc: String,
    val loincDisplay: String,
    val specimenSourceCode: String,
    val specimenType: Coding,
)
