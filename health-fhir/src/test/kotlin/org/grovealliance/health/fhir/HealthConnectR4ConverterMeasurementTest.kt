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
import androidx.health.connect.client.records.BodyFatRecord
import androidx.health.connect.client.records.BodyTemperatureMeasurementLocation
import androidx.health.connect.client.records.BodyTemperatureRecord
import androidx.health.connect.client.records.BodyWaterMassRecord
import androidx.health.connect.client.records.BoneMassRecord
import androidx.health.connect.client.records.DistanceRecord
import androidx.health.connect.client.records.ElevationGainedRecord
import androidx.health.connect.client.records.FloorsClimbedRecord
import androidx.health.connect.client.records.HeartRateRecord
import androidx.health.connect.client.records.HeartRateVariabilityRmssdRecord
import androidx.health.connect.client.records.HeightRecord
import androidx.health.connect.client.records.HydrationRecord
import androidx.health.connect.client.records.LeanBodyMassRecord
import androidx.health.connect.client.records.OxygenSaturationRecord
import androidx.health.connect.client.records.Record
import androidx.health.connect.client.records.RespiratoryRateRecord
import androidx.health.connect.client.records.SleepSessionRecord
import androidx.health.connect.client.records.StepsRecord
import androidx.health.connect.client.records.TotalCaloriesBurnedRecord
import androidx.health.connect.client.records.Vo2MaxRecord
import androidx.health.connect.client.records.WheelchairPushesRecord
import androidx.health.connect.client.records.metadata.Metadata
import androidx.health.connect.client.units.Energy
import androidx.health.connect.client.units.Length
import androidx.health.connect.client.units.Mass
import androidx.health.connect.client.units.Percentage
import androidx.health.connect.client.units.Power
import androidx.health.connect.client.units.Temperature
import androidx.health.connect.client.units.Volume
import com.google.common.truth.Truth.assertThat
import org.hl7.fhir.r4.model.Coding
import org.junit.Assert.assertThrows
import org.junit.Test
import java.math.BigDecimal
import java.time.Instant
import java.time.ZoneOffset
import org.hl7.fhir.r4.model.Device as FhirDevice

@OptIn(ExperimentalMindfulnessSessionApi::class)
class HealthConnectR4ConverterMeasurementTest : HealthConnectR4ConverterTestSupport() {
    @Test
    @Suppress("LongMethod")
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
            .isEqualTo(GroveExchangeIdentity.fullUrl(recordingDeviceSnapshotIdentifier))
        assertThat(observation.hasExtension(HealthConnectContract.RESEARCH_STUDY_EXTENSION)).isTrue()
        assertThat(
            observation.getExtensionsByUrl(
                HealthConnectContract.HEALTH_CONNECT_RECORD_TYPE_EXTENSION,
            ).single().value.primitiveValue(),
        ).isEqualTo("StepsRecord")
        assertThat(
            (observation.getExtensionByUrl(HealthConnectContract.RECORDING_METHOD_EXTENSION).value as Coding).code,
        ).isEqualTo("automatically-recorded")
        assertThat(observation.identifier.count { it.hasGroveRole(GroveIdentifierRole.SOURCE_OUTPUT) })
            .isEqualTo(1)
        assertThat(result.sourceRecordIdentifier.system)
            .isEqualTo(testIdentityKey().identifierSystem(GroveOpaqueIdentityKind.SOURCE_RECORD))
        assertThat(sourceIdentifier(observation).value).startsWith("v2:test-key:1:")
        assertThat(sourceIdentifier(observation).value).doesNotContain("source-record")
        assertThat(outputIdentifier(observation).value).isNotEqualTo(result.sourceRecordIdentifier.value)
        val provenance = requireNotNull(result.provenance)
        assertThat(provenance.entityFirstRep.what.identifier.system)
            .isEqualTo(result.sourceRecordIdentifier.system)
        assertThat(provenance.entityFirstRep.what.identifier.value)
            .isEqualTo(result.sourceRecordIdentifier.value)
        assertThat(provenance.occurredPeriod.startElement.valueAsString)
            .isEqualTo("2026-08-19T09:00:00-07:00")
        assertThat(provenance.occurredPeriod.endElement.valueAsString)
            .isEqualTo("2026-08-19T10:00:00-07:00")
        assertThat(provenance.recordedElement.valueAsString).isEqualTo(convertedAt.toString())
        // The source version, not the conversion instant: an unchanged Record has to convert to
        // the identical graph or the outbox stops recognising it as unchanged.
        assertThat(observation.issuedElement.valueAsString).isEqualTo("2026-08-19T17:30:01Z")
        assertThat(provenance.meta.profile.map { it.value })
            .containsExactly(HealthConnectContract.HEALTH_CONNECT_PROVENANCE_PROFILE)
        assertThat(result.bundle.entry.map { it.fullUrl }).containsNoDuplicates()
        val applicationSnapshots = result.bundle.entry.map { it.resource }
            .filterIsInstance<FhirDevice>()
            .filter { device ->
                device.meta.profile.any { it.value == HealthConnectContract.MOBILE_APPLICATION_DEVICE_PROFILE }
            }
        assertThat(applicationSnapshots).hasSize(1)
        assertThat(applicationSnapshots.all { device ->
            device.identifier.count { it.hasGroveRole(GroveIdentifierRole.DEVICE_SNAPSHOT) } == 1
        }).isTrue()
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
        val dataOrigin = provenance.entityFirstRep.agentFirstRep.who
        assertThat(dataOrigin.hasReference()).isFalse()
        assertThat(dataOrigin.type).isEqualTo("Device")
        assertThat(dataOrigin.identifier.system).isEqualTo(HealthConnectContract.ANDROID_PACKAGE_IDENTIFIER)
        assertThat(dataOrigin.identifier.value).isEqualTo("com.example.source")
        assertThat(result.bundle.entry.none { entry ->
            entry.resource is FhirDevice &&
                (entry.resource as FhirDevice).identifier.any { identifier ->
                    identifier.system == HealthConnectContract.ANDROID_PACKAGE_IDENTIFIER &&
                        identifier.value == "com.example.source"
                }
        }).isTrue()
        assertThat(provenance.agentFirstRep.who.reference)
            .isEqualTo(GroveExchangeIdentity.fullUrl(assemblerSnapshotIdentifier))
        assertThat(result.observations.map { it.device.reference }.distinct())
            .containsExactly(GroveExchangeIdentity.fullUrl(recordingDeviceSnapshotIdentifier))
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
        assertThat(summary.getExtensionByUrl(HealthConnectContract.HEALTH_CONNECT_SESSION_TITLE).value.toString())
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
            FloorsClimbedRecord(instant, ZoneOffset.UTC, end, ZoneOffset.UTC, 6.0, sourceMetadata),
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
        assertThat(vo2Max.method.codingFirstRep.system)
            .isEqualTo(HealthConnectContract.HEALTH_CONNECT_VO2_MAX_MEASUREMENT_METHOD)
        assertThat(vo2Max.method.codingFirstRep.code)
            .isEqualTo("MEASUREMENT_METHOD_HEART_RATE_RATIO")
        assertThat(rmssd.code.codingFirstRep.code).isEqualTo("heart-rate-variability-rmssd")
        assertThat(rmssd.valueQuantity.code).isEqualTo("ms")
        assertThat(rmssd.categoryFirstRep.codingFirstRep.code).isEqualTo("vital-signs")
    }

    @Test
    fun `catalog quantity domains admit zero and percentage endpoints but reject fractional totals`() {
        val instant = Instant.parse("2026-08-19T16:00:00Z")
        val end = instant.plusSeconds(3_600)
        val sourceMetadata = metadata(Metadata.autoRecorded(device), id = "value-domain")
        val records = listOf<Record>(
            BodyFatRecord(instant, ZoneOffset.UTC, Percentage(0.0), sourceMetadata),
            BodyFatRecord(instant, ZoneOffset.UTC, Percentage(100.0), sourceMetadata),
            OxygenSaturationRecord(instant, ZoneOffset.UTC, Percentage(0.0), sourceMetadata),
            OxygenSaturationRecord(instant, ZoneOffset.UTC, Percentage(100.0), sourceMetadata),
            FloorsClimbedRecord(instant, ZoneOffset.UTC, end, ZoneOffset.UTC, 0.0, sourceMetadata),
            WheelchairPushesRecord(instant, ZoneOffset.UTC, end, ZoneOffset.UTC, 0, sourceMetadata),
        )

        assertThat(records.map { converter.convert(it, convertedAt).observations.single().valueQuantity.value })
            .containsExactly(
                BigDecimal("0.0"),
                BigDecimal("100.0"),
                BigDecimal("0.0"),
                BigDecimal("100.0"),
                BigDecimal("0.0"),
                BigDecimal.ZERO,
            )
            .inOrder()
        assertThrows(InvalidHealthConnectRecord::class.java) {
            converter.convert(
                FloorsClimbedRecord(instant, ZoneOffset.UTC, end, ZoneOffset.UTC, 6.5, sourceMetadata),
                convertedAt,
            )
        }
        assertThrows(InvalidHealthConnectRecord::class.java) {
            HealthConnectContract.quantityValueDomains.getValue("body-fat-percentage")
                .requireValue(BigDecimal("100.01"), "percentage")
        }
        assertThrows(InvalidHealthConnectRecord::class.java) {
            HealthConnectContract.quantityValueDomains.getValue("oxygen-saturation")
                .requireValue(BigDecimal("-0.01"), "percentage")
        }
        listOf("flights-climbed", "step-count", "wheelchair-push-count").forEach { measurement ->
            assertThrows(InvalidHealthConnectRecord::class.java) {
                HealthConnectContract.quantityValueDomains.getValue(measurement)
                    .requireValue(BigDecimal("-1"), "$measurement total")
            }
        }
        assertThat(
            HealthConnectContract.quantityValueDomains.getValue("wheelchair-push-count")
                .requireValue(BigDecimal.ZERO, "total"),
        )
            .isEqualTo(BigDecimal.ZERO)
        assertThat(
            HealthConnectContract.quantityValueDomains.getValue("step-count")
                .requireValue(BigDecimal.ZERO, "total"),
        )
            .isEqualTo(BigDecimal.ZERO)
    }
}
