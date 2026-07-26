//
// This source file is part of the My Heart Counts open-source project
//
// SPDX-FileCopyrightText: 2026 Stanford University and the project authors (see CONTRIBUTORS.md)
//
// SPDX-License-Identifier: MIT

@file:Suppress("SpacingBetweenDeclarationsWithAnnotations")

package edu.stanford.myheartcounts.model.demographics

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/**
 * A US state or territory.
 */
@Serializable
enum class USRegion {
    @SerialName("") NOT_SET,
    @SerialName("AL") ALABAMA,
    @SerialName("AK") ALASKA,
    @SerialName("AZ") ARIZONA,
    @SerialName("AR") ARKANSAS,
    @SerialName("CA") CALIFORNIA,
    @SerialName("CO") COLORADO,
    @SerialName("CT") CONNECTICUT,
    @SerialName("DE") DELAWARE,
    @SerialName("DC") DISTRICT_OF_COLUMBIA,
    @SerialName("FL") FLORIDA,
    @SerialName("GA") GEORGIA,
    @SerialName("HI") HAWAII,
    @SerialName("ID") IDAHO,
    @SerialName("IL") ILLINOIS,
    @SerialName("IN") INDIANA,
    @SerialName("IA") IOWA,
    @SerialName("KS") KANSAS,
    @SerialName("KY") KENTUCKY,
    @SerialName("LA") LOUISIANA,
    @SerialName("ME") MAINE,
    @SerialName("MD") MARYLAND,
    @SerialName("MA") MASSACHUSETTS,
    @SerialName("MI") MICHIGAN,
    @SerialName("MN") MINNESOTA,
    @SerialName("MS") MISSISSIPPI,
    @SerialName("MO") MISSOURI,
    @SerialName("MT") MONTANA,
    @SerialName("NE") NEBRASKA,
    @SerialName("NV") NEVADA,
    @SerialName("NH") NEW_HAMPSHIRE,
    @SerialName("NJ") NEW_JERSEY,
    @SerialName("NM") NEW_MEXICO,
    @SerialName("NY") NEW_YORK,
    @SerialName("NC") NORTH_CAROLINA,
    @SerialName("ND") NORTH_DAKOTA,
    @SerialName("OH") OHIO,
    @SerialName("OK") OKLAHOMA,
    @SerialName("OR") OREGON,
    @SerialName("PA") PENNSYLVANIA,
    @SerialName("RI") RHODE_ISLAND,
    @SerialName("SC") SOUTH_CAROLINA,
    @SerialName("SD") SOUTH_DAKOTA,
    @SerialName("TN") TENNESSEE,
    @SerialName("TX") TEXAS,
    @SerialName("UT") UTAH,
    @SerialName("VT") VERMONT,
    @SerialName("VA") VIRGINIA,
    @SerialName("WA") WASHINGTON,
    @SerialName("WV") WEST_VIRGINIA,
    @SerialName("WI") WISCONSIN,
    @SerialName("WY") WYOMING,
    @SerialName("AS") AMERICAN_SAMOA,
    @SerialName("GU") GUAM,
    @SerialName("MP") NORTHERN_MARIANA_ISLANDS,
    @SerialName("PR") PUERTO_RICO,
    @SerialName("TT") TRUST_TERRITORIES,
    @SerialName("VI") VIRGIN_ISLANDS,
}
