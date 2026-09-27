//
// This source file is part of the My Heart Counts Android open-source project
//
// SPDX-FileCopyrightText: 2026 Stanford University and the project authors (see CONTRIBUTORS.md)
//
// SPDX-License-Identifier: MIT

package edu.stanford.myheartcounts.standard.health

import androidx.health.connect.client.feature.ExperimentalMindfulnessSessionApi
import edu.stanford.myheartcounts.MHCStrings
import org.grovealliance.core.ConfigurationBuilder
import org.grovealliance.core.GroveDsl
import org.grovealliance.health.AnyRecordType
import org.grovealliance.health.CollectionMode
import org.grovealliance.health.RecordType
import org.grovealliance.health.health
import org.grovealliance.ui.StringResource
import kotlin.time.Duration.Companion.minutes

/**
 * The Health Connect record types the study reads, and how they are collected.
 *
 * These are the Android counterparts of the HealthKit sample types the MHC study definition asks iOS
 * for. Everything here is read-only: the study observes a participant's health data and never writes
 * to it.
 */
@OptIn(ExperimentalMindfulnessSessionApi::class)
private val COLLECTED_RECORD_TYPES: List<AnyRecordType> = listOf(
    RecordType.activeCaloriesBurned,
    RecordType.basalBodyTemperature,
    RecordType.basalMetabolicRate,
    RecordType.bloodGlucose,
    RecordType.bloodPressure,
    RecordType.bodyFat,
    RecordType.bodyTemperature,
    RecordType.distance,
    RecordType.exerciseSession,
    RecordType.floorsClimbed,
    RecordType.heartRate,
    RecordType.heartRateVariabilityRmssd,
    RecordType.height,
    RecordType.leanBodyMass,
    RecordType.mindfulnessSession,
    RecordType.oxygenSaturation,
    RecordType.respiratoryRate,
    RecordType.restingHeartRate,
    RecordType.sleepSession,
    RecordType.speed,
    RecordType.steps,
    RecordType.totalCaloriesBurned,
    RecordType.vo2Max,
    RecordType.weight,
    RecordType.wheelchairPushes,
)

/**
 * How often Health Connect is polled for new records.
 *
 * Nothing is uploaded at this cadence — new records go into the staging queue and leave the device on
 * [HealthUploadSchedule.UPLOAD_INTERVAL_HOURS] — so this only decides how promptly a sample is
 * noticed, not how much network a participant spends.
 */
private val POLLING_INTERVAL = 15.minutes

/**
 * Registers Health Connect collection.
 *
 * Records reach [MHCStandard][edu.stanford.myheartcounts.standard.MHCStandard], which implements
 * Grove's `HealthConstraint`, and from there [MHCHealthDataHandler].
 */
@GroveDsl
fun ConfigurationBuilder.mhcHealth() {
    health {
        requestReadAccess(COLLECTED_RECORD_TYPES.toSet())
        COLLECTED_RECORD_TYPES.forEach { recordType ->
            collectRecord(
                recordType = recordType,
                start = CollectionMode.Automatic(pollingInterval = POLLING_INTERVAL),
                continueInBackground = true,
            )
        }
        privacy {
            explanationText(
                title = StringResource(MHCStrings.health_privacy_title),
                description = StringResource(MHCStrings.health_privacy_description),
            )
        }
    }
}
