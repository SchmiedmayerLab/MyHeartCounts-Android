//
// This source file belongs to the My Heart Counts Android project
//
// SPDX-FileCopyrightText: 2026 Stanford University and the project authors (see CONTRIBUTORS.md)
//
// SPDX-License-Identifier: MIT

package org.grovealliance.health.fhir

import androidx.health.connect.client.feature.ExperimentalMindfulnessSessionApi
import androidx.health.connect.client.records.ExerciseLap
import androidx.health.connect.client.records.ExerciseSegment
import androidx.health.connect.client.records.ExerciseSessionRecord
import androidx.health.connect.client.records.HeartRateRecord
import androidx.health.connect.client.records.PlannedExerciseSessionRecord
import androidx.health.connect.client.records.WeightRecord
import androidx.health.connect.client.records.metadata.DataOrigin
import androidx.health.connect.client.records.metadata.Device
import androidx.health.connect.client.records.metadata.Metadata
import androidx.health.connect.client.testing.populatedWithTestValues
import androidx.health.connect.client.units.Length
import androidx.health.connect.client.units.Mass
import com.google.common.truth.Truth.assertThat
import org.hl7.fhir.r4.model.Coding
import org.junit.Assert.assertThrows
import org.junit.Test
import java.math.BigDecimal
import java.time.Instant
import java.time.ZoneOffset

@OptIn(ExperimentalMindfulnessSessionApi::class)
class HealthConnectR4ConverterTemporalSessionTest : HealthConnectR4ConverterTestSupport() {
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

        val outcome = converter.convertOutcome(unsupported, convertedAt, EventSequence("1"))
        assertThat(outcome).isInstanceOf(HealthConnectConversionOutcome.Unsupported::class.java)
        assertThat((outcome as HealthConnectConversionOutcome.Unsupported).sourceType)
            .isEqualTo("PlannedExerciseSessionRecord")
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
        assertThat(workout.getExtensionByUrl(HealthConnectContract.HEALTH_CONNECT_SESSION_TITLE).value.toString())
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
        assertThat(first.all { it.matches(Regex("v2:test-key:1:[A-Za-z0-9_-]{43}")) }).isTrue()
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
            .single { it.hasGroveRole(GroveIdentifierRole.WRITER_RECORD) }
            .value
        fun clientRecordVersion(conversion: HealthConnectConversion) = conversion.observations.single()
            .getExtensionByUrl(HealthConnectContract.WRITER_RECORD_VERSION)
            .value.primitiveValue()

        // Scoped to the writer: two apps choosing the same id stay distinct measurements.
        assertThat(clientRecordId(first)).startsWith("v2:test-key:1:")
        assertThat(clientRecordId(first)).doesNotContain("com.example.source")
        assertThat(clientRecordId(first)).doesNotContain("scale-weighin")
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
    fun `client record version preserves the complete non-negative Long domain`() {
        listOf(0L, Long.MAX_VALUE).forEachIndexed { index, version ->
            val record = WeightRecord(
                time = Instant.parse("2026-08-19T15:15:00Z"),
                zoneOffset = ZoneOffset.UTC,
                weight = Mass.kilograms(68.4),
                metadata = Metadata.activelyRecorded(
                    device = Device(type = Device.TYPE_SCALE),
                    clientRecordId = "scale-boundary-$index",
                    clientRecordVersion = version,
                ).populatedWithTestValues(
                    id = "weight-boundary-$index",
                    dataOrigin = DataOrigin("com.example.source"),
                    lastModifiedTime = Instant.parse("2026-08-19T17:30:01Z"),
                ),
            )

            val conversion = converter.convert(
                record,
                convertedAt,
                EventSequence((index + 1).toString()),
            )
            assertThat(
                conversion.observations.single()
                    .getExtensionByUrl(HealthConnectContract.WRITER_RECORD_VERSION)
                    .value.primitiveValue(),
            ).isEqualTo(version.toString())
        }
    }

    @Test
    fun `rejects a blank client record id and a negative version`() {
        listOf("" to 0L, "writer-record" to -1L).forEachIndexed { index, (id, version) ->
            val record = WeightRecord(
                time = Instant.parse("2026-08-19T15:15:00Z"),
                zoneOffset = ZoneOffset.UTC,
                weight = Mass.kilograms(68.4),
                metadata = metadataWithClientRecord(
                    recordingMethod = Metadata.RECORDING_METHOD_ACTIVELY_RECORDED,
                    id = "invalid-writer-$index",
                    dataOrigin = DataOrigin("com.example.source"),
                    lastModifiedTime = Instant.parse("2026-08-19T17:30:01Z"),
                    clientRecordId = id,
                    clientRecordVersion = version,
                    device = Device(type = Device.TYPE_SCALE),
                ),
            )

            assertThrows(InvalidHealthConnectRecord::class.java) {
                converter.convert(record, convertedAt, EventSequence((index + 1).toString()))
            }
        }
    }

    @Test
    fun `omits the default client record version when no client id exists`() {
        val conversion = converter.convert(
            WeightRecord(
                time = Instant.parse("2026-08-19T15:15:00Z"),
                zoneOffset = ZoneOffset.UTC,
                weight = Mass.kilograms(68.4),
                metadata = metadata(Metadata.manualEntry(), id = "no-writer-record"),
            ),
            convertedAt,
        )

        assertThat(
            conversion.observations.single()
                .getExtensionsByUrl(HealthConnectContract.WRITER_RECORD_VERSION),
        ).isEmpty()
        assertThat(
            conversion.observations.single().identifier.any {
                it.hasGroveRole(GroveIdentifierRole.WRITER_RECORD)
            },
        ).isFalse()
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

    @Suppress("LongParameterList")
    private fun metadataWithClientRecord(
        recordingMethod: Int,
        id: String,
        dataOrigin: DataOrigin,
        lastModifiedTime: Instant,
        clientRecordId: String?,
        clientRecordVersion: Long,
        device: Device?,
    ): Metadata = Metadata::class.java
        .getDeclaredConstructor(
            Int::class.javaPrimitiveType,
            String::class.java,
            DataOrigin::class.java,
            Instant::class.java,
            String::class.java,
            Long::class.javaPrimitiveType,
            Device::class.java,
        ).apply { isAccessible = true }
        .newInstance(
            recordingMethod,
            id,
            dataOrigin,
            lastModifiedTime,
            clientRecordId,
            clientRecordVersion,
            device,
        )
}
