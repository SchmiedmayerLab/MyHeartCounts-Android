//
// This source file is part of the My Heart Counts Android open-source project
//
// SPDX-FileCopyrightText: 2026 Stanford University and the project authors (see CONTRIBUTORS.md)
//
// SPDX-License-Identifier: MIT

package edu.stanford.myheartcounts.dashboard.detail

import edu.stanford.myheartcounts.account.enableDebugMode
import edu.stanford.myheartcounts.customsamples.CustomQuantitySample
import edu.stanford.myheartcounts.customsamples.CustomQuantitySampleType
import edu.stanford.myheartcounts.customsamples.CustomSampleSource
import edu.stanford.myheartcounts.dashboard.HeartScoreComponent
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map
import org.grovealliance.account.Account
import org.grovealliance.account.accountId

/**
 * Supplies every stored value of a factor for inspection in debug mode.
 */
interface HeartScoreDebugSource {

    /**
     * Every stored value of [component], each time they change; `null` while debug mode is off,
     * nobody is signed in, or for factors not stored as custom samples.
     */
    fun samples(component: HeartScoreComponent): Flow<List<CustomQuantitySample>?>
}

class HeartScoreDebugSourceImpl(
    private val account: Account,
    private val customSamples: CustomSampleSource,
) : HeartScoreDebugSource {
    @OptIn(ExperimentalCoroutinesApi::class)
    override fun samples(component: HeartScoreComponent): Flow<List<CustomQuantitySample>?> {
        val type = when (component) {
            HeartScoreComponent.Diet -> CustomQuantitySampleType.DietScore
            HeartScoreComponent.NicotineExposure -> CustomQuantitySampleType.NicotineExposure
            HeartScoreComponent.MentalWellbeing -> CustomQuantitySampleType.MentalWellbeingScore
            HeartScoreComponent.BloodLipids -> CustomQuantitySampleType.BloodLipids
            else -> return flowOf(null)
        }
        return account.details.map { it?.accountId to (it?.enableDebugMode == true) }.distinctUntilChanged()
            .flatMapLatest { (id, enabled) ->
                if (id == null || !enabled) {
                    flowOf(null)
                } else {
                    customSamples.samples(type = type, timeRange = null).map { it.getOrNull() }
                }
            }
    }
}
