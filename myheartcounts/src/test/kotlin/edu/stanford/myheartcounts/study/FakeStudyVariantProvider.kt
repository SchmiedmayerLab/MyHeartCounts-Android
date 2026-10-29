//
// This source file is part of the My Heart Counts Android open-source project
//
// SPDX-FileCopyrightText: 2026 Stanford University and the project authors (see CONTRIBUTORS.md)
//
// SPDX-License-Identifier: MIT

package edu.stanford.myheartcounts.study

import edu.stanford.myheartcounts.model.StudyVariant
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import java.util.Locale

/**
 * [StudyVariantProvider] holding the variant in memory, resolving content in the locale set through
 * [setContentLocale] and United States English until then.
 */
class FakeStudyVariantProvider : StudyVariantProvider {
    private val current = MutableStateFlow(StudyVariant.Stanford)
    private var contentLocale: Locale = Locale.US

    override val variant: StateFlow<StudyVariant> = current.asStateFlow()

    override val currentVariant: StudyVariant get() = current.value

    override fun contentLocale(): Locale = contentLocale

    override fun select(variant: StudyVariant) {
        current.value = variant
    }

    override fun clear() {
        current.value = StudyVariant.Stanford
    }

    /**
     * Sets the locale [contentLocale] returns.
     */
    fun setContentLocale(locale: Locale) {
        contentLocale = locale
    }
}
