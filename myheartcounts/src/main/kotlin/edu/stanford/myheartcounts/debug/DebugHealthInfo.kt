//
// This source file is part of the My Heart Counts Android open-source project
//
// SPDX-FileCopyrightText: 2026 Stanford University and the project authors (see CONTRIBUTORS.md)
//
// SPDX-License-Identifier: MIT

package edu.stanford.myheartcounts.debug

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.mapLatest
import org.grovealliance.health.AnyRecordType
import org.grovealliance.health.Health
import org.grovealliance.health.HealthConnectAvailability

/**
 * The state of Health Connect and of the app's access to it, for support and diagnostics.
 *
 * @property availability whether Health Connect can be used on the device
 * @property isFullyAuthorized whether every requested permission is granted
 * @property historyAccess whether data older than the default window can be read, or `null` when no
 * data type is read or Health Connect is not available
 * @property backgroundAccess whether data can be read in the background, or `null` when no data type
 * is read or Health Connect is not available
 * @property read the data types the app reads, each with whether reading it is granted
 * @property write the data types the app writes, each with whether writing it is granted
 */
data class DebugHealthInfo(
    val availability: HealthConnectAvailability,
    val isFullyAuthorized: Boolean,
    val historyAccess: Boolean?,
    val backgroundAccess: Boolean?,
    val read: List<TypeAccess>,
    val write: List<TypeAccess>,
) {

    /**
     * Whether access to the data type named [name] is granted.
     */
    data class TypeAccess(
        val name: String,
        val isGranted: Boolean,
    )
}

/**
 * Provides the current [DebugHealthInfo], updated as it changes.
 */
interface DebugHealthInfoSource {

    /**
     * The health info, read again whenever Health Connect's availability or the app's overall
     * authorization changes.
     */
    val info: Flow<DebugHealthInfo>
}

/**
 * Default [DebugHealthInfoSource] implementation.
 */
class DebugHealthInfoSourceImpl(
    private val health: Health,
) : DebugHealthInfoSource {

    override val info: Flow<DebugHealthInfo> = combine(
        health.availability,
        health.isFullyAuthorizedState,
    ) { availability, isFullyAuthorized -> availability to isFullyAuthorized }
        .mapLatest { (availability, isFullyAuthorized) ->
            info(
                availability = availability,
                isFullyAuthorized = isFullyAuthorized,
            )
        }

    private suspend fun info(
        availability: HealthConnectAvailability,
        isFullyAuthorized: Boolean,
    ): DebugHealthInfo {
        val isAvailable = availability == HealthConnectAvailability.Available
        val requirements = health.dataAccessRequirements
        val readAccess = requirements.read.sortedBy { it.name }.associateWith { type ->
            if (isAvailable) health.readAccess(type = type) else null
        }
        val anyReadAccess = readAccess.values.firstOrNull()
        return DebugHealthInfo(
            availability = availability,
            isFullyAuthorized = isFullyAuthorized,
            historyAccess = anyReadAccess?.history,
            backgroundAccess = anyReadAccess?.background,
            read = readAccess.map { (type, access) ->
                DebugHealthInfo.TypeAccess(
                    name = type.name,
                    isGranted = access?.records == true,
                )
            },
            write = requirements.write.sortedBy { it.name }.map { type ->
                DebugHealthInfo.TypeAccess(
                    name = type.name,
                    isGranted = isAvailable && health.isAuthorizedToWrite(type = type),
                )
            },
        )
    }

    private val AnyRecordType.name: String
        get() = identifier.removeSuffix(RECORD_SUFFIX)

    private companion object {
        /**
         * The suffix shared by Health Connect record type identifiers.
         */
        const val RECORD_SUFFIX = "Record"
    }
}
