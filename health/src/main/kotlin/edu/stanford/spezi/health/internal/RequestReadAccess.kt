//
// This source file is part of the My Heart Counts Android open-source project
//
// SPDX-FileCopyrightText: 2026 Stanford University and the project authors (see CONTRIBUTORS.md)
//
// SPDX-License-Identifier: MIT

package edu.stanford.spezi.health.internal

import edu.stanford.spezi.health.AnyRecordType
import edu.stanford.spezi.health.HealthConstraint
import edu.stanford.spezi.health.HealthDataAccessRequirements

/**
 * A [HealthConfigurationComponent] that requests read access to specific Health Connect
 * [androidx.health.connect.client.records.Record] types.
 *
 * @important This module already declares all necessary
 * `android.permission.health.READ_*` permissions in the Android manifest.
 */
internal class RequestReadAccess(
    recordTypes: Set<AnyRecordType>,
) : HealthConfigurationComponent {

    /**
     * The [edu.stanford.spezi.health.HealthDataAccessRequirements] requesting read access to the specified record types.
     */
    override val dataAccessRequirements: HealthDataAccessRequirements = HealthDataAccessRequirements(read = recordTypes, write = emptySet())

    /**
     * Creates a [RequestReadAccess] component requesting read access
     * to the specified Health Connect [androidx.health.connect.client.records.Record] types.
     *
     * @param recordTypes The set of [androidx.health.connect.client.records.Record] types to request read access for.
     */
    constructor(vararg recordTypes: AnyRecordType) : this(recordTypes.toSet())

    override suspend fun configure(client: DefaultHealthClient, standard: HealthConstraint?) {
        // This component only provides data access requirements,
        // so there is nothing to configure.
    }
}
