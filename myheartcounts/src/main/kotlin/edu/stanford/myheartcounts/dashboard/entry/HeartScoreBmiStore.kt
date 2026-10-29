//
// This source file is part of the My Heart Counts Android open-source project
//
// SPDX-FileCopyrightText: 2026 Stanford University and the project authors (see CONTRIBUTORS.md)
// SPDX-License-Identifier: MIT
//

package edu.stanford.myheartcounts.dashboard.entry

import com.google.firebase.firestore.FieldPath
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.SetOptions
import edu.stanford.myheartcounts.firebase.FirebaseSchema
import edu.stanford.myheartcounts.firebase.MHCFirestore
import kotlinx.coroutines.tasks.await
import org.grovealliance.core.coroutines.runCatchingAsync
import org.grovealliance.core.time.TimeProvider
import java.time.YearMonth
import java.time.format.DateTimeFormatter
import java.time.temporal.ChronoUnit

/**
 * Preserves directly entered BMI independently of native-derived statistics.
 */
interface HeartScoreBmiStore {
    suspend fun save(entry: HeartScoreEntry.BodyMassIndex): Result<Unit>
}

class HeartScoreBmiStoreImpl(
    private val firestore: MHCFirestore,
    private val timeProvider: TimeProvider,
) : HeartScoreBmiStore {
    override suspend fun save(entry: HeartScoreEntry.BodyMassIndex): Result<Unit> = runCatchingAsync {
        val date = entry.date.truncatedTo(ChronoUnit.SECONDS).atZone(timeProvider.currentZone())
        val month = YearMonth.from(date)
        val document = firestore.healthStatsMonths(metricId = FirebaseSchema.StatsMetric.BMI).document(month.toString())
        document.set(
            mapOf(
                FirebaseSchema.StatsDocument.VERSION to 0,
                FirebaseSchema.StatsDocument.METRIC to FirebaseSchema.StatsMetric.BMI,
                FirebaseSchema.StatsDocument.SAMPLES to mapOf(
                    FirebaseSchema.StatsSource.MANUAL_ENTRY to FieldValue.arrayUnion(
                        mapOf(
                            FirebaseSchema.StatsEntry.DATE to DateTimeFormatter.ISO_OFFSET_DATE_TIME.format(date),
                            FirebaseSchema.StatsEntry.UNIT to "count",
                            FirebaseSchema.StatsEntry.VALUE to entry.value,
                        ),
                    ),
                ),
            ),
            SetOptions.mergeFieldPaths(
                listOf(
                    FieldPath.of(FirebaseSchema.StatsDocument.VERSION),
                    FieldPath.of(FirebaseSchema.StatsDocument.METRIC),
                    FieldPath.of(FirebaseSchema.StatsDocument.SAMPLES, FirebaseSchema.StatsSource.MANUAL_ENTRY),
                ),
            ),
        ).await()
    }
}
