//
// This source file belongs to the My Heart Counts Android project
//
// SPDX-FileCopyrightText: 2026 Stanford University and the project authors (see CONTRIBUTORS.md)
//
// SPDX-License-Identifier: MIT

package org.grovealliance.health.fhir

import androidx.health.connect.client.records.BloodGlucoseRecord
import androidx.health.connect.client.records.ExerciseLap
import androidx.health.connect.client.records.ExerciseSegment
import androidx.health.connect.client.records.ExerciseSessionRecord
import androidx.health.connect.client.records.HeartRateRecord
import androidx.health.connect.client.records.MealType
import androidx.health.connect.client.records.NutritionRecord
import androidx.health.connect.client.records.SleepSessionRecord
import androidx.health.connect.client.records.StepsRecord
import androidx.health.connect.client.records.metadata.DataOrigin
import androidx.health.connect.client.records.metadata.Metadata
import androidx.health.connect.client.testing.populatedWithTestValues
import androidx.health.connect.client.units.BloodGlucose
import androidx.health.connect.client.units.Energy
import androidx.health.connect.client.units.Length
import androidx.health.connect.client.units.Mass
import com.google.common.truth.Truth.assertThat
import org.hl7.fhir.r4.model.CodeableConcept
import org.hl7.fhir.r4.model.Coding
import org.hl7.fhir.r4.model.Device
import org.hl7.fhir.r4.model.Identifier
import org.hl7.fhir.r4.model.Observation
import org.hl7.fhir.r4.model.Patient
import org.hl7.fhir.r4.model.Provenance
import org.hl7.fhir.r4.model.ResearchStudy
import org.hl7.fhir.r4.model.Resource
import org.hl7.fhir.r4.model.Specimen
import org.junit.Assert.assertThrows
import org.junit.Test
import java.time.Instant
import java.time.ZoneOffset
import androidx.health.connect.client.records.metadata.Device as HealthConnectDevice

class HealthConnectNativeIdentifierDisclosureTest {
    @Test
    fun `disclosure is omitted by default and emitted exactly on a one-to-one primary`() {
        val withoutDisclosure = converter().convert(steps("native-step-42"), CONVERTED_AT, EventSequence("1"))
        assertThat(withoutDisclosure.bundle.allIdentifiers().none { it.system == NATIVE_SYSTEM }).isTrue()

        val withDisclosure = converter(disclosure()).convert(
            steps("native-step-42"),
            CONVERTED_AT,
            EventSequence("1"),
        )
        val carryingResources = withDisclosure.bundle.entry.map { it.resource }.filter { resource ->
            resource.directIdentifiers().any {
                it.system == NATIVE_SYSTEM && it.value == "native-step-42"
            }
        }
        assertThat(carryingResources).hasSize(1)
        val observation = carryingResources.single() as Observation
        val native = observation.identifier.single { it.system == NATIVE_SYSTEM }
        assertThat(native.value).isEqualTo("native-step-42")
        assertThat(native.type.text).isEqualTo("Health Connect repository record id")
        assertThat(native.type.coding.single().system).isEqualTo(NATIVE_TYPE_SYSTEM)
        assertThat(native.type.coding.single().code).isEqualTo("health-connect-record-id")
        assertThat(native.type.coding.single().display).isEqualTo("Health Connect record id")
        assertThat(native.hasGroveRole(GroveIdentifierRole.SOURCE_RECORD)).isFalse()
        assertThat(native.hasGroveRole(GroveIdentifierRole.SOURCE_OUTPUT)).isFalse()
    }

    @Test
    fun `disclosure never propagates to samples stages segments laps nutrients specimens or support`() {
        val converter = converter(disclosure())
        val heartRate = converter.convert(heartRate(), CONVERTED_AT, EventSequence("1"))
        assertThat(heartRate.observations).hasSize(2)
        assertThat(heartRate.bundle.countNativeIdentifiers()).isEqualTo(0)

        val sleep = converter.convert(sleep(), CONVERTED_AT, EventSequence("2"))
        val sleepSummary = sleep.observations.single {
            it.meta.profile.any { profile ->
                profile.value == HealthConnectContract.MOBILE_SLEEP_DURATION_PROFILE
            }
        }
        val stages = sleep.observations.filter {
            it.meta.profile.any { profile ->
                profile.value == HealthConnectContract.MOBILE_SLEEP_STAGE_PROFILE
            }
        }
        assertThat(sleepSummary.identifier.count { it.system == NATIVE_SYSTEM }).isEqualTo(1)
        assertThat(stages).hasSize(2)
        assertThat(stages.all { stage -> stage.identifier.none { it.system == NATIVE_SYSTEM } }).isTrue()
        assertThat(sleep.bundle.countNativeIdentifiers()).isEqualTo(1)

        val workout = converter.convert(workout(), CONVERTED_AT, EventSequence("3"))
        val workoutSummary = workout.observations.single {
            it.meta.profile.any { profile -> profile.value == HealthConnectContract.MOBILE_WORKOUT_PROFILE }
        }
        val workoutChildren = workout.observations.filter {
            it.meta.profile.any { profile ->
                profile.value == HealthConnectContract.MOBILE_WORKOUT_SEGMENT_PROFILE
            }
        }
        assertThat(workoutSummary.identifier.count { it.system == NATIVE_SYSTEM }).isEqualTo(1)
        assertThat(workoutChildren).hasSize(2)
        assertThat(workoutChildren.all { child -> child.identifier.none { it.system == NATIVE_SYSTEM } }).isTrue()
        assertThat(workout.bundle.countNativeIdentifiers()).isEqualTo(1)

        val nutrition = converter.convert(nutrition(), CONVERTED_AT, EventSequence("4"))
        assertThat(nutrition.observations).hasSize(3)
        assertThat(nutrition.bundle.countNativeIdentifiers()).isEqualTo(0)

        val glucose = converter.convert(glucose(), CONVERTED_AT, EventSequence("5"))
        assertThat(glucose.observations.single().identifier.count { it.system == NATIVE_SYSTEM }).isEqualTo(1)
        val specimen = glucose.bundle.entry.single { it.resource is Specimen }.resource as Specimen
        assertThat(specimen.identifier.none { it.system == NATIVE_SYSTEM }).isTrue()
        assertThat(glucose.bundle.countNativeIdentifiers()).isEqualTo(1)
    }

    @Test
    fun `configuration rejects relative and Grove role systems and invalid type fields`() {
        assertThrows(IllegalArgumentException::class.java) {
            HealthConnectNativeIdentifierDisclosure("repository-records")
        }
        assertThrows(IllegalArgumentException::class.java) {
            HealthConnectNativeIdentifierDisclosure(HealthConnectContract.GROVE_IDENTIFIER_ROLE)
        }
        assertThrows(IllegalArgumentException::class.java) {
            HealthConnectNativeIdentifierType()
        }
        assertThrows(IllegalArgumentException::class.java) {
            HealthConnectNativeIdentifierTypeCoding(
                HealthConnectContract.GROVE_IDENTIFIER_ROLE,
                "source-record",
            )
        }
        assertThrows(IllegalArgumentException::class.java) {
            HealthConnectNativeIdentifierTypeCoding(NATIVE_TYPE_SYSTEM, "  ")
        }
        listOf(" leading", "trailing ", "two  spaces", "tab\tcode", "control\u0001code").forEach { code ->
            assertThrows(IllegalArgumentException::class.java) {
                HealthConnectNativeIdentifierTypeCoding(NATIVE_TYPE_SYSTEM, code)
            }
        }
    }

    @Test
    fun `conversion rejects Grove event entry and opaque systems as native namespaces`() {
        val forbiddenSystems = listOf(
            TEST_EVENT_SYSTEM,
            TEST_ENTRY_NODE_SYSTEM,
        ) + GroveOpaqueIdentityKind.entries.map(testIdentityKey()::identifierSystem)

        forbiddenSystems.forEachIndexed { index, system ->
            val forbidden = HealthConnectNativeIdentifierDisclosure(system)
            assertThrows(IllegalArgumentException::class.java) {
                converter(forbidden).convert(
                    steps("reserved-system-$index"),
                    CONVERTED_AT,
                    EventSequence((index + 1).toString()),
                )
            }
        }
    }

    @Test
    fun `type input snapshots a caller-owned mutable coding list`() {
        val mutableCodings = mutableListOf(
            HealthConnectNativeIdentifierTypeCoding(
                NATIVE_TYPE_SYSTEM,
                "health-connect-record-id",
            ),
        )
        val type = HealthConnectNativeIdentifierType(mutableCodings)
        mutableCodings.clear()

        val converted = converter(
            HealthConnectNativeIdentifierDisclosure(NATIVE_SYSTEM, type),
        ).convert(steps("snapshot-id"), CONVERTED_AT, EventSequence("1"))
        val native = converted.observations.single().identifier.single { it.system == NATIVE_SYSTEM }
        assertThat(native.type.coding.single().code).isEqualTo("health-connect-record-id")
    }

    private fun disclosure(): HealthConnectNativeIdentifierDisclosure =
        HealthConnectNativeIdentifierDisclosure(
            system = NATIVE_SYSTEM,
            type = HealthConnectNativeIdentifierType(
                codings = listOf(
                    HealthConnectNativeIdentifierTypeCoding(
                        system = NATIVE_TYPE_SYSTEM,
                        code = "health-connect-record-id",
                        display = "Health Connect record id",
                    ),
                ),
                text = "Health Connect repository record id",
            ),
        )

    private fun converter(
        disclosure: HealthConnectNativeIdentifierDisclosure? = null,
    ): HealthConnectConverter = HealthConnectConverter(
        context = HealthConnectConversionContext(
            subject = HealthConnectPatientSubject.Logical(
                Identifier().setSystem(PATIENT_SYSTEM).setValue("participant-7"),
            ),
            assembler = application(),
            eventIdentifierSystem = TEST_EVENT_SYSTEM,
            entryNodeIdentifierSystem = TEST_ENTRY_NODE_SYSTEM,
            userAuthoredTextPolicy = HealthConnectUserAuthoredTextPolicy.OMIT,
            nativeIdentifierDisclosure = disclosure,
        ),
        synchronizationScope = testSynchronizationScope(
            repositoryScope = TEST_PRODUCER_INSTANCE,
            configurationFingerprint = "native-identifier-tests-v1",
        ),
    )

    private fun application(): HealthConnectBundleResource<Device> {
        val identifier = Identifier()
            .setSystem(HealthConnectContract.ANDROID_PACKAGE_IDENTIFIER)
            .setValue("org.example.grove.fhir")
        return HealthConnectBundleResource(
            identifier,
            Device().apply {
                meta.addProfile(HealthConnectContract.MOBILE_APPLICATION_DEVICE_PROFILE)
                addIdentifier(identifier.copy())
                addDeviceName().setName("Test converter").setType(Device.DeviceNameType.USERFRIENDLYNAME)
                addVersion()
                    .setType(
                        CodeableConcept(
                            Coding(
                                HealthConnectContract.MDC,
                                HealthConnectContract.APPLICATION_SOFTWARE_VERSION,
                                "MDC_ID_PROD_SPEC_SW",
                            ),
                        ),
                    )
                    .setValue("1.0.0")
            },
        )
    }

    private fun metadata(id: String): Metadata = Metadata.autoRecorded(
        HealthConnectDevice(type = HealthConnectDevice.TYPE_PHONE),
    ).populatedWithTestValues(
        id = id,
        dataOrigin = DataOrigin("org.example.source"),
        lastModifiedTime = SOURCE_VERSION,
    )

    private fun steps(id: String): StepsRecord = StepsRecord(
        startTime = START,
        startZoneOffset = ZoneOffset.UTC,
        endTime = START.plusSeconds(3_600),
        endZoneOffset = ZoneOffset.UTC,
        count = 42,
        metadata = metadata(id),
    )

    private fun heartRate(): HeartRateRecord = HeartRateRecord(
        startTime = START,
        startZoneOffset = ZoneOffset.UTC,
        endTime = START.plusSeconds(60),
        endZoneOffset = ZoneOffset.UTC,
        samples = listOf(
            HeartRateRecord.Sample(START.plusSeconds(15), 72),
            HeartRateRecord.Sample(START.plusSeconds(45), 75),
        ),
        metadata = metadata("native-heart-rate"),
    )

    private fun sleep(): SleepSessionRecord = SleepSessionRecord(
        startTime = START,
        startZoneOffset = ZoneOffset.UTC,
        endTime = START.plusSeconds(7_200),
        endZoneOffset = ZoneOffset.UTC,
        title = null,
        notes = null,
        stages = listOf(
            SleepSessionRecord.Stage(START, START.plusSeconds(3_600), SleepSessionRecord.STAGE_TYPE_SLEEPING),
            SleepSessionRecord.Stage(
                START.plusSeconds(3_600),
                START.plusSeconds(7_200),
                SleepSessionRecord.STAGE_TYPE_AWAKE,
            ),
        ),
        metadata = metadata("native-sleep"),
    )

    @Suppress("LongParameterList")
    private fun workout(): ExerciseSessionRecord = ExerciseSessionRecord::class.java
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
            START,
            ZoneOffset.UTC,
            START.plusSeconds(3_600),
            ZoneOffset.UTC,
            metadata("native-workout"),
            ExerciseSessionRecord.EXERCISE_TYPE_RUNNING,
            null,
            null,
            listOf(
                ExerciseSegment(
                    START,
                    START.plusSeconds(1_800),
                    ExerciseSegment.EXERCISE_SEGMENT_TYPE_RUNNING,
                    0,
                ),
            ),
            listOf(ExerciseLap(START, START.plusSeconds(1_800), Length.meters(400.0))),
        )

    private fun nutrition(): NutritionRecord = NutritionRecord(
        startTime = START,
        startZoneOffset = ZoneOffset.UTC,
        endTime = START.plusSeconds(1_800),
        endZoneOffset = ZoneOffset.UTC,
        metadata = metadata("native-nutrition"),
        energy = Energy.kilocalories(650.0),
        protein = Mass.grams(32.5),
        vitaminC = Mass.milligrams(90.0),
    )

    private fun glucose(): BloodGlucoseRecord = BloodGlucoseRecord(
        time = START,
        zoneOffset = ZoneOffset.UTC,
        metadata = metadata("native-glucose"),
        level = BloodGlucose.milligramsPerDeciliter(95.5),
        specimenSource = BloodGlucoseRecord.SPECIMEN_SOURCE_CAPILLARY_BLOOD,
        mealType = MealType.MEAL_TYPE_UNKNOWN,
        relationToMeal = BloodGlucoseRecord.RELATION_TO_MEAL_UNKNOWN,
    )

    private fun org.hl7.fhir.r4.model.Bundle.allIdentifiers(): List<Identifier> =
        entry.flatMap { it.resource.directIdentifiers() }

    private fun org.hl7.fhir.r4.model.Bundle.countNativeIdentifiers(): Int =
        allIdentifiers().count { it.system == NATIVE_SYSTEM }

    private fun Resource.directIdentifiers(): List<Identifier> = when (this) {
        is Observation -> identifier
        is Specimen -> identifier
        is Device -> identifier
        is Patient -> identifier
        is ResearchStudy -> identifier
        is Provenance -> emptyList()
        else -> emptyList()
    }

    private companion object {
        const val NATIVE_SYSTEM = "https://example.org/repositories/device-7/health-connect-records"
        const val NATIVE_TYPE_SYSTEM = "https://example.org/fhir/CodeSystem/source-identifier-type"
        const val PATIENT_SYSTEM = "https://example.org/fhir/identifiers/patient-pseudonyms"
        val START: Instant = Instant.parse("2026-08-19T08:00:00Z")
        val SOURCE_VERSION: Instant = Instant.parse("2026-08-19T17:30:01Z")
        val CONVERTED_AT: Instant = Instant.parse("2026-08-19T17:30:02Z")
    }
}
