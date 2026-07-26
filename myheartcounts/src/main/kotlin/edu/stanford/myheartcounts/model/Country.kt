//
// This source file is part of the My Heart Counts open-source project
//
// SPDX-FileCopyrightText: 2026 Stanford University and the project authors (see CONTRIBUTORS.md)
//
// SPDX-License-Identifier: MIT

package edu.stanford.myheartcounts.model

import java.text.Collator
import java.util.Locale

/**
 * A country: its ISO 3166-1 alpha-2 [code] and its [name] localized for a chosen locale.
 */
data class Country(
    val code: String,
    val name: String,
) {
    /**
     * Whether the study is open in this country.
     */
    val isEnabled: Boolean
        get() = code in ENABLED_COUNTRIES

    /**
     * Whether the study is planned but not yet open in this country.
     */
    val isComingSoon: Boolean
        get() = code in COMING_SOON_COUNTRIES

    companion object {
        // ISO 3166-1 alpha-2 codes of the countries the study is open in, and the ones launching soon.
        private val ENABLED_COUNTRIES = setOf("US")
        private val COMING_SOON_COUNTRIES = setOf("GB")

        /**
         * Returns every ISO 3166-1 country, named in [locale] and sorted by name using [locale]'s collation.
         */
        fun allCountries(locale: Locale = Locale.getDefault()): List<Country> {
            val collator = Collator.getInstance(locale)
            return Locale.getISOCountries().map { code ->
                Country(
                    code = code,
                    name = Locale.Builder().setRegion(code).build().getDisplayCountry(locale),
                )
            }.sortedWith(compareBy(collator) { it.name })
        }
    }
}
