//
// This source file is part of the My Heart Counts Android open-source project
//
// SPDX-FileCopyrightText: 2026 Stanford University and the project authors (see CONTRIBUTORS.md)
// SPDX-License-Identifier: MIT
//

package edu.stanford.myheartcounts.dashboard.entry

import android.app.Application
import androidx.health.connect.client.HealthConnectClient
import androidx.health.connect.client.permission.HealthPermission
import org.grovealliance.core.coroutines.runCatchingAsync

/**
 * Saves a group of native records atomically after checking every required write permission.
 */
interface HeartScoreRecordStore {
    suspend fun save(entry: HeartScoreEntry): Result<Unit>
}

class HeartScoreRecordStoreImpl(
    private val application: Application,
    private val mapper: HeartScoreRecordMapper,
) : HeartScoreRecordStore {
    private val client by lazy { HealthConnectClient.getOrCreate(context = application) }

    override suspend fun save(entry: HeartScoreEntry): Result<Unit> = runCatchingAsync {
        val records = mapper.records(entry = entry)
        if (records.isNotEmpty()) {
            val permissions = client.permissionController.getGrantedPermissions()
            if (records.any { HealthPermission.getWritePermission(recordType = it::class) !in permissions }) {
                throw SecurityException("Write permission was not granted")
            }
            client.insertRecords(records = records)
        }
        Unit
    }
}
