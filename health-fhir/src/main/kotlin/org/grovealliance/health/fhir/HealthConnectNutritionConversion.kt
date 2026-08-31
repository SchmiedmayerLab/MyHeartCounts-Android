//
// This source file is part of the My Heart Counts Android open-source project
//
// SPDX-FileCopyrightText: 2026 Stanford University and the project authors (see CONTRIBUTORS.md)
//
// SPDX-License-Identifier: MIT

package org.grovealliance.health.fhir

import androidx.health.connect.client.records.NutritionRecord
import org.grovealliance.health.RecordType
import org.hl7.fhir.r4.model.DateTimeType
import org.hl7.fhir.r4.model.Period
import java.time.Instant

internal fun HealthConnectConverter.convertNutrition(
    record: NutritionRecord,
    convertedAt: Instant,
    eventSequence: EventSequence,
): HealthConnectConversion {
    if (!record.startTime.isBefore(record.endTime)) {
        throw InvalidHealthConnectRecord("NutritionRecord must have a positive interval.")
    }
    val source = sourceIdentity(record.metadata, RecordType.nutrition.identifier)
    val resolvedContext = context.resolve(
        record.metadata,
        synchronizationScope.identityKey,
        bundleIdentifier(eventSequence),
    )
    val observations = NUTRIENT_OUTPUTS.mapNotNull { nutrient ->
        val value = nutrient.extract(record) ?: return@mapNotNull null
        val decimal = value.fhirDecimal(nutrient.spec.display, nutrient.spec.valueDomain)
        baseObservation(
            record.metadata,
            source,
            HealthConnectIdentity.nutrientOutput(
                synchronizationScope.identityKey,
                source,
                nutrient.measurement,
            ),
            resolvedContext,
        ).apply {
            claimProfile(nutrient.spec)
            code = concept(nutrient.spec.codeSystem, nutrient.spec.code, nutrient.spec.display)
            effective = Period().apply {
                startElement = DateTimeType(
                    record.startTime.fhirDateTime(record.startZoneOffset, "Nutrition start time"),
                )
                endElement = DateTimeType(
                    record.endTime.fhirDateTime(record.endZoneOffset, "Nutrition end time"),
                )
            }
            this.value = quantity(decimal, nutrient.spec.unitCode, nutrient.spec.unitDisplay)
        }
    }
    return conversion(
        record.metadata,
        RecordType.nutrition.identifier,
        source,
        observations,
        convertedAt,
        eventSequence,
        resolvedContext,
    )
}
