//
// This source file belongs to the My Heart Counts Android project
//
// SPDX-FileCopyrightText: 2026 Stanford University and the project authors (see CONTRIBUTORS.md)
//
// SPDX-License-Identifier: MIT

package org.grovealliance.health.fhir

import androidx.health.connect.client.feature.ExperimentalMindfulnessSessionApi
import androidx.health.connect.client.records.BloodGlucoseRecord
import androidx.health.connect.client.records.ExerciseLap
import androidx.health.connect.client.records.ExerciseSegment
import androidx.health.connect.client.records.ExerciseSessionRecord
import androidx.health.connect.client.records.HeartRateRecord
import androidx.health.connect.client.records.MealType
import androidx.health.connect.client.records.Record
import androidx.health.connect.client.records.StepsRecord
import androidx.health.connect.client.records.metadata.DataOrigin
import androidx.health.connect.client.records.metadata.Device
import androidx.health.connect.client.records.metadata.Metadata
import androidx.health.connect.client.testing.populatedWithTestValues
import androidx.health.connect.client.units.BloodGlucose
import org.hl7.fhir.r4.model.Bundle
import org.hl7.fhir.r4.model.Coding
import org.hl7.fhir.r4.model.Identifier
import org.hl7.fhir.r4.model.Observation
import org.hl7.fhir.r4.model.Patient
import org.hl7.fhir.r4.model.ResearchStudy
import java.time.Instant
import java.time.ZoneOffset
import org.hl7.fhir.r4.model.Device as FhirDevice

@OptIn(ExperimentalMindfulnessSessionApi::class)
abstract class HealthConnectR4ConverterTestSupport {
    protected val convertedAt = Instant.parse("2026-08-19T17:30:02Z")
    protected val synchronizationScope = testSynchronizationScope(
        repositoryScope = EXAMPLE_REPOSITORY_SCOPE,
        configurationFingerprint = "all-supported-records-v1",
    )
    protected val device = Device(
        type = Device.TYPE_WATCH,
        manufacturer = "Example Device Company",
        model = "Study Watch",
    )
    protected val subjectIdentifier = identifier(TEST_CONTEXT_IDENTIFIER_SYSTEM, "participant-001")
    protected val assemblerIdentifier = identifier(
        HealthConnectContract.ANDROID_PACKAGE_IDENTIFIER,
        "edu.stanford.myheartcounts.fhir",
    )
    protected val researchStudyIdentifier = identifier(TEST_CONTEXT_IDENTIFIER_SYSTEM, "my-heart-counts")
    protected val conversionEventIdentifier = HealthConnectIdentity.exchange(
        TEST_EVENT_SYSTEM,
        TEST_PRODUCER_INSTANCE,
        EventSequence("1"),
    )
    protected val assemblerSnapshotIdentifier = HealthConnectIdentity.deviceSnapshot(
        testIdentityKey(),
        conversionEventIdentifier,
        "application",
        assemblerIdentifier.value,
    )
    protected val recordingDeviceSnapshotIdentifier = HealthConnectIdentity.deviceSnapshot(
        testIdentityKey(),
        conversionEventIdentifier,
        "recording-device",
        "study-watch-unit-token",
    )
    protected val fhirContext = HealthConnectConversionContext(
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
            name = "My Heart Counts Android FHIR Converter",
            identifierValue = "edu.stanford.myheartcounts.fhir",
            version = "1.0.0",
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
                stablePerUnitToken = "study-watch-unit-token",
                resource = FhirDevice().apply {
                    meta.addProfile(HealthConnectContract.MOBILE_RECORDING_DEVICE_PROFILE)
                    manufacturer = source.manufacturer
                    modelNumber = source.model
                },
            )
        },
    )
    protected val converter = HealthConnectConverter(fhirContext, synchronizationScope)

    protected fun HealthConnectConverter.convert(record: Record, convertedAt: Instant): HealthConnectConversion =
        convert(record, convertedAt, EventSequence("1"))

    protected fun activeBatch(
        conversion: HealthConnectConversion,
        bundle: Bundle,
    ): HealthConnectExportBatch {
        val bundleJson = HealthConnectWireFormat.bundleJson(bundle)
        return HealthConnectExportBatch(
            eventSequence = EventSequence(bundle.identifier.value.substringAfterLast(':')),
            operation = HealthConnectExportOperation.ACTIVE,
            sourceRecordIdentifier = conversion.sourceRecordIdentifier,
            sourceVersion = conversion.sourceLastModified,
            bundle = bundle,
            bundleJson = bundleJson,
            payloadSha256 = HealthConnectWireFormat.sha256(bundleJson),
        )
    }

    protected fun metadata(
        metadata: Metadata,
        id: String = "source-record",
    ): Metadata = metadata.populatedWithTestValues(
        id = id,
        dataOrigin = DataOrigin("com.example.source"),
        lastModifiedTime = Instant.parse("2026-08-19T17:30:01Z"),
    )

    // The exercise-session primary constructor differs between the pinned compile classpath and the
    // newer client forced by connect-testing; this ten-argument overload is shared by both.
    @Suppress("LongParameterList")
    protected fun exerciseSession(
        exerciseType: Int,
        id: String,
        title: String? = null,
        notes: String? = null,
        segments: List<ExerciseSegment> = emptyList(),
        laps: List<ExerciseLap> = emptyList(),
    ): ExerciseSessionRecord {
        val start = Instant.parse("2026-08-19T08:00:00Z")
        return ExerciseSessionRecord::class.java
            .getConstructor(
                Instant::class.java,
                ZoneOffset::class.java,
                Instant::class.java,
                ZoneOffset::class.java,
                Metadata::class.java,
                Int::class.javaPrimitiveType,
                String::class.java,
                String::class.java,
                List::class.java,
                List::class.java,
            )
            .newInstance(
                start,
                ZoneOffset.UTC,
                start.plusSeconds(3_600),
                ZoneOffset.UTC,
                metadata(Metadata.autoRecorded(device), id = id),
                exerciseType,
                title,
                notes,
                segments,
                laps,
            )
    }

    protected fun heartRateRecord(
        start: Instant,
        samples: List<HeartRateRecord.Sample>,
    ) = HeartRateRecord(
        startTime = start,
        startZoneOffset = ZoneOffset.ofHours(-7),
        endTime = start.plusSeconds(60),
        endZoneOffset = ZoneOffset.ofHours(-7),
        samples = samples,
        metadata = metadata(Metadata.autoRecorded(device), id = "heart-record"),
    )

    protected fun stepRecord() = StepsRecord(
        startTime = Instant.parse("2026-08-19T16:00:00Z"),
        startZoneOffset = ZoneOffset.UTC,
        endTime = Instant.parse("2026-08-19T17:00:00Z"),
        endZoneOffset = ZoneOffset.UTC,
        count = 1,
        metadata = metadata(Metadata.autoRecorded(device)),
    )

    protected fun bloodGlucoseRecord(specimenSource: Int, id: String) = BloodGlucoseRecord(
        time = Instant.parse("2026-08-19T16:00:00Z"),
        zoneOffset = ZoneOffset.UTC,
        metadata = metadata(Metadata.autoRecorded(device), id = id),
        level = BloodGlucose.milligramsPerDeciliter(95.5),
        specimenSource = specimenSource,
        mealType = androidx.health.connect.client.records.MealType.MEAL_TYPE_UNKNOWN,
        relationToMeal = BloodGlucoseRecord.RELATION_TO_MEAL_UNKNOWN,
    )

    protected fun application(
        name: String,
        identifierValue: String,
        version: String? = null,
    ): HealthConnectBundleResource<FhirDevice> {
        val entryIdentifier = identifier(HealthConnectContract.ANDROID_PACKAGE_IDENTIFIER, identifierValue)
        return HealthConnectBundleResource(
            entryIdentifier,
            FhirDevice().apply {
                meta.addProfile(HealthConnectContract.MOBILE_APPLICATION_DEVICE_PROFILE)
                addIdentifier(entryIdentifier.copy())
                addDeviceName().setName(name).setType(FhirDevice.DeviceNameType.USERFRIENDLYNAME)
                version?.let {
                    addVersion()
                        .setType(
                            org.hl7.fhir.r4.model.CodeableConcept(
                                Coding(
                                    HealthConnectContract.MDC,
                                    HealthConnectContract.APPLICATION_SOFTWARE_VERSION,
                                    "MDC_ID_PROD_SPEC_SW",
                                ),
                            ),
                        )
                        .setValue(it)
                }
            },
        )
    }

    protected fun identifier(system: String, value: String): Identifier =
        Identifier().setSystem(system).setValue(value)

    protected fun outputIdentifier(observation: org.hl7.fhir.r4.model.Observation): Identifier =
        observationIdentity(observation)

    protected fun sourceIdentifier(observation: org.hl7.fhir.r4.model.Observation): Identifier =
        observation.identifier.single {
            it.hasGroveRole(GroveIdentifierRole.SOURCE_RECORD)
        }

    protected fun entryIdentifier(bundle: org.hl7.fhir.r4.model.Bundle, resourceType: String): Identifier =
        bundle.entry.single { it.resource.fhirType() == resourceType }
            .getExtensionByUrl(GroveExchangeIdentity.ENTRY_IDENTIFIER_EXTENSION)
            .value as Identifier

    companion object {
        const val EXAMPLE_REPOSITORY_SCOPE = "1f5c58aa-6ec6-4e79-a682-829a9debd3f5"
        const val TEST_CONTEXT_IDENTIFIER_SYSTEM = "urn:uuid:8d3fd52b-efda-5f3d-b83d-50f0a70b44aa"
    }
}
