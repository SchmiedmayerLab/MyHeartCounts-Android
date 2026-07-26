//
// This source file is part of the My Heart Counts open-source project
//
// SPDX-FileCopyrightText: 2026 Stanford University and the project authors (see CONTRIBUTORS.md)
//
// SPDX-License-Identifier: MIT

package edu.stanford.spezi.health

import java.time.Instant

/**
 * Represents the time range for collecting health data records
 */
sealed interface CollectionTimeRange {
    /**
     * Collect all new record entries
     */
    data object NewRecords : CollectionTimeRange

    /**
     * Collect record entries starting from a specific date
     * @param date The starting date for collecting records
     */
    data class StartingAt(val date: Instant) : CollectionTimeRange
}
