//
// This source file is part of the My Heart Counts Android open-source project
//
// SPDX-FileCopyrightText: 2026 Stanford University and the project authors (see CONTRIBUTORS.md)
//
// SPDX-License-Identifier: MIT

package edu.stanford.myheartcounts.demographics

import org.grovealliance.ui.StringResource

/**
 * A selectable demographic response.
 */
data class DemographicsOption(
    val id: String,
    val title: StringResource,
    val subtitle: StringResource? = null,
    val group: StringResource? = null,
)

/**
 * The available demographic questions and their responses.
 */
interface DemographicsCatalog {
    fun title(field: DemographicsField): StringResource
    fun options(field: DemographicsField): List<DemographicsOption>
    fun conditions(): List<DemographicsOption>
    fun counties(nation: String): List<String>
}

/**
 * Default [DemographicsCatalog] implementation, combining the questions of [options], the regions of
 * [regions] and the conditions of [conditions].
 */
class DemographicsCatalogImpl(
    private val options: DemographicsOptionsProvider,
    private val regions: DemographicsRegionsProvider,
    private val conditions: DemographicsConditionsProvider,
) : DemographicsCatalog {

    override fun title(field: DemographicsField): StringResource = options.title(field = field)

    override fun options(field: DemographicsField): List<DemographicsOption> = when (field) {
        DemographicsField.USRegion -> regions.usRegions()
        DemographicsField.UKRegion -> regions.ukNations()
        else -> options.options(field = field)
    }

    override fun conditions(): List<DemographicsOption> = conditions.conditions()

    override fun counties(nation: String): List<String> = regions.counties(nation = nation)
}
