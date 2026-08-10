//
// This source file is part of the My Heart Counts Android open-source project
//
// SPDX-FileCopyrightText: 2026 Stanford University and the project authors (see CONTRIBUTORS.md)
//
// SPDX-License-Identifier: MIT

package edu.stanford.spezi.studydefinition.fixtures

import edu.stanford.spezi.studydefinition.Component.HealthDataCollection.HistoricalDataCollection

/**
 * Fixtures for [HistoricalDataCollection]. [create] returns [HistoricalDataCollection.Disabled].
 */
object HistoricalDataCollectionFixtures {
    fun create(): HistoricalDataCollection = HistoricalDataCollection.Disabled

    fun createEnabled(
        startDate: HistoricalDataCollection.StartDate = createLastStartDate(),
    ): HistoricalDataCollection.Enabled =
        HistoricalDataCollection.Enabled(startDate = startDate)

    fun createLastStartDate(
        years: Int = 10,
    ): HistoricalDataCollection.StartDate.Last =
        HistoricalDataCollection.StartDate.Last(duration = DateComponentsFixtures.create(year = years))
}
