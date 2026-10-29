//
// This source file is part of the My Heart Counts Android open-source project
//
// SPDX-FileCopyrightText: 2026 Stanford University and the project authors (see CONTRIBUTORS.md)
//
// SPDX-License-Identifier: MIT

package edu.stanford.myheartcounts.healthstats

import androidx.health.connect.client.aggregate.AggregateMetric
import androidx.health.connect.client.records.ActiveCaloriesBurnedRecord
import androidx.health.connect.client.records.BloodPressureRecord
import androidx.health.connect.client.records.DistanceRecord
import androidx.health.connect.client.records.ExerciseSessionRecord
import androidx.health.connect.client.records.FloorsClimbedRecord
import androidx.health.connect.client.records.HeartRateRecord
import androidx.health.connect.client.records.HeightRecord
import androidx.health.connect.client.records.Record
import androidx.health.connect.client.records.RestingHeartRateRecord
import androidx.health.connect.client.records.SleepSessionRecord
import androidx.health.connect.client.records.StepsRecord
import androidx.health.connect.client.records.WeightRecord
import edu.stanford.myheartcounts.model.HealthUnit
import org.grovealliance.core.coroutines.runCatchingAsync
import org.grovealliance.health.AggregationInterval
import org.grovealliance.health.Health
import org.grovealliance.health.HealthQueryTimeRange
import org.grovealliance.scheduler.InstantRange
import java.time.Duration
import java.time.Instant
import kotlin.math.pow

/**
 * A complete monthly snapshot and the device records it represents.
 */
data class HealthStatsCollectedMonth(
    val entries: List<HealthStatsEntry>,
    val recordIds: Set<String>,
)

/**
 * Collects the device data needed for one metric's monthly contribution.
 */
interface HealthStatsMonthCollector {
    suspend fun collect(metric: HealthStatsSyncMetric, month: HealthStatsMonth): Result<HealthStatsCollectedMonth>
}

class HealthStatsMonthCollectorImpl(
    private val health: Health,
    private val workoutMapper: HealthStatsWorkoutMapper,
    private val configuration: HealthStatsSyncConfiguration,
) : HealthStatsMonthCollector {
    override suspend fun collect(
        metric: HealthStatsSyncMetric,
        month: HealthStatsMonth,
    ): Result<HealthStatsCollectedMonth> = runCatchingAsync {
        when (metric) {
            HealthStatsSyncMetric.Sleep -> sleep(month = month)
            HealthStatsSyncMetric.BodyMassIndex -> bodyMassIndex(month = month)
            else -> collectStandard(metric = metric, month = month)
        }
    }

    private suspend fun collectStandard(metric: HealthStatsSyncMetric, month: HealthStatsMonth): HealthStatsCollectedMonth {
        val records = health.query(type = metric.recordTypes.first(), timeRange = queryRange(range = month.timeRange))
        val entries = when (metric) {
            HealthStatsSyncMetric.Steps -> sum(metric = StepsRecord.COUNT_TOTAL, month = month, unit = HealthUnit.Count) { it.toDouble() }
            HealthStatsSyncMetric.ExerciseTime -> sum(
                metric = ExerciseSessionRecord.EXERCISE_DURATION_TOTAL,
                month = month,
                unit = HealthUnit.Minute,
            ) { seconds(duration = it) / SECONDS_PER_MINUTE }
            HealthStatsSyncMetric.ActiveEnergy -> sum(
                metric = ActiveCaloriesBurnedRecord.ACTIVE_CALORIES_TOTAL,
                month = month,
                unit = HealthUnit.Kilocalorie,
            ) { it.inKilocalories }
            HealthStatsSyncMetric.Distance -> sum(
                metric = DistanceRecord.DISTANCE_TOTAL,
                month = month,
                unit = HealthUnit.Meter,
            ) { it.inMeters }
            HealthStatsSyncMetric.FlightsClimbed -> sum(
                metric = FloorsClimbedRecord.FLOORS_CLIMBED_TOTAL,
                month = month,
                unit = HealthUnit.Count,
            ) { it }
            HealthStatsSyncMetric.HeartRate -> heartRate(month = month)
            else -> records.mapNotNull { point(record = it, range = month.timeRange) }
        }
        return HealthStatsCollectedMonth(
            entries = entries,
            recordIds = records.filter { belongs(record = it, range = month.timeRange) }.map { it.metadata.id }.toSet(),
        )
    }

    private suspend fun <T : Any> sum(
        metric: AggregateMetric<T>,
        month: HealthStatsMonth,
        unit: HealthUnit,
        value: (T) -> Double,
    ): List<HealthStatsEntry.Aggregate> = health.aggregate(
        metric = metric,
        timeRange = queryRange(range = month.timeRange),
        interval = AggregationInterval.hourly,
    ).mapNotNull { bucket ->
        bucket.value?.let {
            HealthStatsEntry.Aggregate(
                start = bucket.start,
                end = bucket.end,
                unit = unit,
                values = HealthStatsEntry.Aggregate.Values.Sum(sum = value(it)),
            )
        }
    }

    private suspend fun heartRate(month: HealthStatsMonth): List<HealthStatsEntry.Aggregate> {
        val range = queryRange(range = month.timeRange)
        val minima = health.aggregate(metric = HeartRateRecord.BPM_MIN, timeRange = range, interval = AggregationInterval.hourly)
            .associateBy { it.start }
        val maxima = health.aggregate(metric = HeartRateRecord.BPM_MAX, timeRange = range, interval = AggregationInterval.hourly)
            .associateBy { it.start }
        return health.aggregate(metric = HeartRateRecord.BPM_AVG, timeRange = range, interval = AggregationInterval.hourly)
            .mapNotNull { bucket ->
                val min = minima[bucket.start]?.value ?: return@mapNotNull null
                val max = maxima[bucket.start]?.value ?: return@mapNotNull null
                val average = bucket.value ?: return@mapNotNull null
                HealthStatsEntry.Aggregate(
                    start = bucket.start,
                    end = bucket.end,
                    unit = HealthUnit.CountPerMinute,
                    values = HealthStatsEntry.Aggregate.Values.MinMaxAvg(
                        min = min.toDouble(),
                        max = max.toDouble(),
                        avg = average.toDouble(),
                        average = null,
                    ),
                )
            }
    }

    private fun point(record: Record, range: InstantRange): HealthStatsEntry? {
        val date = record.statsRange()?.start ?: return null
        if (date < range.start || date >= range.endExclusive) return null
        return when (record) {
            is WeightRecord -> quantity(date = date, unit = HealthUnit.Kilogram, value = record.weight.inKilograms)
            is HeightRecord -> quantity(date = date, unit = HealthUnit.Centimeter, value = record.height.inMeters * CENTIMETERS_PER_METER)
            is RestingHeartRateRecord -> quantity(date = date, unit = HealthUnit.CountPerMinute, value = record.beatsPerMinute.toDouble())
            is BloodPressureRecord -> HealthStatsEntry.BloodPressure(
                date = date,
                unit = HealthUnit.MillimeterOfMercury,
                systolic = record.systolic.inMillimetersOfMercury,
                diastolic = record.diastolic.inMillimetersOfMercury,
            )
            is ExerciseSessionRecord -> workoutMapper.map(record = record)
            else -> null
        }
    }

    private suspend fun bodyMassIndex(month: HealthStatsMonth): HealthStatsCollectedMonth {
        val weights = health.query(type = org.grovealliance.health.RecordType.weight, timeRange = queryRange(range = month.timeRange))
        val height = health.mostRecent(type = org.grovealliance.health.RecordType.height)
        val entries = if (height == null || height.height.inMeters <= 0) {
            emptyList()
        } else {
            weights.filter { it.time >= month.timeRange.start && it.time < month.timeRange.endExclusive }.map {
                quantity(date = it.time, unit = HealthUnit.Count, value = it.weight.inKilograms / height.height.inMeters.pow(2))
            }
        }
        return HealthStatsCollectedMonth(
            entries = entries,
            recordIds = weights.map { it.metadata.id }.toSet() + listOfNotNull(height?.metadata?.id),
        )
    }

    private suspend fun sleep(month: HealthStatsMonth): HealthStatsCollectedMonth {
        val padded = InstantRange(
            start = month.timeRange.start.minus(SLEEP_MARGIN),
            endExclusive = month.timeRange.endExclusive.plus(SLEEP_MARGIN),
        )
        val records = health.query(
            type = org.grovealliance.health.RecordType.sleepSession,
            timeRange = queryRange(range = padded),
            source = configuration.sleepDataOrigin,
        ).sortedBy { it.startTime }
        val groups = mutableListOf<MutableList<SleepSessionRecord>>()
        records.forEach { record ->
            val group = groups.lastOrNull()
            if (group != null && record.startTime < group.maxOf { it.endTime }.plus(SLEEP_SESSION_GAP)) {
                group += record
            } else {
                groups += mutableListOf(record)
            }
        }
        val entries = mutableListOf<HealthStatsEntry>()
        val ids = mutableSetOf<String>()
        groups.forEach { group ->
            val start = group.minOf { it.startTime }
            val end = group.maxOf { it.endTime }
            val midpoint = start.plus(Duration.between(start, end).dividedBy(2))
            if (midpoint >= month.timeRange.start && midpoint < month.timeRange.endExclusive) {
                val duration = health.aggregate(
                    metric = SleepSessionRecord.SLEEP_DURATION_TOTAL,
                    timeRange = HealthQueryTimeRange(
                        start = start,
                        end = end,
                    ),
                    source = configuration.sleepDataOrigin,
                )
                if (duration != null) {
                    entries += HealthStatsEntry.Aggregate(
                        start = start,
                        end = end,
                        unit = HealthUnit.Hour,
                        values = HealthStatsEntry.Aggregate.Values.Sum(sum = seconds(duration = duration) / SECONDS_PER_HOUR),
                    )
                    ids += group.map { it.metadata.id }
                }
            }
        }
        return HealthStatsCollectedMonth(
            entries = entries,
            recordIds = ids,
        )
    }

    private fun belongs(record: Record, range: InstantRange): Boolean {
        val times = record.statsRange() ?: return false
        return if (times.isEmpty) {
            times.start >= range.start && times.start < range.endExclusive
        } else {
            times.start < range.endExclusive && times.endExclusive > range.start
        }
    }

    private fun quantity(date: Instant, unit: HealthUnit, value: Double) = HealthStatsEntry.Quantity(
        date = date,
        unit = unit,
        value = value,
    )

    private fun queryRange(range: InstantRange) = HealthQueryTimeRange(
        start = range.start,
        end = range.endExclusive,
    )

    private fun seconds(duration: Duration): Double = duration.seconds + duration.nano / NANOS_PER_SECOND

    private companion object {
        const val SECONDS_PER_MINUTE = 60.0
        const val SECONDS_PER_HOUR = 3600.0
        const val NANOS_PER_SECOND = 1_000_000_000.0
        const val CENTIMETERS_PER_METER = 100.0
        val SLEEP_MARGIN: Duration = Duration.ofDays(1)
        val SLEEP_SESSION_GAP: Duration = Duration.ofMinutes(60)
    }
}
