//
// This source file is part of the My Heart Counts Android open-source project
//
// SPDX-FileCopyrightText: 2026 Stanford University and the project authors (see CONTRIBUTORS.md)
//
// SPDX-License-Identifier: MIT

package edu.stanford.myheartcounts.demographics

import edu.stanford.myheartcounts.model.StudyVariant
import edu.stanford.myheartcounts.study.StudyVariantProvider
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

/**
 * Resolves the regional demographics form.
 */
interface DemographicsRegionSource {

    /**
     * The form of the region the participant's study runs in: the current one, then each change.
     */
    val regions: Flow<DemographicsRegion>

    /**
     * The form of the region the participant's study runs in.
     */
    fun region(): DemographicsRegion
}

/**
 * [DemographicsRegionSource] following the participant's [StudyVariant].
 */
class DemographicsRegionSourceImpl(
    private val variantProvider: StudyVariantProvider,
) : DemographicsRegionSource {

    override val regions: Flow<DemographicsRegion> = variantProvider.variant.map { regionOf(variant = it) }

    override fun region(): DemographicsRegion = regionOf(variant = variantProvider.currentVariant)

    private fun regionOf(variant: StudyVariant): DemographicsRegion = when (variant) {
        StudyVariant.Stanford -> DemographicsRegion.US
        StudyVariant.Imperial -> DemographicsRegion.UK
    }
}
