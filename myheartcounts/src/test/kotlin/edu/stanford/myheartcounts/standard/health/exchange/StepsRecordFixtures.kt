//
// This source file is part of the My Heart Counts Android open-source project
//
// SPDX-FileCopyrightText: 2026 Stanford University and the project authors (see CONTRIBUTORS.md)
//
// SPDX-License-Identifier: MIT

package edu.stanford.myheartcounts.standard.health.exchange

import androidx.health.connect.client.records.StepsRecord
import androidx.health.connect.client.records.metadata.DataOrigin
import androidx.health.connect.client.records.metadata.Device
import androidx.health.connect.client.records.metadata.Metadata
import androidx.health.connect.client.testing.populatedWithTestValues
import java.time.Instant
import java.time.ZoneOffset

/**
 * Steps Records as Health Connect stores them, written by a watch.
 */
object StepsRecordFixtures {

    /**
     * A [StepsRecord] of [count] steps over the minute from [start], stored under [id].
     */
    fun create(
        id: String = "record-1",
        start: Instant = Instant.parse("2026-01-01T08:00:00Z"),
        count: Long = 100,
    ): StepsRecord = StepsRecord(
        startTime = start,
        startZoneOffset = ZoneOffset.UTC,
        endTime = start.plusSeconds(60),
        endZoneOffset = ZoneOffset.UTC,
        count = count,
        metadata = Metadata.autoRecorded(
            device = Device(
                type = Device.TYPE_WATCH,
                manufacturer = "Example Devices",
                model = "Study Watch 2",
            ),
        ).populatedWithTestValues(
            id = id,
            dataOrigin = DataOrigin(packageName = "com.example.wearable"),
            lastModifiedTime = Instant.parse("2026-01-01T12:00:00Z"),
        ),
    )
}
