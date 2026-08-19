//
// This source file is part of the My Heart Counts Android open-source project
//
// SPDX-FileCopyrightText: 2026 Stanford University and the project authors (see CONTRIBUTORS.md)
//
// SPDX-License-Identifier: MIT

package org.grovealliance.health.internal

import org.grovealliance.health.HealthConstraint
import org.grovealliance.health.HealthDataAccessRequirements

/**
 * Internal interface representing a health configuration component.
 */
internal interface HealthConfigurationComponent {
    /**
     * The health data access requirements for this configuration component.
     */
    val dataAccessRequirements: HealthDataAccessRequirements

    /**
     * Configures the given [client] with this configuration component.
     *
     * @param client The [DefaultHealthClient] to configure.
     * @param standard The standard [HealthConstraint] to apply, if any.
     */
    suspend fun configure(client: DefaultHealthClient, standard: HealthConstraint?)
}
