//
// This source file is part of the My Heart Counts Android open-source project
//
// SPDX-FileCopyrightText: 2026 Stanford University and the project authors (see CONTRIBUTORS.md)
//
// SPDX-License-Identifier: MIT

package edu.stanford.myheartcounts.comorbidities

import edu.stanford.myheartcounts.model.demographics.Comorbidities

/**
 * A [ComorbiditiesStorage] holding the comorbidities in memory and recording every store.
 */
class FakeComorbiditiesStorage : ComorbiditiesStorage {
    private val _stored = mutableListOf<Comorbidities>()
    private var current: Comorbidities? = null

    /**
     * Every set of comorbidities stored so far, in order.
     */
    val stored: List<Comorbidities> get() = _stored

    override val currentComorbidities: Comorbidities? get() = current

    fun setCurrentComorbidities(comorbidities: Comorbidities?) {
        current = comorbidities
    }

    override fun storeSoon(comorbidities: Comorbidities) {
        _stored += comorbidities
        current = comorbidities
    }
}
