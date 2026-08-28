//
// This source file belongs to the My Heart Counts Android project
//
// SPDX-FileCopyrightText: 2026 Stanford University and the project authors (see CONTRIBUTORS.md)
//
// SPDX-License-Identifier: MIT

package org.grovealliance.health.fhir

import androidx.health.connect.client.feature.ExperimentalMindfulnessSessionApi
import androidx.health.connect.client.records.BloodGlucoseRecord
import androidx.health.connect.client.records.BloodPressureRecord
import androidx.health.connect.client.records.CervicalMucusRecord
import androidx.health.connect.client.records.IntermenstrualBleedingRecord
import androidx.health.connect.client.records.MealType
import androidx.health.connect.client.records.MenstruationFlowRecord
import androidx.health.connect.client.records.MenstruationPeriodRecord
import androidx.health.connect.client.records.MindfulnessSessionRecord
import androidx.health.connect.client.records.NutritionRecord
import androidx.health.connect.client.records.OvulationTestRecord
import androidx.health.connect.client.records.SexualActivityRecord
import androidx.health.connect.client.records.metadata.Metadata
import androidx.health.connect.client.units.BloodGlucose
import androidx.health.connect.client.units.Energy
import androidx.health.connect.client.units.Mass
import androidx.health.connect.client.units.Pressure
import com.google.common.truth.Truth.assertThat
import org.hl7.fhir.r4.model.Coding
import org.hl7.fhir.r4.model.Specimen
import org.junit.Assert.assertThrows
import org.junit.Test
import java.math.BigDecimal
import java.time.Instant
import java.time.ZoneOffset

@OptIn(ExperimentalMindfulnessSessionApi::class)
class HealthConnectR4ConverterClinicalContextTest : HealthConnectR4ConverterTestSupport() {
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
                title = "Morning practice",
                notes = "Participant note",
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
        val sourceType = observation.method.coding.single()
        assertThat(sourceType.system).isEqualTo(HealthConnectContract.HEALTH_CONNECT_MINDFULNESS_SESSION_TYPE)
        assertThat(sourceType.code).isEqualTo("MINDFULNESS_SESSION_TYPE_MEDITATION")
        assertThat(
            observation.hasExtension(
                "https://grovealliance.org/fhir/health-connect/StructureDefinition/" +
                    "health-connect-mindfulness-session-type",
            ),
        ).isFalse()
        assertThat(
            observation.getExtensionByUrl(HealthConnectContract.HEALTH_CONNECT_SESSION_TITLE)
                .value.primitiveValue(),
        ).isEqualTo("Morning practice")
        assertThat(observation.note.single().text).isEqualTo("Participant note")
    }

    @Test
    fun `mindfulness text minimization omits title and notes but retains method`() {
        val start = Instant.parse("2026-08-19T07:00:00Z")
        val minimizingConverter = HealthConnectConverter(
            fhirContext.copy(userAuthoredTextPolicy = HealthConnectUserAuthoredTextPolicy.OMIT),
            synchronizationScope,
        )
        val observation = minimizingConverter.convert(
            MindfulnessSessionRecord(
                startTime = start,
                startZoneOffset = ZoneOffset.UTC,
                endTime = start.plusSeconds(300),
                endZoneOffset = ZoneOffset.UTC,
                metadata = metadata(Metadata.manualEntry(), id = "mindfulness-minimized"),
                mindfulnessSessionType = MindfulnessSessionRecord.MINDFULNESS_SESSION_TYPE_BREATHING,
                title = "Private title",
                notes = "Private note",
            ),
            convertedAt,
            EventSequence("1"),
        ).observations.single()

        assertThat(observation.hasExtension(HealthConnectContract.HEALTH_CONNECT_SESSION_TITLE)).isFalse()
        assertThat(observation.note).isEmpty()
        assertThat(observation.method.coding.single().code)
            .isEqualTo("MINDFULNESS_SESSION_TYPE_BREATHING")
    }

    @Test
    fun `rejects an unsupported mindfulness method instead of emitting an ungoverned code`() {
        val start = Instant.parse("2026-08-19T07:00:00Z")
        val record = MindfulnessSessionRecord(
            startTime = start,
            startZoneOffset = ZoneOffset.UTC,
            endTime = start.plusSeconds(300),
            endZoneOffset = ZoneOffset.UTC,
            metadata = metadata(Metadata.manualEntry(), id = "mindfulness-unknown-method"),
            mindfulnessSessionType = Int.MAX_VALUE,
            title = null,
            notes = null,
        )

        assertThrows(InvalidHealthConnectRecord::class.java) {
            converter.convert(record, convertedAt)
        }
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
    @Suppress("LongMethod")
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
            assertThat(specimen.identifier).hasSize(2)
            val specimenSourceIdentifier = specimen.identifier.single {
                it.hasGroveRole(GroveIdentifierRole.SOURCE_RECORD)
            }
            val specimenOutputIdentifier = specimen.identifier.single {
                it.hasGroveRole(GroveIdentifierRole.SOURCE_OUTPUT)
            }
            assertThat(specimenSourceIdentifier.system).isEqualTo(result.sourceRecordIdentifier.system)
            assertThat(specimenSourceIdentifier.value).isEqualTo(result.sourceRecordIdentifier.value)
            assertThat(specimenEntry.fullUrl).isEqualTo(
                GroveExchangeIdentity.fullUrl(specimenOutputIdentifier),
            )
            val specimenTarget = result.provenance?.target?.single { it.type == "Specimen" }
            assertThat(specimenTarget?.reference).isEqualTo(specimenEntry.fullUrl)
            assertThat(specimenTarget?.identifier?.system).isEqualTo(specimenOutputIdentifier.system)
            assertThat(specimenTarget?.identifier?.value).isEqualTo(specimenOutputIdentifier.value)

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
            assertThat(specimen.subject.reference).isEqualTo(observation.subject.reference)
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
}
