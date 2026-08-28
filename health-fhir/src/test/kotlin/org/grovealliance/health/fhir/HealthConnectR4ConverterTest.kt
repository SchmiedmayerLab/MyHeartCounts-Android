//
// This source file belongs to the My Heart Counts Android project
//
// SPDX-FileCopyrightText: 2026 Stanford University and the project authors (see CONTRIBUTORS.md)
//
// SPDX-License-Identifier: MIT

package org.grovealliance.health.fhir

import androidx.health.connect.client.feature.ExperimentalMindfulnessSessionApi
import androidx.health.connect.client.records.ActiveCaloriesBurnedRecord
import androidx.health.connect.client.records.BasalBodyTemperatureRecord
import androidx.health.connect.client.records.BasalMetabolicRateRecord
import androidx.health.connect.client.records.BloodGlucoseRecord
import androidx.health.connect.client.records.BloodPressureRecord
import androidx.health.connect.client.records.BodyFatRecord
import androidx.health.connect.client.records.BodyTemperatureMeasurementLocation
import androidx.health.connect.client.records.BodyTemperatureRecord
import androidx.health.connect.client.records.BodyWaterMassRecord
import androidx.health.connect.client.records.BoneMassRecord
import androidx.health.connect.client.records.CervicalMucusRecord
import androidx.health.connect.client.records.DistanceRecord
import androidx.health.connect.client.records.ElevationGainedRecord
import androidx.health.connect.client.records.ExerciseLap
import androidx.health.connect.client.records.ExerciseSegment
import androidx.health.connect.client.records.ExerciseSessionRecord
import androidx.health.connect.client.records.FloorsClimbedRecord
import androidx.health.connect.client.records.HeartRateRecord
import androidx.health.connect.client.records.HeartRateVariabilityRmssdRecord
import androidx.health.connect.client.records.HeightRecord
import androidx.health.connect.client.records.HydrationRecord
import androidx.health.connect.client.records.IntermenstrualBleedingRecord
import androidx.health.connect.client.records.LeanBodyMassRecord
import androidx.health.connect.client.records.MealType
import androidx.health.connect.client.records.MenstruationFlowRecord
import androidx.health.connect.client.records.MenstruationPeriodRecord
import androidx.health.connect.client.records.MindfulnessSessionRecord
import androidx.health.connect.client.records.NutritionRecord
import androidx.health.connect.client.records.OvulationTestRecord
import androidx.health.connect.client.records.OxygenSaturationRecord
import androidx.health.connect.client.records.PlannedExerciseSessionRecord
import androidx.health.connect.client.records.PowerRecord
import androidx.health.connect.client.records.Record
import androidx.health.connect.client.records.RespiratoryRateRecord
import androidx.health.connect.client.records.RestingHeartRateRecord
import androidx.health.connect.client.records.SexualActivityRecord
import androidx.health.connect.client.records.SkinTemperatureRecord
import androidx.health.connect.client.records.SleepSessionRecord
import androidx.health.connect.client.records.StepsCadenceRecord
import androidx.health.connect.client.records.StepsRecord
import androidx.health.connect.client.records.TotalCaloriesBurnedRecord
import androidx.health.connect.client.records.Vo2MaxRecord
import androidx.health.connect.client.records.WeightRecord
import androidx.health.connect.client.records.WheelchairPushesRecord
import androidx.health.connect.client.records.metadata.DataOrigin
import androidx.health.connect.client.records.metadata.Device
import androidx.health.connect.client.records.metadata.Metadata
import androidx.health.connect.client.testing.populatedWithTestValues
import androidx.health.connect.client.units.BloodGlucose
import androidx.health.connect.client.units.Energy
import androidx.health.connect.client.units.Length
import androidx.health.connect.client.units.Mass
import androidx.health.connect.client.units.Percentage
import androidx.health.connect.client.units.Power
import androidx.health.connect.client.units.Pressure
import androidx.health.connect.client.units.Temperature
import androidx.health.connect.client.units.TemperatureDelta
import androidx.health.connect.client.units.Volume
import com.google.common.truth.Truth.assertThat
import org.hl7.fhir.r4.model.Coding
import org.hl7.fhir.r4.model.Identifier
import org.hl7.fhir.r4.model.Observation
import org.hl7.fhir.r4.model.Patient
import org.hl7.fhir.r4.model.Provenance
import org.hl7.fhir.r4.model.ResearchStudy
import org.hl7.fhir.r4.model.Specimen
import org.junit.Assert.assertThrows
import org.junit.Test
import java.math.BigDecimal
import java.time.Instant
import java.time.ZoneOffset
import java.util.UUID
import org.hl7.fhir.r4.model.Device as FhirDevice

@OptIn(ExperimentalMindfulnessSessionApi::class)
@Suppress("LargeClass")
class HealthConnectR4ConverterTest {
    private val convertedAt = Instant.parse("2026-08-19T17:30:02Z")
    private val synchronizationScope = HealthConnectSynchronizationScope.create(
        repositoryScope = EXAMPLE_REPOSITORY_SCOPE,
        configurationFingerprint = "all-supported-records-v1",
    )
    private val device = Device(
        type = Device.TYPE_WATCH,
        manufacturer = "Example Device Company",
        model = "Study Watch",
    )
    private val subjectIdentifier = identifier(TEST_CONTEXT_IDENTIFIER_SYSTEM, "participant-001")
    private val assemblerIdentifier = identifier(
        HealthConnectContract.ANDROID_PACKAGE_IDENTIFIER,
        "edu.stanford.myheartcounts.fhir",
    )
    private val researchStudyIdentifier = identifier(TEST_CONTEXT_IDENTIFIER_SYSTEM, "my-heart-counts")
    private val sourceApplicationIdentifier = identifier(
        HealthConnectContract.ANDROID_PACKAGE_IDENTIFIER,
        "com.example.source",
    )
    private val recordingDeviceIdentifier = identifier(TEST_CONTEXT_IDENTIFIER_SYSTEM, "study-watch")
    private val fhirContext = HealthConnectConversionContext(
        graphIdentifierSystem = "urn:grove:health-connect-graph:org.grovealliance.example",
        subject = HealthConnectBundleResource(
            subjectIdentifier,
            Patient().apply { addIdentifier(subjectIdentifier.copy()) },
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
        sourceApplication = { packageName ->
            val sourceIdentifier = identifier(HealthConnectContract.ANDROID_PACKAGE_IDENTIFIER, packageName)
            HealthConnectBundleResource(
                sourceIdentifier,
                FhirDevice().apply {
                    addIdentifier(sourceIdentifier.copy())
                },
            )
        },
        recordingDevice = { source ->
            HealthConnectRecordingDeviceResource(
                bundleResource = HealthConnectBundleResource(
                    recordingDeviceIdentifier,
                    FhirDevice().apply {
                        meta.addProfile(HealthConnectContract.MOBILE_RECORDING_DEVICE_PROFILE)
                        addIdentifier(recordingDeviceIdentifier.copy())
                        manufacturer = source.manufacturer
                        modelNumber = source.model
                    },
                ),
                identityAdmission = HealthConnectRecordingDeviceIdentityAdmission.DEPLOYMENT_SCOPED,
            )
        },
    )
    private val converter = HealthConnectConverter(fhirContext, synchronizationScope)

    private fun HealthConnectConverter.convert(record: Record, convertedAt: Instant): HealthConnectConversion =
        convert(record, convertedAt, EventSequence("1"))

    @Test
    fun `repository scope is a canonical random UUIDv4`() {
        val generated = HealthConnectSynchronizationScope.generateRepositoryScope()

        assertThat(generated).matches("[0-9a-f]{8}-[0-9a-f]{4}-4[0-9a-f]{3}-[89ab][0-9a-f]{3}-[0-9a-f]{12}")
        listOf(
            "urn:uuid:$EXAMPLE_REPOSITORY_SCOPE",
            EXAMPLE_REPOSITORY_SCOPE.uppercase(),
            "6ba7b810-9dad-51d1-80b4-00c04fd430c8",
        ).forEach { invalid ->
            assertThrows(IllegalArgumentException::class.java) {
                HealthConnectSynchronizationScope.create(invalid, "all-supported-records-v1")
            }
        }
    }

    @Test
    fun `recording-device identity disclosure is explicitly admitted and never serialized`() {
        val hardwareIdentifier = identifier("https://hardware.example.org/devices", "watch-42")
        val admitted = HealthConnectRecordingDeviceResource(
            bundleResource = HealthConnectBundleResource(
                hardwareIdentifier,
                FhirDevice().apply {
                    meta.addProfile(HealthConnectContract.MOBILE_RECORDING_DEVICE_PROFILE)
                    addIdentifier(hardwareIdentifier.copy())
                },
            ),
            identityAdmission = HealthConnectRecordingDeviceIdentityAdmission.CALLER_AUTHORIZED_HARDWARE,
        )

        assertThat(admitted.identityAdmission)
            .isEqualTo(HealthConnectRecordingDeviceIdentityAdmission.CALLER_AUTHORIZED_HARDWARE)

        val authorizedConverter = HealthConnectConverter(
            fhirContext.copy(recordingDevice = { admitted }),
            synchronizationScope,
        )
        val sourceRecord = StepsRecord(
            startTime = Instant.parse("2026-08-19T16:00:00Z"),
            startZoneOffset = ZoneOffset.UTC,
            endTime = Instant.parse("2026-08-19T17:00:00Z"),
            endZoneOffset = ZoneOffset.UTC,
            count = 1,
            metadata = metadata(Metadata.autoRecorded(device)),
        )
        val result = authorizedConverter.convert(sourceRecord, convertedAt)
        val wire = HealthConnectWireFormat.bundleJson(result.bundle)
        assertThat(wire).doesNotContain("DEPLOYMENT_SCOPED")
        assertThat(wire).doesNotContain("CALLER_AUTHORIZED_HARDWARE")
        assertThat(wire).doesNotContain("identityAdmission")

        admitted.bundleResource.resource.serialNumber = "late-mutation"
        assertThrows(IllegalArgumentException::class.java) {
            authorizedConverter.convert(sourceRecord, convertedAt)
        }
    }

    @Test
    fun `recording-device serial number is rejected even with hardware admission`() {
        val hardwareIdentifier = identifier("https://hardware.example.org/devices", "watch-42")

        assertThrows(IllegalArgumentException::class.java) {
            HealthConnectRecordingDeviceResource(
                bundleResource = HealthConnectBundleResource(
                    hardwareIdentifier,
                    FhirDevice().apply { serialNumber = "globally-linkable-serial" },
                ),
                identityAdmission = HealthConnectRecordingDeviceIdentityAdmission.CALLER_AUTHORIZED_HARDWARE,
            )
        }
    }

    @Test
    fun `context entry identity mutation after construction fails closed`() {
        fhirContext.subject.entryIdentifier.value = "mutated-patient-entry"

        assertThrows(IllegalArgumentException::class.java) {
            converter.convert(stepRecord(), convertedAt)
        }
    }

    @Test
    fun `static context resource mutation after construction is revalidated`() {
        fhirContext.assembler.resource.meta.profile.clear()

        assertThrows(IllegalArgumentException::class.java) {
            converter.convert(stepRecord(), convertedAt)
        }
    }

    @Test
    fun `context callback cannot invalidate a previously checked static resource`() {
        val mutatingContext = fhirContext.copy(
            sourceApplication = { packageName ->
                fhirContext.assembler.resource.meta.profile.clear()
                application("Source application", packageName)
            },
        )

        assertThrows(IllegalArgumentException::class.java) {
            HealthConnectConverter(mutatingContext, synchronizationScope)
                .convert(stepRecord(), convertedAt)
        }
    }

    @Test
    fun `conversion result protects its validated graph from caller mutation`() {
        val result = converter.convert(stepRecord(), convertedAt)
        val expectedSourceValue = result.sourceRecordIdentifier.value
        val expectedObservation = result.observations.single()
        val expectedProvenance = requireNotNull(result.provenance)
        val expectedBundleJson = HealthConnectWireFormat.bundleJson(result.bundle)

        result.sourceRecordIdentifier.value = "caller-mutated-source"
        result.observations.single().identifier.clear()
        requireNotNull(result.provenance).target.clear()
        result.bundle.entry.clear()

        assertThat(result.sourceRecordIdentifier.value).isEqualTo(expectedSourceValue)
        assertThat(result.observations.single().equalsDeep(expectedObservation)).isTrue()
        assertThat(requireNotNull(result.provenance).equalsDeep(expectedProvenance)).isTrue()
        assertThat(HealthConnectWireFormat.bundleJson(result.bundle)).isEqualTo(expectedBundleJson)
    }

    @Test
    fun `record class participates in repository-scoped source identity`() {
        val steps = converter.convert(
            StepsRecord(
                startTime = Instant.parse("2026-08-19T16:00:00Z"),
                startZoneOffset = ZoneOffset.UTC,
                endTime = Instant.parse("2026-08-19T17:00:00Z"),
                endZoneOffset = ZoneOffset.UTC,
                count = 1,
                metadata = metadata(Metadata.autoRecorded(device), id = "same-raw-id"),
            ),
            convertedAt,
        )
        val weight = converter.convert(
            WeightRecord(
                time = Instant.parse("2026-08-19T16:00:00Z"),
                zoneOffset = ZoneOffset.UTC,
                weight = Mass.kilograms(70.0),
                metadata = metadata(Metadata.manualEntry(), id = "same-raw-id"),
            ),
            convertedAt,
        )

        assertThat(steps.sourceRecordIdentifier.value).isNotEqualTo(weight.sourceRecordIdentifier.value)
    }

    @Test
    fun `maps a Health Connect step interval without changing its meaning`() {
        val result = converter.convert(
            StepsRecord(
                startTime = Instant.parse("2026-08-19T16:00:00Z"),
                startZoneOffset = ZoneOffset.ofHours(-7),
                endTime = Instant.parse("2026-08-19T17:00:00Z"),
                endZoneOffset = ZoneOffset.ofHours(-7),
                count = 1042,
                metadata = metadata(Metadata.autoRecorded(device)),
            ),
            convertedAt,
        )

        val observation = result.observations.single()
        assertThat(observation.meta.profile.map { it.value })
            .contains(HealthConnectContract.MOBILE_STEP_COUNT_PROFILE)
        assertThat(observation.code.codingFirstRep.code).isEqualTo("step-count-total")
        assertThat(observation.valueQuantity.value.toLong()).isEqualTo(1042)
        assertThat(observation.valueQuantity.code).isEqualTo("{steps}")
        assertThat(observation.effectivePeriod.startElement.valueAsString).isEqualTo("2026-08-19T09:00:00-07:00")
        assertThat(observation.effectivePeriod.endElement.valueAsString).isEqualTo("2026-08-19T10:00:00-07:00")
        assertThat(observation.device.reference)
            .isEqualTo(GroveExchangeIdentity.fullUrl(recordingDeviceIdentifier))
        assertThat(observation.hasExtension(HealthConnectContract.RESEARCH_STUDY_EXTENSION)).isTrue()
        assertThat(
            observation.getExtensionsByUrl(
                HealthConnectContract.HEALTH_CONNECT_RECORD_TYPE_EXTENSION,
            ).single().value.primitiveValue(),
        ).isEqualTo("StepsRecord")
        assertThat(
            (observation.getExtensionByUrl(HealthConnectContract.RECORDING_METHOD_EXTENSION).value as Coding).code,
        ).isEqualTo("automatically-recorded")
        // A one-to-one conversion emits no output identifier, so the record identifier is the
        // Observation's identity.
        assertThat(observation.identifier.none {
            it.system == HealthConnectContract.HEALTH_CONNECT_OUTPUT_IDENTIFIER
        }).isTrue()
        assertThat(result.sourceRecordIdentifier.system)
            .isEqualTo(HealthConnectContract.HEALTH_CONNECT_RECORD_IDENTIFIER)
        assertThat(sourceIdentifier(observation).value)
            .isEqualTo("v1:1f5c58aa-6ec6-4e79-a682-829a9debd3f5|StepsRecord|source-record")
        // One Record, one Observation: its identity is the record identifier itself.
        assertThat(outputIdentifier(observation).value)
            .isEqualTo(result.sourceRecordIdentifier.value)
        val provenance = requireNotNull(result.provenance)
        assertThat(provenance.entityFirstRep.what.identifier.system)
            .isEqualTo(result.sourceRecordIdentifier.system)
        assertThat(provenance.entityFirstRep.what.identifier.value)
            .isEqualTo(result.sourceRecordIdentifier.value)
        // The source version, not the conversion instant: an unchanged Record has to convert to
        // the identical graph or the outbox stops recognising it as unchanged.
        assertThat(observation.issuedElement.valueAsString).isEqualTo("2026-08-19T17:30:01Z")
        assertThat(provenance.meta.profile.map { it.value })
            .containsExactly(HealthConnectContract.HEALTH_CONNECT_PROVENANCE_PROFILE)
        assertThat(result.bundle.entry.map { it.fullUrl }).containsNoDuplicates()
        assertThat(result.bundle.entry.map { it.resource.fhirType() })
            .containsAtLeast("Patient", "ResearchStudy", "Device", "Observation", "Provenance")
    }

    @Test
    fun `maps each heart-rate sample and retains one source entity`() {
        val start = Instant.parse("2026-08-19T17:30:00Z")
        val result = converter.convert(
            HeartRateRecord(
                startTime = start,
                startZoneOffset = ZoneOffset.ofHours(-7),
                endTime = start.plusSeconds(60),
                endZoneOffset = ZoneOffset.ofHours(-7),
                samples = listOf(
                    HeartRateRecord.Sample(start.plusSeconds(15), 72),
                    HeartRateRecord.Sample(start.plusSeconds(45), 75),
                ),
                metadata = metadata(Metadata.autoRecorded(device), id = "heart-record"),
            ),
            convertedAt,
        )

        assertThat(result.observations).hasSize(2)
        assertThat(result.observationIdentifiers.map { it.value }).containsNoDuplicates()
        result.observations.forEach { observation ->
            assertThat(observation.meta.profile.map { it.value })
                .contains(HealthConnectContract.MOBILE_HEART_RATE_PROFILE)
            assertThat(observation.code.codingFirstRep.code).isEqualTo("8867-4")
            assertThat(observation.valueQuantity.code).isEqualTo("/min")
        }
        val provenance = requireNotNull(result.provenance)
        assertThat(provenance.target).hasSize(2)
        provenance.target.forEach { target ->
            assertThat(target.reference).startsWith("urn:uuid:")
            assertThat(target.identifier.hasSystem()).isTrue()
            assertThat(target.identifier.hasValue()).isTrue()
        }
        assertThat(provenance.entity).hasSize(1)
        assertThat(provenance.entityFirstRep.agentFirstRep.type.codingFirstRep.code)
            .isEqualTo("enterer")
        assertThat(provenance.entityFirstRep.agentFirstRep.who.reference)
            .isEqualTo(GroveExchangeIdentity.fullUrl(sourceApplicationIdentifier))
        assertThat(provenance.agentFirstRep.who.reference)
            .isEqualTo(GroveExchangeIdentity.fullUrl(assemblerIdentifier))
        assertThat(result.observations.map { it.device.reference }.distinct())
            .containsExactly(GroveExchangeIdentity.fullUrl(recordingDeviceIdentifier))
    }

    @Test
    fun `maps an empty zero-duration heart-rate record to a local zero-output conversion`() {
        val instant = Instant.parse("2026-08-19T17:30:00Z")
        val result = converter.convert(
            HeartRateRecord(
                startTime = instant,
                startZoneOffset = ZoneOffset.UTC,
                endTime = instant,
                endZoneOffset = ZoneOffset.UTC,
                samples = emptyList(),
                metadata = metadata(Metadata.autoRecorded(device), id = "empty-heart-rate"),
            ),
            convertedAt,
        )

        assertThat(result.observations).isEmpty()
        assertThat(result.provenance).isNull()
        assertThat(result.bundle.entry.map { it.resource.fhirType() })
            .containsNoneOf("Observation", "Provenance")
    }

    @Test
    fun `maps sleep summary and stages without discarding Health Connect distinctions`() {
        val start = Instant.parse("2026-08-19T08:00:00Z")
        val result = converter.convert(
            SleepSessionRecord(
                startTime = start,
                startZoneOffset = ZoneOffset.ofHours(-7),
                endTime = start.plusSeconds(8 * 60 * 60),
                endZoneOffset = ZoneOffset.ofHours(-7),
                title = "Night sleep",
                notes = "Participant-reported note",
                stages = listOf(
                    SleepSessionRecord.Stage(
                        start,
                        start.plusSeconds(7 * 60 * 60),
                        SleepSessionRecord.STAGE_TYPE_SLEEPING,
                    ),
                    SleepSessionRecord.Stage(
                        start.plusSeconds(7 * 60 * 60),
                        start.plusSeconds(8 * 60 * 60),
                        SleepSessionRecord.STAGE_TYPE_AWAKE_IN_BED,
                    ),
                ),
                metadata = metadata(Metadata.autoRecorded(device), id = "sleep-record"),
            ),
            convertedAt,
        )

        val summary = result.observations.single {
            it.meta.profile.any { profile -> profile.value == HealthConnectContract.MOBILE_SLEEP_DURATION_PROFILE }
        }
        val stages = result.observations.filter {
            it.meta.profile.any { profile -> profile.value == HealthConnectContract.MOBILE_SLEEP_STAGE_PROFILE }
        }
        assertThat(stages).hasSize(2)
        assertThat(summary.hasMember.map { it.reference })
            .containsExactlyElementsIn(stages.map { GroveExchangeIdentity.fullUrl(outputIdentifier(it)) })
            .inOrder()
        assertThat(summary.valueQuantity.value).isEqualTo(BigDecimal("8"))
        assertThat(summary.note.single().text).isEqualTo("Participant-reported note")
        assertThat(summary.getExtensionByUrl(HealthConnectContract.HEALTH_CONNECT_SLEEP_TITLE).value.toString())
            .isEqualTo("Night sleep")
        assertThat(stages.map { it.valueCodeableConcept.coding[0].code })
            .containsExactly("asleep-unspecified", "awake")
            .inOrder()
        assertThat(stages.map { it.valueCodeableConcept.coding[1].code })
            .containsExactly("STAGE_TYPE_SLEEPING", "STAGE_TYPE_AWAKE_IN_BED")
            .inOrder()
        assertThat(stages.first().effectivePeriod.startElement.valueAsString).isEqualTo(start.toString())
    }

    @Test
    @Suppress("LongMethod")
    fun `normalizes every admitted scalar and interval quantity to the shared mobile contract`() {
        val instant = Instant.parse("2026-08-19T16:00:00Z")
        val end = instant.plusSeconds(3_600)
        val sourceMetadata = metadata(Metadata.autoRecorded(device), id = "normalized-quantities")
        val cases = listOf(
            Triple(
                ActiveCaloriesBurnedRecord(
                    instant,
                    ZoneOffset.ofHours(-7),
                    end,
                    ZoneOffset.ofHours(-7),
                    Energy.kilocalories(412.5),
                    sourceMetadata,
                ),
                HealthConnectContract.MOBILE_ACTIVE_ENERGY_PROFILE,
                "kcal",
            ),
            Triple(
                DistanceRecord(
                    instant,
                    ZoneOffset.UTC,
                    end,
                    ZoneOffset.UTC,
                    Length.kilometers(3.25),
                    sourceMetadata,
                ),
                HealthConnectContract.MOBILE_DISTANCE_PROFILE,
                "m",
            ),
            Triple(
                HeightRecord(
                    instant,
                    ZoneOffset.UTC,
                    Length.meters(1.82),
                    sourceMetadata,
                ),
                HealthConnectContract.MOBILE_BODY_HEIGHT_PROFILE,
                "cm",
            ),
            Triple(
                OxygenSaturationRecord(
                    instant,
                    ZoneOffset.UTC,
                    Percentage(98.2),
                    sourceMetadata,
                ),
                HealthConnectContract.MOBILE_OXYGEN_SATURATION_PROFILE,
                "%",
            ),
            Triple(
                RespiratoryRateRecord(
                    instant,
                    ZoneOffset.UTC,
                    14.5,
                    sourceMetadata,
                ),
                HealthConnectContract.MOBILE_RESPIRATORY_RATE_PROFILE,
                "/min",
            ),
        )

        val observations = cases.map { (record, profile, unit) ->
            converter.convert(record, convertedAt).observations.single().also { observation ->
                assertThat(observation.meta.profile.map { it.value }).containsExactly(
                    profile,
                    HealthConnectContract.HEALTH_CONNECT_OBSERVATION_PROFILE,
                )
                assertThat(observation.valueQuantity.system).isEqualTo(HealthConnectContract.UCUM)
                assertThat(observation.valueQuantity.code).isEqualTo(unit)
            }
        }

        assertThat(observations[0].effectivePeriod.hasStart()).isTrue()
        assertThat(observations[1].effectivePeriod.hasEnd()).isTrue()
        assertThat(observations.drop(2).all { it.hasEffectiveDateTimeType() }).isTrue()
        assertThat(observations[0].valueQuantity.value).isEqualTo(BigDecimal("412.5"))
        assertThat(observations[1].valueQuantity.value).isEqualTo(BigDecimal("3250.0"))
        assertThat(observations[2].valueQuantity.value).isEqualTo(BigDecimal("182.0"))
    }

    @Test
    fun `keeps basal and general body temperature as distinct shared measurements`() {
        val instant = Instant.parse("2026-08-19T16:00:00Z")
        val sourceMetadata = metadata(Metadata.autoRecorded(device), id = "temperature")
        val basal = converter.convert(
            BasalBodyTemperatureRecord(
                instant,
                ZoneOffset.UTC,
                sourceMetadata,
                Temperature.celsius(36.4),
                BodyTemperatureMeasurementLocation.MEASUREMENT_LOCATION_MOUTH,
            ),
            convertedAt,
        ).observations.single()
        val general = converter.convert(
            BodyTemperatureRecord(
                instant,
                ZoneOffset.UTC,
                sourceMetadata,
                Temperature.celsius(37.1),
                BodyTemperatureMeasurementLocation.MEASUREMENT_LOCATION_EAR,
            ),
            convertedAt,
        ).observations.single()

        assertThat(basal.meta.profile.map { it.value }).containsExactly(
            HealthConnectContract.MOBILE_BASAL_BODY_TEMPERATURE_PROFILE,
            HealthConnectContract.HEALTH_CONNECT_OBSERVATION_PROFILE,
        ).inOrder()
        assertThat(basal.code.codingFirstRep.system).isEqualTo(HealthConnectContract.GROVE_MOBILE_MEASUREMENT)
        assertThat(basal.code.codingFirstRep.code).isEqualTo("basal-body-temperature")
        assertThat(basal.bodySite.codingFirstRep.code).isEqualTo("74262004")
        assertThat(general.meta.profile.map { it.value }).containsExactly(
            HealthConnectContract.MOBILE_BODY_TEMPERATURE_PROFILE,
            HealthConnectContract.HEALTH_CONNECT_OBSERVATION_PROFILE,
        ).inOrder()
        assertThat(general.code.codingFirstRep.code).isEqualTo("8310-5")
        assertThat(general.bodySite.codingFirstRep.code).isEqualTo("117590005")
    }

    @Test
    fun `normalizes the body-composition quantities to their shared LOINC measurements`() {
        val instant = Instant.parse("2026-08-19T16:00:00Z")
        val sourceMetadata = metadata(Metadata.autoRecorded(device), id = "body-composition")
        val cases = listOf(
            Triple(
                BodyFatRecord(instant, ZoneOffset.UTC, Percentage(23.4), sourceMetadata),
                "41982-0" to "%",
                HealthConnectContract.MOBILE_BODY_FAT_PERCENTAGE_PROFILE,
            ),
            Triple(
                BodyWaterMassRecord(instant, ZoneOffset.UTC, Mass.kilograms(41.2), sourceMetadata),
                "101683-1" to "kg",
                HealthConnectContract.MOBILE_BODY_WATER_MASS_PROFILE,
            ),
            Triple(
                BoneMassRecord(instant, ZoneOffset.UTC, Mass.kilograms(3.1), sourceMetadata),
                "101685-6" to "kg",
                HealthConnectContract.MOBILE_BONE_MASS_PROFILE,
            ),
            Triple(
                LeanBodyMassRecord(instant, ZoneOffset.UTC, Mass.kilograms(54.8), sourceMetadata),
                "91557-9" to "kg",
                HealthConnectContract.MOBILE_LEAN_BODY_MASS_PROFILE,
            ),
        )

        cases.forEach { (record, codes, profile) ->
            val (loinc, unit) = codes
            val observation = converter.convert(record, convertedAt).observations.single()
            assertThat(observation.meta.profile.map { it.value }).containsExactly(
                profile,
                HealthConnectContract.HEALTH_CONNECT_OBSERVATION_PROFILE,
            ).inOrder()
            assertThat(observation.code.codingFirstRep.system).isEqualTo(HealthConnectContract.LOINC)
            assertThat(observation.code.codingFirstRep.code).isEqualTo(loinc)
            assertThat(observation.valueQuantity.code).isEqualTo(unit)
            assertThat(observation.categoryFirstRep.codingFirstRep.code).isEqualTo("vital-signs")
            assertThat(observation.hasEffectiveDateTimeType()).isTrue()
        }
    }

    @Test
    fun `maps the adapter-owned quantity records with a single Health Connect profile claim`() {
        val instant = Instant.parse("2026-08-19T16:00:00Z")
        val end = instant.plusSeconds(3_600)
        val sourceMetadata = metadata(Metadata.autoRecorded(device), id = "adapter-owned-quantities")
        val basalMetabolicRate = converter.convert(
            BasalMetabolicRateRecord(instant, ZoneOffset.UTC, Power.kilocaloriesPerDay(1585.5), sourceMetadata),
            convertedAt,
        ).observations.single()
        val elevation = converter.convert(
            ElevationGainedRecord(instant, ZoneOffset.UTC, end, ZoneOffset.UTC, Length.meters(-12.5), sourceMetadata),
            convertedAt,
        ).observations.single()
        val totalEnergy = converter.convert(
            TotalCaloriesBurnedRecord(
                instant,
                ZoneOffset.UTC,
                end,
                ZoneOffset.UTC,
                Energy.kilocalories(2101.25),
                sourceMetadata,
            ),
            convertedAt,
        ).observations.single()

        assertThat(basalMetabolicRate.meta.profile.map { it.value })
            .containsExactly(HealthConnectContract.HEALTH_CONNECT_BASAL_METABOLIC_RATE_PROFILE)
        assertThat(basalMetabolicRate.code.codingFirstRep.system)
            .isEqualTo(HealthConnectContract.HEALTH_CONNECT_MEASUREMENT)
        assertThat(basalMetabolicRate.code.codingFirstRep.code).isEqualTo("basal-metabolic-rate")
        assertThat(basalMetabolicRate.valueQuantity.code).isEqualTo("kcal/d")
        assertThat(basalMetabolicRate.valueQuantity.value).isEqualTo(BigDecimal("1585.5"))
        assertThat(basalMetabolicRate.hasEffectiveDateTimeType()).isTrue()
        assertThat(elevation.meta.profile.map { it.value })
            .containsExactly(HealthConnectContract.HEALTH_CONNECT_ELEVATION_GAINED_PROFILE)
        assertThat(elevation.code.codingFirstRep.code).isEqualTo("elevation-gained")
        assertThat(elevation.valueQuantity.value).isEqualTo(BigDecimal("-12.5"))
        assertThat(elevation.effectivePeriod.hasStart()).isTrue()
        assertThat(totalEnergy.meta.profile.map { it.value })
            .containsExactly(HealthConnectContract.HEALTH_CONNECT_TOTAL_ENERGY_PROFILE)
        assertThat(totalEnergy.code.codingFirstRep.code).isEqualTo("total-energy-burned")
        assertThat(totalEnergy.valueQuantity.code).isEqualTo("kcal")
    }

    @Test
    fun `normalizes intake activity and fitness quantities to their catalog units`() {
        val instant = Instant.parse("2026-08-19T16:00:00Z")
        val end = instant.plusSeconds(3_600)
        val sourceMetadata = metadata(Metadata.autoRecorded(device), id = "catalog-quantities")
        val fluidIntake = converter.convert(
            HydrationRecord(instant, ZoneOffset.UTC, end, ZoneOffset.UTC, Volume.milliliters(330.0), sourceMetadata),
            convertedAt,
        ).observations.single()
        val flights = converter.convert(
            FloorsClimbedRecord(instant, ZoneOffset.UTC, end, ZoneOffset.UTC, 6.5, sourceMetadata),
            convertedAt,
        ).observations.single()
        val pushes = converter.convert(
            WheelchairPushesRecord(instant, ZoneOffset.UTC, end, ZoneOffset.UTC, 412, sourceMetadata),
            convertedAt,
        ).observations.single()
        val vo2Max = converter.convert(
            Vo2MaxRecord(instant, ZoneOffset.UTC, sourceMetadata, 44.1, Vo2MaxRecord.MEASUREMENT_METHOD_HEART_RATE_RATIO),
            convertedAt,
        ).observations.single()
        val rmssd = converter.convert(
            HeartRateVariabilityRmssdRecord(instant, ZoneOffset.UTC, 52.25, sourceMetadata),
            convertedAt,
        ).observations.single()

        assertThat(fluidIntake.code.codingFirstRep.code).isEqualTo("8985-4")
        assertThat(fluidIntake.valueQuantity.code).isEqualTo("mL")
        assertThat(fluidIntake.hasCategory()).isFalse()
        assertThat(flights.code.codingFirstRep.code).isEqualTo("100304-5")
        assertThat(flights.valueQuantity.code).isEqualTo("{flights}")
        assertThat(pushes.code.codingFirstRep.code).isEqualTo("96502-0")
        assertThat(pushes.valueQuantity.code).isEqualTo("{pushes}")
        assertThat(pushes.valueQuantity.value.toLong()).isEqualTo(412)
        assertThat(vo2Max.code.codingFirstRep.code).isEqualTo("vo2-max")
        assertThat(vo2Max.valueQuantity.code).isEqualTo("mL/kg/min")
        assertThat(rmssd.code.codingFirstRep.code).isEqualTo("heart-rate-variability-rmssd")
        assertThat(rmssd.valueQuantity.code).isEqualTo("ms")
        assertThat(rmssd.categoryFirstRep.codingFirstRep.code).isEqualTo("vital-signs")
    }

    @Test
    fun `maps resting heart rate onto its stated instant with the daily-mean method`() {
        val observation = converter.convert(
            RestingHeartRateRecord(
                time = Instant.parse("2026-08-19T15:15:00Z"),
                zoneOffset = ZoneOffset.ofHours(-7),
                beatsPerMinute = 61,
                metadata = metadata(Metadata.autoRecorded(device), id = "resting-heart-rate"),
            ),
            convertedAt,
        ).observations.single()

        assertThat(observation.meta.profile.map { it.value }).containsExactly(
            HealthConnectContract.MOBILE_RESTING_HEART_RATE_PROFILE,
            HealthConnectContract.HEALTH_CONNECT_OBSERVATION_PROFILE,
        ).inOrder()
        assertThat(observation.code.codingFirstRep.system)
            .isEqualTo(HealthConnectContract.GROVE_MOBILE_MEASUREMENT)
        assertThat(observation.code.codingFirstRep.code).isEqualTo("resting-heart-rate")
        assertThat(observation.category).isEmpty()
        assertThat(observation.effectivePeriod.startElement.valueAsString)
            .isEqualTo("2026-08-19T08:15:00-07:00")
        assertThat(observation.effectivePeriod.endElement.valueAsString)
            .isEqualTo("2026-08-19T08:15:00-07:00")
        assertThat(observation.method.codingFirstRep.system)
            .isEqualTo(HealthConnectContract.GROVE_AGGREGATION_METHOD)
        assertThat(observation.method.codingFirstRep.code).isEqualTo("daily-mean")
        assertThat(observation.valueQuantity.value.toLong()).isEqualTo(61)
        assertThat(observation.valueQuantity.code).isEqualTo("/min")
    }

    @Test
    fun `maps each power sample and keeps same-instant identities value aware`() {
        val start = Instant.parse("2026-08-19T16:00:00Z")
        val result = converter.convert(
            PowerRecord(
                startTime = start,
                startZoneOffset = ZoneOffset.ofHours(-7),
                endTime = start.plusSeconds(60),
                endZoneOffset = ZoneOffset.ofHours(-7),
                samples = listOf(
                    PowerRecord.Sample(start.plusSeconds(15), Power.watts(215.5)),
                    PowerRecord.Sample(start.plusSeconds(15), Power.watts(220.0)),
                    PowerRecord.Sample(start.plusSeconds(45), Power.watts(215.5)),
                ),
                metadata = metadata(Metadata.autoRecorded(device), id = "power-series"),
            ),
            convertedAt,
        )

        assertThat(result.observations).hasSize(3)
        assertThat(result.observationIdentifiers.map { it.value }.distinct()).hasSize(3)
        result.observations.forEach { observation ->
            assertThat(observation.meta.profile.map { it.value })
                .contains(HealthConnectContract.MOBILE_POWER_PROFILE)
            assertThat(observation.code.codingFirstRep.code).isEqualTo("power")
            assertThat(observation.valueQuantity.code).isEqualTo("W")
            assertThat(observation.hasEffectiveDateTimeType()).isTrue()
        }
        assertThat(result.observations.map { it.valueQuantity.value.toPlainString() })
            .containsExactly("215.5", "220.0", "215.5")
            .inOrder()
    }

    @Test
    fun `power sample identity is stable across replay and sample order`() {
        val start = Instant.parse("2026-08-19T16:00:00Z")
        val samples = listOf(
            PowerRecord.Sample(start.plusSeconds(15), Power.watts(215.5)),
            PowerRecord.Sample(start.plusSeconds(45), Power.watts(220.0)),
        )
        val record = { ordered: List<PowerRecord.Sample> ->
            PowerRecord(
                startTime = start,
                startZoneOffset = ZoneOffset.UTC,
                endTime = start.plusSeconds(60),
                endZoneOffset = ZoneOffset.UTC,
                samples = ordered,
                metadata = metadata(Metadata.autoRecorded(device), id = "power-replay"),
            )
        }

        val first = converter.convert(record(samples), convertedAt).observationIdentifiers.map { it.value }
        val replayed = converter.convert(record(samples.reversed()), convertedAt)
            .observationIdentifiers
            .map { it.value }

        assertThat(replayed).containsExactlyElementsIn(first).inOrder()
    }

    @Test
    fun `maps an empty steps-cadence series to a local zero-output conversion`() {
        val instant = Instant.parse("2026-08-19T16:00:00Z")
        val result = converter.convert(
            StepsCadenceRecord(
                startTime = instant,
                startZoneOffset = ZoneOffset.UTC,
                endTime = instant,
                endZoneOffset = ZoneOffset.UTC,
                samples = emptyList(),
                metadata = metadata(Metadata.autoRecorded(device), id = "empty-cadence"),
            ),
            convertedAt,
        )

        assertThat(result.observations).isEmpty()
        assertThat(result.provenance).isNull()
    }

    @Test
    fun `converts skin-temperature deltas against the explicit baseline`() {
        val start = Instant.parse("2026-08-19T02:00:00Z")
        val result = converter.convert(
            SkinTemperatureRecord(
                startTime = start,
                startZoneOffset = ZoneOffset.UTC,
                endTime = start.plusSeconds(7_200),
                endZoneOffset = ZoneOffset.UTC,
                metadata = metadata(Metadata.autoRecorded(device), id = "skin-temperature"),
                deltas = listOf(
                    SkinTemperatureRecord.Delta(start.plusSeconds(600), TemperatureDelta.celsius(-0.25)),
                    SkinTemperatureRecord.Delta(start.plusSeconds(1_200), TemperatureDelta.celsius(0.5)),
                ),
                baseline = Temperature.celsius(33.5),
                measurementLocation = SkinTemperatureRecord.MEASUREMENT_LOCATION_WRIST,
            ),
            convertedAt,
        )

        assertThat(result.observations).hasSize(2)
        assertThat(result.observationIdentifiers.map { it.value }.distinct()).hasSize(2)
        result.observations.forEach { observation ->
            assertThat(observation.meta.profile.map { it.value })
                .contains(HealthConnectContract.MOBILE_SKIN_TEMPERATURE_PROFILE)
            assertThat(observation.code.codingFirstRep.code).isEqualTo("61008-9")
            assertThat(observation.valueQuantity.code).isEqualTo("Cel")
            assertThat(observation.bodySite.codingFirstRep.code).isEqualTo("8205005")
        }
        assertThat(result.observations.map { it.valueQuantity.value.toDouble() })
            .containsExactly(33.25, 34.0)
            .inOrder()
    }

    @Test
    fun `fails closed for skin-temperature deltas without a baseline`() {
        val start = Instant.parse("2026-08-19T02:00:00Z")
        val record = SkinTemperatureRecord(
            startTime = start,
            startZoneOffset = ZoneOffset.UTC,
            endTime = start.plusSeconds(7_200),
            endZoneOffset = ZoneOffset.UTC,
            metadata = metadata(Metadata.autoRecorded(device), id = "skin-temperature-no-baseline"),
            deltas = listOf(
                SkinTemperatureRecord.Delta(start.plusSeconds(600), TemperatureDelta.celsius(-0.25)),
            ),
        )

        assertThrows(InvalidHealthConnectRecord::class.java) {
            converter.convert(record, convertedAt)
        }
    }

    @Test
    fun `fans a nutrition record out into one observation per present nutrient`() {
        val start = Instant.parse("2026-08-19T12:00:00Z")
        val result = converter.convert(
            NutritionRecord(
                startTime = start,
                startZoneOffset = ZoneOffset.ofHours(-7),
                endTime = start.plusSeconds(1_800),
                endZoneOffset = ZoneOffset.ofHours(-7),
                metadata = metadata(Metadata.manualEntry(), id = "nutrition-record"),
                energy = Energy.kilocalories(650.0),
                protein = Mass.grams(32.5),
                transFat = Mass.grams(0.5),
                vitaminC = Mass.milligrams(90.0),
            ),
            convertedAt,
        )

        assertThat(result.observations).hasSize(4)
        assertThat(result.observationIdentifiers.map { it.value }.distinct()).hasSize(4)
        val byCode = result.observations.associateBy { it.code.codingFirstRep.code }
        assertThat(byCode.keys).containsExactly("9052-2", "9080-3", "dietary-fat-trans", "dietary-vitamin-c")
        val energy = byCode.getValue("9052-2")
        assertThat(energy.code.codingFirstRep.system).isEqualTo(HealthConnectContract.LOINC)
        assertThat(energy.meta.profile.map { it.value }).containsExactly(
            HealthConnectContract.mobileDietaryProfiles.getValue("dietary-energy"),
            HealthConnectContract.HEALTH_CONNECT_OBSERVATION_PROFILE,
        ).inOrder()
        assertThat(energy.valueQuantity.code).isEqualTo("kcal")
        assertThat(energy.valueQuantity.value).isEqualTo(BigDecimal("650.0"))
        val transFat = byCode.getValue("dietary-fat-trans")
        assertThat(transFat.code.codingFirstRep.system)
            .isEqualTo(HealthConnectContract.HEALTH_CONNECT_MEASUREMENT)
        assertThat(transFat.meta.profile.map { it.value })
            .containsExactly(HealthConnectContract.HEALTH_CONNECT_DIETARY_FAT_TRANS_PROFILE)
        assertThat(transFat.valueQuantity.code).isEqualTo("g")
        val vitaminC = byCode.getValue("dietary-vitamin-c")
        assertThat(vitaminC.code.codingFirstRep.system)
            .isEqualTo(HealthConnectContract.GROVE_MOBILE_MEASUREMENT)
        assertThat(vitaminC.valueQuantity.code).isEqualTo("mg")
        assertThat(vitaminC.valueQuantity.value).isEqualTo(BigDecimal("90.0"))
        result.observations.forEach { observation ->
            assertThat(observation.hasCategory()).isFalse()
            assertThat(observation.effectivePeriod.startElement.valueAsString)
                .isEqualTo("2026-08-19T05:00:00-07:00")
            assertThat(observation.effectivePeriod.endElement.valueAsString)
                .isEqualTo("2026-08-19T05:30:00-07:00")
        }
    }

    @Test
    fun `maps an all-absent nutrition record to a local zero-output conversion`() {
        val start = Instant.parse("2026-08-19T12:00:00Z")
        val result = converter.convert(
            NutritionRecord(
                startTime = start,
                startZoneOffset = ZoneOffset.UTC,
                endTime = start.plusSeconds(1_800),
                endZoneOffset = ZoneOffset.UTC,
                metadata = metadata(Metadata.manualEntry(), id = "empty-nutrition"),
            ),
            convertedAt,
        )

        assertThat(result.observations).isEmpty()
        assertThat(result.provenance).isNull()
    }

    @Test
    fun `absorbs coded reproductive enums while retaining the exact Health Connect constants`() {
        val instant = Instant.parse("2026-08-19T07:00:00Z")
        val sourceMetadata = metadata(Metadata.manualEntry(), id = "coded-records")
        val flow = converter.convert(
            MenstruationFlowRecord(
                time = instant,
                zoneOffset = ZoneOffset.UTC,
                metadata = sourceMetadata,
                flow = MenstruationFlowRecord.FLOW_LIGHT,
            ),
            convertedAt,
        ).observations.single()
        val ovulation = converter.convert(
            OvulationTestRecord(instant, ZoneOffset.UTC, OvulationTestRecord.RESULT_HIGH, sourceMetadata),
            convertedAt,
        ).observations.single()
        val sexualActivity = converter.convert(
            SexualActivityRecord(
                time = instant,
                zoneOffset = ZoneOffset.UTC,
                metadata = sourceMetadata,
                protectionUsed = SexualActivityRecord.PROTECTION_USED_UNKNOWN,
            ),
            convertedAt,
        ).observations.single()

        assertThat(flow.meta.profile.map { it.value }).containsExactly(
            HealthConnectContract.MOBILE_MENSTRUATION_FLOW_PROFILE,
            HealthConnectContract.HEALTH_CONNECT_OBSERVATION_PROFILE,
        ).inOrder()
        assertThat(flow.code.codingFirstRep.code).isEqualTo("menstruation-flow")
        assertThat(flow.hasCategory()).isFalse()
        assertThat(flow.hasEffectiveDateTimeType()).isTrue()
        assertThat(flow.valueCodeableConcept.coding.map { it.system to it.code }).containsExactly(
            HealthConnectContract.GROVE_MENSTRUATION_FLOW to "light",
            HealthConnectContract.HEALTH_CONNECT_MENSTRUATION_FLOW to "FLOW_LIGHT",
        ).inOrder()
        assertThat(ovulation.code.codingFirstRep.code).isEqualTo("ovulation-test-result")
        assertThat(ovulation.valueCodeableConcept.coding.map { it.system to it.code }).containsExactly(
            HealthConnectContract.GROVE_OVULATION_TEST_RESULT to "high-fertility",
            HealthConnectContract.HEALTH_CONNECT_OVULATION_TEST_RESULT to "RESULT_HIGH",
        ).inOrder()
        assertThat(sexualActivity.code.codingFirstRep.code).isEqualTo("sexual-activity")
        assertThat(sexualActivity.valueCodeableConcept.coding.map { it.system to it.code }).containsExactly(
            HealthConnectContract.GROVE_SEXUAL_ACTIVITY to "unknown",
            HealthConnectContract.HEALTH_CONNECT_SEXUAL_ACTIVITY_PROTECTION to "PROTECTION_USED_UNKNOWN",
        ).inOrder()
    }

    @Test
    fun `maps cervical mucus quality with its optional sensation component`() {
        val instant = Instant.parse("2026-08-19T07:00:00Z")
        val withSensation = converter.convert(
            CervicalMucusRecord(
                time = instant,
                zoneOffset = ZoneOffset.UTC,
                metadata = metadata(Metadata.manualEntry(), id = "cervical-mucus"),
                appearance = CervicalMucusRecord.APPEARANCE_EGG_WHITE,
                sensation = CervicalMucusRecord.SENSATION_MEDIUM,
            ),
            convertedAt,
        ).observations.single()
        val withoutSensation = converter.convert(
            CervicalMucusRecord(
                time = instant,
                zoneOffset = ZoneOffset.UTC,
                metadata = metadata(Metadata.manualEntry(), id = "cervical-mucus-no-sensation"),
                appearance = CervicalMucusRecord.APPEARANCE_DRY,
                sensation = CervicalMucusRecord.SENSATION_UNKNOWN,
            ),
            convertedAt,
        ).observations.single()

        assertThat(withSensation.code.codingFirstRep.code).isEqualTo("cervical-mucus-quality")
        assertThat(withSensation.valueCodeableConcept.coding.map { it.system to it.code }).containsExactly(
            HealthConnectContract.GROVE_CERVICAL_MUCUS_QUALITY to "egg-white",
            HealthConnectContract.HEALTH_CONNECT_CERVICAL_MUCUS_APPEARANCE to "APPEARANCE_EGG_WHITE",
        ).inOrder()
        val component = withSensation.component.single()
        assertThat(component.code.codingFirstRep.system)
            .isEqualTo(HealthConnectContract.GROVE_MOBILE_MEASUREMENT)
        assertThat(component.code.codingFirstRep.code).isEqualTo("cervical-mucus-sensation")
        assertThat(component.valueCodeableConcept.coding.map { it.system to it.code }).containsExactly(
            HealthConnectContract.GROVE_CERVICAL_MUCUS_SENSATION to "medium",
            HealthConnectContract.HEALTH_CONNECT_CERVICAL_MUCUS_SENSATION to "SENSATION_MEDIUM",
        ).inOrder()
        assertThat(withoutSensation.valueCodeableConcept.codingFirstRep.code).isEqualTo("dry")
        assertThat(withoutSensation.hasComponent()).isFalse()
    }

    @Test
    fun `marks intermenstrual bleeding and menstruation periods as present`() {
        val instant = Instant.parse("2026-08-19T07:00:00Z")
        val bleeding = converter.convert(
            IntermenstrualBleedingRecord(
                instant,
                ZoneOffset.UTC,
                metadata(Metadata.manualEntry(), id = "intermenstrual-bleeding"),
            ),
            convertedAt,
        ).observations.single()
        val period = converter.convert(
            MenstruationPeriodRecord(
                startTime = instant,
                startZoneOffset = ZoneOffset.UTC,
                endTime = instant.plusSeconds(4 * 24 * 60 * 60),
                endZoneOffset = ZoneOffset.UTC,
                metadata = metadata(Metadata.manualEntry(), id = "menstruation-period"),
            ),
            convertedAt,
        ).observations.single()

        assertThat(bleeding.code.codingFirstRep.code).isEqualTo("intermenstrual-bleeding")
        assertThat(bleeding.hasEffectiveDateTimeType()).isTrue()
        assertThat(bleeding.valueCodeableConcept.coding.map { it.system to it.code }).containsExactly(
            HealthConnectContract.GROVE_INTERMENSTRUAL_BLEEDING to "present",
        )
        assertThat(period.meta.profile.map { it.value })
            .containsExactly(HealthConnectContract.HEALTH_CONNECT_MENSTRUATION_PERIOD_PROFILE)
        assertThat(period.code.codingFirstRep.system)
            .isEqualTo(HealthConnectContract.HEALTH_CONNECT_MEASUREMENT)
        assertThat(period.code.codingFirstRep.code).isEqualTo("menstruation-period")
        assertThat(period.effectivePeriod.hasStart()).isTrue()
        assertThat(period.effectivePeriod.hasEnd()).isTrue()
        assertThat(period.valueCodeableConcept.coding.map { it.system to it.code }).containsExactly(
            HealthConnectContract.HEALTH_CONNECT_MENSTRUATION_PERIOD to "present",
        )
    }

    @Test
    fun `normalizes a mindfulness session to its duration in minutes`() {
        val start = Instant.parse("2026-08-19T07:00:00Z")
        val observation = converter.convert(
            MindfulnessSessionRecord(
                startTime = start,
                startZoneOffset = ZoneOffset.ofHours(-7),
                endTime = start.plusSeconds(30 * 60),
                endZoneOffset = ZoneOffset.ofHours(-7),
                metadata = metadata(Metadata.manualEntry(), id = "mindfulness-session"),
                mindfulnessSessionType = MindfulnessSessionRecord.MINDFULNESS_SESSION_TYPE_MEDITATION,
            ),
            convertedAt,
        ).observations.single()

        assertThat(observation.meta.profile.map { it.value }).containsExactly(
            HealthConnectContract.MOBILE_MINDFULNESS_SESSION_PROFILE,
            HealthConnectContract.HEALTH_CONNECT_OBSERVATION_PROFILE,
        ).inOrder()
        assertThat(observation.code.codingFirstRep.code).isEqualTo("mindfulness-session-duration")
        assertThat(observation.valueQuantity.value).isEqualTo(BigDecimal("30"))
        assertThat(observation.valueQuantity.code).isEqualTo("min")
        assertThat(observation.effectivePeriod.startElement.valueAsString)
            .isEqualTo("2026-08-19T00:00:00-07:00")
        assertThat(observation.effectivePeriod.endElement.valueAsString)
            .isEqualTo("2026-08-19T00:30:00-07:00")
    }

    @Test
    fun `retains exact Health Connect meal and blood-pressure context in standard elements`() {
        val instant = Instant.parse("2026-08-19T16:00:00Z")
        val sourceMetadata = metadata(Metadata.autoRecorded(device), id = "context-values")
        val glucose = converter.convert(
            BloodGlucoseRecord(
                time = instant,
                zoneOffset = ZoneOffset.UTC,
                metadata = sourceMetadata,
                level = BloodGlucose.milligramsPerDeciliter(95.5),
                specimenSource = BloodGlucoseRecord.SPECIMEN_SOURCE_WHOLE_BLOOD,
                mealType = MealType.MEAL_TYPE_BREAKFAST,
                relationToMeal = BloodGlucoseRecord.RELATION_TO_MEAL_BEFORE_MEAL,
            ),
            convertedAt,
        ).observations.single()
        val mealContext = requireNotNull(
            glucose.getExtensionByUrl(HealthConnectContract.HEALTH_CONNECT_GLUCOSE_MEAL_CONTEXT),
        )
        val relation = mealContext.getExtensionByUrl("relationToMeal").value as Coding
        val meal = mealContext.getExtensionByUrl("mealType").value as Coding

        assertThat(relation.system).isEqualTo(HealthConnectContract.HEALTH_CONNECT_RELATION_TO_MEAL)
        assertThat(relation.code).isEqualTo("RELATION_TO_MEAL_BEFORE_MEAL")
        assertThat(meal.system).isEqualTo(HealthConnectContract.HEALTH_CONNECT_MEAL_TYPE)
        assertThat(meal.code).isEqualTo("MEAL_TYPE_BREAKFAST")

        val bloodPressure = converter.convert(
            BloodPressureRecord(
                time = instant,
                zoneOffset = ZoneOffset.UTC,
                metadata = sourceMetadata,
                systolic = Pressure.millimetersOfMercury(120.0),
                diastolic = Pressure.millimetersOfMercury(80.0),
                bodyPosition = BloodPressureRecord.BODY_POSITION_SITTING_DOWN,
                measurementLocation = BloodPressureRecord.MEASUREMENT_LOCATION_LEFT_UPPER_ARM,
            ),
            convertedAt,
        ).observations.single()
        val bodyPosition = bloodPressure
            .getExtensionByUrl(HealthConnectContract.OBSERVATION_BODY_POSITION)
            .value as org.hl7.fhir.r4.model.CodeableConcept

        assertThat(bodyPosition.codingFirstRep.code).isEqualTo("33586001")
        assertThat(bloodPressure.bodySite.codingFirstRep.code).isEqualTo("368208006")
    }

    @Test
    fun `selects glucose semantics from the explicit specimen source and emits the specimen graph`() {
        val cases = listOf(
            Triple(
                BloodGlucoseRecord.SPECIMEN_SOURCE_WHOLE_BLOOD,
                "2339-0" to "258580003",
                HealthConnectContract.HEALTH_CONNECT_WHOLE_BLOOD_GLUCOSE_PROFILE,
            ),
            Triple(
                BloodGlucoseRecord.SPECIMEN_SOURCE_CAPILLARY_BLOOD,
                "32016-8" to "122554006",
                HealthConnectContract.HEALTH_CONNECT_CAPILLARY_BLOOD_GLUCOSE_PROFILE,
            ),
            Triple(
                BloodGlucoseRecord.SPECIMEN_SOURCE_PLASMA,
                "2345-7" to "119361006",
                HealthConnectContract.HEALTH_CONNECT_SERUM_PLASMA_GLUCOSE_PROFILE,
            ),
            Triple(
                BloodGlucoseRecord.SPECIMEN_SOURCE_SERUM,
                "2345-7" to "119364003",
                HealthConnectContract.HEALTH_CONNECT_SERUM_PLASMA_GLUCOSE_PROFILE,
            ),
            Triple(
                BloodGlucoseRecord.SPECIMEN_SOURCE_INTERSTITIAL_FLUID,
                "99504-3" to "258479004",
                HealthConnectContract.HEALTH_CONNECT_INTERSTITIAL_GLUCOSE_PROFILE,
            ),
        )

        cases.forEachIndexed { index, (specimenSource, codes, profile) ->
            val (loinc, specimenCode) = codes
            val result = converter.convert(
                bloodGlucoseRecord(specimenSource, id = "glucose-$index"),
                convertedAt,
            )
            val observation = result.observations.single()
            val specimenEntry = result.bundle.entry.single { it.resource is Specimen }
            val specimen = specimenEntry.resource as Specimen
            assertThat(specimen.meta.profile.map { it.value }).containsExactly(
                HealthConnectContract.HEALTH_CONNECT_SPECIMEN_PROFILE,
            ).inOrder()

            assertThat(observation.meta.profile.map { it.value }).containsExactly(
                profile,
            ).inOrder()
            assertThat(
                observation.getExtensionsByUrl(
                    HealthConnectContract.HEALTH_CONNECT_RECORD_TYPE_EXTENSION,
                ).single().value.primitiveValue(),
            ).isEqualTo("BloodGlucoseRecord")
            assertThat(observation.code.codingFirstRep.code).isEqualTo(loinc)
            assertThat(observation.valueQuantity.code).isEqualTo("mg/dL")
            assertThat(observation.specimen.reference).isEqualTo(specimenEntry.fullUrl)
            assertThat(
                observation.hasExtension(HealthConnectContract.HEALTH_CONNECT_GLUCOSE_MEAL_CONTEXT),
            ).isFalse()
            assertThat(specimen.type.codingFirstRep.system).isEqualTo(HealthConnectContract.SNOMED_CT)
            assertThat(specimen.type.codingFirstRep.code).isEqualTo(specimenCode)
            assertThat(specimen.subject.reference).isEqualTo(GroveExchangeIdentity.fullUrl(subjectIdentifier))
        }
    }

    @Test
    fun `fails closed for glucose without admitted specimen semantics`() {
        listOf(
            BloodGlucoseRecord.SPECIMEN_SOURCE_UNKNOWN,
            BloodGlucoseRecord.SPECIMEN_SOURCE_TEARS,
        ).forEachIndexed { index, specimenSource ->
            assertThrows(InvalidHealthConnectRecord::class.java) {
                converter.convert(
                    bloodGlucoseRecord(specimenSource, id = "unsupported-glucose-$index"),
                    convertedAt,
                )
            }
        }
    }

    @Test
    fun `does not impose an arbitrary transport cap on heart-rate samples`() {
        val start = Instant.parse("2026-08-19T16:00:00Z")
        val samples = List(201) { index ->
            HeartRateRecord.Sample(
                time = start.plusSeconds(index.toLong()),
                beatsPerMinute = 60L + index % 20,
            )
        }
        val record = HeartRateRecord(
            startTime = start,
            startZoneOffset = null,
            endTime = start.plusSeconds(201),
            endZoneOffset = null,
            samples = samples,
            metadata = metadata(Metadata.autoRecorded(device), id = "large-heart-rate-series"),
        )

        val conversion = converter.convert(record, convertedAt)

        assertThat(conversion.observations).hasSize(201)
        assertThat(conversion.observationIdentifiers.map { it.value }.distinct()).hasSize(201)
    }

    @Test
    fun `heart-rate output identity is stable across replay and sample order`() {
        val start = Instant.parse("2026-08-19T17:30:00Z")
        val first = heartRateRecord(
            start = start,
            samples = listOf(
                HeartRateRecord.Sample(start.plusSeconds(15), 72),
                HeartRateRecord.Sample(start.plusSeconds(45), 75),
            ),
        )
        val reordered = heartRateRecord(
            start = start,
            samples = first.samples.reversed(),
        )

        val firstIdentifiers = converter.convert(first, convertedAt).observationIdentifiers.map { it.value }
        val replayedIdentifiers = converter.convert(reordered, convertedAt).observationIdentifiers.map { it.value }

        assertThat(replayedIdentifiers).containsExactlyElementsIn(firstIdentifiers).inOrder()
    }

    @Test
    fun `business identities drive RFC UUIDv5 references without producer resource ids`() {
        val record = heartRateRecord(
            start = Instant.parse("2026-08-19T17:30:00Z"),
            samples = listOf(HeartRateRecord.Sample(Instant.parse("2026-08-19T17:30:15Z"), 72)),
        )

        val first = converter.convert(record, convertedAt, EventSequence("41"))
        val replay = converter.convert(record, convertedAt, EventSequence("41"))
        val nextEvent = converter.convert(record, convertedAt, EventSequence("42"))
        val firstConversion = entryIdentifier(first.bundle, "Provenance")
        val replayConversion = entryIdentifier(replay.bundle, "Provenance")
        val nextConversion = entryIdentifier(nextEvent.bundle, "Provenance")
        val referenceUuids = first.bundle.entry.map { entry ->
            UUID.fromString(requireNotNull(entry.fullUrl).removePrefix("urn:uuid:"))
        }

        assertThat(referenceUuids.map(UUID::version).distinct()).containsExactly(5)
        assertThat(referenceUuids.map(UUID::variant).distinct()).containsExactly(2)
        assertThat(first.bundle.id).isNull()
        assertThat(first.provenance!!.id).isNull()
        assertThat(first.observations.single().id).isNull()
        assertThat(replay.bundle.identifier.value).isEqualTo(first.bundle.identifier.value)
        assertThat(replayConversion.value).isEqualTo(firstConversion.value)
        assertThat(nextEvent.bundle.identifier.value).isNotEqualTo(first.bundle.identifier.value)
        assertThat(nextConversion.value).isNotEqualTo(firstConversion.value)
        assertThat(nextEvent.observationIdentifiers.single().value)
            .isEqualTo(first.observationIdentifiers.single().value)
        assertThat(first.bundle.identifier.value)
            .isEqualTo("1f5c58aa-6ec6-4e79-a682-829a9debd3f5|41|exchange-bundle")
        assertThat(firstConversion.value)
            .isEqualTo("1f5c58aa-6ec6-4e79-a682-829a9debd3f5|41|conversion-provenance")
    }

    @Test
    fun `supporting resources cannot inject an Observation`() {
        assertThrows(IllegalArgumentException::class.java) {
            fhirContext.copy(
                supportingResources = listOf(
                    HealthConnectBundleResource(
                        identifier(TEST_CONTEXT_IDENTIFIER_SYSTEM, "forbidden-observation"),
                        Observation(),
                    ),
                ),
            )
        }
    }

    @Test
    fun `supporting resources cannot inject conversion Provenance`() {
        assertThrows(IllegalArgumentException::class.java) {
            fhirContext.copy(
                supportingResources = listOf(
                    HealthConnectBundleResource(
                        identifier(TEST_CONTEXT_IDENTIFIER_SYSTEM, "forbidden-provenance"),
                        Provenance(),
                    ),
                ),
            )
        }
    }

    @Test
    fun `conversion rejects a Bundle whose Provenance differs from its exposed result`() {
        val valid = converter.convert(stepRecord(), convertedAt)
        val mismatchedBundle = valid.bundle.apply {
            (entry.single { it.resource is Provenance }.resource as Provenance).target.clear()
        }

        assertThrows(IllegalArgumentException::class.java) {
            HealthConnectConversion(
                conversionContractVersion = valid.conversionContractVersion,
                sourceRecordIdentifier = valid.sourceRecordIdentifier,
                sourceRecordType = valid.sourceRecordType,
                sourceLastModified = valid.sourceLastModified,
                observations = valid.observations,
                provenance = valid.provenance,
                bundle = mismatchedBundle,
            )
        }
    }

    @Test
    fun `a non-ASCII record id is carried into the identity unchanged`() {
        val result = converter.convert(
            StepsRecord(
                startTime = Instant.parse("2026-08-19T16:00:00Z"),
                startZoneOffset = ZoneOffset.UTC,
                endTime = Instant.parse("2026-08-19T17:00:00Z"),
                endZoneOffset = ZoneOffset.UTC,
                count = 1,
                metadata = metadata(Metadata.autoRecorded(device), id = "héal记录"),
            ),
            convertedAt,
        )

        assertThat(result.observationIdentifiers.single().value)
            .isEqualTo("v1:1f5c58aa-6ec6-4e79-a682-829a9debd3f5|StepsRecord|héal记录")
    }

    @Test
    fun `gives same-instant heart-rate samples distinct occurrence-keyed identities`() {
        val start = Instant.parse("2026-08-19T17:30:00Z")
        val record = heartRateRecord(
            start = start,
            samples = listOf(
                HeartRateRecord.Sample(start.plusSeconds(15), 72),
                HeartRateRecord.Sample(start.plusSeconds(15), 75),
                HeartRateRecord.Sample(start.plusSeconds(15), 72),
            ),
        )

        val observations = converter.convert(record, convertedAt).observations

        // The identity carries no measured value, so three readings sharing an instant are told
        // apart by their occurrence alone. Two identical readings still get two identities.
        val record0 = sourceIdentifier(observations.first()).value
        assertThat(observations.map { outputIdentifier(it).value }).containsExactly(
            "$record0|sample|2026-08-19T17:30:15.000000000Z|0",
            "$record0|sample|2026-08-19T17:30:15.000000000Z|1",
            "$record0|sample|2026-08-19T17:30:15.000000000Z|2",
        ).inOrder()
        assertThat(observations.map { it.effectiveDateTimeType.valueAsString }.distinct())
            .containsExactly("2026-08-19T17:30:15Z")
    }

    @Test
    fun `maps manual body weight to the standard profile`() {
        val result = converter.convert(
            WeightRecord(
                time = Instant.parse("2026-08-19T15:15:00Z"),
                zoneOffset = ZoneOffset.ofHours(-7),
                weight = Mass.kilograms(68.4),
                metadata = metadata(Metadata.manualEntry(), id = "weight-record"),
            ),
            convertedAt,
        )

        val observation = result.observations.single()
        assertThat(observation.meta.profile.map { it.value })
            .contains(HealthConnectContract.MOBILE_BODY_WEIGHT_PROFILE)
        assertThat(observation.code.codingFirstRep.code).isEqualTo("29463-7")
        assertThat(observation.valueQuantity.value.toDouble()).isWithin(0.0001).of(68.4)
        assertThat(observation.valueQuantity.code).isEqualTo("kg")
        val method = observation.getExtensionByUrl(HealthConnectContract.RECORDING_METHOD_EXTENSION)
        assertThat((method.value as Coding).code).isEqualTo("manual-entry")
        assertThat(observation.hasDevice()).isFalse()
    }

    @Test
    fun `accepts both exact FHIR fourteen-hour offset boundaries`() {
        val effectiveTimes = listOf(ZoneOffset.ofHours(-14), ZoneOffset.ofHours(14)).mapIndexed { index, offset ->
            converter.convert(
                WeightRecord(
                    time = Instant.parse("2026-08-19T15:15:00Z"),
                    zoneOffset = offset,
                    weight = Mass.kilograms(68.4),
                    metadata = metadata(Metadata.manualEntry(), id = "boundary-offset-$index"),
                ),
                convertedAt,
            ).observations.single().effectiveDateTimeType.valueAsString
        }

        assertThat(effectiveTimes).containsExactly(
            "2026-08-19T01:15:00-14:00",
            "2026-08-20T05:15:00+14:00",
        ).inOrder()
    }

    @Test
    fun `canonicalizes Mobile effective instants to milliseconds with half-even rounding`() {
        val cases = listOf(
            "1970-01-01T00:00:00.000499999Z" to "1970-01-01T00:00:00Z",
            "1970-01-01T00:00:00.000500000Z" to "1970-01-01T00:00:00Z",
            "1970-01-01T00:00:00.001500000Z" to "1970-01-01T00:00:00.002Z",
            "1969-12-31T23:59:59.999500000Z" to "1970-01-01T00:00:00Z",
            "1969-12-31T23:59:59.998500000Z" to "1969-12-31T23:59:59.998Z",
        )

        val actual = cases.mapIndexed { index, (input, _) ->
            converter.convert(
                WeightRecord(
                    time = Instant.parse(input),
                    zoneOffset = null,
                    weight = Mass.kilograms(68.4),
                    metadata = metadata(Metadata.manualEntry(), id = "millisecond-rounding-$index"),
                ),
                convertedAt,
            ).observations.single().effectiveDateTimeType.valueAsString
        }

        assertThat(actual).containsExactlyElementsIn(cases.map { it.second }).inOrder()
    }

    @Test
    fun `preserves the source offset after Mobile millisecond canonicalization`() {
        val effective = converter.convert(
            WeightRecord(
                time = Instant.parse("2026-08-20T15:30:00.251499999Z"),
                zoneOffset = ZoneOffset.ofHours(-7),
                weight = Mass.kilograms(68.4),
                metadata = metadata(Metadata.manualEntry(), id = "millisecond-offset"),
            ),
            convertedAt,
        ).observations.single().effectiveDateTimeType.valueAsString

        assertThat(effective).isEqualTo("2026-08-20T08:30:00.251-07:00")
    }

    @Test
    fun `rejects offsets outside the FHIR minute precision and fourteen-hour range`() {
        val invalidOffsets = listOf(
            ZoneOffset.ofHoursMinutes(14, 1),
            ZoneOffset.ofHoursMinutes(-14, -1),
            ZoneOffset.ofHoursMinutesSeconds(5, 30, 15),
            ZoneOffset.ofHours(18),
            ZoneOffset.ofHours(-18),
        )

        invalidOffsets.forEachIndexed { index, offset ->
            assertThrows(InvalidHealthConnectRecord::class.java) {
                converter.convert(
                    WeightRecord(
                        time = Instant.parse("2026-08-19T15:15:00Z"),
                        zoneOffset = offset,
                        weight = Mass.kilograms(68.4),
                        metadata = metadata(Metadata.manualEntry(), id = "invalid-offset-$index"),
                    ),
                    convertedAt,
                )
            }
        }
    }

    @Test
    fun `rejects effective source and conversion instants outside the FHIR year range`() {
        listOf(
            Instant.parse("0000-12-31T23:59:59Z"),
            Instant.parse("+10000-01-01T00:00:00Z"),
        ).forEachIndexed { index, invalidTime ->
            assertThrows(InvalidHealthConnectRecord::class.java) {
                converter.convert(
                    WeightRecord(
                        time = invalidTime,
                        zoneOffset = ZoneOffset.UTC,
                        weight = Mass.kilograms(68.4),
                        metadata = metadata(Metadata.manualEntry(), id = "invalid-effective-$index"),
                    ),
                    convertedAt,
                )
            }
        }

        val normal = WeightRecord(
            time = Instant.parse("2026-08-19T15:15:00Z"),
            zoneOffset = ZoneOffset.UTC,
            weight = Mass.kilograms(68.4),
            metadata = metadata(Metadata.manualEntry(), id = "invalid-conversion-time"),
        )
        assertThrows(InvalidHealthConnectRecord::class.java) {
            converter.convert(normal, Instant.parse("+10000-01-01T00:00:00Z"))
        }
    }

    @Test
    fun `rejects a local date that crosses the four-digit year after applying an offset`() {
        val record = WeightRecord(
            time = Instant.parse("9999-12-31T12:00:00Z"),
            zoneOffset = ZoneOffset.ofHours(14),
            weight = Mass.kilograms(68.4),
            metadata = metadata(Metadata.manualEntry(), id = "offset-crosses-year"),
        )

        assertThrows(InvalidHealthConnectRecord::class.java) {
            converter.convert(record, convertedAt)
        }
    }

    @Test
    fun `rejects issued and heart-rate sample instants outside the FHIR year range`() {
        val futureMetadata = Metadata.manualEntry().populatedWithTestValues(
            id = "invalid-issued",
            dataOrigin = DataOrigin("com.example.source"),
            lastModifiedTime = Instant.parse("+10000-01-01T00:00:00Z"),
        )
        assertThrows(InvalidHealthConnectRecord::class.java) {
            converter.convert(
                WeightRecord(
                    time = Instant.parse("2026-08-19T15:15:00Z"),
                    zoneOffset = ZoneOffset.UTC,
                    weight = Mass.kilograms(68.4),
                    metadata = futureMetadata,
                ),
                HealthConnectWireFormat.MAX_FHIR_INSTANT,
            )
        }

        val invalidSampleTime = Instant.parse("0000-12-31T23:59:59Z")
        assertThrows(InvalidHealthConnectRecord::class.java) {
            converter.convert(
                HeartRateRecord(
                    startTime = invalidSampleTime,
                    startZoneOffset = ZoneOffset.UTC,
                    endTime = invalidSampleTime.plusSeconds(1),
                    endZoneOffset = ZoneOffset.UTC,
                    samples = listOf(HeartRateRecord.Sample(invalidSampleTime, 72)),
                    metadata = metadata(Metadata.autoRecorded(device), id = "invalid-sample"),
                ),
                convertedAt,
            )
        }
    }

    @Test
    fun `maps active recording only when Health Connect states it`() {
        val result = converter.convert(
            WeightRecord(
                time = Instant.parse("2026-08-19T15:15:00Z"),
                zoneOffset = ZoneOffset.UTC,
                weight = Mass.kilograms(68.4),
                metadata = metadata(Metadata.activelyRecorded(device), id = "active-weight"),
            ),
            convertedAt,
        )

        val method = result.observations.single()
            .getExtensionByUrl(HealthConnectContract.RECORDING_METHOD_EXTENSION)
        assertThat((method.value as Coding).code).isEqualTo("actively-recorded")
    }

    @Test
    fun `omits unknown capture mode rather than inventing one`() {
        val result = converter.convert(
            WeightRecord(
                time = Instant.parse("2026-08-19T15:15:00Z"),
                zoneOffset = null,
                weight = Mass.kilograms(68.4),
                metadata = metadata(Metadata.unknownRecordingMethod(), id = "unknown-method"),
            ),
            convertedAt,
        )

        assertThat(
            result.observations.single().hasExtension(HealthConnectContract.RECORDING_METHOD_EXTENSION),
        ).isFalse()
    }

    @Test
    fun `fails closed for an unmapped Health Connect type`() {
        val unsupported = PlannedExerciseSessionRecord(
            startTime = Instant.parse("2026-08-19T15:15:00Z"),
            startZoneOffset = ZoneOffset.UTC,
            endTime = Instant.parse("2026-08-19T16:15:00Z"),
            endZoneOffset = ZoneOffset.UTC,
            metadata = metadata(Metadata.unknownRecordingMethod(), id = "planned-exercise-session"),
            blocks = emptyList(),
            exerciseType = ExerciseSessionRecord.EXERCISE_TYPE_RUNNING,
            title = null,
            notes = null,
        )

        val refusal = assertThrows(UnsupportedHealthConnectRecord::class.java) {
            converter.convert(unsupported, convertedAt)
        }
        assertThat(refusal).hasMessageThat().contains("PlannedExerciseSessionRecord")
    }

    @Test
    fun `maps a shared exercise type onto the shared workout activity`() {
        val start = Instant.parse("2026-08-19T08:00:00Z")
        val result = converter.convert(
            exerciseSession(
                exerciseType = ExerciseSessionRecord.EXERCISE_TYPE_RUNNING,
                id = "workout-running",
                title = "Morning run",
                notes = "Participant-reported note",
            ),
            convertedAt,
        )

        val workout = result.observations.single()
        assertThat(workout.meta.profile.map { it.value })
            .containsExactly(
                HealthConnectContract.MOBILE_WORKOUT_PROFILE,
                HealthConnectContract.HEALTH_CONNECT_OBSERVATION_PROFILE,
            )
            .inOrder()
        assertThat(workout.code.codingFirstRep.code).isEqualTo("workout")
        assertThat(workout.effectivePeriod.startElement.valueAsString).isEqualTo(start.toString())
        assertThat(workout.effectivePeriod.endElement.valueAsString).isEqualTo(start.plusSeconds(3_600).toString())
        assertThat(workout.valueCodeableConcept.coding.map { it.system to it.code })
            .containsExactly(
                HealthConnectContract.GROVE_WORKOUT_ACTIVITY to "running",
                HealthConnectContract.HEALTH_CONNECT_EXERCISE_TYPE to "EXERCISE_TYPE_RUNNING",
            )
            .inOrder()
        assertThat(workout.getExtensionByUrl(HealthConnectContract.HEALTH_CONNECT_EXERCISE_TITLE).value.toString())
            .isEqualTo("Morning run")
        assertThat(workout.note.single().text).isEqualTo("Participant-reported note")
    }

    @Test
    fun `absorbs a long-tail exercise type into other while retaining its exact token`() {
        val result = converter.convert(
            exerciseSession(
                exerciseType = ExerciseSessionRecord.EXERCISE_TYPE_SURFING,
                id = "workout-surfing",
            ),
            convertedAt,
        )

        val workout = result.observations.single()
        assertThat(workout.valueCodeableConcept.coding.map { it.code })
            .containsExactly("other", "EXERCISE_TYPE_SURFING")
            .inOrder()
        assertThat(workout.valueCodeableConcept.coding[1].system)
            .isEqualTo(HealthConnectContract.HEALTH_CONNECT_EXERCISE_TYPE)
        assertThat(workout.note).isEmpty()
    }

    @Test
    @Suppress("LongMethod")
    fun `fans a session out into one workout-segment child per segment and lap`() {
        val start = Instant.parse("2026-08-19T08:00:00Z")
        val result = converter.convert(
            exerciseSession(
                exerciseType = ExerciseSessionRecord.EXERCISE_TYPE_OTHER_WORKOUT,
                id = "workout-with-children",
                segments = listOf(
                    ExerciseSegment(
                        start,
                        start.plusSeconds(1_800),
                        ExerciseSegment.EXERCISE_SEGMENT_TYPE_SQUAT,
                        12,
                    ),
                    ExerciseSegment(
                        start.plusSeconds(1_800),
                        start.plusSeconds(2_400),
                        ExerciseSegment.EXERCISE_SEGMENT_TYPE_PAUSE,
                        0,
                    ),
                ),
                laps = listOf(
                    ExerciseLap(start, start.plusSeconds(900), Length.meters(400.0)),
                    ExerciseLap(start.plusSeconds(900), start.plusSeconds(1_800), null),
                ),
            ),
            convertedAt,
        )

        val workout = result.observations.single {
            it.meta.profile.any { profile -> profile.value == HealthConnectContract.MOBILE_WORKOUT_PROFILE }
        }
        val children = result.observations.filter {
            it.meta.profile.any { profile -> profile.value == HealthConnectContract.MOBILE_WORKOUT_SEGMENT_PROFILE }
        }
        assertThat(children).hasSize(4)
        assertThat(workout.hasMember.map { it.reference })
            .containsExactlyElementsIn(children.map { GroveExchangeIdentity.fullUrl(outputIdentifier(it)) })
            .inOrder()
        assertThat(children.map { it.valueCodeableConcept.coding[0].system to it.valueCodeableConcept.coding[0].code })
            .containsExactly(
                HealthConnectContract.GROVE_WORKOUT_ACTIVITY to "strength-training",
                HealthConnectContract.GROVE_WORKOUT_SEGMENT_TYPE to "pause",
                HealthConnectContract.GROVE_WORKOUT_SEGMENT_TYPE to "lap",
                HealthConnectContract.GROVE_WORKOUT_SEGMENT_TYPE to "lap",
            )
            .inOrder()
        assertThat(children.map { it.valueCodeableConcept.coding[1].code })
            .containsExactly(
                "EXERCISE_SEGMENT_TYPE_SQUAT",
                "EXERCISE_SEGMENT_TYPE_PAUSE",
                "EXERCISE_LAP",
                "EXERCISE_LAP",
            )
            .inOrder()
        assertThat(children.map { child -> child.component.map { it.code.codingFirstRep.code } })
            .containsExactly(listOf("repetitions"), emptyList<String>(), listOf("lap-length"), emptyList<String>())
            .inOrder()
        assertThat(children.first().componentFirstRep.valueQuantity.value).isEqualTo(BigDecimal("12"))
        assertThat(children[2].componentFirstRep.valueQuantity.value).isEqualTo(BigDecimal("400.0"))
        assertThat(children[2].componentFirstRep.valueQuantity.code).isEqualTo("m")
        assertThat(children.first().effectivePeriod.startElement.valueAsString).isEqualTo(start.toString())
    }

    @Test
    fun `derives a stable workout-segment identity from its exact interval and token`() {
        val start = Instant.parse("2026-08-19T08:00:00Z")
        val record = exerciseSession(
            exerciseType = ExerciseSessionRecord.EXERCISE_TYPE_OTHER_WORKOUT,
            id = "workout-stable-identity",
            segments = listOf(
                ExerciseSegment(start, start.plusSeconds(900), ExerciseSegment.EXERCISE_SEGMENT_TYPE_RUNNING, 0),
                ExerciseSegment(
                    start.plusSeconds(900),
                    start.plusSeconds(1_800),
                    ExerciseSegment.EXERCISE_SEGMENT_TYPE_RUNNING,
                    0,
                ),
            ),
            laps = listOf(ExerciseLap(start, start.plusSeconds(900), Length.meters(400.0))),
        )

        val first = converter.convert(record, convertedAt).observations.map { outputIdentifier(it).value }
        val second = converter.convert(record, convertedAt).observations.map { outputIdentifier(it).value }

        assertThat(first).isEqualTo(second)
        assertThat(first.distinct()).hasSize(4)
        assertThat(first.all { it.startsWith("v1:") }).isTrue()
    }

    @Test
    fun `rejects a record that was not read back from Health Connect`() {
        val record = WeightRecord(
            time = Instant.parse("2026-08-19T15:15:00Z"),
            zoneOffset = ZoneOffset.UTC,
            weight = Mass.kilograms(68.4),
            metadata = Metadata.unknownRecordingMethod(),
        )

        assertThrows(InvalidHealthConnectRecord::class.java) {
            converter.convert(record, convertedAt)
        }
    }

    @Test
    fun `carries the writer's client record identity so a revision supersedes`() {
        fun weight(id: String, kilograms: Double, version: Long) = WeightRecord(
            time = Instant.parse("2026-08-19T15:15:00Z"),
            zoneOffset = ZoneOffset.UTC,
            weight = Mass.kilograms(kilograms),
            metadata = Metadata.activelyRecorded(
                device = Device(type = Device.TYPE_SCALE),
                clientRecordId = "scale-weighin-2026-08-19",
                clientRecordVersion = version,
            ).populatedWithTestValues(
                id = id,
                dataOrigin = DataOrigin("com.example.source"),
                lastModifiedTime = Instant.parse("2026-08-19T17:30:01Z"),
            ),
        )

        // The same logical measurement re-imported: Health Connect stores a new metadata.id, so
        // only the client record identity ties the two together.
        val first = converter.convert(weight("weight-v1", 68.4, 1), convertedAt, EventSequence("1"))
        val revision = converter.convert(weight("weight-v2", 68.9, 2), convertedAt, EventSequence("2"))

        fun clientRecordId(conversion: HealthConnectConversion) = conversion.observations.single()
            .identifier
            .single { it.system == HealthConnectContract.WRITER_RECORD_IDENTIFIER }
            .value
        fun clientRecordVersion(conversion: HealthConnectConversion) = conversion.observations.single()
            .getExtensionByUrl(HealthConnectContract.WRITER_RECORD_VERSION)
            .value.primitiveValue()

        // Scoped to the writer: two apps choosing the same id stay distinct measurements.
        assertThat(clientRecordId(first)).isEqualTo("v1:com.example.source|scale-weighin-2026-08-19")
        assertThat(clientRecordId(revision)).isEqualTo(clientRecordId(first))
        assertThat(clientRecordVersion(first)).isEqualTo("1")
        assertThat(clientRecordVersion(revision)).isEqualTo("2")
        // The record identifiers differ, which is exactly why the client identity is needed.
        assertThat(first.sourceRecordIdentifier.value).isNotEqualTo(revision.sourceRecordIdentifier.value)
    }

    @Test
    fun `a millisecond-scale client record version survives the wire`() {
        // clientRecordVersion is a Long and writers commonly use epoch millis. Narrowing it to a
        // FHIR integer wrapped 1.7e12 to a negative number, which inverted the supersession order.
        val version = 1_700_000_000_000L
        assertThat(version).isGreaterThan(Int.MAX_VALUE.toLong())

        val record = WeightRecord(
            time = Instant.parse("2026-08-19T15:15:00Z"),
            zoneOffset = ZoneOffset.UTC,
            weight = Mass.kilograms(68.4),
            metadata = Metadata.activelyRecorded(
                device = Device(type = Device.TYPE_SCALE),
                clientRecordId = "scale-millis",
                clientRecordVersion = version,
            ).populatedWithTestValues(
                id = "weight-millis",
                dataOrigin = DataOrigin("com.example.source"),
                lastModifiedTime = Instant.parse("2026-08-19T17:30:01Z"),
            ),
        )

        val conversion = converter.convert(record, convertedAt, EventSequence("1"))
        assertThat(
            conversion.observations.single()
                .getExtensionByUrl(HealthConnectContract.WRITER_RECORD_VERSION)
                .value.primitiveValue(),
        ).isEqualTo("1700000000000")
    }

    @Test
    fun `rejects sentinel source-version time rather than exporting it as issued`() {
        val record = WeightRecord(
            time = Instant.parse("2026-08-19T15:15:00Z"),
            zoneOffset = ZoneOffset.UTC,
            weight = Mass.kilograms(68.4),
            metadata = Metadata.manualEntry().populatedWithTestValues(
                id = "weight-with-sentinel-version",
                dataOrigin = DataOrigin("com.example.source"),
                lastModifiedTime = Instant.EPOCH,
            ),
        )

        assertThrows(InvalidHealthConnectRecord::class.java) {
            converter.convert(record, convertedAt)
        }
    }

    @Test
    fun `rejects a conversion event before the source version was available`() {
        val record = WeightRecord(
            time = Instant.parse("2026-08-19T15:15:00Z"),
            zoneOffset = ZoneOffset.UTC,
            weight = Mass.kilograms(68.4),
            metadata = metadata(Metadata.manualEntry(), id = "future-source-version"),
        )

        assertThrows(InvalidHealthConnectRecord::class.java) {
            converter.convert(record, Instant.parse("2026-08-19T17:30:00Z"))
        }
    }

    private fun metadata(
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
    private fun exerciseSession(
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

    private fun heartRateRecord(
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

    private fun stepRecord() = StepsRecord(
        startTime = Instant.parse("2026-08-19T16:00:00Z"),
        startZoneOffset = ZoneOffset.UTC,
        endTime = Instant.parse("2026-08-19T17:00:00Z"),
        endZoneOffset = ZoneOffset.UTC,
        count = 1,
        metadata = metadata(Metadata.autoRecorded(device)),
    )

    private fun bloodGlucoseRecord(specimenSource: Int, id: String) = BloodGlucoseRecord(
        time = Instant.parse("2026-08-19T16:00:00Z"),
        zoneOffset = ZoneOffset.UTC,
        metadata = metadata(Metadata.autoRecorded(device), id = id),
        level = BloodGlucose.milligramsPerDeciliter(95.5),
        specimenSource = specimenSource,
        mealType = androidx.health.connect.client.records.MealType.MEAL_TYPE_UNKNOWN,
        relationToMeal = BloodGlucoseRecord.RELATION_TO_MEAL_UNKNOWN,
    )

    private fun application(
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

    private fun identifier(system: String, value: String): Identifier =
        Identifier().setSystem(system).setValue(value)

    private fun outputIdentifier(observation: org.hl7.fhir.r4.model.Observation): Identifier =
        observationIdentity(observation)

    private fun sourceIdentifier(observation: org.hl7.fhir.r4.model.Observation): Identifier =
        observation.identifier.single {
            it.system == HealthConnectContract.HEALTH_CONNECT_RECORD_IDENTIFIER
        }

    private fun entryIdentifier(bundle: org.hl7.fhir.r4.model.Bundle, resourceType: String): Identifier =
        bundle.entry.single { it.resource.fhirType() == resourceType }
            .getExtensionByUrl(GroveExchangeIdentity.ENTRY_IDENTIFIER_EXTENSION)
            .value as Identifier

    private companion object {
        const val EXAMPLE_REPOSITORY_SCOPE = "1f5c58aa-6ec6-4e79-a682-829a9debd3f5"
        const val TEST_CONTEXT_IDENTIFIER_SYSTEM = "urn:uuid:8d3fd52b-efda-5f3d-b83d-50f0a70b44aa"
    }
}
