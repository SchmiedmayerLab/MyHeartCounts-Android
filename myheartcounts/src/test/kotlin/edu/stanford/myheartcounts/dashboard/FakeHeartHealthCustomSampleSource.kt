//
// This source file is part of the My Heart Counts Android open-source project
//
// SPDX-FileCopyrightText: 2026 Stanford University and the project authors (see CONTRIBUTORS.md)
//
// SPDX-License-Identifier: MIT

package edu.stanford.myheartcounts.dashboard

import edu.stanford.myheartcounts.customsamples.CustomQuantitySample
import edu.stanford.myheartcounts.customsamples.CustomQuantitySampleType
import edu.stanford.myheartcounts.customsamples.CustomSampleSource
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import org.grovealliance.scheduler.InstantRange

class FakeHeartHealthCustomSampleSource : CustomSampleSource {
    private val data = CustomQuantitySampleType.entries.associateWith {
        MutableStateFlow<Result<List<CustomQuantitySample>>>(Result.success(emptyList()))
    }
    val ranges = mutableListOf<InstantRange?>()

    fun setSamples(type: CustomQuantitySampleType, samples: List<CustomQuantitySample>) {
        data.getValue(type).value = Result.success(samples)
    }

    fun fail(type: CustomQuantitySampleType) {
        data.getValue(type).value = Result.failure(IllegalStateException("unavailable"))
    }

    override fun samples(type: CustomQuantitySampleType, timeRange: InstantRange?): Flow<Result<List<CustomQuantitySample>>> {
        ranges += timeRange
        return data.getValue(type)
    }
}
