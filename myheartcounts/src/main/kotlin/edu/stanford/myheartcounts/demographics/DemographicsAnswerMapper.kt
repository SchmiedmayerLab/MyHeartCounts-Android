//
// This source file is part of the My Heart Counts Android open-source project
//
// SPDX-FileCopyrightText: 2026 Stanford University and the project authors (see CONTRIBUTORS.md)
//
// SPDX-License-Identifier: MIT

package edu.stanford.myheartcounts.demographics

import edu.stanford.myheartcounts.MHCStrings
import edu.stanford.myheartcounts.model.demographics.BiologicalSex
import edu.stanford.myheartcounts.model.demographics.BloodType
import edu.stanford.myheartcounts.model.demographics.Comorbidities
import edu.stanford.myheartcounts.model.demographics.MHCGenderIdentity
import edu.stanford.myheartcounts.model.demographics.UKRegion
import edu.stanford.myheartcounts.model.demographics.USRegion
import org.grovealliance.ui.StringResource
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.time.format.FormatStyle

/**
 * Maps each demographic answer into the text the participant reads.
 */
interface DemographicsAnswerMapper {

    /**
     * The gender identity [value].
     */
    fun mapText(value: MHCGenderIdentity): StringResource

    /**
     * The biological sex at birth [value].
     */
    fun mapText(value: BiologicalSex): StringResource

    /**
     * The blood type [value].
     */
    fun mapText(value: BloodType): StringResource

    /**
     * The date [value], in the device's locale.
     */
    fun mapText(value: LocalDate): StringResource

    /**
     * The nation and county of [value].
     */
    fun mapText(value: UKRegion): StringResource

    /**
     * The state or territory [value], its postal abbreviation.
     */
    fun mapText(value: USRegion): StringResource

    /**
     * The number of conditions chosen in [value], or that there are none.
     */
    fun mapText(value: Comorbidities): StringResource

    /**
     * The length [value] in centimetres.
     */
    fun mapCentimetresText(value: Double): StringResource

    /**
     * The weight [value] in kilograms.
     */
    fun mapKilogramsText(value: Double): StringResource

    /**
     * The measurement [value] as it is entered: without trailing zeros or exponent.
     */
    fun mapEntryText(value: Double): String
}

/**
 * Default [DemographicsAnswerMapper] implementation, naming a US region by its id in [optionIdMapper].
 */
class DemographicsAnswerMapperImpl(
    private val optionIdMapper: DemographicsOptionIdMapper,
) : DemographicsAnswerMapper {

    override fun mapText(value: MHCGenderIdentity): StringResource = when (value) {
        MHCGenderIdentity.PREFER_NOT_TO_STATE -> StringResource(MHCStrings.demographics_gender_identity_0)
        MHCGenderIdentity.MALE -> StringResource(MHCStrings.demographics_gender_identity_1)
        MHCGenderIdentity.FEMALE -> StringResource(MHCStrings.demographics_gender_identity_2)
        MHCGenderIdentity.TRANS_FEMALE -> StringResource(MHCStrings.demographics_gender_identity_3)
        MHCGenderIdentity.TRANS_MALE -> StringResource(MHCStrings.demographics_gender_identity_4)
        MHCGenderIdentity.OTHER -> StringResource(MHCStrings.demographics_gender_identity_5)
    }

    override fun mapText(value: BiologicalSex): StringResource = when (value) {
        BiologicalSex.PREFER_NOT_TO_STATE -> StringResource(MHCStrings.demographics_sex_at_birth_0)
        BiologicalSex.MALE -> StringResource(MHCStrings.demographics_sex_at_birth_1)
        BiologicalSex.FEMALE -> StringResource(MHCStrings.demographics_sex_at_birth_2)
        BiologicalSex.INTERSEX -> StringResource(MHCStrings.demographics_sex_at_birth_3)
    }

    override fun mapText(value: BloodType): StringResource = when (value) {
        BloodType.NOT_SET -> StringResource(MHCStrings.demographics_value_not_set)
        BloodType.A_POSITIVE -> StringResource(MHCStrings.demographics_blood_type_1)
        BloodType.A_NEGATIVE -> StringResource(MHCStrings.demographics_blood_type_2)
        BloodType.B_POSITIVE -> StringResource(MHCStrings.demographics_blood_type_3)
        BloodType.B_NEGATIVE -> StringResource(MHCStrings.demographics_blood_type_4)
        BloodType.AB_POSITIVE -> StringResource(MHCStrings.demographics_blood_type_5)
        BloodType.AB_NEGATIVE -> StringResource(MHCStrings.demographics_blood_type_6)
        BloodType.O_POSITIVE -> StringResource(MHCStrings.demographics_blood_type_7)
        BloodType.O_NEGATIVE -> StringResource(MHCStrings.demographics_blood_type_8)
    }

    override fun mapText(value: LocalDate): StringResource =
        StringResource(value.format(DateTimeFormatter.ofLocalizedDate(FormatStyle.MEDIUM)))

    override fun mapText(value: UKRegion): StringResource = when (value) {
        UKRegion.NotSet -> StringResource(MHCStrings.demographics_value_not_set)
        is UKRegion.England -> StringResource(MHCStrings.demographics_uk_region_england_county, value.county.name)
        is UKRegion.Scotland -> StringResource(MHCStrings.demographics_uk_region_scotland_county, value.county.name)
        is UKRegion.Wales -> StringResource(MHCStrings.demographics_uk_region_wales_county, value.county.name)
        is UKRegion.NorthernIreland ->
            StringResource(MHCStrings.demographics_uk_region_northernireland_county, value.county.name)
    }

    override fun mapText(value: USRegion): StringResource = if (value == USRegion.NOT_SET) {
        StringResource(MHCStrings.demographics_value_not_set)
    } else {
        StringResource(optionIdMapper.mapId(value = value))
    }

    override fun mapText(value: Comorbidities): StringResource = if (value.entries.isEmpty()) {
        StringResource(MHCStrings.demographics_comorbidities_none)
    } else {
        StringResource(MHCStrings.demographics_value_selected_count, value.entries.size)
    }

    override fun mapCentimetresText(value: Double): StringResource =
        StringResource(MHCStrings.demographics_value_cm, mapEntryText(value = value))

    override fun mapKilogramsText(value: Double): StringResource =
        StringResource(MHCStrings.demographics_value_kg, mapEntryText(value = value))

    override fun mapEntryText(value: Double): String = value.toBigDecimal().stripTrailingZeros().toPlainString()
}
