//
// This source file is part of the My Heart Counts Android open-source project
//
// SPDX-FileCopyrightText: 2026 Stanford University and the project authors (see CONTRIBUTORS.md)
//
// SPDX-License-Identifier: MIT

package edu.stanford.myheartcounts.healthstats

import edu.stanford.myheartcounts.account.dateOfEnrollment
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.Job
import kotlinx.coroutines.cancelAndJoin
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.debounce
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import org.grovealliance.account.Account
import org.grovealliance.account.accountId
import org.grovealliance.core.lifecycle.AppLifecycle
import org.grovealliance.core.logging.groveLogger
import org.grovealliance.study.StudyManager
import kotlin.time.Duration.Companion.minutes
import kotlin.time.Duration.Companion.seconds

/**
 * Maintains the enrolled participant's statistics in the foreground and in scheduled background work.
 */
interface HealthStatsSync {
    fun start()
    fun requestRefresh()
    suspend fun refresh(force: Boolean): Result<Unit>
    suspend fun stop()
}

@Suppress("detekt:LongParameterList")
class HealthStatsSyncImpl(
    private val account: Account,
    private val studyManager: StudyManager,
    private val appLifecycle: AppLifecycle,
    private val refresher: HealthStatsRefresher,
    private val scheduler: HealthStatsSyncScheduler,
    private val persistence: HealthStatsPersistence,
    private val scope: CoroutineScope,
) : HealthStatsSync {
    private val logger by groveLogger()
    private val requests = MutableSharedFlow<Unit>(extraBufferCapacity = 1)
    private var observer: Job? = null
    private var requestObserver: Job? = null

    @OptIn(FlowPreview::class)
    override fun start() {
        if (observer?.isActive == true) return
        observer = scope.launch {
            combine(account.details, studyManager.enrollments, appLifecycle.state) { details, enrollments, lifecycle ->
                State(
                    accountId = details?.accountId,
                    hasEnrollment = details?.dateOfEnrollment != null && enrollments.isNotEmpty(),
                    isForeground = lifecycle == AppLifecycle.State.FOREGROUND,
                )
            }.distinctUntilChanged().collectLatest { state ->
                if (state.accountId == null || !state.hasEnrollment) {
                    scheduler.cancel()
                } else {
                    scheduler.schedule()
                    if (state.isForeground) {
                        var first = true
                        while (currentCoroutineContext().isActive) {
                            refresh(force = first).onFailure { logger.w(it) { "Health statistics refresh will be retried" } }
                            first = false
                            delay(FOREGROUND_REFRESH)
                        }
                    }
                }
            }
        }
        requestObserver = scope.launch {
            requests.debounce(REQUEST_DEBOUNCE).collectLatest {
                refresh(force = false).onFailure { logger.w(it) { "Changed health statistics could not be refreshed" } }
            }
        }
    }

    override fun requestRefresh() {
        requests.tryEmit(Unit)
    }

    override suspend fun refresh(force: Boolean): Result<Unit> =
        if (account.details.value?.accountId == null || studyManager.studyEnrollments().isEmpty()) {
            Result.success(Unit)
        } else {
            refresher.refresh(force = force)
        }

    override suspend fun stop() {
        scheduler.cancel()
        observer?.cancelAndJoin()
        requestObserver?.cancelAndJoin()
        observer = null
        requestObserver = null
        refresher.awaitIdle()
        persistence.clear()
    }

    private data class State(
        val accountId: String?,
        val hasEnrollment: Boolean,
        val isForeground: Boolean,
    )

    private companion object {
        val FOREGROUND_REFRESH = 15.minutes
        val REQUEST_DEBOUNCE = 1.seconds
    }
}
