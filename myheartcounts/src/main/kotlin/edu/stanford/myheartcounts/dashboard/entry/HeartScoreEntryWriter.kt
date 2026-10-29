//
// This source file is part of the My Heart Counts Android open-source project
//
// SPDX-FileCopyrightText: 2026 Stanford University and the project authors (see CONTRIBUTORS.md)
// SPDX-License-Identifier: MIT
//

package edu.stanford.myheartcounts.dashboard.entry

import edu.stanford.myheartcounts.customsamples.CustomQuantitySample
import edu.stanford.myheartcounts.customsamples.CustomQuantitySampleType
import edu.stanford.myheartcounts.customsamples.CustomSampleUploader
import edu.stanford.myheartcounts.dashboard.HeartHealthScoreSource
import edu.stanford.myheartcounts.dashboard.HeartScoreComponent
import edu.stanford.myheartcounts.healthstats.HealthStatsSync
import org.grovealliance.core.coroutines.runCatchingAsync
import org.grovealliance.scheduler.InstantRange
import java.util.UUID

/**
 * Saves a manual measurement and refreshes the dashboard's inputs. Direct BMI readings are
 * preserved in monthly statistics because there is no native BMI record type.
 */
interface HeartScoreEntryWriter {
    suspend fun save(entry: HeartScoreEntry): Result<Unit>
}

class HeartScoreEntryWriterImpl(
    private val records: HeartScoreRecordStore,
    private val custom: CustomSampleUploader,
    private val bmiStore: HeartScoreBmiStore,
    private val statsSync: HealthStatsSync,
    private val scores: HeartHealthScoreSource,
) : HeartScoreEntryWriter {
    override suspend fun save(entry: HeartScoreEntry): Result<Unit> = runCatchingAsync {
        when {
            entry is HeartScoreEntry.Quantity && entry.component == HeartScoreComponent.BloodLipids -> custom.upload(
                samples = listOf(CustomQuantitySample(
                    id = UUID.fromString(entry.id),
                    type = CustomQuantitySampleType.BloodLipids,
                    value = entry.value,
                    timeRange = InstantRange(start = entry.date, endExclusive = entry.date),
                )),
            ).getOrThrow()
            entry is HeartScoreEntry.BodyMassIndex && entry.heightMeters == null -> bmiStore.save(entry = entry).getOrThrow()
            else -> {
                records.save(entry = entry).getOrThrow()
                // The statistics catch up in the background, so saving never waits for the server.
                statsSync.requestRefresh()
            }
        }
        scores.refresh()
    }
}
