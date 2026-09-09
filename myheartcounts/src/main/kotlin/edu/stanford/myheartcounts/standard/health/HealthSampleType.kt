//
// This source file is part of the My Heart Counts Android open-source project
//
// SPDX-FileCopyrightText: 2026 Stanford University and the project authors (see CONTRIBUTORS.md)
//
// SPDX-License-Identifier: MIT

package edu.stanford.myheartcounts.standard.health

import androidx.health.connect.client.feature.ExperimentalMindfulnessSessionApi
import org.grovealliance.health.AnyRecordType
import org.grovealliance.health.RecordType

/**
 * How a Health Connect record type is named in the study dataset, and how it gets there.
 *
 * Both platforms write into one dataset, and the name is what joins them: the backend keys Firestore
 * collections on `HealthObservations_{identifier}` and Cloud Storage archives on
 * `{identifier}_{uuid}.json.zstd`, using the **full** HealthKit identifier — the collection is
 * `HealthObservations_HKQuantityTypeIdentifierStepCount`, not `…_stepCount`
 * (`firestore.indexes.json`, `onArchivedLiveHealthSampleUploaded.test.ts:91`).
 *
 * A Health Connect record therefore only gets a HealthKit identifier when it measures the *same
 * thing* as the HealthKit sample of that name. Where it does not, it keeps an Android-native
 * identifier instead: mixing two different measurements into one collection is a data-integrity
 * problem no analysis downstream can undo, whereas a separate collection is merely one an analyst
 * has to ask about.
 *
 * @param identifier The sample type identifier written to the backend.
 * @param strategy Where records of this type are delivered.
 */
data class HealthSampleType(
    val identifier: String,
    val strategy: HealthUploadStrategy,
) {

    companion object {

        /**
         * The identifier prefix for a measurement Health Connect offers and HealthKit does not, or
         * offers differently enough that sharing a collection would be wrong.
         */
        private const val ANDROID_PREFIX = "HealthConnectTypeIdentifier"

        /**
         * Health Connect record types that measure the same thing as the HealthKit sample they are
         * mapped onto, so their observations join the iOS ones in a shared collection.
         *
         * Cross-checked against the sample types the MHC study definition collects and the LOINC
         * codes the backend defines for them (`functions/src/models/codes/codes.ts`).
         */
        @OptIn(ExperimentalMindfulnessSessionApi::class)
        private val SHARED_WITH_HEALTH_KIT: Map<AnyRecordType, String> = mapOf(
            RecordType.activeCaloriesBurned to "HKQuantityTypeIdentifierActiveEnergyBurned",
            RecordType.basalBodyTemperature to "HKQuantityTypeIdentifierBasalBodyTemperature",
            RecordType.bloodGlucose to "HKQuantityTypeIdentifierBloodGlucose",
            RecordType.bloodPressure to "HKCorrelationTypeIdentifierBloodPressure",
            RecordType.bodyFat to "HKQuantityTypeIdentifierBodyFatPercentage",
            RecordType.bodyTemperature to "HKQuantityTypeIdentifierBodyTemperature",
            RecordType.distance to "HKQuantityTypeIdentifierDistanceWalkingRunning",
            RecordType.exerciseSession to "HKWorkoutTypeIdentifier",
            RecordType.floorsClimbed to "HKQuantityTypeIdentifierFlightsClimbed",
            RecordType.heartRate to "HKQuantityTypeIdentifierHeartRate",
            RecordType.height to "HKQuantityTypeIdentifierHeight",
            RecordType.leanBodyMass to "HKQuantityTypeIdentifierLeanBodyMass",
            RecordType.mindfulnessSession to "HKCategoryTypeIdentifierMindfulSession",
            RecordType.oxygenSaturation to "HKQuantityTypeIdentifierOxygenSaturation",
            RecordType.respiratoryRate to "HKQuantityTypeIdentifierRespiratoryRate",
            RecordType.restingHeartRate to "HKQuantityTypeIdentifierRestingHeartRate",
            RecordType.sleepSession to "HKCategoryTypeIdentifierSleepAnalysis",
            RecordType.steps to "HKQuantityTypeIdentifierStepCount",
            RecordType.vo2Max to "HKQuantityTypeIdentifierVO2Max",
            RecordType.weight to "HKQuantityTypeIdentifierBodyMass",
            RecordType.wheelchairPushes to "HKQuantityTypeIdentifierPushCount",
        )

        /**
         * Record types deliberately kept out of [SHARED_WITH_HEALTH_KIT], with the reason.
         *
         * These are the ones where a HealthKit sample of a similar-sounding name exists but measures
         * something else. They are collected all the same, under their own identifier.
         *
         * - `heartRateVariabilityRmssd`: Health Connect reports RMSSD, HealthKit reports SDNN. The
         *   two are different summaries of the same beat intervals and are not interchangeable, so
         *   they must not share `HKQuantityTypeIdentifierHeartRateVariabilitySDNN`.
         * - `basalMetabolicRate`: a power (kcal/day), where `HKQuantityTypeIdentifierBasalEnergyBurned`
         *   is an energy total over an interval.
         * - `totalCaloriesBurned`: active plus basal combined, which HealthKit only ever reports as
         *   two separate sample types.
         * - `speed`: any movement, where `HKQuantityTypeIdentifierWalkingSpeed` is walking only.
         */
        private val DELIBERATELY_ANDROID_ONLY: Set<AnyRecordType> = setOf(
            RecordType.basalMetabolicRate,
            RecordType.heartRateVariabilityRmssd,
            RecordType.speed,
            RecordType.totalCaloriesBurned,
        )

        /**
         * The sample type [recordType] is delivered as.
         *
         * Everything Grove knows about is collected; a record type with no HealthKit counterpart
         * simply gets an Android-native identifier. The upload strategy follows from that: a type
         * both platforms share goes through the local staging queue, exactly as on iOS, while a
         * type only Android produces goes straight to Cloud Storage — the same route iOS takes for
         * a sample type it does not recognize.
         */
        fun of(recordType: AnyRecordType): HealthSampleType {
            SHARED_WITH_HEALTH_KIT[recordType]?.let { identifier ->
                return HealthSampleType(
                    identifier = identifier,
                    strategy = HealthUploadStrategy.QUEUE_LOCALLY,
                )
            }
            return HealthSampleType(
                identifier = "$ANDROID_PREFIX${recordType.identifier.removeSuffix("Record")}",
                strategy = HealthUploadStrategy.FIREBASE_STORAGE,
            )
        }

        /**
         * Whether [recordType] is kept out of the shared collections on purpose rather than for want
         * of a mapping. See [DELIBERATELY_ANDROID_ONLY].
         */
        fun isDeliberatelyAndroidOnly(recordType: AnyRecordType): Boolean =
            recordType in DELIBERATELY_ANDROID_ONLY
    }
}

/**
 * Where a batch of health observations is delivered.
 *
 * Ports iOS's `HealthObservationUploadStrategy`, whose three cases the backend expects to see data
 * arrive through.
 */
enum class HealthUploadStrategy {

    /**
     * Staged on device and uploaded later as one compressed archive per sample type.
     *
     * The default for anything both platforms collect. Staging exists because these arrive
     * continuously and in small batches, and because the study holds samples back until they are
     * [HealthUploadSchedule.RETENTION_OFFSET_DAYS] old.
     */
    QUEUE_LOCALLY,

    /**
     * Written straight to `HealthObservations_{identifier}` in Firestore, in batches.
     *
     * For results the app itself derives — a walking test, say — which arrive rarely and are wanted
     * immediately.
     */
    DIRECT_FIRESTORE,

    /**
     * Compressed and uploaded to Cloud Storage immediately, skipping the staging queue.
     */
    FIREBASE_STORAGE,
}
