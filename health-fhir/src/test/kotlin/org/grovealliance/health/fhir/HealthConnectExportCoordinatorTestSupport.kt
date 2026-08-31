//
// This source file is part of the My Heart Counts Android open-source project
//
// SPDX-FileCopyrightText: 2026 Stanford University and the project authors (see CONTRIBUTORS.md)
//
// SPDX-License-Identifier: MIT

package org.grovealliance.health.fhir

import androidx.health.connect.client.records.ActiveCaloriesBurnedRecord
import androidx.health.connect.client.records.BasalBodyTemperatureRecord
import androidx.health.connect.client.records.BasalMetabolicRateRecord
import androidx.health.connect.client.records.BloodGlucoseRecord
import androidx.health.connect.client.records.BloodPressureRecord
import androidx.health.connect.client.records.BodyTemperatureMeasurementLocation
import androidx.health.connect.client.records.BodyTemperatureRecord
import androidx.health.connect.client.records.DistanceRecord
import androidx.health.connect.client.records.HeartRateRecord
import androidx.health.connect.client.records.HeightRecord
import androidx.health.connect.client.records.MealType
import androidx.health.connect.client.records.OxygenSaturationRecord
import androidx.health.connect.client.records.Record
import androidx.health.connect.client.records.RespiratoryRateRecord
import androidx.health.connect.client.records.SleepSessionRecord
import androidx.health.connect.client.records.StepsRecord
import androidx.health.connect.client.records.WeightRecord
import androidx.health.connect.client.records.metadata.Device
import androidx.health.connect.client.records.metadata.Metadata
import androidx.health.connect.client.units.BloodGlucose
import androidx.health.connect.client.units.Energy
import androidx.health.connect.client.units.Length
import androidx.health.connect.client.units.Mass
import androidx.health.connect.client.units.Percentage
import androidx.health.connect.client.units.Power
import androidx.health.connect.client.units.Pressure
import androidx.health.connect.client.units.Temperature
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import org.hl7.fhir.r4.model.Attachment
import org.hl7.fhir.r4.model.Bundle
import org.hl7.fhir.r4.model.DocumentReference
import org.hl7.fhir.r4.model.Enumerations
import org.hl7.fhir.r4.model.Identifier
import org.hl7.fhir.r4.model.InstantType
import org.hl7.fhir.r4.model.Observation
import org.hl7.fhir.r4.model.Patient
import org.hl7.fhir.r4.model.Provenance
import org.hl7.fhir.r4.model.ResearchStudy
import java.math.BigInteger
import java.time.Instant
import java.time.ZoneOffset
import org.hl7.fhir.r4.model.Device as FhirDevice

abstract class HealthConnectExportCoordinatorTestSupport {
    protected data class SemanticVectorRecord(
        val id: String,
        val profile: String,
        val record: Record,
    )

    protected val conversionTime = Instant.parse("2026-08-19T18:00:00Z")
    protected val semanticConversionTime = Instant.parse("2026-08-21T18:00:00Z")
    protected val watch = Device(Device.TYPE_WATCH, "Example Device Company", "Study Watch")
    protected val synchronizationScope = testSynchronizationScope(
        repositoryScope = EXAMPLE_REPOSITORY_SCOPE,
        configurationFingerprint = "all-supported-records",
    )
    protected val converter = HealthConnectConverter(fhirContext(), synchronizationScope)

    @Suppress("LongMethod")
    protected fun semanticVectorRecords(): List<SemanticVectorRecord> {
        val offset = ZoneOffset.ofHours(-7)
        val metadata = { id: String ->
            metadata(
                Metadata.autoRecorded(watch),
                "semantic-$id",
                Instant.parse("2026-08-20T17:30:01Z"),
            )
        }
        val sleep = SleepSessionRecord(
            startTime = Instant.parse("2026-08-20T06:00:00Z"),
            startZoneOffset = offset,
            endTime = Instant.parse("2026-08-20T13:30:00Z"),
            endZoneOffset = offset,
            title = null,
            notes = null,
            stages = listOf(
                SleepSessionRecord.Stage(
                    Instant.parse("2026-08-20T07:10:00Z"),
                    Instant.parse("2026-08-20T07:42:00Z"),
                    SleepSessionRecord.STAGE_TYPE_LIGHT,
                ),
            ),
            metadata = metadata("sleep"),
        )
        return listOf(
            SemanticVectorRecord(
                "active-energy",
                HealthConnectContract.MOBILE_ACTIVE_ENERGY_PROFILE,
                ActiveCaloriesBurnedRecord(
                    Instant.parse("2026-08-20T15:00:00Z"),
                    offset,
                    Instant.parse("2026-08-20T16:00:00Z"),
                    offset,
                    Energy.kilocalories(312.5),
                    metadata("active-energy"),
                ),
            ),
            SemanticVectorRecord(
                "basal-body-temperature",
                HealthConnectContract.MOBILE_BASAL_BODY_TEMPERATURE_PROFILE,
                BasalBodyTemperatureRecord(
                    Instant.parse("2026-08-20T13:45:00Z"),
                    offset,
                    metadata("basal-body-temperature"),
                    Temperature.celsius(36.52),
                    BodyTemperatureMeasurementLocation.MEASUREMENT_LOCATION_MOUTH,
                ),
            ),
            SemanticVectorRecord(
                "blood-pressure",
                HealthConnectContract.MOBILE_BLOOD_PRESSURE_PROFILE,
                BloodPressureRecord(
                    Instant.parse("2026-08-20T15:10:00Z"),
                    offset,
                    metadata("blood-pressure"),
                    Pressure.millimetersOfMercury(118.0),
                    Pressure.millimetersOfMercury(76.0),
                    BloodPressureRecord.BODY_POSITION_SITTING_DOWN,
                    BloodPressureRecord.MEASUREMENT_LOCATION_LEFT_UPPER_ARM,
                ),
            ),
            SemanticVectorRecord(
                "body-height",
                HealthConnectContract.MOBILE_BODY_HEIGHT_PROFILE,
                HeightRecord(
                    Instant.parse("2026-08-20T15:15:00Z"),
                    offset,
                    Length.meters(1.712),
                    metadata("body-height"),
                ),
            ),
            SemanticVectorRecord(
                "body-temperature",
                HealthConnectContract.MOBILE_BODY_TEMPERATURE_PROFILE,
                BodyTemperatureRecord(
                    Instant.parse("2026-08-20T15:20:00Z"),
                    offset,
                    metadata("body-temperature"),
                    Temperature.celsius(37.1),
                    BodyTemperatureMeasurementLocation.MEASUREMENT_LOCATION_EAR,
                ),
            ),
            SemanticVectorRecord(
                "body-weight",
                HealthConnectContract.MOBILE_BODY_WEIGHT_PROFILE,
                WeightRecord(
                    Instant.parse("2026-08-20T15:25:00Z"),
                    offset,
                    Mass.kilograms(68.4),
                    metadata("body-weight"),
                ),
            ),
            SemanticVectorRecord(
                "distance",
                HealthConnectContract.MOBILE_DISTANCE_PROFILE,
                DistanceRecord(
                    Instant.parse("2026-08-20T14:00:00Z"),
                    offset,
                    Instant.parse("2026-08-20T14:30:00Z"),
                    offset,
                    Length.meters(4820.5),
                    metadata("distance"),
                ),
            ),
            SemanticVectorRecord(
                "heart-rate",
                HealthConnectContract.MOBILE_HEART_RATE_PROFILE,
                HeartRateRecord(
                    startTime = Instant.parse("2026-08-20T15:29:00Z"),
                    startZoneOffset = offset,
                    endTime = Instant.parse("2026-08-20T15:31:00Z"),
                    endZoneOffset = offset,
                    samples = listOf(
                        HeartRateRecord.Sample(Instant.parse("2026-08-20T15:30:00.251Z"), 72),
                    ),
                    metadata = metadata("heart-rate"),
                ),
            ),
            SemanticVectorRecord(
                "oxygen-saturation",
                HealthConnectContract.MOBILE_OXYGEN_SATURATION_PROFILE,
                OxygenSaturationRecord(
                    Instant.parse("2026-08-20T15:35:00Z"),
                    offset,
                    Percentage(98.0),
                    metadata("oxygen-saturation"),
                ),
            ),
            SemanticVectorRecord(
                "respiratory-rate",
                HealthConnectContract.MOBILE_RESPIRATORY_RATE_PROFILE,
                RespiratoryRateRecord(
                    Instant.parse("2026-08-20T15:40:00Z"),
                    offset,
                    15.0,
                    metadata("respiratory-rate"),
                ),
            ),
            SemanticVectorRecord(
                "sleep-duration",
                HealthConnectContract.MOBILE_SLEEP_DURATION_PROFILE,
                sleep,
            ),
            SemanticVectorRecord(
                "sleep-stage",
                HealthConnectContract.MOBILE_SLEEP_STAGE_PROFILE,
                sleep,
            ),
            SemanticVectorRecord(
                "step-count",
                HealthConnectContract.MOBILE_STEP_COUNT_PROFILE,
                StepsRecord(
                    Instant.parse("2026-08-20T15:00:00Z"),
                    offset,
                    Instant.parse("2026-08-20T16:00:00Z"),
                    offset,
                    1042,
                    metadata("step-count"),
                ),
            ),
        )
    }

    @Suppress("LongMethod")
    protected fun completeConformanceRecords(): List<Pair<String, Record>> {
        val start = Instant.parse("2026-08-19T08:00:00Z")
        val instant = Instant.parse("2026-08-19T16:00:00Z")
        val end = instant.plusSeconds(3_600)
        val auto = { id: String -> metadata(Metadata.autoRecorded(watch), id) }
        val glucose = { name: String, specimen: Int ->
            name to BloodGlucoseRecord(
                time = instant,
                zoneOffset = ZoneOffset.UTC,
                metadata = auto("fixture-$name"),
                level = BloodGlucose.milligramsPerDeciliter(95.5),
                specimenSource = specimen,
                mealType = MealType.MEAL_TYPE_BREAKFAST,
                relationToMeal = BloodGlucoseRecord.RELATION_TO_MEAL_BEFORE_MEAL,
            )
        }
        val stageTypes = listOf(
            SleepSessionRecord.STAGE_TYPE_UNKNOWN,
            SleepSessionRecord.STAGE_TYPE_AWAKE,
            SleepSessionRecord.STAGE_TYPE_SLEEPING,
            SleepSessionRecord.STAGE_TYPE_OUT_OF_BED,
            SleepSessionRecord.STAGE_TYPE_LIGHT,
            SleepSessionRecord.STAGE_TYPE_DEEP,
            SleepSessionRecord.STAGE_TYPE_REM,
            SleepSessionRecord.STAGE_TYPE_AWAKE_IN_BED,
        )
        val sleepStages = stageTypes.mapIndexed { index, stage ->
            SleepSessionRecord.Stage(
                start.plusSeconds(index * 3_600L),
                start.plusSeconds((index + 1) * 3_600L),
                stage,
            )
        }
        return listOf(
            "active-energy" to ActiveCaloriesBurnedRecord(
                instant,
                ZoneOffset.ofHours(-7),
                end,
                ZoneOffset.ofHours(-7),
                Energy.kilocalories(412.5),
                auto("fixture-active-energy"),
            ),
            "basal-body-temperature" to BasalBodyTemperatureRecord(
                instant,
                ZoneOffset.UTC,
                auto("fixture-basal-body-temperature"),
                Temperature.celsius(36.4),
                BodyTemperatureMeasurementLocation.MEASUREMENT_LOCATION_MOUTH,
            ),
            glucose("glucose-whole-blood", BloodGlucoseRecord.SPECIMEN_SOURCE_WHOLE_BLOOD),
            glucose("glucose-capillary", BloodGlucoseRecord.SPECIMEN_SOURCE_CAPILLARY_BLOOD),
            glucose("glucose-plasma", BloodGlucoseRecord.SPECIMEN_SOURCE_PLASMA),
            glucose("glucose-serum", BloodGlucoseRecord.SPECIMEN_SOURCE_SERUM),
            glucose("glucose-interstitial", BloodGlucoseRecord.SPECIMEN_SOURCE_INTERSTITIAL_FLUID),
            "blood-pressure" to BloodPressureRecord(
                instant,
                ZoneOffset.UTC,
                auto("fixture-blood-pressure"),
                Pressure.millimetersOfMercury(120.0),
                Pressure.millimetersOfMercury(80.0),
                BloodPressureRecord.BODY_POSITION_SITTING_DOWN,
                BloodPressureRecord.MEASUREMENT_LOCATION_LEFT_UPPER_ARM,
            ),
            "body-temperature" to BodyTemperatureRecord(
                instant,
                ZoneOffset.UTC,
                auto("fixture-body-temperature"),
                Temperature.celsius(37.1),
                BodyTemperatureMeasurementLocation.MEASUREMENT_LOCATION_EAR,
            ),
            "distance" to DistanceRecord(
                instant,
                ZoneOffset.UTC,
                end,
                ZoneOffset.UTC,
                Length.kilometers(3.25),
                auto("fixture-distance"),
            ),
            "heart-rate" to heartRateRecord(twoHeartRateSamples()),
            "height" to HeightRecord(
                instant,
                ZoneOffset.UTC,
                Length.meters(1.82),
                auto("fixture-height"),
            ),
            "oxygen-saturation" to OxygenSaturationRecord(
                instant,
                ZoneOffset.UTC,
                Percentage(98.2),
                auto("fixture-oxygen-saturation"),
            ),
            "respiratory-rate" to RespiratoryRateRecord(
                instant,
                ZoneOffset.UTC,
                14.5,
                auto("fixture-respiratory-rate"),
            ),
            "sleep" to SleepSessionRecord(
                startTime = start,
                startZoneOffset = ZoneOffset.ofHours(-7),
                endTime = start.plusSeconds(8 * 3_600L),
                endZoneOffset = ZoneOffset.ofHours(-7),
                title = "Night sleep",
                notes = "Participant-reported note",
                stages = sleepStages,
                metadata = auto("fixture-sleep"),
            ),
            "steps" to stepRecord("fixture-step"),
            "weight" to weightRecord("fixture-weight"),
        )
    }

    protected fun documentJournalEntry(healthConnectId: String): HealthConnectExportJournalEntry {
        val record = stepRecord(healthConnectId)
        val conversion = converter.convert(record, conversionTime, EventSequence("1"))
        val sourceIdentifier = conversion.sourceRecordIdentifier
        val bundle = conversion.bundle
        val observationEntry = bundle.entry.single { it.resource is Observation }
        val observation = observationEntry.resource as Observation
        val outputIdentifier = observationIdentity(observation)
        val artifactIdentifier = testIdentityKey().identifier(
            GroveOpaqueIdentityKind.SOURCE_ARTIFACT,
            "health-connect",
            "StepsRecord",
            synchronizationScope.repositoryScope.system,
            synchronizationScope.repositoryScope.value,
            healthConnectId,
            "application/octet-stream",
            "0",
        )
        observationEntry.resource = DocumentReference().apply {
            meta.addProfile(SENSOR_RECORDING_DOCUMENT_PROFILE)
            addIdentifier(sourceIdentifier.copy())
            addIdentifier(outputIdentifier.copy())
            addIdentifier(artifactIdentifier)
            status = Enumerations.DocumentReferenceStatus.CURRENT
            subject = observation.subject.copy()
            dateElement = InstantType(conversionTime.toString())
            addContent().attachment = Attachment()
                .setContentType("application/octet-stream")
                .setUrl("https://example.org/recording.bin")
        }
        bundle.entry.mapNotNull { it.resource as? Provenance }.single().target.single().type =
            "DocumentReference"
        bundle.entry.removeAll { entry ->
            entry.resource is ResearchStudy ||
                (entry.resource as? FhirDevice)?.meta?.profile?.any {
                    it.value == HealthConnectContract.MOBILE_RECORDING_DEVICE_PROFILE
                } == true
        }
        return HealthConnectExportJournalEntry(
            repositoryScopeKey = synchronizationScope.repositoryScopeKey,
            projectionScopeKey = synchronizationScope.projectionScopeKey,
            recordType = conversion.sourceRecordType,
            healthConnectId = healthConnectId,
            dataOriginPackage = record.metadata.dataOrigin.packageName,
            sourceLastModified = conversion.sourceLastModified,
            conversionContractMarker = conversion.conversionContractMarker,
            sourceRecordIdentifier = sourceIdentifier,
            bundle = bundle,
            destinationReferences = mapOf(outputIdentifier.key() to "DocumentReference/$healthConnectId"),
            lastEventSequence = EventSequence("1"),
        )
    }

    protected fun stepRecord(id: String, count: Long = 1042) = StepsRecord(
        startTime = Instant.parse("2026-08-19T16:00:00Z"),
        startZoneOffset = ZoneOffset.ofHours(-7),
        endTime = Instant.parse("2026-08-19T17:00:00Z"),
        endZoneOffset = ZoneOffset.ofHours(-7),
        count = count,
        metadata = metadata(Metadata.autoRecorded(watch), id),
    )

    /** Tear specimens are outside the admitted glucose sources, so the converter refuses this record. */
    protected fun tearGlucoseRecord(id: String) = BloodGlucoseRecord(
        time = Instant.parse("2026-08-19T16:00:00Z"),
        zoneOffset = ZoneOffset.UTC,
        metadata = metadata(Metadata.autoRecorded(watch), id),
        level = BloodGlucose.milligramsPerDeciliter(95.5),
        specimenSource = BloodGlucoseRecord.SPECIMEN_SOURCE_TEARS,
        mealType = MealType.MEAL_TYPE_UNKNOWN,
        relationToMeal = BloodGlucoseRecord.RELATION_TO_MEAL_UNKNOWN,
    )

    protected fun basalMetabolicRateRecord(id: String) = BasalMetabolicRateRecord(
        time = Instant.parse("2026-08-19T15:45:00Z"),
        zoneOffset = ZoneOffset.ofHours(-7),
        basalMetabolicRate = Power.kilocaloriesPerDay(1585.5),
        metadata = metadata(Metadata.autoRecorded(watch), id),
    )

    protected fun weightRecord(id: String) = WeightRecord(
        time = Instant.parse("2026-08-19T15:15:00Z"),
        zoneOffset = ZoneOffset.ofHours(-7),
        weight = Mass.kilograms(68.4),
        metadata = metadata(Metadata.manualEntry(), id),
    )

    protected fun heartRateRecord(
        samples: List<HeartRateRecord.Sample>,
        lastModified: Instant = Instant.parse("2026-08-19T17:30:01Z"),
        device: Device = watch,
    ) = HeartRateRecord(
        startTime = Instant.parse("2026-08-19T17:30:00Z"),
        startZoneOffset = ZoneOffset.ofHours(-7),
        endTime = Instant.parse("2026-08-19T17:31:00Z"),
        endZoneOffset = ZoneOffset.ofHours(-7),
        samples = samples,
        metadata = metadata(Metadata.autoRecorded(device), "heart-record", lastModified),
    )

    protected fun twoHeartRateSamples() = listOf(
        HeartRateRecord.Sample(Instant.parse("2026-08-19T17:30:15Z"), 72),
        HeartRateRecord.Sample(Instant.parse("2026-08-19T17:30:45Z"), 75),
    )

    protected fun metadata(
        metadata: Metadata,
        id: String,
        lastModified: Instant = TEST_SOURCE_LAST_MODIFIED,
    ): Metadata = testMetadata(metadata, id, lastModified)

    protected class InMemoryJournal(startingSequence: Long = 1L) : HealthConnectExportJournal {
        private val values = mutableMapOf<Triple<ScopeKey, String, String>, HealthConnectExportJournalEntry>()
        private val pending = mutableMapOf<Triple<ScopeKey, String, String>, HealthConnectPendingExport>()
        private var nextSequence = BigInteger.valueOf(startingSequence)
        private var nextFence = BigInteger.ONE
        private val unmatchedDeletionValues = mutableMapOf<Triple<ScopeKey, String, String>, HealthConnectUnmatchedDeletion>()
        private val rejectedRecordValues = mutableMapOf<Triple<ScopeKey, String, String>, HealthConnectRejectedRecord>()
        private val stateMutex = Mutex()
        private val typeMutexes = mutableMapOf<Pair<ScopeKey, String>, Mutex>()
        private val sourceMutexes = mutableMapOf<Triple<ScopeKey, String, String>, Mutex>()
        private val activeReconciliationFences = mutableMapOf<Pair<ScopeKey, String>, HealthConnectJournalFence>()
        private val activeSourceFences = mutableMapOf<Triple<ScopeKey, String, String>, HealthConnectJournalFence>()
        val unmatchedDeletions: List<HealthConnectUnmatchedDeletion>
            get() = unmatchedDeletionValues.values.toList()
        val rejectedRecords: List<HealthConnectRejectedRecord>
            get() = rejectedRecordValues.values.toList()
        var failCompleteNext = false
        var loseSourceLeaseBeforeNextComplete = false

        init {
            require(startingSequence > 0) { "The first event sequence must be positive." }
        }

        suspend fun entry(recordType: String, healthConnectId: String) =
            stateMutex.withLock {
                values.values.singleOrNull {
                    it.recordType == recordType && it.healthConnectId == healthConnectId
                }?.copy()
            }

        suspend fun entries(recordType: String) = stateMutex.withLock {
            values.values.filter { it.recordType == recordType }.map { it.copy() }
        }

        suspend fun pending(recordType: String, healthConnectId: String) =
            stateMutex.withLock {
                pending.values.singleOrNull {
                    it.recordType == recordType && it.healthConnectId == healthConnectId
                }
            }

        suspend fun storeLocal(entry: HealthConnectExportJournalEntry) = stateMutex.withLock {
            values[entry.sourceKey()] = entry.copy()
        }

        override suspend fun <T> withSourceTransition(
            repositoryScopeKey: ScopeKey,
            recordType: String,
            healthConnectId: String,
            reconciliationLease: HealthConnectReconciliationLease?,
            block: suspend (HealthConnectSourceTransitionLease) -> T,
        ): T {
            val typeKey = repositoryScopeKey to recordType
            val sourceKey = Triple(repositoryScopeKey, recordType, healthConnectId)
            val locks = stateMutex.withLock {
                typeMutexes.getOrPut(typeKey) { Mutex() } to sourceMutexes.getOrPut(sourceKey) { Mutex() }
            }
            return if (reconciliationLease == null) {
                locks.first.withLock {
                    runSourceTransition(sourceKey, null, locks.second, block)
                }
            } else {
                stateMutex.withLock {
                    requireReconciliationLeaseLocked(reconciliationLease, typeKey)
                }
                runSourceTransition(sourceKey, reconciliationLease, locks.second, block)
            }
        }

        override suspend fun <T> withReconciliationLease(
            repositoryScopeKey: ScopeKey,
            recordType: String,
            block: suspend (HealthConnectReconciliationLease) -> T,
        ): T {
            val typeKey = repositoryScopeKey to recordType
            val mutex = stateMutex.withLock { typeMutexes.getOrPut(typeKey) { Mutex() } }
            return mutex.withLock {
                val lease = stateMutex.withLock {
                    HealthConnectReconciliationLease(
                        repositoryScopeKey,
                        recordType,
                        allocateFenceLocked(),
                    ).also { activeReconciliationFences[typeKey] = it.fence }
                }
                try {
                    block(lease)
                } finally {
                    stateMutex.withLock {
                        if (activeReconciliationFences[typeKey] == lease.fence) {
                            activeReconciliationFences.remove(typeKey)
                        }
                    }
                }
            }
        }

        override suspend fun entry(lease: HealthConnectSourceTransitionLease) = stateMutex.withLock {
            requireSourceLeaseLocked(lease)
            values[lease.sourceKey()]?.copy()
        }

        override suspend fun entries(lease: HealthConnectReconciliationLease) = stateMutex.withLock {
            requireReconciliationLeaseLocked(lease, lease.typeKey())
            values.values.filter {
                it.repositoryScopeKey == lease.repositoryScopeKey && it.recordType == lease.recordType
            }.map { it.copy() }
        }

        override suspend fun pending(lease: HealthConnectSourceTransitionLease) = stateMutex.withLock {
            requireSourceLeaseLocked(lease)
            pending[lease.sourceKey()]
        }

        override suspend fun pendingForType(lease: HealthConnectReconciliationLease) = stateMutex.withLock {
            requireReconciliationLeaseLocked(lease, lease.typeKey())
            pending.values.filter {
                it.repositoryScopeKey == lease.repositoryScopeKey && it.recordType == lease.recordType
            }
        }

        override suspend fun stage(
            lease: HealthConnectSourceTransitionLease,
            expectedRevision: HealthConnectJournalRevision?,
            buildDraft: (eventSequence: EventSequence) -> HealthConnectPendingExportDraft,
        ): HealthConnectPendingExport = stateMutex.withLock {
            requireSourceLeaseLocked(lease)
            val key = lease.sourceKey()
            pending[key]?.let { return@withLock it }
            check(values[key]?.revision == expectedRevision) { "The journal base revision changed before staging." }
            val eventSequence = EventSequence(nextSequence.toString())
            val draft = buildDraft(eventSequence)
            check(draft.repositoryScopeKey == lease.repositoryScopeKey)
            check(draft.recordType == lease.recordType)
            check(draft.healthConnectId == lease.healthConnectId)
            val stored = HealthConnectPendingExport(
                eventSequence = eventSequence,
                baseRevision = expectedRevision,
                repositoryScopeKey = draft.repositoryScopeKey,
                projectionScopeKey = draft.projectionScopeKey,
                operation = draft.operation,
                recordType = draft.recordType,
                healthConnectId = draft.healthConnectId,
                sourceRecordIdentifier = draft.sourceRecordIdentifier.copy(),
                sourceVersion = draft.sourceVersion,
                bundle = draft.bundle.copy(),
                bundleJson = draft.bundleJson,
                payloadSha256 = draft.payloadSha256,
                retractedTargets = draft.retractedTargets,
                nextEntry = draft.nextEntry,
            )
            nextSequence += BigInteger.ONE
            pending[key] = stored
            stored
        }

        override suspend fun complete(
            lease: HealthConnectSourceTransitionLease,
            pending: HealthConnectPendingExport,
            destinationReferences: Map<FhirIdentifierKey, String>,
        ) = stateMutex.withLock {
            requireSourceLeaseLocked(lease)
            val key = lease.sourceKey()
            check(pending.sourceKey() == key)
            val entry = pending.acknowledgedEntry(destinationReferences)
            if (loseSourceLeaseBeforeNextComplete) {
                loseSourceLeaseBeforeNextComplete = false
                activeSourceFences[key] = allocateFenceLocked()
                error("The source-transition lease was lost before completion.")
            }
            if (failCompleteNext) {
                failCompleteNext = false
                error("Journal transaction did not commit.")
            }
            val storedPending = this.pending[key]
            if (storedPending == null) {
                check(values[key]?.revision == entry.revision) {
                    "Only an exact-event, exact-revision repeated completion is idempotent."
                }
                return@withLock
            }
            check(storedPending.sameExactEvent(pending)) { "Completion did not name the exact staged event." }
            check(values[key]?.revision == storedPending.baseRevision) {
                "The journal base revision changed before completion."
            }
            values[key] = entry
            this.pending.remove(key)
        }

        override suspend fun storeLocal(
            lease: HealthConnectSourceTransitionLease,
            expectedRevision: HealthConnectJournalRevision?,
            entry: HealthConnectExportJournalEntry,
        ) = stateMutex.withLock {
            requireSourceLeaseLocked(lease)
            val key = lease.sourceKey()
            check(entry.sourceKey() == key)
            check(!entry.hasActiveOutputs)
            check(pending[key] == null)
            check(values[key]?.revision == expectedRevision) { "The journal base revision changed before local storage." }
            values[key] = entry
        }

        override suspend fun recordUnmatchedDeletion(
            lease: HealthConnectSourceTransitionLease,
            deletion: HealthConnectUnmatchedDeletion,
        ) = stateMutex.withLock {
            requireSourceLeaseLocked(lease)
            check(deletion.sourceKey() == lease.sourceKey())
            unmatchedDeletionValues.putIfAbsent(
                Triple(deletion.repositoryScopeKey, deletion.recordType, deletion.healthConnectId),
                deletion,
            )
            Unit
        }

        override suspend fun recordRejectedRecord(
            lease: HealthConnectSourceTransitionLease,
            rejected: HealthConnectRejectedRecord,
        ) = stateMutex.withLock {
            requireSourceLeaseLocked(lease)
            check(rejected.sourceKey() == lease.sourceKey())
            rejectedRecordValues[Triple(rejected.repositoryScopeKey, rejected.recordType, rejected.healthConnectId)] = rejected
        }

        private suspend fun <T> runSourceTransition(
            sourceKey: Triple<ScopeKey, String, String>,
            reconciliationLease: HealthConnectReconciliationLease?,
            mutex: Mutex,
            block: suspend (HealthConnectSourceTransitionLease) -> T,
        ): T = mutex.withLock {
            val lease = stateMutex.withLock {
                reconciliationLease?.let { requireReconciliationLeaseLocked(it, sourceKey.first to sourceKey.second) }
                HealthConnectSourceTransitionLease(
                    sourceKey.first,
                    sourceKey.second,
                    sourceKey.third,
                    allocateFenceLocked(),
                    reconciliationLease?.fence,
                ).also { activeSourceFences[sourceKey] = it.fence }
            }
            try {
                block(lease)
            } finally {
                stateMutex.withLock {
                    if (activeSourceFences[sourceKey] == lease.fence) activeSourceFences.remove(sourceKey)
                }
            }
        }

        private fun requireSourceLeaseLocked(lease: HealthConnectSourceTransitionLease) {
            check(activeSourceFences[lease.sourceKey()] == lease.fence) {
                "The source-transition fence is stale or no longer owned."
            }
            lease.reconciliationFence?.let { fence ->
                check(activeReconciliationFences[lease.typeKey()] == fence) {
                    "The parent reconciliation fence is stale or no longer owned."
                }
            }
        }

        private fun requireReconciliationLeaseLocked(
            lease: HealthConnectReconciliationLease,
            expectedTypeKey: Pair<ScopeKey, String>,
        ) {
            check(lease.typeKey() == expectedTypeKey)
            check(activeReconciliationFences[expectedTypeKey] == lease.fence) {
                "The reconciliation fence is stale or no longer owned."
            }
        }

        private fun allocateFenceLocked(): HealthConnectJournalFence =
            HealthConnectJournalFence(nextFence.toString()).also { nextFence += BigInteger.ONE }

        private fun HealthConnectSourceTransitionLease.sourceKey() =
            Triple(repositoryScopeKey, recordType, healthConnectId)

        private fun HealthConnectSourceTransitionLease.typeKey() = repositoryScopeKey to recordType

        private fun HealthConnectReconciliationLease.typeKey() = repositoryScopeKey to recordType

        private fun HealthConnectPendingExport.sourceKey() =
            Triple(repositoryScopeKey, recordType, healthConnectId)

        private fun HealthConnectExportJournalEntry.sourceKey() =
            Triple(repositoryScopeKey, recordType, healthConnectId)

        private fun HealthConnectUnmatchedDeletion.sourceKey() =
            Triple(repositoryScopeKey, recordType, healthConnectId)

        private fun HealthConnectRejectedRecord.sourceKey() =
            Triple(repositoryScopeKey, recordType, healthConnectId)
    }

    protected class RecordingSink : HealthConnectExportSink {
        val batches = mutableListOf<HealthConnectExportBatch>()
        private val destinationReferences = mutableMapOf<FhirIdentifierKey, String>()
        private var nextDestinationId = 1
        var failNext = false
        var failOnAttempt: Int? = null
        var omitNextAcknowledgement = false
        var pauseBeforeNextApply: Pair<CompletableDeferred<Unit>, CompletableDeferred<Unit>>? = null
        private var attempts = 0

        override suspend fun apply(batch: HealthConnectExportBatch): HealthConnectExportAcknowledgement {
            pauseBeforeNextApply?.let { (entered, release) ->
                pauseBeforeNextApply = null
                entered.complete(Unit)
                release.await()
            }
            attempts++
            if (failNext || failOnAttempt == attempts) {
                failNext = false
                failOnAttempt = null
                error("Sink did not durably apply the batch.")
            }
            batches += batch
            if (omitNextAcknowledgement) {
                omitNextAcknowledgement = false
                return HealthConnectExportAcknowledgement(emptyMap())
            }
            return HealthConnectExportAcknowledgement(
                batch.bundle.groveOutputIdentifiers()
                    .associate { output ->
                        output.key() to destinationReferences.getOrPut(output.key()) {
                            "Resource/${nextDestinationId++}"
                        }
                    },
            )
        }

        fun observationKeys(index: Int) = batches[index].bundle.entry
            .mapNotNull { it.resource as? Observation }
            .map { observation ->
                val identifier = observationIdentity(observation)
                "${identifier.system}|${identifier.value}"
            }
    }

    protected fun fhirContext(
        recordingIdentifierValue: (Device) -> String = { "study-watch" },
    ): HealthConnectConversionContext {
        val subjectIdentifier = contextIdentifier("participant-001")
        val researchStudyIdentifier = contextIdentifier("my-heart-counts")
        return HealthConnectConversionContext(
            eventIdentifierSystem = TEST_EVENT_SYSTEM,
            entryNodeIdentifierSystem = TEST_ENTRY_NODE_SYSTEM,
            userAuthoredTextPolicy = HealthConnectUserAuthoredTextPolicy.RETAIN,
            subject = HealthConnectPatientSubject.Bundled(
                HealthConnectBundleResource(
                    subjectIdentifier,
                    Patient().apply { addIdentifier(subjectIdentifier.copy()) },
                ),
            ),
            assembler = application(
                "My Heart Counts Android FHIR Converter",
                "edu.stanford.myheartcounts.fhir",
                "1.0.0",
            ),
            researchStudies = listOf(
                HealthConnectBundleResource(
                    researchStudyIdentifier,
                    ResearchStudy().apply {
                        addIdentifier(researchStudyIdentifier.copy())
                        status = ResearchStudy.ResearchStudyStatus.ACTIVE
                    },
                ),
            ),
            recordingDevice = { source ->
                HealthConnectRecordingDeviceResource(
                    stablePerUnitToken = recordingIdentifierValue(source),
                    resource = FhirDevice().apply {
                        meta.addProfile(HealthConnectContract.MOBILE_RECORDING_DEVICE_PROFILE)
                        manufacturer = source.manufacturer
                        modelNumber = source.model
                    },
                )
            },
        )
    }

    protected fun application(
        name: String,
        packageName: String,
        version: String? = null,
    ): HealthConnectBundleResource<FhirDevice> = testApplication(name, packageName, version)

    protected fun identifier(system: String, value: String): Identifier = testIdentifier(system, value)

    protected fun contextIdentifier(value: String): Identifier = identifier(TEST_CONTEXT_IDENTIFIER_SYSTEM, value)

    protected fun Bundle.observations(): List<Observation> = entry.mapNotNull { it.resource as? Observation }

    protected fun HealthConnectConverter.convert(record: androidx.health.connect.client.records.Record, at: Instant) =
        convert(record, at, EventSequence("1"))

    companion object {
        const val SENSOR_RECORDING_DOCUMENT_PROFILE =
            "https://grovealliance.org/fhir/sensor/StructureDefinition/grove-sensor-recording-document"
    }
}
