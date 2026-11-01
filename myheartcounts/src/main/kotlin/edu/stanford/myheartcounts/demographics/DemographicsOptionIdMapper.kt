//
// This source file is part of the My Heart Counts Android open-source project
//
// SPDX-FileCopyrightText: 2026 Stanford University and the project authors (see CONTRIBUTORS.md)
//
// SPDX-License-Identifier: MIT

package edu.stanford.myheartcounts.demographics

import edu.stanford.myheartcounts.model.demographics.EducationStatusUK
import edu.stanford.myheartcounts.model.demographics.EducationStatusUS
import edu.stanford.myheartcounts.model.demographics.HouseholdIncomeUK
import edu.stanford.myheartcounts.model.demographics.HouseholdIncomeUS
import edu.stanford.myheartcounts.model.demographics.LatinoStatusOption
import edu.stanford.myheartcounts.model.demographics.StageOfChangeOption
import edu.stanford.myheartcounts.model.demographics.UKRegion
import edu.stanford.myheartcounts.model.demographics.USRegion

/**
 * Maps each answer to the [DemographicsOption.id] offering it in the [DemographicsCatalog], and back.
 */
interface DemographicsOptionIdMapper {

    /**
     * The id of the Hispanic / Latino answer [value].
     */
    fun mapId(value: LatinoStatusOption): String

    /**
     * The id of the household income answer [value].
     */
    fun mapId(value: HouseholdIncomeUS): String

    /**
     * The id of the household income answer [value].
     */
    fun mapId(value: HouseholdIncomeUK): String

    /**
     * The id of the education answer [value].
     */
    fun mapId(value: EducationStatusUS): String

    /**
     * The id of the education answer [value].
     */
    fun mapId(value: EducationStatusUK): String

    /**
     * The id of the stage of change answer [value].
     */
    fun mapId(value: StageOfChangeOption): String

    /**
     * The id of the state or territory [value], its postal abbreviation.
     */
    fun mapId(value: USRegion): String

    /**
     * The id of the nation of [region], or `null` for [UKRegion.NotSet].
     */
    fun mapNationId(region: UKRegion): String?

    /**
     * The region of [county] within the nation identified by [nationId], or `null` for an unknown nation.
     */
    fun mapUKRegion(nationId: String, county: UKRegion.County): UKRegion?
}

/**
 * Default [DemographicsOptionIdMapper] implementation.
 */
class DemographicsOptionIdMapperImpl : DemographicsOptionIdMapper {

    override fun mapId(value: LatinoStatusOption): String = value.rawValue.toString()

    override fun mapId(value: HouseholdIncomeUS): String = value.rawValue.toString()

    override fun mapId(value: HouseholdIncomeUK): String = value.rawValue.toString()

    override fun mapId(value: EducationStatusUS): String =
        EducationStatusUS.serializer().descriptor.getElementName(value.ordinal)

    override fun mapId(value: EducationStatusUK): String =
        EducationStatusUK.serializer().descriptor.getElementName(value.ordinal)

    override fun mapId(value: StageOfChangeOption): String =
        StageOfChangeOption.serializer().descriptor.getElementName(value.ordinal)

    override fun mapId(value: USRegion): String = USRegion.serializer().descriptor.getElementName(value.ordinal)

    override fun mapNationId(region: UKRegion): String? = when (region) {
        UKRegion.NotSet -> null
        is UKRegion.England -> ENGLAND
        is UKRegion.Scotland -> SCOTLAND
        is UKRegion.Wales -> WALES
        is UKRegion.NorthernIreland -> NORTHERN_IRELAND
    }

    override fun mapUKRegion(nationId: String, county: UKRegion.County): UKRegion? = when (nationId) {
        ENGLAND -> UKRegion.England(county = county)
        SCOTLAND -> UKRegion.Scotland(county = county)
        WALES -> UKRegion.Wales(county = county)
        NORTHERN_IRELAND -> UKRegion.NorthernIreland(county = county)
        else -> null
    }

    private companion object {
        const val ENGLAND = "england"
        const val SCOTLAND = "scotland"
        const val WALES = "wales"
        const val NORTHERN_IRELAND = "northernIreland"
    }
}
