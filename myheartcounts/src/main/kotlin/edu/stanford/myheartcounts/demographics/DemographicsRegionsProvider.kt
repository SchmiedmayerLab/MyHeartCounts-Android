//
// This source file is part of the My Heart Counts Android open-source project
//
// SPDX-FileCopyrightText: 2026 Stanford University and the project authors (see CONTRIBUTORS.md)
//
// SPDX-License-Identifier: MIT

package edu.stanford.myheartcounts.demographics

import edu.stanford.myheartcounts.MHCStrings
import org.grovealliance.ui.StringResource

/**
 * Provides the regions a participant lives in: the US states and territories, and the UK nations
 * with their counties.
 */
interface DemographicsRegionsProvider {

    /**
     * The US states and territories, each identified by its postal abbreviation.
     */
    fun usRegions(): List<DemographicsOption>

    /**
     * The UK nations.
     */
    fun ukNations(): List<DemographicsOption>

    /**
     * The counties of the UK nation identified by [nation], or none for an unknown nation.
     */
    fun counties(nation: String): List<String>
}

/**
 * Default [DemographicsRegionsProvider] implementation.
 */
class DemographicsRegionsProviderImpl : DemographicsRegionsProvider {

    override fun usRegions(): List<DemographicsOption> = usRegions

    override fun ukNations(): List<DemographicsOption> = ukNations

    override fun counties(nation: String): List<String> = counties[nation].orEmpty()

    private val usRegions = listOf(
        DemographicsOption(
            id = "AL",
            title = StringResource(MHCStrings.demographics_us_region_al),
        ),
        DemographicsOption(
            id = "AK",
            title = StringResource(MHCStrings.demographics_us_region_ak),
        ),
        DemographicsOption(
            id = "AZ",
            title = StringResource(MHCStrings.demographics_us_region_az),
        ),
        DemographicsOption(
            id = "AR",
            title = StringResource(MHCStrings.demographics_us_region_ar),
        ),
        DemographicsOption(
            id = "CA",
            title = StringResource(MHCStrings.demographics_us_region_ca),
        ),
        DemographicsOption(
            id = "CO",
            title = StringResource(MHCStrings.demographics_us_region_co),
        ),
        DemographicsOption(
            id = "CT",
            title = StringResource(MHCStrings.demographics_us_region_ct),
        ),
        DemographicsOption(
            id = "DE",
            title = StringResource(MHCStrings.demographics_us_region_de),
        ),
        DemographicsOption(
            id = "DC",
            title = StringResource(MHCStrings.demographics_us_region_dc),
        ),
        DemographicsOption(
            id = "FL",
            title = StringResource(MHCStrings.demographics_us_region_fl),
        ),
        DemographicsOption(
            id = "GA",
            title = StringResource(MHCStrings.demographics_us_region_ga),
        ),
        DemographicsOption(
            id = "HI",
            title = StringResource(MHCStrings.demographics_us_region_hi),
        ),
        DemographicsOption(
            id = "ID",
            title = StringResource(MHCStrings.demographics_us_region_id),
        ),
        DemographicsOption(
            id = "IL",
            title = StringResource(MHCStrings.demographics_us_region_il),
        ),
        DemographicsOption(
            id = "IN",
            title = StringResource(MHCStrings.demographics_us_region_in),
        ),
        DemographicsOption(
            id = "IA",
            title = StringResource(MHCStrings.demographics_us_region_ia),
        ),
        DemographicsOption(
            id = "KS",
            title = StringResource(MHCStrings.demographics_us_region_ks),
        ),
        DemographicsOption(
            id = "KY",
            title = StringResource(MHCStrings.demographics_us_region_ky),
        ),
        DemographicsOption(
            id = "LA",
            title = StringResource(MHCStrings.demographics_us_region_la),
        ),
        DemographicsOption(
            id = "ME",
            title = StringResource(MHCStrings.demographics_us_region_me),
        ),
        DemographicsOption(
            id = "MD",
            title = StringResource(MHCStrings.demographics_us_region_md),
        ),
        DemographicsOption(
            id = "MA",
            title = StringResource(MHCStrings.demographics_us_region_ma),
        ),
        DemographicsOption(
            id = "MI",
            title = StringResource(MHCStrings.demographics_us_region_mi),
        ),
        DemographicsOption(
            id = "MN",
            title = StringResource(MHCStrings.demographics_us_region_mn),
        ),
        DemographicsOption(
            id = "MS",
            title = StringResource(MHCStrings.demographics_us_region_ms),
        ),
        DemographicsOption(
            id = "MO",
            title = StringResource(MHCStrings.demographics_us_region_mo),
        ),
        DemographicsOption(
            id = "MT",
            title = StringResource(MHCStrings.demographics_us_region_mt),
        ),
        DemographicsOption(
            id = "NE",
            title = StringResource(MHCStrings.demographics_us_region_ne),
        ),
        DemographicsOption(
            id = "NV",
            title = StringResource(MHCStrings.demographics_us_region_nv),
        ),
        DemographicsOption(
            id = "NH",
            title = StringResource(MHCStrings.demographics_us_region_nh),
        ),
        DemographicsOption(
            id = "NJ",
            title = StringResource(MHCStrings.demographics_us_region_nj),
        ),
        DemographicsOption(
            id = "NM",
            title = StringResource(MHCStrings.demographics_us_region_nm),
        ),
        DemographicsOption(
            id = "NY",
            title = StringResource(MHCStrings.demographics_us_region_ny),
        ),
        DemographicsOption(
            id = "NC",
            title = StringResource(MHCStrings.demographics_us_region_nc),
        ),
        DemographicsOption(
            id = "ND",
            title = StringResource(MHCStrings.demographics_us_region_nd),
        ),
        DemographicsOption(
            id = "OH",
            title = StringResource(MHCStrings.demographics_us_region_oh),
        ),
        DemographicsOption(
            id = "OK",
            title = StringResource(MHCStrings.demographics_us_region_ok),
        ),
        DemographicsOption(
            id = "OR",
            title = StringResource(MHCStrings.demographics_us_region_or),
        ),
        DemographicsOption(
            id = "PA",
            title = StringResource(MHCStrings.demographics_us_region_pa),
        ),
        DemographicsOption(
            id = "RI",
            title = StringResource(MHCStrings.demographics_us_region_ri),
        ),
        DemographicsOption(
            id = "SC",
            title = StringResource(MHCStrings.demographics_us_region_sc),
        ),
        DemographicsOption(
            id = "SD",
            title = StringResource(MHCStrings.demographics_us_region_sd),
        ),
        DemographicsOption(
            id = "TN",
            title = StringResource(MHCStrings.demographics_us_region_tn),
        ),
        DemographicsOption(
            id = "TX",
            title = StringResource(MHCStrings.demographics_us_region_tx),
        ),
        DemographicsOption(
            id = "UT",
            title = StringResource(MHCStrings.demographics_us_region_ut),
        ),
        DemographicsOption(
            id = "VT",
            title = StringResource(MHCStrings.demographics_us_region_vt),
        ),
        DemographicsOption(
            id = "VA",
            title = StringResource(MHCStrings.demographics_us_region_va),
        ),
        DemographicsOption(
            id = "WA",
            title = StringResource(MHCStrings.demographics_us_region_wa),
        ),
        DemographicsOption(
            id = "WV",
            title = StringResource(MHCStrings.demographics_us_region_wv),
        ),
        DemographicsOption(
            id = "WI",
            title = StringResource(MHCStrings.demographics_us_region_wi),
        ),
        DemographicsOption(
            id = "WY",
            title = StringResource(MHCStrings.demographics_us_region_wy),
        ),
        DemographicsOption(
            id = "AS",
            title = StringResource(MHCStrings.demographics_us_region_as),
        ),
        DemographicsOption(
            id = "GU",
            title = StringResource(MHCStrings.demographics_us_region_gu),
        ),
        DemographicsOption(
            id = "MP",
            title = StringResource(MHCStrings.demographics_us_region_mp),
        ),
        DemographicsOption(
            id = "PR",
            title = StringResource(MHCStrings.demographics_us_region_pr),
        ),
        DemographicsOption(
            id = "TT",
            title = StringResource(MHCStrings.demographics_us_region_tt),
        ),
        DemographicsOption(
            id = "VI",
            title = StringResource(MHCStrings.demographics_us_region_vi),
        ),
    )

    private val ukNations = listOf(
        DemographicsOption(
            id = "england",
            title = StringResource(MHCStrings.demographics_uk_region_england),
        ),
        DemographicsOption(
            id = "scotland",
            title = StringResource(MHCStrings.demographics_uk_region_scotland),
        ),
        DemographicsOption(
            id = "wales",
            title = StringResource(MHCStrings.demographics_uk_region_wales),
        ),
        DemographicsOption(
            id = "northernIreland",
            title = StringResource(MHCStrings.demographics_uk_region_northernireland),
        ),
    )

    private val counties = mapOf(
        "england" to listOf(
            "Aberdeenshire",
            "Angus",
            "Argyll",
            "Avon",
            "Ayrshire",
            "Banffshire",
            "Bedfordshire",
            "Berkshire",
            "Berwickshire",
            "Buckinghamshire",
            "Caithness",
            "Cambridgeshire",
            "Cheshire",
            "Clackmannanshire",
            "Cleveland",
            "Clwyd",
            "Cornwall",
            "County Antrim",
            "County Armagh",
            "County Down",
            "County Durham",
            "County Fermanagh",
            "County Londonderry",
            "County Tyrone",
            "Cumbria",
            "Derbyshire",
            "Devon",
            "Dorset",
            "Dumfriesshire",
            "Dunbartonshire",
            "Dyfed",
            "East Lothian",
            "East Sussex",
            "Essex",
            "Fife",
            "Gloucestershire",
            "Gwent",
            "Gwynedd",
            "Hampshire",
            "Herefordshire",
            "Hertfordshire",
            "Inverness-shire",
            "Isle of Arran",
            "Isle of Barra",
            "Isle of Benbecula",
            "Isle of Bute",
            "Isle of Canna",
            "Isle of Coll",
            "Isle of Colonsay",
            "Isle of Cumbrae",
            "Isle of Eigg",
            "Isle of Gigha",
            "Isle of Harris",
            "Isle of Iona",
            "Isle of Islay",
            "Isle of Jura",
            "Isle of Lewis",
            "Isle of Mull",
            "Isle of North Uist",
            "Isle of Rhum",
            "Isle of Scalpay",
            "Isle of Skye",
            "Isle of South Uist",
            "Isle of Tiree",
            "Isle of Wight",
            "Kent",
            "Kincardineshire",
            "Kinross-shire",
            "Kirkcudbrightshire",
            "Lanarkshire",
            "Lancashire",
            "Leicestershire",
            "Lincolnshire",
            "London",
            "Merseyside",
            "Mid Glamorgan",
            "Middlesex",
            "Midlothian",
            "Morayshire",
            "Nairnshire",
            "Norfolk",
            "North Humberside",
            "North Yorkshire",
            "Northamptonshire",
            "Northumberland",
            "Nottinghamshire",
            "Orkney",
            "Oxfordshire",
            "Peeblesshire",
            "Perthshire",
            "Powys",
            "Renfrewshire",
            "Ross-shire",
            "Roxburghshire",
            "Selkirkshire",
            "Shetland",
            "Shropshire",
            "Somerset",
            "South Glamorgan",
            "South Humberside",
            "South Yorkshire",
            "Staffordshire",
            "Stirlingshire",
            "Suffolk",
            "Surrey",
            "Sutherland",
            "Tyne and Wear",
            "Warwickshire",
            "West Glamorgan",
            "West Lothian",
            "West Midlands",
            "West Sussex",
            "West Yorkshire",
            "Wigtownshire",
            "Wiltshire",
            "Worcestershire",
            "Prefer not to state",
        ),
        "scotland" to listOf(
            "Prefer not to state",
        ),
        "wales" to listOf(
            "Clwyd",
            "Dyfed",
            "Gwent",
            "Gwynedd",
            "Mid Glamorgan",
            "Powys",
            "South Glamorgan",
            "West Glamorgan",
            "Prefer not to state",
        ),
        "northernIreland" to listOf(
            "Antrim",
            "Armagh",
            "Down",
            "Fermanagh",
            "Londonderry",
            "Tyrone",
            "Prefer not to state",
        ),
    )
}
