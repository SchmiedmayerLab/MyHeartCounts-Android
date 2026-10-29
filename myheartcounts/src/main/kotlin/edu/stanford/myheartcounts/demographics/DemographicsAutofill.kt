//
// This source file is part of the My Heart Counts Android open-source project
//
// SPDX-FileCopyrightText: 2026 Stanford University and the project authors (see CONTRIBUTORS.md)
//
// SPDX-License-Identifier: MIT

package edu.stanford.myheartcounts.demographics

import edu.stanford.myheartcounts.standard.health.HealthAccess
import org.grovealliance.core.coroutines.runCatchingAsync
import org.grovealliance.health.Health
import org.grovealliance.health.RecordType

/**
 * The most recent body measurements found in health data; `null` where none was found.
 */
data class DemographicsMeasurements(
    val heightInCm: Double?,
    val weightInKg: Double?,
)

/**
 * Reads the participant's most recent height and weight from Health Connect.
 */
interface DemographicsAutofill {

    /**
     * Asks for Health Connect access, then reads the most recent height and weight. Succeeds when
     * at least one of them could be read, even if none was recorded.
     */
    suspend fun read(): Result<DemographicsMeasurements>
}

/**
 * Default [DemographicsAutofill] implementation.
 */
class DemographicsAutofillImpl(
    private val health: Health,
    private val healthAccess: HealthAccess,
) : DemographicsAutofill {

    override suspend fun read(): Result<DemographicsMeasurements> {
        runCatchingAsync { healthAccess.requestPermissions() }.onFailure { return Result.failure(it) }
        val height = runCatchingAsync {
            health.mostRecent(type = RecordType.height)?.height?.inMeters?.times(CENTIMETERS_PER_METER)
        }
        val weight = runCatchingAsync { health.mostRecent(type = RecordType.weight)?.weight?.inKilograms }
        if (weight.isFailure) height.onFailure { return Result.failure(it) }
        return Result.success(
            DemographicsMeasurements(
                heightInCm = height.getOrNull(),
                weightInKg = weight.getOrNull(),
            ),
        )
    }

    private companion object {
        const val CENTIMETERS_PER_METER = 100
    }
}
