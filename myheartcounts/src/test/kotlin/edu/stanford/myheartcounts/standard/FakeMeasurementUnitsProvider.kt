//
// This source file is part of the My Heart Counts Android open-source project
//
// SPDX-FileCopyrightText: 2026 Stanford University and the project authors (see CONTRIBUTORS.md)
//
// SPDX-License-Identifier: MIT

package edu.stanford.myheartcounts.standard

class FakeMeasurementUnitsProvider : MeasurementUnitsProvider {
    override var currentUnits: MeasurementUnits = MeasurementUnits.METRIC
        private set

    fun setUnits(units: MeasurementUnits) {
        currentUnits = units
    }
}
