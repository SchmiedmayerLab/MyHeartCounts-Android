//
// This source file is part of the My Heart Counts Android open-source project
//
// SPDX-FileCopyrightText: 2026 Stanford University and the project authors (see CONTRIBUTORS.md)
//
// SPDX-License-Identifier: MIT

package org.grovealliance.studydefinition

import kotlinx.serialization.Serializable
import java.util.Locale

/**
 * Text authored in one or more localizations, keyed by localization identifier (`en-US`, `es-ES`).
 *
 * A study need not carry every localization, and may carry none at all.
 */
@Serializable
@JvmInline
value class LocalizedText(val values: Map<String, String>) {
    /**
     * Whether any localization was authored.
     */
    val isEmpty: Boolean get() = values.isEmpty()

    /**
     * The text best matching [locale]: an exact language and region match, otherwise any
     * localization in the same language, otherwise [fallback]. Returns `null` only when no
     * localization was authored.
     */
    fun resolve(locale: Locale, fallback: String? = StudyBundle.DEFAULT_LOCALE): String? =
        selectLocalization(values.entries.map { it.value to it.key }, locale, fallback)

    companion object {
        /**
         * Text with no localizations.
         */
        val empty = LocalizedText(emptyMap())
    }
}

/**
 * Picks the candidate whose localization identifier best matches [locale].
 *
 * Candidates pair a value with its localization identifier. Matching prefers an exact language and
 * region match, then the same language in any region, then [fallback], then the first candidate.
 */
internal fun <T> selectLocalization(
    candidates: List<Pair<T, String>>,
    locale: Locale,
    fallback: String?,
): T? {
    if (candidates.isEmpty()) return null

    val language = locale.language.lowercase()
    val region = locale.country.uppercase()
    val exact = "$language-$region"

    return candidates.firstOrNull { it.second.equals(exact, ignoreCase = true) }?.first
        ?: candidates.firstOrNull { it.second.substringBefore('-').equals(language, ignoreCase = true) }?.first
        ?: fallback?.let { fb -> candidates.firstOrNull { it.second.equals(fb, ignoreCase = true) }?.first }
        ?: candidates.first().first
}
