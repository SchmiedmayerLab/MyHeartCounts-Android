//
// This source file is part of the My Heart Counts Android open-source project
//
// SPDX-FileCopyrightText: 2026 Stanford University and the project authors (see CONTRIBUTORS.md)
//
// SPDX-License-Identifier: MIT

package edu.stanford.myheartcounts.healthstats

import androidx.health.connect.client.records.HeightRecord
import androidx.health.connect.client.records.Record
import org.grovealliance.account.Account
import org.grovealliance.account.accountId
import org.grovealliance.core.lifecycle.AppLifecycle
import org.grovealliance.core.time.TimeProvider
import org.grovealliance.health.Health
import org.grovealliance.health.HealthChanges
import org.grovealliance.storage.local.KeyValueStorage
import java.time.Duration
import java.time.Instant

/**
 * Refreshes one monthly contribution from independently acknowledged device changes.
 */
interface HealthStatsMonthlySync {
    suspend fun refresh(metric: HealthStatsSyncMetric, month: HealthStatsMonth, force: Boolean): Result<Unit>
}

class HealthStatsMonthlySyncImpl(
    private val health: Health,
    private val collector: HealthStatsMonthCollector,
    private val persistence: HealthStatsPersistence,
    private val environment: HealthStatsSyncEnvironment,
    private val configuration: HealthStatsSyncConfiguration,
    private val account: Account,
) : HealthStatsMonthlySync {
    @Suppress("detekt:ReturnCount")
    override suspend fun refresh(metric: HealthStatsSyncMetric, month: HealthStatsMonth, force: Boolean): Result<Unit> {
        val owner = account.details.value?.accountId.orEmpty()
        val previous = persistence.checkpoint(metric = metric, month = month.month)
        val zone = environment.zoneId
        if (!environment.canRead(metric = metric, month = month)) return Result.success(Unit)
        val changes = mutableListOf<HealthChanges<out Record>>()
        val tokens = mutableMapOf<String, String>()
        for (type in metric.recordTypes) {
            val token = previous?.tokens?.get(type.identifier).takeIf { previous?.zoneId == zone }
            val change = health.changes(type = type, token = token).getOrElse { return Result.failure(it) }
            changes += change
            tokens[type.identifier] = change.nextToken
        }
        val deleted = changes.flatMap { it.deletedIds }.toSet()
        val hasDeletions = deleted.any { it in previous?.recordIds.orEmpty() }
        val needsSnapshot = force || changes.any { it.isReset } || hasDeletions ||
            changes.any { it.added.any { record -> affects(record = record, metric = metric, month = month) } }
        val snapshot = if (needsSnapshot) {
            collector.collect(metric = metric, month = month).getOrElse { return Result.failure(it) }
        } else {
            HealthStatsCollectedMonth(
                entries = emptyList(),
                recordIds = previous?.recordIds.orEmpty(),
            )
        }
        if (account.details.value?.accountId != owner) return Result.failure(HealthStatsStoreException.SessionChanged())
        if (!environment.canRead(metric = metric, month = month)) return Result.success(Unit)
        return persistence.persist(
            contribution = HealthStatsContribution(
                metricId = metric.id,
                month = month.month,
                collection = metric.collection,
                sourceId = configuration.sourceId,
                entries = snapshot.entries,
            ),
            checkpoint = HealthStatsCheckpoint(
                tokens = tokens,
                recordIds = snapshot.recordIds,
                zoneId = zone,
            ),
            hasDeletions = hasDeletions && needsSnapshot,
        )
    }

    private fun affects(record: Record, metric: HealthStatsSyncMetric, month: HealthStatsMonth): Boolean {
        if (metric == HealthStatsSyncMetric.BodyMassIndex && record is HeightRecord) return true
        val range = record.statsRange() ?: return false
        val margin = if (metric == HealthStatsSyncMetric.Sleep) SLEEP_MARGIN else Duration.ZERO
        return range.start < month.timeRange.endExclusive.plus(margin) && range.endExclusive >= month.timeRange.start.minus(margin)
    }

    private companion object {
        val SLEEP_MARGIN: Duration = Duration.ofDays(1)
    }
}

/**
 * The clock, time zone and permissions relevant to a statistics refresh.
 */
interface HealthStatsSyncEnvironment {
    val zoneId: String
    suspend fun canRead(metric: HealthStatsSyncMetric, month: HealthStatsMonth): Boolean
}

/**
 * Without history access, data is readable from [RECENT_HISTORY] before read access was first granted. The first
 * time this installation observed read access stands in for that grant, so the readable window is never overstated.
 */
class HealthStatsSyncEnvironmentImpl(
    private val health: Health,
    private val timeProvider: TimeProvider,
    private val appLifecycle: AppLifecycle,
    private val storage: KeyValueStorage,
) : HealthStatsSyncEnvironment {
    override val zoneId: String get() = timeProvider.currentZone().id

    override suspend fun canRead(metric: HealthStatsSyncMetric, month: HealthStatsMonth): Boolean {
        for (type in metric.recordTypes) {
            val access = health.readAccess(type = type)
            if (!access.records) return false
            val historyReadable = access.history || month.timeRange.start >= firstReadAccess().minus(RECENT_HISTORY)
            val lifecycleReadable = appLifecycle.isInForeground || access.background
            if (!historyReadable || !lifecycleReadable) return false
        }
        return true
    }

    private fun firstReadAccess(): Instant {
        val stored = storage.getLong(key = FIRST_READ_ACCESS, default = NOT_RECORDED)
        if (stored != NOT_RECORDED) return Instant.ofEpochMilli(stored)
        return timeProvider.nowInstant().also { storage.putLong(key = FIRST_READ_ACCESS, value = it.toEpochMilli()) }
    }

    private companion object {
        val RECENT_HISTORY: Duration = Duration.ofDays(30)
        const val FIRST_READ_ACCESS = "firstReadAccess"
        const val NOT_RECORDED = Long.MIN_VALUE
    }
}
