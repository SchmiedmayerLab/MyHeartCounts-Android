//
// This source file is part of the My Heart Counts Android open-source project
//
// SPDX-FileCopyrightText: 2026 Stanford University and the project authors (see CONTRIBUTORS.md)
// SPDX-License-Identifier: MIT
//

package edu.stanford.myheartcounts.dashboard.entry

import androidx.health.connect.client.records.BloodGlucoseRecord
import androidx.health.connect.client.records.BloodPressureRecord
import androidx.health.connect.client.records.HeightRecord
import androidx.health.connect.client.records.Record
import androidx.health.connect.client.records.WeightRecord
import androidx.health.connect.client.records.metadata.Metadata
import androidx.health.connect.client.units.BloodGlucose
import androidx.health.connect.client.units.Length
import androidx.health.connect.client.units.Mass
import androidx.health.connect.client.units.Pressure
import edu.stanford.myheartcounts.dashboard.HeartScoreComponent
import org.grovealliance.core.time.TimeProvider

/**
 * Converts manual measurements to native records with stable identities for safe retries.
 */
class HeartScoreRecordMapper(
    private val timeProvider: TimeProvider,
) {
    fun records(entry: HeartScoreEntry): List<Record> {
        val offset = entry.date.atZone(timeProvider.currentZone()).offset
        return when (entry) {
            is HeartScoreEntry.Quantity -> if (entry.component == HeartScoreComponent.BloodGlucose) {
                listOf(BloodGlucoseRecord(
                    time = entry.date,
                    zoneOffset = offset,
                    level = BloodGlucose.milligramsPerDeciliter(entry.value),
                    relationToMeal = BloodGlucoseRecord.RELATION_TO_MEAL_FASTING,
                    metadata = Metadata.manualEntry(clientRecordId = entry.id),
                ))
            } else {
                emptyList()
            }
            is HeartScoreEntry.Pressure -> listOf(BloodPressureRecord(
                time = entry.date,
                zoneOffset = offset,
                systolic = Pressure.millimetersOfMercury(entry.systolic),
                diastolic = Pressure.millimetersOfMercury(entry.diastolic),
                metadata = Metadata.manualEntry(clientRecordId = entry.id),
            ))
            is HeartScoreEntry.BodyMassIndex -> if (entry.heightMeters != null && entry.weightKg != null) {
                listOf(
                    HeightRecord(
                        time = entry.date,
                        zoneOffset = offset,
                        height = Length.meters(entry.heightMeters),
                        metadata = Metadata.manualEntry(clientRecordId = "${entry.id}-height"),
                    ),
                    WeightRecord(
                        time = entry.date,
                        zoneOffset = offset,
                        weight = Mass.kilograms(entry.weightKg),
                        metadata = Metadata.manualEntry(clientRecordId = "${entry.id}-weight"),
                    ),
                )
            } else {
                emptyList()
            }
        }
    }
}
