//
// This source file is part of the My Heart Counts Android open-source project
//
// SPDX-FileCopyrightText: 2026 Stanford University and the project authors (see CONTRIBUTORS.md)
//
// SPDX-License-Identifier: MIT

@file:OptIn(kotlinx.coroutines.ExperimentalCoroutinesApi::class)

package edu.stanford.myheartcounts.dashboard.detail

import edu.stanford.myheartcounts.customsamples.CustomQuantitySampleType
import edu.stanford.myheartcounts.customsamples.CustomSampleSource
import edu.stanford.myheartcounts.dashboard.HeartScoreComponent
import edu.stanford.myheartcounts.healthstats.HealthStatsAggregation
import edu.stanford.myheartcounts.healthstats.HealthStatsAggregationInterval
import edu.stanford.myheartcounts.healthstats.HealthStatsMetric
import edu.stanford.myheartcounts.healthstats.HealthStatsRequest
import edu.stanford.myheartcounts.healthstats.HealthStatsStore
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.isActive
import org.grovealliance.account.Account
import org.grovealliance.account.accountId
import org.grovealliance.core.coroutines.runCatchingAsync
import org.grovealliance.core.time.TimeProvider
import org.grovealliance.health.Health
import org.grovealliance.health.HealthQueryTimeRange
import org.grovealliance.health.RecordType
import org.grovealliance.scheduler.InstantRange
import java.time.Duration
import java.time.temporal.ChronoUnit
import kotlin.time.Duration.Companion.seconds

/**
 * Supplies chart history for the signed-in participant and selected calendar range.
 */
interface HeartScoreChartSource {

    /**
     * The history of [component] over [selection], each time it changes; empty while nobody is
     * signed in and for factors without a chart. Values are grouped into days, two-day periods or
     * weeks as the range grows, and steps always into days. Custom observations retain their dates
     * and original values; sleep sessions ending on the same day are added together.
     */
    fun updates(component: HeartScoreComponent, selection: HeartScoreChartRange): Flow<Result<HeartScoreChartData>>
}

class HeartScoreChartSourceImpl(
    private val stats: HealthStatsStore,
    private val customSamples: CustomSampleSource,
    private val health: Health,
    private val account: Account,
    private val ranges: HeartScoreDetailRanges,
    private val timeProvider: TimeProvider,
) : HeartScoreChartSource {
    override fun updates(
        component: HeartScoreComponent,
        selection: HeartScoreChartRange,
    ): Flow<Result<HeartScoreChartData>> =
        combine(account.details.map { it?.accountId }.distinctUntilChanged(), ranges.changes) { id, _ -> id }
            .flatMapLatest { id ->
                val range = ranges.range(selection = selection)
                if (id == null) {
                    flowOf(Result.success(data(range = range, points = emptyList())))
                } else {
                    history(component = component, range = range)
                }
            }

    private fun history(component: HeartScoreComponent, range: InstantRange): Flow<Result<HeartScoreChartData>> = when (component) {
        HeartScoreComponent.Diet, HeartScoreComponent.NicotineExposure -> flowOf(Result.success(data(range = range, points = emptyList())))
        HeartScoreComponent.Sleep -> stats.updates(request = HealthStatsRequest.SleepSessions(timeRange = range)).map { result ->
            result.map { snapshot ->
                data(range = range, points = snapshot.elements.groupBy {
                    it.timeRange.endExclusive.atZone(timeProvider.currentZone()).toLocalDate()
                }.map { (day, sessions) ->
                    HeartScoreChartPoint(
                        timeRange = InstantRange(
                            start = day.atStartOfDay(timeProvider.currentZone()).toInstant(),
                            endExclusive = day.plusDays(1).atStartOfDay(timeProvider.currentZone()).toInstant(),
                        ),
                        value = sessions.sumOf { it.hoursAsleep },
                    )
                })
            }
        }
        HeartScoreComponent.BloodPressure -> pressure(range = range)
        HeartScoreComponent.BloodGlucose -> glucose(range = range)
        HeartScoreComponent.MentalWellbeing -> custom(type = CustomQuantitySampleType.MentalWellbeingScore, range = range)
        HeartScoreComponent.BloodLipids -> custom(type = CustomQuantitySampleType.BloodLipids, range = range)
        else -> quantity(component = component, range = range)
    }

    private fun quantity(component: HeartScoreComponent, range: InstantRange): Flow<Result<HeartScoreChartData>> {
        val metric = when (component) {
            HeartScoreComponent.ExerciseMinutes -> HealthStatsMetric.ExerciseTime
            HeartScoreComponent.StepCount -> HealthStatsMetric.Steps
            else -> HealthStatsMetric.BodyMassIndex
        }
        val days = bucketDays(range = range, daily = component == HeartScoreComponent.StepCount)
        return stats.updates(request = HealthStatsRequest.Quantity(
            metric = metric,
            timeRange = range,
            aggregation = if (component == HeartScoreComponent.BodyMassIndex) HealthStatsAggregation.Avg else HealthStatsAggregation.Sum,
            interval = HealthStatsAggregationInterval(
                length = days,
                unit = ChronoUnit.DAYS,
                anchor = range.start,
                alignmentPolicy = HealthStatsAggregationInterval.AlignmentPolicy.Approximate,
            ),
        )).map { result ->
            result.map { snapshot ->
                data(range = range, points = snapshot.elements.map {
                    HeartScoreChartPoint(
                        timeRange = it.timeRange,
                        value = it.value,
                    )
                })
            }
        }
    }

    private fun pressure(range: InstantRange): Flow<Result<HeartScoreChartData>> =
        stats.updates(request = HealthStatsRequest.BloodPressure(timeRange = range)).map { result ->
            result.map { snapshot ->
                HeartScoreChartData(
                    timeRange = range,
                    series = listOf(HeartScoreChartSeries.Kind.Systolic, HeartScoreChartSeries.Kind.Diastolic).map { kind ->
                        HeartScoreChartSeries(
                            kind = kind,
                            points = snapshot.elements.map {
                                HeartScoreChartPoint(
                                    timeRange = InstantRange(
                                        start = it.date,
                                        endExclusive = it.date,
                                    ),
                                    value = if (kind == HeartScoreChartSeries.Kind.Systolic) it.systolic else it.diastolic,
                                )
                            },
                        )
                    },
                )
            }
        }

    private fun custom(type: CustomQuantitySampleType, range: InstantRange): Flow<Result<HeartScoreChartData>> =
        customSamples.samples(type = type, timeRange = range).map { result ->
            result.map { samples ->
                data(range = range, points = samples.map {
                    HeartScoreChartPoint(
                        timeRange = it.timeRange,
                        value = it.value,
                    )
                })
            }
        }

    private fun glucose(range: InstantRange): Flow<Result<HeartScoreChartData>> = flow {
        while (currentCoroutineContext().isActive) {
            emit(runCatchingAsync {
                val records = health.query(
                    type = RecordType.bloodGlucose,
                    timeRange = HealthQueryTimeRange(
                        start = range.start,
                        end = range.endExclusive,
                    ),
                )
                grouped(range = range, points = records.map {
                    HeartScoreChartPoint(
                        timeRange = InstantRange(
                            start = it.time,
                            endExclusive = it.time,
                        ),
                        value = it.level.inMilligramsPerDeciliter,
                    )
                })
            })
            delay(GLUCOSE_REFRESH)
        }
    }

    private fun grouped(range: InstantRange, points: List<HeartScoreChartPoint>): HeartScoreChartData {
        val zone = timeProvider.currentZone()
        val firstDay = range.start.atZone(zone).toLocalDate()
        val days = bucketDays(range = range, daily = false)
        val grouped = points.filter { it.value.isFinite() && it.timeRange.start >= range.start && it.timeRange.start < range.endExclusive }
            .groupBy { ChronoUnit.DAYS.between(firstDay, it.timeRange.start.atZone(zone).toLocalDate()) / days }
            .toSortedMap().map { (index, samples) ->
                val start = firstDay.plusDays(index * days)
                HeartScoreChartPoint(
                    timeRange = InstantRange(
                        start = start.atStartOfDay(zone).toInstant(),
                        endExclusive = minOf(start.plusDays(days).atStartOfDay(zone).toInstant(), range.endExclusive),
                    ),
                    value = samples.map { it.value }.average(),
                )
            }
        return data(range = range, points = grouped)
    }

    private fun data(range: InstantRange, points: List<HeartScoreChartPoint>) = HeartScoreChartData(
        timeRange = range,
        series = listOf(HeartScoreChartSeries(
            kind = HeartScoreChartSeries.Kind.Value,
            points = points.sortedBy { it.timeRange.start },
        )),
    )

    private fun bucketDays(range: InstantRange, daily: Boolean): Long = when {
        daily || Duration.between(range.start, range.endExclusive).toDays() <= FORTNIGHT_DAYS -> 1
        Duration.between(range.start, range.endExclusive).toDays() <= MONTH_DAYS -> 2
        else -> WEEK_DAYS
    }

    private companion object {
        const val FORTNIGHT_DAYS = 14
        const val MONTH_DAYS = 31
        const val WEEK_DAYS = 7L
        val GLUCOSE_REFRESH = 15.seconds
    }
}
