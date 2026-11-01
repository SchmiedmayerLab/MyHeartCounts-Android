//
// This source file is part of the My Heart Counts Android open-source project
//
// SPDX-FileCopyrightText: 2026 Stanford University and the project authors (see CONTRIBUTORS.md)
//
// SPDX-License-Identifier: MIT

package edu.stanford.myheartcounts.demographics

import edu.stanford.myheartcounts.standard.health.HealthAccess
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import org.grovealliance.health.HealthConnectAvailability

/**
 * A [DemographicsRegionSource] answering a configured region, the US by default.
 */
class FakeDemographicsRegionSource : DemographicsRegionSource {
    private val region = MutableStateFlow(DemographicsRegion.US)

    override val regions: Flow<DemographicsRegion> = region

    /**
     * Sets the region [regions] and [region] report.
     */
    fun setRegion(region: DemographicsRegion) {
        this.region.value = region
    }

    override fun region(): DemographicsRegion = region.value
}

/**
 * A [HealthAccess] counting its permission requests, which fail when configured to.
 */
class FakeDemographicsHealthAccess : HealthAccess {
    private var requestFailure: Exception? = null
    private var requestCount = 0

    override val availability = MutableStateFlow<HealthConnectAvailability>(HealthConnectAvailability.Available)

    /**
     * How often [requestPermissions] was called.
     */
    val requests: Int get() = requestCount

    /**
     * Makes [requestPermissions] throw [error].
     */
    fun setRequestFailure(error: Exception) {
        requestFailure = error
    }

    override suspend fun requestPermissions(): Boolean {
        requestCount++
        requestFailure?.let { throw it }
        return true
    }

    override fun installHealthConnect(): Boolean = false
}

/**
 * A [DemographicsAutofill] answering a configured result, no measurements by default.
 */
class FakeDemographicsAutofill : DemographicsAutofill {
    private var result = Result.success(
        DemographicsMeasurements(
            heightInCm = null,
            weightInKg = null,
        ),
    )

    /**
     * Sets what [read] returns.
     */
    fun setResult(result: Result<DemographicsMeasurements>) {
        this.result = result
    }

    override suspend fun read(): Result<DemographicsMeasurements> = result
}

/**
 * A [DemographicsEditor] applying changes to a configured state without storing them, and recording
 * flushes.
 */
class FakeDemographicsEditor : DemographicsEditor {
    private val mutableState = MutableStateFlow(DemographicsStateFixtures.create())
    private val mutableCompletion = MutableStateFlow<Map<DemographicsField, DemographicsCompletion>>(emptyMap())
    private var flushSoonCount = 0

    override val state: StateFlow<DemographicsState> = mutableState.asStateFlow()
    override val completion = mutableCompletion.asStateFlow()
    override val isComplete = MutableStateFlow(false).asStateFlow()
    private val mutableSaveFailures = MutableSharedFlow<Unit>(extraBufferCapacity = 1)
    override val saveFailures = mutableSaveFailures.asSharedFlow()

    /**
     * How often [flushSoon] was called.
     */
    val flushSoonCalls: Int get() = flushSoonCount

    /**
     * Sets the current state.
     */
    fun setState(state: DemographicsState) {
        mutableState.value = state
    }

    /**
     * Sets the current completion.
     */
    fun setCompletion(completion: Map<DemographicsField, DemographicsCompletion>) {
        mutableCompletion.value = completion
    }

    /**
     * Reports a failed attempt to store the answers.
     */
    fun emitSaveFailure() {
        mutableSaveFailures.tryEmit(Unit)
    }

    override fun configure() = Unit

    override fun update(change: (DemographicsResponse) -> DemographicsResponse) {
        mutableState.value = mutableState.value.copy(response = change(mutableState.value.response))
    }

    override suspend fun flush(): Result<Unit> = Result.success(Unit)

    override fun flushSoon() {
        flushSoonCount++
    }
}
