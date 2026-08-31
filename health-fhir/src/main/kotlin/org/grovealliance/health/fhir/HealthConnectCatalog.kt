//
// This source file is part of the My Heart Counts Android open-source project
//
// SPDX-FileCopyrightText: 2026 Stanford University and the project authors (see CONTRIBUTORS.md)
//
// SPDX-License-Identifier: MIT

package org.grovealliance.health.fhir

import org.grovealliance.health.RecordType

/** Closed source-type inventory admitted by the current Health Connect FHIR producer. */
internal object HealthConnectCatalog {
    /** Exact AndroidX 1.1.0 inventory surfaced by this version of Grove Health. */
    val allRecordTypeIdentifiers: Set<String> = RecordType.all.mapTo(mutableSetOf()) { it.identifier }

    /** Exact source inventory that is deliberately not emitted yet. */
    val deferredRecordTypeIdentifiers: Set<String> = setOf(
        RecordType.plannedExerciseSession.identifier,
    )

    /** Every AndroidX source type this producer emits for; a new type is supported until deferred. */
    val supportedRecordTypeIdentifiers: Set<String> = allRecordTypeIdentifiers - deferredRecordTypeIdentifiers

    /** Supported source types whose successful conversion may legitimately produce no outputs. */
    val zeroOutputRecordTypeIdentifiers: Set<String> = setOf(
        RecordType.cyclingPedalingCadence.identifier,
        RecordType.heartRate.identifier,
        RecordType.nutrition.identifier,
        RecordType.power.identifier,
        RecordType.skinTemperature.identifier,
        RecordType.speed.identifier,
        RecordType.stepsCadence.identifier,
    )

    init {
        check(deferredRecordTypeIdentifiers.all { it in allRecordTypeIdentifiers }) {
            "A deferred source type must name an AndroidX Health Connect Record type."
        }
        check(zeroOutputRecordTypeIdentifiers.all { it in supportedRecordTypeIdentifiers }) {
            "Only a supported Health Connect source type may claim zero-output conversions."
        }
    }
}
