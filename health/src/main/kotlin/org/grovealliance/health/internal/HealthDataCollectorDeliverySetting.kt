//
// This source file is part of the My Heart Counts Android open-source project
//
// SPDX-FileCopyrightText: 2026 Stanford University and the project authors (see CONTRIBUTORS.md)
//
// SPDX-License-Identifier: MIT

package org.grovealliance.health.internal

import org.grovealliance.health.CollectionMode

/**
 * Internal data class representing the delivery settings for health data collection.
 *
 * @property collectionMode The mode of data collection (e.g., Automatic, Manual).
 * @property continueInBackground Indicates whether data collection should continue in the background.
 */
internal data class HealthDataCollectorDeliverySetting(
    val collectionMode: CollectionMode,
    val continueInBackground: Boolean,
)
