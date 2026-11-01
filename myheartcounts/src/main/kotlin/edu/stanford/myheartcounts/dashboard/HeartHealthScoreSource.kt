//
// This source file is part of the My Heart Counts Android open-source project
//
// SPDX-FileCopyrightText: 2026 Stanford University and the project authors (see CONTRIBUTORS.md)
//
// SPDX-License-Identifier: MIT

package edu.stanford.myheartcounts.dashboard

import edu.stanford.myheartcounts.account.raceEthnicity
import edu.stanford.myheartcounts.customsamples.CustomQuantitySampleType
import edu.stanford.myheartcounts.customsamples.CustomSampleSource
import edu.stanford.myheartcounts.healthstats.HealthStatsAggregation
import edu.stanford.myheartcounts.healthstats.HealthStatsAggregationInterval
import edu.stanford.myheartcounts.healthstats.HealthStatsMetric
import edu.stanford.myheartcounts.healthstats.HealthStatsQuantitySample
import edu.stanford.myheartcounts.healthstats.HealthStatsRequest
import edu.stanford.myheartcounts.healthstats.HealthStatsSnapshot
import edu.stanford.myheartcounts.healthstats.HealthStatsStore
import edu.stanford.myheartcounts.model.demographics.RaceEthnicity
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.onStart
import kotlinx.coroutines.isActive
import org.grovealliance.account.Account
import org.grovealliance.account.accountId
import org.grovealliance.core.coroutines.runCatchingAsync
import org.grovealliance.core.logging.groveLogger
import org.grovealliance.health.Health
import org.grovealliance.health.HealthQueryTimeRange
import org.grovealliance.health.RecordType
import org.grovealliance.scheduler.InstantRange
import java.time.Instant
import java.time.temporal.ChronoUnit
import kotlin.time.Duration.Companion.seconds

/**
 * Supplies the participant's heart health scores, emitting again whenever they change.
 */
interface HeartHealthScoreSource {
    val scores: Flow<HeartHealthScores>

    fun refresh() = Unit
}

/**
 * Combines live health statistics and survey observations with the device's latest glucose reading.
 * Account changes clear all prior score inputs before observing the new participant.
 */
class HeartHealthScoreSourceImpl(
    account: Account,
    private val stats: HealthStatsStore,
    private val customSamples: CustomSampleSource,
    private val health: Health,
    private val calculator: HeartHealthScoreCalculator,
    private val ranges: HeartHealthQueryRanges,
) : HeartHealthScoreSource {
    private val logger by groveLogger()
    private val refreshes = MutableStateFlow(0L)

    override fun refresh() { refreshes.value += 1 }

    @OptIn(ExperimentalCoroutinesApi::class)
    override val scores: Flow<HeartHealthScores> = account.details
        .map { details ->
            Session(
                accountId = details?.accountId,
                asianBodyMassIndex = ASIAN_ETHNICITIES.any { it in (details?.raceEthnicity ?: RaceEthnicity.NONE) },
            )
        }
        .distinctUntilChanged()
        .flatMapLatest { session ->
            var latest = HeartHealthScoreInput(
                exerciseRange = ranges.exercise,
                exercise = emptyList(),
                steps = null,
                sleep = emptyList(),
                bodyMassIndex = emptyList(),
                weight = emptyList(),
                height = emptyList(),
                bloodPressure = emptyList(),
                custom = emptyMap(),
                glucose = null,
                usesAsianBodyMassIndex = session.asianBodyMassIndex,
            )
            combine(ranges.changes, refreshes) { _, refresh -> refresh }.flatMapLatest {
                if (session.accountId == null) {
                    flowOf(calculator.calculate(input = latest))
                } else {
                    observe(initial = latest.copy(exerciseRange = ranges.exercise))
                        .onEach { latest = it }
                        .map { calculator.calculate(input = it) }
                }
            }
        }
        .distinctUntilChanged()

    private fun observe(initial: HeartHealthScoreInput): Flow<HeartHealthScoreInput> {
        val changes = listOf(
            quantity(
                metric = HealthStatsMetric.ExerciseTime,
                range = ranges.exercise,
                aggregation = HealthStatsAggregation.Sum,
            ) { input, snapshot ->
                input.copy(exercise = snapshot?.elements.orEmpty())
            },
            observeStats(
                request = HealthStatsRequest.Quantity(
                    metric = HealthStatsMetric.Steps,
                    timeRange = ranges.exercise,
                    aggregation = HealthStatsAggregation.Sum,
                    interval = HealthStatsAggregationInterval(
                        unit = ChronoUnit.DAYS,
                        anchor = ranges.exercise.endExclusive,
                        alignmentPolicy = HealthStatsAggregationInterval.AlignmentPolicy.Approximate,
                    ),
                ),
            ) { input, snapshot -> input.copy(steps = snapshot) },
            observeStats(request = HealthStatsRequest.SleepSessions(timeRange = ranges.fortnight)) { input, snapshot ->
                input.copy(sleep = snapshot?.elements.orEmpty())
            },
            quantity(metric = HealthStatsMetric.BodyMassIndex, range = ranges.fortnight) { input, snapshot ->
                input.copy(bodyMassIndex = snapshot?.elements.orEmpty())
            },
            quantity(metric = HealthStatsMetric.Weight, range = ranges.quarter) { input, snapshot ->
                input.copy(weight = snapshot?.elements.orEmpty())
            },
            quantity(
                metric = HealthStatsMetric.Height,
                range = InstantRange(
                    start = Instant.MIN,
                    endExclusive = Instant.MAX,
                ),
            ) { input, snapshot ->
                input.copy(height = snapshot?.elements.orEmpty())
            },
            observeStats(request = HealthStatsRequest.BloodPressure(timeRange = ranges.quarter)) { input, snapshot ->
                input.copy(bloodPressure = snapshot?.elements.orEmpty())
            },
            glucose(range = ranges.fortnight).map { sample ->
                { input: HeartHealthScoreInput -> input.copy(glucose = sample) }
            },
        ) + CustomQuantitySampleType.entries.map { type ->
            customSamples.samples(type = type, timeRange = ranges.custom).available().map { samples ->
                { input: HeartHealthScoreInput -> input.copy(custom = input.custom + (type to samples.orEmpty())) }
            }
        }
        // Every input starts unchanged, so a restart keeps showing the previous values until new ones arrive.
        return combine(flows = changes.map { change -> change.onStart { emit { input -> input } } }) { updates ->
            updates.fold(initial) { input, update -> update(input) }
        }
    }

    private fun quantity(
        metric: HealthStatsMetric,
        range: InstantRange,
        aggregation: HealthStatsAggregation = HealthStatsAggregation.Avg,
        update: (
            HeartHealthScoreInput,
            HealthStatsSnapshot<HealthStatsQuantitySample>?,
        ) -> HeartHealthScoreInput,
    ): Flow<HeartHealthInputUpdate> = observeStats(
        request = HealthStatsRequest.Quantity(
            metric = metric,
            timeRange = range,
            aggregation = aggregation,
        ),
        update = update,
    )

    private fun <T> observeStats(
        request: HealthStatsRequest<T>,
        update: (HeartHealthScoreInput, HealthStatsSnapshot<T>?) -> HeartHealthScoreInput,
    ): Flow<HeartHealthInputUpdate> = stats.updates(request = request).available().map { snapshot ->
        { input: HeartHealthScoreInput -> update(input, snapshot) }
    }

    private fun <T> Flow<Result<T>>.available(): Flow<T?> = map { result ->
        result.onFailure { logger.w(it) { "A heart health score input is unavailable" } }.getOrNull()
    }.catch { error ->
        logger.w(error) { "A heart health score input could not be observed" }
        emit(null)
    }

    private fun glucose(range: InstantRange): Flow<HeartHealthScoreInput.Glucose?> = flow {
        while (currentCoroutineContext().isActive) {
            val sample = runCatchingAsync {
                health.mostRecent(
                    type = RecordType.bloodGlucose,
                    timeRange = HealthQueryTimeRange(
                        start = range.start,
                        end = range.endExclusive,
                    ),
                )?.let {
                    HeartHealthScoreInput.Glucose(
                        value = it.level.inMilligramsPerDeciliter,
                        measuredAt = it.time,
                    )
                }
            }.getOrElse { error ->

                logger.w(error) { "The glucose score input is unavailable" }
                null
            }
            emit(sample)
            delay(GLUCOSE_REFRESH)
        }
    }.distinctUntilChanged()

    private data class Session(
        val accountId: String?,
        val asianBodyMassIndex: Boolean,
    )

    private companion object {
        val GLUCOSE_REFRESH = 15.seconds
        val ASIAN_ETHNICITIES = listOf(
            RaceEthnicity.ASIAN_INDIAN, RaceEthnicity.CHINESE, RaceEthnicity.FILIPINO,
            RaceEthnicity.JAPANESE, RaceEthnicity.KOREAN, RaceEthnicity.VIETNAMESE, RaceEthnicity.PACIFIC_ISLANDER,
        )
    }
}

private typealias HeartHealthInputUpdate = (HeartHealthScoreInput) -> HeartHealthScoreInput
