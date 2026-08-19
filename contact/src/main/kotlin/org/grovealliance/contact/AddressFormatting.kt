//
// This source file is part of the My Heart Counts Android open-source project
//
// SPDX-FileCopyrightText: 2026 Stanford University and the project authors (see CONTRIBUTORS.md)
//
// SPDX-License-Identifier: MIT

package org.grovealliance.contact

import android.location.Address

fun Address.formatted(): String {
    val lines = (0..maxAddressLineIndex).map { getAddressLine(it) }
    val areaLine = listOf(locality, adminArea, postalCode).mapNotNull { it }.joinToString(" ")
    val countryLine = countryName ?: ""
    return ((lines + areaLine) + countryLine)
        .filter { it.isNotBlank() }
        .joinToString("\n")
}
