//
// This source file is part of the My Heart Counts open-source project
//
// SPDX-FileCopyrightText: 2026 Stanford University and the project authors (see CONTRIBUTORS.md)
//
// SPDX-License-Identifier: MIT

package edu.stanford.spezi.health

import kotlin.time.Duration

/**
 * Represents the mode of data collection for health data records
 */
sealed interface CollectionMode {
    /**
     * Automatic data collection mode with a specified polling interval
     * @param pollingInterval The interval at which data should be polled
     */
    data class Automatic(val pollingInterval: Duration) : CollectionMode

    /**
     * Manual data collection mode
     */
    data object Manual : CollectionMode
}
